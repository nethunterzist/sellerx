package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminBroadcastRequest;
import com.ecommerce.sellerx.admin.dto.AdminNotificationStatsDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    /**
     * Get notification statistics
     * GET /api/admin/notifications/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminNotificationStatsDto> getNotificationStats() {
        log.info("Admin fetching notification stats");
        AdminNotificationStatsDto stats = adminNotificationService.getNotificationStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Broadcast a notification to all users
     * POST /api/admin/notifications/broadcast
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastNotification(
            @Valid @RequestBody AdminBroadcastRequest request) {
        log.info("Admin broadcasting notification: type={}, title='{}'", request.getType(), request.getTitle());
        int recipientCount = adminNotificationService.broadcastNotification(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "recipientCount", recipientCount
        ));
    }
}
