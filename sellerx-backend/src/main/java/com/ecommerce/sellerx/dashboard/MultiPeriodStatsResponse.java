package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing stats for multiple periods (months, weeks, or days)
 * Used for P&L breakdown tables with horizontal scroll
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiPeriodStatsResponse {

    /**
     * List of stats for each period (most recent first)
     */
    private List<PeriodStatsDto> periods;

    /**
     * Aggregated totals across all periods
     */
    private PeriodStatsDto total;

    /**
     * Store ID
     */
    private String storeId;

    /**
     * Period type used for this response (monthly, weekly, daily)
     */
    private String periodType;

    /**
     * Number of periods requested
     */
    private int periodCount;

    /**
     * Calculation timestamp
     */
    private String calculatedAt;
}
