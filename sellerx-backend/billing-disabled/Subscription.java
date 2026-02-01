package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User subscription record with lifecycle tracking
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
    @JoinColumn(name = "price_id", nullable = false)
    private SubscriptionPrice price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    // Trial tracking
    @Column(name = "trial_start_date")
    private LocalDateTime trialStartDate;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    // Billing period
    @Column(name = "current_period_start", nullable = false)
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd;

    /**
     * Grace period end for failed payments (3 days from first failure)
     */
    @Column(name = "grace_period_end")
    private LocalDateTime gracePeriodEnd;

    /**
     * iyzico subscription reference for card-on-file payments
     */
    @Column(name = "iyzico_subscription_reference")
    private String iyzicoSubscriptionReference;

    // Cancellation tracking
    @Column(name = "cancel_at_period_end", nullable = false)
    @Builder.Default
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = true;

    // Downgrade scheduling
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "downgrade_to_plan_id")
    private SubscriptionPlan downgradeToPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "downgrade_to_price_id")
    private SubscriptionPrice downgradeToPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
     * Check if user has access to paid features
     */
    public boolean hasAccess() {
        return status.hasAccess();
    }

    /**
     * Check if subscription is in trial period
     */
    public boolean isInTrial() {
        return status == SubscriptionStatus.TRIAL &&
                trialEndDate != null &&
                LocalDateTime.now().isBefore(trialEndDate);
    }

    /**
     * Check if trial has expired
     */
    public boolean isTrialExpired() {
        return trialEndDate != null && LocalDateTime.now().isAfter(trialEndDate);
    }

    /**
     * Check if subscription needs renewal
     */
    public boolean needsRenewal() {
        return autoRenew &&
                !cancelAtPeriodEnd &&
                LocalDateTime.now().isAfter(currentPeriodEnd.minusDays(1));
    }

    /**
     * Check if in grace period
     */
    public boolean isInGracePeriod() {
        return status == SubscriptionStatus.PAST_DUE &&
                gracePeriodEnd != null &&
                LocalDateTime.now().isBefore(gracePeriodEnd);
    }

    /**
     * Check if grace period has expired
     */
    public boolean isGracePeriodExpired() {
        return gracePeriodEnd != null && LocalDateTime.now().isAfter(gracePeriodEnd);
    }

    /**
     * Get days remaining in trial
     */
    public long getTrialDaysRemaining() {
        if (trialEndDate == null) return 0;
        long days = java.time.Duration.between(LocalDateTime.now(), trialEndDate).toDays();
        return Math.max(0, days);
    }

    /**
     * Get days remaining in current period
     */
    public long getPeriodDaysRemaining() {
        long days = java.time.Duration.between(LocalDateTime.now(), currentPeriodEnd).toDays();
        return Math.max(0, days);
    }
}
