package com.ecommerce.sellerx.billing.service;

import com.ecommerce.sellerx.billing.*;
import com.ecommerce.sellerx.billing.config.SubscriptionConfig;
import com.ecommerce.sellerx.billing.dto.*;
import com.ecommerce.sellerx.referral.ReferralService;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core subscription management service
 *
 * Handles subscription lifecycle:
 * - Creation with trial period
 * - Plan upgrades/downgrades
 * - Cancellation
 * - Status transitions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPriceRepository priceRepository;
    private final SubscriptionEventRepository eventRepository;
    private final SubscriptionConfig config;
    private final UserService userService;
    private final ReferralService referralService;

    /**
     * Get current subscription for user
     */
    @Transactional(readOnly = true)
    public Optional<SubscriptionDto> getCurrentSubscription(Long userId) {
        return subscriptionRepository.findByUserIdWithPlanAndPrice(userId)
                .map(this::toDto);
    }

    /**
     * Create a new subscription with trial period
     */
    @Transactional
    public Subscription createSubscription(Long userId, String planCode, BillingCycle billingCycle) {
        log.info("Creating subscription: userId={}, plan={}, cycle={}", userId, planCode, billingCycle);

        // Check if user already has subscription
        if (subscriptionRepository.existsByUserId(userId)) {
            throw new IllegalStateException("User already has an active subscription");
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        SubscriptionPlan plan = planRepository.findByCodeAndIsActiveTrue(planCode)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planCode));

        SubscriptionPrice price = priceRepository.findByPlanIdAndBillingCycleAndIsActiveTrue(plan.getId(), billingCycle)
                .orElseThrow(() -> new IllegalArgumentException("Price not found for plan: " + planCode + ", cycle: " + billingCycle));

        LocalDateTime now = LocalDateTime.now();
        int trialDays = referralService.getTrialDaysForUser(userId);
        LocalDateTime trialEnd = now.plusDays(trialDays);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .price(price)
                .status(SubscriptionStatus.TRIAL)
                .billingCycle(billingCycle)
                .trialStartDate(now)
                .trialEndDate(trialEnd)
                .currentPeriodStart(now)
                .currentPeriodEnd(trialEnd)
                .autoRenew(true)
                .build();

        subscription = subscriptionRepository.save(subscription);

        // Record event
        recordEvent(subscription, SubscriptionEventType.CREATED, null, SubscriptionStatus.TRIAL, null);
        recordEvent(subscription, SubscriptionEventType.TRIAL_STARTED, null, null, null);

        log.info("Subscription created: id={}, trial ends={}", subscription.getId(), trialEnd);
        return subscription;
    }

    /**
     * Activate subscription after successful payment
     */
    @Transactional
    public Subscription activateSubscription(UUID subscriptionId) {
        log.info("Activating subscription: {}", subscriptionId);

        Subscription subscription = findById(subscriptionId);
        SubscriptionStatus previousStatus = subscription.getStatus();

        if (previousStatus == SubscriptionStatus.ACTIVE) {
            log.warn("Subscription already active: {}", subscriptionId);
            return subscription;
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);

        // Set billing period based on billing cycle
        LocalDateTime now = LocalDateTime.now();
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusMonths(subscription.getBillingCycle().getMonths()));
        subscription.setGracePeriodEnd(null);

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.ACTIVATED, previousStatus, SubscriptionStatus.ACTIVE, null);

        // Process referral reward: grant +days to referrer on first payment
        try {
            referralService.processReferralReward(subscription.getUser().getId());
        } catch (Exception e) {
            log.warn("Referral reward processing failed for user {}: {}",
                    subscription.getUser().getId(), e.getMessage());
        }

        log.info("Subscription activated: {}", subscriptionId);
        return subscription;
    }

    /**
     * Upgrade to a higher plan
     */
    @Transactional
    public Subscription upgradePlan(UUID subscriptionId, String newPlanCode, BillingCycle newCycle) {
        log.info("Upgrading subscription: id={}, newPlan={}", subscriptionId, newPlanCode);

        Subscription subscription = findById(subscriptionId);
        SubscriptionPlan currentPlan = subscription.getPlan();
        SubscriptionPlan newPlan = planRepository.findByCodeAndIsActiveTrue(newPlanCode)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + newPlanCode));

        // Validate upgrade (new plan should have higher sort order)
        if (newPlan.getSortOrder() <= currentPlan.getSortOrder()) {
            throw new IllegalArgumentException("Cannot upgrade to same or lower tier plan");
        }

        SubscriptionPrice newPrice = priceRepository.findByPlanIdAndBillingCycleAndIsActiveTrue(newPlan.getId(), newCycle)
                .orElseThrow(() -> new IllegalArgumentException("Price not found"));

        // Upgrades are immediate
        subscription.setPlan(newPlan);
        subscription.setPrice(newPrice);
        subscription.setBillingCycle(newCycle);

        // Clear any scheduled downgrade
        subscription.setDowngradeToPlan(null);
        subscription.setDowngradeToPrice(null);

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.UPGRADED, null, null, currentPlan, newPlan);

        log.info("Subscription upgraded: {} -> {}", currentPlan.getCode(), newPlan.getCode());
        return subscription;
    }

    /**
     * Schedule downgrade to a lower plan (takes effect at period end)
     */
    @Transactional
    public Subscription schedulePlanDowngrade(UUID subscriptionId, String newPlanCode, BillingCycle newCycle) {
        log.info("Scheduling downgrade: id={}, newPlan={}", subscriptionId, newPlanCode);

        Subscription subscription = findById(subscriptionId);
        SubscriptionPlan currentPlan = subscription.getPlan();
        SubscriptionPlan newPlan = planRepository.findByCodeAndIsActiveTrue(newPlanCode)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + newPlanCode));

        // Validate downgrade
        if (newPlan.getSortOrder() >= currentPlan.getSortOrder()) {
            throw new IllegalArgumentException("Cannot downgrade to same or higher tier plan");
        }

        SubscriptionPrice newPrice = priceRepository.findByPlanIdAndBillingCycleAndIsActiveTrue(newPlan.getId(), newCycle)
                .orElseThrow(() -> new IllegalArgumentException("Price not found"));

        // Schedule for period end
        subscription.setDowngradeToPlan(newPlan);
        subscription.setDowngradeToPrice(newPrice);

        subscription = subscriptionRepository.save(subscription);

        log.info("Downgrade scheduled for period end: {} -> {}", currentPlan.getCode(), newPlan.getCode());
        return subscription;
    }

    /**
     * Apply scheduled downgrade
     */
    @Transactional
    public Subscription applyScheduledDowngrade(UUID subscriptionId) {
        Subscription subscription = findById(subscriptionId);

        if (subscription.getDowngradeToPlan() == null) {
            return subscription;
        }

        SubscriptionPlan currentPlan = subscription.getPlan();
        SubscriptionPlan newPlan = subscription.getDowngradeToPlan();
        SubscriptionPrice newPrice = subscription.getDowngradeToPrice();

        subscription.setPlan(newPlan);
        subscription.setPrice(newPrice);
        subscription.setBillingCycle(newPrice.getBillingCycle());
        subscription.setDowngradeToPlan(null);
        subscription.setDowngradeToPrice(null);

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.DOWNGRADED, null, null, currentPlan, newPlan);

        log.info("Downgrade applied: {} -> {}", currentPlan.getCode(), newPlan.getCode());
        return subscription;
    }

    /**
     * Cancel subscription (takes effect at period end)
     */
    @Transactional
    public Subscription cancelSubscription(UUID subscriptionId, String reason) {
        log.info("Cancelling subscription: {}", subscriptionId);

        Subscription subscription = findById(subscriptionId);

        if (!subscription.getStatus().canCancel()) {
            throw new IllegalStateException("Cannot cancel subscription in status: " + subscription.getStatus());
        }

        subscription.setCancelAtPeriodEnd(true);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setCancellationReason(reason);
        subscription.setAutoRenew(false);

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.CANCELLED, null, null, null);

        log.info("Subscription cancelled: {}, will end at period: {}", subscriptionId, subscription.getCurrentPeriodEnd());
        return subscription;
    }

    /**
     * Reactivate a cancelled subscription (if still in active period)
     */
    @Transactional
    public Subscription reactivateSubscription(UUID subscriptionId) {
        log.info("Reactivating subscription: {}", subscriptionId);

        Subscription subscription = findById(subscriptionId);

        if (!subscription.getCancelAtPeriodEnd()) {
            throw new IllegalStateException("Subscription is not scheduled for cancellation");
        }

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new IllegalStateException("Cannot reactivate expired subscription");
        }

        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancellationReason(null);
        subscription.setAutoRenew(true);

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.REACTIVATED, null, null, null);

        log.info("Subscription reactivated: {}", subscriptionId);
        return subscription;
    }

    /**
     * Mark subscription as past due (payment failed)
     */
    @Transactional
    public Subscription markPastDue(UUID subscriptionId) {
        log.info("Marking subscription past due: {}", subscriptionId);

        Subscription subscription = findById(subscriptionId);
        SubscriptionStatus previousStatus = subscription.getStatus();

        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscription.setGracePeriodEnd(LocalDateTime.now().plusDays(config.getGracePeriodDays()));

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.PAST_DUE, previousStatus, SubscriptionStatus.PAST_DUE, null);

        log.info("Subscription marked past due: {}, grace period ends: {}",
                subscriptionId, subscription.getGracePeriodEnd());
        return subscription;
    }

    /**
     * Suspend subscription (grace period expired)
     */
    @Transactional
    public Subscription suspendSubscription(UUID subscriptionId) {
        log.info("Suspending subscription: {}", subscriptionId);

        Subscription subscription = findById(subscriptionId);
        SubscriptionStatus previousStatus = subscription.getStatus();

        subscription.setStatus(SubscriptionStatus.SUSPENDED);

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.SUSPENDED, previousStatus, SubscriptionStatus.SUSPENDED, null);

        log.info("Subscription suspended: {}", subscriptionId);
        return subscription;
    }

    /**
     * Expire subscription (30 days after suspension)
     */
    @Transactional
    public Subscription expireSubscription(UUID subscriptionId) {
        log.info("Expiring subscription: {}", subscriptionId);

        Subscription subscription = findById(subscriptionId);
        SubscriptionStatus previousStatus = subscription.getStatus();

        subscription.setStatus(SubscriptionStatus.EXPIRED);

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.EXPIRED, previousStatus, SubscriptionStatus.EXPIRED, null);

        log.info("Subscription expired: {}", subscriptionId);
        return subscription;
    }

    /**
     * Renew subscription for next billing period
     */
    @Transactional
    public Subscription renewSubscription(UUID subscriptionId) {
        log.info("Renewing subscription: {}", subscriptionId);

        Subscription subscription = findById(subscriptionId);

        // Apply any scheduled downgrade first
        if (subscription.getDowngradeToPlan() != null) {
            subscription = applyScheduledDowngrade(subscriptionId);
        }

        // Handle cancellation at period end
        if (subscription.getCancelAtPeriodEnd()) {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription = subscriptionRepository.save(subscription);
            recordEvent(subscription, SubscriptionEventType.EXPIRED, SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED, null);
            return subscription;
        }

        // Extend billing period
        LocalDateTime newPeriodStart = subscription.getCurrentPeriodEnd();
        LocalDateTime newPeriodEnd = newPeriodStart.plusMonths(subscription.getBillingCycle().getMonths());

        subscription.setCurrentPeriodStart(newPeriodStart);
        subscription.setCurrentPeriodEnd(newPeriodEnd);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setGracePeriodEnd(null);

        subscription = subscriptionRepository.save(subscription);

        recordEvent(subscription, SubscriptionEventType.RENEWED, null, null, null);

        log.info("Subscription renewed: {}, new period: {} to {}",
                subscriptionId, newPeriodStart, newPeriodEnd);
        return subscription;
    }

    /**
     * End trial and transition to first paid period
     */
    @Transactional
    public Subscription endTrial(UUID subscriptionId) {
        log.info("Ending trial: {}", subscriptionId);

        Subscription subscription = findById(subscriptionId);

        if (subscription.getStatus() != SubscriptionStatus.TRIAL) {
            throw new IllegalStateException("Subscription is not in trial: " + subscription.getStatus());
        }

        recordEvent(subscription, SubscriptionEventType.TRIAL_ENDED, null, null, null);

        // Transition to pending payment for first charge
        subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
        subscription = subscriptionRepository.save(subscription);

        return subscription;
    }

    // =========== Query methods ===========

    public Subscription findById(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));
    }

    public Optional<Subscription> findByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public List<Subscription> findTrialsEndingBefore(LocalDateTime date) {
        return subscriptionRepository.findTrialsEndingBefore(date);
    }

    public List<Subscription> findSubscriptionsToRenew(LocalDateTime date) {
        return subscriptionRepository.findSubscriptionsToRenew(date);
    }

    public List<Subscription> findGracePeriodExpired(LocalDateTime date) {
        return subscriptionRepository.findGracePeriodExpired(date);
    }

    public List<Subscription> findSuspendedOlderThan(LocalDateTime date) {
        return subscriptionRepository.findSuspendedOlderThan(date);
    }

    // =========== Helper methods ===========

    private void recordEvent(Subscription subscription,
                             SubscriptionEventType eventType,
                             SubscriptionStatus previousStatus,
                             SubscriptionStatus newStatus,
                             SubscriptionPlan previousPlan) {
        recordEvent(subscription, eventType, previousStatus, newStatus, previousPlan, null);
    }

    private void recordEvent(Subscription subscription,
                             SubscriptionEventType eventType,
                             SubscriptionStatus previousStatus,
                             SubscriptionStatus newStatus,
                             SubscriptionPlan previousPlan,
                             SubscriptionPlan newPlan) {
        SubscriptionEvent event = SubscriptionEvent.builder()
                .subscription(subscription)
                .user(subscription.getUser())
                .eventType(eventType)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .previousPlan(previousPlan)
                .newPlan(newPlan)
                .build();

        eventRepository.save(event);
    }

    private SubscriptionDto toDto(Subscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .planCode(subscription.getPlan().getCode())
                .planName(subscription.getPlan().getName())
                .status(subscription.getStatus())
                .billingCycle(subscription.getBillingCycle())
                .price(subscription.getPrice().getPriceAmount())
                .currency(subscription.getPrice().getCurrency())
                .trialEndDate(subscription.getTrialEndDate())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .autoRenew(subscription.getAutoRenew())
                .maxStores(subscription.getPlan().getMaxStores())
                .hasDowngradeScheduled(subscription.getDowngradeToPlan() != null)
                .downgradeToPlanCode(subscription.getDowngradeToPlan() != null ?
                        subscription.getDowngradeToPlan().getCode() : null)
                .build();
    }
}
