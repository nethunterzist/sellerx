package com.ecommerce.sellerx.ai.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSettingsDto {
    private UUID id;
    private UUID storeId;
    private Boolean aiEnabled;
    private Boolean autoAnswer;
    private String tone;
    private String language;
    private Integer maxAnswerLength;
    private Boolean includeGreeting;
    private Boolean includeSignature;
    private String signatureText;
    private BigDecimal confidenceThreshold;
}
