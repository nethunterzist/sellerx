package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for deduction invoice aggregation by barcode.
 */
public interface DeductionByBarcodeProjection {
    String getBarcode();
    Long getTotalQuantity();
    BigDecimal getTotalAmount();
    BigDecimal getTotalVatAmount();
    Long getInvoiceCount();
}
