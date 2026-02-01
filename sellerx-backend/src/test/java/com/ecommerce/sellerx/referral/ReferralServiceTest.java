package com.ecommerce.sellerx.referral;

import com.ecommerce.sellerx.billing.*;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReferralService")
class ReferralServiceTest extends BaseUnitTest {

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionEventRepository eventRepository;

    private ReferralConfig config;
    private ReferralService referralService;

    private User referrerUser;
    private User referredUser;
    private SubscriptionPlan starterPlan;
    private SubscriptionPrice monthlyPrice;

    @BeforeEach
    void setUp() {
        config = new ReferralConfig();
        config.setRewardDays(15);
        config.setReferredTrialDays(30);
        config.setMaxBonusDays(180);
        config.setCodeLength(8);
        config.setBaseUrl("http://localhost:3000");

        referralService = new ReferralService(
                referralRepository,
                userRepository,
                subscriptionRepository,
                eventRepository,
                config
        );

        referrerUser = TestDataBuilder.user().build();
        referrerUser.setId(1L);
        referrerUser.setReferralCode("ABC12345");

        referredUser = TestDataBuilder.user().build();
        referredUser.setId(2L);
        referredUser.setReferredByUserId(1L);

        starterPlan = SubscriptionPlan.builder()
                .code("STARTER")
                .name("Starter Plan")
                .sortOrder(1)
                .isActive(true)
                .build();
        starterPlan.setId(UUID.randomUUID());

        monthlyPrice = SubscriptionPrice.builder()
                .plan(starterPlan)
                .billingCycle(BillingCycle.MONTHLY)
                .priceAmount(new BigDecimal("299.00"))
                .currency("TRY")
                .isActive(true)
                .build();
        monthlyPrice.setId(UUID.randomUUID());
    }

    // =========================================================================
    // getOrCreateReferralCode
    // =========================================================================

    @Nested
    @DisplayName("getOrCreateReferralCode")
    class GetOrCreateReferralCode {

        @Test
        @DisplayName("should return existing referral code")
        void shouldReturnExistingCode() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(referrerUser));

            String code = referralService.getOrCreateReferralCode(1L);

            assertThat(code).isEqualTo("ABC12345");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should generate new code for user without one")
        void shouldGenerateNewCode() {
            User userWithoutCode = TestDataBuilder.user().build();
            userWithoutCode.setId(3L);
            userWithoutCode.setReferralCode(null);

            Subscription activeSub = buildSubscription(userWithoutCode, SubscriptionStatus.ACTIVE);

            when(userRepository.findById(3L)).thenReturn(Optional.of(userWithoutCode));
            when(subscriptionRepository.findByUserId(3L)).thenReturn(Optional.of(activeSub));
            when(userRepository.existsByReferralCode(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            String code = referralService.getOrCreateReferralCode(3L);

            assertThat(code).isNotNull();
            assertThat(code).hasSize(8);
            verify(userRepository).save(userWithoutCode);
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> referralService.getOrCreateReferralCode(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should throw when user has no active paid subscription")
        void shouldThrowWhenNoPaidSubscription() {
            User userWithoutCode = TestDataBuilder.user().build();
            userWithoutCode.setId(4L);
            userWithoutCode.setReferralCode(null);

            when(userRepository.findById(4L)).thenReturn(Optional.of(userWithoutCode));
            when(subscriptionRepository.findByUserId(4L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> referralService.getOrCreateReferralCode(4L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only paid plan users");
        }

        @Test
        @DisplayName("should throw when user is on FREE plan")
        void shouldThrowWhenOnFreePlan() {
            User userWithoutCode = TestDataBuilder.user().build();
            userWithoutCode.setId(5L);
            userWithoutCode.setReferralCode(null);

            SubscriptionPlan freePlan = SubscriptionPlan.builder()
                    .code("FREE")
                    .name("Free Plan")
                    .sortOrder(0)
                    .isActive(true)
                    .build();
            freePlan.setId(UUID.randomUUID());

            Subscription freeSub = Subscription.builder()
                    .id(UUID.randomUUID())
                    .user(userWithoutCode)
                    .plan(freePlan)
                    .price(monthlyPrice)
                    .status(SubscriptionStatus.ACTIVE)
                    .billingCycle(BillingCycle.MONTHLY)
                    .currentPeriodStart(LocalDateTime.now())
                    .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                    .cancelAtPeriodEnd(false)
                    .autoRenew(true)
                    .build();

            when(userRepository.findById(5L)).thenReturn(Optional.of(userWithoutCode));
            when(subscriptionRepository.findByUserId(5L)).thenReturn(Optional.of(freeSub));

            assertThatThrownBy(() -> referralService.getOrCreateReferralCode(5L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only paid plan users");
        }
    }

    // =========================================================================
    // recordReferral
    // =========================================================================

    @Nested
    @DisplayName("recordReferral")
    class RecordReferral {

        @Test
        @DisplayName("should record referral successfully")
        void shouldRecordReferral() {
            when(userRepository.findByReferralCode("ABC12345")).thenReturn(Optional.of(referrerUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(referredUser));
            when(referralRepository.existsByReferrerUserIdAndReferredUserId(1L, 2L)).thenReturn(false);
            when(referralRepository.save(any(Referral.class))).thenAnswer(inv -> {
                Referral r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            referralService.recordReferral(2L, "ABC12345");

            ArgumentCaptor<Referral> captor = ArgumentCaptor.forClass(Referral.class);
            verify(referralRepository).save(captor.capture());
            Referral saved = captor.getValue();

            assertThat(saved.getReferrerUser()).isEqualTo(referrerUser);
            assertThat(saved.getReferredUser()).isEqualTo(referredUser);
            assertThat(saved.getReferralCode()).isEqualTo("ABC12345");
            assertThat(saved.getStatus()).isEqualTo(ReferralStatus.PENDING);

            verify(userRepository).save(referredUser);
            assertThat(referredUser.getReferredByUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw on invalid referral code")
        void shouldThrowOnInvalidCode() {
            when(userRepository.findByReferralCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> referralService.recordReferral(2L, "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid referral code");
        }

        @Test
        @DisplayName("should throw on self-referral")
        void shouldThrowOnSelfReferral() {
            when(userRepository.findByReferralCode("ABC12345")).thenReturn(Optional.of(referrerUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(referrerUser));

            assertThatThrownBy(() -> referralService.recordReferral(1L, "ABC12345"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot refer yourself");
        }

        @Test
        @DisplayName("should silently ignore duplicate referral")
        void shouldIgnoreDuplicateReferral() {
            when(userRepository.findByReferralCode("ABC12345")).thenReturn(Optional.of(referrerUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(referredUser));
            when(referralRepository.existsByReferrerUserIdAndReferredUserId(1L, 2L)).thenReturn(true);

            referralService.recordReferral(2L, "ABC12345");

            verify(referralRepository, never()).save(any(Referral.class));
        }
    }

    // =========================================================================
    // processReferralReward
    // =========================================================================

    @Nested
    @DisplayName("processReferralReward")
    class ProcessReferralReward {

        @Test
        @DisplayName("should grant reward days to referrer")
        void shouldGrantRewardDays() {
            Referral referral = Referral.builder()
                    .id(UUID.randomUUID())
                    .referrerUser(referrerUser)
                    .referredUser(referredUser)
                    .referralCode("ABC12345")
                    .status(ReferralStatus.PENDING)
                    .rewardDaysGranted(0)
                    .build();

            LocalDateTime originalPeriodEnd = LocalDateTime.now().plusMonths(1);
            Subscription referrerSub = buildSubscription(referrerUser, SubscriptionStatus.ACTIVE);
            referrerSub.setCurrentPeriodEnd(originalPeriodEnd);

            when(referralRepository.findByReferredUserId(2L)).thenReturn(Optional.of(referral));
            when(referralRepository.totalRewardDaysByReferrerId(1L)).thenReturn(0);
            when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(referrerSub));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));
            when(referralRepository.save(any(Referral.class))).thenAnswer(inv -> inv.getArgument(0));

            referralService.processReferralReward(2L);

            assertThat(referrerSub.getCurrentPeriodEnd()).isEqualTo(originalPeriodEnd.plusDays(15));
            assertThat(referral.getStatus()).isEqualTo(ReferralStatus.COMPLETED);
            assertThat(referral.getRewardDaysGranted()).isEqualTo(15);
            assertThat(referral.getRewardAppliedAt()).isNotNull();

            verify(eventRepository).save(any(SubscriptionEvent.class));
        }

        @Test
        @DisplayName("should do nothing when user was not referred")
        void shouldDoNothingWhenNotReferred() {
            when(referralRepository.findByReferredUserId(2L)).thenReturn(Optional.empty());

            referralService.processReferralReward(2L);

            verify(subscriptionRepository, never()).save(any());
            verify(referralRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip when referral already completed")
        void shouldSkipWhenAlreadyCompleted() {
            Referral referral = Referral.builder()
                    .id(UUID.randomUUID())
                    .referrerUser(referrerUser)
                    .referredUser(referredUser)
                    .referralCode("ABC12345")
                    .status(ReferralStatus.COMPLETED)
                    .rewardDaysGranted(15)
                    .build();

            when(referralRepository.findByReferredUserId(2L)).thenReturn(Optional.of(referral));

            referralService.processReferralReward(2L);

            verify(subscriptionRepository, never()).save(any());
            verify(referralRepository, never()).save(any());
        }

        @Test
        @DisplayName("should cap reward at max bonus days")
        void shouldCapRewardAtMaxBonusDays() {
            Referral referral = Referral.builder()
                    .id(UUID.randomUUID())
                    .referrerUser(referrerUser)
                    .referredUser(referredUser)
                    .referralCode("ABC12345")
                    .status(ReferralStatus.PENDING)
                    .rewardDaysGranted(0)
                    .build();

            // Already earned 180 days (the cap)
            when(referralRepository.findByReferredUserId(2L)).thenReturn(Optional.of(referral));
            when(referralRepository.totalRewardDaysByReferrerId(1L)).thenReturn(180);
            when(referralRepository.save(any(Referral.class))).thenAnswer(inv -> inv.getArgument(0));

            referralService.processReferralReward(2L);

            assertThat(referral.getStatus()).isEqualTo(ReferralStatus.COMPLETED);
            assertThat(referral.getRewardDaysGranted()).isEqualTo(0);
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should grant partial days when near cap")
        void shouldGrantPartialDaysNearCap() {
            Referral referral = Referral.builder()
                    .id(UUID.randomUUID())
                    .referrerUser(referrerUser)
                    .referredUser(referredUser)
                    .referralCode("ABC12345")
                    .status(ReferralStatus.PENDING)
                    .rewardDaysGranted(0)
                    .build();

            LocalDateTime originalPeriodEnd = LocalDateTime.now().plusMonths(1);
            Subscription referrerSub = buildSubscription(referrerUser, SubscriptionStatus.ACTIVE);
            referrerSub.setCurrentPeriodEnd(originalPeriodEnd);

            // Already earned 170 days; max is 180, so only 10 can be granted
            when(referralRepository.findByReferredUserId(2L)).thenReturn(Optional.of(referral));
            when(referralRepository.totalRewardDaysByReferrerId(1L)).thenReturn(170);
            when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(referrerSub));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(eventRepository.save(any(SubscriptionEvent.class))).thenAnswer(inv -> inv.getArgument(0));
            when(referralRepository.save(any(Referral.class))).thenAnswer(inv -> inv.getArgument(0));

            referralService.processReferralReward(2L);

            assertThat(referral.getRewardDaysGranted()).isEqualTo(10);
            assertThat(referrerSub.getCurrentPeriodEnd()).isEqualTo(originalPeriodEnd.plusDays(10));
        }
    }

    // =========================================================================
    // isValidReferralCode
    // =========================================================================

    @Nested
    @DisplayName("isValidReferralCode")
    class IsValidReferralCode {

        @Test
        @DisplayName("should return true for valid code")
        void shouldReturnTrueForValidCode() {
            when(userRepository.existsByReferralCode("ABC12345")).thenReturn(true);

            assertThat(referralService.isValidReferralCode("abc12345")).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid code")
        void shouldReturnFalseForInvalidCode() {
            when(userRepository.existsByReferralCode("INVALID1")).thenReturn(false);

            assertThat(referralService.isValidReferralCode("INVALID1")).isFalse();
        }

        @Test
        @DisplayName("should return false for null code")
        void shouldReturnFalseForNullCode() {
            assertThat(referralService.isValidReferralCode(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for blank code")
        void shouldReturnFalseForBlankCode() {
            assertThat(referralService.isValidReferralCode("  ")).isFalse();
        }
    }

    // =========================================================================
    // getTrialDaysForUser
    // =========================================================================

    @Nested
    @DisplayName("getTrialDaysForUser")
    class GetTrialDaysForUser {

        @Test
        @DisplayName("should return extended trial for referred user")
        void shouldReturnExtendedTrialForReferredUser() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(referredUser));

            int days = referralService.getTrialDaysForUser(2L);

            assertThat(days).isEqualTo(30);
        }

        @Test
        @DisplayName("should return default trial for non-referred user")
        void shouldReturnDefaultTrialForNonReferredUser() {
            User regularUser = TestDataBuilder.user().build();
            regularUser.setId(3L);
            regularUser.setReferredByUserId(null);

            when(userRepository.findById(3L)).thenReturn(Optional.of(regularUser));

            int days = referralService.getTrialDaysForUser(3L);

            assertThat(days).isEqualTo(14);
        }

        @Test
        @DisplayName("should return default trial when user not found")
        void shouldReturnDefaultTrialWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            int days = referralService.getTrialDaysForUser(99L);

            assertThat(days).isEqualTo(14);
        }
    }

    // =========================================================================
    // getReferralStats
    // =========================================================================

    @Nested
    @DisplayName("getReferralStats")
    class GetReferralStats {

        @Test
        @DisplayName("should return stats for user with referral code")
        void shouldReturnStatsForUserWithCode() {
            Referral referral = Referral.builder()
                    .id(UUID.randomUUID())
                    .referrerUser(referrerUser)
                    .referredUser(referredUser)
                    .referralCode("ABC12345")
                    .status(ReferralStatus.COMPLETED)
                    .rewardDaysGranted(15)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(referrerUser));
            when(referralRepository.findByReferrerUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(referral));
            when(referralRepository.totalRewardDaysByReferrerId(1L)).thenReturn(15);
            when(referralRepository.countByReferrerUserIdAndStatus(1L, ReferralStatus.COMPLETED)).thenReturn(1L);
            when(referralRepository.countByReferrerUserIdAndStatus(1L, ReferralStatus.PENDING)).thenReturn(0L);
            when(subscriptionRepository.findByUserId(1L))
                    .thenReturn(Optional.of(buildSubscription(referrerUser, SubscriptionStatus.ACTIVE)));

            ReferralStatsDto stats = referralService.getReferralStats(1L);

            assertThat(stats.getReferralCode()).isEqualTo("ABC12345");
            assertThat(stats.getReferralLink()).isEqualTo("http://localhost:3000/register?ref=ABC12345");
            assertThat(stats.getTotalReferrals()).isEqualTo(1);
            assertThat(stats.getCompletedReferrals()).isEqualTo(1);
            assertThat(stats.getPendingReferrals()).isEqualTo(0);
            assertThat(stats.getTotalDaysEarned()).isEqualTo(15);
            assertThat(stats.getMaxBonusDaysRemaining()).isEqualTo(165);
            assertThat(stats.isCanRefer()).isTrue();
        }

        @Test
        @DisplayName("should return empty stats for user without code")
        void shouldReturnEmptyStatsForUserWithoutCode() {
            User noCodeUser = TestDataBuilder.user().build();
            noCodeUser.setId(3L);
            noCodeUser.setReferralCode(null);

            when(userRepository.findById(3L)).thenReturn(Optional.of(noCodeUser));
            when(subscriptionRepository.findByUserId(3L)).thenReturn(Optional.empty());

            ReferralStatsDto stats = referralService.getReferralStats(3L);

            assertThat(stats.getReferralCode()).isNull();
            assertThat(stats.getReferralLink()).isNull();
            assertThat(stats.getTotalReferrals()).isEqualTo(0);
            assertThat(stats.isCanRefer()).isFalse();
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Subscription buildSubscription(User user, SubscriptionStatus status) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(starterPlan)
                .price(monthlyPrice)
                .status(status)
                .billingCycle(BillingCycle.MONTHLY)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .cancelAtPeriodEnd(false)
                .autoRenew(true)
                .build();
    }
}
