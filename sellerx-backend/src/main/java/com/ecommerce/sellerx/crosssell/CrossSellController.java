package com.ecommerce.sellerx.crosssell;

import com.ecommerce.sellerx.crosssell.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cross-sell")
@RequiredArgsConstructor
public class CrossSellController {

    private final CrossSellService crossSellService;

    // ==================== Settings ====================

    @GetMapping("/stores/{storeId}/settings")
    public ResponseEntity<CrossSellSettingsDto> getSettings(@PathVariable UUID storeId) {
        return ResponseEntity.ok(crossSellService.getSettings(storeId));
    }

    @PutMapping("/stores/{storeId}/settings")
    public ResponseEntity<CrossSellSettingsDto> updateSettings(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateCrossSellSettingsRequest request) {
        return ResponseEntity.ok(crossSellService.updateSettings(storeId, request));
    }

    // ==================== Rules CRUD ====================

    @GetMapping("/stores/{storeId}/rules")
    public ResponseEntity<List<CrossSellRuleDto>> getRules(@PathVariable UUID storeId) {
        return ResponseEntity.ok(crossSellService.getRules(storeId));
    }

    @GetMapping("/rules/{ruleId}")
    public ResponseEntity<CrossSellRuleDto> getRule(@PathVariable UUID ruleId) {
        return ResponseEntity.ok(crossSellService.getRule(ruleId));
    }

    @PostMapping("/stores/{storeId}/rules")
    public ResponseEntity<CrossSellRuleDto> createRule(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateCrossSellRuleRequest request) {
        return ResponseEntity.ok(crossSellService.createRule(storeId, request));
    }

    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<CrossSellRuleDto> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateCrossSellRuleRequest request) {
        return ResponseEntity.ok(crossSellService.updateRule(ruleId, request));
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        crossSellService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rules/{ruleId}/toggle")
    public ResponseEntity<Void> toggleRule(
            @PathVariable UUID ruleId,
            @RequestParam boolean active) {
        crossSellService.toggleRule(ruleId, active);
        return ResponseEntity.ok().build();
    }

    // ==================== Analytics ====================

    @GetMapping("/stores/{storeId}/analytics")
    public ResponseEntity<CrossSellAnalyticsDto> getAnalytics(@PathVariable UUID storeId) {
        return ResponseEntity.ok(crossSellService.getAnalytics(storeId));
    }

    // ==================== Product Search ====================

    @GetMapping("/stores/{storeId}/products/search")
    public ResponseEntity<List<ProductSearchResultDto>> searchProducts(
            @PathVariable UUID storeId,
            @RequestParam("q") String query) {
        return ResponseEntity.ok(crossSellService.searchProducts(storeId, query));
    }
}
