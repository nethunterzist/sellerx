package com.ecommerce.sellerx.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class to hold both financial settlements and their summary for an order item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialOrderItemData {
    
    /**
     * The barcode of the order item this financial data belongs to
     */
    private String barcode;
    
    /**
     * All financial settlements/transactions for this order item
     */
    @Builder.Default
    private List<FinancialSettlement> transactions = new ArrayList<>();
    
    /**
     * Calculated financial summary for this order item
     */
    private FinancialOrderItemsTransactionSummary transactionSummary;
}
