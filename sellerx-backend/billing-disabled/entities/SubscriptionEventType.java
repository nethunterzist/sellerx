package com.ecommerce.sellerx.billing;

/**
 * Subscription event types for audit trail
 */
public enum SubscriptionEventType {
    // Lifecycle events
    CREATED,
    TRIAL_STARTED,
    TRIAL_ENDING_SOON,
    TRIAL_ENDED,
    ACTIVATED,
    RENEWED,
    UPGRADED,
    DOWNGRADED,

    // Payment events
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_RETRY_SCHEDULED,
    PAYMENT_METHOD_CHANGED,

    // Status changes
    PAST_DUE,
    GRACE_PERIOD_STARTED,
    SUSPENDED,
    REACTIVATED,
    CANCELLED,
    CANCEL_SCHEDULED,
    EXPIRED,

    // Feature events
    FEATURE_LIMIT_REACHED,
    STORE_LIMIT_REACHED
}
