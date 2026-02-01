package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * User subscription
 */
@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_id")
    private SubscriptionPrice price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    // Trial period
    @Column(name = "trial_start_date")
    private LocalDateTime trialStartDate;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    // Current billing period
    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    // Grace period
    @Column(name = "grace_period_end")
    private LocalDateTime gracePeriodEnd;

    // Cancellation
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancel_at_period_end")
    @Builder.Default
    private Boolean cancelAtPeriodEnd = false;

    // Renewal
    @Column(name = "auto_renew")
    @Builder.Default
    private Boolean autoRenew = true;

    // Metadata
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if subscription has access
     */
    public boolean hasAccess() {
        return status.hasAccess();
    }

    /**
     * Check if in trial period
     */
    public boolean isInTrial() {
        if (status != SubscriptionStatus.TRIAL) {
            return false;
        }
        if (trialEndDate == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(trialEndDate);
    }

    /**
     * Check if trial is ending soon (within 3 days)
     */
    public boolean isTrialEndingSoon() {
        if (!isInTrial()) {
            return false;
        }
        return trialEndDate.isBefore(LocalDateTime.now().plusDays(3));
    }

    /**
     * Check if current period has ended
     */
    public boolean isPeriodEnded() {
        if (currentPeriodEnd == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(currentPeriodEnd);
    }

    /**
     * Check if in grace period
     */
    public boolean isInGracePeriod() {
        if (status != SubscriptionStatus.PAST_DUE) {
            return false;
        }
        if (gracePeriodEnd == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(gracePeriodEnd);
    }

    /**
     * Check if grace period has expired
     */
    public boolean isGracePeriodExpired() {
        if (gracePeriodEnd == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(gracePeriodEnd);
    }

    /**
     * Get days until trial ends
     */
    public long getDaysUntilTrialEnds() {
        if (trialEndDate == null) {
            return 0;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), trialEndDate);
        return Math.max(0, days);
    }

    /**
     * Get days until period ends
     */
    public long getDaysUntilPeriodEnds() {
        if (currentPeriodEnd == null) {
            return 0;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), currentPeriodEnd);
        return Math.max(0, days);
    }
}
