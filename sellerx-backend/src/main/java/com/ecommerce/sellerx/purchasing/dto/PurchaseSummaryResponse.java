package com.ecommerce.sellerx.purchasing.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseSummaryResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalPurchaseAmount;
    private Integer totalUnits;
    private Integer totalOrders;
    private BigDecimal averageCostPerUnit;
    private List<SupplierBreakdown> supplierBreakdown;
    private List<ProductPurchase> topProductsByAmount;
    private List<MonthlyPurchase> monthlyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierBreakdown {
        private String supplierName;
        private BigDecimal totalAmount;
        private Integer orderCount;
        private Integer totalUnits;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPurchase {
        private UUID productId;
        private String productName;
        private String barcode;
        private String productImage;
        private Integer totalUnits;
        private BigDecimal totalAmount;
        private BigDecimal averageCost;
        private BigDecimal costChange; // % change from first to last purchase
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyPurchase {
        private Integer year;
        private Integer month;
        private String monthName;
        private BigDecimal totalAmount;
        private Integer totalUnits;
        private Integer orderCount;
    }
}
