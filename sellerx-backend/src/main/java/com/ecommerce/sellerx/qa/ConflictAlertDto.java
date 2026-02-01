package com.ecommerce.sellerx.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictAlertDto {
    private UUID id;
    private UUID storeId;
    private UUID questionId;
    private String customerQuestion; // Question text for context
    private String conflictType;
    private String severity;
    private String sourceAType;
    private String sourceAContent;
    private String sourceBType;
    private String sourceBContent;
    private List<String> detectedKeywords;
    private String status;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private String resolvedByEmail;
    private LocalDateTime createdAt;

    public static ConflictAlertDto fromEntity(ConflictAlert entity) {
        ConflictAlertDtoBuilder builder = ConflictAlertDto.builder()
                .id(entity.getId())
                .storeId(entity.getStore().getId())
                .conflictType(entity.getConflictType())
                .severity(entity.getSeverity())
                .sourceAType(entity.getSourceAType())
                .sourceAContent(entity.getSourceAContent())
                .sourceBType(entity.getSourceBType())
                .sourceBContent(entity.getSourceBContent())
                .detectedKeywords(entity.getDetectedKeywords())
                .status(entity.getStatus())
                .resolutionNotes(entity.getResolutionNotes())
                .resolvedAt(entity.getResolvedAt())
                .createdAt(entity.getCreatedAt());

        if (entity.getQuestion() != null) {
            builder.questionId(entity.getQuestion().getId())
                   .customerQuestion(entity.getQuestion().getCustomerQuestion());
        }

        if (entity.getResolvedBy() != null) {
            builder.resolvedBy(entity.getResolvedBy().getId())
                   .resolvedByEmail(entity.getResolvedBy().getEmail());
        }

        return builder.build();
    }
}
