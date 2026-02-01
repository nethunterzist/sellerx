package com.ecommerce.sellerx.ai.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDto {
    private UUID id;
    private UUID storeId;
    private String category;
    private String title;
    private String content;
    private List<String> keywords;
    private Boolean isActive;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
