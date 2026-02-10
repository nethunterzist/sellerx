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
public class CustomerAnalyticsSummary {
    private int totalCustomers;
    private int repeatCustomers;
    private double repeatRate;
    private double avgOrdersPerCustomer;
    private double avgItemsPerCustomer;
    private double avgItemsPerOrder;
    private double avgRepeatIntervalDays;
    private BigDecimal repeatCustomerRevenue;
    private BigDecimal totalRevenue;
    private double repeatRevenueShare;
    private BigDecimal avgOrderValue;  // Average order value (total revenue / total orders)
}
