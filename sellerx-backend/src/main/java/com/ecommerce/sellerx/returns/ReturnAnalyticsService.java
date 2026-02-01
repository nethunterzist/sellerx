package com.ecommerce.sellerx.returns;

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

    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");

    // Default costs (configurable later)
    private static final BigDecimal DEFAULT_SHIPPING_OUT = BigDecimal.valueOf(25);
    private static final BigDecimal DEFAULT_SHIPPING_RETURN = BigDecimal.valueOf(25);
    private static final BigDecimal DEFAULT_PACKAGING = BigDecimal.valueOf(5);

    /**
     * Get comprehensive return analytics for a store
     */
    public ReturnAnalyticsResponse getReturnAnalytics(UUID storeId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating return analytics for store {} from {} to {}", storeId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get returned orders from TrendyolOrder
        List<TrendyolOrder> returnedOrders = orderRepository.findReturnedOrdersByStoreAndDateRange(
                storeId, startDateTime, endDateTime);

        // Get all orders for return rate calculation
        long totalOrders = orderRepository.countByStoreIdAndOrderDateBetween(storeId, startDateTime, endDateTime);

        // Calculate statistics
        int totalReturns = returnedOrders.size();
        int totalReturnedItems = calculateTotalReturnedItems(returnedOrders);

        // Calculate cost breakdown
        ReturnCostBreakdown costBreakdown = calculateCostBreakdown(returnedOrders, storeId);

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
        List<TopReturnedProduct> topProducts = calculateTopReturnedProducts(returnedOrders, storeId, startDateTime, endDateTime);

        // Get return reason distribution
        Map<String, Integer> reasonDistribution = calculateReturnReasonDistribution(returnedOrders);

        // Get daily trend
        List<DailyReturnStats> dailyTrend = calculateDailyTrend(returnedOrders);

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

    private int calculateTotalReturnedItems(List<TrendyolOrder> returnedOrders) {
        return returnedOrders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    private ReturnCostBreakdown calculateCostBreakdown(List<TrendyolOrder> returnedOrders, UUID storeId) {
        BigDecimal totalProductCost = BigDecimal.ZERO;
        BigDecimal totalShippingOut = BigDecimal.ZERO;
        BigDecimal totalShippingReturn = BigDecimal.ZERO;
        BigDecimal totalCommissionLoss = BigDecimal.ZERO;
        BigDecimal totalPackaging = BigDecimal.ZERO;

        for (TrendyolOrder order : returnedOrders) {
            for (OrderItem item : order.getOrderItems()) {
                int qty = item.getQuantity();

                // Product cost
                if (item.getCost() != null) {
                    totalProductCost = totalProductCost.add(item.getCost().multiply(BigDecimal.valueOf(qty)));
                }

                // Commission loss (estimated)
                if (item.getUnitEstimatedCommission() != null) {
                    totalCommissionLoss = totalCommissionLoss.add(
                            item.getUnitEstimatedCommission().multiply(BigDecimal.valueOf(qty)));
                }

                // Shipping costs (default values for now)
                totalShippingOut = totalShippingOut.add(DEFAULT_SHIPPING_OUT.multiply(BigDecimal.valueOf(qty)));
                totalShippingReturn = totalShippingReturn.add(DEFAULT_SHIPPING_RETURN.multiply(BigDecimal.valueOf(qty)));

                // Packaging
                totalPackaging = totalPackaging.add(DEFAULT_PACKAGING.multiply(BigDecimal.valueOf(qty)));
            }
        }

        BigDecimal totalLoss = totalProductCost
                .add(totalShippingOut)
                .add(totalShippingReturn)
                .add(totalCommissionLoss)
                .add(totalPackaging);

        return ReturnCostBreakdown.builder()
                .productCost(totalProductCost)
                .shippingCostOut(totalShippingOut)
                .shippingCostReturn(totalShippingReturn)
                .commissionLoss(totalCommissionLoss)
                .packagingCost(totalPackaging)
                .totalLoss(totalLoss)
                .build();
    }

    private List<TopReturnedProduct> calculateTopReturnedProducts(
            List<TrendyolOrder> returnedOrders,
            UUID storeId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        // Group returns by barcode
        Map<String, List<OrderItem>> returnsByBarcode = new HashMap<>();
        Map<String, String> productNames = new HashMap<>();

        for (TrendyolOrder order : returnedOrders) {
            for (OrderItem item : order.getOrderItems()) {
                String barcode = item.getBarcode();
                returnsByBarcode.computeIfAbsent(barcode, k -> new ArrayList<>()).add(item);
                productNames.put(barcode, item.getProductName());
            }
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

            BigDecimal totalLoss = items.stream()
                    .map(item -> {
                        BigDecimal itemLoss = BigDecimal.ZERO;
                        if (item.getCost() != null) {
                            itemLoss = itemLoss.add(item.getCost().multiply(BigDecimal.valueOf(item.getQuantity())));
                        }
                        if (item.getUnitEstimatedCommission() != null) {
                            itemLoss = itemLoss.add(item.getUnitEstimatedCommission().multiply(BigDecimal.valueOf(item.getQuantity())));
                        }
                        itemLoss = itemLoss.add(DEFAULT_SHIPPING_OUT.add(DEFAULT_SHIPPING_RETURN).add(DEFAULT_PACKAGING)
                                .multiply(BigDecimal.valueOf(item.getQuantity())));
                        return itemLoss;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String riskLevel = calculateRiskLevel(returnRate);

            topProducts.add(TopReturnedProduct.builder()
                    .barcode(barcode)
                    .productName(productNames.get(barcode))
                    .imageUrl(productImages.get(barcode))
                    .returnCount(returnCount)
                    .soldCount(soldCount)
                    .returnRate(returnRate)
                    .totalLoss(totalLoss)
                    .riskLevel(riskLevel)
                    .topReasons(Collections.emptyList()) // TODO: Add reason tracking
                    .build());
        }

        // Sort by return count and limit to top 10
        return topProducts.stream()
                .sorted(Comparator.comparingInt(TopReturnedProduct::getReturnCount).reversed())
                .limit(10)
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

    private Map<String, Integer> calculateReturnReasonDistribution(List<TrendyolOrder> returnedOrders) {
        // For now, return empty map since we don't have return reasons in the current data
        // This will be populated when we integrate webhook return data
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("Bilinmiyor", returnedOrders.size());
        return distribution;
    }

    private List<DailyReturnStats> calculateDailyTrend(List<TrendyolOrder> returnedOrders) {
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
                    BigDecimal totalLoss = orders.stream()
                            .flatMap(order -> order.getOrderItems().stream())
                            .map(item -> {
                                BigDecimal itemLoss = BigDecimal.ZERO;
                                if (item.getCost() != null) {
                                    itemLoss = itemLoss.add(item.getCost().multiply(BigDecimal.valueOf(item.getQuantity())));
                                }
                                itemLoss = itemLoss.add(DEFAULT_SHIPPING_OUT.add(DEFAULT_SHIPPING_RETURN)
                                        .multiply(BigDecimal.valueOf(item.getQuantity())));
                                return itemLoss;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return DailyReturnStats.builder()
                            .date(date.toString())
                            .returnCount(returnCount)
                            .totalLoss(totalLoss)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
