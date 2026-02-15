package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.sync.AsyncOrderSyncService;
import com.ecommerce.sellerx.sync.SyncTask;
import com.ecommerce.sellerx.sync.SyncTaskService;
import com.ecommerce.sellerx.sync.SyncTaskType;
import com.ecommerce.sellerx.sync.dto.StartSyncResponse;
import com.ecommerce.sellerx.sync.dto.SyncTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class TrendyolOrderController {

    private final TrendyolOrderService orderService;
    private final TrendyolOrderScheduledService scheduledService;
    private final OrderCommissionRecalculationService commissionRecalculationService;
    private final SyncTaskService syncTaskService;
    private final AsyncOrderSyncService asyncOrderSyncService;

    /**
     * Start an async order sync operation.
     * Returns immediately with 202 Accepted and a task ID to poll for status.
     * This is the recommended way to sync orders - use /stores/{storeId}/sync/status/{taskId} to check progress.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/sync/async")
    public ResponseEntity<StartSyncResponse> startAsyncOrderSync(@PathVariable UUID storeId) {
        log.info("Starting async order sync for store: {}", storeId);

        // Check if there's already an active sync
        if (syncTaskService.hasActiveTask(storeId, SyncTaskType.ORDERS)) {
            log.warn("Active order sync already in progress for store: {}", storeId);
            List<SyncTaskResponse> activeTasks = syncTaskService.getActiveTasks(storeId);
            SyncTaskResponse activeTask = activeTasks.stream()
                    .filter(t -> t.getTaskType() == SyncTaskType.ORDERS)
                    .findFirst()
                    .orElse(null);

            if (activeTask != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(StartSyncResponse.builder()
                                .taskId(activeTask.getTaskId())
                                .storeId(storeId)
                                .taskType(SyncTaskType.ORDERS)
                                .status(activeTask.getStatus())
                                .message("An order sync is already in progress. Check the status URL.")
                                .statusUrl("/api/orders/stores/" + storeId + "/sync/status/" + activeTask.getTaskId())
                                .build());
            }
        }

        // Create task and start async execution
        SyncTask task = syncTaskService.createTask(storeId, SyncTaskType.ORDERS);
        asyncOrderSyncService.executeOrderSync(task.getId(), storeId);

        StartSyncResponse response = StartSyncResponse.builder()
                .taskId(task.getId())
                .storeId(storeId)
                .taskType(SyncTaskType.ORDERS)
                .status(task.getStatus())
                .message("Order sync operation started. Poll the status URL to check progress.")
                .statusUrl("/api/orders/stores/" + storeId + "/sync/status/" + task.getId())
                .build();

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get the status of an order sync task.
     * Poll this endpoint to track sync progress.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/sync/status/{taskId}")
    public ResponseEntity<SyncTaskResponse> getOrderSyncStatus(
            @PathVariable UUID storeId,
            @PathVariable UUID taskId) {
        SyncTaskResponse status = syncTaskService.getTaskStatus(taskId, storeId);
        return ResponseEntity.ok(status);
    }

    /**
     * Synchronous order sync (DEPRECATED - use /stores/{storeId}/sync/async instead).
     * Kept for backward compatibility, but may timeout for large order histories.
     */
    @Deprecated
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/sync")
    public ResponseEntity<String> syncOrdersForStore(@PathVariable UUID storeId) {
        try {
            log.warn("Using deprecated synchronous sync endpoint for store {}. Consider using /stores/{}/sync/async", storeId, storeId);
            orderService.fetchAndSaveOrdersForStore(storeId);
            return ResponseEntity.ok("Orders synced successfully for store: " + storeId);
        } catch (Exception e) {
            log.error("Error syncing orders for store {}: {}", storeId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error syncing orders: " + e.getMessage());
        }
    }

    /**
     * Manually sync orders for all Trendyol stores.
     * Only ADMIN users can trigger sync for all stores.
     */
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}")
    public ResponseEntity<Page<TrendyolOrderDto>> getOrdersForStore(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            size = Math.min(size, 100);
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
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/by-date-range")
    public ResponseEntity<Page<TrendyolOrderDto>> getOrdersByDateRange(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            size = Math.min(size, 100);
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
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/by-status")
    public ResponseEntity<Page<TrendyolOrderDto>> getOrdersByStatus(
            @PathVariable UUID storeId,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            size = Math.min(size, 100);
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
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
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

    /**
     * Get order statistics for a store within a specific date range.
     * Used for orders page to show statistics filtered by selected date range.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/statistics/by-date-range")
    public ResponseEntity<OrderStatistics> getOrderStatisticsByDateRange(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            log.debug("Fetching order statistics for store {} from {} to {}", storeId, startDate, endDate);
            OrderStatistics stats = orderService.getOrderStatisticsByDateRange(storeId, startDate, endDate);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching order statistics by date range for store {}: {}", storeId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Recalculate estimated commissions for orders.
     * This should be called after financial sync to update orders with commission rates
     * that weren't available during initial order sync.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/recalculate-commissions")
    public ResponseEntity<CommissionRecalculationResponse> recalculateCommissions(@PathVariable UUID storeId) {
        try {
            log.info("Starting commission recalculation for store: {}", storeId);
            int updatedOrders = commissionRecalculationService.recalculateEstimatedCommissions(storeId);
            return ResponseEntity.ok(new CommissionRecalculationResponse(
                    storeId,
                    updatedOrders,
                    "Commission recalculation completed successfully"
            ));
        } catch (Exception e) {
            log.error("Error recalculating commissions for store {}: {}", storeId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(new CommissionRecalculationResponse(
                    storeId,
                    0,
                    "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Response DTO for commission recalculation endpoint
     */
    public record CommissionRecalculationResponse(UUID storeId, int updatedOrders, String message) {}
}
