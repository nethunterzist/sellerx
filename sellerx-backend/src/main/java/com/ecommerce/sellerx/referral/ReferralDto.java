package com.ecommerce.sellerx.referral;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReferralDto {
    private UUID id;
    private String referredUserName;
    private String referredUserEmail;
    private ReferralStatus status;
    private int rewardDaysGranted;
    private LocalDateTime createdAt;
    private LocalDateTime rewardAppliedAt;
}
