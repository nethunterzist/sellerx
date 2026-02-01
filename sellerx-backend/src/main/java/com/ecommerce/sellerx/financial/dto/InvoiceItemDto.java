package com.ecommerce.sellerx.financial.dto;

import com.ecommerce.sellerx.financial.TrendyolDeductionInvoice;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for generic Invoice Item
 * Shows individual items within an invoice (for CEZA, KOMISYON, etc. types)
 * Similar to CargoInvoiceItemDto but for non-cargo invoices
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDto {

    private UUID id;
    private String invoiceSerialNumber;
    private String orderNumber;
    private Long shipmentPackageId;
    private Long paymentOrderId;
    private BigDecimal amount;
    private BigDecimal vatAmount;
    private Integer vatRate;
    private LocalDateTime transactionDate;
    private String transactionType;
    private String description;

    private LocalDateTime createdAt;

    // Default VAT rate for most deductions in Turkey
    private static final BigDecimal DEFAULT_VAT_RATE = BigDecimal.valueOf(20);

    /**
     * Convert from TrendyolDeductionInvoice entity to DTO
     */
    public static InvoiceItemDto fromEntity(TrendyolDeductionInvoice invoice) {
        BigDecimal amount = invoice.getDebt() != null ? invoice.getDebt() : BigDecimal.ZERO;
        boolean isCredit = invoice.getCredit() != null && invoice.getCredit().compareTo(BigDecimal.ZERO) > 0;
        if (isCredit) {
            amount = invoice.getCredit();
        }

        // Refund types (IADE) have 0% VAT - don't calculate VAT for them
        // Other deductions have 20% VAT included
        BigDecimal vatAmount;
        int vatRate;
        if (invoice.isRefundType()) {
            vatAmount = BigDecimal.ZERO;
            vatRate = 0;
        } else {
            vatAmount = calculateVatAmount(amount);
            vatRate = DEFAULT_VAT_RATE.intValue();
        }

        return InvoiceItemDto.builder()
                .id(invoice.getId())
                .invoiceSerialNumber(invoice.getInvoiceSerialNumber())
                .orderNumber(invoice.getOrderNumber())
                .shipmentPackageId(invoice.getShipmentPackageId())
                .paymentOrderId(invoice.getPaymentOrderId())
                .amount(amount)
                .vatAmount(vatAmount)
                .vatRate(vatRate)
                .transactionDate(invoice.getTransactionDate())
                .transactionType(invoice.getTransactionType())
                .description(invoice.getDescription())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    /**
     * Calculate VAT amount from total amount (VAT included)
     * Formula: vatAmount = amount * vatRate / (100 + vatRate)
     */
    private static BigDecimal calculateVatAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // amount = base + vat, so vat = amount * rate / (100 + rate)
        return amount.multiply(DEFAULT_VAT_RATE)
                .divide(BigDecimal.valueOf(100).add(DEFAULT_VAT_RATE), 2, RoundingMode.HALF_UP);
    }
}
