package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for reconciling estimated commissions with real commission data from Settlement API.
 *
 * Reconciliation Process:
 * 1. Find orders with isCommissionEstimated = true (came from Orders API)
 * 2. Check if Settlement API now has data for these orders
 * 3. If found, update the order with real commission and mark as reconciled
 * 4. Track the difference for accuracy monitoring
 *
 * DataSource Transitions:
 * - ORDER_API (estimated) → HYBRID (reconciled with real commission)
 * - The order keeps operational data from Orders API but gets real commission from Settlement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionReconciliationService {

    private final TrendyolOrderRepository orderRepository;
    private final CommissionReconciliationLogRepository reconciliationLogRepository;
    private final StoreRepository storeRepository;

    /**
     * Run reconciliation for a specific store.
     * Matches orders with estimated commission against Settlement data.
     *
     * @param storeId The store to reconcile
     * @return ReconciliationResult with statistics
     */
    @Transactional
    public ReconciliationResult reconcileStore(UUID storeId) {
        log.info("Starting commission reconciliation for store {}", storeId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        // Find all orders with estimated commission
        List<TrendyolOrder> estimatedOrders = orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(storeId);

        if (estimatedOrders.isEmpty()) {
            log.info("No orders with estimated commission found for store {}", storeId);
            return ReconciliationResult.empty(storeId);
        }

        log.info("Found {} orders with estimated commission for store {}", estimatedOrders.size(), storeId);

        int reconciled = 0;
        int notYetSettled = 0;
        BigDecimal totalEstimated = BigDecimal.ZERO;
        BigDecimal totalReal = BigDecimal.ZERO;
        BigDecimal totalDifference = BigDecimal.ZERO;

        for (TrendyolOrder order : estimatedOrders) {
            // Try to find real commission from financial transactions
            BigDecimal realCommission = extractRealCommissionFromOrder(order);

            if (realCommission != null) {
                // Real commission found - reconcile
                BigDecimal estimated = order.getEstimatedCommission() != null
                        ? order.getEstimatedCommission()
                        : BigDecimal.ZERO;

                BigDecimal difference = realCommission.subtract(estimated);

                // Update order
                order.setEstimatedCommission(realCommission); // Now it's the real value
                order.setIsCommissionEstimated(false);
                order.setDataSource("HYBRID");
                order.setCommissionDifference(difference);
                orderRepository.save(order);

                // Update totals
                totalEstimated = totalEstimated.add(estimated);
                totalReal = totalReal.add(realCommission);
                totalDifference = totalDifference.add(difference);
                reconciled++;

                log.debug("Reconciled order {}: estimated={}, real={}, diff={}",
                        order.getTyOrderNumber(), estimated, realCommission, difference);
            } else {
                notYetSettled++;
            }
        }

        // Create reconciliation log entry
        if (reconciled > 0) {
            CommissionReconciliationLog logEntry = createReconciliationLog(
                    store, reconciled, totalEstimated, totalReal, totalDifference);
            reconciliationLogRepository.save(logEntry);
        }

        ReconciliationResult result = ReconciliationResult.builder()
                .storeId(storeId)
                .totalProcessed(estimatedOrders.size())
                .reconciled(reconciled)
                .notYetSettled(notYetSettled)
                .totalEstimated(totalEstimated)
                .totalReal(totalReal)
                .totalDifference(totalDifference)
                .reconciliationDate(LocalDate.now())
                .build();

        log.info("Reconciliation complete for store {}: reconciled={}, notYetSettled={}, " +
                        "totalEstimated={}, totalReal={}, difference={}",
                storeId, reconciled, notYetSettled, totalEstimated, totalReal, totalDifference);

        return result;
    }

    /**
     * Run reconciliation for all stores.
     */
    @Transactional
    public void reconcileAllStores() {
        log.info("Starting commission reconciliation for all stores");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();

        for (Store store : stores) {
            try {
                reconcileStore(store.getId());
            } catch (Exception e) {
                log.error("Error reconciling store {}: {}", store.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed commission reconciliation for {} stores", stores.size());
    }

    /**
     * Reconcile a single order when Settlement data arrives.
     * Called by TrendyolFinancialSettlementService when processing settlements.
     *
     * @param order The order to reconcile
     * @param realCommission The real commission from Settlement API
     */
    @Transactional
    public void reconcileOrder(TrendyolOrder order, BigDecimal realCommission) {
        if (!Boolean.TRUE.equals(order.getIsCommissionEstimated())) {
            // Already reconciled or from Settlement API originally
            return;
        }

        BigDecimal estimated = order.getEstimatedCommission() != null
                ? order.getEstimatedCommission()
                : BigDecimal.ZERO;

        BigDecimal difference = realCommission.subtract(estimated);

        order.setEstimatedCommission(realCommission);
        order.setIsCommissionEstimated(false);
        order.setDataSource("HYBRID");
        order.setCommissionDifference(difference);

        log.info("Reconciled order {}: estimated={} → real={}, diff={}",
                order.getTyOrderNumber(), estimated, realCommission, difference);
    }

    /**
     * Extract real commission from an order's financial transactions.
     * This data is populated when Settlement API data is synced.
     *
     * @param order The order to check
     * @return Real commission amount, or null if not yet available
     */
    private BigDecimal extractRealCommissionFromOrder(TrendyolOrder order) {
        if (order.getFinancialTransactions() == null || order.getFinancialTransactions().isEmpty()) {
            return null;
        }

        // Sum commission from all financial transaction items
        BigDecimal totalCommission = BigDecimal.ZERO;
        boolean hasCommissionData = false;

        for (FinancialOrderItemData item : order.getFinancialTransactions()) {
            if (item.getTransactions() != null) {
                for (FinancialSettlement transaction : item.getTransactions()) {
                    // Commission is stored in commissionAmount field from Settlement API
                    if (transaction.getCommissionAmount() != null) {
                        totalCommission = totalCommission.add(transaction.getCommissionAmount());
                        hasCommissionData = true;
                    }
                }
            }
        }

        return hasCommissionData ? totalCommission : null;
    }

    /**
     * Create a reconciliation log entry.
     */
    private CommissionReconciliationLog createReconciliationLog(Store store,
                                                                 int totalReconciled,
                                                                 BigDecimal totalEstimated,
                                                                 BigDecimal totalReal,
                                                                 BigDecimal totalDifference) {
        CommissionReconciliationLog logEntry = CommissionReconciliationLog.builder()
                .store(store)
                .reconciliationDate(LocalDate.now())
                .totalReconciled(totalReconciled)
                .totalEstimated(totalEstimated)
                .totalReal(totalReal)
                .totalDifference(totalDifference)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Calculate accuracy
        logEntry.calculateAccuracy();

        return logEntry;
    }

    /**
     * Get reconciliation history for a store.
     *
     * @param storeId The store ID
     * @return List of reconciliation logs ordered by date descending
     */
    public List<CommissionReconciliationLog> getReconciliationHistory(UUID storeId) {
        return reconciliationLogRepository.findByStoreIdOrderByReconciliationDateDesc(storeId);
    }

    /**
     * Get reconciliation summary statistics for a store.
     *
     * @param storeId The store ID
     * @return Summary statistics
     */
    public ReconciliationSummary getReconciliationSummary(UUID storeId) {
        Integer totalReconciled = reconciliationLogRepository.sumTotalReconciledByStoreId(storeId);
        BigDecimal totalDifference = reconciliationLogRepository.sumTotalDifferenceByStoreId(storeId);
        BigDecimal avgAccuracy = reconciliationLogRepository.avgAccuracyByStoreId(storeId);
        LocalDate latestDate = reconciliationLogRepository.findLatestReconciliationDateByStoreId(storeId);

        int pendingCount = orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId);

        return ReconciliationSummary.builder()
                .storeId(storeId)
                .totalReconciled(totalReconciled != null ? totalReconciled : 0)
                .totalDifference(totalDifference != null ? totalDifference : BigDecimal.ZERO)
                .averageAccuracy(avgAccuracy)
                .latestReconciliationDate(latestDate)
                .pendingReconciliation(pendingCount)
                .build();
    }

    /**
     * Result of a reconciliation run.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ReconciliationResult {
        private UUID storeId;
        private int totalProcessed;
        private int reconciled;
        private int notYetSettled;
        private BigDecimal totalEstimated;
        private BigDecimal totalReal;
        private BigDecimal totalDifference;
        private LocalDate reconciliationDate;

        public static ReconciliationResult empty(UUID storeId) {
            return ReconciliationResult.builder()
                    .storeId(storeId)
                    .totalProcessed(0)
                    .reconciled(0)
                    .notYetSettled(0)
                    .totalEstimated(BigDecimal.ZERO)
                    .totalReal(BigDecimal.ZERO)
                    .totalDifference(BigDecimal.ZERO)
                    .reconciliationDate(LocalDate.now())
                    .build();
        }
    }

    /**
     * Summary statistics for reconciliation.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ReconciliationSummary {
        private UUID storeId;
        private int totalReconciled;
        private BigDecimal totalDifference;
        private BigDecimal averageAccuracy;
        private LocalDate latestReconciliationDate;
        private int pendingReconciliation;
    }
}
