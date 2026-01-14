package com.ecommerce.sellerx.expenses;

public enum ExpenseFrequency {
    DAILY("Günlük"),
    WEEKLY("Haftalık"),
    MONTHLY("Aylık"),
    YEARLY("Yıllık"),
    ONE_TIME("Tek Seferlik");
    
    private final String displayName;
    
    ExpenseFrequency(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
