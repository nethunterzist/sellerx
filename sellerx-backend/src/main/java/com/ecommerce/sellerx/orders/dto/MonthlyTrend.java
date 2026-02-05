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
public class MonthlyTrend {
    private String month;
    private int newCustomers;
    private int repeatCustomers;
    private BigDecimal newRevenue;
    private BigDecimal repeatRevenue;
}
