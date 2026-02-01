package com.ecommerce.sellerx.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for Trendyol cargo-invoice API response items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrendyolCargoInvoiceItem {

    @JsonProperty("shipmentPackageType")
    private String shipmentPackageType;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("amount")
    private BigDecimal amount; // Actual cargo cost

    @JsonProperty("desi")
    private Integer desi; // Dimensional weight

    @JsonProperty("parcelUniqueId")
    private Long shipmentPackageId;

    @JsonProperty("barcode")
    private String barcode;

    @JsonProperty("productName")
    private String productName;
}
