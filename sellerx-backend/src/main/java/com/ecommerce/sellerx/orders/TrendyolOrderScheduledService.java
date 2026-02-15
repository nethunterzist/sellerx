package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.ecommerce.sellerx.products.TrendyolProductService;
import com.ecommerce.sellerx.qa.TrendyolQaService;
import com.ecommerce.sellerx.returns.TrendyolClaimsService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for Trendyol data synchronization.
 *
 * Sync Strategy:
 * 1. Daily full sync at 6:15 AM - Orders, Products, Q&A, Returns
 * 2. Hourly catch-up sync - Orders (last 2 hours), Products, Q&A, Returns
 *
 * Supports two modes:
 * - Sequential: Original mode, processes stores one by one (for small deployments)
 * - Parallel: Uses ParallelStoreSyncService for 1500+ stores scaling
 *
 * Uses TrendyolRateLimiter to respect API rate limits (10 req/sec).
 */
@Service
@Slf4j
public class TrendyolOrderScheduledService {

    private final TrendyolOrderService orderService;
    private final TrendyolProductService productService;
    private final TrendyolQaService qaService;
    private final TrendyolClaimsService claimsService;
    private final StoreRepository storeRepository;
    private final TrendyolRateLimiter rateLimiter;
    private final ResilientSyncService resilientSyncService;

    @Value("${sellerx.sync.parallel.enabled:true}")
    private boolean parallelSyncEnabled;

    private final Timer syncDurationTimer;
    private final Counter syncSuccessCounter;
    private final Counter syncFailureCounter;

    public TrendyolOrderScheduledService(
            TrendyolOrderService orderService,
            TrendyolProductService productService,
            TrendyolQaService qaService,
            TrendyolClaimsService claimsService,
            StoreRepository storeRepository,
            TrendyolRateLimiter rateLimiter,
            ResilientSyncService resilientSyncService,
            MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.productService = productService;
        this.qaService = qaService;
        this.claimsService = claimsService;
        this.storeRepository = storeRepository;
        this.rateLimiter = rateLimiter;
        this.resilientSyncService = resilientSyncService;

        this.syncDurationTimer = Timer.builder("sellerx.orders.sync.duration")
                .description("Duration of scheduled order sync operations")
                .register(meterRegistry);
        this.syncSuccessCounter = Counter.builder("sellerx.orders.sync")
                .tag("result", "success")
                .description("Number of successful order sync operations")
                .register(meterRegistry);
        this.syncFailureCounter = Counter.builder("sellerx.orders.sync")
                .tag("result", "failure")
                .description("Number of failed order sync operations")
                .register(meterRegistry);
    }

    /**
     * Daily full sync - runs at 6:15 AM Turkey time.
     * Syncs all data from Trendyol: Orders, Products, Q&A, Returns (Claims).
     *
     * Uses parallel sync if enabled (recommended for 100+ stores).
     */
    @Scheduled(cron = "0 15 6 * * ?", zone = "Europe/Istanbul")
    @SchedulerLock(name = "syncAllDataForAllTrendyolStores", lockAtLeastFor = "5m", lockAtMostFor = "60m")
    public void syncAllDataForAllTrendyolStores() {
        log.info("Starting scheduled daily full sync for all Trendyol stores (parallel={})", parallelSyncEnabled);

        if (parallelSyncEnabled) {
            syncDurationTimer.record(() -> {
                var result = resilientSyncService.syncAllWithResilience(false);
                log.info("Parallel full sync completed: {} success, {} failed",
                        result.successCount(), result.failureCount());
                syncSuccessCounter.increment(result.successCount());
                syncFailureCounter.increment(result.failureCount());
            });
        } else {
            syncDurationTimer.record(() -> syncStoresWithRateLimit(false));
        }
    }

    /**
     * Hourly catch-up sync - runs every hour at minute 0.
     * Syncs all data to catch any updates missed by webhooks.
     *
     * This provides resilience: if webhooks fail, polling catches up.
     * Uses parallel sync if enabled (recommended for 100+ stores).
     */
    @Scheduled(cron = "0 0 * * * ?", zone = "Europe/Istanbul")
    @SchedulerLock(name = "catchUpSync", lockAtLeastFor = "2m", lockAtMostFor = "30m")
    public void catchUpSync() {
        log.info("Starting hourly catch-up sync for all Trendyol stores (parallel={})", parallelSyncEnabled);

        if (parallelSyncEnabled) {
            syncDurationTimer.record(() -> {
                var result = resilientSyncService.syncAllWithResilience(true);
                log.info("Parallel catch-up sync completed: {} success, {} failed",
                        result.successCount(), result.failureCount());
                syncSuccessCounter.increment(result.successCount());
                syncFailureCounter.increment(result.failureCount());
            });
        } else {
            syncDurationTimer.record(() -> syncStoresWithRateLimit(true));
        }
    }

    /**
     * Syncs all Trendyol data for all stores with rate limiting.
     * Includes: Orders, Products, Q&A (Customer Questions), Returns (Claims)
     *
     * @param catchUpOnly If true, only sync last 2 hours for orders. If false, full sync.
     */
    private void syncStoresWithRateLimit(boolean catchUpOnly) {
        try {
            List<Store> trendyolStores = storeRepository.findByMarketplaceIgnoreCase("trendyol");
            log.info("Found {} Trendyol stores for {} sync",
                    trendyolStores.size(), catchUpOnly ? "catch-up" : "full");

            int orderSuccessCount = 0;
            int productSuccessCount = 0;
            int qaSuccessCount = 0;
            int claimsSuccessCount = 0;
            int errorCount = 0;

            for (Store store : trendyolStores) {
                // Skip stores that haven't completed initial sync
                if (!Boolean.TRUE.equals(store.getInitialSyncCompleted())) {
                    log.debug("Skipping store {} - initial sync not completed", store.getId());
                    continue;
                }

                // 1. Sync Orders
                rateLimiter.acquire(store.getId());
                try {
                    if (catchUpOnly) {
                        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
                        log.debug("Catch-up order sync for store {} from {}", store.getId(), startTime);
                        orderService.fetchAndSaveOrdersForStoreInRange(store.getId(), startTime, LocalDateTime.now());
                    } else {
                        log.debug("Full order sync for store {}", store.getId());
                        orderService.fetchAndSaveOrdersForStore(store.getId());
                    }
                    orderSuccessCount++;
                    syncSuccessCounter.increment();
                } catch (Exception e) {
                    errorCount++;
                    syncFailureCounter.increment();
                    log.error("Failed to sync orders for store {} ({}): {}",
                            store.getStoreName(), store.getId(), e.getMessage());
                }

                // 2. Sync Products
                rateLimiter.acquire(store.getId());
                try {
                    log.debug("Syncing products for store {}", store.getId());
                    var productResult = productService.syncProductsFromTrendyol(store.getId());
                    if (productResult.isSuccess()) {
                        productSuccessCount++;
                        log.debug("Product sync for store {}: {} fetched, {} new, {} updated",
                                store.getId(), productResult.getTotalFetched(),
                                productResult.getTotalSaved(), productResult.getTotalUpdated());
                    } else {
                        log.warn("Product sync failed for store {}: {}", store.getId(), productResult.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Failed to sync products for store {} ({}): {}",
                            store.getStoreName(), store.getId(), e.getMessage());
                }

                // 3. Sync Q&A (Customer Questions)
                rateLimiter.acquire(store.getId());
                try {
                    log.debug("Syncing Q&A for store {}", store.getId());
                    var qaResult = qaService.syncQuestions(store.getId());
                    qaSuccessCount++;
                    log.debug("Q&A sync for store {}: {} fetched, {} new, {} updated",
                            store.getId(), qaResult.getTotalFetched(),
                            qaResult.getNewQuestions(), qaResult.getUpdatedQuestions());
                } catch (Exception e) {
                    log.error("Failed to sync Q&A for store {} ({}): {}",
                            store.getStoreName(), store.getId(), e.getMessage());
                }

                // 4. Sync Returns/Claims
                rateLimiter.acquire(store.getId());
                try {
                    log.debug("Syncing claims/returns for store {}", store.getId());
                    var claimsResult = claimsService.syncClaims(store.getId());
                    claimsSuccessCount++;
                    log.debug("Claims sync for store {}: {} fetched, {} new, {} updated",
                            store.getId(), claimsResult.getTotalFetched(),
                            claimsResult.getNewClaims(), claimsResult.getUpdatedClaims());
                } catch (Exception e) {
                    log.error("Failed to sync claims for store {} ({}): {}",
                            store.getStoreName(), store.getId(), e.getMessage());
                }
            }

            log.info("Completed {} sync - Orders: {}, Products: {}, Q&A: {}, Returns: {}, Errors: {}",
                    catchUpOnly ? "catch-up" : "daily",
                    orderSuccessCount, productSuccessCount, qaSuccessCount, claimsSuccessCount, errorCount);

        } catch (Exception e) {
            syncFailureCounter.increment();
            log.error("Error during scheduled sync: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual sync for all Trendyol stores (can be called via endpoint)
     * Syncs all data: Orders, Products, Q&A, Returns.
     *
     * @return SyncResult with success/failure counts (only when parallel sync enabled)
     */
    public ParallelStoreSyncService.SyncResult manualSyncAllStores() {
        log.info("Starting manual full sync for all Trendyol stores (parallel={})", parallelSyncEnabled);

        if (parallelSyncEnabled) {
            var result = resilientSyncService.syncAllWithResilience(false);
            log.info("Manual parallel sync completed: {} success, {} failed",
                    result.successCount(), result.failureCount());
            return result;
        } else {
            syncStoresWithRateLimit(false);
            return new ParallelStoreSyncService.SyncResult(0, 0, 0); // Legacy mode doesn't track counts
        }
    }

    /**
     * Check if parallel sync is enabled.
     */
    public boolean isParallelSyncEnabled() {
        return parallelSyncEnabled;
    }
}
