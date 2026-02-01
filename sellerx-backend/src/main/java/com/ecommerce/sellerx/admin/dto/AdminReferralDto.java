package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReferralDto {
    private UUID id;
    private String referrerEmail;
    private String referredEmail;
    private String code;
    private String status;
    private Integer rewardDays;
    private LocalDateTime createdAt;
}
