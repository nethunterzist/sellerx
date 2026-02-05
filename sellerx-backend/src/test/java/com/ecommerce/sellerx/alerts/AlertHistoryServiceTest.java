package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.auth.AuthService;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.orders.StockOrderSynchronizationService;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AlertHistoryService")
class AlertHistoryServiceTest extends BaseUnitTest {

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private AuthService authService;

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private StockOrderSynchronizationService stockSyncService;

    private AlertHistoryService alertHistoryService;

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        alertHistoryService = new AlertHistoryService(alertHistoryRepository, authService, productRepository, stockSyncService);

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);

        testStore = TestDataBuilder.completedStore(testUser).build();
        testStore.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("getUnreadAlerts")
    class GetUnreadAlerts {

        @Test
        @DisplayName("should return unread alerts for user")
        void shouldReturnUnreadAlerts() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertHistory alert1 = buildAlertHistory("Alert 1", null);
            AlertHistory alert2 = buildAlertHistory("Alert 2", null);

            when(alertHistoryRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(testUser.getId()))
                    .thenReturn(List.of(alert1, alert2));

            // When
            List<AlertHistoryDto> result = alertHistoryService.getUnreadAlerts();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("Alert 1");
            assertThat(result.get(0).getRead()).isFalse();
        }

        @Test
        @DisplayName("should return empty list when no unread alerts")
        void shouldReturnEmptyWhenNoUnread() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertHistoryRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(testUser.getId()))
                    .thenReturn(List.of());

            // When
            List<AlertHistoryDto> result = alertHistoryService.getUnreadAlerts();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("should return unread count")
        void shouldReturnUnreadCount() {
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertHistoryRepository.countByUserIdAndReadAtIsNull(testUser.getId())).thenReturn(7L);

            assertThat(alertHistoryService.getUnreadCount()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("getAlertById")
    class GetAlertById {

        @Test
        @DisplayName("should return alert when found")
        void shouldReturnAlertWhenFound() {
            // Given
            UUID alertId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertHistory alert = buildAlertHistory("Test Alert", null);
            alert.setId(alertId);
            when(alertHistoryRepository.findByIdAndUserId(alertId, testUser.getId()))
                    .thenReturn(Optional.of(alert));

            // When
            AlertHistoryDto result = alertHistoryService.getAlertById(alertId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(alertId);
            assertThat(result.getTitle()).isEqualTo("Test Alert");
        }

        @Test
        @DisplayName("should throw AlertNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            UUID alertId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertHistoryRepository.findByIdAndUserId(alertId, testUser.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertHistoryService.getAlertById(alertId))
                    .isInstanceOf(AlertNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should mark alert as read")
        void shouldMarkAlertAsRead() {
            // Given
            UUID alertId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertHistory alert = buildAlertHistory("Unread Alert", null);
            alert.setId(alertId);

            when(alertHistoryRepository.findByIdAndUserId(alertId, testUser.getId()))
                    .thenReturn(Optional.of(alert));
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> i.getArgument(0));

            // When
            AlertHistoryDto result = alertHistoryService.markAsRead(alertId);

            // Then
            assertThat(result.getRead()).isTrue();
            assertThat(alert.getReadAt()).isNotNull();
            verify(alertHistoryRepository).save(alert);
        }

        @Test
        @DisplayName("should not update readAt if already read")
        void shouldNotUpdateIfAlreadyRead() {
            // Given
            UUID alertId = UUID.randomUUID();
            LocalDateTime originalReadAt = LocalDateTime.now().minusHours(1);
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertHistory alert = buildAlertHistory("Already Read", originalReadAt);
            alert.setId(alertId);

            when(alertHistoryRepository.findByIdAndUserId(alertId, testUser.getId()))
                    .thenReturn(Optional.of(alert));
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> i.getArgument(0));

            // When
            AlertHistoryDto result = alertHistoryService.markAsRead(alertId);

            // Then
            assertThat(result.getRead()).isTrue();
            // readAt should remain the original time (markAsRead only sets if null)
            assertThat(alert.getReadAt()).isEqualTo(originalReadAt);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("should mark all alerts as read and return count")
        void shouldMarkAllAsReadAndReturnCount() {
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertHistoryRepository.markAllAsReadForUser(testUser.getId())).thenReturn(5);

            int result = alertHistoryService.markAllAsRead();

            assertThat(result).isEqualTo(5);
            verify(alertHistoryRepository).markAllAsReadForUser(testUser.getId());
        }
    }

    @Nested
    @DisplayName("getAlertStats")
    class GetAlertStats {

        @Test
        @DisplayName("should return alert statistics")
        void shouldReturnAlertStats() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertHistoryRepository.countByUserIdAndReadAtIsNull(testUser.getId())).thenReturn(3L);
            when(alertHistoryRepository.countByUserAndTypeAndSince(eq(testUser.getId()), eq(AlertType.STOCK), any()))
                    .thenReturn(2L);
            when(alertHistoryRepository.countByUserAndTypeAndSince(eq(testUser.getId()), eq(AlertType.PROFIT), any()))
                    .thenReturn(1L);
            when(alertHistoryRepository.countByUserAndTypeAndSince(eq(testUser.getId()), eq(AlertType.ORDER), any()))
                    .thenReturn(0L);
            when(alertHistoryRepository.findByUserIdAndDateRange(eq(testUser.getId()), any(), any()))
                    .thenReturn(List.of(
                            buildAlertHistory("a1", null),
                            buildAlertHistory("a2", null),
                            buildAlertHistory("a3", null),
                            buildAlertHistory("a4", null)
                    ));

            // When
            AlertStatsDto result = alertHistoryService.getAlertStats();

            // Then
            assertThat(result.getUnreadCount()).isEqualTo(3L);
            assertThat(result.getStockAlertsLast24h()).isEqualTo(2L);
            assertThat(result.getProfitAlertsLast24h()).isEqualTo(1L);
            assertThat(result.getOrderAlertsLast24h()).isEqualTo(0L);
            assertThat(result.getTotalAlertsLast7Days()).isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("getAlertsByDateRange")
    class GetAlertsByDateRange {

        @Test
        @DisplayName("should return alerts within date range")
        void shouldReturnAlertsInRange() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            LocalDateTime start = LocalDateTime.now().minusDays(7);
            LocalDateTime end = LocalDateTime.now();

            AlertHistory alert = buildAlertHistory("Range Alert", null);
            when(alertHistoryRepository.findByUserIdAndDateRange(testUser.getId(), start, end))
                    .thenReturn(List.of(alert));

            // When
            List<AlertHistoryDto> result = alertHistoryService.getAlertsByDateRange(start, end);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Range Alert");
        }
    }

    // Helper method
    private AlertHistory buildAlertHistory(String title, LocalDateTime readAt) {
        return AlertHistory.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .store(testStore)
                .alertType(AlertType.STOCK)
                .title(title)
                .message("Test message for " + title)
                .severity(AlertSeverity.MEDIUM)
                .data(Map.of("key", "value"))
                .emailSent(false)
                .pushSent(false)
                .inAppSent(true)
                .readAt(readAt)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
