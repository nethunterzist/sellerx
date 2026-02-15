package com.ecommerce.sellerx.orders.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Filter criteria for customer list endpoint.
 */
@Data
@Builder
public class CustomerListFilter {
    private Integer minOrderCount;
    private Integer maxOrderCount;
    private Integer minItemCount;
    private Integer maxItemCount;
    private BigDecimal minTotalSpend;
    private BigDecimal maxTotalSpend;
    private BigDecimal minAvgOrderValue;
    private BigDecimal maxAvgOrderValue;
    private Double minRepeatInterval;
    private Double maxRepeatInterval;

    public boolean hasAnyFilter() {
        return minOrderCount != null || maxOrderCount != null ||
               minItemCount != null || maxItemCount != null ||
               minTotalSpend != null || maxTotalSpend != null ||
               minAvgOrderValue != null || maxAvgOrderValue != null ||
               minRepeatInterval != null || maxRepeatInterval != null;
    }
}
