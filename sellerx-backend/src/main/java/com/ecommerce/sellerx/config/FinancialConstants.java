package com.ecommerce.sellerx.config;

import java.math.BigDecimal;

/**
 * Financial constants for the application
 */
public final class FinancialConstants {
    
    /**
     * Stoppage rate as percentage (e.g., 1% = 1.0)
     * This rate is applied to total_price to calculate stoppage amount
     */
    public static final BigDecimal STOPPAGE_RATE_PERCENT = BigDecimal.valueOf(1.0);
    
    /**
     * Stoppage rate as decimal (e.g., 1% = 0.01)
     */
    public static final BigDecimal STOPPAGE_RATE_DECIMAL = STOPPAGE_RATE_PERCENT.divide(BigDecimal.valueOf(100));
    
    private FinancialConstants() {
        // Utility class
    }
}
