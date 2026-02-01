package com.ecommerce.sellerx.alerts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing alert history.
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertHistoryController {

    private final AlertHistoryService alertHistoryService;

    /**
     * Get paginated alert history for the current user.
     */
    @GetMapping
    public ResponseEntity<Page<AlertHistoryDto>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertSeverity severity) {
        log.debug("GET /api/alerts?page={}&size={}&type={}&severity={}", page, size, type, severity);

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertHistoryDto> alerts;

        if (type != null) {
            alerts = alertHistoryService.getAlertsByType(type, pageable);
        } else if (severity != null) {
            alerts = alertHistoryService.getAlertsBySeverity(severity, pageable);
        } else {
            alerts = alertHistoryService.getAlertsForCurrentUser(pageable);
        }

        return ResponseEntity.ok(alerts);
    }

    /**
     * Get unread alerts for the current user.
     */
    @GetMapping("/unread")
    public ResponseEntity<List<AlertHistoryDto>> getUnreadAlerts() {
        log.debug("GET /api/alerts/unread");
        List<AlertHistoryDto> alerts = alertHistoryService.getUnreadAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get unread alert count.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        log.debug("GET /api/alerts/unread-count");
        long count = alertHistoryService.getUnreadCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get recent alerts (last 24 hours).
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AlertHistoryDto>> getRecentAlerts() {
        log.debug("GET /api/alerts/recent");
        List<AlertHistoryDto> alerts = alertHistoryService.getRecentAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts within a date range.
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<List<AlertHistoryDto>> getAlertsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.debug("GET /api/alerts/by-date-range?startDate={}&endDate={}", startDate, endDate);
        List<AlertHistoryDto> alerts = alertHistoryService.getAlertsByDateRange(startDate, endDate);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get a specific alert by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertHistoryDto> getAlert(@PathVariable UUID id) {
        log.debug("GET /api/alerts/{}", id);
        AlertHistoryDto alert = alertHistoryService.getAlertById(id);
        return ResponseEntity.ok(alert);
    }

    /**
     * Mark an alert as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<AlertHistoryDto> markAsRead(@PathVariable UUID id) {
        log.debug("PUT /api/alerts/{}/read", id);
        AlertHistoryDto alert = alertHistoryService.markAsRead(id);
        return ResponseEntity.ok(alert);
    }

    /**
     * Mark all alerts as read.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        log.debug("PUT /api/alerts/read-all");
        int count = alertHistoryService.markAllAsRead();
        return ResponseEntity.ok(Map.of("markedCount", count));
    }

    /**
     * Get alert statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<AlertStatsDto> getStats() {
        log.debug("GET /api/alerts/stats");
        AlertStatsDto stats = alertHistoryService.getAlertStats();
        return ResponseEntity.ok(stats);
    }
}
