package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminDashboardStatsDto;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.stores.WebhookStatus;
import com.ecommerce.sellerx.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TrendyolOrderRepository orderRepository;

    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        adminDashboardService = new AdminDashboardService(userRepository, storeRepository, orderRepository);
    }

    @Nested
    @DisplayName("getDashboardStats")
    class GetDashboardStats {

        @Test
        @DisplayName("should return dashboard stats with correct user counts")
        void shouldReturnCorrectUserCounts() {
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByCreatedAtAfter(any(LocalDateTime.class)))
                    .thenReturn(5L)   // today
                    .thenReturn(20L)  // this week
                    .thenReturn(50L); // this month
            when(userRepository.countByLastLoginAtAfter(any(LocalDateTime.class))).thenReturn(80L);
            when(storeRepository.findAll()).thenReturn(Collections.emptyList());
            when(orderRepository.count()).thenReturn(0L);
            when(orderRepository.countAllOrdersBetween(any(), any())).thenReturn(0L);

            AdminDashboardStatsDto result = adminDashboardService.getDashboardStats();

            assertThat(result.getTotalUsers()).isEqualTo(100L);
            assertThat(result.getNewUsersToday()).isEqualTo(5L);
            assertThat(result.getNewUsersThisWeek()).isEqualTo(20L);
            assertThat(result.getNewUsersThisMonth()).isEqualTo(50L);
            assertThat(result.getActiveUsersLast30Days()).isEqualTo(80L);
        }

        @Test
        @DisplayName("should calculate store stats correctly")
        void shouldCalculateStoreStats() {
            when(userRepository.count()).thenReturn(10L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(0L);
            when(userRepository.countByLastLoginAtAfter(any())).thenReturn(0L);
            when(orderRepository.count()).thenReturn(0L);
            when(orderRepository.countAllOrdersBetween(any(), any())).thenReturn(0L);

            Store activeStore = Store.builder()
                    .storeName("Active")
                    .marketplace("trendyol")
                    .initialSyncCompleted(true)
                    .syncStatus(SyncStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            activeStore.setId(UUID.randomUUID());

            Store failedStore = Store.builder()
                    .storeName("Failed")
                    .marketplace("trendyol")
                    .initialSyncCompleted(false)
                    .syncStatus(SyncStatus.FAILED)
                    .syncErrorMessage("Connection error")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            failedStore.setId(UUID.randomUUID());

            Store webhookErrorStore = Store.builder()
                    .storeName("Webhook Error")
                    .marketplace("trendyol")
                    .initialSyncCompleted(true)
                    .syncStatus(SyncStatus.COMPLETED)
                    .webhookStatus(WebhookStatus.FAILED)
                    .webhookErrorMessage("Timeout")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            webhookErrorStore.setId(UUID.randomUUID());

            when(storeRepository.findAll()).thenReturn(List.of(activeStore, failedStore, webhookErrorStore));

            AdminDashboardStatsDto result = adminDashboardService.getDashboardStats();

            assertThat(result.getTotalStores()).isEqualTo(3L);
            assertThat(result.getActiveStores()).isEqualTo(2L); // activeStore + webhookErrorStore
            assertThat(result.getStoresWithSyncErrors()).isEqualTo(1L); // failedStore
            assertThat(result.getStoresWithWebhookErrors()).isEqualTo(1L); // webhookErrorStore
        }

        @Test
        @DisplayName("should return order statistics")
        void shouldReturnOrderStatistics() {
            when(userRepository.count()).thenReturn(0L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(0L);
            when(userRepository.countByLastLoginAtAfter(any())).thenReturn(0L);
            when(storeRepository.findAll()).thenReturn(Collections.emptyList());

            when(orderRepository.count()).thenReturn(1000L);
            when(orderRepository.countAllOrdersBetween(any(), any()))
                    .thenReturn(50L)   // today
                    .thenReturn(200L)  // this week
                    .thenReturn(500L); // this month

            AdminDashboardStatsDto result = adminDashboardService.getDashboardStats();

            assertThat(result.getTotalOrders()).isEqualTo(1000L);
            assertThat(result.getOrdersToday()).isEqualTo(50L);
            assertThat(result.getOrdersThisWeek()).isEqualTo(200L);
            assertThat(result.getOrdersThisMonth()).isEqualTo(500L);
        }

        @Test
        @DisplayName("should return empty recent activities list")
        void shouldReturnEmptyRecentActivities() {
            when(userRepository.count()).thenReturn(0L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(0L);
            when(userRepository.countByLastLoginAtAfter(any())).thenReturn(0L);
            when(storeRepository.findAll()).thenReturn(Collections.emptyList());
            when(orderRepository.count()).thenReturn(0L);
            when(orderRepository.countAllOrdersBetween(any(), any())).thenReturn(0L);

            AdminDashboardStatsDto result = adminDashboardService.getDashboardStats();

            assertThat(result.getRecentActivities()).isNotNull().isEmpty();
        }
    }
}
