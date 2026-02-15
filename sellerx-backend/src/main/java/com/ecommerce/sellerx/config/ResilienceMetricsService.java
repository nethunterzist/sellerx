package com.ecommerce.sellerx.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for monitoring Resilience4j components.
 * Provides metrics and status for circuit breakers, rate limiters, retries, and bulkheads.
 */
@Service
@Slf4j
public class ResilienceMetricsService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final MeterRegistry meterRegistry;

    public ResilienceMetricsService(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry,
            MeterRegistry meterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.retryRegistry = retryRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.meterRegistry = meterRegistry;

        registerCustomMetrics();
    }

    private void registerCustomMetrics() {
        // Register gauges for circuit breaker states
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            meterRegistry.gauge(
                "sellerx.resilience.circuit_breaker.state",
                Tags.of("name", cb.getName()),
                cb,
                circuitBreaker -> circuitBreaker.getState().getOrder()
            );

            meterRegistry.gauge(
                "sellerx.resilience.circuit_breaker.failure_rate",
                Tags.of("name", cb.getName()),
                cb,
                circuitBreaker -> circuitBreaker.getMetrics().getFailureRate()
            );
        });
    }

    /**
     * Get status of all circuit breakers.
     */
    public Map<String, CircuitBreakerStatus> getCircuitBreakerStatus() {
        Map<String, CircuitBreakerStatus> status = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreakerStatus cbStatus = new CircuitBreakerStatus();
            cbStatus.setName(cb.getName());
            cbStatus.setState(cb.getState().name());
            cbStatus.setFailureRate(cb.getMetrics().getFailureRate());
            cbStatus.setSlowCallRate(cb.getMetrics().getSlowCallRate());
            cbStatus.setNumberOfBufferedCalls(cb.getMetrics().getNumberOfBufferedCalls());
            cbStatus.setNumberOfFailedCalls(cb.getMetrics().getNumberOfFailedCalls());
            cbStatus.setNumberOfSuccessfulCalls(cb.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.setNumberOfNotPermittedCalls(cb.getMetrics().getNumberOfNotPermittedCalls());
            status.put(cb.getName(), cbStatus);
        });

        return status;
    }

    /**
     * Get status of all rate limiters.
     */
    public Map<String, RateLimiterStatus> getRateLimiterStatus() {
        Map<String, RateLimiterStatus> status = new HashMap<>();

        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
            RateLimiterStatus rlStatus = new RateLimiterStatus();
            rlStatus.setName(rl.getName());
            rlStatus.setAvailablePermissions(rl.getMetrics().getAvailablePermissions());
            rlStatus.setNumberOfWaitingThreads(rl.getMetrics().getNumberOfWaitingThreads());
            status.put(rl.getName(), rlStatus);
        });

        return status;
    }

    /**
     * Get status of all bulkheads.
     */
    public Map<String, BulkheadStatus> getBulkheadStatus() {
        Map<String, BulkheadStatus> status = new HashMap<>();

        bulkheadRegistry.getAllBulkheads().forEach(bh -> {
            BulkheadStatus bhStatus = new BulkheadStatus();
            bhStatus.setName(bh.getName());
            bhStatus.setAvailableConcurrentCalls(bh.getMetrics().getAvailableConcurrentCalls());
            bhStatus.setMaxAllowedConcurrentCalls(bh.getMetrics().getMaxAllowedConcurrentCalls());
            status.put(bh.getName(), bhStatus);
        });

        return status;
    }

    /**
     * Get combined resilience status.
     */
    public ResilienceStatus getFullStatus() {
        ResilienceStatus status = new ResilienceStatus();
        status.setCircuitBreakers(getCircuitBreakerStatus());
        status.setRateLimiters(getRateLimiterStatus());
        status.setBulkheads(getBulkheadStatus());

        // Check overall health
        boolean healthy = circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .noneMatch(cb -> cb.getState() == CircuitBreaker.State.OPEN);
        status.setHealthy(healthy);

        return status;
    }

    /**
     * Manually reset a circuit breaker (admin function).
     */
    public void resetCircuitBreaker(String name) {
        circuitBreakerRegistry.find(name).ifPresent(cb -> {
            cb.reset();
            log.info("Circuit breaker '{}' has been reset", name);
        });
    }

    /**
     * Transition circuit breaker to closed state.
     */
    public void closeCircuitBreaker(String name) {
        circuitBreakerRegistry.find(name).ifPresent(cb -> {
            cb.transitionToClosedState();
            log.info("Circuit breaker '{}' transitioned to CLOSED", name);
        });
    }

    // DTO classes for status responses
    @Data
    public static class CircuitBreakerStatus {
        private String name;
        private String state;
        private float failureRate;
        private float slowCallRate;
        private int numberOfBufferedCalls;
        private int numberOfFailedCalls;
        private int numberOfSuccessfulCalls;
        private long numberOfNotPermittedCalls;
    }

    @Data
    public static class RateLimiterStatus {
        private String name;
        private int availablePermissions;
        private int numberOfWaitingThreads;
    }

    @Data
    public static class BulkheadStatus {
        private String name;
        private int availableConcurrentCalls;
        private int maxAllowedConcurrentCalls;
    }

    @Data
    public static class ResilienceStatus {
        private boolean healthy;
        private Map<String, CircuitBreakerStatus> circuitBreakers;
        private Map<String, RateLimiterStatus> rateLimiters;
        private Map<String, BulkheadStatus> bulkheads;
    }
}
