package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Return Commission Refund Detection result (Eksik İade Komisyon İadesi)
 * Detects missing commission refunds on returned orders
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCommissionRefundResult {

    private Long paymentOrderId;
    private String orderNumber;
    private Long packageNo;
    private LocalDate returnDate;

    // Return and commission info
    private BigDecimal returnAmount;       // Total return amount
    private BigDecimal commissionRate;     // Commission rate applied
    private BigDecimal expectedRefund;     // returnAmount * commissionRate / 100
    private BigDecimal actualRefund;       // Refund found in stoppages
    private BigDecimal missingRefund;      // expected - actual (if positive)

    // Status: MISSING_REFUND, PARTIAL_REFUND, REFUNDED
    private String status;

    // Additional breakdown
    private Integer returnCount;           // Number of returned items
    private String productName;            // Product info if single return
    private String barcode;
}
