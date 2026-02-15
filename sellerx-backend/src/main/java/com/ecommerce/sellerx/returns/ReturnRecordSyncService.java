package com.ecommerce.sellerx.returns;

import com.ecommerce.sellerx.financial.TrendyolCargoInvoiceRepository;
import com.ecommerce.sellerx.financial.OrderShippingCostProjection;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.returns.dto.ClaimItem;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnRecordSyncService {

    private static final BigDecimal DEFAULT_PACKAGING_COST = new BigDecimal("5.00");

    private final TrendyolClaimRepository claimRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final TrendyolOrderRepository orderRepository;
    private final TrendyolProductRepository productRepository;
    private final TrendyolCargoInvoiceRepository cargoInvoiceRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public int syncReturnRecordsForStore(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        // 1. Get accepted claims (both claim-level and item-level accepted)
        List<TrendyolClaim> acceptedClaims = new ArrayList<>(
                claimRepository.findByStoreIdAndStatus(storeId, "Accepted"));

        // Also include "Created" claims where items were auto-accepted or accepted by seller
        List<TrendyolClaim> createdClaims = claimRepository.findByStoreIdAndStatus(storeId, "Created");
        int itemLevelAccepted = 0;
        for (TrendyolClaim claim : createdClaims) {
            if (claim.getItems() != null && claim.getItems().stream()
                    .anyMatch(i -> Boolean.TRUE.equals(i.getAutoAccepted())
                            || Boolean.TRUE.equals(i.getAcceptedBySeller()))) {
                acceptedClaims.add(claim);
                itemLevelAccepted++;
            }
        }
        if (itemLevelAccepted > 0) {
            log.info("Found {} 'Created' claims with item-level acceptance for store {}", itemLevelAccepted, storeId);
        }

        if (acceptedClaims.isEmpty()) {
            log.debug("No accepted claims for store {}", storeId);
            return 0;
        }

        // 2. Collect all order numbers and barcodes for batch loading
        Set<String> orderNumbers = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        for (TrendyolClaim claim : acceptedClaims) {
            if (claim.getOrderNumber() != null) {
                orderNumbers.add(claim.getOrderNumber());
            }
            if (claim.getItems() != null) {
                for (ClaimItem item : claim.getItems()) {
                    if (item.getBarcode() != null) {
                        barcodes.add(item.getBarcode());
                    }
                }
            }
        }

        // 3. Batch load products (N+1 prevention)
        Map<String, TrendyolProduct> productMap = new HashMap<>();
        if (!barcodes.isEmpty()) {
            List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, new ArrayList<>(barcodes));
            for (TrendyolProduct product : products) {
                productMap.put(product.getBarcode(), product);
            }
        }

        // 4. Batch load shipping costs
        List<String> orderNumberList = new ArrayList<>(orderNumbers);
        Map<String, BigDecimal> outboundShippingMap = new HashMap<>();
        Map<String, BigDecimal> returnShippingMap = new HashMap<>();

        if (!orderNumberList.isEmpty()) {
            try {
                List<OrderShippingCostProjection> outbound = cargoInvoiceRepository.sumOutboundShippingByOrderNumbers(storeId, orderNumberList);
                for (OrderShippingCostProjection p : outbound) {
                    outboundShippingMap.put(p.getOrderNumber(), p.getTotalAmount());
                }
            } catch (Exception e) {
                log.warn("Failed to load outbound shipping costs for store {}: {}", storeId, e.getMessage());
            }

            try {
                List<OrderShippingCostProjection> returns = cargoInvoiceRepository.sumReturnShippingByOrderNumbers(storeId, orderNumberList);
                for (OrderShippingCostProjection p : returns) {
                    returnShippingMap.put(p.getOrderNumber(), p.getTotalAmount());
                }
            } catch (Exception e) {
                log.warn("Failed to load return shipping costs for store {}: {}", storeId, e.getMessage());
            }
        }

        // 5. Process each claim and create return records
        List<ReturnRecord> newRecords = new ArrayList<>();
        int skippedDuplicates = 0;

        for (TrendyolClaim claim : acceptedClaims) {
            if (claim.getOrderNumber() == null || claim.getItems() == null) {
                continue;
            }

            // Find matching orders
            List<TrendyolOrder> matchingOrders = orderRepository.findByStoreIdAndTyOrderNumber(storeId, claim.getOrderNumber());
            if (matchingOrders.isEmpty()) {
                log.debug("No matching orders for claim {} orderNumber {}", claim.getClaimId(), claim.getOrderNumber());
                continue;
            }

            TrendyolOrder order = matchingOrders.get(0); // Use first matching order

            // Get shipping costs for this order
            BigDecimal orderOutboundShipping = outboundShippingMap.getOrDefault(claim.getOrderNumber(), BigDecimal.ZERO);
            BigDecimal orderReturnShipping = returnShippingMap.getOrDefault(claim.getOrderNumber(), BigDecimal.ZERO);

            // Calculate total items value for proportional distribution
            BigDecimal totalClaimValue = claim.getItems().stream()
                    .map(item -> item.getPrice() != null ? item.getPrice().multiply(
                            item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE) : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (ClaimItem claimItem : claim.getItems()) {
                if (claimItem.getBarcode() == null) {
                    continue;
                }

                // Idempotent check
                if (returnRecordRepository.existsByOrderIdAndBarcode(order.getId(), claimItem.getBarcode())) {
                    skippedDuplicates++;
                    continue;
                }

                // Get product cost from the latest CostAndStockInfo entry
                BigDecimal productCost = BigDecimal.ZERO;
                TrendyolProduct product = productMap.get(claimItem.getBarcode());
                if (product != null && product.getCostAndStockInfo() != null && !product.getCostAndStockInfo().isEmpty()) {
                    try {
                        // Get the latest cost entry by stockDate (most recent first)
                        CostAndStockInfo latestCost = product.getCostAndStockInfo().stream()
                                .filter(c -> c.getUnitCost() != null && c.getUnitCost() > 0)
                                .max(Comparator.comparing(
                                        c -> c.getStockDate() != null ? c.getStockDate() : LocalDate.MIN))
                                .orElse(null);
                        if (latestCost != null) {
                            Double effectiveCost = latestCost.getEffectiveUnitCost();
                            if (effectiveCost != null && effectiveCost > 0) {
                                productCost = BigDecimal.valueOf(effectiveCost).setScale(2, RoundingMode.HALF_UP);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Could not extract cost for barcode {}: {}", claimItem.getBarcode(), e.getMessage());
                    }
                }

                // Calculate proportional shipping costs
                BigDecimal itemValue = claimItem.getPrice() != null ?
                        claimItem.getPrice().multiply(claimItem.getQuantity() != null ? claimItem.getQuantity() : BigDecimal.ONE) :
                        BigDecimal.ZERO;
                BigDecimal proportion = totalClaimValue.compareTo(BigDecimal.ZERO) > 0 ?
                        itemValue.divide(totalClaimValue, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                BigDecimal shippingCostOut = orderOutboundShipping.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                BigDecimal shippingCostReturn = orderReturnShipping.multiply(proportion).setScale(2, RoundingMode.HALF_UP);

                // Calculate commission loss (from order's estimated commission, proportional)
                BigDecimal commissionLoss = BigDecimal.ZERO;
                if (order.getEstimatedCommission() != null && order.getGrossAmount() != null
                        && order.getGrossAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal orderProportion = itemValue.divide(order.getGrossAmount(), 4, RoundingMode.HALF_UP);
                    commissionLoss = order.getEstimatedCommission().multiply(orderProportion).setScale(2, RoundingMode.HALF_UP);
                }

                int quantity = claimItem.getQuantity() != null ? claimItem.getQuantity().intValue() : 1;

                ReturnRecord record = ReturnRecord.builder()
                        .order(order)
                        .store(store)
                        .barcode(claimItem.getBarcode())
                        .productName(claimItem.getProductName())
                        .quantity(quantity)
                        .productCost(productCost)
                        .shippingCostOut(shippingCostOut)
                        .shippingCostReturn(shippingCostReturn)
                        .commissionLoss(commissionLoss)
                        .packagingCost(DEFAULT_PACKAGING_COST)
                        .returnReason(claimItem.getReasonName())
                        .returnReasonCode(claimItem.getReasonCode())
                        .returnDate(claim.getClaimDate())
                        .returnStatus("RECEIVED")
                        .build();

                record.calculateTotalLoss(); // Excludes commission per V123
                newRecords.add(record);
            }
        }

        // 6. Batch save
        if (!newRecords.isEmpty()) {
            returnRecordRepository.saveAll(newRecords);
            log.info("Created {} return records for store {} ({} duplicates skipped)",
                    newRecords.size(), storeId, skippedDuplicates);
        } else {
            log.debug("No new return records to create for store {} ({} duplicates skipped)",
                    storeId, skippedDuplicates);
        }

        return newRecords.size();
    }
}
