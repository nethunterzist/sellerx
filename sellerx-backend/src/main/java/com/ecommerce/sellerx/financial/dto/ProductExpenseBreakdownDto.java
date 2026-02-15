package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for product expense breakdown.
 * Shows all expenses related to a specific product (barcode) within a date range.
 * Expenses are categorized by type: platform fees, penalties, international, other.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductExpenseBreakdownDto {

    /**
     * Product barcode (SKU)
     */
    private String barcode;

    /**
     * Product name (if available)
     */
    private String productName;

    /**
     * Product image URL
     */
    private String productImageUrl;

    // ================================================================================
    // Platform Hizmet Bedeli (Platform Service Fees)
    // ================================================================================

    /**
     * Total platform service fee for this product
     */
    @Builder.Default
    private BigDecimal platformServiceFee = BigDecimal.ZERO;

    /**
     * Number of platform service fee transactions
     */
    @Builder.Default
    private int platformServiceFeeCount = 0;

    // ================================================================================
    // Uluslararası Kargo (International Shipping)
    // ================================================================================

    /**
     * Total international shipping fee for this product
     */
    @Builder.Default
    private BigDecimal internationalShippingFee = BigDecimal.ZERO;

    /**
     * Number of international shipping transactions
     */
    @Builder.Default
    private int internationalShippingCount = 0;

    // ================================================================================
    // Cezalar (Penalties)
    // ================================================================================

    /**
     * Total penalty fees for this product
     */
    @Builder.Default
    private BigDecimal penaltyFee = BigDecimal.ZERO;

    /**
     * Number of penalty transactions
     */
    @Builder.Default
    private int penaltyCount = 0;

    /**
     * Breakdown of penalties by type
     */
    private List<ExpenseItemDto> penaltyItems;

    // ================================================================================
    // Diğer Giderler (Other Expenses)
    // ================================================================================

    /**
     * Total other expenses for this product
     */
    @Builder.Default
    private BigDecimal otherExpenses = BigDecimal.ZERO;

    /**
     * Number of other expense transactions
     */
    @Builder.Default
    private int otherExpenseCount = 0;

    /**
     * Breakdown of other expenses by type
     */
    private List<ExpenseItemDto> otherExpenseItems;

    // ================================================================================
    // Toplam (Totals)
    // ================================================================================

    /**
     * Grand total of all expenses for this product
     */
    @Builder.Default
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    /**
     * Total VAT amount (calculated as 20% of expenses)
     */
    @Builder.Default
    private BigDecimal totalVatAmount = BigDecimal.ZERO;

    /**
     * Total number of expense transactions
     */
    @Builder.Default
    private int totalTransactionCount = 0;

    /**
     * Whether any expense data was found
     */
    @Builder.Default
    private boolean hasExpenseData = false;

    /**
     * DTO for individual expense item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseItemDto {
        private String transactionType;
        private String description;
        private BigDecimal amount;
        private BigDecimal vatAmount;
        private String orderNumber;
        private String invoiceSerialNumber;
    }
}
