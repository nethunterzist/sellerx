package com.ecommerce.sellerx.currency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for TCMB exchange rates response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcmbRates {
    private BigDecimal usdTry;
    private BigDecimal eurTry;
}
