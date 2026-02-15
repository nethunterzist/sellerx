package com.ecommerce.sellerx.ai.dto;

import com.ecommerce.sellerx.crosssell.dto.CrossSellRecommendationDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateResponse {
    private UUID logId;
    private String generatedAnswer;
    private BigDecimal confidenceScore;
    private List<ContextSource> contextSources;
    private List<CrossSellRecommendationDto> crossSellRecommendations;
    private String modelVersion;
    private Integer tokensUsed;
    private Integer generationTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextSource {
        private String type;  // "product", "historical_qa", "knowledge_base", "template"
        private String title;
        private String snippet;
    }
}
