package com.ecommerce.sellerx.orders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders/stock-sync")
@RequiredArgsConstructor
@Slf4j
public class StockOrderSynchronizationController {

    private final StockOrderSynchronizationService stockOrderSyncService;

    /**
     * Manually trigger stock-order synchronization for a store
     */
    @PostMapping("/synchronize/{storeId}")
    public ResponseEntity<Map<String, Object>> synchronizeStockOrders(
            @PathVariable UUID storeId,
            @RequestParam(required = false) LocalDate fromDate) {
        
        log.info("Manual stock-order synchronization requested for store: {}, fromDate: {}", storeId, fromDate);
        
        try {
            stockOrderSyncService.synchronizeOrdersAfterStockChange(storeId, fromDate);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Stock-order synchronization completed successfully",
                "storeId", storeId,
                "fromDate", fromDate != null ? fromDate.toString() : "all"
            ));
            
        } catch (Exception e) {
            log.error("Failed to synchronize stock-orders for store {}: {}", storeId, e.getMessage(), e);
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Stock-order synchronization failed: " + e.getMessage(),
                "storeId", storeId
            ));
        }
    }

    /**
     * Get synchronization status/info for a store
     */
    @GetMapping("/status/{storeId}")
    public ResponseEntity<Map<String, Object>> getSynchronizationStatus(@PathVariable UUID storeId) {
        
        try {
            // You could add a method to get synchronization statistics here
            return ResponseEntity.ok(Map.of(
                "success", true,
                "storeId", storeId,
                "message", "Stock-order synchronization service is available"
            ));
            
        } catch (Exception e) {
            log.error("Failed to get synchronization status for store {}: {}", storeId, e.getMessage());
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to get synchronization status: " + e.getMessage(),
                "storeId", storeId
            ));
        }
    }
}
