package com.ecommerce.sellerx.orders;

import lombok.Builder;

@Builder
public record OrderStatistics(
    long totalOrders,
    long deliveredOrders,
    long returnedOrders
) {}
