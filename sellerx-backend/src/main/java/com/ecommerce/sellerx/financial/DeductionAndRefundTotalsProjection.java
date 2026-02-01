package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for total deductions and refunds summary.
 */
public interface DeductionAndRefundTotalsProjection {
    BigDecimal getTotalDeductions();
    BigDecimal getTotalRefunds();
    Long getInvoiceCount();
}
