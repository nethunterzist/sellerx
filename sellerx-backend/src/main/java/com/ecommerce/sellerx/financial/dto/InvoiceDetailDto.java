package com.ecommerce.sellerx.financial.dto;

import com.ecommerce.sellerx.financial.TrendyolInvoice;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Invoice Detail
 * Used in the detail table when a user clicks on an invoice type card
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailDto {

    private UUID id;
    private String invoiceNumber;
    private String invoiceType;
    private String invoiceTypeCode;
    private String invoiceCategory;
    private LocalDateTime invoiceDate;
    private BigDecimal amount;
    private BigDecimal vatAmount;
    private BigDecimal vatRate;
    private BigDecimal baseAmount;
    @JsonProperty("isDeduction")
    private boolean isDeduction;
    private String orderNumber;
    private Long shipmentPackageId;
    private Long paymentOrderId;
    private String barcode;
    private String productName;
    private BigDecimal desi;
    private String description;
    private Map<String, Object> details;

    /**
     * Convert from entity to DTO
     */
    public static InvoiceDetailDto fromEntity(TrendyolInvoice invoice) {
        return InvoiceDetailDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceType(invoice.getInvoiceType())
                .invoiceTypeCode(invoice.getInvoiceTypeCode())
                .invoiceCategory(invoice.getInvoiceCategory())
                .invoiceDate(invoice.getInvoiceDate())
                .amount(invoice.getAmount())
                .vatAmount(invoice.getVatAmount())
                .vatRate(invoice.getVatRate())
                .baseAmount(invoice.getBaseAmount())
                .isDeduction(invoice.getIsDeduction())
                .orderNumber(invoice.getOrderNumber())
                .shipmentPackageId(invoice.getShipmentPackageId())
                .paymentOrderId(invoice.getPaymentOrderId())
                .barcode(invoice.getBarcode())
                .productName(invoice.getProductName())
                .desi(invoice.getDesi())
                .description(invoice.getDescription())
                .details(invoice.getDetails())
                .build();
    }
}
