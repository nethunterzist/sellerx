package com.ecommerce.sellerx.qa.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    private UUID id;
    private String questionId;
    private String productId;
    private String barcode;
    private String productTitle;
    private String customerQuestion;
    private LocalDateTime questionDate;
    private String status;
    private Boolean isPublic;
    private List<AnswerDto> answers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
