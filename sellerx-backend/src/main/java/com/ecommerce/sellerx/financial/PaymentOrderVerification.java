package com.ecommerce.sellerx.financial;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Payment Order verification result (Hak Ediş Kontrolü)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderVerification {

    private Long paymentOrderId;
    private LocalDateTime paymentDate;
    private BigDecimal expectedAmount;    // Calculated from settlements
    private BigDecimal actualAmount;      // From PaymentOrder transaction
    private BigDecimal discrepancy;       // expected - actual
    private String status;                // MATCHED, UNDERPAID, OVERPAID, PENDING

    private SettlementBreakdown settlementBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementBreakdown {
        private BigDecimal saleAmount;
        private Integer saleCount;
        private BigDecimal returnAmount;
        private Integer returnCount;
        private BigDecimal discountAmount;
        private BigDecimal couponAmount;
        private BigDecimal commissionAmount;
        private BigDecimal cargoAmount;
        private BigDecimal stoppageAmount;
    }
}
