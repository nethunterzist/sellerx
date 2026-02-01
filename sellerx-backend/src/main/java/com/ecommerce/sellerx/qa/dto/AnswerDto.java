package com.ecommerce.sellerx.qa.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDto {
    private UUID id;
    private UUID questionId;
    private String answerText;
    private Boolean isSubmitted;
    private String trendyolAnswerId;
    private LocalDateTime submittedAt;
    private Long submittedBy;
    private String submittedByEmail;
    private LocalDateTime createdAt;
}
