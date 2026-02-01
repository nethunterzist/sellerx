package com.ecommerce.sellerx.orders;

import java.math.BigDecimal;

/**
 * Projection for top products by order count (admin query).
 */
public interface TopProductProjection {
    String getBarcode();
    String getTitle();
    String getStoreName();
    Long getOrderCount();
    BigDecimal getTotalRevenue();
}
