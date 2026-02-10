package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for a buyer of a specific product.
 * Used in product detail panel to show which customers purchased a product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBuyerDto {
    private Long customerId;
    private String customerName;
    private String city;
    private int purchaseCount;  // How many times they purchased this product
    private BigDecimal totalSpend;  // Total spend on this product
}
