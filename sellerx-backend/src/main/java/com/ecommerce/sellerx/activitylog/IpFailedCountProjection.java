package com.ecommerce.sellerx.activitylog;

/**
 * Projection for IP address with failed login count.
 */
public interface IpFailedCountProjection {
    String getIpAddress();
    Long getFailedCount();
}
