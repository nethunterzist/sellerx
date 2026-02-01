package com.ecommerce.sellerx.billing;

/**
 * Subscription event types for audit trail
 */
public enum SubscriptionEventType {
    /** Subscription created */
    CREATED,

    /** Trial period started */
    TRIAL_STARTED,

    /** Trial period ended */
    TRIAL_ENDED,

    /** Subscription activated (first payment successful) */
    ACTIVATED,

    /** Plan upgraded */
    UPGRADED,

    /** Plan downgraded */
    DOWNGRADED,

    /** Payment failed */
    PAYMENT_FAILED,

    /** Payment succeeded */
    PAYMENT_SUCCEEDED,

    /** Entered past due state */
    PAST_DUE,

    /** Subscription suspended */
    SUSPENDED,

    /** Subscription reactivated from suspended state */
    REACTIVATED,

    /** Subscription cancelled by user */
    CANCELLED,

    /** Subscription expired */
    EXPIRED,

    /** Subscription renewed */
    RENEWED
}
