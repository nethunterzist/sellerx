package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.financial.HistoricalSyncResult;
import com.ecommerce.sellerx.financial.TrendyolFinancialSettlementService;
import com.ecommerce.sellerx.financial.TrendyolHistoricalSettlementService;
import com.ecommerce.sellerx.financial.TrendyolOtherFinancialsService;
import com.ecommerce.sellerx.orders.OrderCommissionRecalculationService;
import com.ecommerce.sellerx.orders.TrendyolOrderService;
import com.ecommerce.sellerx.products.TrendyolProductService;
import com.ecommerce.sellerx.qa.TrendyolQaService;
import com.ecommerce.sellerx.returns.TrendyolClaimsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for handling new store onboarding - initial data sync.
 *
 * PARALLEL SYNC ARCHITECTURE:
 *
 * PHASE 1: Products (required first - other phases depend on it)
 *          ↓
 * PHASE 2: ┌─ Thread A: Historical → Financial → Gap → Commissions (critical chain ~35min)
 *          ├─ Thread B: Returns (~3min) - can start after Products
 *          └─ Thread C: QA (~2min) - completely independent
 *
 * This reduces total sync time from ~45 minutes to ~15-20 minutes.
 *
 * Each phase updates syncPhases map for real-time progress tracking.
 * The UI can show multiple "active" phases simultaneously.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreOnboardingService {

    private final StoreRepository storeRepository;
    private final TrendyolProductService productService;
    private final TrendyolOrderService orderService;
    private final TrendyolHistoricalSettlementService historicalSettlementService;
    private final TrendyolFinancialSettlementService financialService;
    private final TrendyolOtherFinancialsService otherFinancialsService;
    private final TrendyolQaService qaService;
    private final TrendyolClaimsService claimsService;
    private final OrderCommissionRecalculationService commissionRecalculationService;

    @Qualifier("onboardingExecutor")
    private final Executor onboardingExecutor;

    // Cancel flags for sync operations (thread-safe)
    private final ConcurrentHashMap<UUID, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    // Store-level locks to avoid blocking unrelated stores
    private final ConcurrentHashMap<UUID, ReentrantLock> storeLocks = new ConcurrentHashMap<>();

    // Phase names
    private static final String PHASE_PRODUCTS = "PRODUCTS";
    private static final String PHASE_HISTORICAL = "HISTORICAL";
    private static final String PHASE_FINANCIAL = "FINANCIAL";
    private static final String PHASE_GAP = "GAP";
    private static final String PHASE_COMMISSIONS = "COMMISSIONS";
    private static final String PHASE_RETURNS = "RETURNS";
    private static final String PHASE_QA = "QA";

    /**
     * Performs initial data sync for a newly created store using PARALLEL execution.
     * This method runs asynchronously and updates syncPhases for real-time progress.
     *
     * @param store The store to sync
     */
    @Async("onboardingExecutor")
    public void performInitialSync(Store store) {
        UUID storeId = store.getId();
        Store refreshedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        store = refreshedStore;

        LocalDateTime startTime = LocalDateTime.now();

        log.info("========================================");
        log.info("[PARALLEL SYNC] Starting for store: {} ({})", store.getStoreName(), store.getId());
        log.info("========================================");

        try {
            // Initialize all phases as PENDING
            initializeSyncPhases(store);
            updateOverallStatus(store, OverallSyncStatus.IN_PROGRESS);

            // ═══════════════════════════════════════════════════════════════
            // PHASE 1: Products (REQUIRED FIRST - other phases depend on it)
            // ═══════════════════════════════════════════════════════════════
            log.info("[PHASE 1] Products sync starting...");
            updatePhaseStatus(storeId, PHASE_PRODUCTS, PhaseStatus.active());
            updateLegacySyncStatus(storeId, SyncStatus.SYNCING_PRODUCTS);

            productService.syncProductsFromTrendyol(storeId);

            updatePhaseStatus(storeId, PHASE_PRODUCTS, PhaseStatus.completed());
            log.info("[PHASE 1] Products sync COMPLETED");

            // ═══════════════════════════════════════════════════════════════
            // PHASE 2: PARALLEL EXECUTION - 3 threads running simultaneously
            // ═══════════════════════════════════════════════════════════════
            log.info("[PHASE 2] Starting PARALLEL execution...");
            log.info("  → Thread A: Historical → Financial → Gap → Commissions (critical chain)");
            log.info("  → Thread B: Returns (independent)");
            log.info("  → Thread C: QA (independent)");

            // Thread A: Critical chain (must run sequentially within itself)
            CompletableFuture<Void> criticalChainFuture = CompletableFuture.runAsync(() -> {
                runCriticalChain(storeId);
            }, onboardingExecutor);

            // Thread B: Returns (independent - can run in parallel)
            CompletableFuture<Void> returnsFuture = CompletableFuture.runAsync(() -> {
                runReturnsSync(storeId);
            }, onboardingExecutor);

            // Thread C: QA (independent - can run in parallel)
            CompletableFuture<Void> qaFuture = CompletableFuture.runAsync(() -> {
                runQaSync(storeId);
            }, onboardingExecutor);

            // Wait for ALL parallel tasks to complete
            CompletableFuture.allOf(criticalChainFuture, returnsFuture, qaFuture).join();

            // ═══════════════════════════════════════════════════════════════
            // ALL DONE - Update final status
            // ═══════════════════════════════════════════════════════════════
            Store finalStore = storeRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

            // Check if sync was cancelled
            boolean wasCancelled = isCancelled(storeId);
            if (wasCancelled) {
                updateOverallStatus(finalStore, OverallSyncStatus.CANCELLED);
                finalStore.setSyncStatus(SyncStatus.CANCELLED);
                log.warn("[PARALLEL SYNC] Sync was CANCELLED for store: {}", storeId);
                clearCancelFlag(storeId);
            } else {
                // Check if any phase failed
                boolean anyFailed = finalStore.getSyncPhases().values().stream()
                        .anyMatch(p -> PhaseStatusType.FAILED == p.getStatus());

                if (anyFailed) {
                    updateOverallStatus(finalStore, OverallSyncStatus.PARTIAL_COMPLETE);
                    finalStore.setSyncStatus(SyncStatus.PARTIAL_COMPLETE);
                    log.warn("[PARALLEL SYNC] Completed with some failures for store: {}", storeId);
                } else {
                    updateOverallStatus(finalStore, OverallSyncStatus.COMPLETED);
                    finalStore.setSyncStatus(SyncStatus.COMPLETED);
                    log.info("[PARALLEL SYNC] All phases completed successfully for store: {}", storeId);
                }
            }

            finalStore.setInitialSyncCompleted(true);
            finalStore.setSyncErrorMessage(null);
            storeRepository.save(finalStore);

            // Calculate and log total duration
            LocalDateTime endTime = LocalDateTime.now();
            long totalDurationMinutes = Duration.between(startTime, endTime).toMinutes();
            long totalDurationSeconds = Duration.between(startTime, endTime).toSeconds() % 60;

            log.info("========================================");
            log.info("[SUCCESS] PARALLEL ONBOARDING COMPLETED!");
            log.info("  Store: {}", finalStore.getStoreName());
            log.info("  Total Time: {}m {}s", totalDurationMinutes, totalDurationSeconds);
            log.info("  Phases completed in parallel - significant time savings!");
            log.info("========================================");

        } catch (Exception e) {
            log.error("[PARALLEL SYNC] Fatal error for store {}: {}", storeId, e.getMessage(), e);
            updateOverallStatus(storeId, OverallSyncStatus.FAILED);
            updateLegacySyncStatus(storeId, SyncStatus.FAILED);
        }
    }

    /**
     * Critical chain: Historical → Financial → Gap → Commissions
     * These MUST run sequentially because each depends on the previous.
     */
    private void runCriticalChain(UUID storeId) {
        try {
            // Check cancellation before starting
            if (isCancelled(storeId)) {
                log.info("[Thread A] Critical chain cancelled before start");
                return;
            }

            // ─── HISTORICAL ───
            log.info("[Thread A] HISTORICAL sync starting...");
            updatePhaseStatus(storeId, PHASE_HISTORICAL, PhaseStatus.active());
            updateLegacySyncStatus(storeId, SyncStatus.SYNCING_HISTORICAL);

            try {
                HistoricalSyncResult result = historicalSettlementService.syncHistoricalSettlementsForStore(storeId);

                Store store = storeRepository.findById(storeId).orElseThrow();
                store.setHistoricalSyncStatus(HistoricalSyncStatus.valueOf(result.getStatus()));
                store.setHistoricalSyncDate(LocalDateTime.now());
                storeRepository.save(store);

                updatePhaseStatus(storeId, PHASE_HISTORICAL, PhaseStatus.completed());
                log.info("[Thread A] HISTORICAL completed: {} orders", result.getOrdersCreated());
            } catch (Exception e) {
                log.warn("[Thread A] HISTORICAL failed, continuing: {}", e.getMessage());
                updatePhaseStatus(storeId, PHASE_HISTORICAL, PhaseStatus.failed(e.getMessage()));
            }

            // Check cancellation before next phase
            if (isCancelled(storeId)) {
                log.info("[Thread A] Critical chain cancelled after HISTORICAL");
                return;
            }

            // ─── FINANCIAL ───
            log.info("[Thread A] FINANCIAL sync starting...");
            updatePhaseStatus(storeId, PHASE_FINANCIAL, PhaseStatus.active());
            updateLegacySyncStatus(storeId, SyncStatus.SYNCING_FINANCIAL);

            try {
                financialService.fetchAndUpdateSettlementsForStore(storeId);

                // Also sync payment data
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusMonths(12);
                otherFinancialsService.syncPaymentOrders(storeId, startDate, endDate);
                otherFinancialsService.syncStoppages(storeId, startDate, endDate);

                // Sync ALL historical cargo invoices (from earliest order date to today)
                // This ensures complete shipping cost data for all orders
                log.info("[Thread A] Syncing historical cargo invoices (full history)...");
                TrendyolOtherFinancialsService.HistoricalCargoSyncResult cargoResult =
                        otherFinancialsService.syncHistoricalCargoInvoices(storeId);
                log.info("[Thread A] Historical cargo sync: {} invoices, {} orders updated",
                        cargoResult.getSyncedCargoInvoices(), cargoResult.getUpdatedOrders());

                updatePhaseStatus(storeId, PHASE_FINANCIAL, PhaseStatus.completed());
                log.info("[Thread A] FINANCIAL completed");
            } catch (Exception e) {
                log.warn("[Thread A] FINANCIAL failed, continuing: {}", e.getMessage());
                updatePhaseStatus(storeId, PHASE_FINANCIAL, PhaseStatus.failed(e.getMessage()));
            }

            // Check cancellation before next phase
            if (isCancelled(storeId)) {
                log.info("[Thread A] Critical chain cancelled after FINANCIAL");
                return;
            }

            // ─── GAP FILL ───
            log.info("[Thread A] GAP fill starting...");
            updatePhaseStatus(storeId, PHASE_GAP, PhaseStatus.active());
            updateLegacySyncStatus(storeId, SyncStatus.SYNCING_GAP);

            try {
                int gapOrders = orderService.syncGapFromOrdersApi(storeId);

                // Update gap orders with real shipping costs from cargo invoices
                // This is critical because gap orders were added AFTER the initial cargo sync
                log.info("[Thread A] Updating gap orders with real shipping costs...");
                int shippingUpdated = otherFinancialsService.updateOrdersWithRealShipping(storeId);
                log.info("[Thread A] Updated {} gap orders with real shipping costs", shippingUpdated);

                updatePhaseStatus(storeId, PHASE_GAP, PhaseStatus.completed());
                log.info("[Thread A] GAP completed: {} orders", gapOrders);
            } catch (Exception e) {
                log.warn("[Thread A] GAP failed, continuing: {}", e.getMessage());
                updatePhaseStatus(storeId, PHASE_GAP, PhaseStatus.failed(e.getMessage()));
            }

            // Check cancellation before next phase
            if (isCancelled(storeId)) {
                log.info("[Thread A] Critical chain cancelled after GAP");
                return;
            }

            // ─── COMMISSIONS ───
            log.info("[Thread A] COMMISSIONS recalculation starting...");
            updatePhaseStatus(storeId, PHASE_COMMISSIONS, PhaseStatus.active());
            updateLegacySyncStatus(storeId, SyncStatus.RECALCULATING_COMMISSIONS);

            try {
                int recalculated = commissionRecalculationService.recalculateEstimatedCommissions(storeId);
                updatePhaseStatus(storeId, PHASE_COMMISSIONS, PhaseStatus.completed());
                log.info("[Thread A] COMMISSIONS completed: {} orders updated", recalculated);
            } catch (Exception e) {
                log.warn("[Thread A] COMMISSIONS failed, continuing: {}", e.getMessage());
                updatePhaseStatus(storeId, PHASE_COMMISSIONS, PhaseStatus.failed(e.getMessage()));
            }

            log.info("[Thread A] Critical chain FINISHED");

        } catch (Exception e) {
            log.error("[Thread A] Critical chain error: {}", e.getMessage(), e);
        }
    }

    /**
     * Returns sync - runs in parallel with critical chain
     */
    private void runReturnsSync(UUID storeId) {
        try {
            // Check cancellation before starting
            if (isCancelled(storeId)) {
                log.info("[Thread B] RETURNS cancelled before start");
                return;
            }

            log.info("[Thread B] RETURNS sync starting...");
            updatePhaseStatus(storeId, PHASE_RETURNS, PhaseStatus.active());
            updateLegacySyncStatus(storeId, SyncStatus.SYNCING_RETURNS);

            var result = claimsService.syncClaims(storeId);

            updatePhaseStatus(storeId, PHASE_RETURNS, PhaseStatus.completed());
            log.info("[Thread B] RETURNS completed: {} claims ({} new, {} updated)",
                    result.getTotalFetched(), result.getNewClaims(), result.getUpdatedClaims());

        } catch (Exception e) {
            log.warn("[Thread B] RETURNS failed: {}", e.getMessage());
            updatePhaseStatus(storeId, PHASE_RETURNS, PhaseStatus.failed(e.getMessage()));
        }
    }

    /**
     * QA sync - runs in parallel with critical chain
     */
    private void runQaSync(UUID storeId) {
        try {
            // Check cancellation before starting
            if (isCancelled(storeId)) {
                log.info("[Thread C] QA cancelled before start");
                return;
            }

            log.info("[Thread C] QA sync starting...");
            updatePhaseStatus(storeId, PHASE_QA, PhaseStatus.active());
            updateLegacySyncStatus(storeId, SyncStatus.SYNCING_QA);

            qaService.syncQuestions(storeId);

            updatePhaseStatus(storeId, PHASE_QA, PhaseStatus.completed());
            log.info("[Thread C] QA completed");

        } catch (Exception e) {
            log.warn("[Thread C] QA failed: {}", e.getMessage());
            updatePhaseStatus(storeId, PHASE_QA, PhaseStatus.failed(e.getMessage()));
        }
    }

    /**
     * Initialize all sync phases as PENDING
     */
    private void initializeSyncPhases(Store store) {
        Map<String, PhaseStatus> phases = new HashMap<>();
        phases.put(PHASE_PRODUCTS, PhaseStatus.pending());
        phases.put(PHASE_HISTORICAL, PhaseStatus.pending());
        phases.put(PHASE_FINANCIAL, PhaseStatus.pending());
        phases.put(PHASE_GAP, PhaseStatus.pending());
        phases.put(PHASE_COMMISSIONS, PhaseStatus.pending());
        phases.put(PHASE_RETURNS, PhaseStatus.pending());
        phases.put(PHASE_QA, PhaseStatus.pending());

        store.setSyncPhases(phases);
        store.setOverallSyncStatus(OverallSyncStatus.PENDING);
        storeRepository.save(store);
    }

    /**
     * Update a single phase status (thread-safe with store-level locking)
     */
    private void updatePhaseStatus(UUID storeId, String phaseName, PhaseStatus status) {
        ReentrantLock lock = storeLocks.computeIfAbsent(storeId, k -> new ReentrantLock());
        lock.lock();
        try {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

            Map<String, PhaseStatus> phases = store.getSyncPhases();
            if (phases == null) {
                phases = new HashMap<>();
            }

            // Preserve startedAt if transitioning from ACTIVE
            PhaseStatus existing = phases.get(phaseName);
            if (existing != null && existing.getStartedAt() != null && status.getStartedAt() == null) {
                status.setStartedAt(existing.getStartedAt());
            }

            phases.put(phaseName, status);
            store.setSyncPhases(phases);
            storeRepository.save(store);

            log.debug("[PHASE UPDATE] {} -> {}", phaseName, status.getStatus());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Update overall sync status
     */
    private void updateOverallStatus(Store store, OverallSyncStatus status) {
        store.setOverallSyncStatus(status);
        storeRepository.save(store);
    }

    private void updateOverallStatus(UUID storeId, OverallSyncStatus status) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        updateOverallStatus(store, status);
    }

    /**
     * Update legacy syncStatus for backward compatibility (thread-safe with store-level locking)
     */
    private void updateLegacySyncStatus(UUID storeId, SyncStatus status) {
        ReentrantLock lock = storeLocks.computeIfAbsent(storeId, k -> new ReentrantLock());
        lock.lock();
        try {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
            store.setSyncStatus(status);
            storeRepository.save(store);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retries or starts initial sync for a store.
     */
    public void retryInitialSync(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        OverallSyncStatus currentStatus = store.getOverallSyncStatus();
        SyncStatus legacyStatus = store.getSyncStatus();

        // Block if sync is already in progress
        if (OverallSyncStatus.IN_PROGRESS == currentStatus ||
            (legacyStatus != null && legacyStatus.name().startsWith("SYNCING_"))) {
            log.warn("Cannot start sync for store {} - sync already in progress", storeId);
            return;
        }

        // Block if already fully completed
        if (OverallSyncStatus.COMPLETED == currentStatus && Boolean.TRUE.equals(store.getInitialSyncCompleted())) {
            log.warn("Store {} already completed initial sync. Use force-sync if you want to re-sync.", storeId);
            return;
        }

        // Allow retry for PARTIAL_COMPLETE or CANCELLED
        if (OverallSyncStatus.PARTIAL_COMPLETE == currentStatus || OverallSyncStatus.CANCELLED == currentStatus) {
            log.info("Store {} has status {} - allowing retry", storeId, currentStatus);
        }

        log.info("Starting sync for store {} with current status: {}", storeId, currentStatus);
        performInitialSync(store);
    }

    /**
     * Gets the current sync status of a store.
     */
    public String getSyncStatus(UUID storeId) {
        return storeRepository.findById(storeId)
                .map(store -> store.getSyncStatus() != null ? store.getSyncStatus().name() : "UNKNOWN")
                .orElse("UNKNOWN");
    }

    /**
     * Gets the sync phases for a store.
     */
    public Map<String, PhaseStatus> getSyncPhases(UUID storeId) {
        return storeRepository.findById(storeId)
                .map(Store::getSyncPhases)
                .orElse(new HashMap<>());
    }

    /**
     * Request cancellation of an in-progress sync operation.
     * The sync will stop at the next cancellation check point.
     */
    public void requestCancelSync(UUID storeId) {
        cancelFlags.put(storeId, new AtomicBoolean(true));
        log.info("[CANCEL] Cancel requested for store: {}", storeId);
    }

    /**
     * Check if sync has been cancelled for a store.
     */
    private boolean isCancelled(UUID storeId) {
        AtomicBoolean flag = cancelFlags.get(storeId);
        return flag != null && flag.get();
    }

    /**
     * Clear the cancel flag after sync completes or is cancelled.
     */
    private void clearCancelFlag(UUID storeId) {
        cancelFlags.remove(storeId);
    }
}
