package com.ecommerce.sellerx.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Financial summary for an order item's transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialOrderItemsTransactionSummary {
    
    /**
     * Total gross price (sum of all SOLD credits, excluding returns)
     */
    private BigDecimal totalPrice;
    
    /**
     * Total discount amount (sum of all DISCOUNT debts, excluding returns)
     */
    private BigDecimal totalDiscount;
    
    /**
     * Total coupon amount (sum of all COUPON debts, excluding returns)
     */
    private BigDecimal totalCoupon;
    
    /**
     * Net commission amount (SOLD commission - DISCOUNT commission - COUPON commission, excluding returns)
     */
    private BigDecimal totalCommission;
    
    /**
     * Final net price after discounts and coupons (totalPrice - totalDiscount - totalCoupon)
     */
    private BigDecimal finalPrice;
    
    /**
     * Net amount after commission (finalPrice - totalCommission)
     * This represents the actual profit/revenue for the seller
     */
    private BigDecimal netAmount;
    
    /**
     * Number of sold items (excluding returns)
     */
    private Integer soldQuantity;
    
    /**
     * Number of returned items
     */
    private Integer returnedQuantity;
}
