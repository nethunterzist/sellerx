package com.ecommerce.sellerx.returns;

import java.math.BigDecimal;

/**
 * Projection for top returned products ranked by return count.
 */
public interface TopReturnedProductProjection {
    String getBarcode();
    String getProductName();
    Long getReturnCount();
    BigDecimal getTotalLoss();
}
