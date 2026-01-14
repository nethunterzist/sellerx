package com.ecommerce.sellerx.products;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

/**
 * Test class for BigDecimal comparison logic used in TrendyolProductService
 * This ensures that decimal values are compared correctly regardless of scale differences
 */
class BigDecimalComparisonTest {

    /**
     * Helper method that mimics the isBigDecimalChanged method from TrendyolProductService
     */
    private boolean isBigDecimalChanged(BigDecimal existing, BigDecimal incoming) {
        if (existing == null && incoming == null) {
            return false;
        }
        if (existing == null || incoming == null) {
            return true;
        }
        return existing.compareTo(incoming) != 0;
    }

    @Test
    void testSameValuesWithSameScale() {
        BigDecimal a1 = new BigDecimal("1150.69");
        BigDecimal a2 = new BigDecimal("1150.69");
        
        assertFalse(isBigDecimalChanged(a1, a2), 
            "Same values with same scale should not be considered changed");
    }

    @Test
    void testSameValuesWithDifferentScale() {
        BigDecimal b1 = new BigDecimal("1150.69");
        BigDecimal b2 = new BigDecimal("1150.690");
        
        assertFalse(isBigDecimalChanged(b1, b2), 
            "Same values with different scale should not be considered changed");
    }

    @Test
    void testDifferentValues() {
        BigDecimal c1 = new BigDecimal("1150.69");
        BigDecimal c2 = new BigDecimal("1150.70");
        
        assertTrue(isBigDecimalChanged(c1, c2), 
            "Different values should be considered changed");
    }

    @Test
    void testNullValues() {
        // Both null
        assertFalse(isBigDecimalChanged(null, null), 
            "Both null values should not be considered changed");
        
        // One null
        BigDecimal value = new BigDecimal("1150.69");
        assertTrue(isBigDecimalChanged(null, value), 
            "Null to non-null should be considered changed");
        assertTrue(isBigDecimalChanged(value, null), 
            "Non-null to null should be considered changed");
    }

    @Test
    void testZeroValues() {
        BigDecimal zero1 = new BigDecimal("0.00");
        BigDecimal zero2 = new BigDecimal("0");
        
        assertFalse(isBigDecimalChanged(zero1, zero2), 
            "Zero values with different scale should not be considered changed");
    }

    @Test
    void testCommonTrendyolPrices() {
        // Test common price scenarios from Trendyol
        BigDecimal price1 = new BigDecimal("850.00");  // Database format
        BigDecimal price2 = new BigDecimal("850");     // API format
        
        assertFalse(isBigDecimalChanged(price1, price2), 
            "850.00 and 850 should be considered same (common Trendyol scenario)");

        BigDecimal weight1 = new BigDecimal("0.00");   // Database format
        BigDecimal weight2 = new BigDecimal("0");      // API format
        
        assertFalse(isBigDecimalChanged(weight1, weight2), 
            "0.00 and 0 should be considered same (common dimensional weight scenario)");
    }

    @Test
    void testRealDecimalPrices() {
        // Test with real decimal prices like we found in the database
        BigDecimal realPrice1 = new BigDecimal("1026.19");
        BigDecimal realPrice2 = new BigDecimal("1026.19");
        
        assertFalse(isBigDecimalChanged(realPrice1, realPrice2), 
            "Real decimal prices should be compared correctly");

        BigDecimal realPrice3 = new BigDecimal("967.50");
        BigDecimal realPrice4 = new BigDecimal("967.5");
        
        assertFalse(isBigDecimalChanged(realPrice3, realPrice4), 
            "967.50 and 967.5 should be considered same");
    }

    @Test
    void testEqualsVsCompareTo() {
        // This test demonstrates why we use compareTo instead of equals
        BigDecimal d1 = new BigDecimal("1150.69");
        BigDecimal d2 = new BigDecimal("1150.690");
        
        // equals() considers scale, so it returns false
        assertFalse(d1.equals(d2), 
            "equals() should return false for different scales");
        
        // compareTo() ignores scale, so it returns 0 (equal)
        assertEquals(0, d1.compareTo(d2), 
            "compareTo() should return 0 for same values with different scales");
        
        // Our method should use compareTo logic
        assertFalse(isBigDecimalChanged(d1, d2), 
            "Our method should consider them as unchanged");
    }
}
