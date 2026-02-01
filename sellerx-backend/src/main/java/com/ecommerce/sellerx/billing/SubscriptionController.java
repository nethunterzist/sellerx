package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.billing.dto.*;
import com.ecommerce.sellerx.billing.service.SubscriptionService;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Subscription management controller.
 * Handles subscription creation, updates, cancellation, and status.
 */
@RestController
@RequestMapping("/api/billing/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
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
     * Get current user's subscription
     */
    @GetMapping
    public ResponseEntity<SubscriptionDto> getCurrentSubscription() {
        Long userId = getCurrentUserId();

        return subscriptionService.getCurrentSubscription(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Start a trial subscription
     */
    @PostMapping("/trial")
    public ResponseEntity<SubscriptionDto> startTrial(
            @RequestBody CreateSubscriptionRequest request) {

        Long userId = getCurrentUserId();

        log.info("Starting trial for user: {}, plan: {}", userId, request.getPlanCode());

        Subscription subscription = subscriptionService.createSubscription(
                userId,
                request.getPlanCode(),
                request.getBillingCycle()
        );

        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(userId)
                .orElseThrow(() -> new IllegalStateException("Subscription not found after creation")));
    }

    /**
     * Activate subscription after payment
     */
    @PostMapping("/activate")
    public ResponseEntity<SubscriptionDto> activateSubscription() {
        Long userId = getCurrentUserId();

        return subscriptionService.getCurrentSubscription(userId)
                .map(sub -> {
                    subscriptionService.activateSubscription(sub.getId());
                    return subscriptionService.getCurrentSubscription(userId)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Change subscription plan (upgrade)
     */
    @PostMapping("/plan")
    public ResponseEntity<SubscriptionDto> changePlan(
            @RequestBody ChangePlanRequest request) {

        Long userId = getCurrentUserId();

        log.info("Changing plan for user: {}, new plan: {}", userId, request.getPlanCode());

        return subscriptionService.getCurrentSubscription(userId)
                .map(sub -> {
                    subscriptionService.upgradePlan(sub.getId(), request.getPlanCode(), request.getBillingCycle());
                    return subscriptionService.getCurrentSubscription(userId)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel subscription
     */
    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionDto> cancelSubscription(
            @RequestBody(required = false) CancelSubscriptionRequest request) {

        Long userId = getCurrentUserId();

        log.info("Cancelling subscription for user: {}", userId);

        String reason = request != null ? request.getReason() : null;

        return subscriptionService.getCurrentSubscription(userId)
                .map(sub -> {
                    subscriptionService.cancelSubscription(sub.getId(), reason);
                    return subscriptionService.getCurrentSubscription(userId)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.noContent().build());
                })
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Reactivate cancelled subscription
     */
    @PostMapping("/reactivate")
    public ResponseEntity<SubscriptionDto> reactivateSubscription() {
        Long userId = getCurrentUserId();

        log.info("Reactivating subscription for user: {}", userId);

        return subscriptionService.getCurrentSubscription(userId)
                .map(sub -> {
                    subscriptionService.reactivateSubscription(sub.getId());
                    return subscriptionService.getCurrentSubscription(userId)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
