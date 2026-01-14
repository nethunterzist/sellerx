package com.ecommerce.sellerx.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardStatsService dashboardStatsService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(Authentication authentication) {
        
        try {
            // For now, we'll use a dummy store ID since the service expects one
            // In a real implementation, you would get the user's current store
            UUID dummyStoreId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            
            DashboardStatsResponse stats = dashboardStatsService.getStatsForStore(dummyStoreId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats/{storeId}")
    public ResponseEntity<DashboardStatsResponse> getDashboardStatsByStore(
            @PathVariable String storeId,
            Authentication authentication) {
        
        try {
            UUID storeUuid = UUID.fromString(storeId);
            DashboardStatsResponse stats = dashboardStatsService.getStatsForStore(storeUuid);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}