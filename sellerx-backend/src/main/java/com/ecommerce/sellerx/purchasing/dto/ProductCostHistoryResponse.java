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
public class ProductCostHistoryResponse {
    private UUID productId;
    private String productName;
    private String barcode;
    private String productImage;
    private List<CostEntry> entries;
    private BigDecimal averageCost;
    private BigDecimal totalValue;
    private Integer totalQuantity;
    private Integer remainingQuantity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostEntry {
        private LocalDate stockDate;
        private Integer quantity;
        private Integer usedQuantity;
        private Integer remainingQuantity;
        private BigDecimal unitCost;
        private Integer vatRate;
        private BigDecimal totalValue;
        private Double usagePercentage;
        private Long purchaseOrderId;
        private String purchaseOrderNumber;
    }
}
