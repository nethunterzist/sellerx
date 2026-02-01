package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderStatsDto {
    private StatusStats draft;
    private StatusStats ordered;
    private StatusStats shipped;
    private StatusStats closed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusStats {
        private Long count;
        private BigDecimal totalCost;
        private Long totalUnits;
    }
}
