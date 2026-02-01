package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Feature access controller.
 * Handles feature availability and usage tracking.
 */
@RestController
@RequestMapping("/api/billing/features")
@RequiredArgsConstructor
@Slf4j
public class FeatureController {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanFeatureRepository featureRepository;
    private final FeatureUsageRepository usageRepository;
    private final UserService userService;

    /**
     * Get current authenticated user ID from SecurityContext
     */
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }

    /**
     * Get current authenticated user from SecurityContext
     */
    private User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userService.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    /**
     * Get all features available to current user based on their subscription
     */
    @GetMapping
    public ResponseEntity<List<FeatureAccessDto>> getFeatures() {
        Long userId = getCurrentUserId();

        Optional<Subscription> subscription = subscriptionRepository.findByUserIdWithPlanAndPrice(userId);

        if (subscription.isEmpty()) {
            // No subscription - return free tier features
            return ResponseEntity.ok(getFreeTierFeatures());
        }

        Subscription sub = subscription.get();
        List<PlanFeature> features = featureRepository.findByPlanIdAndIsEnabledTrue(sub.getPlan().getId());

        List<FeatureAccessDto> featureDtos = features.stream()
                .map(f -> toFeatureAccessDto(f, userId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(featureDtos);
    }

    /**
     * Check access to a specific feature
     */
    @GetMapping("/{code}")
    public ResponseEntity<FeatureAccessDto> checkFeature(@PathVariable String code) {
        Long userId = getCurrentUserId();

        Optional<Subscription> subscription = subscriptionRepository.findByUserIdWithPlanAndPrice(userId);

        if (subscription.isEmpty()) {
            return ResponseEntity.ok(getFreeTierFeatureAccess(code, userId));
        }

        Subscription sub = subscription.get();
        Optional<PlanFeature> feature = featureRepository.findByPlanIdAndFeatureCode(sub.getPlan().getId(), code);

        if (feature.isEmpty()) {
            return ResponseEntity.ok(new FeatureAccessDto(code, null, false, null, null, null));
        }

        return ResponseEntity.ok(toFeatureAccessDto(feature.get(), userId));
    }

    /**
     * Record usage of a feature (for limit-based features)
     */
    @PostMapping("/{code}/use")
    public ResponseEntity<FeatureUsageResponse> useFeature(@PathVariable String code) {
        User user = getCurrentUser();

        log.debug("Recording usage for feature: {} by user: {}", code, user.getId());

        // Get current period usage
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime periodEnd = periodStart.plusMonths(1);

        FeatureUsage usage = usageRepository.findByUserIdAndFeatureCodeAndPeriodStart(
                user.getId(), code, periodStart
        ).orElseGet(() -> FeatureUsage.builder()
                .user(user)
                .featureCode(code)
                .usageCount(0)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .build());

        usage.setUsageCount(usage.getUsageCount() + 1);
        usage = usageRepository.save(usage);

        // Check if limit exceeded
        Optional<Subscription> subscription = subscriptionRepository.findByUserIdWithPlanAndPrice(user.getId());
        Integer limit = null;
        boolean limitExceeded = false;

        if (subscription.isPresent()) {
            Optional<PlanFeature> feature = featureRepository.findByPlanIdAndFeatureCode(
                    subscription.get().getPlan().getId(), code
            );
            if (feature.isPresent() && feature.get().getFeatureType() == FeatureType.LIMIT) {
                limit = feature.get().getLimitValue();
                limitExceeded = limit != null && usage.getUsageCount() > limit;
            }
        }

        return ResponseEntity.ok(new FeatureUsageResponse(
                code,
                usage.getUsageCount(),
                limit,
                limitExceeded
        ));
    }

    private FeatureAccessDto toFeatureAccessDto(PlanFeature feature, Long userId) {
        Integer currentUsage = null;
        Integer remaining = null;

        if (feature.getFeatureType() == FeatureType.LIMIT) {
            LocalDateTime periodStart = LocalDateTime.now()
                    .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

            currentUsage = usageRepository.findByUserIdAndFeatureCodeAndPeriodStart(
                    userId, feature.getFeatureCode(), periodStart
            ).map(FeatureUsage::getUsageCount).orElse(0);

            if (feature.getLimitValue() != null) {
                remaining = Math.max(0, feature.getLimitValue() - currentUsage);
            }
        }

        boolean hasAccess = feature.getIsEnabled() && (
                feature.getFeatureType() == FeatureType.BOOLEAN ||
                feature.getFeatureType() == FeatureType.UNLIMITED ||
                (feature.getFeatureType() == FeatureType.LIMIT && remaining != null && remaining > 0)
        );

        return new FeatureAccessDto(
                feature.getFeatureCode(),
                feature.getFeatureName(),
                hasAccess,
                feature.getLimitValue(),
                currentUsage,
                remaining
        );
    }

    private List<FeatureAccessDto> getFreeTierFeatures() {
        // Return default free tier features
        return List.of(
                new FeatureAccessDto("max_stores", "Maksimum Mağaza", true, 1, 0, 1),
                new FeatureAccessDto("ai_qa_responses", "AI QA Yanıt", true, 10, 0, 10),
                new FeatureAccessDto("advanced_analytics", "Gelişmiş Analitik", false, null, null, null),
                new FeatureAccessDto("webhook_support", "Webhook Desteği", false, null, null, null),
                new FeatureAccessDto("api_access", "API Erişimi", false, null, null, null)
        );
    }

    private FeatureAccessDto getFreeTierFeatureAccess(String code, Long userId) {
        return switch (code) {
            case "max_stores" -> new FeatureAccessDto(code, "Maksimum Mağaza", true, 1, 0, 1);
            case "ai_qa_responses" -> new FeatureAccessDto(code, "AI QA Yanıt", true, 10, 0, 10);
            default -> new FeatureAccessDto(code, null, false, null, null, null);
        };
    }

    /**
     * Feature access DTO
     */
    public record FeatureAccessDto(
            String code,
            String name,
            boolean hasAccess,
            Integer limit,
            Integer currentUsage,
            Integer remaining
    ) {}

    /**
     * Feature usage response
     */
    public record FeatureUsageResponse(
            String code,
            int currentUsage,
            Integer limit,
            boolean limitExceeded
    ) {}
}
