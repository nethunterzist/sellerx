package com.ecommerce.sellerx.products.dto;

import lombok.Builder;

@Builder
public record BuyboxSummaryDto(
    int totalProducts,
    int buyboxWinning,
    int buyboxLosing,
    int withCompetitors,
    int noCompetition,
    int notChecked,
    double winRate
) {}
