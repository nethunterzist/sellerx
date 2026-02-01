package com.ecommerce.sellerx.returns;

/**
 * Projection for return reason distribution.
 */
public interface ReturnReasonDistributionProjection {
    String getReturnReason();
    Long getReasonCount();
}
