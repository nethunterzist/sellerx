package com.ecommerce.sellerx.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSuggestionDto {
    private UUID id;
    private UUID storeId;
    private String suggestedTitle;
    private String suggestedContent;
    private List<String> sampleQuestions;
    private Integer questionCount;
    private BigDecimal avgSimilarity;
    private String status;
    private String priority;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private String reviewedByEmail;
    private String reviewNotes;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;

    public static KnowledgeSuggestionDto fromEntity(KnowledgeSuggestion entity) {
        return KnowledgeSuggestionDto.builder()
                .id(entity.getId())
                .storeId(entity.getStore().getId())
                .suggestedTitle(entity.getSuggestedTitle())
                .suggestedContent(entity.getSuggestedContent())
                .sampleQuestions(entity.getSampleQuestions())
                .questionCount(entity.getQuestionCount())
                .avgSimilarity(entity.getAvgSimilarity())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .reviewedAt(entity.getReviewedAt())
                .reviewedBy(entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null)
                .reviewedByEmail(entity.getReviewedBy() != null ? entity.getReviewedBy().getEmail() : null)
                .reviewNotes(entity.getReviewNotes())
                .firstSeenAt(entity.getFirstSeenAt())
                .lastSeenAt(entity.getLastSeenAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
