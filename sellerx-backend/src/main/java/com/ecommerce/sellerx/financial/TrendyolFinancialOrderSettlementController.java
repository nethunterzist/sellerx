package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.auth.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@Slf4j
public class TrendyolFinancialOrderSettlementController {

    private final TrendyolFinancialSettlementService settlementService;
    private final JwtService jwtService;

    /**
     * Sync settlements for a specific store
     */
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
}
