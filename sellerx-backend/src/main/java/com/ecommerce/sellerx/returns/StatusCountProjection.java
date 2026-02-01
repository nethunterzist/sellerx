package com.ecommerce.sellerx.returns;

/**
 * Projection for status distribution counts.
 */
public interface StatusCountProjection {
    String getStatus();
    Long getStatusCount();
}
