package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPurchaseOrderItemRequest {
    private UUID productId;
    private Integer unitsOrdered;
    private Integer unitsPerBox;
    private Integer boxesOrdered;
    private String boxDimensions;
    private BigDecimal manufacturingCostPerUnit;
    private BigDecimal transportationCostPerUnit;
    private Integer costVatRate; // Optional: defaults to product's VAT rate or 20%
    private String hsCode;
    private BigDecimal manufacturingCostSupplierCurrency;
    private String labels;
    private LocalDate stockEntryDate; // Optional: overrides PO-level stockEntryDate for this item
    private String comment;
}
