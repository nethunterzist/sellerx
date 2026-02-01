package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminProductStatsDto;
import com.ecommerce.sellerx.admin.dto.AdminTopProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    /**
     * Get platform-wide product statistics.
     * GET /api/admin/products/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminProductStatsDto> getProductStats() {
        log.info("Admin requesting product stats");
        AdminProductStatsDto stats = adminProductService.getProductStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get top products by order count across all stores.
     * GET /api/admin/products/top
     */
    @GetMapping("/top")
    public ResponseEntity<List<AdminTopProductDto>> getTopProducts() {
        log.info("Admin requesting top products");
        List<AdminTopProductDto> topProducts = adminProductService.getTopProducts();
        return ResponseEntity.ok(topProducts);
    }
}
