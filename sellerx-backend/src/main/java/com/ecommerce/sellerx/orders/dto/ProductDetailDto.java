package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for product detail panel.
 * Extends ProductRepeatData with list of buyers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDto {
    // From ProductRepeatData
    private String barcode;
    private String productName;
    private int totalBuyers;
    private int repeatBuyers;
    private double repeatRate;
    private double avgDaysBetweenRepurchase;
    private int totalQuantitySold;
    private String image;
    private String productUrl;

    // New: list of customers who bought this product
    private List<ProductBuyerDto> buyers;
}
