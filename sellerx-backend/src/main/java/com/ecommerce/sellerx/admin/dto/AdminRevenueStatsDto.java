package com.ecommerce.sellerx.admin.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueStatsDto {
    private BigDecimal mrr;
    private BigDecimal arr;
    private Long activeCount;
    private Long trialCount;
    private Long cancelledCount;
    private Double churnRate;
    private Long totalUsers;
    private Boolean billingEnabled;
}
