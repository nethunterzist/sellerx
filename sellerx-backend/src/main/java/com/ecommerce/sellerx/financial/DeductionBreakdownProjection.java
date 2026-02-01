package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for deduction breakdown by transaction type.
 */
public interface DeductionBreakdownProjection {
    String getTransactionType();
    BigDecimal getTotalDebt();
}
