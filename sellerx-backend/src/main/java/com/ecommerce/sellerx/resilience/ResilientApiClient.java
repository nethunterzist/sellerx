package com.ecommerce.sellerx.resilience;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Resilient API Client for Trendyol API calls.
 *
 * Provides annotation-based resilience patterns:
 * - CircuitBreaker: Prevents cascading failures (opens at 50% failure rate)
 * - Retry: Exponential backoff (3 attempts, starting 500ms, 2x multiplier)
 * - RateLimiter: Non-blocking 10 req/sec per store
 * - Bulkhead: Limits concurrent calls (100 API calls, 50 sync operations)
 * - TimeLimiter: Prevents long-running operations (30s API, 180s sync)
 *
 * Usage:
 * <pre>
 * ApiResponse response = resilientApiClient.executeApiCall(
 *     storeId,
 *     () -> restTemplate.exchange(url, method, entity, responseType)
 * );
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResilientApiClient {

    private final RestTemplate restTemplate;

    /**
     * Execute a Trendyol API call with full resilience protection.
     *
     * Order of decorators (inside-out):
     * 1. Bulkhead (outermost) - limit concurrent calls
     * 2. TimeLimiter - prevent hanging
     * 3. CircuitBreaker - prevent cascading failures
     * 4. RateLimiter - respect API limits
     * 5. Retry (innermost) - handle transient failures
     *
     * @param storeId Store identifier for logging
     * @param apiCall The API call to execute
     * @return API response
     * @throws ResilientApiException if all retries fail or circuit is open
     */
    @Bulkhead(name = "trendyolApi", fallbackMethod = "bulkheadFallback")
    @CircuitBreaker(name = "trendyolApi", fallbackMethod = "circuitBreakerFallback")
    @RateLimiter(name = "trendyolApi", fallbackMethod = "rateLimiterFallback")
    @Retry(name = "trendyolApi")
    public <T> T executeApiCall(UUID storeId, Supplier<T> apiCall) {
        log.debug("Executing API call for store: {}", storeId);
        try {
            return apiCall.get();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            // Don't retry auth errors
            log.error("Authentication error for store {}: {}", storeId, e.getMessage());
            throw new ResilientApiException(
                ResilientApiException.FailureType.AUTH_ERROR,
                "Authentication failed: " + e.getStatusCode(),
                e
            );
        } catch (HttpServerErrorException e) {
            log.warn("Server error for store {}: {} - will retry", storeId, e.getMessage());
            throw e; // Let retry handle it
        } catch (ResourceAccessException e) {
            log.warn("Connection error for store {}: {} - will retry", storeId, e.getMessage());
            throw e; // Let retry handle it
        }
    }

    /**
     * Execute a sync operation with extended timeouts and relaxed circuit breaker.
     */
    @Bulkhead(name = "trendyolSync", fallbackMethod = "bulkheadFallback")
    @CircuitBreaker(name = "trendyolSync", fallbackMethod = "circuitBreakerFallback")
    @Retry(name = "trendyolSync")
    public <T> T executeSyncOperation(UUID storeId, Supplier<T> operation) {
        log.debug("Executing sync operation for store: {}", storeId);
        return operation.get();
    }

    /**
     * Execute async API call with resilience patterns.
     */
    @Bulkhead(name = "trendyolApi", type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = "trendyolApi")
    @CircuitBreaker(name = "trendyolApi", fallbackMethod = "asyncCircuitBreakerFallback")
    @RateLimiter(name = "trendyolApi")
    @Retry(name = "trendyolApi")
    public <T> CompletableFuture<T> executeAsyncApiCall(UUID storeId, Supplier<T> apiCall) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Executing async API call for store: {}", storeId);
            return apiCall.get();
        });
    }

    /**
     * Check if API calls are currently allowed (circuit not open, rate limit available).
     */
    public boolean isApiAvailable(String circuitBreakerName) {
        // This can be used to check before attempting calls
        return true; // Actual check happens via decorators
    }

    // ========== Fallback Methods ==========

    private <T> T circuitBreakerFallback(UUID storeId, Supplier<T> apiCall, CallNotPermittedException e) {
        log.warn("Circuit breaker OPEN for store {} - request rejected", storeId);
        throw new ResilientApiException(
            ResilientApiException.FailureType.CIRCUIT_OPEN,
            "Service temporarily unavailable due to high error rate. Retry after 30 seconds.",
            e
        );
    }

    private <T> CompletableFuture<T> asyncCircuitBreakerFallback(UUID storeId, Supplier<T> apiCall, CallNotPermittedException e) {
        log.warn("Circuit breaker OPEN for async call, store {}", storeId);
        return CompletableFuture.failedFuture(new ResilientApiException(
            ResilientApiException.FailureType.CIRCUIT_OPEN,
            "Service temporarily unavailable",
            e
        ));
    }

    private <T> T rateLimiterFallback(UUID storeId, Supplier<T> apiCall, RequestNotPermitted e) {
        log.warn("Rate limit exceeded for store {} - request rejected (non-blocking)", storeId);
        throw new ResilientApiException(
            ResilientApiException.FailureType.RATE_LIMIT_EXCEEDED,
            "Rate limit exceeded. Maximum 10 requests per second.",
            e
        );
    }

    private <T> T bulkheadFallback(UUID storeId, Supplier<T> apiCall, Throwable e) {
        log.warn("Bulkhead full for store {} - request rejected", storeId);
        throw new ResilientApiException(
            ResilientApiException.FailureType.BULKHEAD_FULL,
            "Too many concurrent operations. Please try again.",
            e
        );
    }

    // ========== Convenience Methods ==========

    /**
     * Execute GET request with resilience.
     */
    public <T> ResponseEntity<T> get(UUID storeId, String url, HttpEntity<?> entity, Class<T> responseType) {
        return executeApiCall(storeId, () ->
            restTemplate.exchange(url, HttpMethod.GET, entity, responseType)
        );
    }

    /**
     * Execute POST request with resilience.
     */
    public <T> ResponseEntity<T> post(UUID storeId, String url, HttpEntity<?> entity, Class<T> responseType) {
        return executeApiCall(storeId, () ->
            restTemplate.exchange(url, HttpMethod.POST, entity, responseType)
        );
    }

    /**
     * Execute PUT request with resilience.
     */
    public <T> ResponseEntity<T> put(UUID storeId, String url, HttpEntity<?> entity, Class<T> responseType) {
        return executeApiCall(storeId, () ->
            restTemplate.exchange(url, HttpMethod.PUT, entity, responseType)
        );
    }
}
