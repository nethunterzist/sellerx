package com.ecommerce.sellerx.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j Configuration for SellerX.
 *
 * Provides centralized resilience patterns:
 * - Circuit Breaker: Prevents cascading failures
 * - Retry: Handles transient failures with exponential backoff
 * - Rate Limiter: Non-blocking per-store rate limiting (10 req/sec)
 * - Bulkhead: Limits concurrent operations
 * - Time Limiter: Prevents long-running operations
 *
 * Named instances:
 * - trendyolApi: For general Trendyol API calls
 * - trendyolSync: For sync operations (products, orders, financial)
 * - externalService: For other external services
 */
@Configuration
@Slf4j
public class Resilience4jConfig {

    /**
     * Circuit breaker for Trendyol API calls.
     * Opens after 50% failure rate in sliding window of 10 calls.
     * Waits 30 seconds before half-open state.
     */
    @Bean
    public CircuitBreaker trendyolApiCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("trendyolApi");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Trendyol API Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.warn("Trendyol API failure rate exceeded: {}%", event.getFailureRate()))
            .onError(event ->
                log.error("Trendyol API error recorded: {}", event.getThrowable().getMessage()));

        return circuitBreaker;
    }

    /**
     * Circuit breaker for sync operations.
     * More tolerant: opens after 40% failure rate in sliding window of 20 calls.
     * Waits 60 seconds before half-open state.
     */
    @Bean
    public CircuitBreaker trendyolSyncCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("trendyolSync");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Trendyol Sync Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    /**
     * Non-blocking rate limiter for Trendyol API (10 req/sec).
     * Returns immediately if limit exceeded (no waiting).
     */
    @Bean
    public RateLimiter trendyolApiRateLimiter(RateLimiterRegistry registry) {
        RateLimiter rateLimiter = registry.rateLimiter("trendyolApi");

        rateLimiter.getEventPublisher()
            .onFailure(event ->
                log.warn("Rate limit exceeded for Trendyol API - request rejected"));

        return rateLimiter;
    }

    /**
     * Retry for Trendyol API with exponential backoff.
     * 3 attempts, starting at 500ms, multiplied by 2 each retry.
     */
    @Bean
    public Retry trendyolApiRetry(RetryRegistry registry) {
        Retry retry = registry.retry("trendyolApi");

        retry.getEventPublisher()
            .onRetry(event ->
                log.info("Retrying Trendyol API call, attempt #{}: {}",
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage()))
            .onError(event ->
                log.error("Trendyol API retry exhausted after {} attempts",
                    event.getNumberOfRetryAttempts()));

        return retry;
    }

    /**
     * Retry for sync operations with exponential backoff.
     * 5 attempts, starting at 1s, multiplied by 2.5 each retry.
     */
    @Bean
    public Retry trendyolSyncRetry(RetryRegistry registry) {
        Retry retry = registry.retry("trendyolSync");

        retry.getEventPublisher()
            .onRetry(event ->
                log.info("Retrying sync operation, attempt #{}", event.getNumberOfRetryAttempts()));

        return retry;
    }

    /**
     * Bulkhead for sync operations.
     * Limits to 50 concurrent sync operations.
     */
    @Bean
    public Bulkhead trendyolSyncBulkhead(BulkheadRegistry registry) {
        Bulkhead bulkhead = registry.bulkhead("trendyolSync");

        bulkhead.getEventPublisher()
            .onCallRejected(event ->
                log.warn("Sync operation rejected by bulkhead - too many concurrent operations"))
            .onCallPermitted(event ->
                log.debug("Sync operation permitted by bulkhead"));

        return bulkhead;
    }

    /**
     * Bulkhead for general API calls.
     * Limits to 100 concurrent API calls.
     */
    @Bean
    public Bulkhead trendyolApiBulkhead(BulkheadRegistry registry) {
        return registry.bulkhead("trendyolApi");
    }

    /**
     * Resilience metrics endpoint for monitoring.
     */
    @Bean
    public ResilienceMetricsService resilienceMetricsService(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry,
            MeterRegistry meterRegistry) {
        return new ResilienceMetricsService(
            circuitBreakerRegistry,
            rateLimiterRegistry,
            retryRegistry,
            bulkheadRegistry,
            meterRegistry);
    }
}
