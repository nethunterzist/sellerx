package com.ecommerce.sellerx.admin.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubscriptionDetailDto {
    private Long id;
    private String userEmail;
    private String planName;
    private String status;
    private String billingCycle;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime createdAt;
    private Boolean autoRenew;
    private String cancellationReason;
    private List<AdminPaymentDto> paymentHistory;
}
