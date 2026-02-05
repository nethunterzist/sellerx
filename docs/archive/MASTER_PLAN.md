# SellerX Master Plan - 2000 Mağaza Ölçeklendirme

Bu doküman, SellerX platformunun 1000 kullanıcı / 2000 mağaza ölçeğine hazırlanması için gereken tüm çalışmaları kapsar.

---

## 🎯 Hedef Ölçek

| Metrik | Değer |
|--------|-------|
| Kullanıcı sayısı | ~1,000 |
| Mağaza sayısı | ~2,000 |
| Günlük sipariş (mağaza başı) | ~1,000 |
| Toplam günlük sipariş | ~2,000,000 |
| İlk sync sipariş (mağaza başı) | ~10,000 |

---

## 📦 Proje Kapsamı

### 3 Ana Modül

```
┌─────────────────────────────────────────────────────────────────┐
│                     MASTER PLAN                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   MODÜL 1   │  │   MODÜL 2   │  │   MODÜL 3   │             │
│  │  Komisyon   │  │  Sipariş    │  │  Mağaza     │             │
│  │  Sistemi    │  │  Sync       │  │  Onboarding │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

# MODÜL 1: Komisyon Hesaplama Sistemi

## Problem
- Trendyol Order API komisyon verisi döndürmüyor
- Financial API 1-7 gün gecikmeli veri döndürüyor
- Yeni siparişlerde komisyon bilinmiyor

## Çözüm: Tahmini + Gerçek Komisyon

```
YENİ SİPARİŞ                         FİNANCİAL SYNC (1-7 gün sonra)
     ↓                                        ↓
Ürünün last_commission_rate'i al     Gerçek komisyonu al
     ↓                                        ↓
Tahmini komisyon hesapla              Siparişi güncelle
     ↓                                        ↓
is_commission_estimated = TRUE        is_commission_estimated = FALSE
                                              ↓
                                      Ürünün last_commission_rate güncelle
```

## Veritabanı Değişiklikleri

### Migration: V45__add_commission_tracking_fields.sql

```sql
-- trendyol_products tablosu
ALTER TABLE trendyol_products
ADD COLUMN IF NOT EXISTS last_commission_rate DECIMAL(5,2);

ALTER TABLE trendyol_products
ADD COLUMN IF NOT EXISTS last_commission_date TIMESTAMP;

-- trendyol_orders tablosu
ALTER TABLE trendyol_orders
ADD COLUMN IF NOT EXISTS is_commission_estimated BOOLEAN DEFAULT TRUE;

-- Performans için index'ler (2M+ satırda kritik)
CREATE INDEX idx_orders_commission_estimated
    ON trendyol_orders(is_commission_estimated);

CREATE INDEX idx_products_store_barcode
    ON trendyol_products(store_id, barcode);
```

## Kod Değişiklikleri

### 1. TrendyolProduct.java
```java
// Yeni alanlar
private BigDecimal lastCommissionRate;
private LocalDateTime lastCommissionDate;
```

### 2. TrendyolOrder.java
```java
// Yeni alan
private Boolean isCommissionEstimated = true;
```

### 3. TrendyolOrderService.java - convertLineToOrderItem()
```java
// Komisyon oranını ürünün last_commission_rate'inden al (öncelik sırası)
BigDecimal commissionRate;

// 1. Önce son gerçek komisyon oranı (Financial API'den)
if (product != null && product.getLastCommissionRate() != null) {
    commissionRate = product.getLastCommissionRate();
}
// 2. Yoksa kategori komisyon oranı
else if (product != null && product.getCommissionRate() != null) {
    commissionRate = product.getCommissionRate();
}
// 3. Yeni ürün - henüz komisyon verisi yok
else {
    commissionRate = BigDecimal.ZERO;
    // Frontend'de info mesajı gösterilecek:
    // "ℹ️ Yeni ürün - Trendyol kesinleşen komisyon raporunuz verildikten sonra bu rakam güncellenecektir."
}

// Komisyon hesapla (vatBaseAmount API'den geliyor - KDV hariç)
BigDecimal unitCommission = vatBaseAmount.multiply(commissionRate)
    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
```

### 4. TrendyolFinancialSettlementService.java - updateOrderWithSettlement()
```java
// Siparişi güncelle
order.setIsCommissionEstimated(false);
order.setEstimatedCommission(realCommission);

// Ürünün komisyon oranını güncelle
product.setLastCommissionRate(settlement.getCommissionRate());
product.setLastCommissionDate(LocalDateTime.now());
productRepository.save(product);
```

## Komisyon Formülü

```
unitEstimatedCommission = vatBaseAmount × commissionRate / 100
```

| Parametre | Kaynak | Açıklama |
|-----------|--------|----------|
| vatBaseAmount | Order API → lines[].vatBaseAmount | KDV hariç birim fiyat (Trendyol hesaplıyor) |
| commissionRate | product.lastCommissionRate | Son bilinen komisyon oranı (%) |

> **Not**: Trendyol Order API `vatBaseAmount` olarak KDV hariç tutarı zaten döndürüyor. Manuel KDV hesaplamasına (0.8 çarpanı) gerek yok.

---

# MODÜL 2: Sipariş Senkronizasyon Sistemi

## Mevcut Durum

| Bileşen | Durum |
|---------|-------|
| Webhook alıcı | ✅ Var |
| İmza doğrulama | ✅ Var |
| Idempotency | ✅ Var |
| Rate limiting | ❌ Yok |
| Batch polling | ❌ Yok |

## Çözüm: Hibrit Yaklaşım

```
┌─────────────────────────────────────────────────────────────────┐
│  KATMAN 1: WEBHOOK (Birincil - Anlık)                          │
│  ─────────────────────────────────────                          │
│  Trendyol → POST /api/webhook/trendyol/{sellerId}              │
│  • İmza doğrulama ✅                                            │
│  • Idempotency ✅                                               │
│  • 5 saniye kuralı ✅                                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  KATMAN 2: CATCH-UP POLLING (Yedek - Saatlik)                  │
│  ────────────────────────────────────────────                   │
│  • 2000 mağazayı 50'şerli batch'lere böl                       │
│  • Batch arası 90 saniye bekle                                  │
│  • Son 2 saati sorgula                                          │
│  • Rate limiting: max 10 req/saniye                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  KATMAN 3: FINANCIAL SYNC (6 saatte bir)                       │
│  ───────────────────────────────────────                        │
│  • Son 14 günü sorgula                                          │
│  • Gerçek komisyonları güncelle                                │
│  • Ürün commission_rate güncelle                               │
└─────────────────────────────────────────────────────────────────┘
```

## Rate Limiting Implementasyonu

### TrendyolRateLimiter.java (Yeni - Guava RateLimiter)
```java
@Component
public class TrendyolRateLimiter {
    // Saniyede max 10 istek (Trendyol API limiti)
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    /**
     * Non-blocking rate limiting.
     * Thread uyumaz, sadece gerektiğinde kısa süre bekler.
     */
    public void acquire() {
        rateLimiter.acquire();
    }
}
```

> **Not**: `Thread.sleep()` KULLANILMAMALI - thread'i bloke eder ve ölçeklenmez. Guava RateLimiter non-blocking çalışır.

### Batch Polling Scheduler
```java
@Scheduled(cron = "0 0 * * * ?") // Her saat başı
public void catchUpSync() {
    List<Store> stores = storeRepository.findAllActive();

    for (Store store : stores) {
        rateLimiter.acquire(); // Non-blocking bekler
        syncStore(store);
    }
}

private void syncStore(Store store) {
    // Son 2 saatin siparişlerini çek
    orderService.syncOrdersForHours(store.getId(), 2);
}
```

---

# MODÜL 3: Mağaza Onboarding Sistemi

## Akış: Yeni Mağaza Eklendiğinde

```
┌─────────────────────────────────────────────────────────────────┐
│  ADIM 1: Credential Doğrulama (Mevcut ✅)                       │
│  ────────────────────────────────────────                       │
│  POST /api/stores/test-credentials                              │
│  • Trendyol API test isteği                                    │
│  • Başarılı/Başarısız response                                 │
└──────────────────────────────┬──────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│  ADIM 2: Mağaza Kayıt (Mevcut ✅)                               │
│  ───────────────────────────────                                │
│  POST /api/stores                                               │
│  • Store entity oluştur                                        │
│  • Credentials şifrele ve kaydet                               │
└──────────────────────────────┬──────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│  ADIM 3: Webhook Kaydı (YENİ)                                  │
│  ────────────────────────────                                   │
│  POST Trendyol /suppliers/{id}/webhooks                        │
│  • Webhook URL kaydet                                          │
│  • Webhook ID'yi store'a kaydet                                │
│  • webhook_status = 'active'                                   │
└──────────────────────────────┬──────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│  ADIM 4: İlk Sync (YENİ - Asenkron)                            │
│  ──────────────────────────────────                             │
│  @Async başlat:                                                 │
│  1. Ürün sync (tüm ürünler)                                    │
│  2. Sipariş sync (son 30 gün)                                  │
│  3. Financial sync (son 30 gün)                                │
│  4. initial_sync_completed = true                              │
└─────────────────────────────────────────────────────────────────┘
```

## Veritabanı Değişiklikleri

### Migration: V46__add_store_onboarding_fields.sql

```sql
-- Webhook alanları
ALTER TABLE stores
ADD COLUMN IF NOT EXISTS webhook_id VARCHAR(255);

ALTER TABLE stores
ADD COLUMN IF NOT EXISTS webhook_status VARCHAR(50) DEFAULT 'pending';

ALTER TABLE stores
ADD COLUMN IF NOT EXISTS webhook_error_message TEXT;

-- Sync durumu alanları
ALTER TABLE stores
ADD COLUMN IF NOT EXISTS sync_status VARCHAR(50) DEFAULT 'pending';
-- Değerler: pending, SYNCING_PRODUCTS, SYNCING_ORDERS, SYNCING_FINANCIAL, COMPLETED, FAILED

ALTER TABLE stores
ADD COLUMN IF NOT EXISTS last_sync_at TIMESTAMP;

ALTER TABLE stores
ADD COLUMN IF NOT EXISTS initial_sync_completed BOOLEAN DEFAULT FALSE;

ALTER TABLE stores
ADD COLUMN IF NOT EXISTS sync_error_message TEXT;
```

## Kod Değişiklikleri

### 1. Store.java
```java
// Webhook alanları
private String webhookId;
private String webhookStatus; // pending, active, failed
private String webhookErrorMessage;

// Sync durumu alanları
private String syncStatus; // pending, SYNCING_PRODUCTS, SYNCING_ORDERS, SYNCING_FINANCIAL, COMPLETED, FAILED
private LocalDateTime lastSyncAt;
private Boolean initialSyncCompleted = false;
private String syncErrorMessage;
```

### 2. TrendyolWebhookRegistrationService.java (Yeni)
```java
@Service
public class TrendyolWebhookRegistrationService {

    /**
     * Webhook kaydı - hata olursa polling yedek olarak çalışır
     */
    public void registerWebhook(Store store) {
        String webhookUrl = webhookBaseUrl + "/api/webhook/trendyol/" + store.getSellerId();

        WebhookRequest request = WebhookRequest.builder()
            .url(webhookUrl)
            .subscribedStatuses(Arrays.asList(
                "Created", "Picking", "Invoiced",
                "Shipped", "Delivered", "Cancelled", "Returned"
            ))
            .build();

        try {
            String webhookId = trendyolClient.registerWebhook(store, request);
            store.setWebhookId(webhookId);
            store.setWebhookStatus("active");
        } catch (Exception e) {
            log.error("Webhook registration failed for store {}: {}", store.getId(), e.getMessage());
            store.setWebhookStatus("failed");
            store.setWebhookErrorMessage(e.getMessage());
            // Polling yedek olarak çalışmaya devam eder
        }
        storeRepository.save(store);
    }

    /**
     * Webhook silme - mağaza silindiğinde çağrılır
     */
    public void unregisterWebhook(Store store) {
        if (store.getWebhookId() != null) {
            try {
                trendyolClient.deleteWebhook(store, store.getWebhookId());
            } catch (Exception e) {
                log.warn("Webhook deletion failed for store {}: {}", store.getId(), e.getMessage());
            }
            store.setWebhookId(null);
            store.setWebhookStatus("inactive");
            storeRepository.save(store);
        }
    }
}
```

### 3. StoreOnboardingService.java (Yeni)
```java
@Service
public class StoreOnboardingService {

    /**
     * Asenkron ilk sync - adım adım status takibi ile
     * Frontend bu status'ları okuyarak kullanıcıya progress gösterir
     */
    @Async
    public void performInitialSync(Store store) {
        try {
            // 1. Ürün sync
            store.setSyncStatus("SYNCING_PRODUCTS");
            storeRepository.save(store);
            productService.syncAllProducts(store.getId());

            // 2. Sipariş sync
            store.setSyncStatus("SYNCING_ORDERS");
            storeRepository.save(store);
            orderService.syncOrdersForDays(store.getId(), 30);

            // 3. Finansal sync
            store.setSyncStatus("SYNCING_FINANCIAL");
            storeRepository.save(store);
            financialService.syncSettlementsForDays(store.getId(), 30);

            // 4. Tamamlandı
            store.setSyncStatus("COMPLETED");
            store.setInitialSyncCompleted(true);
            store.setLastSyncAt(LocalDateTime.now());

        } catch (Exception e) {
            store.setSyncStatus("FAILED");
            store.setSyncErrorMessage(e.getMessage());
            log.error("Initial sync failed for store {}: {}", store.getId(), e.getMessage());
        } finally {
            storeRepository.save(store);
        }
    }
}
```

**Frontend Gösterimi**:
| sync_status | Kullanıcıya Gösterilen Mesaj |
|-------------|------------------------------|
| pending | "Senkronizasyon bekleniyor..." |
| SYNCING_PRODUCTS | "Ürünler senkronize ediliyor... ⏳" |
| SYNCING_ORDERS | "Siparişler senkronize ediliyor... ⏳" |
| SYNCING_FINANCIAL | "Finansal veriler senkronize ediliyor... ⏳" |
| COMPLETED | "Senkronizasyon tamamlandı ✅" |
| FAILED | "Hata: [sync_error_message] ❌" |

---

# UYGULAMA PLANI

## Faz 1: Komisyon Sistemi (Öncelik: YÜKSEK)

| # | Görev | Dosya | Süre |
|---|-------|-------|------|
| 1.1 | Migration oluştur | V45__add_commission_tracking_fields.sql | 10 dk |
| 1.2 | Entity güncelle | TrendyolProduct.java, TrendyolOrder.java | 15 dk |
| 1.3 | Order sync güncelle | TrendyolOrderService.java | 30 dk |
| 1.4 | Financial sync güncelle | TrendyolFinancialSettlementService.java | 45 dk |
| 1.5 | Test | Manual test | 30 dk |

**Toplam: ~2 saat**

## Faz 2: Rate Limiting + Batch Polling (Öncelik: YÜKSEK)

| # | Görev | Dosya | Süre |
|---|-------|-------|------|
| 2.1 | Rate limiter oluştur | TrendyolRateLimiter.java | 30 dk |
| 2.2 | Batch scheduler | TrendyolOrderScheduledService.java | 45 dk |
| 2.3 | Financial batch | TrendyolFinancialSettlementScheduledService.java | 30 dk |
| 2.4 | Test | Manual test | 30 dk |

**Toplam: ~2 saat**

## Faz 3: Webhook Kayıt Servisi (Öncelik: ORTA)

| # | Görev | Dosya | Süre |
|---|-------|-------|------|
| 3.1 | Migration oluştur | V46__add_store_onboarding_fields.sql | 10 dk |
| 3.2 | Entity güncelle | Store.java | 15 dk |
| 3.3 | Webhook kayıt servisi | TrendyolWebhookRegistrationService.java | 1 saat |
| 3.4 | Store service güncelle | StoreService.java | 30 dk |
| 3.5 | Test | Manual test | 30 dk |

**Toplam: ~2.5 saat**

## Faz 4: İlk Sync Servisi (Öncelik: ORTA)

| # | Görev | Dosya | Süre |
|---|-------|-------|------|
| 4.1 | Onboarding servisi | StoreOnboardingService.java | 1 saat |
| 4.2 | Async config | AsyncConfig.java | 15 dk |
| 4.3 | Frontend status | Sync durumu gösterimi | 1 saat |
| 4.4 | Test | End-to-end test | 30 dk |

**Toplam: ~3 saat**

---

# TOPLAM SÜRE TAHMİNİ

| Faz | Süre |
|-----|------|
| Faz 1: Komisyon Sistemi | ~2 saat |
| Faz 2: Rate Limiting + Batch | ~2 saat |
| Faz 3: Webhook Kayıt | ~2.5 saat |
| Faz 4: İlk Sync | ~3 saat |
| **TOPLAM** | **~9.5 saat** |

---

# ÖNCELİK SIRASI

```
1. [YÜKSEK] Komisyon Sistemi → Dashboard'da doğru veri gösterilmesi
2. [YÜKSEK] Rate Limiting → Trendyol ban riski engelleme
3. [ORTA] Webhook Kayıt → Otomatik mağaza kurulumu
4. [ORTA] İlk Sync → Kullanıcı deneyimi iyileştirme
```

---

# BAŞARI KRİTERLERİ

| Kriter | Hedef |
|--------|-------|
| Komisyon doğruluğu | Tahmini ±%5 sapma |
| Webhook başarı oranı | >%99 |
| Polling catch-up | Kaçırılan siparişlerin %100'ü yakalanmalı |
| İlk sync süresi | <10 dakika / mağaza |
| Rate limit ihlali | 0 |

---

# İLGİLİ DOKÜMANLAR

- [Komisyon Sistemi Detayları](./COMMISSION_SYSTEM.md)
- [Sipariş Sync Sistemi Detayları](./ORDER_SYNC_SYSTEM.md)

---

## Tarihçe

| Tarih | Değişiklik |
|-------|------------|
| 2026-01-18 | Master plan oluşturuldu |
| 2026-01-18 | Komisyon formülü güncellendi (vatBaseAmount kullan, 0.8 kaldırıldı) |
| 2026-01-18 | V45 migration'a performans index'leri eklendi |
| 2026-01-18 | Thread.sleep → Guava RateLimiter değiştirildi |
| 2026-01-18 | Sync status takibi eklendi (adım adım progress) |
| 2026-01-18 | Webhook hata yönetimi ve silme fonksiyonu eklendi |
