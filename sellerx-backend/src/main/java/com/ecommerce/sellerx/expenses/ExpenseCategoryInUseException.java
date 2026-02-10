package com.ecommerce.sellerx.expenses;

public class ExpenseCategoryInUseException extends RuntimeException {

    private final long expenseCount;

    public ExpenseCategoryInUseException(String message, long expenseCount) {
        super(message);
        this.expenseCount = expenseCount;
    }

    public long getExpenseCount() {
        return expenseCount;
    }
}
