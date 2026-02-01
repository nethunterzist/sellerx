package com.ecommerce.sellerx.qa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    @NotBlank(message = "Answer text is required")
    private String answerText;
}
