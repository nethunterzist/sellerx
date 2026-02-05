package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderSummaryDto {
    private Long id;
    private String poNumber;
    private LocalDate poDate;
    private LocalDate estimatedArrival;
    private LocalDate stockEntryDate;
    private PurchaseOrderStatus status;
    private String supplierName;
    private Long supplierId;
    private String supplierCurrency;
    private Long parentPoId;
    private BigDecimal totalCost;
    private Integer totalUnits;
    private Integer itemCount;
}
