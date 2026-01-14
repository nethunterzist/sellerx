package com.ecommerce.sellerx.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Financial summary for an entire order's transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialOrderTransactionSummary {
    
    /**
     * Total gross price for all order items (sum of all SOLD credits, excluding returns)
     */
    private BigDecimal totalPrice;
    
    /**
     * Total discount amount for all order items (sum of all DISCOUNT debts, excluding returns)
     */
    private BigDecimal totalDiscount;
    
    /**
     * Total coupon amount for all order items (sum of all COUPON debts, excluding returns)
     */
    private BigDecimal totalCoupon;
    
    /**
     * Net commission amount for all order items (SOLD commission - DISCOUNT commission - COUPON commission, excluding returns)
     */
    private BigDecimal totalCommission;
    
    /**
     * Final net price after discounts and coupons for all order items
     */
    private BigDecimal finalPrice;
    
    /**
     * Net amount after commission for all order items (finalPrice - totalCommission)
     * This represents the actual profit/revenue for the seller
     */
    private BigDecimal netAmount;
    
    /**
     * Total number of sold items across all order items (excluding returns)
     */
    private Integer totalSoldQuantity;
    
    /**
     * Total number of returned items across all order items
     */
    private Integer totalReturnedQuantity;
    
    /**
     * Number of unique product types in this order
     */
    private Integer uniqueProductCount;
}
