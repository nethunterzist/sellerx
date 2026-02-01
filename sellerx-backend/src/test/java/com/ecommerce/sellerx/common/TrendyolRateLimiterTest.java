package com.ecommerce.sellerx.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TrendyolRateLimiter.
 * Tests rate limiting behavior for Trendyol API calls.
 */
@DisplayName("TrendyolRateLimiter")
class TrendyolRateLimiterTest extends BaseUnitTest {

    private TrendyolRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new TrendyolRateLimiter();
    }

    @Nested
    @DisplayName("acquire")
    class Acquire {

        @Test
        @DisplayName("should return wait time for first permit")
        void shouldReturnWaitTimeForFirstPermit() {
            // When
            double waitTime = rateLimiter.acquire();

            // Then
            assertThat(waitTime)
                    .as("First permit should have minimal wait time")
                    .isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("should allow acquiring multiple permits")
        void shouldAllowAcquiringMultiplePermits() {
            // When/Then - should not throw
            for (int i = 0; i < 5; i++) {
                double waitTime = rateLimiter.acquire();
                assertThat(waitTime).isGreaterThanOrEqualTo(0.0);
            }
        }
    }

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquire {

        @Test
        @DisplayName("should successfully acquire first permit without blocking")
        void shouldAcquireFirstPermitWithoutBlocking() {
            // When
            boolean acquired = rateLimiter.tryAcquire();

            // Then
            assertThat(acquired)
                    .as("First tryAcquire should succeed")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("getRate")
    class GetRate {

        @Test
        @DisplayName("should return configured rate of 10 requests per second")
        void shouldReturnConfiguredRate() {
            // When
            double rate = rateLimiter.getRate();

            // Then
            assertThat(rate)
                    .as("Rate should be 10.0 requests per second (Trendyol limit)")
                    .isEqualTo(10.0);
        }
    }
}
