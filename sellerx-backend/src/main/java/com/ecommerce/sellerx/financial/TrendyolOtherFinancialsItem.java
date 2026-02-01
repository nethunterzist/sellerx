package com.ecommerce.sellerx.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for Trendyol otherfinancials API response items
 * Used for Stoppage, PaymentOrder, DeductionInvoices transaction types
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrendyolOtherFinancialsItem {

    @JsonProperty("id")
    private String id;

    @JsonProperty("transactionDate")
    private Long transactionDate; // Timestamp in milliseconds

    @JsonProperty("transactionType")
    private String transactionType; // "Stoppage", "PaymentOrder", "DeductionInvoices"

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

    @JsonProperty("paymentOrderId")
    private Long paymentOrderId;

    @JsonProperty("paymentDate")
    private Long paymentDate; // Timestamp in milliseconds

    @JsonProperty("invoiceSerialNumber")
    private String invoiceSerialNumber;

    @JsonProperty("sellerId")
    private Long sellerId;

    @JsonProperty("storeId")
    private Long storeId;

    @JsonProperty("storeName")
    private String storeName;

    @JsonProperty("country")
    private String country;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("orderDate")
    private Long orderDate;

    @JsonProperty("shipmentPackageId")
    private Long shipmentPackageId;
}
