package com.ecommerce.sellerx.returns;

import com.ecommerce.sellerx.returns.dto.ReturnAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@Slf4j
public class ReturnAnalyticsController {

    private final ReturnAnalyticsService returnAnalyticsService;

    /**
     * Get return analytics for a store within a date range
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/analytics")
    public ResponseEntity<ReturnAnalyticsResponse> getReturnAnalytics(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/returns/stores/{}/analytics?startDate={}&endDate={}", storeId, startDate, endDate);

        ReturnAnalyticsResponse response = returnAnalyticsService.getReturnAnalytics(storeId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Get return analytics for current month
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/analytics/current-month")
    public ResponseEntity<ReturnAnalyticsResponse> getCurrentMonthAnalytics(@PathVariable UUID storeId) {
        log.info("GET /api/returns/stores/{}/analytics/current-month", storeId);

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        ReturnAnalyticsResponse response = returnAnalyticsService.getReturnAnalytics(storeId, startOfMonth, today);
        return ResponseEntity.ok(response);
    }

    /**
     * Get return analytics for last 30 days
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/analytics/last-30-days")
    public ResponseEntity<ReturnAnalyticsResponse> getLast30DaysAnalytics(@PathVariable UUID storeId) {
        log.info("GET /api/returns/stores/{}/analytics/last-30-days", storeId);

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        ReturnAnalyticsResponse response = returnAnalyticsService.getReturnAnalytics(storeId, thirtyDaysAgo, today);
        return ResponseEntity.ok(response);
    }
}
