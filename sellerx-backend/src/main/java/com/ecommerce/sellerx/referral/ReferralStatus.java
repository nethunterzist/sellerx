package com.ecommerce.sellerx.referral;

/**
 * Referral lifecycle status
 */
public enum ReferralStatus {
    /** Referred user registered but hasn't made first payment */
    PENDING,

    /** First payment made, reward granted to referrer */
    COMPLETED,

    /** Referred user never converted (trial expired without payment) */
    EXPIRED
}
