package com.ecommerce.sellerx.dashboard.dto;

import java.math.BigDecimal;

/**
 * DTO for deduction invoice breakdown by transaction type.
 * Used for dashboard detail panel to show all invoice types individually.
 *
 * @param transactionType The type of deduction (e.g., "Platform Hizmet Bedeli", "Reklam Bedeli")
 * @param totalDebt Total debit amount for this type (money deducted from seller)
 * @param totalCredit Total credit amount for this type (money refunded to seller)
 */
public record DeductionBreakdownDto(
    String transactionType,
    BigDecimal totalDebt,
    BigDecimal totalCredit
) {}
