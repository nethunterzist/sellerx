package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for deduction breakdown by transaction type.
 * Used for dashboard detail panel to show all invoice types individually.
 */
public interface DeductionBreakdownProjection {
    String getTransactionType();
    BigDecimal getTotalDebt();
    BigDecimal getTotalCredit();
}
