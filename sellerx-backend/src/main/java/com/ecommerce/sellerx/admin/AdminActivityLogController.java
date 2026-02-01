package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminActivityLogDto;
import com.ecommerce.sellerx.admin.dto.AdminSecuritySummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/activity-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityLogController {

    private final AdminActivityLogService adminActivityLogService;

    /**
     * Get paginated activity logs with optional filters.
     * GET /api/admin/activity-logs?email=test&action=login&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<AdminActivityLogDto>> getActivityLogs(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String action,
            @PageableDefault(size = 20) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        log.info("Admin fetching activity logs - email={}, action={}, page={}", email, action, pageable.getPageNumber());
        Page<AdminActivityLogDto> logs = adminActivityLogService.getActivityLogs(email, action, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get security summary with failed login stats and suspicious activity.
     * GET /api/admin/activity-logs/security/summary
     */
    @GetMapping("/security/summary")
    public ResponseEntity<AdminSecuritySummaryDto> getSecuritySummary() {
        log.info("Admin fetching security summary");
        AdminSecuritySummaryDto summary = adminActivityLogService.getSecuritySummary();
        return ResponseEntity.ok(summary);
    }
}
