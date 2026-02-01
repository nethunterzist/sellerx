package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Sale Shortfall Detection result (Eksik Satış Bedeli)
 * Detects when received sale amount is less than expected
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleShortfallResult {

    private Long paymentOrderId;
    private String orderNumber;
    private Long packageNo;
    private LocalDate saleDate;

    // Amount comparison
    private BigDecimal expectedSaleAmount;   // Expected seller revenue from order
    private BigDecimal actualSaleAmount;     // Actual amount received in payment
    private BigDecimal shortfallAmount;      // expected - actual (if positive)

    // Status: SHORTFALL, MATCHED
    private String status;

    // Breakdown for investigation
    private BigDecimal grossAmount;          // Original sale amount before deductions
    private BigDecimal commissionAmount;     // Commission deducted
    private BigDecimal cargoAmount;          // Cargo deducted
    private BigDecimal stoppageAmount;       // Other deductions
    private BigDecimal discountAmount;       // Discounts applied

    // Additional info
    private Integer itemCount;               // Number of items in order
    private String productSummary;           // Brief product description
}
