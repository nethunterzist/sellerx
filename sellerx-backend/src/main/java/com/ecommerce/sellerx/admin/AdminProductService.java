package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminProductStatsDto;
import com.ecommerce.sellerx.admin.dto.AdminTopProductDto;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminProductService {

    private final TrendyolProductRepository productRepository;
    private final TrendyolOrderRepository orderRepository;

    /**
     * Get platform-wide product statistics for the admin panel.
     * Uses aggregation queries instead of loading all products into memory.
     */
    public AdminProductStatsDto getProductStats() {
        log.info("Admin fetching product stats using aggregation queries");

        // Use COUNT instead of loading all products
        long totalProducts = productRepository.count();

        // Get products with cost info using indexed query
        long withCostCount = productRepository.countWithCostInfo();
        long withoutCostCount = totalProducts - withCostCount;

        // Get products by store using GROUP BY query
        Map<String, Long> productsByStore = new LinkedHashMap<>();
        List<Object[]> storeStats = productRepository.countProductsByStore();
        for (Object[] row : storeStats) {
            String storeName = (String) row[0];
            Long productCount = ((Number) row[1]).longValue();
            productsByStore.put(storeName != null ? storeName : "Unknown", productCount);
        }

        return AdminProductStatsDto.builder()
                .totalProducts(totalProducts)
                .productsByStore(productsByStore)
                .withCostCount(withCostCount)
                .withoutCostCount(withoutCostCount)
                .build();
    }

    /**
     * Get top products by order count across all stores.
     * Uses a native query to aggregate order items from JSONB and rank products.
     */
    public List<AdminTopProductDto> getTopProducts() {
        log.info("Admin fetching top products");

        var results = orderRepository.findTopProductsByOrderCount();
        List<AdminTopProductDto> topProducts = new ArrayList<>();

        for (var row : results) {
            topProducts.add(AdminTopProductDto.builder()
                    .barcode(row.getBarcode())
                    .title(row.getTitle())
                    .storeName(row.getStoreName())
                    .orderCount(row.getOrderCount() != null ? row.getOrderCount() : 0L)
                    .totalRevenue(row.getTotalRevenue() != null ? row.getTotalRevenue() : BigDecimal.ZERO)
                    .build());
        }

        return topProducts;
    }
}
