package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for commission reconciliation summary statistics.
 */
public interface ReconciliationSummaryProjection {
    Long getLogCount();
    Long getTotalReconciled();
    Long getTotalEstimated();
    Long getTotalReal();
    BigDecimal getTotalDifference();
}
