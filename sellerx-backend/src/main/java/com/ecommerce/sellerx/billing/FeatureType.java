package com.ecommerce.sellerx.billing;

/**
 * Feature type for plan-based feature gating
 */
public enum FeatureType {
    /** Boolean feature (on/off) */
    BOOLEAN,

    /** Limited feature with numeric cap */
    LIMIT,

    /** Unlimited feature (no cap) */
    UNLIMITED
}
