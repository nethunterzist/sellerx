package com.ecommerce.sellerx.billing;

/**
 * Projection for current feature usage summary.
 */
public interface FeatureUsageSummaryProjection {
    String getFeatureCode();
    Integer getUsageCount();
}
