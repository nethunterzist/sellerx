package com.ecommerce.sellerx.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResilienceMetricsService")
class ResilienceMetricsServiceTest {

    private ResilienceMetricsService service;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RateLimiterRegistry rateLimiterRegistry;
    private RetryRegistry retryRegistry;
    private BulkheadRegistry bulkheadRegistry;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .build();

        circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);
        rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();
        bulkheadRegistry = BulkheadRegistry.ofDefaults();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        // Create some circuit breakers for testing
        circuitBreakerRegistry.circuitBreaker("testApi");
        circuitBreakerRegistry.circuitBreaker("testSync");

        // Create some rate limiters
        rateLimiterRegistry.rateLimiter("testApi");

        // Create some bulkheads
        bulkheadRegistry.bulkhead("testSync");

        service = new ResilienceMetricsService(
            circuitBreakerRegistry,
            rateLimiterRegistry,
            retryRegistry,
            bulkheadRegistry,
            meterRegistry
        );
    }

    @Nested
    @DisplayName("getCircuitBreakerStatus()")
    class GetCircuitBreakerStatus {

        @Test
        @DisplayName("should return status for all circuit breakers")
        void shouldReturnStatusForAllCircuitBreakers() {
            Map<String, ResilienceMetricsService.CircuitBreakerStatus> status =
                service.getCircuitBreakerStatus();

            assertThat(status).containsKeys("testApi", "testSync");
            assertThat(status.get("testApi").getState()).isEqualTo("CLOSED");
            assertThat(status.get("testSync").getState()).isEqualTo("CLOSED");
        }

        @Test
        @DisplayName("should show OPEN state when circuit is open")
        void shouldShowOpenStateWhenCircuitIsOpen() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testApi");
            cb.transitionToOpenState();

            Map<String, ResilienceMetricsService.CircuitBreakerStatus> status =
                service.getCircuitBreakerStatus();

            assertThat(status.get("testApi").getState()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("should track failure count")
        void shouldTrackFailureCount() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testApi");

            // Record some failures
            for (int i = 0; i < 3; i++) {
                cb.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("test"));
            }

            Map<String, ResilienceMetricsService.CircuitBreakerStatus> status =
                service.getCircuitBreakerStatus();

            assertThat(status.get("testApi").getNumberOfFailedCalls()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getRateLimiterStatus()")
    class GetRateLimiterStatus {

        @Test
        @DisplayName("should return status for all rate limiters")
        void shouldReturnStatusForAllRateLimiters() {
            Map<String, ResilienceMetricsService.RateLimiterStatus> status =
                service.getRateLimiterStatus();

            assertThat(status).containsKey("testApi");
        }

        @Test
        @DisplayName("should show available permissions")
        void shouldShowAvailablePermissions() {
            Map<String, ResilienceMetricsService.RateLimiterStatus> status =
                service.getRateLimiterStatus();

            // Default rate limiter has 50 permits per period
            assertThat(status.get("testApi").getAvailablePermissions()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("getBulkheadStatus()")
    class GetBulkheadStatus {

        @Test
        @DisplayName("should return status for all bulkheads")
        void shouldReturnStatusForAllBulkheads() {
            Map<String, ResilienceMetricsService.BulkheadStatus> status =
                service.getBulkheadStatus();

            assertThat(status).containsKey("testSync");
        }

        @Test
        @DisplayName("should show max concurrent calls")
        void shouldShowMaxConcurrentCalls() {
            Map<String, ResilienceMetricsService.BulkheadStatus> status =
                service.getBulkheadStatus();

            assertThat(status.get("testSync").getMaxAllowedConcurrentCalls()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("getFullStatus()")
    class GetFullStatus {

        @Test
        @DisplayName("should return combined status")
        void shouldReturnCombinedStatus() {
            ResilienceMetricsService.ResilienceStatus status = service.getFullStatus();

            assertThat(status.getCircuitBreakers()).isNotEmpty();
            assertThat(status.getRateLimiters()).isNotEmpty();
            assertThat(status.getBulkheads()).isNotEmpty();
        }

        @Test
        @DisplayName("should be healthy when no circuits open")
        void shouldBeHealthyWhenNoCircuitsOpen() {
            ResilienceMetricsService.ResilienceStatus status = service.getFullStatus();

            assertThat(status.isHealthy()).isTrue();
        }

        @Test
        @DisplayName("should be unhealthy when circuit is open")
        void shouldBeUnhealthyWhenCircuitIsOpen() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testApi");
            cb.transitionToOpenState();

            ResilienceMetricsService.ResilienceStatus status = service.getFullStatus();

            assertThat(status.isHealthy()).isFalse();
        }
    }

    @Nested
    @DisplayName("resetCircuitBreaker()")
    class ResetCircuitBreaker {

        @Test
        @DisplayName("should reset circuit breaker state")
        void shouldResetCircuitBreakerState() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testApi");
            cb.transitionToOpenState();
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            service.resetCircuitBreaker("testApi");

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("should do nothing for non-existent circuit breaker")
        void shouldDoNothingForNonExistentCircuitBreaker() {
            // Should not throw exception
            service.resetCircuitBreaker("nonExistent");
        }
    }

    @Nested
    @DisplayName("closeCircuitBreaker()")
    class CloseCircuitBreaker {

        @Test
        @DisplayName("should transition circuit breaker to closed")
        void shouldTransitionCircuitBreakerToClosed() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testApi");
            cb.transitionToOpenState();

            service.closeCircuitBreaker("testApi");

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }
}
