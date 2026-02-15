package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.orders.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/customer-analytics")
@RequiredArgsConstructor
public class CustomerAnalyticsController {

    private final CustomerAnalyticsService analyticsService;

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/summary")
    public ResponseEntity<CustomerAnalyticsResponse> getSummary(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getAnalytics(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/lifecycle")
    public ResponseEntity<List<LifecycleStageData>> getLifecycleStages(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getLifecycleStages(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/cohorts")
    public ResponseEntity<List<CohortData>> getCohortAnalysis(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getCohortAnalysis(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/frequency-distribution")
    public ResponseEntity<List<FrequencyDistributionData>> getFrequencyDistribution(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getFrequencyDistribution(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/clv-summary")
    public ResponseEntity<ClvSummaryData> getClvSummary(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getClvSummary(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> getCustomers(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "totalSpend") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            // Filter parameters
            @RequestParam(required = false) Integer minOrderCount,
            @RequestParam(required = false) Integer maxOrderCount,
            @RequestParam(required = false) Integer minItemCount,
            @RequestParam(required = false) Integer maxItemCount,
            @RequestParam(required = false) Double minTotalSpend,
            @RequestParam(required = false) Double maxTotalSpend,
            @RequestParam(required = false) Double minAvgOrderValue,
            @RequestParam(required = false) Double maxAvgOrderValue,
            @RequestParam(required = false) Double minRepeatInterval,
            @RequestParam(required = false) Double maxRepeatInterval) {

        CustomerListFilter filter = CustomerListFilter.builder()
                .minOrderCount(minOrderCount)
                .maxOrderCount(maxOrderCount)
                .minItemCount(minItemCount)
                .maxItemCount(maxItemCount)
                .minTotalSpend(minTotalSpend != null ? java.math.BigDecimal.valueOf(minTotalSpend) : null)
                .maxTotalSpend(maxTotalSpend != null ? java.math.BigDecimal.valueOf(maxTotalSpend) : null)
                .minAvgOrderValue(minAvgOrderValue != null ? java.math.BigDecimal.valueOf(minAvgOrderValue) : null)
                .maxAvgOrderValue(maxAvgOrderValue != null ? java.math.BigDecimal.valueOf(maxAvgOrderValue) : null)
                .minRepeatInterval(minRepeatInterval)
                .maxRepeatInterval(maxRepeatInterval)
                .build();

        return ResponseEntity.ok(analyticsService.getCustomerList(storeId, page, size, sortBy, sortDir, search, filter));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/customers/{customerId}/orders")
    public ResponseEntity<CustomerOrdersPageDto> getCustomerOrders(
            @PathVariable UUID storeId,
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(analyticsService.getCustomerOrdersPaginated(storeId, customerId, page, size));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/product-repeat")
    public ResponseEntity<List<ProductRepeatData>> getProductRepeat(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getProductRepeatAnalysis(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/products/{barcode}")
    public ResponseEntity<ProductDetailDto> getProductDetail(
            @PathVariable UUID storeId,
            @PathVariable String barcode) {
        return ResponseEntity.ok(analyticsService.getProductDetail(storeId, barcode));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/products/{barcode}/buyers")
    public ResponseEntity<ProductBuyersPageDto> getProductBuyers(
            @PathVariable UUID storeId,
            @PathVariable String barcode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(analyticsService.getProductBuyers(storeId, barcode, page, size));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/cross-sell")
    public ResponseEntity<List<CrossSellData>> getCrossSell(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getCrossSellAnalysis(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/backfill-status")
    public ResponseEntity<Map<String, Object>> getBackfillStatus(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getBackfillCoverage(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/trigger-backfill")
    public ResponseEntity<Map<String, String>> triggerBackfill(@PathVariable UUID storeId) {
        analyticsService.triggerCustomerDataBackfill(storeId);
        return ResponseEntity.ok(Map.of("status", "started", "message", "Customer data backfill started from first order date (14-day chunks)"));
    }

}
