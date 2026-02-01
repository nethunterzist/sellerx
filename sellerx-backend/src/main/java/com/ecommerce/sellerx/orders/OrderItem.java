package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.financial.FinancialSettlement;
import com.ecommerce.sellerx.financial.FinancialOrderItemsTransactionSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    @JsonProperty("barcode")
    private String barcode;
    
    @JsonProperty("productName")
    private String productName;
    
    @JsonProperty("quantity")
    private Integer quantity;
    
    @JsonProperty("unitPriceOrder")
    private BigDecimal unitPriceOrder; // This is "amount" from Trendyol API
    
    @JsonProperty("unitPriceDiscount")
    private BigDecimal unitPriceDiscount; // This is "discount" from Trendyol API
    
    @JsonProperty("unitPriceTyDiscount")
    private BigDecimal unitPriceTyDiscount; // This is "tyDiscount" from Trendyol API
    
    @JsonProperty("vatBaseAmount")
    private BigDecimal vatBaseAmount;

    // VAT fields for KDV Mahsuplasmasi
    @JsonProperty("saleVatRate")
    private Integer saleVatRate; // VAT rate from product (e.g., 1, 10, 20)

    @JsonProperty("saleVatAmount")
    private BigDecimal saleVatAmount; // Calculated: price * vatRate / (100 + vatRate)

    @JsonProperty("price")
    private BigDecimal price; // This is the actual price after discounts
    
    // Additional fields that we'll get from our trendyol_products table
    @JsonProperty("cost")
    private BigDecimal cost; // Product cost from our system
    
    @JsonProperty("costVat")
    private Integer costVat; // VAT rate for cost calculation
    
    @JsonProperty("stockDate")
    private LocalDate stockDate; // Which stock date this order item was sourced from
    
    // ESTIMATED values from order data (these may not be the final values)
    @JsonProperty("estimatedCommissionRate")
    private BigDecimal estimatedCommissionRate; // Commission rate percentage from product (estimated)
    
    @JsonProperty("estimatedShippingVolumeWeight")
    private BigDecimal estimatedShippingVolumeWeight; // Shipping volume weight from product (estimated)
    
    @JsonProperty("unitEstimatedCommission")
    private BigDecimal unitEstimatedCommission; // Calculated as (unitPriceOrder - unitPriceDiscount) * commissionRate / 100
    
    // Settlement transactions for this specific product
    @JsonProperty("transactions")
    @Builder.Default
    private List<FinancialSettlement> transactions = new java.util.ArrayList<>();
    
    // Financial summary of all transactions
    @JsonProperty("transactionSummary")
    private FinancialOrderItemsTransactionSummary transactionSummary;
}
