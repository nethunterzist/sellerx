package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.purchasing.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseReportService {

    private final TrendyolProductRepository productRepository;
    private final TrendyolOrderRepository orderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    /**
     * Get product cost history with FIFO lot details
     */
    @Transactional(readOnly = true)
    public ProductCostHistoryResponse getProductCostHistory(UUID storeId, UUID productId) {
        TrendyolProduct product = productRepository.findByStoreIdAndId(storeId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<CostAndStockInfo> costHistory = product.getCostAndStockInfo();
        if (costHistory == null || costHistory.isEmpty()) {
            return ProductCostHistoryResponse.builder()
                    .productId(productId)
                    .productName(product.getTitle())
                    .barcode(product.getBarcode())
                    .productImage(product.getImage())
                    .entries(Collections.emptyList())
                    .averageCost(BigDecimal.ZERO)
                    .totalValue(BigDecimal.ZERO)
                    .totalQuantity(0)
                    .remainingQuantity(0)
                    .build();
        }

        // Find related purchase orders for each stock entry
        Map<LocalDate, PurchaseOrderItem> poItemsByDate = findPurchaseOrderItemsByProduct(storeId, productId);

        // Build cost entries
        List<ProductCostHistoryResponse.CostEntry> entries = new ArrayList<>();
        int totalQuantity = 0;
        int remainingQuantity = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal weightedCostSum = BigDecimal.ZERO;

        for (CostAndStockInfo stock : costHistory) {
            int qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
            int used = stock.getUsedQuantity();
            int remaining = stock.getRemainingQuantity();
            BigDecimal unitCost = stock.getUnitCost() != null ? BigDecimal.valueOf(stock.getUnitCost()) : BigDecimal.ZERO;
            BigDecimal entryValue = unitCost.multiply(BigDecimal.valueOf(remaining));
            double usagePercentage = qty > 0 ? (double) used / qty * 100 : 0;

            // Find associated PO
            PurchaseOrderItem poItem = poItemsByDate.get(stock.getStockDate());

            entries.add(ProductCostHistoryResponse.CostEntry.builder()
                    .stockDate(stock.getStockDate())
                    .quantity(qty)
                    .usedQuantity(used)
                    .remainingQuantity(remaining)
                    .unitCost(unitCost)
                    .vatRate(stock.getCostVatRate())
                    .totalValue(entryValue)
                    .usagePercentage(usagePercentage)
                    .purchaseOrderId(poItem != null ? poItem.getPurchaseOrder().getId() : null)
                    .purchaseOrderNumber(poItem != null ? poItem.getPurchaseOrder().getPoNumber() : null)
                    .build());

            totalQuantity += qty;
            remainingQuantity += remaining;
            totalValue = totalValue.add(entryValue);
            weightedCostSum = weightedCostSum.add(unitCost.multiply(BigDecimal.valueOf(qty)));
        }

        BigDecimal averageCost = totalQuantity > 0
                ? weightedCostSum.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Sort entries by date (most recent first)
        entries.sort((a, b) -> b.getStockDate().compareTo(a.getStockDate()));

        return ProductCostHistoryResponse.builder()
                .productId(productId)
                .productName(product.getTitle())
                .barcode(product.getBarcode())
                .productImage(product.getImage())
                .entries(entries)
                .averageCost(averageCost)
                .totalValue(totalValue)
                .totalQuantity(totalQuantity)
                .remainingQuantity(remainingQuantity)
                .build();
    }

    /**
     * Get FIFO analysis showing how stock lots were allocated to orders
     */
    @Transactional(readOnly = true)
    public FifoAnalysisResponse getFifoAnalysis(UUID storeId, String barcode, LocalDate startDate, LocalDate endDate) {
        TrendyolProduct product = productRepository.findByStoreIdAndBarcode(storeId, barcode)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with barcode: " + barcode));

        List<CostAndStockInfo> costHistory = product.getCostAndStockInfo();
        if (costHistory == null || costHistory.isEmpty()) {
            return FifoAnalysisResponse.builder()
                    .barcode(barcode)
                    .productName(product.getTitle())
                    .productId(product.getId())
                    .productImage(product.getImage())
                    .lots(Collections.emptyList())
                    .totalCost(BigDecimal.ZERO)
                    .totalRevenue(BigDecimal.ZERO)
                    .totalProfit(BigDecimal.ZERO)
                    .profitMargin(0.0)
                    .build();
        }

        // Get orders in date range
        List<TrendyolOrder> orders = orderRepository.findRevenueOrdersByStoreAndDateRange(
                storeId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        // Build lots with allocations
        List<FifoAnalysisResponse.FifoLot> lots = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (CostAndStockInfo stock : costHistory) {
            List<FifoAnalysisResponse.OrderAllocation> allocations = new ArrayList<>();

            // Find orders that used this stock lot
            for (TrendyolOrder order : orders) {
                if (order.getOrderItems() == null) continue;

                for (OrderItem item : order.getOrderItems()) {
                    if (barcode.equals(item.getBarcode()) &&
                        stock.getStockDate().equals(item.getStockDate())) {

                        BigDecimal costPerUnit = item.getCost() != null ? item.getCost() : BigDecimal.ZERO;
                        BigDecimal salePrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                        BigDecimal itemProfit = salePrice.subtract(costPerUnit).multiply(BigDecimal.valueOf(item.getQuantity()));
                        double itemMargin = salePrice.compareTo(BigDecimal.ZERO) > 0
                                ? itemProfit.divide(salePrice.multiply(BigDecimal.valueOf(item.getQuantity())), 4, RoundingMode.HALF_UP).doubleValue() * 100
                                : 0;

                        allocations.add(FifoAnalysisResponse.OrderAllocation.builder()
                                .orderNumber(order.getTyOrderNumber())
                                .orderDate(order.getOrderDate())
                                .quantity(item.getQuantity())
                                .costPerUnit(costPerUnit)
                                .salePrice(salePrice)
                                .profit(itemProfit)
                                .profitMargin(itemMargin)
                                .build());

                        totalCost = totalCost.add(costPerUnit.multiply(BigDecimal.valueOf(item.getQuantity())));
                        totalRevenue = totalRevenue.add(salePrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }

            lots.add(FifoAnalysisResponse.FifoLot.builder()
                    .stockDate(stock.getStockDate())
                    .originalQuantity(stock.getQuantity())
                    .usedQuantity(stock.getUsedQuantity())
                    .remainingQuantity(stock.getRemainingQuantity())
                    .unitCost(stock.getUnitCost() != null ? BigDecimal.valueOf(stock.getUnitCost()) : BigDecimal.ZERO)
                    .vatRate(stock.getCostVatRate())
                    .allocations(allocations)
                    .build());
        }

        // Sort lots by date
        lots.sort((a, b) -> a.getStockDate().compareTo(b.getStockDate()));

        BigDecimal totalProfit = totalRevenue.subtract(totalCost);
        double profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        return FifoAnalysisResponse.builder()
                .barcode(barcode)
                .productName(product.getTitle())
                .productId(product.getId())
                .productImage(product.getImage())
                .lots(lots)
                .totalCost(totalCost)
                .totalRevenue(totalRevenue)
                .totalProfit(totalProfit)
                .profitMargin(profitMargin)
                .build();
    }

    /**
     * Get stock valuation report with aging analysis and estimated depletion dates.
     * Uses Trendyol actual stock (trendyolQuantity) as the source of truth for current stock levels.
     * Calculates estimated depletion date based on average daily sales velocity (last 90 days).
     */
    @Transactional(readOnly = true)
    public StockValuationResponse getStockValuation(UUID storeId) {
        List<TrendyolProduct> products = productRepository.findByStoreId(storeId);
        LocalDate today = LocalDate.now();

        // Get sales velocity for all products (last 90 days)
        LocalDateTime salesStartDate = LocalDateTime.now().minusDays(90);
        Map<String, Long> salesByBarcode = orderRepository.getSalesVelocityByBarcode(storeId, salesStartDate)
                .stream()
                .collect(Collectors.toMap(
                        TrendyolOrderRepository.SalesVelocityProjection::getBarcode,
                        TrendyolOrderRepository.SalesVelocityProjection::getTotalQuantitySold,
                        (a, b) -> a
                ));

        List<StockValuationResponse.ProductValuation> valuations = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        int totalQuantity = 0;

        // Aging buckets
        BigDecimal value0to30 = BigDecimal.ZERO;
        BigDecimal value30to60 = BigDecimal.ZERO;
        BigDecimal value60to90 = BigDecimal.ZERO;
        BigDecimal value90plus = BigDecimal.ZERO;
        int count0to30 = 0;
        int count30to60 = 0;
        int count60to90 = 0;
        int count90plus = 0;

        for (TrendyolProduct product : products) {
            // Use Trendyol actual stock as the source of truth
            Integer trendyolStock = product.getTrendyolQuantity();
            if (trendyolStock == null || trendyolStock <= 0) continue;

            List<CostAndStockInfo> costHistory = product.getCostAndStockInfo();

            // Calculate FIFO value and average cost from purchase history
            BigDecimal productValue = BigDecimal.ZERO;
            BigDecimal weightedCost = BigDecimal.ZERO;
            LocalDate oldestDate = null;
            int historyQuantity = 0;

            if (costHistory != null && !costHistory.isEmpty()) {
                for (CostAndStockInfo stock : costHistory) {
                    int remaining = stock.getRemainingQuantity();
                    if (remaining <= 0) continue;

                    BigDecimal unitCost = stock.getUnitCost() != null ? BigDecimal.valueOf(stock.getUnitCost()) : BigDecimal.ZERO;
                    BigDecimal stockValue = unitCost.multiply(BigDecimal.valueOf(remaining));

                    historyQuantity += remaining;
                    productValue = productValue.add(stockValue);
                    weightedCost = weightedCost.add(stockValue);

                    if (oldestDate == null || (stock.getStockDate() != null && stock.getStockDate().isBefore(oldestDate))) {
                        oldestDate = stock.getStockDate();
                    }

                    // Aging calculation based on purchase history
                    if (stock.getStockDate() != null) {
                        long daysOld = ChronoUnit.DAYS.between(stock.getStockDate(), today);
                        if (daysOld <= 30) {
                            value0to30 = value0to30.add(stockValue);
                            count0to30 += remaining;
                        } else if (daysOld <= 60) {
                            value30to60 = value30to60.add(stockValue);
                            count30to60 += remaining;
                        } else if (daysOld <= 90) {
                            value60to90 = value60to90.add(stockValue);
                            count60to90 += remaining;
                        } else {
                            value90plus = value90plus.add(stockValue);
                            count90plus += remaining;
                        }
                    }
                }
            }

            // Get fallback cost from most recent entry (regardless of remaining quantity)
            // First try to find entry with unitCost > 0, then fall back to any entry with unitCost
            BigDecimal fallbackCost = BigDecimal.ZERO;
            if (costHistory != null && !costHistory.isEmpty()) {
                // First: Try to find entry with unitCost > 0
                CostAndStockInfo mostRecent = costHistory.stream()
                        .filter(c -> c.getUnitCost() != null && c.getUnitCost() > 0)
                        .max(Comparator.comparing(c -> c.getStockDate() != null ? c.getStockDate() : LocalDate.MIN))
                        .orElse(null);

                // Second fallback: If no entry with cost > 0, get the most recent entry with any unitCost
                if (mostRecent == null) {
                    mostRecent = costHistory.stream()
                            .filter(c -> c.getUnitCost() != null)
                            .max(Comparator.comparing(c -> c.getStockDate() != null ? c.getStockDate() : LocalDate.MIN))
                            .orElse(null);
                }

                if (mostRecent != null) {
                    fallbackCost = BigDecimal.valueOf(mostRecent.getUnitCost());
                }
            }

            // Calculate average cost per unit (from purchase history or fallback to most recent cost)
            BigDecimal averageCost;
            if (historyQuantity > 0) {
                averageCost = weightedCost.divide(BigDecimal.valueOf(historyQuantity), 2, RoundingMode.HALF_UP);
            } else {
                // Fallback to most recent cost entry (even if all stock is used)
                averageCost = fallbackCost;
            }

            // If we have no purchase history but have Trendyol stock, estimate value using fallback cost
            if (historyQuantity == 0 && trendyolStock > 0) {
                // Use fallback cost × Trendyol stock for estimated value
                productValue = averageCost.multiply(BigDecimal.valueOf(trendyolStock));
            }

            int daysInStock = oldestDate != null ? (int) ChronoUnit.DAYS.between(oldestDate, today) : 0;
            String agingCategory = daysInStock <= 30 ? "0-30" :
                                   daysInStock <= 60 ? "30-60" :
                                   daysInStock <= 90 ? "60-90" : "90+";

            // Calculate estimated depletion date based on sales velocity
            Long totalSold = salesByBarcode.get(product.getBarcode());
            Double avgDailySales = null;
            LocalDate estimatedDepletionDate = null;
            Integer daysUntilDepletion = null;

            if (totalSold != null && totalSold > 0) {
                // Average daily sales = total sold in 90 days / 90
                avgDailySales = totalSold / 90.0;

                if (avgDailySales > 0) {
                    // Days until depletion = current stock / daily sales
                    double daysDouble = trendyolStock / avgDailySales;
                    daysUntilDepletion = (int) Math.ceil(daysDouble);

                    // Cap at 365 days to avoid unreasonable projections
                    if (daysUntilDepletion > 365) {
                        daysUntilDepletion = 365;
                    }

                    estimatedDepletionDate = today.plusDays(daysUntilDepletion);
                }
            }

            valuations.add(StockValuationResponse.ProductValuation.builder()
                    .productId(product.getId())
                    .productName(product.getTitle())
                    .barcode(product.getBarcode())
                    .productImage(product.getImage())
                    .quantity(trendyolStock)  // Use Trendyol actual stock
                    .fifoValue(productValue)
                    .averageCost(averageCost)
                    .oldestStockDate(oldestDate)
                    .daysInStock(daysInStock)
                    .agingCategory(agingCategory)
                    .stockDepleted(product.getStockDepleted() != null && product.getStockDepleted())  // Use actual FIFO depletion status from entity
                    .averageDailySales(avgDailySales)
                    .estimatedDepletionDate(estimatedDepletionDate)
                    .daysUntilDepletion(daysUntilDepletion)
                    .build());

            totalValue = totalValue.add(productValue);
            totalQuantity += trendyolStock;
        }

        // Sort by value descending
        valuations.sort((a, b) -> b.getFifoValue().compareTo(a.getFifoValue()));

        return StockValuationResponse.builder()
                .products(valuations)
                .totalValue(totalValue)
                .totalProducts(valuations.size())
                .totalQuantity(totalQuantity)
                .aging(StockValuationResponse.AgingBreakdown.builder()
                        .days0to30(value0to30)
                        .count0to30(count0to30)
                        .days30to60(value30to60)
                        .count30to60(count30to60)
                        .days60to90(value60to90)
                        .count60to90(count60to90)
                        .days90plus(value90plus)
                        .count90plus(count90plus)
                        .build())
                .build();
    }

    /**
     * Get profitability analysis for a date range
     * Updated to include net profit calculation with all expenses (matching DashboardStatsService)
     */
    @Transactional(readOnly = true)
    public ProfitabilityResponse getProfitabilityAnalysis(UUID storeId, LocalDate startDate, LocalDate endDate) {
        List<TrendyolOrder> orders = orderRepository.findRevenueOrdersByStoreAndDateRange(
                storeId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        // Product profitability map
        Map<String, ProfitabilityResponse.ProductProfitability.ProductProfitabilityBuilder> productMap = new HashMap<>();

        // Daily profitability map
        Map<LocalDate, BigDecimal[]> dailyMap = new HashMap<>(); // [revenue, cost, orderCount]

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalQuantitySold = 0;

        // Expense tracking (order-level data)
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalShippingCost = BigDecimal.ZERO;
        BigDecimal totalStoppage = BigDecimal.ZERO;

        for (TrendyolOrder order : orders) {
            if (order.getOrderItems() == null) continue;

            LocalDate orderDate = order.getOrderDate().toLocalDate();
            BigDecimal[] dailyData = dailyMap.computeIfAbsent(orderDate, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            dailyData[2] = dailyData[2].add(BigDecimal.ONE);

            // Accumulate order-level expenses
            if (order.getEstimatedCommission() != null) {
                totalCommission = totalCommission.add(order.getEstimatedCommission());
            }
            if (order.getEstimatedShippingCost() != null) {
                totalShippingCost = totalShippingCost.add(order.getEstimatedShippingCost());
            }
            if (order.getStoppage() != null) {
                totalStoppage = totalStoppage.add(order.getStoppage());
            }

            for (OrderItem item : order.getOrderItems()) {
                BigDecimal itemRevenue = item.getPrice() != null
                        ? item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                        : BigDecimal.ZERO;
                BigDecimal itemCost = item.getCost() != null
                        ? item.getCost().multiply(BigDecimal.valueOf(item.getQuantity()))
                        : BigDecimal.ZERO;

                totalRevenue = totalRevenue.add(itemRevenue);
                totalCost = totalCost.add(itemCost);
                totalQuantitySold += item.getQuantity();

                // Daily aggregation
                dailyData[0] = dailyData[0].add(itemRevenue);
                dailyData[1] = dailyData[1].add(itemCost);

                // Product aggregation
                String barcode = item.getBarcode();
                if (barcode != null) {
                    ProfitabilityResponse.ProductProfitability.ProductProfitabilityBuilder builder =
                            productMap.computeIfAbsent(barcode, k -> {
                                return ProfitabilityResponse.ProductProfitability.builder()
                                        .productName(item.getProductName())
                                        .barcode(barcode)
                                        .quantitySold(0)
                                        .revenue(BigDecimal.ZERO)
                                        .cost(BigDecimal.ZERO)
                                        .profit(BigDecimal.ZERO)
                                        .costEstimated(false);
                            });

                    // Check if this item used LAST_KNOWN cost
                    boolean hasLastKnown = "LAST_KNOWN".equals(item.getCostSource());

                    // Update builder values
                    ProfitabilityResponse.ProductProfitability current = builder.build();
                    productMap.put(barcode, ProfitabilityResponse.ProductProfitability.builder()
                            .productName(item.getProductName())
                            .barcode(barcode)
                            .quantitySold(current.getQuantitySold() + item.getQuantity())
                            .revenue(current.getRevenue().add(itemRevenue))
                            .cost(current.getCost().add(itemCost))
                            .profit(current.getProfit().add(itemRevenue.subtract(itemCost)))
                            .costEstimated(Boolean.TRUE.equals(current.getCostEstimated()) || hasLastKnown));
                }
            }
        }

        // Finalize product profitability - lookup products for additional info
        List<String> barcodes = new ArrayList<>(productMap.keySet());
        List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, barcodes);
        Map<String, TrendyolProduct> productByBarcode = products.stream()
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p, (a, b) -> a));

        List<ProfitabilityResponse.ProductProfitability> productList = productMap.values().stream()
                .map(builder -> {
                    ProfitabilityResponse.ProductProfitability p = builder.build();
                    TrendyolProduct product = productByBarcode.get(p.getBarcode());
                    double margin = p.getRevenue().compareTo(BigDecimal.ZERO) > 0
                            ? p.getProfit().divide(p.getRevenue(), 4, RoundingMode.HALF_UP).doubleValue() * 100
                            : 0;
                    String category = margin > 30 ? "high" : margin > 15 ? "medium" : "low";
                    return ProfitabilityResponse.ProductProfitability.builder()
                            .productId(product != null ? product.getId() : null)
                            .productName(p.getProductName())
                            .barcode(p.getBarcode())
                            .productImage(product != null ? product.getImage() : null)
                            .quantitySold(p.getQuantitySold())
                            .revenue(p.getRevenue())
                            .cost(p.getCost())
                            .profit(p.getProfit())
                            .margin(margin)
                            .marginCategory(category)
                            .costEstimated(p.getCostEstimated())
                            .build();
                })
                .collect(Collectors.toList());

        // Sort and get top/bottom performers
        List<ProfitabilityResponse.ProductProfitability> topProfitable = productList.stream()
                .sorted((a, b) -> b.getProfit().compareTo(a.getProfit()))
                .limit(10)
                .collect(Collectors.toList());

        List<ProfitabilityResponse.ProductProfitability> leastProfitable = productList.stream()
                .sorted(Comparator.comparing(ProfitabilityResponse.ProductProfitability::getProfit))
                .limit(10)
                .collect(Collectors.toList());

        // Build daily trend
        List<ProfitabilityResponse.DailyProfitability> dailyTrend = dailyMap.entrySet().stream()
                .map(entry -> {
                    BigDecimal rev = entry.getValue()[0];
                    BigDecimal cost = entry.getValue()[1];
                    BigDecimal profit = rev.subtract(cost);
                    double margin = rev.compareTo(BigDecimal.ZERO) > 0
                            ? profit.divide(rev, 4, RoundingMode.HALF_UP).doubleValue() * 100
                            : 0;
                    return ProfitabilityResponse.DailyProfitability.builder()
                            .date(entry.getKey())
                            .revenue(rev)
                            .cost(cost)
                            .profit(profit)
                            .margin(margin)
                            .orderCount(entry.getValue()[2].intValue())
                            .build();
                })
                .sorted(Comparator.comparing(ProfitabilityResponse.DailyProfitability::getDate))
                .collect(Collectors.toList());

        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        double grossMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        // Calculate net profit (matching DashboardStatsService formula)
        // Net Profit = Gross Profit - Commission - Shipping - Stoppage
        // Note: This is a simplified calculation using order-level data.
        // For comprehensive analysis including platform fees, expenses, invoiced deductions,
        // and advertising costs, use the Dashboard Stats API.
        BigDecimal netProfit = grossProfit
                .subtract(totalCommission)
                .subtract(totalShippingCost)
                .subtract(totalStoppage);

        log.info("Profitability analysis for store {} from {} to {}: revenue={}, cost={}, grossProfit={}, " +
                        "commission={}, shipping={}, stoppage={}, netProfit={}",
                storeId, startDate, endDate, totalRevenue, totalCost, grossProfit,
                totalCommission, totalShippingCost, totalStoppage, netProfit);

        return ProfitabilityResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(totalRevenue)
                .totalCost(totalCost)
                .grossProfit(grossProfit)
                .grossMargin(grossMargin)
                // Net profit and expenses
                .netProfit(netProfit)
                .totalCommission(totalCommission)
                .totalShippingCost(totalShippingCost)
                .totalStoppage(totalStoppage)
                .totalReturnCost(BigDecimal.ZERO) // Not tracked in this report
                .totalExpenses(BigDecimal.ZERO) // Not tracked in this report
                .totalPlatformFees(BigDecimal.ZERO) // Not tracked in this report
                .totalInvoicedDeductions(BigDecimal.ZERO) // Not tracked in this report
                .totalAdvertisingCost(BigDecimal.ZERO) // Not tracked in this report
                // Summary
                .totalOrders(orders.size())
                .totalQuantitySold(totalQuantitySold)
                .topProfitable(topProfitable)
                .leastProfitable(leastProfitable)
                .dailyTrend(dailyTrend)
                .build();
    }

    /**
     * Get purchase summary for a date range
     */
    @Transactional(readOnly = true)
    public PurchaseSummaryResponse getPurchaseSummary(UUID storeId, LocalDate startDate, LocalDate endDate) {
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository
                .findByStoreIdAndPoDateBetweenAndStatus(storeId, startDate, endDate, PurchaseOrderStatus.CLOSED);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalUnits = 0;
        Map<String, BigDecimal[]> supplierMap = new HashMap<>(); // [amount, orderCount, units]
        Map<String, BigDecimal[]> productMap = new HashMap<>(); // [units, amount, firstCost, lastCost]
        Map<String, BigDecimal[]> monthlyMap = new HashMap<>(); // [amount, units, orderCount]

        for (PurchaseOrder po : purchaseOrders) {
            totalAmount = totalAmount.add(po.getTotalCost() != null ? po.getTotalCost() : BigDecimal.ZERO);
            totalUnits += po.getTotalUnits() != null ? po.getTotalUnits() : 0;

            // Supplier breakdown
            String supplier = po.getSupplierName() != null ? po.getSupplierName() : "Bilinmiyor";
            BigDecimal[] supplierData = supplierMap.computeIfAbsent(supplier, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            supplierData[0] = supplierData[0].add(po.getTotalCost() != null ? po.getTotalCost() : BigDecimal.ZERO);
            supplierData[1] = supplierData[1].add(BigDecimal.ONE);
            supplierData[2] = supplierData[2].add(BigDecimal.valueOf(po.getTotalUnits() != null ? po.getTotalUnits() : 0));

            // Monthly breakdown
            String monthKey = po.getPoDate().getYear() + "-" + String.format("%02d", po.getPoDate().getMonthValue());
            BigDecimal[] monthlyData = monthlyMap.computeIfAbsent(monthKey, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            monthlyData[0] = monthlyData[0].add(po.getTotalCost() != null ? po.getTotalCost() : BigDecimal.ZERO);
            monthlyData[1] = monthlyData[1].add(BigDecimal.valueOf(po.getTotalUnits() != null ? po.getTotalUnits() : 0));
            monthlyData[2] = monthlyData[2].add(BigDecimal.ONE);

            // Product breakdown
            for (PurchaseOrderItem item : po.getItems()) {
                String barcode = item.getProduct().getBarcode();
                BigDecimal costPerUnit = item.getManufacturingCostPerUnit()
                        .add(item.getTransportationCostPerUnit() != null ? item.getTransportationCostPerUnit() : BigDecimal.ZERO);

                BigDecimal[] productData = productMap.computeIfAbsent(barcode, k -> new BigDecimal[]{
                        BigDecimal.ZERO, BigDecimal.ZERO, costPerUnit, costPerUnit
                });
                productData[0] = productData[0].add(BigDecimal.valueOf(item.getUnitsOrdered()));
                productData[1] = productData[1].add(item.getTotalCost());
                productData[3] = costPerUnit; // Update last cost
            }
        }

        // Final copy for use in lambdas
        final BigDecimal finalTotalAmount = totalAmount;

        // Build supplier breakdown
        List<PurchaseSummaryResponse.SupplierBreakdown> supplierBreakdown = supplierMap.entrySet().stream()
                .map(entry -> PurchaseSummaryResponse.SupplierBreakdown.builder()
                        .supplierName(entry.getKey())
                        .totalAmount(entry.getValue()[0])
                        .orderCount(entry.getValue()[1].intValue())
                        .totalUnits(entry.getValue()[2].intValue())
                        .percentage(finalTotalAmount.compareTo(BigDecimal.ZERO) > 0
                                ? entry.getValue()[0].divide(finalTotalAmount, 4, RoundingMode.HALF_UP).doubleValue() * 100
                                : 0)
                        .build())
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());

        // Build top products
        List<PurchaseSummaryResponse.ProductPurchase> topProducts = productMap.entrySet().stream()
                .map(entry -> {
                    BigDecimal firstCost = entry.getValue()[2];
                    BigDecimal lastCost = entry.getValue()[3];
                    BigDecimal costChange = firstCost.compareTo(BigDecimal.ZERO) > 0
                            ? lastCost.subtract(firstCost).divide(firstCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    TrendyolProduct product = productRepository.findByStoreIdAndBarcode(storeId, entry.getKey()).orElse(null);

                    return PurchaseSummaryResponse.ProductPurchase.builder()
                            .productId(product != null ? product.getId() : null)
                            .productName(product != null ? product.getTitle() : entry.getKey())
                            .barcode(entry.getKey())
                            .productImage(product != null ? product.getImage() : null)
                            .totalUnits(entry.getValue()[0].intValue())
                            .totalAmount(entry.getValue()[1])
                            .averageCost(entry.getValue()[0].compareTo(BigDecimal.ZERO) > 0
                                    ? entry.getValue()[1].divide(entry.getValue()[0], 2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO)
                            .costChange(costChange)
                            .build();
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .limit(10)
                .collect(Collectors.toList());

        // Build monthly trend
        String[] monthNames = {"", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
                               "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"};
        List<PurchaseSummaryResponse.MonthlyPurchase> monthlyTrend = monthlyMap.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    return PurchaseSummaryResponse.MonthlyPurchase.builder()
                            .year(year)
                            .month(month)
                            .monthName(monthNames[month] + " " + year)
                            .totalAmount(entry.getValue()[0])
                            .totalUnits(entry.getValue()[1].intValue())
                            .orderCount(entry.getValue()[2].intValue())
                            .build();
                })
                .sorted(Comparator.comparing((PurchaseSummaryResponse.MonthlyPurchase m) -> m.getYear())
                        .thenComparing(PurchaseSummaryResponse.MonthlyPurchase::getMonth))
                .collect(Collectors.toList());

        BigDecimal averageCost = totalUnits > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalUnits), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PurchaseSummaryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalPurchaseAmount(totalAmount)
                .totalUnits(totalUnits)
                .totalOrders(purchaseOrders.size())
                .averageCostPerUnit(averageCost)
                .supplierBreakdown(supplierBreakdown)
                .topProductsByAmount(topProducts)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    /**
     * Find purchase order items by product, keyed by effective stock entry date.
     * Uses item-level stockEntryDate → PO-level stockEntryDate → poDate fallback.
     */
    private Map<LocalDate, PurchaseOrderItem> findPurchaseOrderItemsByProduct(UUID storeId, UUID productId) {
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByProductIdAndStoreId(productId, storeId);
        return items.stream()
                .filter(item -> item.getPurchaseOrder().getStatus() == PurchaseOrderStatus.CLOSED)
                .collect(Collectors.toMap(
                        this::getEffectiveStockEntryDate,
                        item -> item,
                        (existing, replacement) -> existing // Keep first if duplicate dates
                ));
    }

    /**
     * Get effective stock entry date for a PO item (same logic as PurchaseOrderService).
     */
    private LocalDate getEffectiveStockEntryDate(PurchaseOrderItem item) {
        if (item.getStockEntryDate() != null) {
            return item.getStockEntryDate();
        }
        PurchaseOrder po = item.getPurchaseOrder();
        if (po.getStockEntryDate() != null) {
            return po.getStockEntryDate();
        }
        return po.getPoDate();
    }
}
