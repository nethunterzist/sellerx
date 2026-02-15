package com.ecommerce.sellerx.financial;

import java.math.BigDecimal;

/**
 * Projection for cargo invoice summary by barcode.
 * Used for aggregate totals while paginating cargo invoice details.
 */
public interface CargoSummaryProjection {
    BigDecimal getTotalAmount();
    BigDecimal getTotalVatAmount();
    BigDecimal getTotalDesi();
    Long getShipmentCount();
    Long getOrderCount();
}
