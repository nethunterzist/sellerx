package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSecuritySummaryDto {
    private long failedLogins24h;
    private long failedLogins7d;
    private List<SuspiciousIpDto> suspiciousIps;
    private List<SuspiciousAccountDto> suspiciousAccounts;
    private long totalLoginsToday;
}
