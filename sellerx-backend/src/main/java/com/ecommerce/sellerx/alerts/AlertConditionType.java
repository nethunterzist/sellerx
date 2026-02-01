package com.ecommerce.sellerx.alerts;

/**
 * Condition types for alert rule evaluation.
 */
public enum AlertConditionType {
    /**
     * Value is below threshold (e.g., stock < 10)
     */
    BELOW,

    /**
     * Value is above threshold (e.g., profit margin > 20%)
     */
    ABOVE,

    /**
     * Value equals threshold exactly
     */
    EQUALS,

    /**
     * Value has changed from previous state
     */
    CHANGED,

    /**
     * Value is zero (special case for stock)
     */
    ZERO
}
