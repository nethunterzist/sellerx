package com.ecommerce.sellerx.alerts;

/**
 * Types of alerts that can be configured by users.
 */
public enum AlertType {
    /**
     * Stock-related alerts (low stock, out of stock, rapid decrease)
     */
    STOCK,

    /**
     * Profit margin alerts (below threshold, negative profit)
     */
    PROFIT,

    /**
     * Price-related alerts (commission changes, cost updates needed)
     */
    PRICE,

    /**
     * Order-related alerts (new orders, cancellations, returns)
     */
    ORDER,

    /**
     * System alerts (sync failures, API errors)
     */
    SYSTEM
}
