package com.ecommerce.sellerx.websocket.event;

import com.ecommerce.sellerx.alerts.AlertHistory;
import com.ecommerce.sellerx.alerts.AlertHistoryRepository;
import com.ecommerce.sellerx.websocket.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for alert-related events.
 * Pushes notifications via WebSocket when alerts are created.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventListener {

    private final AlertNotificationService notificationService;
    private final AlertHistoryRepository alertHistoryRepository;

    /**
     * Handle alert created event.
     * Uses @TransactionalEventListener to ensure alert is committed before pushing.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertCreated(AlertCreatedEvent event) {
        AlertHistory alert = event.getAlert();

        if (alert == null || alert.getUser() == null) {
            log.warn("[ALERT-EVENT] Received alert event with null alert or user");
            return;
        }

        try {
            // Push the new alert to the user
            notificationService.pushAlert(alert);

            // Update unread count for the user
            long unreadCount = alertHistoryRepository.countByUserIdAndReadAtIsNull(alert.getUser().getId());
            notificationService.pushUnreadCount(alert.getUser().getId(), unreadCount);

            log.debug("[ALERT-EVENT] Alert {} pushed to user {}", alert.getId(), alert.getUser().getId());
        } catch (Exception e) {
            log.error("[ALERT-EVENT] Failed to process alert event for alert {}: {}",
                    alert.getId(), e.getMessage());
        }
    }
}
