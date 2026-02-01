package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReferralStatsDto {
    private long total;
    private long completed;
    private long pending;
    private long totalRewardDays;
    private List<TopReferrerDto> topReferrers;
}
