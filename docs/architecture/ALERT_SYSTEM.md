# Uyarı/Alarm Sistemi

> Kullanıcı tanımlı kurallar ile proaktif bildirim altyapısı.

## Genel Bakış

Alert sistemi, satıcıların stok seviyeleri, kar marjları ve siparişler için özelleştirilebilir uyarı kuralları tanımlamasını sağlar. Koşullar sağlandığında otomatik olarak email ve uygulama içi bildirimler gönderilir.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ALERT SİSTEMİ MİMARİSİ                               │
└─────────────────────────────────────────────────────────────────────────────┘

    Kullanıcı              Backend                    Bildirim
        │                     │                          │
        │  Kural Tanımla      │                          │
        │────────────────────▶│                          │
        │                     │                          │
        │              ┌──────┴──────┐                   │
        │              │ AlertRule   │                   │
        │              │  (Kaydet)   │                   │
        │              └──────┬──────┘                   │
        │                     │                          │
        │                     │                          │
    ════════════════════════════════════════════════════════════
        │                     │                          │
        │   Product Sync      │                          │
        │   veya Webhook      │                          │
        │─────────────────────│                          │
        │                     │                          │
        │              ┌──────┴──────┐                   │
        │              │ AlertEngine │                   │
        │              │ (Değerlendir)│                   │
        │              └──────┬──────┘                   │
        │                     │                          │
        │              ┌──────┴──────┐                   │
        │              │ Koşul       │                   │
        │              │ Sağlandı mı?│                   │
        │              └──────┬──────┘                   │
        │                     │ EVET                     │
        │              ┌──────┴──────┐                   │
        │              │AlertHistory │                   │
        │              │  (Kaydet)   │                   │
        │              └──────┬──────┘                   │
        │                     │                          │
        │                     │─────────────────────────▶│ Email
        │                     │                          │ In-App
        │   Bildirim Al       │                          │
        │◀────────────────────│                          │
        │                     │                          │
```

---

## Melontik Karşılaştırması

| Özellik | Melontik | SellerX |
|---------|----------|---------|
| Kural sayısı | Sabit (3-4) | **Sınırsız özel kural** |
| Kapsam | Genel | **Ürün/Kategori bazlı** |
| Bildirim kanalı | Email + App | **Email + App + Push** |
| Spam önleme | Yok | **Cooldown süresi** |
| Uyarı geçmişi | Yok | **Detaylı log** |
| Severity seviyeleri | Yok | **LOW/MEDIUM/HIGH/CRITICAL** |

---

## Uyarı Türleri (AlertType)

| Tür | Açıklama | Örnek |
|-----|----------|-------|
| `STOCK` | Stok seviyesi uyarıları | Stok < 10 adet |
| `PROFIT` | Kar marjı uyarıları | Kar < %5 |
| `PRICE` | Fiyat değişikliği uyarıları | Fiyat düştü |
| `ORDER` | Sipariş bildirimleri | Yeni sipariş geldi |
| `SYSTEM` | Sistem uyarıları | Sync başarısız |

---

## Koşul Türleri (AlertConditionType)

| Koşul | Açıklama | Kullanım |
|-------|----------|----------|
| `BELOW` | Değer eşiğin altında | Stok < 10 |
| `ABOVE` | Değer eşiğin üstünde | Stok > 1000 |
| `EQUALS` | Değer eşiğe eşit | Stok = 0 |
| `ZERO` | Değer sıfır | Stok tükendi |
| `CHANGED` | Değer değişti | Fiyat değişti |

---

## Ciddiyet Seviyeleri (AlertSeverity)

| Seviye | Renk | Kullanım |
|--------|------|----------|
| `LOW` | Yeşil | Bilgilendirme |
| `MEDIUM` | Sarı | Dikkat gerektirir |
| `HIGH` | Turuncu | Önemli |
| `CRITICAL` | Kırmızı | Acil aksiyon gerekli |

**Otomatik Severity Belirleme:**
- Stok = 0 → `CRITICAL`
- Stok < Threshold × 0.5 → `HIGH`
- Diğer → `MEDIUM`

---

## Veritabanı Şeması

### Tablo: `alert_rules`

```sql
CREATE TABLE alert_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    store_id UUID REFERENCES stores(id),  -- NULL = tüm mağazalar

    name VARCHAR(200) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,      -- STOCK, PROFIT, PRICE, ORDER, SYSTEM
    condition_type VARCHAR(50) NOT NULL,  -- BELOW, ABOVE, EQUALS, ZERO, CHANGED
    threshold DECIMAL(10,2),

    product_barcode VARCHAR(100),         -- NULL = tüm ürünler
    category_name VARCHAR(200),           -- NULL = tüm kategoriler

    email_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT false,
    in_app_enabled BOOLEAN DEFAULT true,
    active BOOLEAN DEFAULT true,

    cooldown_minutes INTEGER DEFAULT 60,  -- Spam önleme
    last_triggered_at TIMESTAMP,
    trigger_count INTEGER DEFAULT 0,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(user_id, name)
);

CREATE INDEX idx_alert_rules_user_active ON alert_rules(user_id, active);
CREATE INDEX idx_alert_rules_store ON alert_rules(store_id);
CREATE INDEX idx_alert_rules_type ON alert_rules(alert_type);
```

### Tablo: `alert_history`

```sql
CREATE TABLE alert_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID REFERENCES alert_rules(id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    store_id UUID REFERENCES stores(id),

    alert_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    message TEXT,
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    data JSONB,  -- Ek veri (ürün bilgisi, değerler vs.)

    email_sent BOOLEAN DEFAULT false,
    push_sent BOOLEAN DEFAULT false,
    in_app_sent BOOLEAN DEFAULT true,
    read_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_alert_history_user_created ON alert_history(user_id, created_at DESC);
CREATE INDEX idx_alert_history_unread ON alert_history(user_id, read_at) WHERE read_at IS NULL;
```

---

## API Endpoints

### Alert Rules (Kural Yönetimi)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `GET` | `/api/alert-rules` | Kullanıcının tüm kuralları |
| `GET` | `/api/alert-rules/{id}` | Tek kural detayı |
| `POST` | `/api/alert-rules` | Yeni kural oluştur |
| `PUT` | `/api/alert-rules/{id}` | Kural güncelle |
| `DELETE` | `/api/alert-rules/{id}` | Kural sil |
| `PUT` | `/api/alert-rules/{id}/toggle` | Aktif/Pasif toggle |
| `GET` | `/api/alert-rules/count` | Toplam kural sayısı |

**Örnek Request - Yeni Kural:**
```json
POST /api/alert-rules
{
  "name": "Düşük Stok Uyarısı",
  "alertType": "STOCK",
  "conditionType": "BELOW",
  "threshold": 10,
  "storeId": "uuid-optional",
  "productBarcode": null,
  "categoryName": null,
  "emailEnabled": true,
  "pushEnabled": false,
  "inAppEnabled": true,
  "cooldownMinutes": 60
}
```

### Alert History (Uyarı Geçmişi)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `GET` | `/api/alerts` | Uyarı geçmişi (paginated) |
| `GET` | `/api/alerts/unread` | Okunmamış uyarılar |
| `GET` | `/api/alerts/unread-count` | Okunmamış sayısı |
| `GET` | `/api/alerts/recent` | Son 24 saat |
| `GET` | `/api/alerts/stats` | İstatistikler |
| `GET` | `/api/alerts/{id}` | Tek uyarı detayı |
| `PUT` | `/api/alerts/{id}/read` | Okundu işaretle |
| `PUT` | `/api/alerts/read-all` | Tümünü okundu işaretle |

**Örnek Response - Stats:**
```json
{
  "unreadCount": 5,
  "stockAlertsLast24h": 3,
  "profitAlertsLast24h": 1,
  "orderAlertsLast24h": 1,
  "totalAlertsLast7Days": 15
}
```

---

## Backend Bileşenler

### Konum
`sellerx-backend/src/main/java/com/ecommerce/sellerx/alerts/`

### Dosyalar

| Dosya | Tür | Açıklama |
|-------|-----|----------|
| `AlertRule.java` | Entity | Kullanıcı tanımlı kurallar |
| `AlertHistory.java` | Entity | Tetiklenen uyarı kayıtları |
| `AlertType.java` | Enum | STOCK, PROFIT, PRICE, ORDER, SYSTEM |
| `AlertConditionType.java` | Enum | BELOW, ABOVE, EQUALS, ZERO, CHANGED |
| `AlertSeverity.java` | Enum | LOW, MEDIUM, HIGH, CRITICAL |
| `AlertRuleDto.java` | DTO | Kural transfer objesi |
| `AlertHistoryDto.java` | DTO | Uyarı transfer objesi |
| `AlertStatsDto.java` | DTO | İstatistik objesi |
| `CreateAlertRuleRequest.java` | Request | Kural oluşturma |
| `UpdateAlertRuleRequest.java` | Request | Kural güncelleme |
| `AlertRuleRepository.java` | Repository | Kural veritabanı işlemleri |
| `AlertHistoryRepository.java` | Repository | Uyarı geçmişi işlemleri |
| `AlertRuleService.java` | Service | Kural CRUD işlemleri |
| `AlertHistoryService.java` | Service | Uyarı geçmişi yönetimi |
| `AlertEngine.java` | Service | Kural değerlendirme motoru |
| `AlertRuleController.java` | Controller | Kural REST API |
| `AlertHistoryController.java` | Controller | Uyarı geçmişi REST API |
| `AlertRuleNotFoundException.java` | Exception | Kural bulunamadı hatası |
| `AlertNotFoundException.java` | Exception | Uyarı bulunamadı hatası |
| `AlertRuleValidationException.java` | Exception | Kural validasyon hatası |

---

## AlertEngine - Kural Motoru

`AlertEngine.java` sistemin kalbidir. Ürün sync sonrası stok kontrolü yapar.

### Tetikleme Noktaları

```java
// Product sync sonrası çağrılır
@Async
@Transactional
public void checkStockAlerts(Store store, List<TrendyolProduct> products) {
    // 1. Kullanıcının aktif STOCK kurallarını al
    // 2. Her ürün için her kuralı değerlendir
    // 3. Koşul sağlanırsa alert tetikle
}
```

### Cooldown Mekanizması

Spam önleme için her kural bir cooldown süresine sahiptir:

```java
// AlertRule.java
public boolean canTrigger() {
    if (lastTriggeredAt == null) return true;
    if (cooldownMinutes == null || cooldownMinutes <= 0) return true;

    LocalDateTime cooldownEnd = lastTriggeredAt.plusMinutes(cooldownMinutes);
    return LocalDateTime.now().isAfter(cooldownEnd);
}
```

---

## Email Entegrasyonu

`EmailService` interface'ine eklenen alert email metodları:

### Stok Uyarısı Emaili

```java
void sendStockAlertEmail(
    String to,           // Alıcı email
    String productName,  // Ürün adı
    String barcode,      // Barkod
    int currentStock,    // Mevcut stok
    int threshold,       // Eşik değeri
    String storeName,    // Mağaza adı
    String severity      // LOW/MEDIUM/HIGH/CRITICAL
);
```

### Genel Uyarı Emaili

```java
void sendAlertEmail(
    String to,
    String alertTitle,
    String alertMessage,
    String storeName,
    String severity,
    String alertType
);
```

### Günlük/Haftalık Özet

```java
void sendAlertDigestEmail(
    String to,
    String digestPeriod,        // "Günlük" veya "Haftalık"
    List<String> alertSummaries, // Uyarı özetleri
    int totalAlerts             // Toplam uyarı sayısı
);
```

---

## Frontend Entegrasyonu

### React Query Hooks
`sellerx-frontend/hooks/queries/use-alerts.ts`

```typescript
// Kural işlemleri
useAlertRules()
useCreateAlertRule()
useUpdateAlertRule()
useDeleteAlertRule()
useToggleAlertRule()

// Uyarı geçmişi
useAlerts(page, size)
useUnreadAlerts()
useUnreadCount()
useAlertStats()
useMarkAsRead()
useMarkAllAsRead()
```

### Next.js API Routes
`sellerx-frontend/app/api/alerts/`

```
app/api/alerts/
├── route.ts              # GET /api/alerts (list)
├── [id]/
│   ├── route.ts          # GET/PUT /api/alerts/{id}
│   └── read/route.ts     # PUT /api/alerts/{id}/read
├── unread/route.ts       # GET /api/alerts/unread
├── unread-count/route.ts # GET /api/alerts/unread-count
├── read-all/route.ts     # PUT /api/alerts/read-all
└── stats/route.ts        # GET /api/alerts/stats

app/api/alert-rules/
├── route.ts              # GET/POST /api/alert-rules
├── [id]/
│   ├── route.ts          # GET/PUT/DELETE /api/alert-rules/{id}
│   └── toggle/route.ts   # PUT /api/alert-rules/{id}/toggle
└── count/route.ts        # GET /api/alert-rules/count
```

---

## Kullanım Örnekleri

### 1. Düşük Stok Uyarısı

```json
{
  "name": "Stok 10'un altına düşerse",
  "alertType": "STOCK",
  "conditionType": "BELOW",
  "threshold": 10,
  "emailEnabled": true,
  "inAppEnabled": true,
  "cooldownMinutes": 60
}
```

### 2. Stok Tükendi Kritik Uyarı

```json
{
  "name": "Stok tükendi - ACİL",
  "alertType": "STOCK",
  "conditionType": "ZERO",
  "emailEnabled": true,
  "inAppEnabled": true,
  "cooldownMinutes": 30
}
```

### 3. Kategori Bazlı Uyarı

```json
{
  "name": "Elektronik kategorisi düşük stok",
  "alertType": "STOCK",
  "conditionType": "BELOW",
  "threshold": 5,
  "categoryName": "Elektronik",
  "emailEnabled": true,
  "cooldownMinutes": 120
}
```

### 4. Belirli Ürün İçin Uyarı

```json
{
  "name": "Bestseller ürün stok kontrolü",
  "alertType": "STOCK",
  "conditionType": "BELOW",
  "threshold": 20,
  "productBarcode": "8680001234567",
  "emailEnabled": true,
  "cooldownMinutes": 30
}
```

---

## Varsayılan Kurallar

Yeni kullanıcı kaydolduğunda otomatik oluşturulabilecek önerilen kurallar:

| Kural | Tür | Koşul | Eşik | Bildirim |
|-------|-----|-------|------|----------|
| Stok düşük | STOCK | BELOW | 10 | Email + App |
| Stok tükendi | STOCK | ZERO | - | Email + App (Kritik) |
| Kar marjı düşük | PROFIT | BELOW | 5% | Email |
| Zarar eden ürün | PROFIT | BELOW | 0% | Email + App |

---

## İlgili Dosyalar

- Migration: `V60__create_alert_tables.sql`
- Security: `WebhookSecurityRules.java` - alert endpoint'leri auth gerektir
- Async: `AsyncConfig.java` - `@Async` annotation desteği
