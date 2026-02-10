package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Statistics for a single period (month, week, or day)
 * Used in MultiPeriodStatsResponse for P&L breakdown tables
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodStatsDto {

    /**
     * Human-readable period label
     * Examples: "Aralık 2025", "Hafta 52", "17 Oca"
     */
    private String periodLabel;

    /**
     * Period start date (ISO format: yyyy-MM-dd)
     */
    private String startDate;

    /**
     * Period end date (ISO format: yyyy-MM-dd)
     */
    private String endDate;

    // Order stats
    private Integer totalOrders;
    private Integer totalProductsSold;

    // Revenue stats
    private BigDecimal totalRevenue; // Ciro = gross_amount - total_discount

    // Return stats
    private Integer returnCount;
    private BigDecimal returnCost; // İade masrafı

    // Cost stats
    private BigDecimal totalProductCosts; // Ürün maliyetleri toplamı

    // Profit stats
    private BigDecimal grossProfit; // Brüt Kar = ciro - ürün maliyetleri
    private BigDecimal vatDifference; // KDV Farkı
    private BigDecimal totalStoppage; // Toplam Stopaj
    private BigDecimal totalEstimatedCommission; // Toplam Tahmini Komisyon

    // Net profit = grossProfit - commission - stoppage - expenses
    private BigDecimal netProfit;

    // Margin = (grossProfit / revenue) * 100
    private BigDecimal profitMargin;

    // ROI = (netProfit / productCosts) * 100
    private BigDecimal roi;

    // Items without cost calculation
    private Integer itemsWithoutCost;

    // Expense stats
    private Integer totalExpenseNumber;
    private BigDecimal totalExpenseAmount;

    // Kargo Maliyeti
    private BigDecimal totalShippingCost;

    // Kesilen Faturalar (Invoiced Fees)
    private BigDecimal platformServiceFee;
    private BigDecimal azPlatformServiceFee;
    private BigDecimal invoicedAdvertisingFees;
    private BigDecimal invoicedPenaltyFees;
    private BigDecimal invoicedInternationalFees;
    private BigDecimal invoicedOtherFees;
    private BigDecimal invoicedRefunds;

    // Gider Kategorileri (Expense Categories)
    private Map<String, BigDecimal> expensesByCategory;
}
