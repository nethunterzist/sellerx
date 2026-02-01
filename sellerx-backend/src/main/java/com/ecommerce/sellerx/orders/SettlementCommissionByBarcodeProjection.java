package com.ecommerce.sellerx.orders;

import java.math.BigDecimal;

/**
 * Projection for aggregated commission data by barcode from financial settlements.
 * Uses actual settlement data instead of estimated commission from order_items.
 */
public interface SettlementCommissionByBarcodeProjection {
    String getBarcode();
    String getProductName();
    Long getOrderCount();
    Long getTransactionCount();
    BigDecimal getNetCommission();
    BigDecimal getSaleCommission();
    BigDecimal getDiscountCommission();
    BigDecimal getCouponCommission();
    BigDecimal getNetVatAmount();
}
