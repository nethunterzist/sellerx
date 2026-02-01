package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for invoice summary grouped by category.
 */
public interface InvoiceSummaryByCategoryProjection {
    String getInvoiceCategory();
    Long getInvoiceCount();
    BigDecimal getTotalAmount();
    BigDecimal getTotalVatAmount();
}
