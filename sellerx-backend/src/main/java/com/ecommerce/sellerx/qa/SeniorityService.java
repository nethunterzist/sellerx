package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seniority Service
 *
 * Pattern kıdem yönetimi ve auto-submit uygunluk kontrolü
 * JUNIOR → LEARNING → SENIOR → EXPERT progression
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeniorityService {

    private final QaPatternRepository patternRepository;

    // Seniority thresholds
    private static final int MIN_OCCURRENCES_FOR_LEARNING = 3;
    private static final int MIN_APPROVALS_FOR_SENIOR = 5;
    private static final double MIN_APPROVAL_RATE_FOR_SENIOR = 0.80;
    private static final int MIN_APPROVALS_FOR_EXPERT = 10;
    private static final double MIN_APPROVAL_RATE_FOR_EXPERT = 0.90;

    // Auto-submit requirements
    private static final int AUTO_SUBMIT_MIN_OCCURRENCES = 5;
    private static final int AUTO_SUBMIT_MIN_APPROVALS = 3;
    private static final double AUTO_SUBMIT_MIN_APPROVAL_RATE = 0.90;
    private static final int AUTO_SUBMIT_WAITING_DAYS = 3;

    /**
     * Record an approval for a pattern
     */
    @Transactional
    public void recordApproval(UUID patternId, boolean wasModified) {
        QaPattern pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new RuntimeException("Pattern not found: " + patternId));

        if (wasModified) {
            pattern.setModificationCount(pattern.getModificationCount() + 1);
        } else {
            pattern.setApprovalCount(pattern.getApprovalCount() + 1);
        }

        pattern.setLastHumanReview(LocalDateTime.now());

        // Recalculate seniority and confidence
        updateSeniorityLevel(pattern);
        updateConfidenceScore(pattern);
        checkAutoSubmitEligibility(pattern);

        patternRepository.save(pattern);

        log.info("Recorded {} for pattern {}, new seniority: {}",
                wasModified ? "modification" : "approval",
                patternId,
                pattern.getSeniorityLevel());
    }

    /**
     * Record a rejection for a pattern
     */
    @Transactional
    public void recordRejection(UUID patternId) {
        QaPattern pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new RuntimeException("Pattern not found: " + patternId));

        pattern.setRejectionCount(pattern.getRejectionCount() + 1);
        pattern.setLastHumanReview(LocalDateTime.now());

        // Recalculate seniority and confidence
        updateSeniorityLevel(pattern);
        updateConfidenceScore(pattern);

        // Disable auto-submit if enabled
        if (pattern.getIsAutoSubmitEligible()) {
            pattern.setIsAutoSubmitEligible(false);
            pattern.setAutoSubmitDisabledReason("Kullanıcı tarafından reddedildi");
        }

        patternRepository.save(pattern);

        log.info("Recorded rejection for pattern {}, new seniority: {}",
                patternId, pattern.getSeniorityLevel());
    }

    /**
     * Update seniority level based on metrics
     */
    private void updateSeniorityLevel(QaPattern pattern) {
        int totalReviews = pattern.getTotalReviews();
        double approvalRate = pattern.getApprovalRate();

        String newLevel;

        if (totalReviews >= MIN_APPROVALS_FOR_EXPERT &&
            approvalRate >= MIN_APPROVAL_RATE_FOR_EXPERT &&
            pattern.getApprovalCount() >= MIN_APPROVALS_FOR_EXPERT) {
            newLevel = QaPattern.SENIORITY_EXPERT;
        } else if (totalReviews >= MIN_APPROVALS_FOR_SENIOR &&
                   approvalRate >= MIN_APPROVAL_RATE_FOR_SENIOR &&
                   pattern.getApprovalCount() >= MIN_APPROVALS_FOR_SENIOR) {
            newLevel = QaPattern.SENIORITY_SENIOR;
        } else if (pattern.getOccurrenceCount() >= MIN_OCCURRENCES_FOR_LEARNING) {
            newLevel = QaPattern.SENIORITY_LEARNING;
        } else {
            newLevel = QaPattern.SENIORITY_JUNIOR;
        }

        pattern.setSeniorityLevel(newLevel);
    }

    /**
     * Update confidence score based on various factors
     */
    private void updateConfidenceScore(QaPattern pattern) {
        // Multi-factor confidence calculation
        double approvalFactor = pattern.getApprovalRate() * 0.4;

        // Occurrence factor (log scale, max at 20)
        double occurrenceFactor = Math.min(1.0,
                Math.log(pattern.getOccurrenceCount() + 1) / Math.log(21)) * 0.3;

        // Seniority factor
        double seniorityFactor = switch (pattern.getSeniorityLevel()) {
            case QaPattern.SENIORITY_EXPERT -> 1.0;
            case QaPattern.SENIORITY_SENIOR -> 0.75;
            case QaPattern.SENIORITY_LEARNING -> 0.5;
            default -> 0.25;
        } * 0.2;

        // Recency factor (more recent reviews = higher confidence)
        double recencyFactor = 0.1;
        if (pattern.getLastHumanReview() != null) {
            long daysSinceReview = java.time.Duration.between(
                    pattern.getLastHumanReview(), LocalDateTime.now()).toDays();
            recencyFactor = Math.max(0.0, 0.1 * (1.0 - daysSinceReview / 30.0));
        }

        double totalScore = approvalFactor + occurrenceFactor + seniorityFactor + recencyFactor;
        pattern.setConfidenceScore(BigDecimal.valueOf(totalScore).setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * Check and update auto-submit eligibility
     */
    private void checkAutoSubmitEligibility(QaPattern pattern) {
        // Must be EXPERT level
        if (!QaPattern.SENIORITY_EXPERT.equals(pattern.getSeniorityLevel())) {
            return;
        }

        // Check all requirements
        boolean meetsRequirements =
                pattern.getOccurrenceCount() >= AUTO_SUBMIT_MIN_OCCURRENCES &&
                pattern.getApprovalCount() >= AUTO_SUBMIT_MIN_APPROVALS &&
                pattern.getApprovalRate() >= AUTO_SUBMIT_MIN_APPROVAL_RATE &&
                pattern.getLastHumanReview() != null;

        if (!meetsRequirements) {
            return;
        }

        // Check waiting period (3 days since reaching EXPERT)
        if (!pattern.getIsAutoSubmitEligible()) {
            // First time reaching eligibility
            if (pattern.getAutoSubmitEnabledAt() == null) {
                // Set pending date, will be enabled after waiting period
                pattern.setAutoSubmitEnabledAt(LocalDateTime.now().plusDays(AUTO_SUBMIT_WAITING_DAYS));
                log.info("Pattern {} will become auto-submit eligible at {}",
                        pattern.getId(), pattern.getAutoSubmitEnabledAt());
            } else if (LocalDateTime.now().isAfter(pattern.getAutoSubmitEnabledAt())) {
                // Waiting period passed
                pattern.setIsAutoSubmitEligible(true);
                pattern.setAutoSubmitDisabledReason(null);
                log.info("Pattern {} is now auto-submit eligible", pattern.getId());
            }
        }
    }

    /**
     * Manually promote a pattern to higher seniority
     */
    @Transactional
    public QaPatternDto promotePattern(UUID patternId, User user) {
        QaPattern pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new RuntimeException("Pattern not found: " + patternId));

        String currentLevel = pattern.getSeniorityLevel();
        String newLevel = switch (currentLevel) {
            case QaPattern.SENIORITY_JUNIOR -> QaPattern.SENIORITY_LEARNING;
            case QaPattern.SENIORITY_LEARNING -> QaPattern.SENIORITY_SENIOR;
            case QaPattern.SENIORITY_SENIOR -> QaPattern.SENIORITY_EXPERT;
            default -> currentLevel;
        };

        if (!newLevel.equals(currentLevel)) {
            pattern.setSeniorityLevel(newLevel);
            pattern.setLastHumanReview(LocalDateTime.now());
            updateConfidenceScore(pattern);
            checkAutoSubmitEligibility(pattern);
            patternRepository.save(pattern);

            log.info("User {} promoted pattern {} from {} to {}",
                    user.getEmail(), patternId, currentLevel, newLevel);
        }

        return QaPatternDto.fromEntity(pattern);
    }

    /**
     * Manually demote a pattern to lower seniority
     */
    @Transactional
    public QaPatternDto demotePattern(UUID patternId, User user, String reason) {
        QaPattern pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new RuntimeException("Pattern not found: " + patternId));

        String currentLevel = pattern.getSeniorityLevel();
        String newLevel = switch (currentLevel) {
            case QaPattern.SENIORITY_EXPERT -> QaPattern.SENIORITY_SENIOR;
            case QaPattern.SENIORITY_SENIOR -> QaPattern.SENIORITY_LEARNING;
            case QaPattern.SENIORITY_LEARNING -> QaPattern.SENIORITY_JUNIOR;
            default -> currentLevel;
        };

        if (!newLevel.equals(currentLevel)) {
            pattern.setSeniorityLevel(newLevel);
            pattern.setLastHumanReview(LocalDateTime.now());

            // Disable auto-submit if demoting from EXPERT
            if (QaPattern.SENIORITY_EXPERT.equals(currentLevel)) {
                pattern.setIsAutoSubmitEligible(false);
                pattern.setAutoSubmitDisabledReason(reason != null ? reason : "Manuel olarak düşürüldü");
            }

            updateConfidenceScore(pattern);
            patternRepository.save(pattern);

            log.info("User {} demoted pattern {} from {} to {}, reason: {}",
                    user.getEmail(), patternId, currentLevel, newLevel, reason);
        }

        return QaPatternDto.fromEntity(pattern);
    }

    /**
     * Enable auto-submit immediately for a pattern (skip waiting period)
     */
    @Transactional
    public QaPatternDto enableAutoSubmit(UUID patternId, User user) {
        QaPattern pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new RuntimeException("Pattern not found: " + patternId));

        if (!QaPattern.SENIORITY_EXPERT.equals(pattern.getSeniorityLevel())) {
            throw new IllegalStateException("Pattern must be EXPERT level for auto-submit");
        }

        if (pattern.getApprovalCount() < AUTO_SUBMIT_MIN_APPROVALS ||
            pattern.getApprovalRate() < AUTO_SUBMIT_MIN_APPROVAL_RATE) {
            throw new IllegalStateException("Pattern does not meet minimum approval requirements");
        }

        pattern.setIsAutoSubmitEligible(true);
        pattern.setAutoSubmitEnabledAt(LocalDateTime.now());
        pattern.setAutoSubmitDisabledReason(null);
        patternRepository.save(pattern);

        log.info("User {} manually enabled auto-submit for pattern {}", user.getEmail(), patternId);

        return QaPatternDto.fromEntity(pattern);
    }

    /**
     * Disable auto-submit for a pattern
     */
    @Transactional
    public QaPatternDto disableAutoSubmit(UUID patternId, User user, String reason) {
        QaPattern pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new RuntimeException("Pattern not found: " + patternId));

        pattern.setIsAutoSubmitEligible(false);
        pattern.setAutoSubmitDisabledReason(reason != null ? reason : "Manuel olarak devre dışı bırakıldı");
        patternRepository.save(pattern);

        log.info("User {} disabled auto-submit for pattern {}, reason: {}",
                user.getEmail(), patternId, reason);

        return QaPatternDto.fromEntity(pattern);
    }

    /**
     * Get patterns for a store with optional filters
     */
    public List<QaPatternDto> getPatterns(UUID storeId, String seniorityLevel, Boolean autoSubmitOnly) {
        List<QaPattern> patterns;

        if (autoSubmitOnly != null && autoSubmitOnly) {
            patterns = patternRepository.findAutoSubmitEligibleByStoreId(storeId);
        } else if (seniorityLevel != null && !seniorityLevel.isEmpty()) {
            patterns = patternRepository.findByStoreIdAndSeniorityLevel(storeId, seniorityLevel);
        } else {
            patterns = patternRepository.findByStoreIdOrderBySeniorityLevelDescConfidenceScoreDesc(storeId);
        }

        return patterns.stream()
                .map(QaPatternDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get seniority statistics for a store
     */
    public SeniorityStatsDto getSeniorityStats(UUID storeId) {
        return patternRepository.getSeniorityStats(storeId);
    }

    /**
     * Check if a pattern is auto-submit eligible for immediate submission
     */
    public boolean canAutoSubmit(QaPattern pattern) {
        return pattern.getIsAutoSubmitEligible() &&
               QaPattern.SENIORITY_EXPERT.equals(pattern.getSeniorityLevel()) &&
               pattern.getConfidenceScore().compareTo(BigDecimal.valueOf(0.85)) >= 0;
    }

    /**
     * Scheduled job: Review auto-submit eligibility for pending patterns
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void reviewAutoSubmitEligibility() {
        log.info("Reviewing auto-submit eligibility...");

        List<QaPattern> pendingPatterns = patternRepository.findPatternsEligibleForAutoSubmitPromotion();

        for (QaPattern pattern : pendingPatterns) {
            if (pattern.getAutoSubmitEnabledAt() != null &&
                LocalDateTime.now().isAfter(pattern.getAutoSubmitEnabledAt()) &&
                !pattern.getIsAutoSubmitEligible()) {

                pattern.setIsAutoSubmitEligible(true);
                patternRepository.save(pattern);

                log.info("Pattern {} is now auto-submit eligible after waiting period", pattern.getId());
            }
        }

        log.info("Auto-submit eligibility review completed");
    }
}
