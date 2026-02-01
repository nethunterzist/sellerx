package com.ecommerce.sellerx.billing;

/**
 * Projection for subscription count by plan code.
 */
public interface PlanCountProjection {
    String getPlanCode();
    Long getSubscriptionCount();
}
