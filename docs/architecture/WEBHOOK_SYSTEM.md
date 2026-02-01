# Webhook Sistemi

> Trendyol'dan gerçek zamanlı sipariş bildirimleri alma altyapısı.

## Genel Bakış

Webhook sistemi, Trendyol'dan gelen sipariş güncellemelerini gerçek zamanlı olarak alır ve işler. Bu sayede kullanıcılar yeni siparişleri anında görebilir.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         WEBHOOK AKIŞI                                   │
└─────────────────────────────────────────────────────────────────────────┘

    Trendyol                    SellerX Backend                 Database
        │                              │                            │
        │  POST /webhook/trendyol      │                            │
        │      /{sellerId}             │                            │
        │─────────────────────────────▶│                            │
        │                              │                            │
        │                       ┌──────┴──────┐                     │
        │                       │  Signature  │                     │
        │                       │   Check     │                     │
        │                       └──────┬──────┘                     │
        │                              │                            │
        │                       ┌──────┴──────┐                     │
        │                       │ Idempotency │                     │
        │                       │   Check     │                     │
        │                       └──────┬──────┘                     │
        │                              │                            │
        │                       ┌──────┴──────┐                     │
        │                       │   Process   │                     │
        │                       │    Event    │─────────────────────▶│
        │                       └──────┬──────┘                     │
        │                              │                            │
        │      200 OK                  │                            │
        │◀─────────────────────────────│                            │
        │                              │                            │
```

---

## Webhook URL Formatı

```
POST https://your-domain.com/api/webhook/trendyol/{sellerId}

Örnek:
POST https://sellerx.app/api/webhook/trendyol/123456
```

---

## Webhook Event Payload

### Sipariş Event'i

```json
{
  "eventType": "OrderStatusChanged",
  "eventId": "evt_abc123def456",
  "timestamp": "2026-01-18T10:30:00Z",
  "sellerId": "123456",
  "data": {
    "orderNumber": "1234567890",
    "status": "Created",
    "orderDate": "2026-01-18T10:25:00Z",
    "lines": [
      {
        "lineId": "line_001",
        "barcode": "8680000123456",
        "productName": "Örnek Ürün",
        "quantity": 2,
        "amount": 199.99,
        "vatBaseAmount": 169.49,
        "discount": 0
      }
    ],
    "shipmentAddress": {
      "city": "İstanbul",
      "district": "Kadıköy",
      "fullAddress": "..."
    }
  }
}
```

---

## Backend Implementasyonu

### 1. WebhookSecurityRules (Public Endpoint)

```java
package com.ecommerce.sellerx.webhook;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

/**
 * Webhook endpoint'leri için security kuralları.
 * Bu endpoint'ler JWT gerektirmez.
 */
@Configuration
public class WebhookSecurityRules {

    @Bean
    @Order(1)  // Ana security config'den önce çalışır
    public SecurityFilterChain webhookSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/webhook/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // Public erişim
            );
        return http.build();
    }
}
```

### 2. TrendyolWebhookController

```java
@RestController
@RequestMapping("/api/webhook/trendyol")
@RequiredArgsConstructor
@Slf4j
public class TrendyolWebhookController {

    private final TrendyolWebhookService webhookService;
    private final WebhookSignatureValidator signatureValidator;

    /**
     * Trendyol webhook receiver.
     * Trendyol 5 saniye içinde 200 OK bekler.
     */
    @PostMapping("/{sellerId}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable String sellerId,
            @RequestBody String payload,
            @RequestHeader(value = "X-Trendyol-Signature", required = false) String signature,
            @RequestHeader(value = "X-Event-Id", required = false) String eventId) {

        log.info("Received webhook for seller: {}, eventId: {}", sellerId, eventId);

        try {
            // 1. Signature doğrulama (opsiyonel)
            if (!signatureValidator.isValid(payload, signature)) {
                log.warn("Invalid webhook signature for seller: {}", sellerId);
                // Yine de 200 döndür (Trendyol retry yapmasın)
                return ResponseEntity.ok("OK");
            }

            // 2. Async olarak işle (5 saniye limitine uymak için)
            webhookService.processWebhookAsync(sellerId, payload, eventId);

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing webhook for seller: {}", sellerId, e);
            // Hata olsa bile 200 döndür
            return ResponseEntity.ok("OK");
        }
    }
}
```

### 3. TrendyolWebhookService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolWebhookService {

    private final WebhookEventRepository eventRepository;
    private final TrendyolOrderService orderService;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Webhook'u async olarak işler.
     */
    @Async
    public void processWebhookAsync(String sellerId, String payload, String eventId) {
        try {
            // 1. Idempotency check - aynı event'i tekrar işleme
            if (eventId != null && eventRepository.existsByEventId(eventId)) {
                log.info("Duplicate webhook event: {}", eventId);
                return;
            }

            // 2. Store'u bul
            Store store = storeRepository.findByCredentialsSellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + sellerId));

            // 3. Payload'ı parse et
            WebhookPayload webhookPayload = objectMapper.readValue(payload, WebhookPayload.class);

            // 4. Event'i kaydet (idempotency için)
            saveWebhookEvent(eventId, sellerId, webhookPayload.getEventType(), payload);

            // 5. Event tipine göre işle
            switch (webhookPayload.getEventType()) {
                case "OrderStatusChanged":
                case "OrderCreated":
                    processOrderEvent(store, webhookPayload);
                    break;
                default:
                    log.info("Unhandled webhook event type: {}", webhookPayload.getEventType());
            }

        } catch (Exception e) {
            log.error("Failed to process webhook: sellerId={}, eventId={}", sellerId, eventId, e);
        }
    }

    private void processOrderEvent(Store store, WebhookPayload payload) {
        // Siparişi güncelle veya oluştur
        orderService.upsertOrderFromWebhook(store.getId(), payload.getData());
    }

    private void saveWebhookEvent(String eventId, String sellerId, String eventType, String payload) {
        WebhookEvent event = new WebhookEvent();
        event.setEventId(eventId);
        event.setSellerId(sellerId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setReceivedAt(LocalDateTime.now());
        eventRepository.save(event);
    }
}
```

### 4. WebhookSignatureValidator

```java
@Component
public class WebhookSignatureValidator {

    @Value("${webhook.signature.secret:}")
    private String signatureSecret;

    public boolean isValid(String payload, String signature) {
        // Secret yoksa validation atla
        if (signatureSecret == null || signatureSecret.isEmpty()) {
            return true;
        }

        if (signature == null || signature.isEmpty()) {
            return false;
        }

        try {
            // HMAC-SHA256 ile imza doğrula
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                signatureSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## Veritabanı

### WebhookEvent Entity

```java
@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", unique = true)
    private String eventId;  // Idempotency key

    @Column(name = "seller_id")
    private String sellerId;

    @Column(name = "event_type")
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "status")
    private String status = "received";  // received, processed, failed
}
```

### Migration

```sql
-- V20__create_webhook_events_table.sql

CREATE TABLE IF NOT EXISTS webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) UNIQUE,
    seller_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100),
    payload TEXT,
    received_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'received'
);

CREATE INDEX idx_webhook_events_seller ON webhook_events(seller_id);
CREATE INDEX idx_webhook_events_event_id ON webhook_events(event_id);
CREATE INDEX idx_webhook_events_received ON webhook_events(received_at);
```

---

## Webhook Yönetimi

### Webhook Oluşturma

```java
@Service
public class TrendyolWebhookManagementService {

    public WebhookResult createWebhookForStore(MarketplaceCredentials credentials) {
        // Webhook'lar global olarak disable edilmişse
        if (webhooksDisabled) {
            return WebhookResult.disabled();
        }

        try {
            String webhookUrl = webhookBaseUrl + "/api/webhook/trendyol/" + credentials.getSellerId();

            // Trendyol API'ye webhook kaydet
            WebhookRegistration registration = trendyolClient.registerWebhook(
                credentials,
                webhookUrl,
                List.of("OrderStatusChanged", "OrderCreated")
            );

            return WebhookResult.success(registration.getWebhookId());

        } catch (Exception e) {
            return WebhookResult.failure(e.getMessage());
        }
    }
}
```

### WebhookResult Pattern

```java
public class WebhookResult {
    private final boolean success;
    private final boolean disabled;
    private final String webhookId;
    private final String errorMessage;

    public static WebhookResult success(String webhookId) {
        return new WebhookResult(true, false, webhookId, null);
    }

    public static WebhookResult failure(String errorMessage) {
        return new WebhookResult(false, false, null, errorMessage);
    }

    public static WebhookResult disabled() {
        return new WebhookResult(false, true, null, "Webhooks are disabled globally");
    }

    // Getters...
}
```

---

## Store'daki Webhook Status

### Entity Alanları

```java
@Entity
@Table(name = "stores")
public class Store {

    @Column(name = "webhook_id")
    private String webhookId;

    @Column(name = "webhook_status")
    private String webhookStatus = "pending";  // pending, active, failed, inactive

    @Column(name = "webhook_error_message")
    private String webhookErrorMessage;
}
```

### Status Değerleri

| Status | Açıklama |
|--------|----------|
| `pending` | Webhook henüz oluşturulmadı |
| `active` | Webhook aktif ve çalışıyor |
| `failed` | Webhook oluşturma/güncelleme başarısız |
| `inactive` | Webhook devre dışı |

---

## Frontend Webhook Yönetimi

### Settings Sayfası

```tsx
// components/settings/webhook-status.tsx
export function WebhookStatus({ store }: { store: Store }) {
  const { mutate: enableWebhook, isPending } = useEnableWebhook();

  return (
    <Card>
      <CardHeader>
        <CardTitle>Webhook Durumu</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-center gap-2">
          <Badge variant={
            store.webhookStatus === "active" ? "success" :
            store.webhookStatus === "failed" ? "destructive" :
            "secondary"
          }>
            {store.webhookStatus === "active" ? "Aktif" :
             store.webhookStatus === "failed" ? "Başarısız" :
             "Beklemede"}
          </Badge>
        </div>

        {store.webhookStatus === "failed" && store.webhookErrorMessage && (
          <p className="text-red-500 text-sm mt-2">
            Hata: {store.webhookErrorMessage}
          </p>
        )}

        {store.webhookStatus !== "active" && (
          <Button
            onClick={() => enableWebhook(store.id)}
            disabled={isPending}
          >
            {isPending ? "Etkinleştiriliyor..." : "Webhook'u Etkinleştir"}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
```

---

## Idempotency

### Neden Önemli?

Trendyol aynı event'i birden fazla kez gönderebilir (network retry, duplicate delivery). Bu durumda aynı siparişi iki kez işlememek için idempotency kontrolü yapılır.

### Kontrol Akışı

```
Webhook geldi (eventId: abc123)
        │
        ▼
┌─────────────────────────┐
│ webhook_events tablosunda│
│ abc123 var mı?          │
└──────────┬──────────────┘
           │
     ┌─────┴─────┐
     │           │
    VAR        YOK
     │           │
     ▼           ▼
  SKIP     ┌─────────────┐
  (return) │ Event kaydet │
           │ İşlemi yap   │
           └─────────────┘
```

---

## Performans

### 5 Saniye Kuralı

Trendyol, webhook'ların 5 saniye içinde 200 OK dönmesini bekler. Bu yüzden:

1. **Hemen 200 döndür**: İşlem async yapılır
2. **Async işle**: `@Async` annotation ile arka planda
3. **Hata olsa bile 200**: Trendyol retry yapmasın

```java
@PostMapping("/{sellerId}")
public ResponseEntity<String> handleWebhook(...) {
    // Hemen async işleme gönder
    webhookService.processWebhookAsync(sellerId, payload, eventId);

    // Hemen 200 döndür (işlem arka planda devam eder)
    return ResponseEntity.ok("OK");
}
```

### Webhook Event Retention

```sql
-- 30 günden eski event'leri temizle (cron job)
DELETE FROM webhook_events
WHERE received_at < NOW() - INTERVAL '30 days';
```

---

## Monitoring

### Webhook Events Dashboard

```sql
-- Son 24 saatteki webhook istatistikleri
SELECT
    seller_id,
    event_type,
    status,
    COUNT(*) as count,
    MAX(received_at) as last_received
FROM webhook_events
WHERE received_at > NOW() - INTERVAL '24 hours'
GROUP BY seller_id, event_type, status
ORDER BY count DESC;
```

### Hata Takibi

```sql
-- Başarısız webhook'lar
SELECT *
FROM webhook_events
WHERE status = 'failed'
ORDER BY received_at DESC
LIMIT 100;
```

---

## Sorun Giderme

### Webhook Gelmiyor

1. **URL kontrolü**: Trendyol panelinde doğru URL kayıtlı mı?
2. **Firewall**: Sunucu Trendyol IP'lerine açık mı?
3. **SSL**: HTTPS zorunlu, sertifika geçerli mi?

### Duplicate İşleme

1. **eventId kontrolü**: Idempotency key kullanılıyor mu?
2. **Database unique constraint**: `event_id` unique mi?

### 5 Saniye Timeout

1. **Async işleme**: `@Async` kullanılıyor mu?
2. **Blocking işlem yok**: Sync DB call yok mu?
