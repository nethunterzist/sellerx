package com.ecommerce.sellerx.expenses;

import java.math.BigDecimal;

/**
 * Projection interface for category-grouped expense sums.
 */
public interface CategoryExpenseSum {
    String getCategoryName();
    BigDecimal getTotalAmount();
}
