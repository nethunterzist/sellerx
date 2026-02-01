package com.ecommerce.sellerx.qa.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaSyncResponse {
    private int totalFetched;
    private int newQuestions;
    private int updatedQuestions;
    private String message;
}
