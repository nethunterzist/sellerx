package com.ecommerce.sellerx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimsStatsDto {
    private long totalClaims;
    private long pendingClaims;
    private long acceptedClaims;
    private long rejectedClaims;
    private long unresolvedClaims;
    private long waitingFraudCheckClaims; // Trendyol API: WaitingFraudCheck status (14.03.2025)
}
