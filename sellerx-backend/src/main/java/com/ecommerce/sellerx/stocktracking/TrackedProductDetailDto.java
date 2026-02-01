package com.ecommerce.sellerx.stocktracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for detailed view of a tracked product with history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackedProductDetailDto {
    private UUID id;
    private Long trendyolProductId;
    private String productUrl;
    private String productName;
    private String brandName;
    private String imageUrl;

    // Current state
    private Integer lastStockQuantity;
    private BigDecimal lastPrice;
    private LocalDateTime lastCheckedAt;
    private Boolean isActive;

    // All alert settings
    private Boolean alertOnOutOfStock;
    private Boolean alertOnLowStock;
    private Integer lowStockThreshold;
    private Boolean alertOnStockIncrease;
    private Boolean alertOnBackInStock;
    private Integer checkIntervalHours;

    // History
    private List<StockSnapshotDto> recentSnapshots;
    private List<StockAlertDto> recentAlerts;

    // Statistics
    private StockStatistics statistics;

    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockStatistics {
        private Integer minStock;
        private Integer maxStock;
        private Double avgStock;
        private Integer totalChecks;
        private Integer outOfStockCount;
        private LocalDateTime lastOutOfStock;
    }

    public static TrackedProductDetailDto fromEntity(
            StockTrackedProduct entity,
            List<StockSnapshotDto> snapshots,
            List<StockAlertDto> alerts,
            StockStatistics stats) {

        return TrackedProductDetailDto.builder()
                .id(entity.getId())
                .trendyolProductId(entity.getTrendyolProductId())
                .productUrl(entity.getProductUrl())
                .productName(entity.getProductName())
                .brandName(entity.getBrandName())
                .imageUrl(entity.getImageUrl())
                .lastStockQuantity(entity.getLastStockQuantity())
                .lastPrice(entity.getLastPrice())
                .lastCheckedAt(entity.getLastCheckedAt())
                .isActive(entity.getIsActive())
                .alertOnOutOfStock(entity.getAlertOnOutOfStock())
                .alertOnLowStock(entity.getAlertOnLowStock())
                .lowStockThreshold(entity.getLowStockThreshold())
                .alertOnStockIncrease(entity.getAlertOnStockIncrease())
                .alertOnBackInStock(entity.getAlertOnBackInStock())
                .checkIntervalHours(entity.getCheckIntervalHours())
                .recentSnapshots(snapshots)
                .recentAlerts(alerts)
                .statistics(stats)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
