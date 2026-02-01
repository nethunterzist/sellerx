package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/billing")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminBillingController {

    private final AdminBillingService billingService;

    @GetMapping("/subscriptions")
    public ResponseEntity<Page<AdminSubscriptionListDto>> getSubscriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String planName,
            Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        log.info("Admin fetching subscriptions - status: {}, plan: {}", status, planName);
        return ResponseEntity.ok(billingService.getSubscriptions(status, planName, pageable));
    }

    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<AdminSubscriptionDetailDto> getSubscriptionDetail(@PathVariable Long id) {
        log.info("Admin fetching subscription detail for id: {}", id);
        return ResponseEntity.ok(billingService.getSubscriptionDetail(id));
    }

    @GetMapping("/revenue/stats")
    public ResponseEntity<AdminRevenueStatsDto> getRevenueStats() {
        log.info("Admin fetching revenue stats");
        return ResponseEntity.ok(billingService.getRevenueStats());
    }

    @GetMapping("/revenue/history")
    public ResponseEntity<List<AdminRevenueHistoryDto>> getRevenueHistory() {
        log.info("Admin fetching revenue history");
        return ResponseEntity.ok(billingService.getRevenueHistory());
    }

    @GetMapping("/payments")
    public ResponseEntity<Page<AdminPaymentDto>> getPayments(Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        log.info("Admin fetching payments");
        return ResponseEntity.ok(billingService.getPayments(pageable));
    }
}
