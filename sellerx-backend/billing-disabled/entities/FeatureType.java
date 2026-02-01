package com.ecommerce.sellerx.billing;

/**
 * Feature types for plan configuration
 */
public enum FeatureType {
    /** Boolean feature - enabled or disabled */
    BOOLEAN,

    /** Limited feature - has a usage limit per period */
    LIMIT,

    /** Unlimited feature - no usage restrictions */
    UNLIMITED
}
