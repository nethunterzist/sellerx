package com.ecommerce.sellerx.billing;

/**
 * Billing cycle options for subscriptions
 */
public enum BillingCycle {
    MONTHLY("Aylık", 1),
    QUARTERLY("3 Aylık", 3),
    SEMIANNUAL("6 Aylık", 6);

    private final String displayName;
    private final int months;

    BillingCycle(String displayName, int months) {
        this.displayName = displayName;
        this.months = months;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMonths() {
        return months;
    }
}
