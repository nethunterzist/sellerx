# Historical Settlement Sync - Tarihi Sipariş Senkronizasyonu

> **KRITIK BELGE**: Bu belge, Trendyol API limitlerini aşarak tarihi sipariş verilerini çekme yöntemini açıklar. Bu bilgiler asla kaybolmamalı!

## Problem Tanımı

### Trendyol Orders API Limiti

Trendyol Orders API'nin **90 günlük hard limiti** vardır:
- Son 90 gün dışındaki siparişler **ÇEKILEMEZ**
- Bu limit Trendyol tarafından belirlenir ve değiştirilemez
- Yeni müşteriler SellerX'e abone olduğunda sadece son 3 aylık veri görürler

### Kullanıcı Beklentisi

Kullanıcılar mağazalarını bağladığında:
- İlk siparişten bugüne kadar TÜM verileri görmek istiyorlar
- Yıllık kar/zarar analizi yapmak istiyorlar
- Geçmiş performanslarını değerlendirmek istiyorlar

## Çözüm: Settlements API

### Keşif (19 Ocak 2026)

Trendyol Financial Settlements API'nin sipariş seviyesinde veri içerdiği keşfedildi:

```
Endpoint: /integration/finance/che/sellers/{sellerId}/settlements
```

### Settlements API'nin Avantajları

| Özellik | Orders API | Settlements API |
|---------|------------|-----------------|
| Zaman Limiti | Son 90 gün | **2+ yıl geriye** |
| Komisyon Oranı | Tahmini | **GERÇEK** |
| Veri Detayı | Tam sipariş | Özet (barcode, fiyat) |
| Müşteri Bilgisi | Var | Yok |
| Kargo Takibi | Var | Yok |

### Settlements API Parametreleri

```
GET /integration/finance/che/sellers/{sellerId}/settlements
  ?transactionType=Sale|Return|Discount|Coupon  (ZORUNLU!)
  &startDate={timestamp_ms}
  &endDate={timestamp_ms}
  &page=0
  &size=500|1000  (SADECE 500 veya 1000 kabul edilir!)
```

**Kritik Kısıtlamalar:**
- `transactionType` parametresi **ZORUNLU** - eksikse 500 hatası
- `size` parametresi **500 veya 1000** olmalı - başka değer 400 hatası
- Tarih aralığı **maksimum 15 gün** - daha fazlası hata verir
- Kimlik doğrulama: Basic Auth (API Key:API Secret base64 encoded)

### Settlement Response Yapısı

```json
{
  "content": [
    {
      "id": "7233382653",
      "orderNumber": "9966349644",
      "orderDate": 1738337211184,
      "transactionDate": 1738571320087,
      "barcode": "8809640733642",
      "transactionType": "Satış",
      "credit": 585.0,
      "debt": 0.0,
      "commissionRate": 3.8,
      "commissionAmount": 22.23,
      "sellerRevenue": 562.77,
      "shipmentPackageId": 2722331454,
      "paymentOrderId": 47384818,
      "currency": "TRY"
    }
  ],
  "totalElements": 542,
  "totalPages": 2
}
```

## Uygulama Detayları

### Yeni Onboarding Akışı

```
1. SYNCING_PRODUCTS      → Tüm ürünler
2. SYNCING_ORDERS        → Son 90 gün (Orders API)
3. SYNCING_HISTORICAL    → 90 gün öncesi (Settlements API) ← YENİ!
4. SYNCING_FINANCIAL     → Finansal veriler
5. RECALCULATING_COMMISSIONS → Komisyon güncellemesi
6. SYNCING_QA            → Soru-cevaplar
7. COMPLETED
```

### Kritik Kod Düzeltmesi

**YANLIŞ** (ilk versiyon):
```java
// store.getCreatedAt() = SellerX'e eklenme tarihi (yeni!)
LocalDateTime syncFrom = store.getCreatedAt();
```

**DOĞRU** (düzeltilmiş):
```java
// 2 yıl geriye git - gerçek Trendyol mağaza tarihi bilinmiyor
private static final int HISTORICAL_MONTHS_BACK = 24;
LocalDateTime syncFrom = LocalDateTime.now().minusMonths(HISTORICAL_MONTHS_BACK);
```

### Veri Kaynağı Takibi

`trendyol_orders` tablosuna `data_source` kolonu eklendi:

```sql
ALTER TABLE trendyol_orders ADD COLUMN data_source VARCHAR(50);
-- Değerler: 'ORDER_API' veya 'SETTLEMENT_API'
```

## K-Pure Vaka Çalışması

### Doğrulama Testi (19 Ocak 2026)

K-pure mağazası ile yapılan test:

| Metrik | Değer |
|--------|-------|
| Mağaza ID | 4c158da4-bfea-407a-95c1-155bf4176770 |
| Seller ID | 1080066 |
| İlk Sipariş | **9966349644** |
| İlk Sipariş Tarihi | **31 Ocak 2025, 18:26:51** |
| SellerX'e Eklenme | 19 Ocak 2026 |
| Orders API Verileri | 19 Ekim 2025 - 19 Ocak 2026 |
| Kayıp Veri Aralığı | 31 Ocak 2025 - 19 Ekim 2025 (~9 ay) |

### Aylık Settlement İstatistikleri

K-pure için aylık Sale settlement sayıları (1-14 arası):

| Ay | Settlement | Benzersiz Sipariş |
|----|------------|-------------------|
| Şubat 2025 | 542 | 461 |
| Mart 2025 | 678 | ~580 |
| Nisan 2025 | 257 | ~220 |
| Mayıs 2025 | 764 | ~650 |
| Haziran 2025 | 2,053 | 874 |
| Temmuz 2025 | 624 | ~530 |
| Ağustos 2025 | 323 | ~275 |
| Eylül 2025 | 680 | ~580 |
| Ekim 2025 | 175 | ~150 |

**Toplam Tahmini Tarihi Sipariş: ~10,000-12,000**

### İlk Siparişi Doğrulama

```bash
# Ocak 1-30, 2025 kontrol
curl ... ?transactionType=Sale&startDate=1704067200000&endDate=1706745600000
# Sonuç: 0 settlement (sipariş yok)

# Ocak 31 - Şubat 14, 2025 kontrol
curl ... ?transactionType=Sale&startDate=1738270800000&endDate=1739480400000
# Sonuç: 542 settlement, ilk sipariş 9966349644
```

## Performans Tahminleri

### API Çağrı Hesabı

```
Tarih Aralığı: 2 yıl = 730 gün
Chunk Boyutu: 14 gün
Chunk Sayısı: 730 / 14 = ~52 chunk
Transaction Tipleri: 4 (Sale, Return, Discount, Coupon)
Toplam API Çağrısı: 52 × 4 = ~208 çağrı (pagination hariç)
```

### Tahmini Süre

| Mağaza Tipi | Tarihi Sipariş | API Çağrısı | Süre |
|-------------|----------------|-------------|------|
| Yeni (<90 gün) | 0 | 0 | Atlanır |
| Normal (6 ay) | ~500 | ~80 | 3-5 dk |
| Aktif (12 ay) | ~2,000 | ~120 | 8-12 dk |
| Yoğun (2 yıl) | ~10,000+ | ~250 | 15-25 dk |

## Dosya Referansları

### Backend

| Dosya | Açıklama |
|-------|----------|
| `TrendyolHistoricalSettlementService.java` | Ana servis (2 yıl geriye) |
| `HistoricalSyncResult.java` | Sync sonuç DTO |
| `StoreOnboardingService.java` | SYNCING_HISTORICAL aşaması |
| `V52__add_data_source_to_orders.sql` | Database migration |
| `TrendyolOrder.java` | `dataSource` alanı |
| `Store.java` | `historicalSyncStatus`, `historicalSyncDate` |

### Frontend

| Dosya | Açıklama |
|-------|----------|
| `types/store.ts` | SyncStatus tipi güncellemesi |
| `sync-status-display.tsx` | 6 aşamalı UI |

## Hata Senaryoları

### 1. API 500 Hatası
```
Sebep: transactionType parametresi eksik
Çözüm: Her zaman transactionType=Sale|Return|Discount|Coupon ekle
```

### 2. API 400 Hatası - Size
```
Sebep: size parametresi 500 veya 1000 değil
Mesaj: "Size değeri 500 ya da 1000 olmalıdır"
Çözüm: size=500 veya size=1000 kullan
```

### 3. API 400 Hatası - Tarih Aralığı
```
Sebep: startDate ve endDate arası 15 günden fazla
Mesaj: "15 günden büyük olamaz"
Çözüm: 14 günlük chunk'lar kullan (güvenli margin)
```

### 4. Eksik Tarihi Veri
```
Sebep: store.getCreatedAt() kullanılmış
Çözüm: Sabit 2 yıl geriye git (HISTORICAL_MONTHS_BACK = 24)
```

## Gelecek İyileştirmeler

1. **Dinamik Başlangıç Tarihi**: İlk settlement tarihini bul ve oradan başla
2. **Incremental Sync**: Sadece yeni settlement'ları çek
3. **Progress Tracking**: Detaylı ilerleme göstergesi (X/Y chunk)
4. **Parallel Processing**: Farklı tarih aralıklarını paralel çek
5. **Resume Support**: Yarıda kalan sync'i devam ettir

## Önemli Notlar

> **ASLA UNUTMA:**
> 1. Orders API sadece **90 gün** veri verir - bu Trendyol'un hard limiti
> 2. Settlements API **2+ yıl** geriye gidebilir
> 3. `store.getCreatedAt()` = SellerX'e eklenme tarihi, **Trendyol mağaza açılış tarihi DEĞİL**
> 4. Settlement'taki komisyon oranları **GERÇEK**, Orders API'deki **TAHMİNİ**
> 5. `size` parametresi **SADECE 500 veya 1000** kabul ediyor
> 6. Tarih aralığı **maksimum 15 gün** olmalı

---

*Son Güncelleme: 19 Ocak 2026*
*Yazar: Claude Code*
*Doğrulama: K-pure mağazası (seller_id: 1080066)*
