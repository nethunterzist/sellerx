package com.ecommerce.sellerx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimItemAuditDto {
    private String claimId;
    private String claimItemId;
    private String previousStatus;
    private String newStatus;
    private String executorId;
    private String executorApp;
    private String executorUser;
    private LocalDateTime date;
}
