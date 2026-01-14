package com.ecommerce.sellerx.dashboard;

import com.ecommerce.sellerx.expenses.ExpenseFrequency;

import java.math.BigDecimal;

public record PeriodExpenseDto(
    String expenseName,
    int expenseQuantity,
    BigDecimal expenseTotal,
    ExpenseFrequency expenseFrequency
) {}
