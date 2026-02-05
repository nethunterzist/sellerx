package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePurchaseOrderRequest {
    private LocalDate poDate;
    private LocalDate estimatedArrival;
    private LocalDate stockEntryDate;
    private String supplierName;
    private Long supplierId;
    private String supplierCurrency;
    private BigDecimal exchangeRate;
    private String carrier;
    private String trackingNumber;
    private String comment;
    private BigDecimal transportationCost;
}
