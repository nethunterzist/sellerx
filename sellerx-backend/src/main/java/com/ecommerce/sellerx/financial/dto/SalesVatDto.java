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
    private List<ProductSalesVatDto> byProduct;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesVatDto {
        private String barcode;
        private String productName;
        private int quantity;
        private BigDecimal salesAmount;
        private BigDecimal vatAmount;
        private Integer vatRate;
        // Product enrichment fields
        private String image;          // Product image URL
        private String brand;          // Brand name
        private String productUrl;     // Trendyol product page URL
    }
}
