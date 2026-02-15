package com.ecommerce.sellerx.crosssell.dto;

import com.ecommerce.sellerx.crosssell.RecommendationType;
import com.ecommerce.sellerx.crosssell.TriggerType;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCrossSellRuleRequest {

    @Size(max = 200)
    private String name;

    private TriggerType triggerType;
    private Map<String, Object> triggerConditions;
    private RecommendationType recommendationType;
    private String recommendationText;
    private Integer priority;
    private Integer maxProducts;
    private Boolean active;

    private List<CreateCrossSellRuleRequest.RuleProductRequest> products;
}
