package com.ecommerce.sellerx.returns;

import java.math.BigDecimal;

/**
 * Projection for return cost breakdown totals.
 */
public interface ReturnCostBreakdownProjection {
    BigDecimal getTotalProductCost();
    BigDecimal getTotalShippingCostOut();
    BigDecimal getTotalShippingCostReturn();
    BigDecimal getTotalCommissionLoss();
    BigDecimal getTotalPackagingCost();
    BigDecimal getTotalLoss();
}
