package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemDto {
    private Long id;
    private UUID productId;
    private String productName;
    private String productBarcode;
    private String productImage;
    private Integer unitsOrdered;
    private Integer unitsPerBox;
    private Integer boxesOrdered;
    private String boxDimensions;
    private BigDecimal manufacturingCostPerUnit;
    private BigDecimal transportationCostPerUnit;
    private Integer costVatRate;
    private BigDecimal totalCostPerUnit;
    private BigDecimal totalCost;
    private String hsCode;
    private BigDecimal manufacturingCostSupplierCurrency;
    private String labels;
    private LocalDate stockEntryDate;
    private String comment;
}
