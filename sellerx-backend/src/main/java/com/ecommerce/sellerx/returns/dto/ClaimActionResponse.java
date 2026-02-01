package com.ecommerce.sellerx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimActionResponse {
    private boolean success;
    private String message;
    private String claimId;
    private String newStatus;
}
