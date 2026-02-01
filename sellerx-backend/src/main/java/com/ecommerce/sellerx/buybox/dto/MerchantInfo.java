package com.ecommerce.sellerx.buybox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Trendyol API'sinden gelen satıcı bilgisi DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantInfo {

    private Long merchantId;
    private String merchantName;
    private BigDecimal price;
    private BigDecimal sellerScore;
    private boolean isWinner;
    private boolean hasStock;
    private boolean isFreeCargo;
    private String deliveryDate;
}
