package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCostAndStockRequest {
    private Integer quantity;
    private Double unitCost;
    private Integer costVatRate;
    private LocalDate stockDate;
}
