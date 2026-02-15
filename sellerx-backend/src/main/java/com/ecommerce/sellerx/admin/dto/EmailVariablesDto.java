package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVariablesDto {

    private Map<String, List<VariableInfo>> variablesByType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableInfo {
        private String name;
        private String description;
        private String sampleValue;
    }
}
