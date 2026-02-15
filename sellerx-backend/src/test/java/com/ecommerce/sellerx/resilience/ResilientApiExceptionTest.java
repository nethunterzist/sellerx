package com.ecommerce.sellerx.resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResilientApiException")
class ResilientApiExceptionTest {

    @Nested
    @DisplayName("isRetryable()")
    class IsRetryable {

        @Test
        @DisplayName("should return true for CIRCUIT_OPEN")
        void shouldReturnTrueForCircuitOpen() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.CIRCUIT_OPEN,
                "Circuit is open"
            );

            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("should return true for RATE_LIMIT_EXCEEDED")
        void shouldReturnTrueForRateLimitExceeded() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.RATE_LIMIT_EXCEEDED,
                "Rate limit exceeded"
            );

            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("should return true for BULKHEAD_FULL")
        void shouldReturnTrueForBulkheadFull() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.BULKHEAD_FULL,
                "Bulkhead full"
            );

            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("should return true for TIMEOUT")
        void shouldReturnTrueForTimeout() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.TIMEOUT,
                "Operation timed out"
            );

            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("should return false for AUTH_ERROR")
        void shouldReturnFalseForAuthError() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.AUTH_ERROR,
                "Authentication failed"
            );

            assertThat(exception.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("should return false for ERROR")
        void shouldReturnFalseForError() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.ERROR,
                "Generic error"
            );

            assertThat(exception.isRetryable()).isFalse();
        }
    }

    @Nested
    @DisplayName("getSuggestedWaitMs()")
    class GetSuggestedWaitMs {

        @Test
        @DisplayName("should return 30s for CIRCUIT_OPEN")
        void shouldReturn30sForCircuitOpen() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.CIRCUIT_OPEN,
                "Circuit is open"
            );

            assertThat(exception.getSuggestedWaitMs()).isEqualTo(30_000);
        }

        @Test
        @DisplayName("should return 1s for RATE_LIMIT_EXCEEDED")
        void shouldReturn1sForRateLimitExceeded() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.RATE_LIMIT_EXCEEDED,
                "Rate limit exceeded"
            );

            assertThat(exception.getSuggestedWaitMs()).isEqualTo(1_000);
        }

        @Test
        @DisplayName("should return 500ms for BULKHEAD_FULL")
        void shouldReturn500msForBulkheadFull() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.BULKHEAD_FULL,
                "Bulkhead full"
            );

            assertThat(exception.getSuggestedWaitMs()).isEqualTo(500);
        }

        @Test
        @DisplayName("should return 5s for TIMEOUT")
        void shouldReturn5sForTimeout() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.TIMEOUT,
                "Operation timed out"
            );

            assertThat(exception.getSuggestedWaitMs()).isEqualTo(5_000);
        }

        @Test
        @DisplayName("should return 0 for AUTH_ERROR")
        void shouldReturn0ForAuthError() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.AUTH_ERROR,
                "Auth failed"
            );

            assertThat(exception.getSuggestedWaitMs()).isZero();
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("should format correctly")
        void shouldFormatCorrectly() {
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.CIRCUIT_OPEN,
                "Service unavailable"
            );

            assertThat(exception.toString())
                .isEqualTo("ResilientApiException{type=CIRCUIT_OPEN, message='Service unavailable'}");
        }
    }

    @Nested
    @DisplayName("Constructor with cause")
    class ConstructorWithCause {

        @Test
        @DisplayName("should preserve cause")
        void shouldPreserveCause() {
            Exception cause = new RuntimeException("Original error");
            ResilientApiException exception = new ResilientApiException(
                ResilientApiException.FailureType.ERROR,
                "Wrapped error",
                cause
            );

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getMessage()).isEqualTo("Wrapped error");
            assertThat(exception.getFailureType()).isEqualTo(ResilientApiException.FailureType.ERROR);
        }
    }
}
