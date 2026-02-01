package com.ecommerce.sellerx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimsSyncResponse {
    private int totalFetched;
    private int newClaims;
    private int updatedClaims;
    private String message;
}
