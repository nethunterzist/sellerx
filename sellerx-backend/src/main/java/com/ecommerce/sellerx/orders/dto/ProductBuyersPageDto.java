package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for product buyers list.
 * Used for lazy loading in product detail panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBuyersPageDto {
    private List<ProductBuyerDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
