package com.ecommerce.sellerx.products;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BulkCostUpdateRequest {
    private List<CostUpdateItem> items;

    @Data
    public static class CostUpdateItem {
        private String barcode;
        private BigDecimal unitCost;
        private BigDecimal costVatRate;
        private Integer quantity;
        private LocalDate stockDate;
    }
}
