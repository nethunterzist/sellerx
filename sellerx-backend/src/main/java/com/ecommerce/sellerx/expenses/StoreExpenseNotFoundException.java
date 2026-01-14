package com.ecommerce.sellerx.expenses;

public class StoreExpenseNotFoundException extends RuntimeException {
    public StoreExpenseNotFoundException(String message) {
        super(message);
    }
}
