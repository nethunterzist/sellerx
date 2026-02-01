package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminOrderStatsDto;
import com.ecommerce.sellerx.admin.dto.AdminRecentOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * Get platform-wide order statistics.
     * GET /api/admin/orders/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminOrderStatsDto> getOrderStats() {
        log.info("Admin requesting order stats");
        AdminOrderStatsDto stats = adminOrderService.getOrderStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get recent orders across all stores (paginated).
     * GET /api/admin/orders/recent?page=0&size=20&sort=orderDate,desc
     */
    @GetMapping("/recent")
    public ResponseEntity<Page<AdminRecentOrderDto>> getRecentOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        log.info("Admin requesting recent orders, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<AdminRecentOrderDto> orders = adminOrderService.getRecentOrders(pageable);
        return ResponseEntity.ok(orders);
    }
}
