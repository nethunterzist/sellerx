package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStockInfoRequest {
    private Integer quantity;
    private Double unitCost;
    private Integer costVatRate;
}
