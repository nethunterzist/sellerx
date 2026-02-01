package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    private Integer netUnitsSold;        // Net Satış Adedi = Brüt Satış - İade Adedi
    
    // Revenue stats  
    private BigDecimal totalRevenue; // Ciro = gross_amount - total_discount
    
    // Return stats
    private Integer returnCount;
    private BigDecimal returnCost; // iade masrafı (50 * iade sayısı for now)
    
    // Cost stats
    private BigDecimal totalProductCosts; // Ürün maliyetleri toplamı
    
    // Profit stats
    private BigDecimal grossProfit; // Brüt Kar = ciro - ürün maliyetleri
    private BigDecimal netProfit; // Net Kar = brüt kar - komisyon - stopaj - iade maliyeti - giderler
    private BigDecimal profitMargin; // Kar Marjı % = (brüt kar / ciro) * 100
    private BigDecimal vatDifference; // KDV Farkı
    private BigDecimal totalStoppage; // Toplam Stopaj
    private BigDecimal totalEstimatedCommission; // Toplam Tahmini Komisyon
    
    // Items without cost calculation
    private Integer itemsWithoutCost; // Maliyeti olmayan ürün sayısı
    
    // Expense stats
    private Integer totalExpenseNumber; // Toplam masraf kalem sayısı
    private BigDecimal totalExpenseAmount; // Toplam masraf tutarı

    // ============== YENİ ALANLAR (32 Metrik) ==============

    // ============== KESİLEN FATURALAR (Dashboard Kartları için) ==============
    // Toplam kesilen faturalar: REKLAM + CEZA + ULUSLARARASI + DIGER - IADE
    // NOT: KOMISYON ve KARGO faturaları HARİÇ (bunlar sipariş bazlı hesaplanıyor)
    private BigDecimal invoicedDeductions;        // Toplam kesilen faturalar

    // Fatura Kategorileri Detayı (Detay modalı için)
    private BigDecimal invoicedAdvertisingFees;   // REKLAM kategorisi (Reklam Bedeli, Influencer reklamları)
    private BigDecimal invoicedPenaltyFees;       // CEZA kategorisi (Termin Gecikme, Tedarik Edememe, Kusurlu/Eksik/Yanlış)
    private BigDecimal invoicedInternationalFees; // ULUSLARARASI kategorisi (Uluslararası Hizmet, Yurtdışı Operasyon)
    private BigDecimal invoicedOtherFees;         // DIGER kategorisi (Erken Ödeme, Kurumsal Kampanya, Kontör Satış)
    private BigDecimal invoicedRefunds;           // IADE kategorisi (Tazmin, Kargo İtiraz İade) - pozitif değer, toplam'dan düşülür

    // Eski alan - uyumluluk için korunuyor, invoicedAdvertisingFees ile aynı değer
    private BigDecimal invoicedAdvertisingCost;

    // İndirimler & Kuponlar
    private BigDecimal totalSellerDiscount;    // Satıcı indirimi
    private BigDecimal totalPlatformDiscount;  // Platform indirimi (Trendyol)
    private BigDecimal totalCouponDiscount;    // Kupon indirimi

    // Kargo Maliyetleri
    private BigDecimal totalShippingCost;      // Kargo gönderim maliyeti
    private BigDecimal totalShippingIncome;    // Kargo geliri (alıcıdan alınan)

    // Platform Ücretleri (15 kategori - TrendyolStoppage description'dan parse)
    private BigDecimal internationalServiceFee;  // Uluslararası Hizmet Bedeli
    private BigDecimal overseasOperationFee;     // Yurt Dışı Operasyon Bedeli
    private BigDecimal terminDelayFee;           // Termin Gecikme Bedeli
    private BigDecimal platformServiceFee;       // Platform Hizmet Bedeli
    private BigDecimal invoiceCreditFee;         // Fatura Kontör Satış Bedeli
    private BigDecimal unsuppliedFee;            // Tedarik Edememe
    private BigDecimal azOverseasOperationFee;   // AZ-Yurtdışı Operasyon Bedeli
    private BigDecimal azPlatformServiceFee;     // AZ-Platform Hizmet Bedeli
    private BigDecimal packagingServiceFee;      // Paketleme Hizmet Bedeli
    private BigDecimal warehouseServiceFee;      // Depo Hizmet Bedeli
    private BigDecimal callCenterFee;            // Çağrı Merkezi Bedeli
    private BigDecimal photoShootingFee;         // Fotoğraf Çekim Bedeli
    private BigDecimal integrationFee;           // Entegrasyon Bedeli
    private BigDecimal storageServiceFee;        // Depolama Hizmet Bedeli
    private BigDecimal otherPlatformFees;        // Diğer Platform Ücretleri

    // Erken Ödeme Maliyeti (Settlement API'den)
    private BigDecimal earlyPaymentFee;

    // Gider Kategorileri (StoreExpense category bazlı)
    private BigDecimal officeExpenses;       // Ofis giderleri (eski - uyumluluk için)
    private BigDecimal packagingExpenses;    // Ambalaj/Paketleme giderleri (eski - uyumluluk için)
    private BigDecimal accountingExpenses;   // Muhasebe giderleri (eski - uyumluluk için)
    private BigDecimal otherExpenses;        // Diğer giderler (eski - uyumluluk için)

    // Dinamik gider kategorileri - yeni kategoriler otomatik desteklenir
    private Map<String, BigDecimal> expensesByCategory;

    // İade Detayları
    private BigDecimal refundRate;           // İade oranı (%)

    // Net Ciro (Brüt Ciro - İndirimler)
    private BigDecimal netRevenue;

    // ROI (Return on Investment)
    private BigDecimal roi;

    // Detailed data
    private List<OrderDetailDto> orders; // Siparişlerin detayları
    private List<ProductDetailDto> products; // Ürünlerin detayları
    private List<PeriodExpenseDto> expenses; // Dönem masrafları
}
