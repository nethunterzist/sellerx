package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qa_patterns")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "pattern_hash", nullable = false, length = 64)
    private String patternHash;

    @Column(name = "canonical_question", columnDefinition = "TEXT", nullable = false)
    private String canonicalQuestion;

    @Column(name = "canonical_answer", columnDefinition = "TEXT")
    private String canonicalAnswer;

    @Column(name = "occurrence_count", nullable = false)
    @Builder.Default
    private Integer occurrenceCount = 1;

    @Column(name = "approval_count", nullable = false)
    @Builder.Default
    private Integer approvalCount = 0;

    @Column(name = "rejection_count", nullable = false)
    @Builder.Default
    private Integer rejectionCount = 0;

    @Column(name = "modification_count", nullable = false)
    @Builder.Default
    private Integer modificationCount = 0;

    @Column(name = "confidence_score", precision = 5, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Column(name = "last_human_review")
    private LocalDateTime lastHumanReview;

    @Column(name = "seniority_level", nullable = false, length = 20)
    @Builder.Default
    private String seniorityLevel = SENIORITY_JUNIOR;

    @Column(name = "is_auto_submit_eligible", nullable = false)
    @Builder.Default
    private Boolean isAutoSubmitEligible = false;

    @Column(name = "auto_submit_enabled_at")
    private LocalDateTime autoSubmitEnabledAt;

    @Column(name = "auto_submit_disabled_reason", columnDefinition = "TEXT")
    private String autoSubmitDisabledReason;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "category")
    private String category;

    @Column(name = "first_seen_at", nullable = false)
    @Builder.Default
    private LocalDateTime firstSeenAt = LocalDateTime.now();

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastSeenAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Seniority level constants
    public static final String SENIORITY_JUNIOR = "JUNIOR";
    public static final String SENIORITY_LEARNING = "LEARNING";
    public static final String SENIORITY_SENIOR = "SENIOR";
    public static final String SENIORITY_EXPERT = "EXPERT";

    // Helper methods
    public int getTotalReviews() {
        return approvalCount + rejectionCount + modificationCount;
    }

    public double getApprovalRate() {
        int total = getTotalReviews();
        return total == 0 ? 0.0 : (double) approvalCount / total;
    }
}
