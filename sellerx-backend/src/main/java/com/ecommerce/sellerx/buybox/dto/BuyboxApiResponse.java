package com.ecommerce.sellerx.buybox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trendyol Buybox API yanıt DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxApiResponse {

    private boolean success;
    private String errorMessage;

    private String productName;
    private String brandName;
    private BigDecimal averageRating;

    private List<MerchantInfo> allMerchants;
    private MerchantInfo winner;

    private int totalSellers;
    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;
}
