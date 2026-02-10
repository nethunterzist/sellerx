package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for Stoppage (Withholding Tax) summary.
 * Provides aggregate data for stopaj/tevkifat records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoppageSummaryDto {

    private String storeId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private int count;
    private BigDecimal totalAmount;

    /**
     * Paginated stoppage items (optional, for combined endpoint).
     */
    private List<StoppageDto> items;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
