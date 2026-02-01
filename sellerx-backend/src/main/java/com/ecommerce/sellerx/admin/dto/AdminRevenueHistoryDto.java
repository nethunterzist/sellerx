package com.ecommerce.sellerx.admin.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueHistoryDto {
    private String month;
    private BigDecimal totalRevenue;
    private Long subscriptionCount;
}
