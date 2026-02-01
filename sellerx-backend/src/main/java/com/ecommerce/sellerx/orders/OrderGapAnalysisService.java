package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.financial.TrendyolFinancialSettlementService;
import com.ecommerce.sellerx.stores.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for analyzing gaps between Settlement API data and Orders API data.
 *
 * Settlement API provides commission data but has a 3-7 day delay (transactionDate).
 * Orders API provides real-time data but without commission.
 *
 * Gap Period:
 * - Settlement covers: Historical data up to T-7 days (approximately)
 * - Orders API covers: Real-time data including last 7 days
 * - Gap: The period where Settlement hasn't arrived yet but Orders API has data
 *
 * This service identifies:
 * 1. What date range needs to be filled from Orders API
 * 2. Which orders are missing commission data
 * 3. When reconciliation should be attempted
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderGapAnalysisService {

    private final TrendyolOrderRepository orderRepository;

    /**
     * Default settlement delay in days.
     * Trendyol typically settles orders 3-7 days after the transaction.
     */
    private static final int SETTLEMENT_DELAY_DAYS = 7;

    /**
     * Extra buffer days for safety margin.
     */
    private static final int BUFFER_DAYS = 1;

    /**
     * Analyze the gap period for a store.
     * Returns information about what period needs to be synced from Orders API.
     *
     * @param storeId The store to analyze
     * @return GapAnalysis with details about the gap period
     */
    public GapAnalysis analyzeGap(UUID storeId) {
        // Find the latest settlement date for this store
        LocalDate latestSettlementOrderDate = findLatestSettlementOrderDate(storeId);

        // Calculate gap boundaries
        LocalDate gapStartDate;
        if (latestSettlementOrderDate != null) {
            // Start from the day after the latest settlement order
            gapStartDate = latestSettlementOrderDate.plusDays(1);
        } else {
            // No settlement data - start from SETTLEMENT_DELAY_DAYS ago
            gapStartDate = LocalDate.now().minusDays(SETTLEMENT_DELAY_DAYS + BUFFER_DAYS);
        }

        LocalDate gapEndDate = LocalDate.now();

        // Count orders in different states
        int ordersNeedingCommission = countOrdersNeedingCommission(storeId);
        int ordersFromOrdersApi = countOrdersByDataSource(storeId, "ORDER_API");
        int ordersFromSettlementApi = countOrdersByDataSource(storeId, "SETTLEMENT_API");
        int hybridOrders = countOrdersByDataSource(storeId, "HYBRID");

        GapAnalysis analysis = GapAnalysis.builder()
                .storeId(storeId)
                .latestSettlementOrderDate(latestSettlementOrderDate)
                .gapStartDate(gapStartDate)
                .gapEndDate(gapEndDate)
                .gapDays((int) java.time.temporal.ChronoUnit.DAYS.between(gapStartDate, gapEndDate))
                .ordersNeedingCommission(ordersNeedingCommission)
                .ordersFromOrdersApi(ordersFromOrdersApi)
                .ordersFromSettlementApi(ordersFromSettlementApi)
                .hybridOrders(hybridOrders)
                .analysisTime(LocalDateTime.now())
                .build();

        log.info("Gap analysis for store {}: gap={} to {} ({} days), " +
                        "needingCommission={}, orderApi={}, settlementApi={}, hybrid={}",
                storeId, gapStartDate, gapEndDate, analysis.getGapDays(),
                ordersNeedingCommission, ordersFromOrdersApi, ordersFromSettlementApi, hybridOrders);

        return analysis;
    }

    /**
     * Find orders that need commission data (estimated commission, not reconciled yet).
     *
     * @param storeId The store ID
     * @return List of orders needing commission reconciliation
     */
    public List<TrendyolOrder> findOrdersNeedingReconciliation(UUID storeId) {
        return orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(storeId);
    }

    /**
     * Find orders from Orders API within a date range that haven't been reconciled.
     *
     * @param storeId The store ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of orders needing reconciliation in the date range
     */
    public List<TrendyolOrder> findOrdersNeedingReconciliationInRange(UUID storeId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        return orderRepository.findByStoreIdAndOrderDateBetweenAndIsCommissionEstimatedTrue(
                storeId, startDateTime, endDateTime);
    }

    /**
     * Find the latest order date from Settlement API data for a store.
     * This represents the "reliable" data boundary.
     *
     * @param storeId The store ID
     * @return Latest order date from Settlement API, or null if no settlement data
     */
    public LocalDate findLatestSettlementOrderDate(UUID storeId) {
        return orderRepository.findLatestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API");
    }

    /**
     * Find the earliest order date from Settlement API data for a store.
     *
     * @param storeId The store ID
     * @return Earliest order date from Settlement API, or null if no settlement data
     */
    public LocalDate findEarliestSettlementOrderDate(UUID storeId) {
        return orderRepository.findEarliestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API");
    }

    /**
     * Check if a specific order has been reconciled (received real commission from Settlement).
     *
     * @param order The order to check
     * @return true if the order has real commission data
     */
    public boolean isOrderReconciled(TrendyolOrder order) {
        return Boolean.FALSE.equals(order.getIsCommissionEstimated())
                || "SETTLEMENT_API".equals(order.getDataSource())
                || "HYBRID".equals(order.getDataSource());
    }

    /**
     * Get recommended sync period for Orders API.
     * Returns the date range that should be synced from Orders API.
     *
     * @param storeId The store ID
     * @return SyncRecommendation with start and end dates
     */
    public SyncRecommendation getOrdersApiSyncRecommendation(UUID storeId) {
        GapAnalysis analysis = analyzeGap(storeId);

        // For Orders API sync, we want to cover the gap period plus a buffer
        LocalDate recommendedStart = analysis.getGapStartDate().minusDays(BUFFER_DAYS);
        LocalDate recommendedEnd = analysis.getGapEndDate();

        // Don't go back more than 90 days (Orders API typical limit)
        LocalDate maxHistoricalDate = LocalDate.now().minusDays(90);
        if (recommendedStart.isBefore(maxHistoricalDate)) {
            recommendedStart = maxHistoricalDate;
        }

        return SyncRecommendation.builder()
                .startDate(recommendedStart)
                .endDate(recommendedEnd)
                .reason("Gap fill from Orders API")
                .priority(analysis.getOrdersNeedingCommission() > 0 ? "HIGH" : "NORMAL")
                .estimatedOrders(analysis.getOrdersNeedingCommission())
                .build();
    }

    /**
     * Count orders needing commission reconciliation.
     */
    private int countOrdersNeedingCommission(UUID storeId) {
        return orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId);
    }

    /**
     * Count orders by data source.
     */
    private int countOrdersByDataSource(UUID storeId, String dataSource) {
        return orderRepository.countByStoreIdAndDataSource(storeId, dataSource);
    }

    /**
     * Data class for gap analysis results.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class GapAnalysis {
        private UUID storeId;
        private LocalDate latestSettlementOrderDate;
        private LocalDate gapStartDate;
        private LocalDate gapEndDate;
        private int gapDays;
        private int ordersNeedingCommission;
        private int ordersFromOrdersApi;
        private int ordersFromSettlementApi;
        private int hybridOrders;
        private LocalDateTime analysisTime;
    }

    /**
     * Data class for sync recommendations.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SyncRecommendation {
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
        private String priority;
        private int estimatedOrders;
    }
}
