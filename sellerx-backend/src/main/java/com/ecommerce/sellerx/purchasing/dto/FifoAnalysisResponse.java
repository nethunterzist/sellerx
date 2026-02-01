package com.ecommerce.sellerx.purchasing.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FifoAnalysisResponse {
    private String barcode;
    private String productName;
    private UUID productId;
    private String productImage;
    private List<FifoLot> lots;
    private BigDecimal totalCost;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private Double profitMargin;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FifoLot {
        private LocalDate stockDate;
        private Integer originalQuantity;
        private Integer usedQuantity;
        private Integer remainingQuantity;
        private BigDecimal unitCost;
        private Integer vatRate;
        private List<OrderAllocation> allocations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderAllocation {
        private String orderNumber;
        private LocalDateTime orderDate;
        private Integer quantity;
        private BigDecimal costPerUnit;
        private BigDecimal salePrice;
        private BigDecimal profit;
        private Double profitMargin;
    }
}
