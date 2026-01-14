package com.ecommerce.sellerx.expenses;

import java.math.BigDecimal;
import java.util.List;

public record StoreExpensesResponse(
    BigDecimal totalExpense,
    List<StoreExpenseDto> expenses
) {}
