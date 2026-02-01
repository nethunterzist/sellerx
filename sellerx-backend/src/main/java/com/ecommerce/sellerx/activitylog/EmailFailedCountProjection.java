package com.ecommerce.sellerx.activitylog;

/**
 * Projection for email with failed login count.
 */
public interface EmailFailedCountProjection {
    String getEmail();
    Long getFailedCount();
}
