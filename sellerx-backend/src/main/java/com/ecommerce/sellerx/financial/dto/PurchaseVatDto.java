package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for purchase-based VAT calculation on KDV page.
 * Shows total cost VAT from stock purchases (purchase orders) in a period,
 * based on stockEntryDate — reflecting when VAT liability actually occurs
 * per Turkish tax law.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseVatDto {
    private BigDecimal totalPurchaseCostExclVat;
    private BigDecimal totalPurchaseVatAmount;
    private int totalItemsPurchased;
    private List<PurchaseVatByRate> byRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseVatByRate {
        private int vatRate;
        private BigDecimal costAmount;
        private BigDecimal vatAmount;
        private int itemCount;
    }
}
