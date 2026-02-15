package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.stores.Store;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Resilient synchronization wrapper with 4-level protection:
 *
 * Level 1: Try-Catch (Error Isolation)
 *   - Each store's error is isolated
 *   - One store failure doesn't affect others
 *
 * Level 2: Timeout (Hanging Prevention)
 *   - Per-operation timeout
 *   - Prevents single slow store from blocking system
 *
 * Level 3: Bulkhead (Resource Isolation)
 *   - Limits concurrent operations per store
 *   - Prevents resource exhaustion
 *
 * Level 4: Circuit Breaker (Cascading Failure Prevention)
 *   - Tracks consecutive failures
 *   - Opens circuit after threshold (blocks calls)
 *   - Half-open state for recovery testing
 *   - Metrics for monitoring
 *
 * Scaling Target: 1500+ stores, 225M+ orders/year
 */
@Service
@Slf4j
public class ResilientSyncService {

    private final ParallelStoreSyncService parallelSyncService;

    // Circuit breaker configuration
    private static final int FAILURE_THRESHOLD = 50;        // Consecutive failures to open circuit
    private static final Duration OPEN_DURATION = Duration.ofMinutes(1);
    private static final int HALF_OPEN_PERMITS = 5;         // Test calls in half-open state

    // Per-store circuit breaker state
    private final Map<UUID, CircuitBreaker> storeCircuits = new ConcurrentHashMap<>();

    // Bulkhead: limit concurrent syncs per store
    private final Map<UUID, Semaphore> storeSemaphores = new ConcurrentHashMap<>();
    private static final int MAX_CONCURRENT_PER_STORE = 1;

    // Timeout configuration
    private static final Duration OPERATION_TIMEOUT = Duration.ofMinutes(3);

    // Metrics
    private final Counter circuitOpenCounter;
    private final Counter circuitHalfOpenCounter;
    private final Counter circuitClosedCounter;
    private final Counter timeoutCounter;
    private final Counter bulkheadRejectedCounter;

    public ResilientSyncService(
            ParallelStoreSyncService parallelSyncService,
            MeterRegistry meterRegistry) {
        this.parallelSyncService = parallelSyncService;

        this.circuitOpenCounter = Counter.builder("sellerx.circuit.state")
                .tag("state", "open")
                .description("Number of times circuit breaker opened")
                .register(meterRegistry);
        this.circuitHalfOpenCounter = Counter.builder("sellerx.circuit.state")
                .tag("state", "half_open")
                .description("Number of times circuit breaker went half-open")
                .register(meterRegistry);
        this.circuitClosedCounter = Counter.builder("sellerx.circuit.state")
                .tag("state", "closed")
                .description("Number of times circuit breaker closed")
                .register(meterRegistry);
        this.timeoutCounter = Counter.builder("sellerx.resilient.timeout")
                .description("Number of operation timeouts")
                .register(meterRegistry);
        this.bulkheadRejectedCounter = Counter.builder("sellerx.resilient.bulkhead.rejected")
                .description("Number of operations rejected by bulkhead")
                .register(meterRegistry);
    }

    /**
     * Execute sync operation with full resilience protection.
     *
     * @param store The store to sync
     * @param operation The sync operation to execute
     * @param <T> Return type
     * @return Result or empty on failure
     */
    public <T> ResilientResult<T> executeWithResilience(Store store, Supplier<T> operation) {
        UUID storeId = store.getId();
        String storeName = store.getStoreName();

        // Level 4: Circuit Breaker Check
        CircuitBreaker circuit = getCircuitBreaker(storeId);
        if (!circuit.allowRequest()) {
            log.warn("Circuit OPEN for store {} - rejecting request", storeName);
            return ResilientResult.circuitOpen(storeId);
        }

        // Level 3: Bulkhead Check
        Semaphore semaphore = getSemaphore(storeId);
        if (!semaphore.tryAcquire()) {
            bulkheadRejectedCounter.increment();
            log.warn("Bulkhead full for store {} - rejecting request", storeName);
            return ResilientResult.bulkheadFull(storeId);
        }

        try {
            // Level 2: Timeout Wrapper
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                // Level 1: Try-Catch (Error Isolation)
                try {
                    return operation.get();
                } catch (Exception e) {
                    log.error("Operation failed for store {}: {}", storeName, e.getMessage());
                    throw new CompletionException(e);
                }
            });

            try {
                T result = future.get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

                // Success: record to circuit breaker
                circuit.recordSuccess();
                return ResilientResult.success(storeId, result);

            } catch (TimeoutException e) {
                timeoutCounter.increment();
                circuit.recordFailure();
                future.cancel(true);
                log.error("Operation timeout for store {} after {}s", storeName, OPERATION_TIMEOUT.toSeconds());
                return ResilientResult.timeout(storeId);

            } catch (ExecutionException e) {
                circuit.recordFailure();
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.error("Operation failed for store {}: {}", storeName, cause.getMessage());
                return ResilientResult.error(storeId, cause.getMessage());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                circuit.recordFailure();
                return ResilientResult.error(storeId, "Interrupted");
            }

        } finally {
            semaphore.release();
        }
    }

    /**
     * Execute parallel sync with resilience wrapper.
     */
    public ParallelStoreSyncService.SyncResult syncAllWithResilience(boolean catchUpOnly) {
        log.info("Starting resilient {} sync", catchUpOnly ? "catch-up" : "full");

        try {
            return parallelSyncService.syncAllStoresParallel(catchUpOnly);
        } catch (Exception e) {
            log.error("Resilient sync failed: {}", e.getMessage(), e);
            return new ParallelStoreSyncService.SyncResult(0, 0, 0);
        }
    }

    /**
     * Get circuit breaker status for monitoring.
     */
    public Map<UUID, CircuitState> getCircuitStates() {
        Map<UUID, CircuitState> states = new ConcurrentHashMap<>();
        storeCircuits.forEach((storeId, circuit) ->
                states.put(storeId, circuit.getState()));
        return states;
    }

    /**
     * Manually reset a store's circuit breaker (admin function).
     */
    public void resetCircuit(UUID storeId) {
        CircuitBreaker circuit = storeCircuits.get(storeId);
        if (circuit != null) {
            circuit.reset();
            log.info("Circuit breaker reset for store {}", storeId);
        }
    }

    /**
     * Get all open circuits (for monitoring dashboard).
     */
    public Map<UUID, CircuitBreaker> getOpenCircuits() {
        Map<UUID, CircuitBreaker> openCircuits = new ConcurrentHashMap<>();
        storeCircuits.forEach((storeId, circuit) -> {
            if (circuit.getState() == CircuitState.OPEN) {
                openCircuits.put(storeId, circuit);
            }
        });
        return openCircuits;
    }

    private CircuitBreaker getCircuitBreaker(UUID storeId) {
        return storeCircuits.computeIfAbsent(storeId,
                id -> new CircuitBreaker(FAILURE_THRESHOLD, OPEN_DURATION, HALF_OPEN_PERMITS,
                        circuitOpenCounter, circuitHalfOpenCounter, circuitClosedCounter));
    }

    private Semaphore getSemaphore(UUID storeId) {
        return storeSemaphores.computeIfAbsent(storeId,
                id -> new Semaphore(MAX_CONCURRENT_PER_STORE));
    }

    // ========== Result Types ==========

    public record ResilientResult<T>(
            UUID storeId,
            boolean success,
            T result,
            FailureReason failureReason,
            String errorMessage
    ) {
        public static <T> ResilientResult<T> success(UUID storeId, T result) {
            return new ResilientResult<>(storeId, true, result, null, null);
        }

        public static <T> ResilientResult<T> circuitOpen(UUID storeId) {
            return new ResilientResult<>(storeId, false, null, FailureReason.CIRCUIT_OPEN, "Circuit breaker is open");
        }

        public static <T> ResilientResult<T> bulkheadFull(UUID storeId) {
            return new ResilientResult<>(storeId, false, null, FailureReason.BULKHEAD_FULL, "Too many concurrent operations");
        }

        public static <T> ResilientResult<T> timeout(UUID storeId) {
            return new ResilientResult<>(storeId, false, null, FailureReason.TIMEOUT, "Operation timed out");
        }

        public static <T> ResilientResult<T> error(UUID storeId, String message) {
            return new ResilientResult<>(storeId, false, null, FailureReason.ERROR, message);
        }
    }

    public enum FailureReason {
        CIRCUIT_OPEN,
        BULKHEAD_FULL,
        TIMEOUT,
        ERROR
    }

    public enum CircuitState {
        CLOSED,      // Normal operation
        OPEN,        // Blocking all requests
        HALF_OPEN    // Testing if system recovered
    }

    // ========== Circuit Breaker Implementation ==========

    public static class CircuitBreaker {
        private final int failureThreshold;
        private final Duration openDuration;
        private final int halfOpenPermits;

        private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
        private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private volatile CircuitState state = CircuitState.CLOSED;

        private final Counter openCounter;
        private final Counter halfOpenCounter;
        private final Counter closedCounter;

        public CircuitBreaker(int failureThreshold, Duration openDuration, int halfOpenPermits,
                              Counter openCounter, Counter halfOpenCounter, Counter closedCounter) {
            this.failureThreshold = failureThreshold;
            this.openDuration = openDuration;
            this.halfOpenPermits = halfOpenPermits;
            this.openCounter = openCounter;
            this.halfOpenCounter = halfOpenCounter;
            this.closedCounter = closedCounter;
        }

        public synchronized boolean allowRequest() {
            switch (state) {
                case CLOSED:
                    return true;

                case OPEN:
                    // Check if we should transition to half-open
                    long timeSinceFailure = System.currentTimeMillis() - lastFailureTime.get();
                    if (timeSinceFailure > openDuration.toMillis()) {
                        transitionTo(CircuitState.HALF_OPEN);
                        halfOpenAttempts.set(0);
                        return true;
                    }
                    return false;

                case HALF_OPEN:
                    // Allow limited requests in half-open state
                    return halfOpenAttempts.incrementAndGet() <= halfOpenPermits;

                default:
                    return false;
            }
        }

        public synchronized void recordSuccess() {
            switch (state) {
                case HALF_OPEN:
                    // Success in half-open: close circuit
                    consecutiveFailures.set(0);
                    transitionTo(CircuitState.CLOSED);
                    break;

                case CLOSED:
                    // Reset failure count on success
                    consecutiveFailures.set(0);
                    break;

                case OPEN:
                    // Shouldn't happen, but reset if it does
                    break;
            }
        }

        public synchronized void recordFailure() {
            lastFailureTime.set(System.currentTimeMillis());

            switch (state) {
                case HALF_OPEN:
                    // Failure in half-open: re-open circuit
                    transitionTo(CircuitState.OPEN);
                    break;

                case CLOSED:
                    int failures = consecutiveFailures.incrementAndGet();
                    if (failures >= failureThreshold) {
                        transitionTo(CircuitState.OPEN);
                    }
                    break;

                case OPEN:
                    // Already open, nothing to do
                    break;
            }
        }

        public void reset() {
            consecutiveFailures.set(0);
            halfOpenAttempts.set(0);
            state = CircuitState.CLOSED;
        }

        public CircuitState getState() {
            return state;
        }

        public int getConsecutiveFailures() {
            return consecutiveFailures.get();
        }

        public long getLastFailureTime() {
            return lastFailureTime.get();
        }

        private void transitionTo(CircuitState newState) {
            CircuitState oldState = this.state;
            this.state = newState;

            log.info("Circuit breaker: {} -> {}", oldState, newState);

            switch (newState) {
                case OPEN -> openCounter.increment();
                case HALF_OPEN -> halfOpenCounter.increment();
                case CLOSED -> closedCounter.increment();
            }
        }
    }
}
