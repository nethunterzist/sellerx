package com.ecommerce.sellerx.billing;

import lombok.Getter;

/**
 * Billing cycle options
 */
@Getter
public enum BillingCycle {
    MONTHLY(1, "Aylık"),
    QUARTERLY(3, "3 Aylık"),
    SEMIANNUAL(6, "6 Aylık");

    private final int months;
    private final String displayName;

    BillingCycle(int months, String displayName) {
        this.months = months;
        this.displayName = displayName;
    }
}
