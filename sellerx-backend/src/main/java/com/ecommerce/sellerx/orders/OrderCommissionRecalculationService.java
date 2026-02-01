package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for recalculating estimated commissions on orders.
 *
 * This service should be called AFTER financial sync completes, because:
 * 1. During initial order sync, products don't have commission rates yet
 * 2. Financial sync populates lastCommissionRate on products
 * 3. This service then updates orders with proper estimated commissions
 *
 * The recalculation only affects orders where:
 * - isCommissionEstimated = true (not yet confirmed by Financial API)
 * - estimatedCommission = 0 (no commission was calculated during order sync)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommissionRecalculationService {

    private final TrendyolOrderRepository orderRepository;
    private final TrendyolProductRepository productRepository;
    private final OrderCostCalculator costCalculator;

    /**
     * Recalculates estimated commissions for orders that have zero commission.
     * Uses the lastCommissionRate from products (populated by Financial API).
     *
     * @param storeId The store to recalculate commissions for
     * @return Number of orders updated
     */
    @Transactional
    public int recalculateEstimatedCommissions(UUID storeId) {
        log.info("[RECALCULATING] Eksik komisyonlar (0 TL olanlar) ürün bazlı güncelleniyor...");
        log.info("Starting commission recalculation for store: {}", storeId);

        // Find orders that need recalculation:
        // - isCommissionEstimated = true (still estimated, not confirmed by Financial API)
        // - estimatedCommission = 0 or null
        List<TrendyolOrder> ordersToUpdate = orderRepository.findOrdersNeedingCommissionRecalculation(storeId);

        if (ordersToUpdate.isEmpty()) {
            log.info("No orders need commission recalculation for store: {}", storeId);
            return 0;
        }

        log.info("[RECALCULATING] {} sipariş bulundu.", ordersToUpdate.size());
        log.info("Found {} orders needing commission recalculation for store: {}",
                ordersToUpdate.size(), storeId);

        // Load all products for this store into a cache for efficient lookup
        List<TrendyolProduct> products = productRepository.findByStoreId(storeId);
        Map<String, TrendyolProduct> productCache = products.stream()
                .filter(p -> p.getBarcode() != null)
                .collect(Collectors.toMap(
                        TrendyolProduct::getBarcode,
                        p -> p,
                        (existing, replacement) -> existing // Keep first if duplicates
                ));

        int updatedCount = 0;
        int batchSize = 100;

        for (int i = 0; i < ordersToUpdate.size(); i++) {
            TrendyolOrder order = ordersToUpdate.get(i);

            try {
                boolean updated = recalculateOrderCommission(order, productCache);
                if (updated) {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to recalculate commission for order {}: {}",
                        order.getTyOrderNumber(), e.getMessage());
            }

            // Batch save every 100 orders
            if ((i + 1) % batchSize == 0) {
                orderRepository.flush();
                log.info("[RECALCULATING] {}/{} sipariş işlendi... ({} güncellendi)",
                    i + 1, ordersToUpdate.size(), updatedCount);
                log.debug("Processed {} / {} orders", i + 1, ordersToUpdate.size());
            }
        }

        // Save remaining orders
        orderRepository.flush();

        log.info("[RECALCULATING] [COMPLETED] {} siparişin komisyonu güncellendi.", updatedCount);
        log.info("Commission recalculation completed for store {}: {} orders updated out of {} processed",
                storeId, updatedCount, ordersToUpdate.size());

        return updatedCount;
    }

    /**
     * Recalculates commission for a single order.
     *
     * @param order The order to recalculate
     * @param productCache Map of barcode to product for efficient lookup
     * @return true if the order was updated, false otherwise
     */
    private boolean recalculateOrderCommission(TrendyolOrder order, Map<String, TrendyolProduct> productCache) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return false;
        }

        BigDecimal totalEstimatedCommission = BigDecimal.ZERO;
        boolean anyItemUpdated = false;

        for (OrderItem item : order.getOrderItems()) {
            if (item.getBarcode() == null) {
                continue;
            }

            TrendyolProduct product = productCache.get(item.getBarcode());
            if (product == null) {
                continue;
            }

            // Get effective commission rate (lastCommissionRate or commissionRate)
            BigDecimal commissionRate = costCalculator.getEffectiveCommissionRate(product);

            if (commissionRate.compareTo(BigDecimal.ZERO) > 0 && item.getPrice() != null) {
                // IMPORTANT: vatBaseAmount field is actually VAT RATE (e.g., 20 for 20%), not VAT base amount
                // Formula: (price / (1 + vatRate/100)) * commissionRate / 100
                BigDecimal unitCommission = costCalculator.calculateUnitEstimatedCommission(
                        item.getPrice(),
                        item.getVatBaseAmount(), // This is actually VAT rate
                        commissionRate
                );

                // Update item
                item.setEstimatedCommissionRate(commissionRate);
                item.setUnitEstimatedCommission(unitCommission);

                // Add to total (multiply by quantity)
                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                totalEstimatedCommission = totalEstimatedCommission.add(
                        unitCommission.multiply(BigDecimal.valueOf(quantity)));

                anyItemUpdated = true;
            }
        }

        if (anyItemUpdated && totalEstimatedCommission.compareTo(BigDecimal.ZERO) > 0) {
            order.setEstimatedCommission(totalEstimatedCommission);
            orderRepository.save(order);
            return true;
        }

        return false;
    }
}
