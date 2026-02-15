package com.ecommerce.sellerx.crosssell.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellAnalyticsDto {
    private long totalRecommendations;
    private long includedInAnswers;
    private long totalRules;
    private long activeRules;
}
