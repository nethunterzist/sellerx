package com.ecommerce.sellerx.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Subscription plan DTO for API responses
 */
@Data
@Builder
public class SubscriptionPlanDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private String currency;
    private Integer maxStores;
    private Map<String, Object> featuresMap;
    private List<FeatureDto> features;
    private Integer sortOrder;
    private List<PriceDto> prices;
    private boolean isFree;
    private boolean hasUnlimitedStores;
    private boolean isPopular;

    @Data
    @Builder
    public static class PriceDto {
        private UUID id;
        private String billingCycle;
        private String billingCycleDisplay;
        private BigDecimal price;
        private BigDecimal discountPercentage;
        private BigDecimal monthlyEquivalent;
        private String currency;
    }

    @Data
    @Builder
    public static class FeatureDto {
        private String code;
        private String name;
        private String type;
        private Integer limitValue;
    }
}
