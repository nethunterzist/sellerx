package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for cargo shipping cost per order number.
 * Used by ReturnAnalyticsService to get real shipping costs
 * (both outbound and return) for returned orders.
 */
public interface OrderShippingCostProjection {
    String getOrderNumber();
    BigDecimal getTotalAmount();
}
