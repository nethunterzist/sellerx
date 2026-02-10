package com.ecommerce.sellerx.financial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for cargo invoice items.
 * Used for lazy loading in invoice detail panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoInvoiceItemsPageDto {
    private List<CargoInvoiceItemDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
