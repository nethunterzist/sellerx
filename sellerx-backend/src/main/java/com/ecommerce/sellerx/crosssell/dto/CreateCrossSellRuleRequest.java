package com.ecommerce.sellerx.crosssell.dto;

import com.ecommerce.sellerx.crosssell.RecommendationType;
import com.ecommerce.sellerx.crosssell.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCrossSellRuleRequest {

    @NotBlank(message = "Kural adı zorunludur")
    @Size(max = 200)
    private String name;

    @NotNull(message = "Tetikleme türü zorunludur")
    private TriggerType triggerType;

    private Map<String, Object> triggerConditions;

    @NotNull(message = "Öneri türü zorunludur")
    private RecommendationType recommendationType;

    private String recommendationText;

    private Integer priority;
    private Integer maxProducts;

    private List<RuleProductRequest> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleProductRequest {
        @NotBlank
        private String productBarcode;
        private Integer displayOrder;
    }
}
