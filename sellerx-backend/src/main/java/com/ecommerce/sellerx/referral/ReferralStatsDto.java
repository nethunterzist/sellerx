package com.ecommerce.sellerx.referral;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReferralStatsDto {
    private String referralCode;
    private String referralLink;
    private int totalReferrals;
    private int completedReferrals;
    private int pendingReferrals;
    private int totalDaysEarned;
    private int maxBonusDaysRemaining;
    private boolean canRefer;
    private List<ReferralDto> referrals;
}
