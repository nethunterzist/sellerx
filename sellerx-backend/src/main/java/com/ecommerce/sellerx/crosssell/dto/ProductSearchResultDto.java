package com.ecommerce.sellerx.crosssell.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for product search results in cross-sell rule builder.
 * Returns simplified product info for dropdown selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResultDto {
    private String barcode;
    private String title;
    private String image;
    private BigDecimal salePrice;
    private Boolean onSale;
    private Integer trendyolQuantity;
}
