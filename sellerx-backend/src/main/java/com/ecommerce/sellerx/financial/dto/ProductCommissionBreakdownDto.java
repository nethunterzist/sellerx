package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for product commission breakdown response.
 * Shows commission breakdown by transaction type for a specific product (barcode).
 * Used in the "Detay" panel for KOMISYON Ürünler tab.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCommissionBreakdownDto {

    /**
     * Product barcode (SKU)
     */
    private String barcode;

    /**
     * Product name
     */
    private String productName;

    /**
     * Product image URL
     */
    private String productImageUrl;

    /**
     * Product URL on Trendyol
     */
    private String productUrl;

    /**
     * Total commission amount for this product
     */
    private BigDecimal totalCommission;

    /**
     * Total VAT amount for this product
     */
    private BigDecimal totalVatAmount;

    /**
     * Total number of items/transactions for this product
     */
    private int totalItemCount;

    /**
     * Number of distinct orders for this product
     */
    private int orderCount;

    /**
     * Commission breakdown by transaction type
     */
    private List<TransactionTypeBreakdownDto> breakdown;

    // ================================================================================
    // Individual transaction type totals
    // Net Commission = Satış - İndirim - Kupon (İade excluded from commission invoice)
    // ================================================================================

    /**
     * Total sale commission (Satış) - positive, goes to Trendyol
     */
    private BigDecimal saleCommission;

    /**
     * Total discount commission (İndirim) - stored as positive, but SUBTRACTED in net calculation
     */
    private BigDecimal discountCommission;

    /**
     * Total coupon commission (Kupon) - stored as positive, but SUBTRACTED in net calculation
     */
    private BigDecimal couponCommission;

    /**
     * Total return commission (İade) - tracked but EXCLUDED from commission invoice calculation
     * Returns are handled in a separate invoice/process in Trendyol
     */
    private BigDecimal returnCommission;
}
