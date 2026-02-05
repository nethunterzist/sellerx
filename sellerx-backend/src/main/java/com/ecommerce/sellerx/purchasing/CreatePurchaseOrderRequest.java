package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseOrderRequest {
    private LocalDate poDate;
    private LocalDate estimatedArrival;
    private LocalDate stockEntryDate;
    private String supplierName;
    private Long supplierId;
    private String supplierCurrency;
    private java.math.BigDecimal exchangeRate;
    private String carrier;
    private String trackingNumber;
    private String comment;
}
