package com.ecommerce.sellerx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimIssueReasonDto {
    private int id;
    private String name;
    private boolean requiresFile;
}
