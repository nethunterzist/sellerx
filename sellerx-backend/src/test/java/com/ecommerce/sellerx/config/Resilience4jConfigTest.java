package com.ecommerce.sellerx.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resilience4jConfig")
class Resilience4jConfigTest {

    private Resilience4jConfig config;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RateLimiterRegistry rateLimiterRegistry;
    private RetryRegistry retryRegistry;
    private BulkheadRegistry bulkheadRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();
        bulkheadRegistry = BulkheadRegistry.ofDefaults();
        config = new Resilience4jConfig();
    }

    @Nested
    @DisplayName("Circuit Breakers")
    class CircuitBreakers {

        @Test
        @DisplayName("should create trendyolApi circuit breaker")
        void shouldCreateTrendyolApiCircuitBreaker() {
            CircuitBreaker cb = config.trendyolApiCircuitBreaker(circuitBreakerRegistry);

            assertThat(cb).isNotNull();
            assertThat(cb.getName()).isEqualTo("trendyolApi");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("should create trendyolSync circuit breaker")
        void shouldCreateTrendyolSyncCircuitBreaker() {
            CircuitBreaker cb = config.trendyolSyncCircuitBreaker(circuitBreakerRegistry);

            assertThat(cb).isNotNull();
            assertThat(cb.getName()).isEqualTo("trendyolSync");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }

    @Nested
    @DisplayName("Rate Limiters")
    class RateLimiters {

        @Test
        @DisplayName("should create trendyolApi rate limiter")
        void shouldCreateTrendyolApiRateLimiter() {
            RateLimiter rl = config.trendyolApiRateLimiter(rateLimiterRegistry);

            assertThat(rl).isNotNull();
            assertThat(rl.getName()).isEqualTo("trendyolApi");
        }
    }

    @Nested
    @DisplayName("Retries")
    class Retries {

        @Test
        @DisplayName("should create trendyolApi retry")
        void shouldCreateTrendyolApiRetry() {
            Retry retry = config.trendyolApiRetry(retryRegistry);

            assertThat(retry).isNotNull();
            assertThat(retry.getName()).isEqualTo("trendyolApi");
        }

        @Test
        @DisplayName("should create trendyolSync retry")
        void shouldCreateTrendyolSyncRetry() {
            Retry retry = config.trendyolSyncRetry(retryRegistry);

            assertThat(retry).isNotNull();
            assertThat(retry.getName()).isEqualTo("trendyolSync");
        }
    }

    @Nested
    @DisplayName("Bulkheads")
    class Bulkheads {

        @Test
        @DisplayName("should create trendyolSync bulkhead")
        void shouldCreateTrendyolSyncBulkhead() {
            Bulkhead bh = config.trendyolSyncBulkhead(bulkheadRegistry);

            assertThat(bh).isNotNull();
            assertThat(bh.getName()).isEqualTo("trendyolSync");
        }

        @Test
        @DisplayName("should create trendyolApi bulkhead")
        void shouldCreateTrendyolApiBulkhead() {
            Bulkhead bh = config.trendyolApiBulkhead(bulkheadRegistry);

            assertThat(bh).isNotNull();
            assertThat(bh.getName()).isEqualTo("trendyolApi");
        }
    }

    @Nested
    @DisplayName("ResilienceMetricsService")
    class ResilienceMetricsServiceTests {

        @Test
        @DisplayName("should create metrics service")
        void shouldCreateMetricsService() {
            SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

            ResilienceMetricsService service = config.resilienceMetricsService(
                circuitBreakerRegistry,
                rateLimiterRegistry,
                retryRegistry,
                bulkheadRegistry,
                meterRegistry
            );

            assertThat(service).isNotNull();
        }
    }
}
