package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.sync.AsyncProductSyncService;
import com.ecommerce.sellerx.sync.SyncTask;
import com.ecommerce.sellerx.sync.SyncTaskService;
import com.ecommerce.sellerx.sync.SyncTaskType;
import com.ecommerce.sellerx.sync.dto.StartSyncResponse;
import com.ecommerce.sellerx.sync.dto.SyncTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
public class TrendyolProductController {

    private final TrendyolProductService trendyolProductService;
    private final SyncTaskService syncTaskService;
    private final AsyncProductSyncService asyncProductSyncService;

    /**
     * Start an async product sync operation.
     * Returns immediately with 202 Accepted and a task ID to poll for status.
     * This is the recommended way to sync products - use /sync/{storeId}/status/{taskId} to check progress.
     */
    @PostMapping("/sync/{storeId}/async")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<StartSyncResponse> startAsyncProductSync(@PathVariable UUID storeId) {
        log.info("Starting async product sync for store: {}", storeId);

        // Check if there's already an active sync
        if (syncTaskService.hasActiveTask(storeId, SyncTaskType.PRODUCTS)) {
            log.warn("Active product sync already in progress for store: {}", storeId);
            List<SyncTaskResponse> activeTasks = syncTaskService.getActiveTasks(storeId);
            SyncTaskResponse activeTask = activeTasks.stream()
                    .filter(t -> t.getTaskType() == SyncTaskType.PRODUCTS)
                    .findFirst()
                    .orElse(null);

            if (activeTask != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(StartSyncResponse.builder()
                                .taskId(activeTask.getTaskId())
                                .storeId(storeId)
                                .taskType(SyncTaskType.PRODUCTS)
                                .status(activeTask.getStatus())
                                .message("A product sync is already in progress. Check the status URL.")
                                .statusUrl("/products/sync/" + storeId + "/status/" + activeTask.getTaskId())
                                .build());
            }
        }

        // Create task and start async execution
        SyncTask task = syncTaskService.createTask(storeId, SyncTaskType.PRODUCTS);
        asyncProductSyncService.executeProductSync(task.getId(), storeId);

        return ResponseEntity.accepted().body(syncTaskService.toStartResponse(task));
    }

    /**
     * Get the status of a sync task.
     * Poll this endpoint to track sync progress.
     */
    @GetMapping("/sync/{storeId}/status/{taskId}")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<SyncTaskResponse> getSyncStatus(
            @PathVariable UUID storeId,
            @PathVariable UUID taskId) {
        SyncTaskResponse status = syncTaskService.getTaskStatus(taskId, storeId);
        return ResponseEntity.ok(status);
    }

    /**
     * Get all active sync tasks for a store.
     */
    @GetMapping("/sync/{storeId}/active")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<List<SyncTaskResponse>> getActiveSyncTasks(@PathVariable UUID storeId) {
        List<SyncTaskResponse> activeTasks = syncTaskService.getActiveTasks(storeId);
        return ResponseEntity.ok(activeTasks);
    }

    /**
     * Synchronous product sync (DEPRECATED - use /sync/{storeId}/async instead).
     * Kept for backward compatibility, but may timeout for large catalogs.
     */
    @Deprecated
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<SyncProductsResponse> syncProductsFromTrendyol(@PathVariable UUID storeId) {
        log.warn("Using deprecated synchronous sync endpoint for store {}. Consider using /sync/{}/async", storeId, storeId);
        SyncProductsResponse response = trendyolProductService.syncProductsFromTrendyol(storeId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/store/{storeId}")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ProductListResponse<TrendyolProductDto>> getProductsByStoreWithPagination(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer maxStock,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minCommission,
            @RequestParam(required = false) BigDecimal maxCommission,
            @RequestParam(required = false) BigDecimal minCost,
            @RequestParam(required = false) BigDecimal maxCost,
            @RequestParam(defaultValue = "onSale") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        size = Math.min(size, 100);
        ProductListResponse<TrendyolProductDto> products = trendyolProductService.getProductsByStoreWithPagination(
                storeId, page, size, search,
                minStock, maxStock, minPrice, maxPrice,
                minCommission, maxCommission, minCost, maxCost,
                sortBy, sortDirection);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/store/{storeId}/all")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<AllProductsResponse> getAllProductsByStore(@PathVariable UUID storeId) {
        AllProductsResponse products = trendyolProductService.getAllProductsByStore(storeId);
        return ResponseEntity.ok(products);
    }
    
    @PutMapping("/{productId}/cost-and-stock")
    @PreAuthorize("@userSecurityRules.canAccessProduct(authentication, #productId)")
    public ResponseEntity<TrendyolProductDto> updateCostAndStock(
            @PathVariable UUID productId,
            @RequestBody UpdateCostAndStockRequest request) {
        TrendyolProductDto updatedProduct = trendyolProductService.updateCostAndStock(productId, request);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @PostMapping("/{productId}/stock-info")
    @PreAuthorize("@userSecurityRules.canAccessProduct(authentication, #productId)")
    public ResponseEntity<TrendyolProductDto> addStockInfo(
            @PathVariable UUID productId,
            @RequestBody AddStockInfoRequest request) {
        TrendyolProductDto updatedProduct = trendyolProductService.addStockInfo(productId, request);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @PutMapping("/{productId}/stock-info/{stockDate}")
    @PreAuthorize("@userSecurityRules.canAccessProduct(authentication, #productId)")
    public ResponseEntity<TrendyolProductDto> updateStockInfoByDate(
            @PathVariable UUID productId,
            @PathVariable LocalDate stockDate,
            @RequestBody UpdateStockInfoRequest request) {
        TrendyolProductDto updatedProduct = trendyolProductService.updateStockInfoByDate(productId, stockDate, request);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @DeleteMapping("/{productId}/stock-info/{stockDate}")
    @PreAuthorize("@userSecurityRules.canAccessProduct(authentication, #productId)")
    public ResponseEntity<TrendyolProductDto> deleteStockInfoByDate(
            @PathVariable UUID productId,
            @PathVariable LocalDate stockDate) {
        TrendyolProductDto updatedProduct = trendyolProductService.deleteStockInfoByDate(productId, stockDate);
        return ResponseEntity.ok(updatedProduct);
    }

    @PostMapping("/store/{storeId}/bulk-cost-update")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<BulkCostUpdateResponse> bulkUpdateCosts(
            @PathVariable UUID storeId,
            @RequestBody BulkCostUpdateRequest request) {
        BulkCostUpdateResponse response = trendyolProductService.bulkUpdateCosts(storeId, request);
        return ResponseEntity.ok(response);
    }
}
