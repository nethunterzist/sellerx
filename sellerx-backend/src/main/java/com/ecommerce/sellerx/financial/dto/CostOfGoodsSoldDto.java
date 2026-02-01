package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for Cost of Goods Sold (COGS) in KDV page.
 * Shows total cost and cost VAT for products sold in a period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostOfGoodsSoldDto {
    private BigDecimal totalCostIncludingVat;
    private BigDecimal totalCostVatAmount;
    private int totalItemsSold;
    private int itemsWithoutCost;
    private int itemsWithoutCostVat;
}
