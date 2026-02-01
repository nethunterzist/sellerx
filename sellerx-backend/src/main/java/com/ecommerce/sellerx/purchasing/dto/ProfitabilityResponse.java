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
public class ProfitabilityResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private Double grossMargin;
    private Integer totalOrders;
    private Integer totalQuantitySold;
    private List<ProductProfitability> topProfitable;
    private List<ProductProfitability> leastProfitable;
    private List<DailyProfitability> dailyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductProfitability {
        private UUID productId;
        private String productName;
        private String barcode;
        private String productImage;
        private Integer quantitySold;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private Double margin;
        private String marginCategory; // "high" (>30%), "medium" (15-30%), "low" (<15%)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyProfitability {
        private LocalDate date;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private Double margin;
        private Integer orderCount;
    }
}
