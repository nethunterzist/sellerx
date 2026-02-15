package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailPreviewRequest {

    private String subjectTemplate;
    private String bodyTemplate;
    private Map<String, Object> sampleVariables;
}
