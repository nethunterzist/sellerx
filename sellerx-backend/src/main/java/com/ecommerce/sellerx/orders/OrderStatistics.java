package com.ecommerce.sellerx.orders;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderStatistics(
    long totalOrders,
    long pendingOrders,
    long shippedOrders,
    long deliveredOrders,
    long cancelledOrders,
    long returnedOrders,
    BigDecimal totalRevenue,
    BigDecimal averageOrderValue
) {}
