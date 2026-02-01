package com.ecommerce.sellerx.billing;

/**
 * Subscription lifecycle states
 *
 * State machine:
 * PENDING_PAYMENT -> TRIAL (14 days) -> ACTIVE
 *                          |              |
 *                    payment_fail    payment_fail
 *                          |              |
 *                       PAST_DUE (3 day grace)
 *                          |
 *                    3 retries fail
 *                          |
 *                       SUSPENDED
 *                          |
 *                     30 days no pay
 *                          |
 *                       EXPIRED
 *
 * Any state can go to CANCELLED (user initiated)
 */
public enum SubscriptionStatus {
    /** Initial state - waiting for first payment */
    PENDING_PAYMENT,

    /** Trial period active - 14 days free access */
    TRIAL,

    /** Subscription is active and payment is current */
    ACTIVE,

    /** Payment failed, within grace period (3 days) */
    PAST_DUE,

    /** Grace period expired, access suspended */
    SUSPENDED,

    /** User cancelled subscription */
    CANCELLED,

    /** Long-term non-payment, subscription terminated */
    EXPIRED;

    /**
     * Check if subscription has access to features
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
        return this == TRIAL || this == ACTIVE || this == PAST_DUE || this == SUSPENDED;
    }

    /**
     * Check if subscription is in a terminal state
     */
    public boolean isTerminal() {
        return this == CANCELLED || this == EXPIRED;
    }
}
