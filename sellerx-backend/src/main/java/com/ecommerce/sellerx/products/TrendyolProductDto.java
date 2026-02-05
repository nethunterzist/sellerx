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
