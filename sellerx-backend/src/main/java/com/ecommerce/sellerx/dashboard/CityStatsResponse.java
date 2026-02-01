package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for city statistics API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityStatsResponse {
    private List<CityStatsDto> cities;
    private Integer totalCities;
    private Long ordersWithoutCity;
}
