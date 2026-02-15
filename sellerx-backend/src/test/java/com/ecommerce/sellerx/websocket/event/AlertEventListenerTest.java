package com.ecommerce.sellerx.websocket.event;

import com.ecommerce.sellerx.alerts.*;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.websocket.AlertNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertEventListener.
 */
@ExtendWith(MockitoExtension.class)
class AlertEventListenerTest {

    @Mock
    private AlertNotificationService notificationService;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @InjectMocks
    private AlertEventListener eventListener;

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
    @DisplayName("Should push alert and unread count on alert created event")
    void shouldPushAlertAndUnreadCountOnEvent() {
        // Given
        AlertCreatedEvent event = new AlertCreatedEvent(this, testAlert);
        when(alertHistoryRepository.countByUserIdAndReadAtIsNull(123L)).thenReturn(5L);

        // When
        eventListener.onAlertCreated(event);

        // Then
        verify(notificationService).pushAlert(testAlert);
        verify(notificationService).pushUnreadCount(123L, 5L);
        verify(alertHistoryRepository).countByUserIdAndReadAtIsNull(123L);
    }

    @Test
    @DisplayName("Should not push when alert is null")
    void shouldNotPushWhenAlertIsNull() {
        // Given
        AlertCreatedEvent event = new AlertCreatedEvent(this, null);

        // When
        eventListener.onAlertCreated(event);

        // Then
        verify(notificationService, never()).pushAlert(any());
        verify(notificationService, never()).pushUnreadCount(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should not push when user is null")
    void shouldNotPushWhenUserIsNull() {
        // Given
        AlertHistory alertWithNullUser = AlertHistory.builder()
                .id(UUID.randomUUID())
                .user(null)
                .alertType(AlertType.STOCK)
                .title("Test Alert")
                .message("Test message")
                .severity(AlertSeverity.MEDIUM)
                .build();
        AlertCreatedEvent event = new AlertCreatedEvent(this, alertWithNullUser);

        // When
        eventListener.onAlertCreated(event);

        // Then
        verify(notificationService, never()).pushAlert(any());
        verify(notificationService, never()).pushUnreadCount(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should handle exception gracefully")
    void shouldHandleExceptionGracefully() {
        // Given
        AlertCreatedEvent event = new AlertCreatedEvent(this, testAlert);
        doThrow(new RuntimeException("Push failed"))
                .when(notificationService).pushAlert(any());

        // When - should not throw
        eventListener.onAlertCreated(event);

        // Then - exception is caught
        verify(notificationService).pushAlert(testAlert);
        // unread count push should not be called due to exception
    }

    @Test
    @DisplayName("Should query correct user ID for unread count")
    void shouldQueryCorrectUserIdForUnreadCount() {
        // Given
        testUser.setId(999L);
        AlertCreatedEvent event = new AlertCreatedEvent(this, testAlert);
        when(alertHistoryRepository.countByUserIdAndReadAtIsNull(999L)).thenReturn(10L);

        // When
        eventListener.onAlertCreated(event);

        // Then
        verify(alertHistoryRepository).countByUserIdAndReadAtIsNull(999L);
        verify(notificationService).pushUnreadCount(999L, 10L);
    }
}
