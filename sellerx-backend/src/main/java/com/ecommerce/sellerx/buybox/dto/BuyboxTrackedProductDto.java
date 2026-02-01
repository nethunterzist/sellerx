package com.ecommerce.sellerx.buybox.dto;

import com.ecommerce.sellerx.buybox.BuyboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Takip edilen ürün DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxTrackedProductDto {

    private UUID id;
    private UUID storeId;
    private UUID productId;

    // Ürün bilgileri
    private String productTitle;
    private String productBarcode;
    private String productImageUrl;
    private String trendyolProductId;

    // Takip ayarları
    private boolean isActive;
    private boolean alertOnLoss;
    private boolean alertOnNewCompetitor;
    private BigDecimal alertPriceThreshold;

    // Son buybox durumu
    private BuyboxStatus lastStatus;
    private BigDecimal lastWinnerPrice;
    private String lastWinnerName;
    private BigDecimal myPrice;
    private BigDecimal priceDifference;
    private Integer myPosition;
    private Integer totalSellers;
    private LocalDateTime lastCheckedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
