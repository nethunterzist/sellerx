package com.ecommerce.sellerx.financial;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolFinancialSettlementItem {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("transactionDate")
    private Long transactionDate; // Timestamp in milliseconds
    
    @JsonProperty("barcode")
    private String barcode;
    
    @JsonProperty("transactionType")
    private String transactionType;
    
    @JsonProperty("receiptId")
    private Long receiptId;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("debt")
    private BigDecimal debt;
    
    @JsonProperty("credit")
    private BigDecimal credit;
    
    @JsonProperty("paymentPeriod")
    private Integer paymentPeriod;
    
    @JsonProperty("commissionRate")
    private BigDecimal commissionRate;
    
    @JsonProperty("commissionAmount")
    private BigDecimal commissionAmount;
    
    @JsonProperty("commissionInvoiceSerialNumber")
    private String commissionInvoiceSerialNumber;
    
    @JsonProperty("sellerRevenue")
    private BigDecimal sellerRevenue;
    
    @JsonProperty("orderNumber")
    private String orderNumber;
    
    @JsonProperty("orderDate")
    private Long orderDate; // Timestamp in milliseconds
    
    @JsonProperty("paymentOrderId")
    private Long paymentOrderId;
    
    @JsonProperty("paymentDate")
    private Long paymentDate; // Timestamp in milliseconds
    
    @JsonProperty("sellerId")
    private Long sellerId;
    
    @JsonProperty("storeId")
    private Long storeId;
    
    @JsonProperty("storeName")
    private String storeName;
    
    @JsonProperty("storeAddress")
    private String storeAddress;
    
    @JsonProperty("country")
    private String country;
    
    @JsonProperty("affiliate")
    private String affiliate;
    
    @JsonProperty("shipmentPackageId")
    private Long shipmentPackageId;
}
