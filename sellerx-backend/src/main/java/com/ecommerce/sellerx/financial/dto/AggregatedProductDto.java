package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for aggregated product data from invoice items.
 * Groups invoice items by barcode (SKU) and shows totals.
 * Used in "Ürünler" tab when user clicks on KARGO or KOMISYON invoice category cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedProductDto {

    /**
     * Product barcode (SKU)
     */
    private String barcode;

    /**
     * Product name (from TrendyolProduct or rawData)
     */
    private String productName;

    /**
     * Product image URL (from TrendyolProduct)
     */
    private String productImageUrl;

    /**
     * Product URL on Trendyol (from TrendyolProduct)
     */
    private String productUrl;

    /**
     * Total quantity (number of invoice items/shipments for this product)
     */
    private int totalQuantity;

    /**
     * Total amount deducted/charged for this product
     */
    private BigDecimal totalAmount;

    /**
     * Total VAT amount for this product
     */
    private BigDecimal totalVatAmount;

    /**
     * Number of distinct invoices containing this product
     */
    private int invoiceCount;

    /**
     * Total desi (volumetric weight) - only for KARGO invoices
     */
    private Integer totalDesi;

    /**
     * Total commission amount - only for KOMISYON invoices
     */
    private BigDecimal totalCommission;

    // ================================================================================
    // Commission breakdown fields (only for KOMISYON category)
    // These show the breakdown before net calculation: Net = Satış - İndirim - Kupon
    // ================================================================================

    /**
     * Total sale commission (Satış) - positive, goes to Trendyol
     */
    private BigDecimal saleCommission;

    /**
     * Total discount commission (İndirim) - stored as positive, but should be subtracted
     */
    private BigDecimal discountCommission;

    /**
     * Total coupon commission (Kupon) - stored as positive, but should be subtracted
     */
    private BigDecimal couponCommission;
}
