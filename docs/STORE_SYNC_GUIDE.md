# Mağaza Senkronizasyonu Rehberi

Bu belge, SellerX platformunda Trendyol mağazası bağlama ve senkronizasyon sürecini detaylı olarak açıklar.

## İçindekiler

1. [Genel Bakış](#genel-bakış)
2. [Senkronizasyon Aşamaları](#senkronizasyon-aşamaları)
3. [Veri Kaynakları](#veri-kaynakları)
4. [Settlement API vs Order API](#settlement-api-vs-order-api)
5. [Binary Search Algoritması](#binary-search-algoritması)
6. [GAP Algoritması](#gap-algoritması)
7. [Komisyon Sistemi](#komisyon-sistemi)
8. [Teknik Detaylar ve Limitler](#teknik-detaylar-ve-limitler)
9. [Troubleshooting](#troubleshooting)
10. [Gerçek Dünya Örneği](#gerçek-dünya-örneği)

---

## Genel Bakış

Bir Trendyol mağazası bağlandığında, sistem otomatik olarak tüm geçmiş verileri çeker. Bu süreç **6 aşamadan** oluşur:

```
MAĞAZA OLUŞTURULDU
        ↓
SYNCING_PRODUCTS (Ürünler)
        ↓
SYNCING_HISTORICAL (Geçmiş Siparişler - Settlement API)
        ↓
SYNCING_FINANCIAL (Finansal Detaylar + Hakediş + Stopaj)
        ↓
SYNCING_GAP (Son Günlerin Siparişleri - Order API)
        ↓
SYNCING_QA (Müşteri Soruları)
        ↓
COMPLETED ✓
```

### Neden Bu Kadar Aşama?

Trendyol'un farklı API'leri farklı veriler sunar:

| API | Ne Verir | Eksik Ne |
|-----|----------|----------|
| **Products API** | Ürün kataloğu, stok, fiyat | Sipariş yok |
| **Orders API** | Tüm siparişler (anlık) | Gerçek komisyon yok |
| **Settlement API** | Tamamlanmış siparişler + gerçek komisyon | Bekleyen siparişler yok (2-3 gün gecikme) |
| **OtherFinancials API** | Hakediş, stopaj, kargo faturaları | Sipariş detayı yok |

---

## Senkronizasyon Aşamaları

### 1. SYNCING_PRODUCTS

**Ne Yapıyor?**
- Trendyol'dan tüm ürünleri çeker
- Barkod, isim, fiyat, stok bilgisi kaydeder
- Ürün görselleri ve kategori bilgisi alır

**Neden Gerekli?**
- Sipariş verilerini ürünlerle eşleştirmek için
- Maliyet girişi yapabilmek için
- Dashboard'da ürün bazlı analiz için
- GAP algoritmasında `lastCommissionRate` referansı için

**Kod:** `TrendyolProductService.syncProducts()`

---

### 2. SYNCING_HISTORICAL (Uzun vadeli - ÖNEMLİ!)

**Ne Yapıyor?**
1. Binary Search ile mağazanın ilk sipariş tarihini bulur
2. O tarihten bugüne kadar Settlement API'den TÜM siparişleri çeker
3. 14 günlük chunk'lar halinde işler (Trendyol limiti: 15 gün)

**Neden Gerekli?**
- **GERÇEK komisyon oranları** için (tahmini değil)
- Geçmiş tüm satış verisi için
- Doğru kar/zarar hesabı için

**Veri Farkı:**

```
Order API'den gelen sipariş:
{
  "commission": 134.03,           // TAHMİNİ
  "isCommissionEstimated": true   // Tahmini mi? EVET
}

Settlement API'den gelen sipariş:
{
  "commission": 127.85,           // GERÇEK
  "isCommissionEstimated": false, // Tahmini mi? HAYIR
  "commissionRate": 5.2,          // Gerçek oran
  "sellerRevenue": 1071.65        // Net kazanç
}
```

**Kod:** `TrendyolHistoricalSettlementService.syncHistoricalOrders()`

---

### 3. SYNCING_FINANCIAL (Detay ekleme)

**Ne Yapıyor?**

**3a. Settlement Detayları:**
- Her siparişe ek finansal detaylar ekler
- Kupon indirimleri (COUPON transaction)
- Kargo kesintileri
- İade kesintileri

**3b. Ürün Komisyon Oranları:**
- Son 12 ayın settlement verilerini tarar
- Her ürün için `lastCommissionRate` günceller
- Bu oran GAP algoritmasında referans olarak kullanılır

**3c. Hakediş ve Stopaj Verileri:**
- `syncPaymentOrders()` - Hakediş kayıtları
- `syncStoppages()` - Stopaj kayıtları
- `syncCargoInvoices()` - Kargo faturaları

**Neden Gerekli?**
- Tam finansal tablo için
- Stopaj, kesinti detayları için
- Ödeme takibi için
- **GAP algoritmasının doğru komisyon tahmini yapabilmesi için**

**Örnek Eklenen Veri:**
```json
{
  "financialTransactions": [
    {
      "type": "SATIS",
      "amount": 1199.00,
      "commission": 62.35
    },
    {
      "type": "COUPON",
      "amount": -50.00,
      "description": "Trendyol kuponu"
    },
    {
      "type": "KARGO",
      "amount": -15.00,
      "description": "Kargo kesintisi"
    }
  ]
}
```

**Kod:** `TrendyolFinancialSettlementService.fetchSettlementsForStore()`

**Tahmini Süre:** 4-10 dakika (12 aylık veri, rate limiting nedeniyle)

---

### 4. SYNCING_GAP (Son Günlerin Boşluğunu Doldurma)

**Ne Yapıyor?**
1. Orders API'den son 15 günün siparişlerini çeker
2. Settlement API'de **olmayan** siparişleri tespit eder
3. Bu siparişleri **tahmini komisyon** ile kaydeder

**Neden Gerekli?**
- Settlement API 2-3 gün gecikmeli veri verir
- Son günlerin siparişleri Settlement'ta henüz yok
- Dashboard'da güncel veri göstermek için

**Komisyon Tahmin Algoritması:**
```java
// OrderCostCalculator.java
public BigDecimal getEffectiveCommissionRate(TrendyolProduct product) {
    // 1. Öncelik: Financial API'den gelen gerçek oran
    if (product.getLastCommissionRate() != null) {
        return product.getLastCommissionRate();
    }
    // 2. Öncelik: Product API'den gelen kategori oranı
    else if (product.getCommissionRate() != null) {
        return product.getCommissionRate();
    }
    // 3. Fallback: Veri yoksa 0
    return BigDecimal.ZERO;
}
```

**Önemli:** GAP siparişleri `isCommissionEstimated = true` olarak işaretlenir. Settlement verisi geldiğinde reconciliation job ile güncellenir.

**Kod:** `TrendyolOrderService.syncGapFromOrdersApi()`

---

### 4b. Komisyon Yeniden Hesaplama

**Ne Yapıyor?**
- 0 komisyonlu siparişleri tespit eder
- `lastCommissionRate` kullanarak yeniden hesaplar

**Kod:** `OrderCommissionRecalculationService.recalculateEstimatedCommissions()`

---

### 5. SYNCING_QA (Müşteri Soruları)

**Ne Yapıyor?**
- Son 14 günün müşteri sorularını çeker
- Cevaplanan/cevaplanmayan durumları kaydeder

**Kod:** `TrendyolQaService.syncQuestions()`

---

### 6. COMPLETED

Tüm aşamalar tamamlandığında:
- `sync_status` = `COMPLETED`
- `initial_sync_completed` = `true`
- Mağaza kullanıma hazır

---

## Veri Kaynakları

### data_source Alanı

Her sipariş kaydında `data_source` alanı nereden geldiğini gösterir:

| Değer | Anlamı | Komisyon Durumu |
|-------|--------|-----------------|
| `SETTLEMENT_API` | Settlement API'den geldi | **GERÇEK** komisyon |
| `ORDER_API` | Order API'den geldi (GAP) | **TAHMİNİ** komisyon |
| `WEBHOOK` | Webhook ile geldi | **TAHMİNİ** komisyon |

### Hangi Kaynağı Tercih Etmeliyiz?

**SETTLEMENT_API öncelikli** çünkü:
- Gerçek komisyon oranları var
- Trendyol'un kesinleşmiş verileri
- Finansal raporlarla uyumlu

---

## Settlement API vs Order API

### Order API

```
Endpoint: GET /suppliers/{supplierId}/orders
```

**Avantajları:**
- Anlık veri (real-time)
- Tüm sipariş durumları
- Bekleyen siparişler dahil

**Dezavantajları:**
- Komisyon **tahmini**
- Finansal detay yok
- Kesinleşmemiş veriler

### Settlement API

```
Endpoint: GET /suppliers/{supplierId}/settlements
```

**Avantajları:**
- **GERÇEK** komisyon oranları
- Finansal detaylar (kupon, kesinti)
- Trendyol'un resmi verileri

**Dezavantajları:**
- Sadece "Delivered" siparişler
- 2-3 gün gecikme
- Bekleyen siparişler yok

### Örnek Karşılaştırma

Aynı sipariş, iki API'den:

**Order API:**
```json
{
  "orderNumber": "10195973744",
  "totalPrice": 2083.90,
  "estimatedCommission": 134.03
}
```

**Settlement API:**
```json
{
  "orderNumber": "10195973744",
  "totalPrice": 2083.90,
  "commission": 127.85,
  "commissionRate": 5.2,
  "paymentOrderId": 49624018,
  "sellerRevenue": 1956.05
}
```

Fark: **6.18 TL** (134.03 - 127.85)

---

## Binary Search Algoritması

### Problem
Mağazanın ilk sipariş tarihini bulmak lazım. Trendyol 2017'den beri çalışıyor = ~3000 gün.

### Çözüm: Binary Search
Lineer arama yerine ikili arama ile O(log n) karmaşıklık.

```
Başlangıç: 2017-10-01 ────────────────────── Bugün: 2026-01-21
                              ↓
                    Orta nokta: 2021-12-01
                    Veri var mı? EVET
                              ↓
         2017-10-01 ─────── 2021-12-01
                    ↓
              2019-11-01
              Veri var mı? HAYIR
                    ↓
                    2019-11-01 ─── 2021-12-01
                              ↓
                        ... devam ...
                              ↓
                    İlk sipariş: 2025-01-31 ✓
```

### Kod Mantığı

```java
public LocalDate findFirstOrderDate(Store store) {
    LocalDate low = LocalDate.of(2017, 10, 1);  // Trendyol başlangıcı
    LocalDate high = LocalDate.now();
    LocalDate result = high;

    while (low.isBefore(high) || low.isEqual(high)) {
        LocalDate mid = low.plusDays(ChronoUnit.DAYS.between(low, high) / 2);

        if (hasDataInRange(store, mid, mid.plusDays(14))) {
            result = mid;
            high = mid.minusDays(1);  // Daha erken tarih ara
        } else {
            low = mid.plusDays(1);    // Daha geç tarih ara
        }
    }
    return result;
}
```

---

## GAP Algoritması

### Amaç
Settlement API 2-3 gün gecikmeyle veri sağlar. Bu süredeki siparişleri Order API'den çekip tahmini komisyonla kaydetmek.

### Akış

```
┌─────────────────────────────────────────────────────────────┐
│                    GAP DOLDURUM AKIŞI                       │
└─────────────────────────────────────────────────────────────┘

1. Orders API'den son 15 günün siparişlerini çek
                    ↓
2. Her sipariş için: Settlement API'de var mı?
        │
        ├─ EVET → Atla (zaten gerçek komisyonla var)
        │
        └─ HAYIR → Sipariş GAP'te
                    ↓
3. Siparişin ürününü bul (barcode ile)
                    ↓
4. Ürünün lastCommissionRate değerini al
   (Financial Sync'te güncellenmişti)
                    ↓
5. Tahmini komisyon hesapla:
   commission = totalPrice × (lastCommissionRate / 100)
                    ↓
6. Siparişi kaydet:
   - data_source = 'ORDER_API'
   - is_commission_estimated = true
```

### Komisyon Öncelik Sırası

```
1. lastCommissionRate (Financial API'den) → En güvenilir
   ↓ (yoksa)
2. commissionRate (Product API'den) → Kategori bazlı
   ↓ (yoksa)
3. 0 → Hiç veri yoksa
```

### Reconciliation (Uzlaşma)

GAP siparişleri daha sonra Settlement verisi geldiğinde güncellenir:
- `CommissionReconciliationService` daily job olarak çalışır
- Tahmini komisyonu gerçek komisyonla değiştirir
- `isCommissionEstimated` = false yapar

---

## Komisyon Sistemi

### Komisyon Nasıl Hesaplanıyor?

```
Komisyon = vatBaseAmount × (commissionRate / 100)
```

**Örnek:**
- Ürün fiyatı: 1199.00 TL
- KDV dahil tutar (vatBaseAmount): 1199.00 TL
- Komisyon oranı: %5.2
- Komisyon: 1199.00 × 0.052 = **62.35 TL**

### Tahmini vs Gerçek Komisyon

| Alan | Tahmini | Gerçek |
|------|---------|--------|
| Kaynak | Order API / GAP | Settlement API |
| `isCommissionEstimated` | `true` | `false` |
| Doğruluk | ~%90-95 | %100 |
| Ne zaman | Anlık | 2-3 gün sonra |

### Neden Fark Oluyor?

1. **Kategori bazlı oran değişimi**: Trendyol oranları değiştirebilir
2. **Kampanya indirimleri**: Özel dönem oranları
3. **Kupon etkileri**: Kuponlu satışlarda farklı hesaplama

---

## Teknik Detaylar ve Limitler

### Trendyol API Limitleri

| Limit | Değer | Açıklama |
|-------|-------|----------|
| Rate Limit | **10 req/sec** | Saniyede maksimum istek |
| Tarih Aralığı | **15 gün** | Settlement API maksimum |
| Page Size | **1000** | Sayfa başına maksimum kayıt |

### Kritik Parametreler

**Settlement API çağrısı:**
```
GET /suppliers/{supplierId}/settlements
    ?startDate=2025-01-01
    &endDate=2025-01-15      // Maksimum 15 gün fark!
    &size=1000               // 1000 olmalı, 1 veya 10 ÇALIŞMAZ!
```

### Chunk İşleme

15 günlük API limiti nedeniyle veriler 14 günlük parçalar halinde çekilir:

```
Toplam süre: 2025-01-31 → 2026-01-21 = ~356 gün

Chunk 1:  2025-01-31 → 2025-02-14 (14 gün)
Chunk 2:  2025-02-15 → 2025-03-01 (14 gün)
Chunk 3:  2025-03-02 → 2025-03-16 (14 gün)
...
Chunk 26: 2026-01-07 → 2026-01-21 (14 gün)
```

**Neden 14 gün, 15 değil?**
```java
// YANLIŞ - 16 gün oluşur (start dahil, end dahil)
endDate = startDate.plusDays(15);

// DOĞRU - 15 gün oluşur
endDate = startDate.plusDays(14);
```

---

## Troubleshooting

### Sık Karşılaşılan Hatalar

#### 1. "tarih aralığı 15 günden büyük olamaz"

**Sebep:** `plusDays(15)` kullanılmış
**Çözüm:** `plusDays(14)` kullanılmalı

```java
// Yanlış
LocalDate end = start.plusDays(15);

// Doğru
LocalDate end = start.plusDays(14);
```

#### 2. "keys.error.parameter.invalid.size" (400 Bad Request)

**Sebep:** `size=1` veya `size=10` kullanılmış
**Çözüm:** `size=1000` kullanılmalı

```java
// Yanlış
String url = baseUrl + "&size=1";

// Doğru
String url = baseUrl + "&size=1000";
```

#### 3. Sync "SYNCING_*" durumunda takılı kalıyor

**Olası Sebepler:**
1. API hatası (yukarıdaki hatalar)
2. Rate limit aşımı
3. Bağlantı kopması
4. Backend crash (exit code 137)

**Çözüm:**
```sql
-- Durumu FAILED yap
UPDATE stores
SET sync_status = 'FAILED'
WHERE id = 'store-uuid';
```

Sonra retry-sync endpoint'ini çağır:
```bash
# DİKKAT: /api prefix YOK!
curl -X POST "http://localhost:8080/stores/{storeId}/retry-sync" \
  -H "Authorization: Bearer {token}"
```

#### 4. 404 Not Found - retry-sync endpoint

**Sebep:** Yanlış endpoint kullanılmış
```
❌ /api/stores/{id}/retry-sync  → 404 Not Found
✅ /stores/{id}/retry-sync      → Çalışıyor
```

**Açıklama:** `StoreController` `@RequestMapping("/stores")` kullanıyor, `/api` prefix yok.

#### 5. retry-sync çağrılıyor ama hiçbir şey olmuyor

**Sebep:** `retryInitialSync()` metodu `SYNCING_*` durumundaki mağazaları blokluyor

```java
// StoreOnboardingService.java
if (store.getSyncStatus().startsWith("SYNCING_")) {
    log.warn("Store {} is already syncing", storeId);
    return; // BLOCKED!
}
```

**Çözüm:** Önce durumu `FAILED` yapın:
```sql
UPDATE stores SET sync_status = 'FAILED' WHERE id = 'store-uuid';
```

#### 6. Binary Search 0 sonuç dönüyor

**Olası Sebepler:**
1. `size` parametresi yanlış (1000 olmalı)
2. Tarih formatı hatalı
3. API credentials geçersiz

**Debug için:**
```java
log.info("[BINARY-CHECK] {} type for {} to {}: totalElements={}",
    transactionType, start, end, response.getTotalElements());
```

#### 7. SYNCING_FINANCIAL çok uzun sürüyor

**Normal Davranış:** 4-10 dakika sürebilir

**Sebep:**
- 12 aylık veri işleniyor
- 15 günlük parçalar = ~24 periyot
- Her periyot için 4 transaction type
- Pagination + rate limiting
- 500ms delay between calls

**Kontrol:**
```sql
-- Ürün komisyon oranları güncellenmiş mi?
SELECT COUNT(*) FROM trendyol_products
WHERE store_id = 'store-uuid' AND last_commission_rate IS NOT NULL;

-- Hakediş/stopaj verileri var mı?
SELECT
  (SELECT COUNT(*) FROM trendyol_payment_orders WHERE store_id = 'store-uuid') as payment_orders,
  (SELECT COUNT(*) FROM trendyol_stoppages WHERE store_id = 'store-uuid') as stoppages;
```

### Store Durumunu Kontrol Etme

```sql
SELECT
  store_name,
  sync_status,
  initial_sync_completed,
  sync_error_message
FROM stores
WHERE id = 'store-uuid';
```

### Sipariş İstatistiklerini Kontrol Etme

```sql
SELECT
  data_source,
  COUNT(*) as siparis_sayisi,
  SUM(CASE WHEN is_commission_estimated = false THEN 1 ELSE 0 END) as gercek_komisyon,
  SUM(CASE WHEN is_commission_estimated = true THEN 1 ELSE 0 END) as tahmini_komisyon
FROM trendyol_orders
WHERE store_id = 'store-uuid'
GROUP BY data_source;
```

### Finansal Verileri Kontrol Etme

```sql
SELECT
  (SELECT COUNT(*) FROM trendyol_payment_orders WHERE store_id = 'store-uuid') as hakedis,
  (SELECT COUNT(*) FROM trendyol_stoppages WHERE store_id = 'store-uuid') as stopaj,
  (SELECT COUNT(*) FROM trendyol_cargo_invoices WHERE store_id = 'store-uuid') as kargo_fatura,
  (SELECT COUNT(*) FROM trendyol_products WHERE store_id = 'store-uuid' AND last_commission_rate IS NOT NULL) as komisyon_oranli_urun;
```

---

## Gerçek Dünya Örneği

### k-pure Mağazası Senkronizasyonu (21 Ocak 2026)

**Başlangıç Durumu:**
- Mağaza `SYNCING_HISTORICAL` aşamasında takılı kalmıştı
- Backend crash (exit code 137)

**Tespit Edilen ve Düzeltilen Buglar:**
1. `plusDays(15)` → `plusDays(14)` (3 yerde)
2. `size=1` → `size=1000`

**Çözüm Adımları:**
1. Store durumu `FAILED` yapıldı
2. Backend yeniden başlatıldı
3. retry-sync çağrıldı (doğru endpoint: `/stores/{id}/retry-sync`)

**Final Sonuçlar:**

| Metrik | Değer | Oran |
|--------|-------|------|
| Toplam Sipariş | 26,673 | %100 |
| Gerçek Komisyon (Settlement) | 25,506 | %95.6 |
| Tahmini Komisyon (GAP) | 1,167 | %4.4 |
| Toplam Ürün | 260 | - |
| Komisyon Oranı Olan Ürün | 236 | %90.8 |
| Hakediş Kayıtları | 105 | - |
| Stopaj Kayıtları | 153 | - |

**Sync Süresi:** ~15-20 dakika

---

## Özet

### Senkronizasyon Akışı

```
1. Mağaza Oluştur (API keys gir)
        ↓
2. Ürünleri Çek (Products API)
        ↓
3. Geçmiş Siparişleri Çek (Settlement API - tüm tarihler)
   - Binary Search ile ilk tarih bul
   - 14 günlük chunk'lar halinde çek
   - GERÇEK komisyon verileri al
        ↓
4. Finansal Detayları Ekle
   - Kupon, kesinti, iade bilgileri
   - Ürünlere lastCommissionRate ata
   - Hakediş ve stopaj verilerini çek
        ↓
5. GAP Doldur (Order API - son 15 gün)
   - Settlement'ta olmayan siparişleri ekle
   - lastCommissionRate ile tahmini komisyon hesapla
        ↓
6. Q&A Sync (Müşteri soruları)
        ↓
7. COMPLETED ✓
```

### Kritik Noktalar

1. **Settlement API tercih edilmeli** - gerçek komisyon için
2. **15 gün limiti** - `plusDays(14)` kullanılmalı
3. **size=1000** - daha düşük değerler çalışmaz
4. **Rate limit: 10 req/sec** - aşılmamalı
5. **Endpoint: `/stores/{id}/retry-sync`** - `/api` prefix YOK!
6. **Takılı sync** - önce `FAILED` yap, sonra retry

---

## İlgili Dosyalar

| Dosya | Açıklama |
|-------|----------|
| `StoreOnboardingService.java` | Ana senkronizasyon orkestratörü |
| `TrendyolHistoricalSettlementService.java` | Geçmiş sipariş çekme + Binary Search |
| `TrendyolFinancialSettlementService.java` | Finansal detay + komisyon oranları |
| `TrendyolOtherFinancialsService.java` | Hakediş, stopaj, kargo faturaları |
| `TrendyolOrderService.java` | GAP sipariş çekme |
| `TrendyolProductService.java` | Ürün senkronizasyonu |
| `OrderCostCalculator.java` | Komisyon tahmin algoritması |
| `OrderCommissionRecalculationService.java` | Komisyon yeniden hesaplama |
| `CommissionReconciliationService.java` | Tahmini → Gerçek komisyon güncelleme |

---

*Son güncelleme: 2026-01-21*
*Yazan: Claude Code - k-pure mağazası debugging session sonrası*
