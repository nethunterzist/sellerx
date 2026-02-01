package com.ecommerce.sellerx.products;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class TrendyolProductController {
    
    private final TrendyolProductService trendyolProductService;
    
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<SyncProductsResponse> syncProductsFromTrendyol(@PathVariable UUID storeId) {
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
            @RequestParam(defaultValue = "onSale") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        size = Math.min(size, 100);
        ProductListResponse<TrendyolProductDto> products = trendyolProductService.getProductsByStoreWithPagination(
                storeId, page, size, search, sortBy, sortDirection);
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
