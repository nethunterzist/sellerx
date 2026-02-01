package com.ecommerce.sellerx.referral;

import com.ecommerce.sellerx.billing.*;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventRepository eventRepository;
    private final ReferralConfig config;

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Generate or retrieve referral code for a paid user.
     * Only users with active subscription on STARTER/PRO/ENTERPRISE can generate.
     */
    @Transactional
    public String getOrCreateReferralCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getReferralCode() != null) {
            return user.getReferralCode();
        }

        validateUserCanRefer(userId);

        String code = generateUniqueCode();
        user.setReferralCode(code);
        userRepository.save(user);

        log.info("Referral code generated: userId={}, code={}", userId, code);
        return code;
    }

    /**
     * Record a referral during registration.
     * Called from UserService.registerUser() when referralCode is provided.
     */
    @Transactional
    public void recordReferral(Long referredUserId, String referralCode) {
        User referrer = userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid referral code"));

        User referred = userRepository.findById(referredUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Self-referral prevention
        if (referrer.getId().equals(referred.getId())) {
            throw new IllegalArgumentException("Cannot refer yourself");
        }

        // Duplicate prevention
        if (referralRepository.existsByReferrerUserIdAndReferredUserId(
                referrer.getId(), referred.getId())) {
            log.warn("Duplicate referral attempt: referrer={}, referred={}",
                    referrer.getId(), referred.getId());
            return;
        }

        Referral referral = Referral.builder()
                .referrerUser(referrer)
                .referredUser(referred)
                .referralCode(referralCode)
                .status(ReferralStatus.PENDING)
                .build();
        referralRepository.save(referral);

        referred.setReferredByUserId(referrer.getId());
        userRepository.save(referred);

        log.info("Referral recorded: referrer={}, referred={}, code={}",
                referrer.getId(), referred.getId(), referralCode);
    }

    /**
     * Process referral reward when referred user makes first payment.
     * Called from SubscriptionService.activateSubscription().
     */
    @Transactional
    public void processReferralReward(Long referredUserId) {
        Optional<Referral> optReferral = referralRepository.findByReferredUserId(referredUserId);
        if (optReferral.isEmpty()) {
            return; // Not referred
        }

        Referral referral = optReferral.get();
        if (referral.getStatus() != ReferralStatus.PENDING) {
            return; // Already processed
        }

        Long referrerId = referral.getReferrerUser().getId();

        // Check bonus cap
        int totalEarned = referralRepository.totalRewardDaysByReferrerId(referrerId);
        if (totalEarned >= config.getMaxBonusDays()) {
            log.info("Referrer {} hit max bonus days cap ({}). Marking completed with 0 days.",
                    referrerId, config.getMaxBonusDays());
            referral.setStatus(ReferralStatus.COMPLETED);
            referral.setRewardDaysGranted(0);
            referral.setRewardAppliedAt(LocalDateTime.now());
            referralRepository.save(referral);
            return;
        }

        int daysToGrant = Math.min(config.getRewardDays(), config.getMaxBonusDays() - totalEarned);

        // Extend referrer's subscription
        Optional<Subscription> optSub = subscriptionRepository.findByUserId(referrerId);
        if (optSub.isPresent()) {
            Subscription sub = optSub.get();
            sub.setCurrentPeriodEnd(sub.getCurrentPeriodEnd().plusDays(daysToGrant));
            subscriptionRepository.save(sub);

            // Record event
            SubscriptionEvent event = SubscriptionEvent.builder()
                    .subscription(sub)
                    .user(sub.getUser())
                    .eventType(SubscriptionEventType.REFERRAL_REWARD)
                    .metadata(Map.of(
                            "referredUserId", referredUserId,
                            "daysGranted", daysToGrant,
                            "referralId", referral.getId().toString(),
                            "totalDaysEarned", totalEarned + daysToGrant
                    ))
                    .build();
            eventRepository.save(event);

            log.info("Referral reward granted: referrer={}, referred={}, days={}, newPeriodEnd={}",
                    referrerId, referredUserId, daysToGrant, sub.getCurrentPeriodEnd());
        }

        // Update referral record
        referral.setStatus(ReferralStatus.COMPLETED);
        referral.setRewardDaysGranted(daysToGrant);
        referral.setRewardAppliedAt(LocalDateTime.now());
        referralRepository.save(referral);
    }

    /**
     * Get referral stats for a user's dashboard.
     */
    @Transactional(readOnly = true)
    public ReferralStatsDto getReferralStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String code = user.getReferralCode();
        boolean canRefer = canUserRefer(userId);

        List<Referral> referrals = code != null
                ? referralRepository.findByReferrerUserIdOrderByCreatedAtDesc(userId)
                : List.of();

        int totalDaysEarned = code != null
                ? referralRepository.totalRewardDaysByReferrerId(userId)
                : 0;

        long completedCount = code != null
                ? referralRepository.countByReferrerUserIdAndStatus(userId, ReferralStatus.COMPLETED)
                : 0;

        long pendingCount = code != null
                ? referralRepository.countByReferrerUserIdAndStatus(userId, ReferralStatus.PENDING)
                : 0;

        List<ReferralDto> referralDtos = referrals.stream()
                .map(this::toDto)
                .toList();

        return ReferralStatsDto.builder()
                .referralCode(code)
                .referralLink(code != null ? config.getBaseUrl() + "/register?ref=" + code : null)
                .totalReferrals(referralDtos.size())
                .completedReferrals((int) completedCount)
                .pendingReferrals((int) pendingCount)
                .totalDaysEarned(totalDaysEarned)
                .maxBonusDaysRemaining(config.getMaxBonusDays() - totalDaysEarned)
                .canRefer(canRefer)
                .referrals(referralDtos)
                .build();
    }

    /**
     * Validate a referral code (public, used during registration).
     */
    @Transactional(readOnly = true)
    public boolean isValidReferralCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return userRepository.existsByReferralCode(code.toUpperCase());
    }

    /**
     * Get trial days for a user - 30 if referred, default otherwise.
     */
    public int getTrialDaysForUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getReferredByUserId() != null) {
            return config.getReferredTrialDays();
        }
        return 14; // default
    }

    // -- Private helpers --

    private boolean canUserRefer(Long userId) {
        Optional<Subscription> optSub = subscriptionRepository.findByUserId(userId);
        if (optSub.isEmpty()) {
            return false;
        }
        Subscription sub = optSub.get();
        if (!sub.getStatus().hasAccess()) {
            return false;
        }
        String planCode = sub.getPlan().getCode();
        return !"FREE".equals(planCode);
    }

    private void validateUserCanRefer(Long userId) {
        if (!canUserRefer(userId)) {
            throw new IllegalStateException(
                    "Only paid plan users (STARTER/PRO/ENTERPRISE) with active subscription can generate referral codes.");
        }
    }

    private String generateUniqueCode() {
        SecureRandom random = new SecureRandom();
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(config.getCodeLength());
            for (int i = 0; i < config.getCodeLength(); i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!userRepository.existsByReferralCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique referral code after 10 attempts");
    }

    private ReferralDto toDto(Referral referral) {
        User referred = referral.getReferredUser();
        return ReferralDto.builder()
                .id(referral.getId())
                .referredUserName(referred.getName())
                .referredUserEmail(maskEmail(referred.getEmail()))
                .status(referral.getStatus())
                .rewardDaysGranted(referral.getRewardDaysGranted())
                .createdAt(referral.getCreatedAt())
                .rewardAppliedAt(referral.getRewardAppliedAt())
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + parts[1];
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
    }
}
