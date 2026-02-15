package com.ecommerce.sellerx.dashboard;

import com.ecommerce.sellerx.stores.StoreService;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.ecommerce.sellerx.dashboard.dto.DeductionBreakdownDto;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardStatsService dashboardStatsService;
    private final UserRepository userRepository;
    private final StoreService storeService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(Authentication authentication) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AccessDeniedException("User not found"));

            UUID selectedStoreId = user.getSelectedStoreId();
            if (selectedStoreId == null) {
                log.warn("User {} has no selected store", userId);
                return ResponseEntity.badRequest().build();
            }

            DashboardStatsResponse stats = dashboardStatsService.getStatsForStore(selectedStoreId);
            return ResponseEntity.ok(stats);
        } catch (AccessDeniedException e) {
            log.warn("Access denied for dashboard stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error fetching dashboard stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats/{storeId}")
    public ResponseEntity<DashboardStatsResponse> getDashboardStatsByStore(
            @PathVariable String storeId,
            Authentication authentication) {

        try {
            Long userId = (Long) authentication.getPrincipal();
            UUID storeUuid = UUID.fromString(storeId);

            // Authorization check: verify user owns this store
            if (!storeService.isStoreOwnedByUser(storeUuid, userId)) {
                log.warn("User {} attempted to access store {} without authorization", userId, storeId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            DashboardStatsResponse stats = dashboardStatsService.getStatsForStore(storeUuid);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid store ID format: {}", storeId);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching dashboard stats for store {}", storeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get dashboard stats for a custom date range.
     *
     * @param storeId Store UUID
     * @param startDate Start date (ISO format: 2025-01-01)
     * @param endDate End date (ISO format: 2025-01-15)
     * @param periodLabel Optional label for the period (e.g., "Son 7 Gün")
     */
    @GetMapping("/stats/{storeId}/range")
    public ResponseEntity<DashboardStatsDto> getStatsByDateRange(
            @PathVariable String storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String periodLabel,
            Authentication authentication) {

        try {
            Long userId = (Long) authentication.getPrincipal();
            UUID storeUuid = UUID.fromString(storeId);

            // Authorization check
            if (!storeService.isStoreOwnedByUser(storeUuid, userId)) {
                log.warn("User {} attempted to access store {} without authorization", userId, storeId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Validation: startDate must be before or equal to endDate
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().build();
            }

            // Validation: max 365 days
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 365) {
                return ResponseEntity.badRequest().build();
            }

            // Validation: endDate cannot be in the future
            if (endDate.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().build();
            }

            DashboardStatsDto stats = dashboardStatsService.getStatsForDateRange(
                    storeUuid, startDate, endDate, periodLabel);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for date range stats: storeId={}", storeId);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching date range stats for store {}", storeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get city-based order statistics for analytics/map visualization.
     *
     * @param storeId Store UUID
     * @param startDate Start date (ISO format: 2025-01-01)
     * @param endDate End date (ISO format: 2025-01-15)
     * @param productBarcode Optional - filter by specific product barcode
     */
    @GetMapping("/stats/{storeId}/cities")
    public ResponseEntity<CityStatsResponse> getCityStats(
            @PathVariable String storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String productBarcode,
            Authentication authentication) {

        try {
            Long userId = (Long) authentication.getPrincipal();
            UUID storeUuid = UUID.fromString(storeId);

            // Authorization check
            if (!storeService.isStoreOwnedByUser(storeUuid, userId)) {
                log.warn("User {} attempted to access store {} city stats without authorization", userId, storeId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Validation: startDate must be before or equal to endDate
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().build();
            }

            // Validation: max 365 days
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 365) {
                return ResponseEntity.badRequest().build();
            }

            CityStatsResponse stats = dashboardStatsService.getCityStats(storeUuid, startDate, endDate, productBarcode);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for city stats: storeId={}", storeId);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching city stats for store {}", storeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get multi-period statistics for P&L breakdown table.
     * Returns stats for multiple periods (months, weeks, or days) with horizontal scroll table data.
     *
     * @param storeId Store UUID
     * @param periodType "monthly", "weekly", or "daily"
     * @param periodCount Number of periods to include (e.g., 12 for last 12 months)
     * @param productBarcode Optional - filter by specific product barcode
     */
    @GetMapping("/stats/{storeId}/multi-period")
    public ResponseEntity<MultiPeriodStatsResponse> getMultiPeriodStats(
            @PathVariable String storeId,
            @RequestParam(defaultValue = "monthly") String periodType,
            @RequestParam(defaultValue = "12") int periodCount,
            @RequestParam(required = false) String productBarcode,
            Authentication authentication) {

        try {
            Long userId = (Long) authentication.getPrincipal();
            UUID storeUuid = UUID.fromString(storeId);

            // Authorization check
            if (!storeService.isStoreOwnedByUser(storeUuid, userId)) {
                log.warn("User {} attempted to access store {} multi-period stats without authorization", userId, storeId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Validation: periodType must be valid
            if (!periodType.equalsIgnoreCase("monthly") &&
                !periodType.equalsIgnoreCase("weekly") &&
                !periodType.equalsIgnoreCase("daily")) {
                log.warn("Invalid periodType: {}", periodType);
                return ResponseEntity.badRequest().build();
            }

            // Validation: periodCount must be reasonable
            if (periodCount < 1 || periodCount > 365) {
                log.warn("Invalid periodCount: {}", periodCount);
                return ResponseEntity.badRequest().build();
            }

            MultiPeriodStatsResponse stats = dashboardStatsService.getMultiPeriodStats(
                    storeUuid, periodType, periodCount, productBarcode);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for multi-period stats: storeId={}, periodType={}, periodCount={}",
                    storeId, periodType, periodCount);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching multi-period stats for store {}", storeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get deduction invoice breakdown by transaction type for a date range.
     * Used for dashboard detail panel to show all invoice types individually.
     *
     * @param storeId Store UUID
     * @param startDate Start date (ISO format: 2025-01-01)
     * @param endDate End date (ISO format: 2025-01-15)
     */
    @GetMapping("/stores/{storeId}/deductions/breakdown")
    public ResponseEntity<List<DeductionBreakdownDto>> getDeductionBreakdown(
            @PathVariable String storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        try {
            Long userId = (Long) authentication.getPrincipal();
            UUID storeUuid = UUID.fromString(storeId);

            // Authorization check
            if (!storeService.isStoreOwnedByUser(storeUuid, userId)) {
                log.warn("User {} attempted to access store {} deduction breakdown without authorization", userId, storeId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Validation: startDate must be before or equal to endDate
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().build();
            }

            List<DeductionBreakdownDto> breakdown = dashboardStatsService.getDeductionBreakdown(storeUuid, startDate, endDate);
            return ResponseEntity.ok(breakdown);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for deduction breakdown: storeId={}", storeId);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching deduction breakdown for store {}", storeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}