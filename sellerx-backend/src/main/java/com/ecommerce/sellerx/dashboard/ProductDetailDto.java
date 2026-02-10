package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailDto {

    private String productName;
    private String barcode; // ürün barkodu
    private String brand; // marka
    private String image; // ürün görseli URL
    private String productUrl; // Trendyol ürün sayfası URL'i
    private Integer stock; // stok adedi
    private Integer totalSoldQuantity; // brüt satış adedi
    private Integer returnQuantity; // iade adedi
    private BigDecimal revenue; // ciro (brüt)
    private BigDecimal grossProfit; // brüt kar
    private BigDecimal estimatedCommission; // tahmini komisyon

    // ============== YENİ ALANLAR (Ürün Bazlı Metrikler) ==============

    // İndirimler
    private BigDecimal sellerDiscount;       // Satıcı indirimi
    private BigDecimal platformDiscount;     // Platform indirimi
    private BigDecimal couponDiscount;       // Kupon indirimi
    private BigDecimal totalDiscount;        // Toplam indirim

    // Net Ciro
    private BigDecimal netRevenue;           // Net ciro = brüt ciro - indirimler

    // Maliyetler
    private BigDecimal productCost;          // Ürün maliyeti (FIFO)
    private BigDecimal shippingCost;         // Kargo maliyeti (sipariş bazlı)

    // İade
    private BigDecimal refundCost;           // İade maliyeti
    private BigDecimal refundRate;           // İade oranı (%)

    // Net Kar ve Metrikler
    private BigDecimal netProfit;            // Net kar
    private BigDecimal profitMargin;         // Kar marjı (%)
    private BigDecimal roi;                  // ROI (%)

    // Sipariş Sayısı
    private Integer orderCount;              // Bu ürünü içeren sipariş sayısı

    // ============== REKLAM METRİKLERİ (Excel C23, C24) ==============
    private BigDecimal cpc;                  // Cost Per Click (TL)
    private BigDecimal cvr;                  // Conversion Rate (örn: 0.018 = %1.8)
    private BigDecimal advertisingCostPerSale; // Reklam Maliyeti = CPC / CVR
    private BigDecimal acos;                 // ACOS = (advertisingCostPerSale / salePrice) * 100
    private BigDecimal totalAdvertisingCost; // Toplam reklam maliyeti = advertisingCostPerSale × satış adedi
}
