package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for latest shipping cost share by barcode.
 */
public interface BarcodeShippingShareProjection {
    String getBarcode();
    BigDecimal getShippingShare();
}
