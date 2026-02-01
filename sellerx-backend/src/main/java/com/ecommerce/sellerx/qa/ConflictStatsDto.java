package com.ecommerce.sellerx.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictStatsDto {
    private long totalActive;
    private long criticalCount;
    private long highCount;
    private long mediumCount;
    private long lowCount;
    private long legalRiskCount;
    private long healthSafetyCount;
    private long knowledgeConflictCount;
    private long brandInconsistencyCount;
}
