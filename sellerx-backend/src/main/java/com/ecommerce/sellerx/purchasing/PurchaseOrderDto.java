package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDto {
    private Long id;
    private String poNumber;
    private LocalDate poDate;
    private LocalDate estimatedArrival;
    private LocalDate stockEntryDate;
    private PurchaseOrderStatus status;
    private String supplierName;
    private Long supplierId;
    private String supplierCurrency;
    private BigDecimal exchangeRate;
    private Long parentPoId;
    private String carrier;
    private String trackingNumber;
    private String comment;
    private BigDecimal transportationCost;
    private BigDecimal totalCost;
    private Integer totalUnits;
    private List<PurchaseOrderItemDto> items;
    private Integer itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
