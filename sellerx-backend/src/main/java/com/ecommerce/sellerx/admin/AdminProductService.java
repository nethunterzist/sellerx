package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminProductStatsDto;
import com.ecommerce.sellerx.admin.dto.AdminTopProductDto;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
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
    private final StoreRepository storeRepository;

    /**
     * Get platform-wide product statistics for the admin panel.
     */
    public AdminProductStatsDto getProductStats() {
        log.info("Admin fetching product stats");

        List<TrendyolProduct> allProducts = productRepository.findAll();
        long totalProducts = allProducts.size();

        // Products by store (storeName -> count)
        Map<String, Long> productsByStore = new LinkedHashMap<>();
        long withCostCount = 0;
        long withoutCostCount = 0;

        for (TrendyolProduct product : allProducts) {
            // Count by store
            String storeName = product.getStore() != null ? product.getStore().getStoreName() : "Unknown";
            productsByStore.merge(storeName, 1L, Long::sum);

            // Check if product has cost info (non-empty costAndStockInfo list with at least one entry having unitCost)
            boolean hasCost = product.getCostAndStockInfo() != null
                    && !product.getCostAndStockInfo().isEmpty()
                    && product.getCostAndStockInfo().stream()
                        .anyMatch(info -> info.getUnitCost() != null && info.getUnitCost() > 0);

            if (hasCost) {
                withCostCount++;
            } else {
                withoutCostCount++;
            }
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
