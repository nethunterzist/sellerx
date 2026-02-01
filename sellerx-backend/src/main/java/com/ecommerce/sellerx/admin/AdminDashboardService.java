package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminDashboardStatsDto;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.stores.OverallSyncStatus;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.stores.WebhookStatus;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final TrendyolOrderRepository orderRepository;

    /**
     * Get dashboard statistics for admin panel
     */
    public AdminDashboardStatsDto getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.with(LocalTime.MIN);
        LocalDateTime weekStart = now.minusDays(7).with(LocalTime.MIN);
        LocalDateTime monthStart = now.minusDays(30).with(LocalTime.MIN);

        // User stats - real queries using createdAt and lastLoginAt
        long totalUsers = userRepository.count();
        long newUsersToday = userRepository.countByCreatedAtAfter(todayStart);
        long newUsersThisWeek = userRepository.countByCreatedAtAfter(weekStart);
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(monthStart);
        long activeUsersLast30Days = userRepository.countByLastLoginAtAfter(monthStart);

        // Store stats
        List<Store> allStores = storeRepository.findAll();
        long totalStores = allStores.size();
        long activeStores = allStores.stream()
                .filter(s -> Boolean.TRUE.equals(s.getInitialSyncCompleted()))
                .count();
        long storesWithSyncErrors = allStores.stream()
                .filter(s -> SyncStatus.FAILED == s.getSyncStatus() || s.getSyncErrorMessage() != null)
                .count();
        long storesWithWebhookErrors = allStores.stream()
                .filter(s -> WebhookStatus.FAILED == s.getWebhookStatus() || s.getWebhookErrorMessage() != null)
                .count();

        // Sync stats
        long pendingSyncs = allStores.stream()
                .filter(s -> SyncStatus.PENDING == s.getSyncStatus())
                .count();
        long activeSyncs = allStores.stream()
                .filter(s -> s.getSyncStatus() != null &&
                        (s.getSyncStatus().name().startsWith("SYNCING") || OverallSyncStatus.IN_PROGRESS == s.getOverallSyncStatus()))
                .count();

        // Order stats - real platform-wide queries
        long totalOrders = orderRepository.count();
        long ordersToday = orderRepository.countAllOrdersBetween(todayStart, now);
        long ordersThisWeek = orderRepository.countAllOrdersBetween(weekStart, now);
        long ordersThisMonth = orderRepository.countAllOrdersBetween(monthStart, now);

        // Recent activities (simplified - can be enhanced with activity_logs)
        List<AdminDashboardStatsDto.RecentActivity> recentActivities = new ArrayList<>();

        return AdminDashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .activeUsersLast30Days(activeUsersLast30Days)
                .totalStores(totalStores)
                .activeStores(activeStores)
                .storesWithSyncErrors(storesWithSyncErrors)
                .storesWithWebhookErrors(storesWithWebhookErrors)
                .pendingSyncs(pendingSyncs)
                .activeSyncs(activeSyncs)
                .completedSyncsToday(0L) // Would need sync history tracking
                .failedSyncsToday(storesWithSyncErrors)
                .totalOrders(totalOrders)
                .ordersToday(ordersToday)
                .ordersThisWeek(ordersThisWeek)
                .ordersThisMonth(ordersThisMonth)
                .mrr(null) // Billing module disabled
                .activeSubscriptions(0L) // Billing module disabled
                .trialUsers(0L) // Billing module disabled
                .recentActivities(recentActivities)
                .build();
    }
}
