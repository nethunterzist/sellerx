package com.ecommerce.sellerx.billing;

/**
 * Subscription lifecycle states
 *
 * State machine:
 * NEW → PENDING_PAYMENT → TRIAL → ACTIVE
 *                              ↓       ↓
 *                        payment_fail  payment_fail
 *                              ↓       ↓
 *                          PAST_DUE (3 day grace period)
 *                              ↓
 *                      3 retry failed
 *                              ↓
 *                          SUSPENDED
 *                              ↓
 *                      30 days no payment
 *                              ↓
 *                          EXPIRED
 */
public enum SubscriptionStatus {
    /** Initial state, waiting for first payment */
    PENDING_PAYMENT,

    /** Trial period active (14 days) */
    TRIAL,

    /** Subscription is active and paid */
    ACTIVE,

    /** Payment failed, in grace period (3 days) */
    PAST_DUE,

    /** Grace period expired, access suspended */
    SUSPENDED,

    /** User cancelled, will not renew at period end */
    CANCELLED,

    /** Subscription expired after prolonged non-payment */
    EXPIRED;

    /**
     * Check if subscription grants access to paid features
     */
    public boolean hasAccess() {
        return this == TRIAL || this == ACTIVE || this == PAST_DUE;
    }

    /**
     * Check if subscription can be renewed
     */
    public boolean canRenew() {
        return this == ACTIVE || this == PAST_DUE || this == SUSPENDED;
    }

    /**
     * Check if subscription can be cancelled
     */
    public boolean canCancel() {
        return this == TRIAL || this == ACTIVE || this == PAST_DUE;
    }
}
