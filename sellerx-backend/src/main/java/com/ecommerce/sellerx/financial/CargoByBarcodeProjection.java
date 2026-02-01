package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for cargo invoice aggregation by barcode.
 */
public interface CargoByBarcodeProjection {
    String getBarcode();
    String getProductName();
    Long getTotalQuantity();
    BigDecimal getTotalAmount();
    BigDecimal getTotalVatAmount();
    Long getTotalDesi();
    Long getInvoiceCount();
}
