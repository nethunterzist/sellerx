# Sipariş Senkronizasyon Sistemi

Bu doküman, SellerX platformundaki sipariş çekme ve senkronizasyon sistemini açıklar.

## Genel Bakış

Sistem **hibrit yaklaşım** kullanır:
1. **Webhook** (Birincil) - Anlık sipariş bildirimleri
2. **Polling** (Yedek) - Kaçırılan siparişleri yakalama
3. **Financial Sync** - Komisyon verisi güncelleme

---

## Ölçek Hedefi

| Metrik | Değer |
|--------|-------|
| Kullanıcı sayısı | ~1,000 |
| Mağaza sayısı | ~2,000 (kullanıcı başı ~2 mağaza) |
| Günlük sipariş (mağaza başı) | ~1,000 |
| Toplam günlük sipariş | ~2,000,000 |

---

## Katman 1: Webhook (Birincil - Anlık)

### Trendyol Webhook API

Trendyol resmi webhook desteği sunmaktadır.

**Webhook Kayıt:**
```
POST https://api.trendyol.com/sapigw/suppliers/{supplierId}/webhooks
Authorization: Basic {base64(apiKey:apiSecret)}
Content-Type: application/json

{
  "url": "https://sellerx.com/api/webhook/trendyol/{sellerId}",
  "subscribedStatuses": ["Created", "Picking", "Invoiced", "Shipped", "Delivered", "Cancelled", "Returned"]
}
```

**Desteklenen Durumlar:**

| Status | Açıklama |
|--------|----------|
| Created | Yeni sipariş oluştu |
| Picking | Hazırlanıyor |
| Invoiced | Faturalandı |
| Shipped | Kargoya verildi |
| Delivered | Teslim edildi |
| Cancelled | İptal edildi |
| Returned | İade edildi |

### Mevcut Webhook Altyapısı

| Dosya | Rol |
|-------|-----|
| `TrendyolWebhookController.java` | Webhook alıcı endpoint |
| `TrendyolWebhookService.java` | Sipariş işleme |
| `WebhookSignatureValidator.java` | İmza doğrulama (HMAC-SHA256) |
| `WebhookEvent.java` | Audit log entity |
| `webhook_events` tablosu | Idempotency ve log |

### Webhook Endpoint

```
POST /api/webhook/trendyol/{sellerId}

Headers:
  X-Trendyol-Signature: {hmac-sha256}
  X-API-Key: {optional}

Response: 200 OK (5 saniye içinde - Trendyol gereksinimi)
```

### Güvenlik Özellikleri

1. **İmza Doğrulama**: HMAC-SHA256 ile payload doğrulama
2. **Idempotency**: `event_id` ile duplicate engelleme
3. **Audit Log**: Tüm webhook'lar `webhook_events` tablosunda
4. **5 Saniye Kuralı**: Trendyol retry'ı engellemek için hızlı response

---

## Katman 2: Catch-up Polling (Yedek)

### Neden Gerekli?

- Webhook kaçırılabilir (sunucu down, network sorunu)
- Trendyol webhook retry'ı sınırlı
- Veri tutarlılığı için yedek mekanizma

### Mevcut Scheduler

```java
// TrendyolOrderScheduledService.java
@Scheduled(cron = "0 15 6 * * ?", zone = "Europe/Istanbul")
public void syncOrdersForAllTrendyolStores() {
    // Günde 1 kez, sabah 06:15
}
```

### Önerilen Akıllı Scheduler (2000 Mağaza İçin)

**Guava RateLimiter Kullanımı**:

```java
@Component
public class TrendyolRateLimiter {
    // Saniyede max 10 istek (Trendyol rate limit)
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    public void acquire() {
        rateLimiter.acquire(); // Non-blocking, gerektiğinde bekler
    }
}

// TrendyolOrderScheduledService.java
@Service
@RequiredArgsConstructor
public class TrendyolOrderScheduledService {

    private final TrendyolRateLimiter rateLimiter;
    private final StoreRepository storeRepository;
    private final TrendyolOrderService orderService;

    // Her 1 saatte bir çalışır
    @Scheduled(cron = "0 0 * * * ?", zone = "Europe/Istanbul")
    public void catchUpSync() {
        List<Store> stores = storeRepository.findAllActive();

        for (Store store : stores) {
            rateLimiter.acquire(); // Thread uyumaz, rate limit'e uyar
            try {
                orderService.syncLastTwoHours(store);
            } catch (Exception e) {
                log.error("Catch-up sync failed for store {}: {}", store.getId(), e.getMessage());
                // Hata olsa bile diğer mağazalara devam et
            }
        }
    }
}
```

> **Neden RateLimiter?**: `Thread.sleep()` thread'i tamamen bloke eder ve ölçeklenmez. RateLimiter ise non-blocking çalışır ve Trendyol'un rate limit'ine otomatik uyum sağlar.

### Rate Limiting Hesabı

```
2000 mağaza × 0.1 saniye/istek = 200 saniye ≈ 3.3 dakika

Sonuç: Tüm mağazalar 3-4 dakikada kontrol edilir (saatte 1 kez tetiklenir)
```

> **Not**: Kullanıcılara manuel "Senkronize Et" butonu **VERİLMEZ**. Sistem otomatik olarak webhook + saatlik polling ile verileri güncel tutar. Bu yaklaşım büyük SaaS platformlarının (Shopify, Trendyol, Parasut) standardıdır.

---

## Katman 3: Financial Sync (Komisyon Güncelleme)

### Amaç

- Gerçek komisyon verilerini çekmek
- Tahmini komisyonları gerçek değerlerle güncellemek
- Ürünlerin `last_commission_rate` alanını güncellemek

### Mevcut Scheduler

```java
// TrendyolFinancialSettlementScheduledService.java
@Scheduled(cron = "0 0 2 * * *") // Gece 02:00
@Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 saatte bir
```

### Financial API Penceresi

- **Sorgu penceresi**: Son 14 gün
- **Neden 14 gün?**: Sipariş → Teslimat → Ödeme işleme = 1-7 gün gecikme

---

## Yeni Mağaza Onboarding Akışı

### Adım 1: Credential Doğrulama (Mevcut ✅)

```java
// TrendyolService.java
public boolean testCredentials(String sellerId, String apiKey, String apiSecret) {
    // Trendyol API'ye test isteği
}
```

### Adım 2: Webhook Kaydı (YENİ)

```java
// TrendyolWebhookRegistrationService.java (oluşturulacak)
@Service
@RequiredArgsConstructor
public class TrendyolWebhookRegistrationService {

    private final TrendyolClient trendyolClient;
    private final StoreRepository storeRepository;

    @Value("${webhook.base-url}")
    private String webhookBaseUrl;

    /**
     * Webhook kaydet - hata olursa polling yedek olarak çalışmaya devam eder
     */
    public void registerWebhook(Store store) {
        String webhookUrl = webhookBaseUrl + "/api/webhook/trendyol/" + store.getSellerId();

        try {
            String webhookId = trendyolClient.registerWebhook(store, webhookUrl);
            store.setWebhookId(webhookId);
            store.setWebhookStatus("active");
            store.setWebhookErrorMessage(null);
        } catch (Exception e) {
            // Webhook başarısız olsa bile sistem çalışır (polling yedek)
            store.setWebhookStatus("failed");
            store.setWebhookErrorMessage(e.getMessage());
            log.warn("Webhook registration failed for store {}: {}. Polling will be used as fallback.",
                store.getId(), e.getMessage());
        }
        storeRepository.save(store);
    }

    /**
     * Webhook sil - mağaza silindiğinde veya credential değiştiğinde
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

> **Önemli**: Webhook kaydı başarısız olsa bile sistem çalışmaya devam eder. Saatlik polling yedek mekanizma olarak tüm siparişleri yakalar.

### Adım 3: İlk Sync (Asenkron)

```java
// StoreOnboardingService.java (oluşturulacak)
@Async
public void performInitialSync(Store store) {
    try {
        // 1. Ürün sync
        store.setSyncStatus("SYNCING_PRODUCTS");
        storeRepository.save(store);
        productService.syncAllProducts(store.getId());

        // 2. Sipariş sync (son 30 gün)
        store.setSyncStatus("SYNCING_ORDERS");
        storeRepository.save(store);
        orderService.syncOrdersForDays(store.getId(), 30);

        // 3. Financial sync (son 30 gün)
        store.setSyncStatus("SYNCING_FINANCIAL");
        storeRepository.save(store);
        financialService.syncSettlements(store.getId(), 30);

        // Tamamlandı
        store.setSyncStatus("COMPLETED");
        store.setInitialSyncCompleted(true);

    } catch (Exception e) {
        store.setSyncStatus("FAILED");
        store.setSyncErrorMessage(e.getMessage());
        log.error("Initial sync failed for store {}: {}", store.getId(), e.getMessage());
    } finally {
        storeRepository.save(store);
    }
}
```

**Frontend Gösterimi**:

| Status | Mesaj |
|--------|-------|
| SYNCING_PRODUCTS | "Ürünler senkronize ediliyor... ⏳" |
| SYNCING_ORDERS | "Siparişler senkronize ediliyor... ⏳" |
| SYNCING_FINANCIAL | "Finansal veriler senkronize ediliyor... ⏳" |
| COMPLETED | "Senkronizasyon tamamlandı ✅" |
| FAILED | "Hata: [detay] ❌" |

---

## Veritabanı Değişiklikleri

### stores Tablosu (V46 Migration)

```sql
-- Migration: V46__add_store_sync_tracking.sql

-- Webhook tracking
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_id VARCHAR(255);
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_status VARCHAR(50) DEFAULT 'pending';
-- webhook_status: pending, active, failed, inactive
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_error_message TEXT;

-- Sync tracking
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_status VARCHAR(50) DEFAULT 'pending';
-- sync_status: pending, SYNCING_PRODUCTS, SYNCING_ORDERS, SYNCING_FINANCIAL, COMPLETED, FAILED
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_error_message TEXT;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS last_sync_at TIMESTAMP;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS initial_sync_completed BOOLEAN DEFAULT FALSE;
```

---

## Hata Yönetimi

### Webhook Kayıt Hataları

| Hata | Aksiyon | Sistem Durumu |
|------|---------|---------------|
| API bağlantı hatası | Log + webhook_status='failed' | Polling devam eder ✅ |
| Auth hatası (401/403) | Log + webhook_status='failed' + kullanıcıya bildir | Polling devam eder ✅ |
| Geçersiz URL | Log + webhook_status='failed' | Polling devam eder ✅ |

> **Önemli**: Webhook kaydı başarısız olsa bile **sistem çalışmaya devam eder**. Saatlik polling tüm siparişleri yakalar.

### Webhook Alıcı Hataları

| Hata | Aksiyon |
|------|---------|
| İmza doğrulama başarısız | Log + 401 döndür |
| Store bulunamadı | Log + 200 OK (retry engelle) |
| İşleme hatası | Log + 200 OK (catch-up'a bırak) |
| Duplicate webhook | Log + 200 OK |

### Polling Hataları

| Hata | Aksiyon |
|------|---------|
| API timeout | Sonraki mağazaya geç |
| Rate limit (429) | RateLimiter otomatik yönetir |
| Auth hatası | Store'u disable et, kullanıcıya bildir |
| Genel hata | Log, sonraki mağazaya geç |

---

## Monitoring Metrikleri

| Metrik | Açıklama |
|--------|----------|
| `webhook.received.count` | Alınan webhook sayısı |
| `webhook.processed.count` | İşlenen webhook sayısı |
| `webhook.duplicate.count` | Duplicate webhook sayısı |
| `webhook.failed.count` | Başarısız webhook sayısı |
| `webhook.processing.time` | İşleme süresi (ms) |
| `polling.batch.count` | Polling batch sayısı |
| `polling.orders.synced` | Polling ile çekilen sipariş |
| `financial.sync.count` | Financial sync sayısı |

---

## Uygulama Planı

### Faz 1: Temel İyileştirmeler
1. ✅ Webhook altyapısı (mevcut)
2. ⏳ Rate limiting ekle
3. ⏳ Batch polling implementasyonu

### Faz 2: Otomatik Onboarding
4. ⏳ Webhook kayıt servisi
5. ⏳ İlk sync servisi
6. ⏳ Store status tracking

### Faz 3: Ölçeklendirme
7. ⏳ Queue sistemi (opsiyonel - Redis/RabbitMQ)
8. ⏳ Horizontal scaling desteği
9. ⏳ Monitoring dashboard

---

## Kaynaklar

- [Trendyol Developers - Webhook](https://developers.trendyol.com/docs/marketplace/webhook/webhook-model)
- [Trendyol PHP API](https://github.com/ismail0234/trendyol-php-api) - Topluluk kütüphanesi

---

## Tarihçe

| Tarih | Değişiklik |
|-------|------------|
| 2026-01-18 | İlk doküman oluşturuldu |
| 2026-01-18 | 2000 mağaza ölçeklendirme planı eklendi |
| 2026-01-18 | Thread.sleep → Guava RateLimiter değişikliği |
| 2026-01-18 | Manuel sync kaldırıldı (otomatik webhook + polling) |
| 2026-01-18 | StoreOnboardingService'e sync_status takibi eklendi |
| 2026-01-18 | Webhook hata yönetimi ve silme (unregister) eklendi |
| 2026-01-18 | V46 migration güncellendi (sync_status, webhook_error_message) |
