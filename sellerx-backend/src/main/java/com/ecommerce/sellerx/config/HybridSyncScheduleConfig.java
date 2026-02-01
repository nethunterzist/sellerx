package com.ecommerce.sellerx.config;

import com.ecommerce.sellerx.financial.CommissionReconciliationService;
import com.ecommerce.sellerx.financial.TrendyolFinancialSettlementService;
import com.ecommerce.sellerx.financial.TrendyolOtherFinancialsService;
import com.ecommerce.sellerx.orders.OrderGapAnalysisService;
import com.ecommerce.sellerx.orders.TrendyolOrderService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Scheduled job configuration for the Hybrid Synchronization System.
 *
 * The Hybrid Sync System combines data from two sources:
 * 1. Settlement API: Historical data with REAL commission (3-7 day delay)
 * 2. Orders API: Real-time data WITHOUT commission
 *
 * Daily Schedule (Turkey time - Europe/Istanbul):
 * - 07:00: Settlement API sync (fetches last 14 days with real commission)
 * - 07:30: Product commission cache update (updates lastCommissionRate from settlements)
 * - 08:00: Reconciliation job (matches ORDER_API orders with Settlement data)
 * - Every hour: Gap fill from Orders API (fetches recent orders with estimated commission)
 *
 * Data Flow:
 * 1. Settlement sync → Real commission data arrives
 * 2. Product cache update → lastCommissionRate updated for future estimates
 * 3. Reconciliation → ORDER_API orders get updated with real commission
 * 4. Gap fill → New orders get estimated commission based on barcode reference
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class HybridSyncScheduleConfig {

    private final StoreRepository storeRepository;
    private final TrendyolFinancialSettlementService settlementService;
    private final TrendyolOtherFinancialsService otherFinancialsService;
    private final TrendyolOrderService orderService;
    private final CommissionReconciliationService reconciliationService;
    private final OrderGapAnalysisService gapAnalysisService;

    /**
     * Daily Settlement API sync at 07:00 Turkey time.
     * Fetches financial settlement data which contains REAL commission amounts.
     *
     * Settlement data typically arrives 3-7 days after the order transaction.
     * This job fetches the last 14 days to catch any delayed settlements.
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "dailySettlementSync", lockAtLeastFor = "5m", lockAtMostFor = "60m")
    public void dailySettlementSync() {
        log.info("Starting daily Settlement API sync for all stores");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;
        int failCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                settlementService.fetchAndUpdateSettlementsForStore(store.getId());
                successCount++;
                log.debug("Settlement sync completed for store: {}", store.getId());
            } catch (Exception e) {
                failCount++;
                log.error("Settlement sync failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Daily Settlement sync completed: {} success, {} failed", successCount, failCount);
    }

    /**
     * Daily Cargo Shipping sync at 07:15 Turkey time.
     * Fetches cargo invoice details and updates orders with real shipping costs.
     *
     * This job:
     * 1. Syncs cargo invoice details from Trendyol (per-order shipping costs)
     * 2. Updates orders with real shipping costs (is_shipping_estimated = false)
     */
    @Scheduled(cron = "0 15 7 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "dailyCargoShippingSync", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void dailyCargoShippingSync() {
        log.info("Starting daily Cargo Shipping sync for all stores");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;
        int failCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                // Sync last 14 days of cargo invoices and update orders
                java.time.LocalDate endDate = java.time.LocalDate.now();
                java.time.LocalDate startDate = endDate.minusDays(14);

                int syncedCount = otherFinancialsService.syncCargoInvoices(store.getId(), startDate, endDate);
                successCount++;
                log.debug("Cargo shipping sync completed for store: {} - {} invoices synced", store.getId(), syncedCount);
            } catch (Exception e) {
                failCount++;
                log.error("Cargo shipping sync failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Daily Cargo Shipping sync completed: {} success, {} failed", successCount, failCount);
    }

    /**
     * Product commission cache update at 07:30 Turkey time.
     * Updates product.lastCommissionRate based on recent settlement data.
     *
     * This ensures that new orders get accurate commission estimates
     * based on the most recent settlement data for each product.
     *
     * Note: This is handled automatically by TrendyolFinancialSettlementService
     * when processing settlements, but this job ensures cache is up-to-date.
     */
    @Scheduled(cron = "0 30 7 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "updateProductCommissionCache", lockAtLeastFor = "2m", lockAtMostFor = "15m")
    public void updateProductCommissionCache() {
        log.info("Product commission cache update is handled during settlement processing");
        // Commission rates are updated during settlement processing in TrendyolFinancialSettlementService
        // This scheduled slot can be used for additional cache maintenance if needed
    }

    /**
     * Daily reconciliation job at 08:00 Turkey time.
     * Matches orders with estimated commission against Settlement data.
     *
     * For orders that came from Orders API (isCommissionEstimated=true):
     * 1. Finds matching settlement data
     * 2. Updates estimated commission with real commission
     * 3. Sets isCommissionEstimated=false
     * 4. Changes dataSource to HYBRID
     * 5. Records the difference for accuracy tracking
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "dailyReconciliation", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void dailyReconciliation() {
        log.info("Starting daily commission reconciliation for all stores");

        try {
            reconciliationService.reconcileAllStores();
            log.info("Daily reconciliation completed");
        } catch (Exception e) {
            log.error("Daily reconciliation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Hourly gap fill from Orders API.
     * Fetches recent orders that don't have settlement data yet.
     *
     * These orders get:
     * - dataSource = ORDER_API
     * - isCommissionEstimated = true
     * - estimatedCommission = calculated using barcode reference
     *
     * When Settlement data arrives later, these orders will be reconciled
     * by the daily reconciliation job.
     */
    @Scheduled(cron = "0 30 * * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "hourlyGapFill", lockAtLeastFor = "2m", lockAtMostFor = "30m")
    public void hourlyGapFill() {
        log.info("Starting hourly gap fill from Orders API");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int totalSynced = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                orderService.syncRecentOrdersWithEstimation(store.getId());
                log.debug("Gap fill completed for store: {}", store.getId());
            } catch (Exception e) {
                log.error("Gap fill failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Hourly gap fill completed for {} stores", stores.size());
    }

    /**
     * Log gap analysis status every 6 hours (for monitoring).
     * Shows the current gap between Settlement and Orders API data.
     */
    @Scheduled(cron = "0 0 */6 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "logGapAnalysisStatus", lockAtLeastFor = "1m", lockAtMostFor = "10m")
    public void logGapAnalysisStatus() {
        log.info("=== Hybrid Sync Gap Analysis Status ===");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                OrderGapAnalysisService.GapAnalysis analysis = gapAnalysisService.analyzeGap(store.getId());
                log.info("Store {}: gap={} days, needingCommission={}, orderApi={}, settlementApi={}, hybrid={}",
                        store.getId(),
                        analysis.getGapDays(),
                        analysis.getOrdersNeedingCommission(),
                        analysis.getOrdersFromOrdersApi(),
                        analysis.getOrdersFromSettlementApi(),
                        analysis.getHybridOrders());
            } catch (Exception e) {
                log.error("Failed to analyze gap for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("=== End of Gap Analysis Status ===");
    }

    /**
     * Manual trigger for full gap sync (not scheduled).
     * Can be called via API endpoint for on-demand sync.
     *
     * @param storeId Store ID to sync
     */
    public void triggerFullGapSync(java.util.UUID storeId) {
        log.info("Manual full gap sync triggered for store: {}", storeId);

        try {
            // 1. Sync recent settlements
            settlementService.fetchAndUpdateSettlementsForStore(storeId);

            // 2. Fill gap from Orders API
            orderService.syncGapFromOrdersApi(storeId);

            // 3. Run reconciliation
            reconciliationService.reconcileStore(storeId);

            log.info("Full gap sync completed for store: {}", storeId);
        } catch (Exception e) {
            log.error("Full gap sync failed for store {}: {}", storeId, e.getMessage(), e);
            throw e;
        }
    }
}
