package com.ecommerce.sellerx.returns;

import com.ecommerce.sellerx.financial.OrderShippingCostProjection;
import com.ecommerce.sellerx.financial.TrendyolCargoInvoiceRepository;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.returns.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnAnalyticsService {

    private final TrendyolOrderRepository orderRepository;
    private final TrendyolProductRepository productRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final TrendyolCargoInvoiceRepository cargoInvoiceRepository;
    private final TrendyolClaimRepository claimRepository;

    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");

    // Default packaging cost (configurable later)
    private static final BigDecimal DEFAULT_PACKAGING = BigDecimal.valueOf(5);

    /**
     * Get comprehensive return analytics for a store
     */
    public ReturnAnalyticsResponse getReturnAnalytics(UUID storeId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating return analytics for store {} from {} to {}", storeId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get returned orders from all 3 sources (orders, claims, cargo invoices)
        List<TrendyolOrder> returnedOrders = findAllReturnedOrders(storeId, startDateTime, endDateTime);

        // Get all orders for return rate calculation
        long totalOrders = orderRepository.countByStoreIdAndOrderDateBetween(storeId, startDateTime, endDateTime);

        // Build shipping cost maps from real cargo invoice data
        Map<String, BigDecimal> outboundShippingMap = buildShippingCostMap(storeId, returnedOrders, true);
        Map<String, BigDecimal> returnShippingMap = buildShippingCostMap(storeId, returnedOrders, false);

        // Calculate statistics
        int totalReturns = returnedOrders.size();
        int totalReturnedItems = calculateTotalReturnedItems(returnedOrders);

        // Calculate cost breakdown
        ReturnCostBreakdown costBreakdown = calculateCostBreakdown(
                returnedOrders, outboundShippingMap, returnShippingMap, storeId);

        // Calculate return rate
        BigDecimal returnRate = totalOrders > 0
                ? BigDecimal.valueOf(totalReturns)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate average loss per return
        BigDecimal avgLossPerReturn = totalReturns > 0
                ? costBreakdown.getTotalLoss().divide(BigDecimal.valueOf(totalReturns), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get top returned products
        List<TopReturnedProduct> topProducts = calculateTopReturnedProducts(
                returnedOrders, storeId, startDateTime, endDateTime,
                outboundShippingMap, returnShippingMap);

        // Get return reason distribution (from claims data)
        Map<String, Integer> reasonDistribution = calculateReturnReasonDistribution(returnedOrders, storeId);

        // Get daily trend
        List<DailyReturnStats> dailyTrend = calculateDailyTrend(
                returnedOrders, outboundShippingMap, returnShippingMap, storeId);

        return ReturnAnalyticsResponse.builder()
                .totalReturns(totalReturns)
                .totalReturnedItems(totalReturnedItems)
                .totalReturnLoss(costBreakdown.getTotalLoss())
                .returnRate(returnRate)
                .avgLossPerReturn(avgLossPerReturn)
                .costBreakdown(costBreakdown)
                .topReturnedProducts(topProducts)
                .returnReasonDistribution(reasonDistribution)
                .dailyTrend(dailyTrend)
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .calculatedAt(LocalDateTime.now(TURKEY_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    /**
     * Build a map of orderNumber → shipping cost from cargo invoices.
     * @param outbound true for 'Gönderi Kargo Bedeli', false for 'İade Kargo Bedeli'
     */
    private Map<String, BigDecimal> buildShippingCostMap(
            UUID storeId, List<TrendyolOrder> orders, boolean outbound) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> orderNumbers = orders.stream()
                .map(TrendyolOrder::getTyOrderNumber)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (orderNumbers.isEmpty()) {
            return Collections.emptyMap();
        }

        List<OrderShippingCostProjection> results = outbound
                ? cargoInvoiceRepository.sumOutboundShippingByOrderNumbers(storeId, orderNumbers)
                : cargoInvoiceRepository.sumReturnShippingByOrderNumbers(storeId, orderNumbers);

        return results.stream()
                .filter(r -> r.getOrderNumber() != null && r.getTotalAmount() != null)
                .collect(Collectors.toMap(
                        OrderShippingCostProjection::getOrderNumber,
                        OrderShippingCostProjection::getTotalAmount,
                        (a, b) -> a));
    }

    /**
     * Get outbound shipping cost for an order.
     * Priority: 1) cargo invoice data, 2) order's estimatedShippingCost,
     *           3) product reference MAX(lastShippingCostPerUnit) — package-based, 4) zero
     */
    private BigDecimal getOutboundShippingCost(TrendyolOrder order, Map<String, BigDecimal> outboundMap, UUID storeId) {
        BigDecimal fromInvoice = outboundMap.get(order.getTyOrderNumber());
        if (fromInvoice != null && fromInvoice.compareTo(BigDecimal.ZERO) > 0) {
            return fromInvoice;
        }
        // Fallback to order's estimated shipping cost (may already be from cargo invoice)
        if (order.getEstimatedShippingCost() != null && order.getEstimatedShippingCost().compareTo(BigDecimal.ZERO) > 0) {
            return order.getEstimatedShippingCost();
        }
        // Fallback: package-based shipping cost — MAX(lastShippingCostPerUnit) across products
        // Trendyol charges shipping per PACKAGE, not per item
        if (order.getOrderItems() != null) {
            BigDecimal maxRef = BigDecimal.ZERO;
            for (OrderItem item : order.getOrderItems()) {
                if (item.getBarcode() != null) {
                    Optional<TrendyolProduct> product = productRepository.findByStoreIdAndBarcode(
                            storeId, item.getBarcode());
                    if (product.isPresent() && product.get().getLastShippingCostPerUnit() != null
                            && product.get().getLastShippingCostPerUnit().compareTo(maxRef) > 0) {
                        maxRef = product.get().getLastShippingCostPerUnit();
                    }
                }
            }
            if (maxRef.compareTo(BigDecimal.ZERO) > 0) {
                return maxRef;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get return shipping cost for an order.
     * Priority: 1) cargo invoice data (gerçek), 2) order's returnShippingCost (gerçek),
     *           3) outbound shipping as estimate (tahmini - iade ≈ gönderi), 4) zero
     */
    private BigDecimal getReturnShippingCost(TrendyolOrder order,
                                              Map<String, BigDecimal> returnMap,
                                              Map<String, BigDecimal> outboundMap,
                                              UUID storeId) {
        // 1. Real return cargo invoice
        BigDecimal fromInvoice = returnMap.get(order.getTyOrderNumber());
        if (fromInvoice != null && fromInvoice.compareTo(BigDecimal.ZERO) > 0) {
            return fromInvoice;
        }
        // 2. Order's return shipping cost field (already populated from invoice sync)
        if (order.getReturnShippingCost() != null && order.getReturnShippingCost().compareTo(BigDecimal.ZERO) > 0) {
            return order.getReturnShippingCost();
        }
        // 3. Estimate: use outbound shipping as reference (iade kargo ≈ gönderi kargo)
        BigDecimal outboundCost = getOutboundShippingCost(order, outboundMap, storeId);
        if (outboundCost.compareTo(BigDecimal.ZERO) > 0) {
            return outboundCost;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if return shipping cost is estimated (no real return cargo invoice).
     */
    private boolean isReturnShippingEstimated(TrendyolOrder order, Map<String, BigDecimal> returnMap) {
        BigDecimal fromInvoice = returnMap.get(order.getTyOrderNumber());
        if (fromInvoice != null && fromInvoice.compareTo(BigDecimal.ZERO) > 0) {
            return false; // Real invoice data
        }
        if (order.getReturnShippingCost() != null && order.getReturnShippingCost().compareTo(BigDecimal.ZERO) > 0) {
            return false; // Already populated from invoice sync
        }
        return true; // Using estimate or zero
    }

    private int calculateTotalReturnedItems(List<TrendyolOrder> returnedOrders) {
        return returnedOrders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    private ReturnCostBreakdown calculateCostBreakdown(
            List<TrendyolOrder> returnedOrders,
            Map<String, BigDecimal> outboundShippingMap,
            Map<String, BigDecimal> returnShippingMap,
            UUID storeId) {
        BigDecimal totalProductCost = BigDecimal.ZERO;
        BigDecimal totalShippingOut = BigDecimal.ZERO;
        BigDecimal totalShippingReturn = BigDecimal.ZERO;
        BigDecimal totalCommissionLoss = BigDecimal.ZERO;
        BigDecimal totalPackaging = BigDecimal.ZERO;

        int estimatedReturnShippingCount = 0;
        int realReturnShippingCount = 0;

        for (TrendyolOrder order : returnedOrders) {
            // Shipping costs are per ORDER (not per item)
            totalShippingOut = totalShippingOut.add(getOutboundShippingCost(order, outboundShippingMap, storeId));
            totalShippingReturn = totalShippingReturn.add(getReturnShippingCost(order, returnShippingMap, outboundShippingMap, storeId));

            // Track estimated vs real return shipping
            if (isReturnShippingEstimated(order, returnShippingMap)) {
                estimatedReturnShippingCount++;
            } else {
                realReturnShippingCount++;
            }

            for (OrderItem item : order.getOrderItems()) {
                int qty = item.getQuantity();

                // Product cost - sadece satılamaz (isResalable == false) ise dahil et
                if (Boolean.FALSE.equals(order.getIsResalable()) && item.getCost() != null) {
                    totalProductCost = totalProductCost.add(item.getCost().multiply(BigDecimal.valueOf(qty)));
                }

                // Commission loss (estimated)
                if (item.getUnitEstimatedCommission() != null) {
                    totalCommissionLoss = totalCommissionLoss.add(
                            item.getUnitEstimatedCommission().multiply(BigDecimal.valueOf(qty)));
                }

                // Packaging per item
                totalPackaging = totalPackaging.add(DEFAULT_PACKAGING.multiply(BigDecimal.valueOf(qty)));
            }
        }

        // Trendyol iade durumunda komisyonu geri veriyor, totalLoss'a dahil etme
        BigDecimal totalLoss = totalProductCost
                .add(totalShippingOut)
                .add(totalShippingReturn)
                .add(totalPackaging);

        return ReturnCostBreakdown.builder()
                .productCost(totalProductCost)
                .shippingCostOut(totalShippingOut)
                .shippingCostReturn(totalShippingReturn)
                .commissionLoss(totalCommissionLoss)
                .packagingCost(totalPackaging)
                .totalLoss(totalLoss)
                .estimatedReturnShippingCount(estimatedReturnShippingCount)
                .realReturnShippingCount(realReturnShippingCount)
                .build();
    }

    private List<TopReturnedProduct> calculateTopReturnedProducts(
            List<TrendyolOrder> returnedOrders,
            UUID storeId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Map<String, BigDecimal> outboundShippingMap,
            Map<String, BigDecimal> returnShippingMap) {

        // Group returns by barcode, also track which orders each barcode appears in
        Map<String, List<OrderItem>> returnsByBarcode = new HashMap<>();
        Map<String, String> productNames = new HashMap<>();
        Map<String, Set<String>> barcodeToOrderNumbers = new HashMap<>();

        for (TrendyolOrder order : returnedOrders) {
            for (OrderItem item : order.getOrderItems()) {
                String barcode = item.getBarcode();
                returnsByBarcode.computeIfAbsent(barcode, k -> new ArrayList<>()).add(item);
                productNames.put(barcode, item.getProductName());
                barcodeToOrderNumbers.computeIfAbsent(barcode, k -> new HashSet<>())
                        .add(order.getTyOrderNumber());
            }
        }

        // Build order → shipping cost and resalable lookup for quick access
        Map<String, BigDecimal> orderOutboundCosts = new HashMap<>();
        Map<String, BigDecimal> orderReturnCosts = new HashMap<>();
        Map<String, Boolean> orderResalable = new HashMap<>();
        for (TrendyolOrder order : returnedOrders) {
            orderOutboundCosts.put(order.getTyOrderNumber(),
                    getOutboundShippingCost(order, outboundShippingMap, storeId));
            orderReturnCosts.put(order.getTyOrderNumber(),
                    getReturnShippingCost(order, returnShippingMap, outboundShippingMap, storeId));
            orderResalable.put(order.getTyOrderNumber(), order.getIsResalable());
        }

        // Get sold counts for each product
        Map<String, Integer> soldCounts = new HashMap<>();
        List<TrendyolOrder> allOrders = orderRepository.findRevenueOrdersByStoreAndDateRange(
                storeId, startDateTime, endDateTime);

        for (TrendyolOrder order : allOrders) {
            for (OrderItem item : order.getOrderItems()) {
                String barcode = item.getBarcode();
                soldCounts.merge(barcode, item.getQuantity(), Integer::sum);
            }
        }

        // Get product images
        List<TrendyolProduct> products = productRepository.findByStoreId(storeId);
        Map<String, String> productImages = products.stream()
                .filter(p -> p.getBarcode() != null && p.getImage() != null)
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, TrendyolProduct::getImage, (a, b) -> a));

        // Pre-fetch claims for reason extraction by barcode
        List<TrendyolClaim> allClaims = claimRepository.findByStoreIdAndDateRange(storeId, startDateTime, endDateTime);
        // Build barcode → reason counts map
        Map<String, Map<String, Integer>> barcodeReasonCounts = new HashMap<>();
        for (TrendyolClaim claim : allClaims) {
            if (claim.getItems() != null) {
                for (com.ecommerce.sellerx.returns.dto.ClaimItem claimItem : claim.getItems()) {
                    if (claimItem.getBarcode() != null && claimItem.getReasonName() != null
                            && !claimItem.getReasonName().isBlank()) {
                        barcodeReasonCounts
                                .computeIfAbsent(claimItem.getBarcode(), k -> new HashMap<>())
                                .merge(claimItem.getReasonName(), 1, Integer::sum);
                    }
                }
            }
        }

        // Build top returned products list
        List<TopReturnedProduct> topProducts = new ArrayList<>();

        for (Map.Entry<String, List<OrderItem>> entry : returnsByBarcode.entrySet()) {
            String barcode = entry.getKey();
            List<OrderItem> items = entry.getValue();

            int returnCount = items.stream().mapToInt(OrderItem::getQuantity).sum();
            int soldCount = soldCounts.getOrDefault(barcode, 0);

            BigDecimal returnRate = soldCount > 0
                    ? BigDecimal.valueOf(returnCount)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(soldCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(100);

            // Calculate per-item costs (commission excluded - Trendyol refunds it)
            // Packaging always included; product cost only when isResalable == false
            BigDecimal itemCosts = BigDecimal.ZERO;
            for (OrderItem item : items) {
                itemCosts = itemCosts.add(DEFAULT_PACKAGING.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            // Product cost: iterate orders to check isResalable per order
            for (TrendyolOrder order : returnedOrders) {
                if (!Boolean.FALSE.equals(order.getIsResalable())) continue;
                for (OrderItem item : order.getOrderItems()) {
                    if (barcode.equals(item.getBarcode()) && item.getCost() != null) {
                        itemCosts = itemCosts.add(item.getCost().multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }

            // Add shipping costs from orders this barcode appears in
            Set<String> orderNums = barcodeToOrderNumbers.getOrDefault(barcode, Collections.emptySet());
            BigDecimal shippingCosts = orderNums.stream()
                    .map(on -> orderOutboundCosts.getOrDefault(on, BigDecimal.ZERO)
                            .add(orderReturnCosts.getOrDefault(on, BigDecimal.ZERO)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalLoss = itemCosts.add(shippingCosts);

            String riskLevel = calculateRiskLevel(returnRate);

            // Get top 3 reasons for this barcode from claims data
            List<String> topReasons = Collections.emptyList();
            Map<String, Integer> reasonCounts = barcodeReasonCounts.get(barcode);
            if (reasonCounts != null && !reasonCounts.isEmpty()) {
                topReasons = reasonCounts.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(3)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
            }

            topProducts.add(TopReturnedProduct.builder()
                    .barcode(barcode)
                    .productName(productNames.get(barcode))
                    .imageUrl(productImages.get(barcode))
                    .returnCount(returnCount)
                    .soldCount(soldCount)
                    .returnRate(returnRate)
                    .totalLoss(totalLoss)
                    .riskLevel(riskLevel)
                    .topReasons(topReasons)
                    .build());
        }

        // Sort by return count (all products)
        return topProducts.stream()
                .sorted(Comparator.comparingInt(TopReturnedProduct::getReturnCount).reversed())
                .collect(Collectors.toList());
    }

    private String calculateRiskLevel(BigDecimal returnRate) {
        if (returnRate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return "CRITICAL";
        } else if (returnRate.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return "HIGH";
        } else if (returnRate.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Map<String, Integer> calculateReturnReasonDistribution(List<TrendyolOrder> returnedOrders, UUID storeId) {
        Map<String, Integer> distribution = new LinkedHashMap<>();

        for (TrendyolOrder order : returnedOrders) {
            String reason = "Bilinmiyor";
            if (order.getTyOrderNumber() != null) {
                List<TrendyolClaim> claims = claimRepository.findByStoreIdAndOrderNumber(storeId, order.getTyOrderNumber());
                if (!claims.isEmpty()) {
                    TrendyolClaim claim = claims.get(0);
                    if (claim.getItems() != null) {
                        for (com.ecommerce.sellerx.returns.dto.ClaimItem item : claim.getItems()) {
                            if (item.getReasonName() != null && !item.getReasonName().isBlank()) {
                                reason = item.getReasonName();
                                break;
                            }
                        }
                    }
                }
            }
            distribution.merge(reason, 1, Integer::sum);
        }

        // Sort by count descending
        return distribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private List<DailyReturnStats> calculateDailyTrend(
            List<TrendyolOrder> returnedOrders,
            Map<String, BigDecimal> outboundShippingMap,
            Map<String, BigDecimal> returnShippingMap,
            UUID storeId) {
        // Group by date
        Map<LocalDate, List<TrendyolOrder>> ordersByDate = returnedOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()));

        return ordersByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<TrendyolOrder> orders = entry.getValue();

                    int returnCount = orders.size();
                    BigDecimal totalLoss = BigDecimal.ZERO;

                    for (TrendyolOrder order : orders) {
                        // Add shipping costs per order
                        totalLoss = totalLoss
                                .add(getOutboundShippingCost(order, outboundShippingMap, storeId))
                                .add(getReturnShippingCost(order, returnShippingMap, outboundShippingMap, storeId));

                        // Add product costs per item - sadece satılamaz (isResalable == false) ise
                        if (Boolean.FALSE.equals(order.getIsResalable())) {
                            for (OrderItem item : order.getOrderItems()) {
                                if (item.getCost() != null) {
                                    totalLoss = totalLoss.add(
                                            item.getCost().multiply(BigDecimal.valueOf(item.getQuantity())));
                                }
                            }
                        }
                    }

                    return DailyReturnStats.builder()
                            .date(date.toString())
                            .returnCount(returnCount)
                            .totalLoss(totalLoss)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // =====================================================
    // 3-Source Return Detection
    // =====================================================

    /**
     * Find all returned orders by combining 2 sources:
     * 1. Orders referenced in trendyol_claims (claim-based returns) — PRIMARY, matches Trendyol's "İade Talep Tarihi"
     * 2. Orders with return cargo invoices ('İade Kargo Bedeli') — SECONDARY, safety net
     *
     * Note: Previously included Source 1 (orders with status='Returned' filtered by orderDate),
     * but orderDate is the order creation date, NOT the return date. This caused mismatches with
     * Trendyol's return counts which filter by claimDate. Removed to align with Trendyol.
     *
     * Deduplicates by tyOrderNumber to avoid counting the same return multiple times.
     */
    private List<TrendyolOrder> findAllReturnedOrders(UUID storeId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        // Source 1 (REMOVED): orderDate is the order creation date, not the return date.
        // Trendyol filters by "İade Talep Tarihi" (claimDate), so we use claims as primary source.

        // Source 2: Claims → orderNumbers (PRIMARY - matches Trendyol)
        List<TrendyolClaim> claims = claimRepository.findByStoreIdAndDateRange(storeId, startDateTime, endDateTime);
        Set<String> claimOrderNumbers = claims.stream()
                .map(TrendyolClaim::getOrderNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Source 3: Return cargo invoices → orderNumbers (SECONDARY - safety net)
        Set<String> cargoOrderNumbers = new HashSet<>(
                cargoInvoiceRepository.findReturnCargoOrderNumbers(
                        storeId, startDateTime.toLocalDate(), endDateTime.toLocalDate()));

        // Collect all unique order numbers
        Set<String> allOrderNumbers = new LinkedHashSet<>();
        allOrderNumbers.addAll(claimOrderNumbers);
        allOrderNumbers.addAll(cargoOrderNumbers);

        // Fetch TrendyolOrder objects for each order number
        Map<String, TrendyolOrder> orderMap = new LinkedHashMap<>();
        for (String orderNumber : allOrderNumbers) {
            List<TrendyolOrder> orders = orderRepository.findByStoreIdAndTyOrderNumber(storeId, orderNumber);
            if (!orders.isEmpty()) {
                orderMap.put(orderNumber, orders.get(0));
            }
        }

        log.info("Return detection for store {}: claimReturns={}, cargoReturns={}, totalUnique={}",
                storeId, claimOrderNumbers.size(), cargoOrderNumbers.size(), orderMap.size());

        return new ArrayList<>(orderMap.values());
    }

    // =====================================================
    // Returned Orders Listing & Resalable Decision
    // =====================================================

    /**
     * Get returned orders with cost breakdown for resalable decision UI.
     */
    public List<ReturnedOrderDto> getReturnedOrders(UUID storeId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get returned orders from all 3 sources
        List<TrendyolOrder> returnedOrders = findAllReturnedOrders(storeId, startDateTime, endDateTime);

        if (returnedOrders.isEmpty()) {
            return Collections.emptyList();
        }

        // Build shipping cost maps
        Map<String, BigDecimal> outboundShippingMap = buildShippingCostMap(storeId, returnedOrders, true);
        Map<String, BigDecimal> returnShippingMap = buildShippingCostMap(storeId, returnedOrders, false);

        // Pre-fetch all claims for this store/date range for efficient lookup
        List<TrendyolClaim> allClaims = claimRepository.findByStoreIdAndDateRange(storeId, startDateTime, endDateTime);
        Map<String, TrendyolClaim> claimsByOrderNumber = allClaims.stream()
                .filter(c -> c.getOrderNumber() != null)
                .collect(Collectors.toMap(TrendyolClaim::getOrderNumber, c -> c, (a, b) -> a));

        // Build set of order numbers from cargo invoices for returnSource detection
        Set<String> cargoInvoiceReturns = cargoInvoiceRepository.findReturnCargoOrderNumbers(
                storeId, startDate, endDate).stream().collect(Collectors.toSet());

        List<ReturnedOrderDto> result = new ArrayList<>();
        for (TrendyolOrder order : returnedOrders) {
            BigDecimal shippingOut = getOutboundShippingCost(order, outboundShippingMap, storeId);
            BigDecimal shippingReturn = getReturnShippingCost(order, returnShippingMap, outboundShippingMap, storeId);

            BigDecimal productCost = BigDecimal.ZERO;
            List<ReturnedOrderDto.ReturnedItemDto> itemDtos = new ArrayList<>();

            for (OrderItem item : order.getOrderItems()) {
                BigDecimal unitCost = item.getCost() != null ? item.getCost() : BigDecimal.ZERO;
                BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(item.getQuantity()));
                productCost = productCost.add(totalCost);

                itemDtos.add(ReturnedOrderDto.ReturnedItemDto.builder()
                        .barcode(item.getBarcode())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitCost(unitCost)
                        .totalCost(totalCost)
                        .build());
            }

            // Total loss depends on isResalable decision
            BigDecimal totalLoss = shippingOut.add(shippingReturn);
            if (Boolean.FALSE.equals(order.getIsResalable())) {
                totalLoss = totalLoss.add(productCost);
            }

            String customerName = "";
            if (order.getCustomerFirstName() != null) {
                customerName = order.getCustomerFirstName();
                if (order.getCustomerLastName() != null) {
                    customerName += " " + order.getCustomerLastName();
                }
            }

            // Determine return reason and source from claims
            String returnReason = null;
            String claimStatus = null;
            String returnSource;

            TrendyolClaim claim = claimsByOrderNumber.get(order.getTyOrderNumber());
            if (claim != null) {
                claimStatus = claim.getStatus();
                if (claim.getItems() != null && !claim.getItems().isEmpty()) {
                    returnReason = claim.getItems().get(0).getReasonName();
                }
            }

            // Determine source priority: order_status (in-memory check) > claim > cargo_invoice
            if ("Returned".equals(order.getStatus())) {
                returnSource = "order_status";
            } else if (claim != null) {
                returnSource = "claim";
            } else if (cargoInvoiceReturns.contains(order.getTyOrderNumber())) {
                returnSource = "cargo_invoice";
            } else {
                returnSource = "claim"; // fallback (found via claims date range)
            }

            result.add(ReturnedOrderDto.builder()
                    .orderNumber(order.getTyOrderNumber())
                    .customerName(customerName)
                    .orderDate(order.getOrderDate())
                    .items(itemDtos)
                    .shippingCostOut(shippingOut)
                    .shippingCostReturn(shippingReturn)
                    .productCost(productCost)
                    .totalLoss(totalLoss)
                    .isResalable(order.getIsResalable())
                    .returnReason(returnReason)
                    .claimStatus(claimStatus)
                    .returnSource(returnSource)
                    .build());
        }

        return result;
    }

    /**
     * Update isResalable for a returned order (and its return records).
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateResalable(UUID storeId, String orderNumber, Boolean isResalable) {
        List<TrendyolOrder> orders = orderRepository.findByStoreIdAndTyOrderNumber(storeId, orderNumber);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderNumber);
        }

        for (TrendyolOrder order : orders) {
            order.setIsResalable(isResalable);
            orderRepository.save(order);

            // Also update associated return records
            List<ReturnRecord> records = returnRecordRepository.findByOrderId(order.getId());
            for (ReturnRecord record : records) {
                record.setIsResalable(isResalable);
                record.calculateTotalLoss();
                returnRecordRepository.save(record);
            }
        }

        log.info("Updated isResalable={} for order {} in store {}", isResalable, orderNumber, storeId);
    }
}
