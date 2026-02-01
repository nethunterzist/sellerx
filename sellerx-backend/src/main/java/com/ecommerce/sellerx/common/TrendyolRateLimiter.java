package com.ecommerce.sellerx.common;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-store rate limiter for Trendyol API calls.
 * Uses Guava RateLimiter for non-blocking rate limiting.
 *
 * Trendyol API rate limit: ~10 requests per second per store.
 * Each store gets its own independent RateLimiter instance so that
 * one store's API usage doesn't block another store's operations.
 */
@Component
public class TrendyolRateLimiter {

    private static final double PERMITS_PER_SECOND = 10.0;
    private static final UUID DEFAULT_STORE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ConcurrentHashMap<UUID, RateLimiter> storeLimiters = new ConcurrentHashMap<>();

    /**
     * Acquires a permit from the per-store rate limiter.
     * Creates a new RateLimiter (10 permits/sec) for the store on first use.
     * This method blocks until a permit is available.
     *
     * @param storeId the store UUID to rate-limit for
     * @return the time (in seconds) spent waiting for the permit
     */
    public double acquire(UUID storeId) {
        RateLimiter limiter = storeLimiters.computeIfAbsent(storeId, id -> RateLimiter.create(PERMITS_PER_SECOND));
        return limiter.acquire();
    }

    /**
     * @deprecated Use {@link #acquire(UUID)} with a storeId instead.
     *             This method exists for backward compatibility during transition.
     */
    @Deprecated
    public double acquire() {
        return acquire(DEFAULT_STORE_ID);
    }

    /**
     * Tries to acquire a permit without blocking for a specific store.
     *
     * @param storeId the store UUID to rate-limit for
     * @return true if permit was acquired, false otherwise
     */
    public boolean tryAcquire(UUID storeId) {
        RateLimiter limiter = storeLimiters.computeIfAbsent(storeId, id -> RateLimiter.create(PERMITS_PER_SECOND));
        return limiter.tryAcquire();
    }

    /**
     * Tries to acquire a permit without blocking.
     *
     * @return true if permit was acquired, false otherwise
     */
    public boolean tryAcquire() {
        return tryAcquire(DEFAULT_STORE_ID);
    }

    /**
     * Gets the configured rate (permits per second).
     *
     * @return the rate in permits per second
     */
    public double getRate() {
        return PERMITS_PER_SECOND;
    }

    /**
     * Removes the rate limiter for a specific store.
     * Useful when a store is deleted or deactivated to free resources.
     *
     * @param storeId the store UUID to remove the limiter for
     */
    public void cleanUp(UUID storeId) {
        storeLimiters.remove(storeId);
    }

    /**
     * Returns the number of active per-store rate limiters.
     * Useful for monitoring and diagnostics.
     *
     * @return the number of stores with active rate limiters
     */
    public int getActiveStoreCount() {
        return storeLimiters.size();
    }
}
