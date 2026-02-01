package com.ecommerce.sellerx.stocktracking;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTrackedProductRequest {

    @NotBlank(message = "Product URL is required")
    private String productUrl;

    // Optional: Override default alert settings
    private Boolean alertOnOutOfStock;
    private Boolean alertOnLowStock;
    private Integer lowStockThreshold;
    private Boolean alertOnStockIncrease;
    private Boolean alertOnBackInStock;
}
