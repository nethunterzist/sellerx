package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsResponse {
    private CustomerAnalyticsSummary summary;
    private List<SegmentData> segmentation;
    private List<CityRepeatData> cityAnalysis;
    private List<MonthlyTrend> monthlyTrend;
}
