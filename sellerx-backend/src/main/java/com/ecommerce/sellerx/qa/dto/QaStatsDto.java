package com.ecommerce.sellerx.qa.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaStatsDto {
    private long totalQuestions;
    private long pendingQuestions;
    private long answeredQuestions;
}
