package com.ecommerce.sellerx.config;

import com.ecommerce.sellerx.financial.CommissionReconciliationService;
import com.ecommerce.sellerx.financial.TrendyolFinancialSettlementService;
import com.ecommerce.sellerx.financial.TrendyolOtherFinancialsService;
import com.ecommerce.sellerx.orders.OrderGapAnalysisService;
import com.ecommerce.sellerx.orders.TrendyolOrderService;
import com.ecommerce.sellerx.products.BuyboxService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
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
 * - 07:15: Cargo Shipping sync (last 30 days - cargo invoice details & shipping costs)
 * - 07:30: Product commission cache update (updates lastCommissionRate from settlements)
 * - 07:45: Payment Order sync (fetches last 90 days of payment orders for hakedis)
 * - 08:00: Reconciliation job (matches ORDER_API orders with Settlement data)
 * - 08:15: Deduction invoice sync (fetches last 30 days of deduction & return invoices)
 * - 08:30: Stoppage sync (fetches last 90 days of stoppages/tevkifat)
 * - Every hour @:30: Gap fill from Orders API (fetches recent orders with estimated commission)
 * - Every hour @:45: Deduction invoice catch-up (fetches last 3 days of deduction & return invoices)
 * - Every 6 hours @:00: Gap analysis status logging
 * - Every 6 hours @:30: Extended order catch-up (7-day window for status transitions)
 *
 * Weekly Schedule:
 * - Monday 03:00: Cargo invoice catch-up (last 90 days - catches delayed Trendyol invoices)
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
    private final BuyboxService buyboxService;

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
                // Sync last 30 days of cargo invoices and update orders
                // Trendyol issues cargo invoices with 1-4 week delays, so 30 days covers typical delays
                java.time.LocalDate endDate = java.time.LocalDate.now();
                java.time.LocalDate startDate = endDate.minusDays(30);

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
     * Weekly cargo invoice catch-up at 03:00 every Monday (Turkey time).
     * Fetches last 90 days of cargo invoices to catch delayed Trendyol invoices.
     *
     * Trendyol issues cargo invoices in batches with 1-4 week delays.
     * The daily sync (30-day window) covers most cases, but this weekly job
     * ensures complete cargo invoice coverage for edge cases where invoices
     * are issued beyond the 30-day window.
     */
    @Scheduled(cron = "0 0 3 * * MON", zone = "Europe/Istanbul")
    @SchedulerLock(name = "weeklyCargoInvoiceCatchUp", lockAtLeastFor = "10m", lockAtMostFor = "120m")
    public void weeklyCargoInvoiceCatchUp() {
        log.info("Starting weekly Cargo Invoice catch-up (90-day window) for all stores");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;
        int failCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                java.time.LocalDate endDate = java.time.LocalDate.now();
                java.time.LocalDate startDate = endDate.minusDays(90);

                int syncedCount = otherFinancialsService.syncCargoInvoices(store.getId(), startDate, endDate);
                successCount++;
                log.info("Weekly cargo catch-up completed for store: {} - {} invoices synced", store.getId(), syncedCount);
            } catch (Exception e) {
                failCount++;
                log.error("Weekly cargo catch-up failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Weekly Cargo Invoice catch-up completed: {} success, {} failed", successCount, failCount);
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
     * Daily Payment Order sync at 07:45 Turkey time.
     * Fetches payment orders (hak edis) from Trendyol OtherFinancials API.
     *
     * Payment orders contain settlement details for completed orders.
     * Uses 90-day lookback (Trendyol API limit) to catch all pending settlements.
     */
    @Scheduled(cron = "0 45 7 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "dailyPaymentOrderSync", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void dailyPaymentOrderSync() {
        log.info("Starting daily Payment Order sync for all stores");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;
        int failCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(90);

                int syncedCount = otherFinancialsService.syncPaymentOrders(store.getId(), startDate, endDate);
                successCount++;
                log.debug("Payment order sync completed for store: {} - {} payment orders synced", store.getId(), syncedCount);
            } catch (Exception e) {
                failCount++;
                log.error("Payment order sync failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Daily Payment Order sync completed: {} success, {} failed", successCount, failCount);
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
     * Daily Deduction Invoice sync at 08:15 Turkey time.
     * Fetches deduction invoices (advertising fees, penalties, international fees, etc.)
     * and return invoices from Trendyol OtherFinancials API.
     *
     * Uses a 30-day lookback window because deduction invoices (especially ad fees
     * and penalties) can appear days or weeks after the triggering event.
     */
    @Scheduled(cron = "0 15 8 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "dailyDeductionInvoiceSync", lockAtLeastFor = "5m", lockAtMostFor = "60m")
    public void dailyDeductionInvoiceSync() {
        log.info("Starting daily Deduction Invoice sync for all stores");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;
        int failCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                java.time.LocalDate endDate = java.time.LocalDate.now();
                java.time.LocalDate startDate = endDate.minusDays(30);

                int deductionCount = otherFinancialsService.syncDeductionInvoices(store.getId(), startDate, endDate);
                int returnCount = otherFinancialsService.syncReturnInvoices(store.getId(), startDate, endDate);
                successCount++;
                log.debug("Deduction invoice sync completed for store: {} - {} deductions, {} returns synced",
                        store.getId(), deductionCount, returnCount);
            } catch (Exception e) {
                failCount++;
                log.error("Deduction invoice sync failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Daily Deduction Invoice sync completed: {} success, {} failed", successCount, failCount);
    }

    /**
     * Daily Stoppage (Tevkifat) sync at 08:30 Turkey time.
     * Fetches stoppage transactions from Trendyol OtherFinancials API.
     *
     * Stoppages are tax withholdings (tevkifat) applied to seller payments.
     * Uses 90-day lookback (Trendyol API limit) to ensure complete coverage.
     */
    @Scheduled(cron = "0 30 8 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "dailyStoppageSync", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void dailyStoppageSync() {
        log.info("Starting daily Stoppage sync for all stores");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;
        int failCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(90);

                int syncedCount = otherFinancialsService.syncStoppages(store.getId(), startDate, endDate);
                successCount++;
                log.debug("Stoppage sync completed for store: {} - {} stoppages synced", store.getId(), syncedCount);
            } catch (Exception e) {
                failCount++;
                log.error("Stoppage sync failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Daily Stoppage sync completed: {} success, {} failed", successCount, failCount);
    }

    /**
     * Hourly Deduction Invoice catch-up at :45 past every hour.
     * Fetches recent deduction and return invoices with a 3-day lookback window.
     *
     * This ensures newly generated invoices (e.g., ad charges, penalties)
     * are captured promptly without waiting for the daily sync.
     */
    @Scheduled(cron = "0 45 * * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "hourlyDeductionInvoiceCatchUp", lockAtLeastFor = "2m", lockAtMostFor = "30m")
    public void hourlyDeductionInvoiceCatchUp() {
        log.info("Starting hourly Deduction Invoice catch-up");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                java.time.LocalDate endDate = java.time.LocalDate.now();
                java.time.LocalDate startDate = endDate.minusDays(3);

                otherFinancialsService.syncDeductionInvoices(store.getId(), startDate, endDate);
                otherFinancialsService.syncReturnInvoices(store.getId(), startDate, endDate);
                successCount++;
                log.debug("Deduction invoice catch-up completed for store: {}", store.getId());
            } catch (Exception e) {
                log.error("Deduction invoice catch-up failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Hourly Deduction Invoice catch-up completed for {} stores", successCount);
    }


    /**
     * Extended catch-up every 6 hours at :30.
     * Scans last 7 days to catch status transitions (Shipped → Delivered).
     * Complements the hourly gap fill which only adds new orders.
     */
    @Scheduled(cron = "0 30 */6 * * *", zone = "Europe/Istanbul")
    @SchedulerLock(name = "extendedOrderCatchUp", lockAtLeastFor = "5m", lockAtMostFor = "60m")
    public void extendedOrderCatchUp() {
        log.info("Starting extended order catch-up (7-day window) for all stores");

        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;
        int failCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
                java.time.LocalDateTime startTime = endTime.minusDays(7);
                orderService.fetchAndSaveOrdersForStoreInRange(store.getId(), startTime, endTime);
                successCount++;
                log.debug("Extended catch-up completed for store: {}", store.getId());
            } catch (Exception e) {
                failCount++;
                log.error("Extended catch-up failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Extended order catch-up completed: {} success, {} failed", successCount, failCount);
    }

    /**
     * Buybox sync every 2 hours at :30 Turkey time.
     * Fetches buybox information from Trendyol for all on-sale products.
     * Updates buybox_order, buybox_price, has_multiple_seller for competitive analysis.
     */
    @Scheduled(cron = "0 30 */2 * * ?", zone = "Europe/Istanbul")
    @SchedulerLock(name = "syncBuyboxForAllStores", lockAtLeastFor = "2m", lockAtMostFor = "15m")
    public void syncBuyboxForAllStores() {
        log.info("Starting scheduled buybox sync for all stores");
        buyboxService.syncBuyboxForAllStores();
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
