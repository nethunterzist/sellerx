package com.ecommerce.sellerx.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeniorityStatsDto {
    private long totalPatterns;
    private long juniorCount;
    private long learningCount;
    private long seniorCount;
    private long expertCount;
    private long autoSubmitEligibleCount;
}
