package com.ecommerce.sellerx.stocktracking;

/**
 * Types of stock alerts that can be triggered
 */
public enum StockAlertType {
    OUT_OF_STOCK,      // Stock became zero
    LOW_STOCK,         // Stock fell below threshold
    BACK_IN_STOCK,     // Stock returned after being zero
    STOCK_INCREASED    // Significant stock increase detected
}
