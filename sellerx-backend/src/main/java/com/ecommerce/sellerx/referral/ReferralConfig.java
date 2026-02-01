package com.ecommerce.sellerx.referral;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "referral")
@Data
public class ReferralConfig {

    /**
     * Days added to referrer's subscription per successful referral
     */
    private int rewardDays = 15;

    /**
     * Trial days for referred users (instead of default 14)
     */
    private int referredTrialDays = 30;

    /**
     * Maximum total bonus days a single user can earn via referrals
     */
    private int maxBonusDays = 180;

    /**
     * Referral code length (alphanumeric uppercase)
     */
    private int codeLength = 8;

    /**
     * Base URL for referral links
     */
    private String baseUrl = "http://localhost:3000";
}
