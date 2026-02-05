package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortData {
    private String cohortMonth;
    private int cohortSize;
    private Map<String, Double> retentionRates; // month -> retention percentage
}
