package com.ecommerce.sellerx.crosssell.dto;

import com.ecommerce.sellerx.crosssell.RecommendationType;
import com.ecommerce.sellerx.crosssell.TriggerType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellRuleDto {
    private UUID id;
    private UUID storeId;
    private String name;
    private TriggerType triggerType;
    private Map<String, Object> triggerConditions;
    private RecommendationType recommendationType;
    private String recommendationText;
    private Integer priority;
    private Integer maxProducts;
    private Boolean active;
    private List<CrossSellRuleProductDto> products;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
