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
public class StockValuationResponse {
    private List<ProductValuation> products;
    private BigDecimal totalValue;
    private Integer totalProducts;
    private Integer totalQuantity;
    private AgingBreakdown aging;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductValuation {
        private UUID productId;
        private String productName;
        private String barcode;
        private String productImage;
        private Integer quantity;
        private BigDecimal fifoValue;
        private BigDecimal averageCost;
        private LocalDate oldestStockDate;
        private Integer daysInStock;
        private String agingCategory; // "0-30", "30-60", "60-90", "90+"
        private Boolean stockDepleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingBreakdown {
        private BigDecimal days0to30;
        private Integer count0to30;
        private BigDecimal days30to60;
        private Integer count30to60;
        private BigDecimal days60to90;
        private Integer count60to90;
        private BigDecimal days90plus;
        private Integer count90plus;
    }
}
