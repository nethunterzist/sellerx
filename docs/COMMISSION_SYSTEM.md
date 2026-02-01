# Komisyon Hesaplama Sistemi

Bu doküman, SellerX platformundaki Trendyol komisyon hesaplama sisteminin **kesinleşmiş mimarisini** açıklar.

## Temel Bilgiler

### Komisyon Hesaplama Formülü

```
unitEstimatedCommission = vatBaseAmount × commissionRate / 100
```

| Parametre | Kaynak | Açıklama |
|-----------|--------|----------|
| `vatBaseAmount` | Order API → lines[].vatBaseAmount | KDV hariç birim fiyat (Trendyol hesaplıyor) |
| `commissionRate` | product.lastCommissionRate | Son bilinen komisyon oranı (%) |

**Örnek Hesaplama:**
- vatBaseAmount: 75 TL (KDV hariç)
- Komisyon oranı: %12
- Komisyon = 75 × 12 / 100 = **9.00 TL**

### Neden vatBaseAmount?

Trendyol Order API her sipariş satırında `vatBaseAmount` döndürüyor - bu zaten **KDV hariç** tutarı içeriyor.

> **Önemli**: Manuel KDV hesaplaması (0.8 çarpanı) KULLANILMAMALI. Trendyol farklı KDV oranları (%0, %1, %10, %20) uygulayabiliyor ve bunu zaten hesaplıyor.

---

## Veri Kaynakları Analizi

### Trendyol API'leri ve Komisyon Verisi

| API | Komisyon Verisi | Durum |
|-----|-----------------|-------|
| Order API | ❌ Yok | Sipariş satırlarında komisyon bilgisi dönmüyor |
| Product API | ❌ Yok | Ürün bilgilerinde komisyon oranı yok |
| Category API | ❌ Yok | Kategori bazlı komisyon oranı dönmüyor |
| **Financial Settlement API** | ✅ **VAR** | Gerçek komisyon oranı ve tutarı mevcut |

### Önemli Keşif: Komisyon Oranları Değişkendir

Komisyon oranları **sadece kategoriye bağlı DEĞİL**, aynı zamanda:
- Satıcı performansına
- Satıcı seviyesine (yıldız)
- Kampanya dönemlerine
- Trendyol'un dinamik fiyatlandırmasına

bağlı olarak **satıcıdan satıcıya farklılık gösterir**.

---

## Financial API Gecikme Analizi

### Bulgu: Gecikme Sabit Değil, Değişken

Financial API'deki veri, siparişin **işlendiği tarihte** (ödeme/komisyon kesildiğinde) görünür, sipariş tarihinde değil.

```
Sipariş Geldi (Gün 0)
       ↓
Satıcı Kargoyu Verdi (1-6 gün sonra)
       ↓
Kargo Teslim Etti (1-5 gün sonra)
       ↓
Müşteri Onayı / Otomatik Onay (0-3 gün)
       ↓
Trendyol Ödeme İşledi ← Financial API'ye veri BURADA düşüyor
```

### Test Sonuçları (18 Ocak 2026)

| Sipariş Tarihi | İşlem Tarihi | Gecikme |
|----------------|--------------|---------|
| 11 Ocak | 18 Ocak | 7 gün |
| 14 Ocak | 18 Ocak | 4 gün |
| 14-16 Ocak | 17 Ocak | 1-3 gün |
| 13-15 Ocak | 16 Ocak | 1-3 gün |

### Gecikme Özeti

| Durum | Gecikme Süresi |
|-------|----------------|
| Minimum | 1 gün (hızlı teslimat, aynı şehir) |
| Ortalama | 3-4 gün |
| Maksimum | 7+ gün (uzak şehir, gecikmeli teslimat) |

**Sonuç:** Financial Sync'te **son 14 günü** sorgulamak tüm siparişleri yakalar.

---

## Kesinleşen Mimari: Tahmini + Gerçek Komisyon

### Temel Prensip

1. **Yeni sipariş geldiğinde:** O ürünün (barkodun) en son bilinen komisyon oranını kullan → **TAHMİNİ**
2. **Financial verisi geldiğinde:** Gerçek komisyon ile güncelle → **GERÇEK**

### Veri Akışı

```
┌─────────────────────────────────────────────────────────────────┐
│  YENİ SİPARİŞ GELDİĞİNDE (Order Sync)                          │
│  ─────────────────────────────────────                          │
│  1. Barkoda göre ürünü bul                                      │
│  2. Komisyon oranı öncelik sırası:                              │
│     a) product.lastCommissionRate (Financial API'den)           │
│     b) product.commissionRate (kategoriden)                     │
│     c) 0 (yeni ürün - henüz veri yok)                          │
│  3. Komisyon hesapla: vatBaseAmount × commissionRate / 100      │
│  4. Siparişe yaz:                                               │
│     - estimated_commission (hesaplanan tutar)                   │
│     - is_commission_estimated = TRUE                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                         1-14 gün sonra
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  FİNANCİAL SYNC (6 saatte bir)                                 │
│  ─────────────────────────────────                              │
│  1. Son 14 günün finansal verisini Trendyol'dan çek             │
│  2. Her sipariş için:                                           │
│     - Gerçek komisyon tutarını siparişe yaz                     │
│     - is_commission_estimated = FALSE yap                       │
│  3. Her barkod için:                                            │
│     - Ürünün last_commission_rate alanını güncelle              │
│     - last_commission_date = işlem tarihi                       │
└─────────────────────────────────────────────────────────────────┘
```

### Yeni Ürün Durumu (Fallback Logic)

Eğer ürünün komisyon oranı hiç bilinmiyorsa (yeni mağaza, yeni ürün):

| Durum | commissionRate | Frontend Gösterimi |
|-------|----------------|-------------------|
| lastCommissionRate var | Gerçek oran | Normal gösterim |
| Sadece commissionRate var | Kategori oranı | "~" işareti ile tahmini |
| Her ikisi de yok | 0 | ℹ️ "Yeni ürün - Trendyol kesinleşen komisyon raporunuz verildikten sonra bu rakam güncellenecektir." |

> **Not**: Financial Sync çalıştıktan sonra (1-7 gün) gerçek komisyon oranı otomatik güncellenir.

### Avantajları

| Özellik | Fayda |
|---------|-------|
| Ürün bazlı tahmin | Kategori değil, o ürünün gerçek oranı kullanılır |
| Satıcıya özel | Her satıcının kendi komisyon oranları |
| Otomatik iyileşme | Financial sync çalıştıkça tahminler doğrulaşır |
| Şeffaflık | Kullanıcı tahmini mi gerçek mi görür |
| Ölçeklenebilir | 2000+ mağaza için çalışır |

---

## Veritabanı Şeması

### trendyol_products Tablosu

```sql
-- Mevcut alan
commission_rate DECIMAL(5,2)           -- Komisyon oranı (%)

-- Yeni eklenecek alanlar
last_commission_rate DECIMAL(5,2)      -- Financial API'den gelen son oran
last_commission_date TIMESTAMP         -- Bu oranın geldiği tarih
```

### trendyol_orders Tablosu

```sql
-- Mevcut alan
estimated_commission DECIMAL(12,2)     -- Toplam komisyon tutarı

-- Yeni eklenecek alan
is_commission_estimated BOOLEAN DEFAULT TRUE  -- Tahmini mi, gerçek mi?
```

### order_items JSONB Yapısı

Her sipariş satırında:
```json
{
  "barcode": "ABC123",
  "productName": "Ürün Adı",
  "quantity": 2,
  "unitPriceOrder": 100.00,
  "discount": 10.00,
  "vatBaseAmount": 75.00,
  "estimatedCommissionRate": 12.50,
  "unitEstimatedCommission": 9.38
}
```

> **Not**: `unitEstimatedCommission = vatBaseAmount × estimatedCommissionRate / 100`

---

## Dashboard'da Gösterim

| Komisyon Durumu | Gösterim Önerisi |
|-----------------|------------------|
| `is_commission_estimated = true` | "~₺125" veya "Tahmini: ₺125" (açık renk) |
| `is_commission_estimated = false` | "₺128.50" (normal renk, kesin değer) |

---

## Kod Yapısı

### Backend Dosyaları

| Dosya | Rol |
|-------|-----|
| `TrendyolOrderService.java` | Sipariş sync, tahmini komisyon hesaplama |
| `TrendyolFinancialSettlementService.java` | Financial API sync, gerçek komisyon |
| `OrderCostCalculator.java` | Komisyon hesaplama utility |
| `TrendyolProductService.java` | Ürün sync, komisyon oranı güncelleme |
| `DashboardStatsService.java` | Dashboard istatistik hesaplama |

### Financial API Endpoint

```
GET /integration/finance/che/sellers/{sellerId}/settlements
    ?startDate={timestamp}
    &endDate={timestamp}
    &size=500  (veya 1000)
```

**Önemli:** `size` parametresi sadece 500 veya 1000 kabul eder.

---

## Uygulama Planı

### Adım 1: Database Migration
```sql
-- Migration: V45__add_commission_tracking_fields.sql

-- Kolonlar
ALTER TABLE trendyol_products
ADD COLUMN IF NOT EXISTS last_commission_rate DECIMAL(5,2);

ALTER TABLE trendyol_products
ADD COLUMN IF NOT EXISTS last_commission_date TIMESTAMP;

ALTER TABLE trendyol_orders
ADD COLUMN IF NOT EXISTS is_commission_estimated BOOLEAN DEFAULT TRUE;

-- Performans Index'leri
CREATE INDEX idx_orders_commission_estimated
    ON trendyol_orders(is_commission_estimated);

CREATE INDEX idx_products_store_barcode
    ON trendyol_products(store_id, barcode);
```

### Adım 2: Financial Sync Güncelleme
- Siparişleri güncellerken `is_commission_estimated = FALSE` yap
- Her barkod için `last_commission_rate` güncelle

### Adım 3: Order Sync Güncelleme
- Yeni sipariş geldiğinde ürünün `last_commission_rate` kullan
- `is_commission_estimated = TRUE` olarak kaydet

### Adım 4: Dashboard Güncelleme
- Tahmini ve gerçek komisyonları farklı göster

---

## Ölçeklendirme Notları

Sistem şu ölçekte çalışacak şekilde tasarlanmıştır:

| Metrik | Değer |
|--------|-------|
| Mağaza sayısı | 1,000 - 2,000 |
| İlk sync sipariş sayısı (mağaza başı) | ~10,000 |
| Günlük yeni sipariş (mağaza başı) | ~1,000 |
| Financial sync penceresi | Son 14 gün |

---

## Tarihçe

| Tarih | Değişiklik |
|-------|------------|
| 2026-01-16 | İlk versiyon: Eski siparişlerden komisyon çıkarma |
| 2026-01-18 | Financial API analizi ve gecikme tespiti |
| 2026-01-18 | Kesinleşen mimari: Tahmini + Gerçek komisyon sistemi |
| 2026-01-18 | Formül güncelleme: vatBaseAmount kullanımı (0.8 çarpanı kaldırıldı) |
| 2026-01-18 | Yeni ürün fallback logic ve UX mesajı eklendi |
| 2026-01-18 | V45 migration'a performans index'leri eklendi |

---

## Sonraki Adımlar

1. ✅ Komisyon hesaplama formülü - Tamamlandı
2. ✅ Financial API gecikme analizi - Tamamlandı (1-7 gün)
3. ✅ Mimari tasarım - Tahmini + Gerçek yaklaşımı
4. ⏳ Database migration - Bekliyor
5. ⏳ Financial Sync güncelleme - Bekliyor
6. ⏳ Order Sync güncelleme - Bekliyor
7. ⏳ Sipariş çekme mekanizması - Tartışılacak (Polling vs Webhook)
