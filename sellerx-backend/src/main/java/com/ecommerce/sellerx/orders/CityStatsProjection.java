package com.ecommerce.sellerx.orders;

import java.math.BigDecimal;

/**
 * Projection for city statistics from order aggregation.
 */
public interface CityStatsProjection {
    String getCity();
    Integer getCityCode();
    Long getOrderCount();
    BigDecimal getTotalRevenue();
    Long getTotalQuantity();
}
