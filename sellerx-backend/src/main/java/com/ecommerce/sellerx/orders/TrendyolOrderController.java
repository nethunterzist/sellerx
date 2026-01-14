package com.ecommerce.sellerx.orders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class TrendyolOrderController {

    private final TrendyolOrderService orderService;
    private final TrendyolOrderScheduledService scheduledService;

    /**
     * Fetch and save orders from Trendyol API for a specific store
     */
    @PostMapping("/stores/{storeId}/sync")
    public ResponseEntity<String> syncOrdersForStore(@PathVariable UUID storeId) {
        try {
            log.info("Starting order sync for store: {}", storeId);
            orderService.fetchAndSaveOrdersForStore(storeId);
            return ResponseEntity.ok("Orders synced successfully for store: " + storeId);
        } catch (Exception e) {
            log.error("Error syncing orders for store {}: {}", storeId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error syncing orders: " + e.getMessage());
        }
    }

    /**
     * Manually sync orders for all Trendyol stores
     */
    @PostMapping("/sync-all")
    public ResponseEntity<String> syncOrdersForAllStores() {
        try {
            log.info("Starting manual order sync for all Trendyol stores");
            scheduledService.manualSyncAllStores();
            return ResponseEntity.ok("Orders sync initiated for all Trendyol stores");
        } catch (Exception e) {
            log.error("Error syncing orders for all stores: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error syncing orders: " + e.getMessage());
        }
    }

    /**
     * Get orders for a store with pagination
     */
    @GetMapping("/stores/{storeId}")
    public ResponseEntity<Page<TrendyolOrderDto>> getOrdersForStore(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TrendyolOrderDto> orders = orderService.getOrdersForStore(storeId, pageable);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders for store {}: {}", storeId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get orders for store by date range
     */
    @GetMapping("/stores/{storeId}/by-date-range")
    public ResponseEntity<Page<TrendyolOrderDto>> getOrdersByDateRange(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TrendyolOrderDto> orders = orderService.getOrdersForStoreByDateRange(
                    storeId, startDate, endDate, pageable);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders by date range for store {}: {}", storeId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get orders by status
     */
    @GetMapping("/stores/{storeId}/by-status")
    public ResponseEntity<Page<TrendyolOrderDto>> getOrdersByStatus(
            @PathVariable UUID storeId,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TrendyolOrderDto> orders = orderService.getOrdersByStatus(storeId, status, pageable);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders by status for store {}: {}", storeId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get order statistics for a store
     */
    @GetMapping("/stores/{storeId}/statistics")
    public ResponseEntity<OrderStatistics> getOrderStatistics(@PathVariable UUID storeId) {
        try {
            OrderStatistics stats = orderService.getOrderStatistics(storeId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching order statistics for store {}: {}", storeId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
