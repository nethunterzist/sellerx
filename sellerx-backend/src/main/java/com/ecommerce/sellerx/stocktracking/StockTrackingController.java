package com.ecommerce.sellerx.stocktracking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Stock Tracking feature.
 * Allows tracking competitor product stock levels.
 */
@Slf4j
@RestController
@RequestMapping("/api/stock-tracking")
@RequiredArgsConstructor
public class StockTrackingController {

    private final StockTrackingService stockTrackingService;
    private final TrendyolPublicStockClient trendyolPublicStockClient;

    /**
     * Preview a product URL before adding to tracking.
     * Returns product info (name, image, price, stock) for user verification.
     */
    @GetMapping("/preview")
    public ResponseEntity<ProductPreviewDto> previewProduct(@RequestParam String url) {
        log.debug("Previewing product URL: {}", url);

        // Validate URL format
        if (!trendyolPublicStockClient.isValidProductUrl(url)) {
            return ResponseEntity.ok(ProductPreviewDto.invalid("Geçerli bir Trendyol ürün sayfası URL'si girin"));
        }

        // Fetch product data
        TrendyolPublicStockClient.StockData stockData = trendyolPublicStockClient.fetchStock(url);

        // Convert to preview DTO
        ProductPreviewDto preview = ProductPreviewDto.fromStockData(stockData);

        return ResponseEntity.ok(preview);
    }

    /**
     * Get dashboard summary for a store
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/dashboard")
    public ResponseEntity<StockTrackingDashboardDto> getDashboard(@PathVariable UUID storeId) {
        log.debug("Getting stock tracking dashboard for store: {}", storeId);
        return ResponseEntity.ok(stockTrackingService.getDashboard(storeId));
    }

    /**
     * Get all tracked products for a store
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<List<TrackedProductDto>> getTrackedProducts(@PathVariable UUID storeId) {
        log.debug("Getting tracked products for store: {}", storeId);
        return ResponseEntity.ok(stockTrackingService.getTrackedProducts(storeId));
    }

    /**
     * Add a product to track
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/products")
    public ResponseEntity<TrackedProductDto> addProduct(
            @PathVariable UUID storeId,
            @Valid @RequestBody AddTrackedProductRequest request) {
        log.info("Adding product to track for store: {}, URL: {}", storeId, request.getProductUrl());
        return ResponseEntity.ok(stockTrackingService.addProductToTrack(storeId, request));
    }

    /**
     * Get detailed view of a tracked product
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/products/{trackedProductId}")
    public ResponseEntity<TrackedProductDetailDto> getProductDetail(
            @PathVariable UUID trackedProductId,
            @RequestParam UUID storeId) {
        log.debug("Getting product detail: {} for store: {}", trackedProductId, storeId);
        return ResponseEntity.ok(stockTrackingService.getProductDetail(trackedProductId, storeId));
    }

    /**
     * Update alert settings for a tracked product
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PutMapping("/products/{trackedProductId}/settings")
    public ResponseEntity<TrackedProductDto> updateSettings(
            @PathVariable UUID trackedProductId,
            @RequestParam UUID storeId,
            @RequestBody UpdateAlertSettingsRequest request) {
        log.info("Updating settings for product: {}", trackedProductId);
        return ResponseEntity.ok(stockTrackingService.updateAlertSettings(trackedProductId, storeId, request));
    }

    /**
     * Manually trigger a stock check
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/products/{trackedProductId}/check")
    public ResponseEntity<TrackedProductDto> checkStockNow(
            @PathVariable UUID trackedProductId,
            @RequestParam UUID storeId) {
        log.info("Manual stock check for product: {}", trackedProductId);
        return ResponseEntity.ok(stockTrackingService.checkStockNow(trackedProductId, storeId));
    }

    /**
     * Remove a product from tracking
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @DeleteMapping("/products/{trackedProductId}")
    public ResponseEntity<Map<String, String>> removeProduct(
            @PathVariable UUID trackedProductId,
            @RequestParam UUID storeId) {
        log.info("Removing product: {} from tracking for store: {}", trackedProductId, storeId);
        stockTrackingService.removeProduct(trackedProductId, storeId);
        return ResponseEntity.ok(Map.of("message", "Product removed from tracking"));
    }

    /**
     * Get alerts for a store
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/alerts")
    public ResponseEntity<List<StockAlertDto>> getAlerts(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        log.debug("Getting alerts for store: {}, unreadOnly: {}", storeId, unreadOnly);
        return ResponseEntity.ok(stockTrackingService.getAlerts(storeId, unreadOnly));
    }

    /**
     * Mark an alert as read
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PutMapping("/alerts/{alertId}/read")
    public ResponseEntity<Map<String, String>> markAlertAsRead(
            @PathVariable UUID alertId,
            @RequestParam UUID storeId) {
        stockTrackingService.markAlertAsRead(alertId, storeId);
        return ResponseEntity.ok(Map.of("message", "Alert marked as read"));
    }

    /**
     * Mark all alerts as read for a store
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PutMapping("/stores/{storeId}/alerts/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAlertsAsRead(@PathVariable UUID storeId) {
        int count = stockTrackingService.markAllAlertsAsRead(storeId);
        return ResponseEntity.ok(Map.of("message", "All alerts marked as read", "count", count));
    }
}
