package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {
    
    private DashboardStatsDto today;
    private DashboardStatsDto yesterday;
    private DashboardStatsDto thisMonth;
    private DashboardStatsDto lastMonth;
    
    // Summary info
    private String storeId;
    private String calculatedAt;
}
