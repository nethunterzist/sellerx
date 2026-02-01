package com.ecommerce.sellerx.webhook;

/**
 * Projection for processing status count grouping.
 */
public interface StatusCountProjection {
    String getProcessingStatus();
    Long getStatusCount();
}
