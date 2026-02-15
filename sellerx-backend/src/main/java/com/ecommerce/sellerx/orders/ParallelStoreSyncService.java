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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parallel store synchronization service for scaling to 1500+ stores.
 *
 * Key Features:
 * - FixedThreadPool with controlled concurrency (vs unbounded @Async)
 * - Batch processing in groups of 50 stores
 * - Per-store timeout (2 minutes) to prevent hanging
 * - Error isolation - one store failure doesn't affect others
 * - Memory efficient: ~25MB vs 1.5GB with @Async per store
 *
 * Scaling Target:
 * - 1500 stores
 * - 225M+ orders/year
 * - 10 req/sec Trendyol API limit
 */
@Service
@Slf4j
public class ParallelStoreSyncService {

    private final TrendyolOrderService orderService;
    private final TrendyolProductService productService;
    private final TrendyolQaService qaService;
    private final TrendyolClaimsService claimsService;
    private final StoreRepository storeRepository;
    private final TrendyolRateLimiter rateLimiter;

    // Metrics
    private final Timer parallelSyncTimer;
    private final Counter parallelSyncSuccess;
    private final Counter parallelSyncFailure;
    private final AtomicInteger activeStoreCount;

    // Thread pool configuration
    @Value("${sellerx.sync.parallel.threads:15}")
    private int parallelThreads;

    @Value("${sellerx.sync.batch.size:50}")
    private int batchSize;

    @Value("${sellerx.sync.store.timeout.seconds:120}")
    private int storeTimeoutSeconds;

    private ExecutorService executorService;

    public ParallelStoreSyncService(
            TrendyolOrderService orderService,
            TrendyolProductService productService,
            TrendyolQaService qaService,
            TrendyolClaimsService claimsService,
            StoreRepository storeRepository,
            TrendyolRateLimiter rateLimiter,
            MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.productService = productService;
        this.qaService = qaService;
        this.claimsService = claimsService;
        this.storeRepository = storeRepository;
        this.rateLimiter = rateLimiter;

        // Register metrics
        this.parallelSyncTimer = Timer.builder("sellerx.parallel.sync.duration")
                .description("Duration of parallel store sync operations")
                .register(meterRegistry);
        this.parallelSyncSuccess = Counter.builder("sellerx.parallel.sync")
                .tag("result", "success")
                .description("Number of successful parallel store syncs")
                .register(meterRegistry);
        this.parallelSyncFailure = Counter.builder("sellerx.parallel.sync")
                .tag("result", "failure")
                .description("Number of failed parallel store syncs")
                .register(meterRegistry);
        this.activeStoreCount = new AtomicInteger(0);

        // Register gauge for active store count
        meterRegistry.gauge("sellerx.parallel.sync.active", activeStoreCount);
    }

    @PostConstruct
    public void init() {
        // Create thread pool with bounded queue and named threads
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "parallel-sync-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        this.executorService = new ThreadPoolExecutor(
                parallelThreads,           // Core pool size
                parallelThreads,           // Max pool size (fixed)
                60L, TimeUnit.SECONDS,     // Keep alive
                new LinkedBlockingQueue<>(500),  // Bounded queue
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()  // Back-pressure
        );

        log.info("ParallelStoreSyncService initialized with {} threads, batch size {}, store timeout {}s",
                parallelThreads, batchSize, storeTimeoutSeconds);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ParallelStoreSyncService executor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sync all Trendyol stores in parallel batches.
     *
     * @param catchUpOnly If true, only sync last 2 hours for orders
     * @return SyncResult with success/failure counts
     */
    public SyncResult syncAllStoresParallel(boolean catchUpOnly) {
        return parallelSyncTimer.record(() -> {
            List<Store> stores = storeRepository.findByMarketplaceIgnoreCase("trendyol");

            // Filter to only stores with completed initial sync
            List<Store> eligibleStores = stores.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getInitialSyncCompleted()))
                    .toList();

            log.info("Starting parallel {} sync for {} stores (of {} total Trendyol stores)",
                    catchUpOnly ? "catch-up" : "full",
                    eligibleStores.size(),
                    stores.size());

            if (eligibleStores.isEmpty()) {
                return new SyncResult(0, 0, 0);
            }

            // Split into batches
            List<List<Store>> batches = partitionList(eligibleStores, batchSize);
            log.info("Processing {} batches of up to {} stores each", batches.size(), batchSize);

            AtomicInteger totalSuccess = new AtomicInteger(0);
            AtomicInteger totalFailure = new AtomicInteger(0);
            AtomicInteger totalSkipped = new AtomicInteger(0);

            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<Store> batch = batches.get(batchIndex);
                log.info("Processing batch {}/{} with {} stores",
                        batchIndex + 1, batches.size(), batch.size());

                processBatch(batch, catchUpOnly, totalSuccess, totalFailure, totalSkipped);
            }

            SyncResult result = new SyncResult(
                    totalSuccess.get(),
                    totalFailure.get(),
                    totalSkipped.get()
            );

            log.info("Parallel sync completed: {} success, {} failed, {} skipped",
                    result.successCount(), result.failureCount(), result.skippedCount());

            return result;
        });
    }

    /**
     * Process a batch of stores in parallel with timeout.
     */
    private void processBatch(
            List<Store> stores,
            boolean catchUpOnly,
            AtomicInteger totalSuccess,
            AtomicInteger totalFailure,
            AtomicInteger totalSkipped) {

        List<CompletableFuture<StoreSyncResult>> futures = new ArrayList<>();

        for (Store store : stores) {
            CompletableFuture<StoreSyncResult> future = CompletableFuture.supplyAsync(
                    () -> syncSingleStore(store, catchUpOnly),
                    executorService
            ).orTimeout(storeTimeoutSeconds, TimeUnit.SECONDS)
             .exceptionally(ex -> {
                 if (ex instanceof TimeoutException) {
                     log.error("Store {} sync timed out after {}s", store.getId(), storeTimeoutSeconds);
                 } else {
                     log.error("Store {} sync failed: {}", store.getId(), ex.getMessage());
                 }
                 return new StoreSyncResult(store.getId(), false, ex.getMessage());
             });

            futures.add(future);
        }

        // Wait for all futures in batch to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results
        for (CompletableFuture<StoreSyncResult> future : futures) {
            try {
                StoreSyncResult result = future.get();
                if (result.success()) {
                    totalSuccess.incrementAndGet();
                    parallelSyncSuccess.increment();
                } else {
                    totalFailure.incrementAndGet();
                    parallelSyncFailure.increment();
                }
            } catch (Exception e) {
                totalFailure.incrementAndGet();
                parallelSyncFailure.increment();
            }
        }
    }

    /**
     * Sync a single store with all data types.
     * Error in one type doesn't stop others.
     */
    private StoreSyncResult syncSingleStore(Store store, boolean catchUpOnly) {
        UUID storeId = store.getId();
        activeStoreCount.incrementAndGet();

        try {
            log.debug("Starting sync for store {} ({})", store.getStoreName(), storeId);

            boolean allSuccess = true;
            StringBuilder errors = new StringBuilder();

            // 1. Sync Orders
            rateLimiter.acquire(storeId);
            try {
                if (catchUpOnly) {
                    LocalDateTime startTime = LocalDateTime.now().minusHours(2);
                    orderService.fetchAndSaveOrdersForStoreInRange(storeId, startTime, LocalDateTime.now());
                } else {
                    orderService.fetchAndSaveOrdersForStore(storeId);
                }
            } catch (Exception e) {
                allSuccess = false;
                errors.append("Orders: ").append(e.getMessage()).append("; ");
                log.warn("Order sync failed for store {}: {}", storeId, e.getMessage());
            }

            // 2. Sync Products
            rateLimiter.acquire(storeId);
            try {
                var result = productService.syncProductsFromTrendyol(storeId);
                if (!result.isSuccess()) {
                    allSuccess = false;
                    errors.append("Products: ").append(result.getMessage()).append("; ");
                }
            } catch (Exception e) {
                allSuccess = false;
                errors.append("Products: ").append(e.getMessage()).append("; ");
                log.warn("Product sync failed for store {}: {}", storeId, e.getMessage());
            }

            // 3. Sync Q&A
            rateLimiter.acquire(storeId);
            try {
                qaService.syncQuestions(storeId);
            } catch (Exception e) {
                allSuccess = false;
                errors.append("Q&A: ").append(e.getMessage()).append("; ");
                log.warn("Q&A sync failed for store {}: {}", storeId, e.getMessage());
            }

            // 4. Sync Claims/Returns
            rateLimiter.acquire(storeId);
            try {
                claimsService.syncClaims(storeId);
            } catch (Exception e) {
                allSuccess = false;
                errors.append("Claims: ").append(e.getMessage()).append("; ");
                log.warn("Claims sync failed for store {}: {}", storeId, e.getMessage());
            }

            if (allSuccess) {
                log.debug("Store {} sync completed successfully", storeId);
            } else {
                log.warn("Store {} sync completed with errors: {}", storeId, errors);
            }

            return new StoreSyncResult(storeId, allSuccess, errors.toString());

        } finally {
            activeStoreCount.decrementAndGet();
        }
    }

    /**
     * Partition a list into smaller sublists.
     */
    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    // Result records
    public record SyncResult(int successCount, int failureCount, int skippedCount) {
        public int totalCount() {
            return successCount + failureCount + skippedCount;
        }

        public double successRate() {
            int processed = successCount + failureCount;
            return processed > 0 ? (double) successCount / processed * 100 : 0;
        }
    }

    public record StoreSyncResult(UUID storeId, boolean success, String errorMessage) {}
}
