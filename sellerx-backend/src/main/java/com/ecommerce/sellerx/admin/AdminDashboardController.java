package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminDashboardStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Get dashboard statistics
     * GET /api/admin/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsDto> getDashboardStats() {
        log.info("Admin fetching dashboard stats");
        AdminDashboardStatsDto stats = adminDashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
