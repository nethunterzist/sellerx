package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for Invoice Summary Response
 * Contains all invoice type stats and totals for a store
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSummaryDto {

    private String storeId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    // Totals
    private BigDecimal totalDeductions;
    private BigDecimal totalRefunds;
    private BigDecimal netAmount;
    private long totalInvoiceCount;

    // By type breakdown
    private List<InvoiceTypeStatsDto> invoicesByType;

    // By category breakdown
    private List<CategorySummaryDto> invoicesByCategory;

    // Purchase VAT (Alış KDV'si) for KDV page — based on stock entry date
    private PurchaseVatDto purchaseVat;

    // Sales VAT (Satış KDV'si) for KDV page
    private SalesVatDto salesVat;

    // Stoppage (Stopaj/Tevkifat) summary
    private BigDecimal totalStoppageAmount;
    private int stoppageCount;

    /**
     * Nested DTO for category summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummaryDto {
        private String category;
        private long invoiceCount;
        private BigDecimal totalAmount;
        private BigDecimal totalVatAmount;
    }

    /**
     * Calculate net amount (refunds - deductions)
     */
    public void calculateNetAmount() {
        if (totalDeductions != null && totalRefunds != null) {
            this.netAmount = totalRefunds.subtract(totalDeductions);
        }
    }
}
