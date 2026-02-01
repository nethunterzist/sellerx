package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.billing.dto.SubscriptionPlanDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Billing controller for plans and general billing operations.
 * Plans endpoint is public for pricing page display.
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPriceRepository priceRepository;
    private final PlanFeatureRepository featureRepository;

    /**
     * Get all active subscription plans with prices
     * Public endpoint for pricing page
     */
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanDto>> getPlans() {
        log.debug("Fetching all active subscription plans");

        List<SubscriptionPlan> plans = planRepository.findByIsActiveTrueOrderBySortOrderAsc();

        List<SubscriptionPlanDto> planDtos = plans.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(planDtos);
    }

    /**
     * Get a specific plan by code
     */
    @GetMapping("/plans/{code}")
    public ResponseEntity<SubscriptionPlanDto> getPlanByCode(@PathVariable String code) {
        log.debug("Fetching plan by code: {}", code);

        return planRepository.findByCodeAndIsActiveTrue(code)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private SubscriptionPlanDto toDto(SubscriptionPlan plan) {
        // Get prices for this plan
        List<SubscriptionPrice> prices = priceRepository.findByPlanIdAndIsActiveTrue(plan.getId());

        // Get features for this plan
        List<PlanFeature> features = featureRepository.findByPlanIdAndIsEnabledTrue(plan.getId());

        // Extract maxStores from features
        Integer maxStores = features.stream()
                .filter(f -> "max_stores".equals(f.getFeatureCode()))
                .findFirst()
                .map(f -> f.getFeatureType() == FeatureType.UNLIMITED ? null : f.getLimitValue())
                .orElse(1);

        // Build prices array for frontend compatibility
        List<SubscriptionPlanDto.PriceDto> priceDtos = prices.stream()
                .map(p -> SubscriptionPlanDto.PriceDto.builder()
                        .id(p.getId())
                        .billingCycle(p.getBillingCycle().name())
                        .billingCycleDisplay(getBillingCycleDisplay(p.getBillingCycle()))
                        .price(p.getPriceAmount())
                        .discountPercentage(p.getDiscountPercentage())
                        .monthlyEquivalent(calculateMonthlyEquivalent(p))
                        .currency(p.getCurrency())
                        .build())
                .collect(Collectors.toList());

        // Build features map for frontend compatibility
        java.util.Map<String, Object> featuresMap = new java.util.HashMap<>();
        for (PlanFeature f : features) {
            if (f.getFeatureType() == FeatureType.BOOLEAN) {
                featuresMap.put(f.getFeatureCode(), true);
            } else if (f.getFeatureType() == FeatureType.UNLIMITED) {
                featuresMap.put(f.getFeatureCode(), -1); // -1 represents unlimited
            } else if (f.getFeatureType() == FeatureType.LIMIT && f.getLimitValue() != null) {
                featuresMap.put(f.getFeatureCode(), f.getLimitValue());
            }
        }

        return SubscriptionPlanDto.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .monthlyPrice(prices.stream()
                        .filter(p -> p.getBillingCycle() == BillingCycle.MONTHLY)
                        .findFirst()
                        .map(SubscriptionPrice::getPriceAmount)
                        .orElse(null))
                .yearlyPrice(prices.stream()
                        .filter(p -> p.getBillingCycle() == BillingCycle.YEARLY)
                        .findFirst()
                        .map(SubscriptionPrice::getPriceAmount)
                        .orElse(null))
                .currency(prices.isEmpty() ? "TRY" : prices.get(0).getCurrency())
                .maxStores(maxStores)
                .featuresMap(featuresMap)
                .features(features.stream()
                        .map(f -> SubscriptionPlanDto.FeatureDto.builder()
                                .code(f.getFeatureCode())
                                .name(f.getFeatureName())
                                .type(f.getFeatureType().name())
                                .limitValue(f.getLimitValue())
                                .build())
                        .collect(Collectors.toList()))
                .prices(priceDtos)
                .isFree("FREE".equals(plan.getCode()))
                .hasUnlimitedStores(maxStores == null)
                .isPopular("PRO".equals(plan.getCode()))
                .sortOrder(plan.getSortOrder())
                .build();
    }

    private String getBillingCycleDisplay(BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> "Aylık";
            case QUARTERLY -> "3 Aylık";
            case SEMIANNUAL -> "6 Aylık";
            case YEARLY -> "Yıllık";
        };
    }

    private java.math.BigDecimal calculateMonthlyEquivalent(SubscriptionPrice price) {
        if (price.getPriceAmount() == null) return null;
        int months = switch (price.getBillingCycle()) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case SEMIANNUAL -> 6;
            case YEARLY -> 12;
        };
        return price.getPriceAmount().divide(java.math.BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP);
    }
}
