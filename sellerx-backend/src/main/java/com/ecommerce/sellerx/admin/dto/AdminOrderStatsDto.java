package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderStatsDto {
    private long totalOrders;
    private long ordersToday;
    private long ordersThisWeek;
    private long ordersThisMonth;
    private Map<String, Long> statusBreakdown;
}
