package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.auth.AuthService;
import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing alert history.
 * Provides read operations and status updates for triggered alerts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AuthService authService;

    /**
     * Get paginated alert history for the current user.
     */
    @Transactional(readOnly = true)
    public Page<AlertHistoryDto> getAlertsForCurrentUser(Pageable pageable) {
        User user = authService.getCurrentUser();
        return alertHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toDto);
    }

    /**
     * Get alerts filtered by type for the current user.
     */
    @Transactional(readOnly = true)
    public Page<AlertHistoryDto> getAlertsByType(AlertType alertType, Pageable pageable) {
        User user = authService.getCurrentUser();
        return alertHistoryRepository.findByUserIdAndAlertTypeOrderByCreatedAtDesc(
                        user.getId(), alertType, pageable)
                .map(this::toDto);
    }

    /**
     * Get alerts filtered by severity for the current user.
     */
    @Transactional(readOnly = true)
    public Page<AlertHistoryDto> getAlertsBySeverity(AlertSeverity severity, Pageable pageable) {
        User user = authService.getCurrentUser();
        return alertHistoryRepository.findByUserIdAndSeverityOrderByCreatedAtDesc(
                        user.getId(), severity, pageable)
                .map(this::toDto);
    }

    /**
     * Get unread alerts for the current user.
     */
    @Transactional(readOnly = true)
    public List<AlertHistoryDto> getUnreadAlerts() {
        User user = authService.getCurrentUser();
        return alertHistoryRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get unread alert count for the current user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = authService.getCurrentUser();
        return alertHistoryRepository.countByUserIdAndReadAtIsNull(user.getId());
    }

    /**
     * Get a specific alert by ID.
     */
    @Transactional(readOnly = true)
    public AlertHistoryDto getAlertById(UUID alertId) {
        User user = authService.getCurrentUser();
        AlertHistory alert = alertHistoryRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new AlertNotFoundException("Alert not found: " + alertId));
        return toDto(alert);
    }

    /**
     * Mark an alert as read.
     */
    @Transactional
    public AlertHistoryDto markAsRead(UUID alertId) {
        User user = authService.getCurrentUser();
        AlertHistory alert = alertHistoryRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new AlertNotFoundException("Alert not found: " + alertId));

        alert.markAsRead();
        AlertHistory saved = alertHistoryRepository.save(alert);

        log.debug("Marked alert {} as read for user {}", alertId, user.getId());
        return toDto(saved);
    }

    /**
     * Mark all alerts as read for the current user.
     */
    @Transactional
    public int markAllAsRead() {
        User user = authService.getCurrentUser();
        int count = alertHistoryRepository.markAllAsReadForUser(user.getId());
        log.info("Marked {} alerts as read for user {}", count, user.getId());
        return count;
    }

    /**
     * Get recent alerts (last 24 hours) for the current user.
     */
    @Transactional(readOnly = true)
    public List<AlertHistoryDto> getRecentAlerts() {
        User user = authService.getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return alertHistoryRepository.findRecentAlerts(user.getId(), since)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts within a date range.
     */
    @Transactional(readOnly = true)
    public List<AlertHistoryDto> getAlertsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        User user = authService.getCurrentUser();
        return alertHistoryRepository.findByUserIdAndDateRange(user.getId(), startDate, endDate)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get alert statistics for the current user.
     */
    @Transactional(readOnly = true)
    public AlertStatsDto getAlertStats() {
        User user = authService.getCurrentUser();
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        return AlertStatsDto.builder()
                .unreadCount(alertHistoryRepository.countByUserIdAndReadAtIsNull(user.getId()))
                .stockAlertsLast24h(alertHistoryRepository.countByUserAndTypeAndSince(
                        user.getId(), AlertType.STOCK, last24Hours))
                .profitAlertsLast24h(alertHistoryRepository.countByUserAndTypeAndSince(
                        user.getId(), AlertType.PROFIT, last24Hours))
                .orderAlertsLast24h(alertHistoryRepository.countByUserAndTypeAndSince(
                        user.getId(), AlertType.ORDER, last24Hours))
                .totalAlertsLast7Days(alertHistoryRepository.findByUserIdAndDateRange(
                        user.getId(), last7Days, LocalDateTime.now()).size())
                .build();
    }

    /**
     * Convert entity to DTO.
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
                .read(alert.isRead())
                .readAt(alert.getReadAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
