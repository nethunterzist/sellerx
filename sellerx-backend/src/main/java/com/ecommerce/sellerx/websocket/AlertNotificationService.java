package com.ecommerce.sellerx.websocket;

import com.ecommerce.sellerx.alerts.AlertHistory;
import com.ecommerce.sellerx.alerts.AlertHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for pushing real-time notifications to users via WebSocket.
 * Replaces polling-based alert retrieval with push notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push a new alert to a specific user.
     * User subscribes to /user/queue/alerts to receive these.
     */
    public void pushAlert(Long userId, AlertHistoryDto alert) {
        String destination = "/queue/alerts";
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    destination,
                    alert
            );
            log.debug("[WS-PUSH] Alert {} sent to user {} via WebSocket", alert.getId(), userId);
        } catch (Exception e) {
            log.error("[WS-PUSH] Failed to push alert to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Push an alert directly from AlertHistory entity.
     */
    public void pushAlert(AlertHistory alert) {
        if (alert.getUser() == null) {
            log.warn("[WS-PUSH] Cannot push alert {} - no user associated", alert.getId());
            return;
        }

        AlertHistoryDto dto = toDto(alert);
        pushAlert(alert.getUser().getId(), dto);
    }

    /**
     * Push unread count update to a user.
     * User subscribes to /user/queue/alerts/count to receive these.
     */
    public void pushUnreadCount(Long userId, long count) {
        String destination = "/queue/alerts/count";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("unreadCount", count);
            payload.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    destination,
                    payload
            );
            log.debug("[WS-PUSH] Unread count {} sent to user {}", count, userId);
        } catch (Exception e) {
            log.error("[WS-PUSH] Failed to push unread count to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Push sync progress update to a user.
     * User subscribes to /user/queue/sync/progress to receive these.
     */
    public void pushSyncProgress(Long userId, UUID storeId, String syncType, int progress, String status) {
        String destination = "/queue/sync/progress";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("storeId", storeId.toString());
            payload.put("syncType", syncType);
            payload.put("progress", progress);
            payload.put("status", status);
            payload.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    destination,
                    payload
            );
            log.debug("[WS-PUSH] Sync progress {}% sent to user {} for store {}", progress, userId, storeId);
        } catch (Exception e) {
            log.error("[WS-PUSH] Failed to push sync progress to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Push sync completion notification to a user.
     */
    public void pushSyncComplete(Long userId, UUID storeId, String syncType, int itemsProcessed) {
        String destination = "/queue/sync/complete";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("storeId", storeId.toString());
            payload.put("syncType", syncType);
            payload.put("itemsProcessed", itemsProcessed);
            payload.put("status", "COMPLETED");
            payload.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    destination,
                    payload
            );
            log.info("[WS-PUSH] Sync complete notification sent to user {} for store {}", userId, storeId);
        } catch (Exception e) {
            log.error("[WS-PUSH] Failed to push sync complete to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Broadcast a system-wide notification to all connected users.
     * Use sparingly for important system announcements.
     */
    public void broadcastSystemNotification(String title, String message) {
        String destination = "/topic/system";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("message", message);
            payload.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(destination, payload);
            log.info("[WS-BROADCAST] System notification sent: {}", title);
        } catch (Exception e) {
            log.error("[WS-BROADCAST] Failed to broadcast system notification: {}", e.getMessage());
        }
    }

    /**
     * Convert AlertHistory entity to DTO.
     */
    private AlertHistoryDto toDto(AlertHistory alert) {
        return AlertHistoryDto.builder()
                .id(alert.getId())
                .ruleId(alert.getRule() != null ? alert.getRule().getId() : null)
                .ruleName(alert.getRule() != null ? alert.getRule().getName() : null)
                .storeId(alert.getStore() != null ? alert.getStore().getId() : null)
                .storeName(alert.getStore() != null ? alert.getStore().getStoreName() : null)
                .alertType(alert.getAlertType())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .severity(alert.getSeverity())
                .data(alert.getData())
                .emailSent(alert.getEmailSent())
                .pushSent(alert.getPushSent())
                .inAppSent(alert.getInAppSent())
                .status(alert.getStatus())
                .read(alert.isRead())
                .readAt(alert.getReadAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
