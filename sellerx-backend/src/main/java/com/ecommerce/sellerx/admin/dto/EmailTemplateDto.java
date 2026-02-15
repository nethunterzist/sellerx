package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateDto {

    private UUID id;
    private String emailType;
    private String name;
    private String subjectTemplate;
    private String bodyTemplate;
    private String description;
    private Boolean isActive;
    private List<String> availableVariables;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
