package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.auth.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@Slf4j
public class TrendyolFinancialOrderSettlementController {

    private final TrendyolFinancialSettlementService settlementService;
    private final TrendyolOtherFinancialsService otherFinancialsService;
    private final JwtService jwtService;

    /**
     * Get settlement statistics for a specific store
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/stats")
    public ResponseEntity<?> getSettlementStats(@PathVariable UUID storeId, HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            log.info("Getting settlement stats for store: {} by user: {}", storeId, userId);

            Map<String, Object> stats = settlementService.getSettlementStats(storeId);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to get settlement stats for store: {}", storeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get settlement stats: " + e.getMessage()));
        }
    }

    /**
     * Sync settlements for a specific store
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/sync")
    public ResponseEntity<?> syncSettlementsForStore(@PathVariable UUID storeId, HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            log.info("Starting settlement sync for store: {} by user: {}", storeId, userId);

            settlementService.fetchAndUpdateSettlementsForStore(storeId);

            return ResponseEntity.ok(Map.of(
                "message", "Settlement sync completed successfully",
                "storeId", storeId.toString()
            ));

        } catch (Exception e) {
            log.error("Failed to sync settlements for store: {}", storeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to sync settlements: " + e.getMessage()));
        }
    }

    /**
     * Sync ALL historical cargo invoices for a store.
     * Fetches cargo invoices from the earliest order date to today.
     * This is a long-running operation for stores with many orders.
     *
     * Use this endpoint when:
     * 1. A store is missing historical shipping cost data
     * 2. The daily sync job missed historical data
     * 3. Manual recovery of shipping data is needed
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/sync-historical-cargo")
    public ResponseEntity<?> syncHistoricalCargoInvoices(@PathVariable UUID storeId, HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            log.info("Starting historical cargo invoice sync for store: {} by user: {}", storeId, userId);

            TrendyolOtherFinancialsService.HistoricalCargoSyncResult result =
                    otherFinancialsService.syncHistoricalCargoInvoices(storeId);

            return ResponseEntity.ok(Map.of(
                "storeId", storeId.toString(),
                "syncedCargoInvoices", result.getSyncedCargoInvoices(),
                "updatedOrders", result.getUpdatedOrders(),
                "startDate", result.getStartDate() != null ? result.getStartDate().toString() : "N/A",
                "endDate", result.getEndDate() != null ? result.getEndDate().toString() : "N/A",
                "totalDays", result.getTotalDays(),
                "message", result.getMessage(),
                "success", result.isSuccess()
            ));

        } catch (Exception e) {
            log.error("Failed to sync historical cargo invoices for store: {}", storeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to sync historical cargo invoices: " + e.getMessage(),
                    "storeId", storeId.toString(),
                    "success", false
                ));
        }
    }
}
