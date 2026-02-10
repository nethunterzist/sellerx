package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Order Invoice Items
 * Contains all invoice items (cargo, commission, deductions) linked to a specific order.
 * Used to show actual invoiced expenses in order detail panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInvoiceItemsDto {

    private String orderNumber;

    // Kargo fatura kalemleri
    private List<CargoInvoiceItemDto> cargoItems;
    private BigDecimal totalCargoAmount;
    private BigDecimal totalCargoVatAmount;

    // Komisyon kalemleri (financial_transactions'dan)
    private List<CommissionInvoiceItemDto> commissionItems;
    private BigDecimal totalCommissionAmount;
    private BigDecimal totalCommissionVatAmount;

    // Kesinti kalemleri (platform, reklam, ceza vb.)
    private List<InvoiceItemDto> deductionItems;
    private BigDecimal totalDeductionAmount;
    private BigDecimal totalDeductionVatAmount;

    // Toplam fatura gideri (tüm kategoriler)
    private BigDecimal grandTotal;

    // Fatura durumu - en az bir fatura kalemi var mı?
    private boolean hasInvoiceData;
}
