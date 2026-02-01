package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for city-level order statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityStatsDto {
    private String cityName;
    private Integer cityCode;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Long totalQuantity;
    private BigDecimal averageOrderValue;
}
