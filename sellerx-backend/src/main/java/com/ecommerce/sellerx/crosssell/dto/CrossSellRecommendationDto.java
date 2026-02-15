package com.ecommerce.sellerx.crosssell.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellRecommendationDto {
    private UUID ruleId;
    private String ruleName;
    private String recommendationText;
    private String productBarcode;
    private String productTitle;
    private String productImage;
    private BigDecimal productPrice;
    private String productUrl;
}
