package com.ecommerce.sellerx.billing.dto;

import com.ecommerce.sellerx.billing.BillingCycle;
import com.ecommerce.sellerx.billing.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription DTO for API responses
 */
@Data
@Builder
public class SubscriptionDto {

    private UUID id;
    private Long userId;

    // Plan info
    private String planCode;
    private String planName;
    private Integer maxStores;

    // Status
    private SubscriptionStatus status;
    private BillingCycle billingCycle;

    // Pricing
    private BigDecimal price;
    private String currency;

    // Dates
    private LocalDateTime trialEndDate;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;

    // Flags
    private Boolean cancelAtPeriodEnd;
    private Boolean autoRenew;

    // Downgrade info
    private Boolean hasDowngradeScheduled;
    private String downgradeToPlanCode;

    // Computed fields
    public boolean isInTrial() {
        return status == SubscriptionStatus.TRIAL;
    }

    public boolean hasAccess() {
        return status.hasAccess();
    }

    public long getDaysRemaining() {
        if (currentPeriodEnd == null) return 0;
        long days = java.time.Duration.between(LocalDateTime.now(), currentPeriodEnd).toDays();
        return Math.max(0, days);
    }

    public long getTrialDaysRemaining() {
        if (trialEndDate == null) return 0;
        long days = java.time.Duration.between(LocalDateTime.now(), trialEndDate).toDays();
        return Math.max(0, days);
    }
}
