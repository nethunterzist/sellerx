package com.ecommerce.sellerx.stocktracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTrackingDashboardDto {
    // Counts
    private int totalTrackedProducts;
    private int activeTrackedProducts;
    private int outOfStockProducts;
    private int lowStockProducts;

    // Alert counts (last 24h)
    private int outOfStockAlertsToday;
    private int lowStockAlertsToday;
    private int backInStockAlertsToday;
    private int totalUnreadAlerts;

    // Recent alerts
    private List<StockAlertDto> recentAlerts;

    // Products needing attention
    private List<TrackedProductDto> outOfStockList;
    private List<TrackedProductDto> lowStockList;
}
