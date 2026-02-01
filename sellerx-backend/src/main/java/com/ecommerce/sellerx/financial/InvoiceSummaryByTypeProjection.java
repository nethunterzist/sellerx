package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for invoice summary grouped by type.
 */
public interface InvoiceSummaryByTypeProjection {
    String getInvoiceTypeCode();
    String getInvoiceType();
    String getInvoiceCategory();
    Boolean getIsDeduction();
    Long getInvoiceCount();
    BigDecimal getTotalAmount();
    BigDecimal getTotalVatAmount();
}
