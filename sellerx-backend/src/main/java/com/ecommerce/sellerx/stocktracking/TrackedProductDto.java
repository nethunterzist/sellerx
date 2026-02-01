package com.ecommerce.sellerx.stocktracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for list view of tracked products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackedProductDto {
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

    // Alert settings summary
    private Boolean alertOnOutOfStock;
    private Boolean alertOnLowStock;
    private Integer lowStockThreshold;

    // Stats
    private Integer unreadAlertCount;

    private LocalDateTime createdAt;

    public static TrackedProductDto fromEntity(StockTrackedProduct entity) {
        return TrackedProductDto.builder()
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
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
