package com.ecommerce.sellerx.returns;

import com.ecommerce.sellerx.returns.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ClaimsController {

    private final TrendyolClaimsService claimsService;
    private final ReturnAnalyticsService returnAnalyticsService;

    /**
     * Get claims for a store with pagination
     * GET /api/returns/stores/{storeId}/claims
     */
    @GetMapping("/stores/{storeId}/claims")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<Page<ClaimDto>> getClaims(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String filter) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);

        // Handle filter parameter for grouped statuses
        if (filter != null) {
            switch (filter.toLowerCase()) {
                case "pending":
                    // Pending = WaitingInAction + Created
                    List<String> pendingStatuses = Arrays.asList("WaitingInAction", "Created");
                    return ResponseEntity.ok(claimsService.getClaimsByStatuses(storeId, pendingStatuses, pageable));
                case "approved":
                    return ResponseEntity.ok(claimsService.getClaims(storeId, "Accepted", pageable));
                case "rejected":
                    // Rejected includes Rejected and Unresolved
                    List<String> rejectedStatuses = Arrays.asList("Rejected", "Unresolved");
                    return ResponseEntity.ok(claimsService.getClaimsByStatuses(storeId, rejectedStatuses, pageable));
                default:
                    // "all" or unknown filter - return all
                    return ResponseEntity.ok(claimsService.getClaims(storeId, null, pageable));
            }
        }

        // Direct status filter
        Page<ClaimDto> claims = claimsService.getClaims(storeId, status, pageable);
        return ResponseEntity.ok(claims);
    }

    /**
     * Get single claim by Trendyol claim ID
     * GET /api/returns/stores/{storeId}/claims/{claimId}
     */
    @GetMapping("/stores/{storeId}/claims/{claimId}")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ClaimDto> getClaim(
            @PathVariable UUID storeId,
            @PathVariable String claimId) {

        ClaimDto claim = claimsService.getClaim(storeId, claimId);
        return ResponseEntity.ok(claim);
    }

    /**
     * Sync claims from Trendyol
     * POST /api/returns/stores/{storeId}/claims/sync
     */
    @PostMapping("/stores/{storeId}/claims/sync")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ClaimsSyncResponse> syncClaims(@PathVariable UUID storeId) {
        ClaimsSyncResponse response = claimsService.syncClaims(storeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve claim
     * PUT /api/returns/stores/{storeId}/claims/{claimId}/approve
     */
    @PutMapping("/stores/{storeId}/claims/{claimId}/approve")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ClaimActionResponse> approveClaim(
            @PathVariable UUID storeId,
            @PathVariable String claimId,
            @RequestBody ApproveClaimRequest request) {

        ClaimActionResponse response = claimsService.approveClaim(
                storeId, claimId, request.getClaimLineItemIds());
        return ResponseEntity.ok(response);
    }

    /**
     * Reject claim (create issue)
     * POST /api/returns/stores/{storeId}/claims/{claimId}/reject
     */
    @PostMapping("/stores/{storeId}/claims/{claimId}/reject")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ClaimActionResponse> rejectClaim(
            @PathVariable UUID storeId,
            @PathVariable String claimId,
            @RequestBody RejectClaimRequest request) {

        ClaimActionResponse response = claimsService.rejectClaim(
                storeId, claimId, request.getReasonId(),
                request.getClaimItemIds(), request.getDescription());
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk approve claims
     * POST /api/returns/stores/{storeId}/claims/bulk-approve
     */
    @PostMapping("/stores/{storeId}/claims/bulk-approve")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<BulkActionResponse> bulkApproveClaims(
            @PathVariable UUID storeId,
            @RequestBody BulkApproveRequest request) {

        int successCount = 0;
        int failCount = 0;

        for (BulkApproveRequest.ClaimApproval approval : request.getClaims()) {
            ClaimActionResponse result = claimsService.approveClaim(
                    storeId, approval.getClaimId(), approval.getClaimLineItemIds());
            if (result.isSuccess()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        return ResponseEntity.ok(BulkActionResponse.builder()
                .successCount(successCount)
                .failCount(failCount)
                .message(String.format("%d claims approved, %d failed", successCount, failCount))
                .build());
    }

    /**
     * Get claim issue reasons
     * GET /api/returns/claim-issue-reasons
     */
    @GetMapping("/claim-issue-reasons")
    public ResponseEntity<List<ClaimIssueReasonDto>> getClaimIssueReasons() {
        List<ClaimIssueReasonDto> reasons = claimsService.getClaimIssueReasons();
        return ResponseEntity.ok(reasons);
    }

    /**
     * Get claims statistics for a store
     * GET /api/returns/stores/{storeId}/stats
     */
    @GetMapping("/stores/{storeId}/stats")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ClaimsStatsDto> getStats(@PathVariable UUID storeId) {
        ClaimsStatsDto stats = claimsService.getStats(storeId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get audit trail for a claim item
     * GET /api/returns/stores/{storeId}/claims/items/{itemId}/audit
     */
    @GetMapping("/stores/{storeId}/claims/items/{itemId}/audit")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<List<ClaimItemAuditDto>> getClaimItemAudit(
            @PathVariable UUID storeId,
            @PathVariable String itemId) {
        List<ClaimItemAuditDto> audit = claimsService.getClaimItemAudit(storeId, itemId);
        return ResponseEntity.ok(audit);
    }

    // =====================================================
    // RETURNED ORDERS & RESALABLE DECISION
    // =====================================================

    /**
     * Get returned orders with cost breakdown for resalable decision.
     * GET /api/returns/stores/{storeId}/returned-orders?startDate=&endDate=
     */
    @GetMapping("/stores/{storeId}/returned-orders")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<List<ReturnedOrderDto>> getReturnedOrders(
            @PathVariable UUID storeId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<ReturnedOrderDto> orders = returnAnalyticsService.getReturnedOrders(storeId, start, end);
        return ResponseEntity.ok(orders);
    }

    /**
     * Update resalable decision for a returned order.
     * PUT /api/returns/stores/{storeId}/orders/{orderNumber}/resalable
     */
    @PutMapping("/stores/{storeId}/orders/{orderNumber}/resalable")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<Void> updateResalable(
            @PathVariable UUID storeId,
            @PathVariable String orderNumber,
            @RequestBody UpdateResalableRequest request) {

        returnAnalyticsService.updateResalable(storeId, orderNumber, request.getIsResalable());
        return ResponseEntity.ok().build();
    }

    @lombok.Data
    public static class UpdateResalableRequest {
        private Boolean isResalable;
    }

    // =====================================================
    // TEST ENDPOINTS (for API limit discovery)
    // =====================================================

    /**
     * Test Claims API date range limit
     * POST /api/returns/stores/{storeId}/test-date-range
     *
     * Used to discover how far back the Trendyol Claims API allows querying
     */
    @PostMapping("/stores/{storeId}/test-date-range")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ClaimsDateRangeTestResult> testDateRange(
            @PathVariable UUID storeId,
            @RequestBody ClaimsDateRangeTestRequest request) {

        ClaimsDateRangeTestResult result = claimsService.testDateRange(storeId, request.getYearsBack());
        return ResponseEntity.ok(result);
    }

    /**
     * Test multiple date ranges (1-5 years)
     * POST /api/returns/stores/{storeId}/test-date-ranges
     *
     * Batch test to find maximum supported date range
     */
    @PostMapping("/stores/{storeId}/test-date-ranges")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<ClaimsDateRangeTestResult> testMultipleDateRanges(@PathVariable UUID storeId) {
        ClaimsDateRangeTestResult result = claimsService.testMultipleDateRanges(storeId);
        return ResponseEntity.ok(result);
    }

    // =====================================================
    // REQUEST DTOs (Inner Classes)
    // =====================================================

    @lombok.Data
    public static class ApproveClaimRequest {
        private List<String> claimLineItemIds;
    }

    @lombok.Data
    public static class RejectClaimRequest {
        private Integer reasonId;
        private List<String> claimItemIds;
        private String description;
    }

    @lombok.Data
    public static class BulkApproveRequest {
        private List<ClaimApproval> claims;

        @lombok.Data
        public static class ClaimApproval {
            private String claimId;
            private List<String> claimLineItemIds;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BulkActionResponse {
        private int successCount;
        private int failCount;
        private String message;
    }
}
