package com.ecommerce.sellerx.ai.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerTemplateDto {
    private UUID id;
    private UUID storeId;
    private String name;
    private String templateText;
    private String category;
    private List<String> variables;
    private Integer usageCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
