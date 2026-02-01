package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductStatsDto {
    private long totalProducts;
    private Map<String, Long> productsByStore;
    private long withCostCount;
    private long withoutCostCount;
}
