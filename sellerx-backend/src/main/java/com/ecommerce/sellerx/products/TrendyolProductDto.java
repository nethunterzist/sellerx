package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrendyolProductDto {
    private UUID id;
    private UUID storeId;
    private String productId;
    private String barcode;
    private String title;
    private String categoryName;
    private Long createDateTime;
    private Boolean hasActiveCampaign;
    private String brand;
    private Long brandId;
    private Long pimCategoryId;
    private String productMainId;
    private String image;
    private String productUrl;
    private BigDecimal dimensionalWeight;
    private BigDecimal salePrice;
    private Integer vatRate;
    private Integer trendyolQuantity;
    private BigDecimal commissionRate;
    private BigDecimal shippingVolumeWeight;

    // ============== Reklam Metrikleri (Excel C23, C24) ==============
    private BigDecimal cpc; // Cost Per Click (TL)
    private BigDecimal cvr; // Conversion Rate (örn: 0.018 = %1.8)
    private BigDecimal advertisingCostPerSale; // Hesaplanan: cpc / cvr
    private BigDecimal acos; // ACOS: advertisingCostPerSale / salePrice

    // ============== Döviz Kuru (Excel F1) ==============
    private String defaultCurrency; // "TRY", "USD", "EUR"
    private BigDecimal defaultExchangeRate; // Varsayılan döviz kuru

    // ============== ÖTV (Excel F5) ==============
    private BigDecimal otvRate; // Özel Tüketim Vergisi oranı

    // ============== Kargo Maliyeti ==============
    private BigDecimal lastShippingCostPerUnit; // Son kargo faturasından hesaplanan birim kargo maliyeti

    // ============== Buybox Bilgileri ==============
    private Integer buyboxOrder;
    private BigDecimal buyboxPrice;
    private Boolean hasMultipleSeller;
    private LocalDateTime buyboxUpdatedAt;

    private Boolean approved;
    private Boolean archived;
    private Boolean blacklisted;
    private Boolean rejected;
    private Boolean onSale;
    private List<CostAndStockInfo> costAndStockInfo;
    private Boolean hasAutoDetectedCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
