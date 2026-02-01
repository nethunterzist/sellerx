package com.ecommerce.sellerx.returns;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projection for daily return statistics.
 */
public interface DailyReturnStatsProjection {
    LocalDate getReturnDate();
    Long getReturnCount();
    BigDecimal getTotalLoss();
}
