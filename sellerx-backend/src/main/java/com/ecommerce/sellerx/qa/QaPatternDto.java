package com.ecommerce.sellerx.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaPatternDto {
    private UUID id;
    private UUID storeId;
    private String patternHash;
    private String canonicalQuestion;
    private String canonicalAnswer;
    private Integer occurrenceCount;
    private Integer approvalCount;
    private Integer rejectionCount;
    private Integer modificationCount;
    private BigDecimal confidenceScore;
    private String seniorityLevel;
    private Boolean isAutoSubmitEligible;
    private LocalDateTime autoSubmitEnabledAt;
    private String autoSubmitDisabledReason;
    private String productId;
    private String category;
    private LocalDateTime lastHumanReview;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;

    // Computed fields
    private Integer totalReviews;
    private Double approvalRate;

    public static QaPatternDto fromEntity(QaPattern entity) {
        return QaPatternDto.builder()
                .id(entity.getId())
                .storeId(entity.getStore().getId())
                .patternHash(entity.getPatternHash())
                .canonicalQuestion(entity.getCanonicalQuestion())
                .canonicalAnswer(entity.getCanonicalAnswer())
                .occurrenceCount(entity.getOccurrenceCount())
                .approvalCount(entity.getApprovalCount())
                .rejectionCount(entity.getRejectionCount())
                .modificationCount(entity.getModificationCount())
                .confidenceScore(entity.getConfidenceScore())
                .seniorityLevel(entity.getSeniorityLevel())
                .isAutoSubmitEligible(entity.getIsAutoSubmitEligible())
                .autoSubmitEnabledAt(entity.getAutoSubmitEnabledAt())
                .autoSubmitDisabledReason(entity.getAutoSubmitDisabledReason())
                .productId(entity.getProductId())
                .category(entity.getCategory())
                .lastHumanReview(entity.getLastHumanReview())
                .firstSeenAt(entity.getFirstSeenAt())
                .lastSeenAt(entity.getLastSeenAt())
                .totalReviews(entity.getTotalReviews())
                .approvalRate(entity.getApprovalRate())
                .build();
    }
}
