package com.ecommerce.sellerx.resilience;

import lombok.Getter;

/**
 * Exception thrown when resilience patterns reject a request.
 */
@Getter
public class ResilientApiException extends RuntimeException {

    private final FailureType failureType;

    public ResilientApiException(FailureType failureType, String message) {
        super(message);
        this.failureType = failureType;
    }

    public ResilientApiException(FailureType failureType, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
    }

    /**
     * Types of resilience-related failures.
     */
    public enum FailureType {
        /**
         * Circuit breaker is open - too many failures.
         */
        CIRCUIT_OPEN,

        /**
         * Rate limit exceeded - too many requests per second.
         */
        RATE_LIMIT_EXCEEDED,

        /**
         * Bulkhead full - too many concurrent operations.
         */
        BULKHEAD_FULL,

        /**
         * Operation timed out.
         */
        TIMEOUT,

        /**
         * All retry attempts exhausted.
         */
        RETRY_EXHAUSTED,

        /**
         * Authentication error (not retried).
         */
        AUTH_ERROR,

        /**
         * Generic error.
         */
        ERROR
    }

    /**
     * Check if the error is retryable (worth trying again later).
     */
    public boolean isRetryable() {
        return failureType == FailureType.CIRCUIT_OPEN ||
               failureType == FailureType.RATE_LIMIT_EXCEEDED ||
               failureType == FailureType.BULKHEAD_FULL ||
               failureType == FailureType.TIMEOUT;
    }

    /**
     * Suggested wait time in milliseconds before retrying.
     */
    public long getSuggestedWaitMs() {
        return switch (failureType) {
            case CIRCUIT_OPEN -> 30_000;      // Wait 30s for circuit to half-open
            case RATE_LIMIT_EXCEEDED -> 1_000; // Wait 1s for rate limit reset
            case BULKHEAD_FULL -> 500;         // Wait 500ms for slot
            case TIMEOUT -> 5_000;             // Wait 5s before retry
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return String.format("ResilientApiException{type=%s, message='%s'}",
            failureType, getMessage());
    }
}
