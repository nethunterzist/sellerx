package com.ecommerce.sellerx.referral;

/**
 * Projection for top referrers query.
 */
public interface TopReferrerProjection {
    Long getUserId();
    String getEmail();
    Long getReferralCount();
    Long getTotalRewardDays();
}
