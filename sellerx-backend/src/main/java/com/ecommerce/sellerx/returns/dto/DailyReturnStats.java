package com.ecommerce.sellerx.returns.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DailyReturnStats {
    private String date;
    private int returnCount;
    private BigDecimal totalLoss;
}
