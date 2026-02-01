# Trendyol Webhook Entegrasyonu

Bu dokümantasyon SellerX platformundaki Trendyol webhook sisteminin teknik detaylarını açıklar.

## Genel Bakış

Webhook sistemi, Trendyol'dan gelen sipariş bildirimlerini gerçek zamanlı olarak alır ve işler. Polling (15 dakika gecikme) yerine event-driven mimari kullanarak siparişler 2-3 saniye içinde sisteme eklenir.

```
┌─────────────┐     Sipariş      ┌─────────────┐     2-3 sn      ┌─────────────┐
│  Trendyol   │ ───────────────▶ │   SellerX   │ ─────────────▶  │  Database   │
│   Server    │    Webhook       │   Backend   │    İşleme       │             │
└─────────────┘                  └─────────────┘                 └─────────────┘
```

## Mimari

### Backend Yapısı

```
sellerx-backend/src/main/java/com/ecommerce/sellerx/webhook/
├── TrendyolWebhookController.java      # Webhook receiver endpoint
├── TrendyolWebhookService.java         # Sipariş işleme mantığı
├── TrendyolWebhookManagementService.java # Trendyol API iletişimi
├── TrendyolWebhookPayload.java         # 150+ field veri modeli
├── WebhookEvent.java                   # JPA entity
├── WebhookEventRepository.java         # Repository interface
├── WebhookSignatureValidator.java      # Güvenlik servisi
├── WebhookSecurityRules.java           # Public endpoint güvenlik kuralları
└── WebhookManagementController.java    # Yönetim API'si
```

### Frontend Yapısı

```
sellerx-frontend/
├── app/api/stores/[id]/webhooks/    # [id] kullanılıyor (Next.js route convention)
│   ├── status/route.ts
│   ├── enable/route.ts
│   ├── disable/route.ts
│   ├── events/route.ts
│   └── test/route.ts
├── hooks/queries/use-webhooks.ts
├── components/settings/webhook-settings.tsx
└── lib/api/client.ts (webhookApi)
```

> **Not**: Frontend route'larında `[id]` kullanılıyor çünkü `stores/[id]/route.ts` zaten mevcut. Next.js aynı path'te farklı parametre isimlerine izin vermiyor.

## Veritabanı Şeması

### webhook_events Tablosu

```sql
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,       -- Idempotency key
    store_id UUID NOT NULL REFERENCES stores(id),
    seller_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,             -- order.created, order.status.changed
    order_number VARCHAR(255),
    status VARCHAR(50),
    payload TEXT,                                 -- Raw JSON payload
    processing_status VARCHAR(20) NOT NULL,      -- RECEIVED, PROCESSING, COMPLETED, FAILED, DUPLICATE
    error_message TEXT,
    processing_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_webhook_event_id ON webhook_events(event_id);
CREATE INDEX idx_webhook_store_created ON webhook_events(store_id, created_at DESC);
```

## API Endpoints

### Webhook Receiver

```
POST /api/webhook/trendyol/{sellerId}
```

**Headers:**
- `Content-Type: application/json`
- `X-Trendyol-Signature: <HMAC-SHA256 signature>` (optional)
- `X-API-Key: <API key>` (optional)

**Response:**
- `200 OK` - Her durumda (retry önlemek için)

### Yönetim API'leri

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/api/stores/{storeId}/webhooks/status` | Webhook durumu ve istatistikler |
| POST | `/api/stores/{storeId}/webhooks/enable` | Trendyol'a webhook kaydet |
| POST | `/api/stores/{storeId}/webhooks/disable` | Trendyol'dan webhook sil |
| GET | `/api/stores/{storeId}/webhooks/events` | Olay geçmişi (sayfalı) |
| POST | `/api/stores/{storeId}/webhooks/test` | Test olayı oluştur |

### Status Response

```json
{
  "storeId": "uuid",
  "webhookId": "trendyol-webhook-id",
  "enabled": true,
  "webhookUrl": "/api/webhook/trendyol/12345",
  "eventStats": {
    "COMPLETED": 150,
    "FAILED": 2,
    "DUPLICATE": 5
  },
  "totalEvents": 157
}
```

### Events Response (Paginated)

```json
{
  "content": [
    {
      "id": "uuid",
      "eventId": "sha256-hash",
      "storeId": "uuid",
      "sellerId": "12345",
      "eventType": "order.created",
      "orderNumber": "1234567890",
      "status": "Created",
      "processingStatus": "COMPLETED",
      "processingTimeMs": 45,
      "createdAt": "2026-01-15T14:30:00",
      "processedAt": "2026-01-15T14:30:00"
    }
  ],
  "totalElements": 157,
  "totalPages": 8,
  "size": 20,
  "number": 0
}
```

## Güvenlik

### HMAC-SHA256 Signature Doğrulama

```java
public boolean validateSignature(String signature, String payload, String sellerId) {
    if (!signatureValidationEnabled) {
        return true; // Dev ortamında devre dışı
    }

    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec key = new SecretKeySpec(signatureSecret.getBytes(), "HmacSHA256");
    mac.init(key);

    byte[] hmacBytes = mac.doFinal(payload.getBytes());
    String expectedSignature = Base64.getEncoder().encodeToString(hmacBytes);

    // Timing-safe karşılaştırma
    return MessageDigest.isEqual(
        signature.getBytes(),
        expectedSignature.getBytes()
    );
}
```

### Idempotency

Event ID, şu bilgilerden SHA-256 hash ile oluşturulur:
- sellerId
- orderNumber
- status
- lastModifiedDate (timestamp)

```java
String data = String.format("%s:%s:%s:%d", sellerId, orderNumber, status, timestamp);
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(data.getBytes());
return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
```

## Konfigürasyon

### application.yaml

```yaml
app:
  webhook:
    enabled: true                          # Webhook receiver aktif
    base-url: ${WEBHOOK_BASE_URL:http://localhost:8080}
    api-key: ${WEBHOOK_API_KEY:sellerx-webhook-key}
    signature-secret: ${WEBHOOK_SIGNATURE_SECRET:}
    signature-validation-enabled: false    # Production'da true yapılmalı
```

### Environment Variables

| Variable | Açıklama | Default |
|----------|----------|---------|
| `WEBHOOK_BASE_URL` | Webhook endpoint base URL | `http://localhost:8080` |
| `WEBHOOK_API_KEY` | API key (opsiyonel) | `sellerx-webhook-key` |
| `WEBHOOK_SIGNATURE_SECRET` | HMAC secret key | (empty) |

## İşlem Akışı

```
1. Trendyol POST /api/webhook/trendyol/{sellerId}
   │
2. Signature Doğrulama (opsiyonel)
   │ ❌ 401 Unauthorized
   │ ✅
3. JSON Parse
   │
4. Store Bulma (sellerId -> storeId)
   │ ❌ 200 OK "Store not found"
   │ ✅
5. Event ID Oluşturma (SHA-256)
   │
6. Idempotency Kontrolü
   │ ❌ DUPLICATE olarak kaydet, 200 OK
   │ ✅
7. Event Log Oluştur (status: PROCESSING)
   │
8. Sipariş İşleme
   │ ❌ status: FAILED, error_message kaydet
   │ ✅ status: COMPLETED
   │
9. Event Log Güncelle
   │
10. 200 OK Dön
```

## Frontend Kullanımı

### React Query Hooks

```typescript
import {
  useWebhookStatus,
  useWebhookEvents,
  useEnableWebhooks,
  useDisableWebhooks,
  useTestWebhook,
} from "@/hooks/queries/use-webhooks";

// Durum sorgulama
const { data: status, isLoading } = useWebhookStatus(storeId);

// Olayları listeleme
const { data: events } = useWebhookEvents(storeId, page, size);

// Webhook etkinleştirme
const enableMutation = useEnableWebhooks();
await enableMutation.mutateAsync(storeId);

// Webhook devre dışı bırakma
const disableMutation = useDisableWebhooks();
await disableMutation.mutateAsync(storeId);

// Test olayı oluşturma
const testMutation = useTestWebhook();
await testMutation.mutateAsync(storeId);
```

## Hata Senaryoları

| Senaryo | Davranış |
|---------|----------|
| Invalid signature | 401 Unauthorized |
| Store not found | 200 OK (log: warning) |
| Duplicate event | 200 OK, DUPLICATE olarak kaydet |
| Processing error | 200 OK, FAILED olarak kaydet |
| Timeout (>5s) | Trendyol retry yapabilir |

## Monitoring

### Log Formatı

```
INFO  Received webhook for seller: 12345 with order: 1234567890 and status: Created
INFO  Duplicate webhook received, eventId: abc123...
INFO  Enabled webhook xyz789 for store uuid
ERROR Error processing webhook: Connection refused
```

### Metrikler

Dashboard'da görüntülenen metrikler:
- Toplam olay sayısı
- Başarılı işlem sayısı
- Başarısız işlem sayısı
- Duplicate sayısı
- Ortalama işlem süresi

## Troubleshooting

### Webhook çalışmıyor

1. `app.webhook.enabled: true` kontrol et
2. Store'un `webhookId` değerinin dolu olduğunu kontrol et
3. Trendyol entegrasyonunun aktif olduğunu doğrula

### Siparişler gelmiyor

1. `/api/stores/{storeId}/webhooks/events` endpoint'inden logları kontrol et
2. `processingStatus: FAILED` olan kayıtların `errorMessage` alanını incele
3. Trendyol Seller Panel'den webhook durumunu kontrol et

### Signature hatası

1. `WEBHOOK_SIGNATURE_SECRET` environment variable'ının doğru ayarlandığını kontrol et
2. Trendyol'daki webhook ayarlarında secret key'i doğrula
3. Development'da `signature-validation-enabled: false` yapılabilir
