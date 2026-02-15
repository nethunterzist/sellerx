package com.ecommerce.sellerx.websocket;

import com.ecommerce.sellerx.alerts.*;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertNotificationService.
 */
@ExtendWith(MockitoExtension.class)
class AlertNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AlertNotificationService notificationService;

    private User testUser;
    private Store testStore;
    private AlertHistory testAlert;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(123L);
        testUser.setEmail("test@example.com");

        testStore = new Store();
        testStore.setId(UUID.randomUUID());
        testStore.setStoreName("Test Store");

        testAlert = AlertHistory.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .store(testStore)
                .alertType(AlertType.STOCK)
                .title("Test Alert")
                .message("Test message")
                .severity(AlertSeverity.MEDIUM)
                .data(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("Should push alert to user via WebSocket")
    void shouldPushAlertToUser() {
        // When
        notificationService.pushAlert(testAlert);

        // Then
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("123"),
                eq("/queue/alerts"),
                payloadCaptor.capture()
        );

        Object payload = payloadCaptor.getValue();
        assertThat(payload).isNotNull();
    }

    @Test
    @DisplayName("Should not push alert when user is null")
    void shouldNotPushAlertWhenUserIsNull() {
        // Given
        testAlert = AlertHistory.builder()
                .id(UUID.randomUUID())
                .user(null)
                .alertType(AlertType.STOCK)
                .title("Test Alert")
                .message("Test message")
                .severity(AlertSeverity.MEDIUM)
                .build();

        // When
        notificationService.pushAlert(testAlert);

        // Then
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(), anyString(), any()
        );
    }

    @Test
    @DisplayName("Should push unread count to user")
    void shouldPushUnreadCountToUser() {
        // When
        notificationService.pushUnreadCount(123L, 5);

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("123"),
                eq("/queue/alerts/count"),
                payloadCaptor.capture()
        );

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsEntry("unreadCount", 5L);
        assertThat(payload).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should push sync progress to user")
    void shouldPushSyncProgressToUser() {
        // Given
        UUID storeId = UUID.randomUUID();

        // When
        notificationService.pushSyncProgress(
                123L, storeId, "PRODUCTS", 75, "Processing products..."
        );

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("123"),
                eq("/queue/sync/progress"),
                payloadCaptor.capture()
        );

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsEntry("storeId", storeId.toString());
        assertThat(payload).containsEntry("syncType", "PRODUCTS");
        assertThat(payload).containsEntry("progress", 75);
        assertThat(payload).containsEntry("status", "Processing products...");
        assertThat(payload).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should push sync complete to user")
    void shouldPushSyncCompleteToUser() {
        // Given
        UUID storeId = UUID.randomUUID();

        // When
        notificationService.pushSyncComplete(123L, storeId, "ORDERS", 1500);

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("123"),
                eq("/queue/sync/complete"),
                payloadCaptor.capture()
        );

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsEntry("storeId", storeId.toString());
        assertThat(payload).containsEntry("syncType", "ORDERS");
        assertThat(payload).containsEntry("status", "COMPLETED");
        assertThat(payload).containsEntry("itemsProcessed", 1500);
        assertThat(payload).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should broadcast system notification to all users")
    void shouldBroadcastSystemNotification() {
        // When
        notificationService.broadcastSystemNotification("System Update", "Maintenance scheduled");

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/system"),
                payloadCaptor.capture()
        );

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsEntry("title", "System Update");
        assertThat(payload).containsEntry("message", "Maintenance scheduled");
        assertThat(payload).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should handle exception gracefully when pushing alert")
    void shouldHandleExceptionGracefullyWhenPushingAlert() {
        // Given
        doThrow(new RuntimeException("WebSocket error"))
                .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // When - should not throw
        notificationService.pushAlert(testAlert);

        // Then - exception is caught and logged
        verify(messagingTemplate).convertAndSendToUser(
                eq("123"),
                eq("/queue/alerts"),
                any()
        );
    }

    @Test
    @DisplayName("Should handle exception gracefully when pushing unread count")
    void shouldHandleExceptionGracefullyWhenPushingUnreadCount() {
        // Given
        doThrow(new RuntimeException("WebSocket error"))
                .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // When - should not throw
        notificationService.pushUnreadCount(123L, 5);

        // Then - exception is caught and logged
        verify(messagingTemplate).convertAndSendToUser(
                eq("123"),
                eq("/queue/alerts/count"),
                any()
        );
    }

    @Test
    @DisplayName("Should push AlertHistoryDto when pushing alert by userId")
    void shouldPushAlertDtoByUserId() {
        // Given
        AlertHistoryDto dto = AlertHistoryDto.builder()
                .id(UUID.randomUUID())
                .alertType(AlertType.STOCK)
                .title("DTO Alert")
                .message("DTO message")
                .severity(AlertSeverity.HIGH)
                .build();

        // When
        notificationService.pushAlert(999L, dto);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq("999"),
                eq("/queue/alerts"),
                eq(dto)
        );
    }
}
