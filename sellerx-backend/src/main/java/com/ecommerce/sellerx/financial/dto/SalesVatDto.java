package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Sales VAT (Satış KDV'si) in KDV page.
 * Shows total sales VAT collected on delivered orders in a period,
 * with breakdown by VAT rate (1%, 10%, 20%).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesVatDto {
    private BigDecimal totalSalesAmount;
    private BigDecimal totalVatAmount;
    private int totalItemsSold;
    private int itemsWithoutVatRate;
    private List<SalesVatByRateDto> byRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesVatByRateDto {
        private int vatRate;
        private BigDecimal salesAmount;
        private BigDecimal vatAmount;
        private int itemCount;
    }
}
