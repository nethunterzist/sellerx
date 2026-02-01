package com.ecommerce.sellerx.stocktracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAlertSettingsRequest {
    private Boolean alertOnOutOfStock;
    private Boolean alertOnLowStock;
    private Integer lowStockThreshold;
    private Boolean alertOnStockIncrease;
    private Boolean alertOnBackInStock;
    private Boolean isActive;
}
