package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.orders.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/customer-analytics")
@RequiredArgsConstructor
public class CustomerAnalyticsController {

    private final CustomerAnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<CustomerAnalyticsResponse> getSummary(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getAnalytics(storeId));
    }

    @GetMapping("/lifecycle")
    public ResponseEntity<List<LifecycleStageData>> getLifecycleStages(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getLifecycleStages(storeId));
    }

    @GetMapping("/cohorts")
    public ResponseEntity<List<CohortData>> getCohortAnalysis(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getCohortAnalysis(storeId));
    }

    @GetMapping("/frequency-distribution")
    public ResponseEntity<List<FrequencyDistributionData>> getFrequencyDistribution(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getFrequencyDistribution(storeId));
    }

    @GetMapping("/clv-summary")
    public ResponseEntity<ClvSummaryData> getClvSummary(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getClvSummary(storeId));
    }

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> getCustomers(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "totalSpend") String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(analyticsService.getCustomerList(storeId, page, size, sort, search));
    }

    @GetMapping("/product-repeat")
    public ResponseEntity<List<ProductRepeatData>> getProductRepeat(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getProductRepeatAnalysis(storeId));
    }

    @GetMapping("/cross-sell")
    public ResponseEntity<List<CrossSellData>> getCrossSell(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getCrossSellAnalysis(storeId));
    }

    @GetMapping("/backfill-status")
    public ResponseEntity<Map<String, Object>> getBackfillStatus(@PathVariable UUID storeId) {
        return ResponseEntity.ok(analyticsService.getBackfillCoverage(storeId));
    }

    @PostMapping("/trigger-backfill")
    public ResponseEntity<Map<String, String>> triggerBackfill(@PathVariable UUID storeId) {
        analyticsService.triggerCustomerDataBackfill(storeId);
        return ResponseEntity.ok(Map.of("status", "started", "message", "Customer data backfill started from first order date (14-day chunks)"));
    }

}
