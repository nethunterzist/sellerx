package com.ecommerce.sellerx.stocktracking;

/**
 * Severity levels for stock alerts
 */
public enum StockAlertSeverity {
    LOW,       // Informational (e.g., stock increased)
    MEDIUM,    // Notable change (e.g., back in stock)
    HIGH,      // Important (e.g., low stock)
    CRITICAL   // Urgent (e.g., out of stock)
}
