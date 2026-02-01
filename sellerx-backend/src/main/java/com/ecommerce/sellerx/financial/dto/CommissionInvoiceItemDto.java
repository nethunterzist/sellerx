package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Commission Invoice Item
 * Shows individual order commission details within a commission invoice
 * Data comes from trendyol_orders.financial_transactions JSONB column
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionInvoiceItemDto {

    private String orderNumber;
    private LocalDateTime orderDate;
    private String barcode;
    private String productName;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal sellerRevenue;
    private BigDecimal trendyolRevenue;
    private String transactionType;

    // Yeni alanlar - Trendyol Excel export uyumu için
    private String recordId;              // Kayıt No (settlement id veya receiptId)
    private LocalDateTime transactionDate; // İşlem Tarihi
    private Integer paymentPeriod;        // Vade Süresi (gün)
    private LocalDateTime paymentDate;    // Vade Tarihi
    private BigDecimal totalAmount;       // Toplam Tutar (credit)
}
