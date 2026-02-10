package com.ecommerce.sellerx.financial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for generic invoice items (CEZA, KOMISYON, etc.).
 * Used for lazy loading in invoice detail panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemsPageDto {
    private List<InvoiceItemDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
