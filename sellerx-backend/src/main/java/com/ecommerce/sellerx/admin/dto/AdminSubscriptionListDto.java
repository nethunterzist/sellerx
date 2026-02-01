package com.ecommerce.sellerx.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubscriptionListDto {
    private Long id;
    private String userEmail;
    private String planName;
    private String status;
    private String billingCycle;
    private LocalDateTime periodEnd;
    private LocalDateTime createdAt;
}
