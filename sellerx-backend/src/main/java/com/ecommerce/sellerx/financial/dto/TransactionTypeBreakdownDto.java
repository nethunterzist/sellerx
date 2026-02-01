package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for transaction type breakdown item.
 * Shows commission breakdown by transaction type (Satış, Kupon, İndirim, İade).
 * Used in the "Detay" panel for KOMISYON Ürünler tab.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTypeBreakdownDto {

    /**
     * Transaction type code from Trendyol (e.g., "Sale", "Coupon", "Discount", "Return")
     */
    private String transactionType;

    /**
     * Turkish display name for the transaction type (e.g., "Satış", "Kupon", "İndirim", "İade")
     */
    private String transactionTypeDisplay;

    /**
     * Number of invoice items for this transaction type
     */
    private int itemCount;

    /**
     * Total commission amount for this transaction type
     */
    private BigDecimal totalCommission;

    /**
     * Total VAT amount for this transaction type
     */
    private BigDecimal totalVatAmount;

    /**
     * Average commission rate for this transaction type
     */
    private BigDecimal averageCommissionRate;
}
