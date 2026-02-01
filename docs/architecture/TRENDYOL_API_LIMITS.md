# Trendyol API Date Range Limits

Bu döküman, Trendyol API'lerinin tarih aralığı limitlerini açıklar. Bu limitler test edilerek belirlenmiştir (Ocak 2026).

## API Limit Özeti

| API | Endpoint | Tarih Limiti | Chunking Gerekli mi? | Notlar |
|-----|----------|--------------|---------------------|--------|
| **Orders** | `/integration/order/sellers/{sellerId}/orders` | **3 ay** (Trendyol limiti) | Evet (15 gün) | Değiştirilemez - Trendyol'un hard limiti |
| **PaymentOrders** | `/integration/finance/che/sellers/{sellerId}/otherfinancials?transactionType=PaymentOrder` | **12+ ay** | Evet (15 gün) | Tarih aralığı max 15 gün olmalı |
| **Stoppages** | `/integration/finance/che/sellers/{sellerId}/otherfinancials?transactionType=Stoppage` | **12+ ay** | Evet (15 gün) | Tarih aralığı max 15 gün olmalı |
| **Claims (İadeler)** | `/integration/order/sellers/{sellerId}/claims` | **12+ ay** | Hayır | Chunking gerekmez |
| **Products** | `/integration/sellers/{sellerId}/products` | **Limit yok** | Hayır | Tüm ürünler çekilir |
| **Q&A** | `/integration/sellers/{sellerId}/questions` | **Test edilemedi** | ? | API "Service Unavailable" döndü |
| **Settlements** | `/integration/finance/che/sellers/{sellerId}/settlements` | **12+ ay** | Evet (15 gün) | **KRİTİK**: Order-level detayları içerir! |

## 🎯 KRİTİK KEŞİF: Settlements API ile Tarihi Sipariş Verisi

**Problem**: Orders API sadece 3 ay geriye veri veriyor.

**Çözüm**: Settlements API, sipariş seviyesinde detayları içeriyor ve 12+ ay geriye gidebiliyor!

### Settlements API Veri İçeriği

```json
{
  "orderNumber": "10038878584",      // Sipariş numarası
  "orderDate": 1741071697682,        // Sipariş tarihi (epoch)
  "barcode": "8809751119168",        // Ürün barkodu
  "credit": 999.9,                   // Satış tutarı (TL)
  "commissionRate": 15.4,            // GERÇEK komisyon oranı (%)
  "commissionAmount": 153.98,        // Kesilen komisyon tutarı
  "sellerRevenue": 845.92,           // Net gelir
  "shipmentPackageId": 2788563033,   // Paket ID
  "paymentOrderId": 48205565         // Hakediş referansı
}
```

### Desteklenen Transaction Tipleri

| TransactionType | Açıklama | Test Sonucu (6 ay geriye, 14 gün) |
|-----------------|----------|-----------------------------------|
| **Sale** | Satış kayıtları | 1618 kayıt ✅ |
| **Return** | İade kayıtları | 31 kayıt ✅ |
| **Discount** | İndirim bilgileri | 407 kayıt ✅ |
| **Coupon** | Kupon kullanımları | 91 kayıt ✅ |

### Settlements API Avantajları

1. **Gerçek Komisyon Oranları**: Tahmini değil, Trendyol'un uyguladığı gerçek oranlar
2. **Tarihi Veri**: Mağaza açılışından itibaren tüm veriler erişilebilir
3. **Net Gelir**: `sellerRevenue` alanı doğrudan net geliri verir
4. **Order-Level Detay**: Her satış/iade için sipariş numarası ve tarih

### Kullanım Stratejisi

```
┌─────────────────────────────────────────────────────────────┐
│                    VERİ KATMANI MİMARİSİ                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Son 3 Ay: Orders API (detaylı sipariş bilgisi)             │
│  ├── Müşteri adresi, ürün varyantları, kargo durumu         │
│  └── Paket takibi, sipariş durumu değişiklikleri            │
│                                                              │
│  3+ Ay Önce: Settlements API (finansal sipariş özeti)       │
│  ├── Sipariş numarası, tarih, ürün barkodu                  │
│  ├── Gerçek komisyon oranı ve tutarı                        │
│  └── Net gelir (sellerRevenue)                              │
│                                                              │
│  Karşılaştırma: Settlement verisi + Orders verisi           │
│  └── Son 3 ayda tahmin edilen komisyonları düzelt           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Sistem Varsayılan Ayarları

### Store Onboarding (İlk Senkronizasyon)
Yeni bir mağaza bağlandığında aşağıdaki tarih aralıkları kullanılır:

| Veri Tipi | Tarih Aralığı | Dosya |
|-----------|---------------|-------|
| Orders | Son 3 ay (API limiti) | `TrendyolOrderService.java:63` |
| PaymentOrders | Son 12 ay | `TrendyolFinancialSettlementService.java` |
| **Settlements** | **Mağaza açılışından itibaren** | `TODO: Implement` |
| Claims | Son 12 ay (365 gün) | `TrendyolClaimsService.java:66` |
| Stoppages | Son 12 ay | `TrendyolOtherFinancialsService.java` |
| Products | Tümü | `TrendyolProductService.java` |
| Q&A | Son 14 gün | `TrendyolQaService.java` |

### Scheduled Jobs (Günlük Senkronizasyon)
| Job | Sıklık | Tarih Aralığı | Dosya |
|-----|--------|---------------|-------|
| Order Sync | Her saat | Son 2 saat | `TrendyolOrderScheduledService.java` |
| Financial Sync | Günlük 07:00 | Son 12 ay | `TrendyolOrderScheduledService.java` |

## API Chunking Kuralları

### OtherFinancials API (PaymentOrders, Stoppages)
Trendyol OtherFinancials API'si maksimum **15 günlük** tarih aralığı kabul eder.

```java
// Doğru - 14 günlük chunk'lar kullan
LocalDate chunkStart = startDate;
while (chunkStart.isBefore(endDate)) {
    LocalDate chunkEnd = chunkStart.plusDays(14);
    if (chunkEnd.isAfter(endDate)) {
        chunkEnd = endDate;
    }
    // API çağrısı yap
    chunkStart = chunkEnd.plusDays(1);
}
```

### Orders API
Trendyol Orders API'si de **15 günlük** chunk'lar gerektirir.

## Test Sonuçları (Ocak 2026)

### Orders API Testi
```
3 ay geriye: 412 sonuç ✅
4 ay geriye: 0 sonuç ❌
```
**Sonuç**: Trendyol Orders API sadece son 3 aylık veriyi döndürür.

### PaymentOrders API Testi
```
3 ay geriye: Veri var ✅
6 ay geriye: Veri var ✅
12 ay geriye: 35 sonuç ✅
```
**Sonuç**: PaymentOrders API 12+ ay veri destekler.

### Claims API Testi
```
3 ay geriye: 123 sonuç ✅
6 ay geriye: 209 sonuç ✅
12 ay geriye: 339 sonuç ✅
```
**Sonuç**: Claims API 12+ ay veri destekler, chunking gerekmez.

## Kritik Bug Düzeltmeleri (Ocak 2026)

1. **PaymentOrders**: 3 ay → 12 ay değiştirildi
2. **Stoppages**: 15 günlük chunking eklendi
3. **Claims**: 30 gün → 365 gün değiştirildi

### Settlements API Testi
```
6 ay geriye (Sale): 1618 sonuç ✅
9 ay geriye (Sale): 877 sonuç ✅
10 ay geriye (Sale): 581 sonuç ✅
11 ay geriye (Sale): 153 sonuç ✅
12 ay geriye (Sale): 0 sonuç (mağaza henüz açılmamış)
```
**Sonuç**: Settlements API mağaza açılışından itibaren TÜM veriyi destekler!

## Test Endpoint'leri (Geçici)

Test için oluşturulan endpoint'ler (`TrendyolApiLimitTestController.java`):

```
GET /api/test/trendyol-limits/orders/{storeId}?monthsBack=X
GET /api/test/trendyol-limits/financials/{storeId}?monthsBack=X
GET /api/test/trendyol-limits/claims/{storeId}?monthsBack=X
GET /api/test/trendyol-limits/questions/{storeId}?daysBack=X
GET /api/test/trendyol-limits/settlements/{storeId}?monthsBack=X&transactionType=Sale|Return|Discount|Coupon
POST /api/test/trendyol-limits/sync-all-financials/{storeId}?monthsBack=X
```

⚠️ **NOT**: Test dosyaları (`TrendyolApiLimitTestController.java` ve `TestSecurityRules.java`) production'da silinmelidir.
