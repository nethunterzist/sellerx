package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.billing.config.SubscriptionConfig;
import com.ecommerce.sellerx.billing.service.SubscriptionService;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.referral.ReferralService;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("SubscriptionService")
class SubscriptionServiceTest extends BaseUnitTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private SubscriptionPriceRepository priceRepository;

    @Mock
    private SubscriptionEventRepository eventRepository;

    @Mock
    private UserService userService;

    @Mock
    private ReferralService referralService;

    private SubscriptionConfig config;
    private SubscriptionService subscriptionService;

    private User testUser;
    private SubscriptionPlan starterPlan;
    private SubscriptionPlan proPlan;
    private SubscriptionPrice monthlyPrice;
    private SubscriptionPrice proMonthlyPrice;

    @BeforeEach
    void setUp() {
        config = new SubscriptionConfig();
        config.setTrialDays(14);
        config.setGracePeriodDays(3);

        subscriptionService = new SubscriptionService(
                subscriptionRepository,
                planRepository,
                priceRepository,
                eventRepository,
                config,
                userService,
                referralService
        );

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);

        starterPlan = SubscriptionPlan.builder()
                .code("STARTER")
                .name("Starter Plan")
                .maxStores(1)
                .sortOrder(1)
                .isActive(true)
                .build();
        starterPlan.setId(UUID.randomUUID());

        proPlan = SubscriptionPlan.builder()
                .code("PRO")
                .name("Pro Plan")
                .maxStores(5)
                .sortOrder(2)
                .isActive(true)
                .build();
        proPlan.setId(UUID.randomUUID());

        monthlyPrice = SubscriptionPrice.builder()
                .plan(starterPlan)
                .billingCycle(BillingCycle.MONTHLY)
                .priceAmount(new BigDecimal("299.00"))
                .currency("TRY")
                .isActive(true)
                .build();
        monthlyPrice.setId(UUID.randomUUID());

        proMonthlyPrice = SubscriptionPrice.builder()
                .plan(proPlan)
                .billingCycle(BillingCycle.MONTHLY)
                .priceAmount(new BigDecimal("599.00"))
                .currency("TRY")
                .isActive(true)
                .build();
        proMonthlyPrice.setId(UUID.randomUUID());
    }

    // =========================================================================
    // createSubscription
    // =========================================================================

    @Nested
    @DisplayName("createSubscription")
    class CreateSubscription {

        @Test
        @DisplayName("should create subscription with trial period")
        void shouldCreateWithTrialPeriod() {
            when(subscriptionRepository.existsByUserId(1L)).thenReturn(false);
            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(planRepository.findByCodeAndIsActiveTrue("STARTER")).thenReturn(Optional.of(starterPlan));
            when(priceRepository.findByPlanIdAndBillingCycleAndIsActiveTrue(starterPlan.getId(), BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(monthlyPrice));
            when(referralService.getTrialDaysForUser(1L)).thenReturn(14);
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
                Subscription s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.createSubscription(1L, "STARTER", BillingCycle.MONTHLY);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getPlan()).isEqualTo(starterPlan);
            assertThat(result.getPrice()).isEqualTo(monthlyPrice);
            assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(result.getAutoRenew()).isTrue();
            assertThat(result.getTrialStartDate()).isNotNull();
            assertThat(result.getTrialEndDate()).isAfter(result.getTrialStartDate());

            verify(subscriptionRepository).save(any(Subscription.class));
            verify(eventRepository, times(2)).save(any(SubscriptionEvent.class));
        }

        @Test
        @DisplayName("should use extended trial days for referred users")
        void shouldUseExtendedTrialForReferredUsers() {
            when(subscriptionRepository.existsByUserId(1L)).thenReturn(false);
            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(planRepository.findByCodeAndIsActiveTrue("STARTER")).thenReturn(Optional.of(starterPlan));
            when(priceRepository.findByPlanIdAndBillingCycleAndIsActiveTrue(starterPlan.getId(), BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(monthlyPrice));
            when(referralService.getTrialDaysForUser(1L)).thenReturn(30);
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
                Subscription s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.createSubscription(1L, "STARTER", BillingCycle.MONTHLY);

            // Trial end should be ~30 days from now, not 14
            assertThat(result.getTrialEndDate())
                    .isAfter(LocalDateTime.now().plusDays(29))
                    .isBefore(LocalDateTime.now().plusDays(31));
        }

        @Test
        @DisplayName("should throw when user already has subscription")
        void shouldThrowWhenUserAlreadyHasSubscription() {
            when(subscriptionRepository.existsByUserId(1L)).thenReturn(true);

            assertThatThrownBy(() -> subscriptionService.createSubscription(1L, "STARTER", BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already has an active subscription");
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(subscriptionRepository.existsByUserId(1L)).thenReturn(false);
            when(userService.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.createSubscription(1L, "STARTER", BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should throw when plan not found")
        void shouldThrowWhenPlanNotFound() {
            when(subscriptionRepository.existsByUserId(1L)).thenReturn(false);
            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(planRepository.findByCodeAndIsActiveTrue("NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.createSubscription(1L, "NONEXISTENT", BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plan not found");
        }

        @Test
        @DisplayName("should throw when price not found for billing cycle")
        void shouldThrowWhenPriceNotFound() {
            when(subscriptionRepository.existsByUserId(1L)).thenReturn(false);
            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(planRepository.findByCodeAndIsActiveTrue("STARTER")).thenReturn(Optional.of(starterPlan));
            when(priceRepository.findByPlanIdAndBillingCycleAndIsActiveTrue(starterPlan.getId(), BillingCycle.YEARLY))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.createSubscription(1L, "STARTER", BillingCycle.YEARLY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price not found");
        }
    }

    // =========================================================================
    // activateSubscription
    // =========================================================================

    @Nested
    @DisplayName("activateSubscription")
    class ActivateSubscription {

        @Test
        @DisplayName("should activate trial subscription")
        void shouldActivateTrialSubscription() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.TRIAL);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.activateSubscription(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getCurrentPeriodStart()).isNotNull();
            assertThat(result.getGracePeriodEnd()).isNull();

            verify(referralService).processReferralReward(testUser.getId());
        }

        @Test
        @DisplayName("should return subscription unchanged when already active")
        void shouldReturnUnchangedWhenAlreadyActive() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));

            Subscription result = subscriptionService.activateSubscription(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not fail when referral reward processing fails")
        void shouldNotFailWhenReferralRewardFails() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.TRIAL);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Referral error")).when(referralService).processReferralReward(anyLong());

            Subscription result = subscriptionService.activateSubscription(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }

    // =========================================================================
    // upgradePlan
    // =========================================================================

    @Nested
    @DisplayName("upgradePlan")
    class UpgradePlan {

        @Test
        @DisplayName("should upgrade from starter to pro")
        void shouldUpgradeFromStarterToPro() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(planRepository.findByCodeAndIsActiveTrue("PRO")).thenReturn(Optional.of(proPlan));
            when(priceRepository.findByPlanIdAndBillingCycleAndIsActiveTrue(proPlan.getId(), BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(proMonthlyPrice));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.upgradePlan(subId, "PRO", BillingCycle.MONTHLY);

            assertThat(result.getPlan()).isEqualTo(proPlan);
            assertThat(result.getPrice()).isEqualTo(proMonthlyPrice);
            assertThat(result.getDowngradeToPlan()).isNull();
            assertThat(result.getDowngradeToPrice()).isNull();
        }

        @Test
        @DisplayName("should throw when upgrading to same or lower tier")
        void shouldThrowWhenUpgradingToSameOrLowerTier() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);
            subscription.setPlan(proPlan); // already on PRO

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(planRepository.findByCodeAndIsActiveTrue("STARTER")).thenReturn(Optional.of(starterPlan));

            assertThatThrownBy(() -> subscriptionService.upgradePlan(subId, "STARTER", BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot upgrade to same or lower tier");
        }
    }

    // =========================================================================
    // schedulePlanDowngrade
    // =========================================================================

    @Nested
    @DisplayName("schedulePlanDowngrade")
    class SchedulePlanDowngrade {

        @Test
        @DisplayName("should schedule downgrade for period end")
        void shouldScheduleDowngradeForPeriodEnd() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);
            subscription.setPlan(proPlan);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(planRepository.findByCodeAndIsActiveTrue("STARTER")).thenReturn(Optional.of(starterPlan));
            when(priceRepository.findByPlanIdAndBillingCycleAndIsActiveTrue(starterPlan.getId(), BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(monthlyPrice));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.schedulePlanDowngrade(subId, "STARTER", BillingCycle.MONTHLY);

            assertThat(result.getDowngradeToPlan()).isEqualTo(starterPlan);
            assertThat(result.getDowngradeToPrice()).isEqualTo(monthlyPrice);
        }

        @Test
        @DisplayName("should throw when downgrading to same or higher tier")
        void shouldThrowWhenDowngradingToSameOrHigherTier() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(planRepository.findByCodeAndIsActiveTrue("PRO")).thenReturn(Optional.of(proPlan));

            assertThatThrownBy(() -> subscriptionService.schedulePlanDowngrade(subId, "PRO", BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot downgrade to same or higher tier");
        }
    }

    // =========================================================================
    // cancelSubscription
    // =========================================================================

    @Nested
    @DisplayName("cancelSubscription")
    class CancelSubscription {

        @Test
        @DisplayName("should cancel active subscription at period end")
        void shouldCancelAtPeriodEnd() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.cancelSubscription(subId, "Too expensive");

            assertThat(result.getCancelAtPeriodEnd()).isTrue();
            assertThat(result.getCancelledAt()).isNotNull();
            assertThat(result.getCancellationReason()).isEqualTo("Too expensive");
            assertThat(result.getAutoRenew()).isFalse();
        }

        @Test
        @DisplayName("should throw when cancelling expired subscription")
        void shouldThrowWhenCancellingExpiredSubscription() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.EXPIRED);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(subId, "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel subscription in status");
        }

        @Test
        @DisplayName("should throw when cancelling suspended subscription")
        void shouldThrowWhenCancellingSuspendedSubscription() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.SUSPENDED);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(subId, "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel subscription in status");
        }
    }

    // =========================================================================
    // reactivateSubscription
    // =========================================================================

    @Nested
    @DisplayName("reactivateSubscription")
    class ReactivateSubscription {

        @Test
        @DisplayName("should reactivate cancelled subscription")
        void shouldReactivateCancelledSubscription() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);
            subscription.setCancelAtPeriodEnd(true);
            subscription.setCancellationReason("Too expensive");

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.reactivateSubscription(subId);

            assertThat(result.getCancelAtPeriodEnd()).isFalse();
            assertThat(result.getCancellationReason()).isNull();
            assertThat(result.getAutoRenew()).isTrue();
        }

        @Test
        @DisplayName("should throw when subscription is not scheduled for cancellation")
        void shouldThrowWhenNotScheduledForCancellation() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);
            subscription.setCancelAtPeriodEnd(false);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));

            assertThatThrownBy(() -> subscriptionService.reactivateSubscription(subId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not scheduled for cancellation");
        }

        @Test
        @DisplayName("should throw when reactivating expired subscription")
        void shouldThrowWhenReactivatingExpiredSubscription() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.EXPIRED);
            subscription.setCancelAtPeriodEnd(true);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));

            assertThatThrownBy(() -> subscriptionService.reactivateSubscription(subId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot reactivate expired");
        }
    }

    // =========================================================================
    // renewSubscription
    // =========================================================================

    @Nested
    @DisplayName("renewSubscription")
    class RenewSubscription {

        @Test
        @DisplayName("should extend billing period on renewal")
        void shouldExtendBillingPeriod() {
            UUID subId = UUID.randomUUID();
            LocalDateTime periodEnd = LocalDateTime.now().plusDays(1);
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodEnd(periodEnd);
            subscription.setCancelAtPeriodEnd(false);
            subscription.setAutoRenew(true);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.renewSubscription(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getCurrentPeriodStart()).isEqualTo(periodEnd);
            assertThat(result.getCurrentPeriodEnd()).isAfter(periodEnd);
            assertThat(result.getGracePeriodEnd()).isNull();
        }

        @Test
        @DisplayName("should cancel subscription when cancelAtPeriodEnd is true")
        void shouldCancelWhenCancelAtPeriodEnd() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);
            subscription.setCancelAtPeriodEnd(true);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.renewSubscription(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }
    }

    // =========================================================================
    // endTrial
    // =========================================================================

    @Nested
    @DisplayName("endTrial")
    class EndTrial {

        @Test
        @DisplayName("should transition trial to pending payment")
        void shouldTransitionTrialToPendingPayment() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.TRIAL);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.endTrial(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        }

        @Test
        @DisplayName("should throw when subscription is not in trial")
        void shouldThrowWhenNotInTrial() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));

            assertThatThrownBy(() -> subscriptionService.endTrial(subId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not in trial");
        }
    }

    // =========================================================================
    // markPastDue and suspendSubscription
    // =========================================================================

    @Nested
    @DisplayName("statusTransitions")
    class StatusTransitions {

        @Test
        @DisplayName("should mark subscription as past due with grace period")
        void shouldMarkPastDue() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.markPastDue(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
            assertThat(result.getGracePeriodEnd()).isNotNull();
            assertThat(result.getGracePeriodEnd()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("should suspend subscription")
        void shouldSuspendSubscription() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.PAST_DUE);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.suspendSubscription(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        }

        @Test
        @DisplayName("should expire subscription")
        void shouldExpireSubscription() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.SUSPENDED);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = subscriptionService.expireSubscription(subId);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        }
    }

    // =========================================================================
    // findById
    // =========================================================================

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return subscription when found")
        void shouldReturnSubscriptionWhenFound() {
            UUID subId = UUID.randomUUID();
            Subscription subscription = buildSubscription(subId, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));

            Subscription result = subscriptionService.findById(subId);

            assertThat(result).isEqualTo(subscription);
        }

        @Test
        @DisplayName("should throw when subscription not found")
        void shouldThrowWhenNotFound() {
            UUID subId = UUID.randomUUID();
            when(subscriptionRepository.findById(subId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.findById(subId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription not found");
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Subscription buildSubscription(UUID id, SubscriptionStatus status) {
        return Subscription.builder()
                .id(id)
                .user(testUser)
                .plan(starterPlan)
                .price(monthlyPrice)
                .status(status)
                .billingCycle(BillingCycle.MONTHLY)
                .trialStartDate(LocalDateTime.now().minusDays(14))
                .trialEndDate(LocalDateTime.now())
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .cancelAtPeriodEnd(false)
                .autoRenew(true)
                .build();
    }
}
