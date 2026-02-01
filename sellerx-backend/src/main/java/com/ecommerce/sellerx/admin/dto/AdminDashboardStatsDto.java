package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDto {
    // User stats
    private long totalUsers;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
    private long activeUsersLast30Days;

    // Store stats
    private long totalStores;
    private long activeStores; // initialSyncCompleted = true
    private long storesWithSyncErrors;
    private long storesWithWebhookErrors;

    // Sync stats
    private long pendingSyncs;
    private long activeSyncs;
    private long completedSyncsToday;
    private long failedSyncsToday;

    // Order stats
    private long totalOrders;
    private long ordersToday;
    private long ordersThisWeek;
    private long ordersThisMonth;

    // Revenue stats (optional - if billing is active)
    private BigDecimal mrr; // Monthly Recurring Revenue
    private long activeSubscriptions;
    private long trialUsers;

    // Recent activity
    private List<RecentActivity> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type; // USER_REGISTERED, STORE_CREATED, SYNC_COMPLETED, SYNC_FAILED
        private String description;
        private String timestamp;
        private Long userId;
        private String userEmail;
    }
}
