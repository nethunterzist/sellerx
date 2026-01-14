package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDto {
    
    // Period info
    private String period; // "today", "yesterday", "thisMonth", "lastMonth"
    
    // Order stats
    private Integer totalOrders;
    private Integer totalProductsSold;
    
    // Revenue stats  
    private BigDecimal totalRevenue; // Ciro = gross_amount - total_discount
    
    // Return stats
    private Integer returnCount;
    private BigDecimal returnCost; // iade masrafı (50 * iade sayısı for now)
    
    // Cost stats
    private BigDecimal totalProductCosts; // Ürün maliyetleri toplamı
    
    // Profit stats
    private BigDecimal grossProfit; // Brüt Kar = ciro - ürün maliyetleri
    private BigDecimal vatDifference; // KDV Farkı
    private BigDecimal totalStoppage; // Toplam Stopaj
    private BigDecimal totalEstimatedCommission; // Toplam Tahmini Komisyon
    
    // Items without cost calculation
    private Integer itemsWithoutCost; // Maliyeti olmayan ürün sayısı
    
    // Expense stats
    private Integer totalExpenseNumber; // Toplam masraf kalem sayısı
    private BigDecimal totalExpenseAmount; // Toplam masraf tutarı
    
    // Detailed data
    private List<OrderDetailDto> orders; // Siparişlerin detayları
    private List<ProductDetailDto> products; // Ürünlerin detayları
    private List<PeriodExpenseDto> expenses; // Dönem masrafları
}
