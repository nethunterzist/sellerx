package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrequencyDistributionData {
    private String bucket;        // "1", "2", "3", "4", "5+"
    private int customerCount;
    private BigDecimal totalRevenue;
    private int totalOrders;
    private double percentage;    // percentage of total customers
    private double revenueShare;  // percentage of total revenue
}
