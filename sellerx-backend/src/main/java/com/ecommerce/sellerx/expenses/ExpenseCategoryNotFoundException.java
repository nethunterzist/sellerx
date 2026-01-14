package com.ecommerce.sellerx.expenses;

public class ExpenseCategoryNotFoundException extends RuntimeException {
    public ExpenseCategoryNotFoundException(String message) {
        super(message);
    }
}
