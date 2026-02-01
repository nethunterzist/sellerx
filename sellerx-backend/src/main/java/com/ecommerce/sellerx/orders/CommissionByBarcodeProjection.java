package com.ecommerce.sellerx.orders;

import java.math.BigDecimal;

/**
 * Projection for aggregated commission data by barcode (from order_items).
 * Used by aggregateCommissionByBarcode native query.
 */
public interface CommissionByBarcodeProjection {
    String getBarcode();
    String getProductName();
    Integer getTotalQuantity();
    BigDecimal getTotalCommission();
    BigDecimal getTotalVatAmount();
    Long getOrderCount();
}
