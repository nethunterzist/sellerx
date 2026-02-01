package com.ecommerce.sellerx.buybox.dto;

import com.ecommerce.sellerx.buybox.BuyboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Buybox ürün detay DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxProductDetailDto {

    private UUID id;
    private UUID storeId;
    private UUID productId;

    // Ürün bilgileri
    private String productTitle;
    private String productBarcode;
    private String productImageUrl;
    private String trendyolProductId;
    private String trendyolUrl;

    // Takip ayarları
    private boolean isActive;
    private boolean alertOnLoss;
    private boolean alertOnNewCompetitor;
    private BigDecimal alertPriceThreshold;

    // Mevcut buybox durumu
    private BuyboxStatus currentStatus;
    private BigDecimal winnerPrice;
    private String winnerName;
    private Long winnerMerchantId;
    private BigDecimal winnerSellerScore;
    private BigDecimal myPrice;
    private Integer myPosition;
    private BigDecimal priceDifference;
    private Integer totalSellers;
    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;
    private LocalDateTime lastCheckedAt;

    // Rakipler
    private List<MerchantInfo> competitors;

    // Geçmiş
    private List<BuyboxSnapshotDto> history;

    // Alertler
    private List<BuyboxAlertDto> recentAlerts;

    private LocalDateTime createdAt;
}
