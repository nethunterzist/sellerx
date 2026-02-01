package com.ecommerce.sellerx.returns.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReturnAnalyticsResponse {
    // Summary statistics
    private int totalReturns;
    private int totalReturnedItems;
    private BigDecimal totalReturnLoss;
    private BigDecimal returnRate; // % (returns / total orders)
    private BigDecimal avgLossPerReturn;

    // Cost breakdown
    private ReturnCostBreakdown costBreakdown;

    // Top returned products
    private List<TopReturnedProduct> topReturnedProducts;

    // Return reason distribution
    private Map<String, Integer> returnReasonDistribution;

    // Daily trend
    private List<DailyReturnStats> dailyTrend;

    // Period info
    private String startDate;
    private String endDate;
    private String calculatedAt;
}
