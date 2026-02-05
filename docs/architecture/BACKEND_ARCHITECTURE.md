# SellerX Backend Mimarisi - A'dan Z'ye Tam Rehber

## Genel Bakış

SellerX, Türkiye pazaryerleri (özellikle Trendyol) için çok mağazalı e-ticaret yönetim platformudur. Backend, Spring Boot 3.4.4 + Java 21 ile geliştirilmiştir.

---

## 1. HTTP İSTEK AKIŞI (Request Flow)

Bir HTTP isteği geldiğinde şu aşamalardan geçer:

```
HTTP İstek (örn: GET /products)
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  1. LoggingFilter                                            │
│     - İstek/yanıt loglaması                                  │
│     - Süre ölçümü (processing time)                          │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  2. JwtAuthenticationFilter                                  │
│     a) Token çıkarma:                                        │
│        - Önce: Authorization: Bearer <token> header          │
│        - Fallback: access_token cookie                       │
│     b) Token doğrulama:                                      │
│        - JwtService.parseToken() ile parse                   │
│        - HMAC-SHA256 imza kontrolü                           │
│        - Süre kontrolü (isExpired)                           │
│     c) SecurityContext'e kullanıcı ekleme:                   │
│        - principal = userId (Long)                           │
│        - authority = ROLE_USER veya ROLE_ADMIN               │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Spring Security Filter Chain                             │
│     - SecurityConfig'deki kurallar uygulanır                 │
│     - Her SecurityRules bean endpoint'leri tanımlar          │
│     - Public endpoint'ler: /auth/*, /api/webhook/*           │
│     - Korumalı: diğer tüm endpoint'ler                       │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Controller → Service → Repository                        │
│     - Controller: HTTP işleme, DTO dönüşümü                  │
│     - Service: İş mantığı, validasyon                        │
│     - Repository: Veritabanı erişimi (JPA)                   │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  5. GlobalExceptionHandler                                   │
│     - Hatalar yakalanır ve uygun HTTP kodu döner             │
│     - 400: Validasyon hatası                                 │
│     - 403: Yetki hatası (AccessDeniedException)              │
│     - 404: Kaynak bulunamadı                                 │
│     - 500: Sunucu hatası                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. JWT KİMLİK DOĞRULAMA SİSTEMİ

### Token Yapısı
```json
{
  "sub": "123",              // userId (Long → String)
  "email": "user@email.com",
  "name": "Kullanıcı Adı",
  "role": "USER",            // USER veya ADMIN
  "iat": 1706000000,         // Token oluşturma zamanı
  "exp": 1706003600          // Token bitiş zamanı
}
```

### Token Türleri
| Token | Süre | Kullanım |
|-------|------|----------|
| Access Token | 1 saat | API istekleri için |
| Refresh Token | 7 gün | Access token yenilemek için |

### Cookie'ler
- `access_token`: JWT (HttpOnly, Secure)
- `refreshToken`: Refresh JWT (HttpOnly, Secure)
- `selected_store_id`: Seçili mağaza UUID (frontend okuyabilir)

### Kritik Kısıtlama
**JWT_SECRET en az 32 karakter olmalı!** Aksi halde login 500 hatası verir (401 değil).

---

## 3. VERİTABANI ŞEMASI VE ENTITY'LER

### Ana Entity'ler

#### User (Kullanıcı)
```
users
├── id: BIGSERIAL (PK)
├── email: VARCHAR (UNIQUE)
├── password: VARCHAR (BCrypt hash)
├── name, phone
├── role: ENUM (USER, ADMIN)
├── selected_store_id: UUID (FK → stores)
└── preferences: JSONB (dil, tema, para birimi)
```

#### Store (Mağaza)
```
stores
├── id: UUID (PK)
├── user_id: BIGINT (FK → users)
├── store_name, marketplace
├── credentials: JSONB (API anahtarları)
│   └── TrendyolCredentials:
│       ├── apiKey, apiSecret
│       ├── sellerId: Long
│       └── integrationCode
├── sync_status: ENUM (PENDING → SYNCING_* → COMPLETED)
├── sync_phases: JSONB (her faz için durum)
├── webhook_id, webhook_status
├── initial_sync_completed: BOOLEAN
└── historical_sync_* (geçmiş veri senkronizasyonu)
```

#### TrendyolProduct (Ürün)
```
trendyol_products
├── id: UUID (PK)
├── store_id: UUID (FK → stores)
├── product_id, barcode, title
├── sale_price, vat_rate
├── commission_rate: Tahmini komisyon (Product API)
├── last_commission_rate: Gerçek komisyon (Financial API)
├── cost_and_stock_info: JSONB [
│   {
│     "stockDate": "2024-01-01",
│     "unitCost": 50.00,
│     "costVatRate": 18,
│     "quantity": 100,
│     "usedQuantity": 45,
│     "remainingQuantity": 55
│   }
│ ]
└── approved, archived, on_sale flags
```

#### TrendyolOrder (Sipariş)
```
trendyol_orders
├── id: UUID (PK)
├── store_id: UUID (FK → stores)
├── ty_order_number, package_no (UNIQUE per store)
├── order_date, status
├── Fiyatlandırma:
│   ├── gross_amount (brüt)
│   ├── total_discount (satıcı indirimi)
│   ├── total_ty_discount (platform indirimi)
│   ├── coupon_discount, early_payment_fee
│   └── total_price (net)
├── Komisyon:
│   ├── estimated_commission
│   ├── is_commission_estimated: BOOLEAN
│   └── commission_difference (gerçek - tahmini)
├── order_items: JSONB [
│   {
│     "barcode": "123456",
│     "productName": "Ürün",
│     "quantity": 1,
│     "price": 95.00,
│     "cost": 50.00,           // Bizim sistemden
│     "costVat": 18,
│     "stockDate": "2024-01-15",
│     "estimatedCommissionRate": 19,
│     "unitEstimatedCommission": 15.20
│   }
│ ]
├── data_source: ENUM (ORDER_API, SETTLEMENT_API, HYBRID)
└── shipment_city, shipment_district
```

---

## 4. SERVİS KATMANI (Business Logic)

### 4.1 Store Onboarding (Mağaza Ekleme)

Yeni mağaza eklendiğinde **StoreOnboardingService** async paralel senkronizasyon başlatır:

```
Kullanıcı Mağaza Ekler
         │
         ▼
    @Async paralel başlatma
         │
    ┌────┴────┬────────────────┐
    │         │                │
    ▼         ▼                ▼
 Phase 1   Phase 2          Phase 3
PRODUCTS → HISTORICAL →    QA & RETURNS
   │      FINANCIAL        (bağımsız)
   │      COMMISSIONS
   │
   ▼
 COMPLETED (initialSyncCompleted = true)
```

**Faz Durumları**: PENDING → ACTIVE → COMPLETED/FAILED

### 4.2 Ürün Senkronizasyonu (TrendyolProductService)

```java
// Trendyol API'den ürünleri çeker
fetchAllProducts(Store store) {
    // Sayfalama ile tüm ürünleri al
    while (hasMorePages) {
        rateLimiter.acquire();  // 10 req/sec limit

        List<TrendyolProductResponse> products = callTrendyolApi(page);

        for (product : products) {
            // JSONB cost_and_stock_info dizisine ekle/güncelle
            updateOrCreateProduct(product);
        }
    }
}
```

### 4.3 Sipariş Senkronizasyonu (TrendyolOrderService)

**15 günlük parçalar halinde** son 90 günü çeker:

```java
syncOrders(Store store) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusMonths(3);

    // 15 günlük chunk'lar
    while (startDate < endDate) {
        LocalDate chunkEnd = min(startDate.plusDays(15), endDate);

        rateLimiter.acquire();
        List<Order> orders = fetchOrdersForDateRange(startDate, chunkEnd);

        for (order : orders) {
            // Her OrderItem için maliyet hesapla
            orderCostCalculator.setCostInfo(orderItem, product);
            saveOrder(order);
        }

        startDate = chunkEnd;
    }
}
```

### 4.4 Maliyet Hesaplama (FIFO Algoritması)

**OrderCostCalculator** siparişlere maliyet atar:

```java
setCostInfo(OrderItem item, TrendyolProduct product) {
    // 1. Ürünün cost_and_stock_info JSONB'sinden stok bul
    List<CostAndStockInfo> costs = product.getCostAndStockInfo();

    // 2. FIFO: Sipariş tarihine uygun EN ESKİ stoğu bul
    CostAndStockInfo match = costs.stream()
        .filter(c -> c.getStockDate() <= orderDate)
        .filter(c -> c.getRemainingQuantity() > 0)
        .min(Comparator.comparing(CostAndStockInfo::getStockDate))
        .orElse(null);

    // 3. Maliyeti OrderItem'a ata
    if (match != null) {
        item.setCost(match.getUnitCost());
        item.setCostVat(match.getCostVatRate());
        item.setStockDate(match.getStockDate());
    }
}
```

### 4.5 Komisyon Hesaplama

**Tahmini Komisyon** (sipariş geldiğinde):
```java
// Formül: (fiyat / (1 + kdv/100)) × komisyon_oranı / 100
// Örnek:
// - price: 799.40 TL (KDV dahil)
// - vatRate: 20 (yüzde)
// - vatBase: 799.40 / 1.20 = 666.17 TL
// - commissionRate: 19%
// - commission: 666.17 × 0.19 = 126.57 TL

BigDecimal commission = price
    .divide(BigDecimal.ONE.add(vatRate.divide(100)))  // KDV çıkar
    .multiply(commissionRate)
    .divide(100);
```

**Komisyon Oranı Fallback** (öncelik sırası):
1. `lastCommissionRate` - Financial API'den gerçek oran (en doğru)
2. `commissionRate` - Product API'den tahmini oran
3. `0` - Hiç oran yoksa

---

## 5. ZAMANLANMIŞ GÖREVLER (Scheduled Jobs)

| Görev | Zamanlama | Açıklama |
|-------|-----------|----------|
| `syncAllDataForAllTrendyolStores` | Her gün 06:15 | Tam sipariş/ürün/QA/iade senkronizasyonu |
| `catchUpSync` | Her saat başı | Son 2 saatteki siparişleri yakala |
| `dailySettlementSync` | Her gün 07:00 | Finansal mutabakat (son 14 gün) |
| `updateProductCommissionCache` | Her gün 07:30 | Ürün komisyon oranlarını güncelle |
| `dailyReconciliation` | Her gün 08:00 | Tahmini vs gerçek komisyon eşleştir |

**Önemli**: Sadece `initialSyncCompleted = true` olan mağazalar işlenir.

---

## 6. WEBHOOK SİSTEMİ

Trendyol sipariş durumu değiştiğinde webhook gönderir:

```
Trendyol → POST /api/webhook/trendyol/{sellerId}
              │
              ▼
    ┌──────────────────────┐
    │ 1. İmza Doğrulama    │  HMAC-SHA256
    │    (X-Trendyol-      │
    │     Signature)       │
    └──────────┬───────────┘
              │
              ▼
    ┌──────────────────────┐
    │ 2. Idempotency       │  event_id = hash(sellerId +
    │    Kontrolü          │  orderNumber + status + timestamp)
    └──────────┬───────────┘
              │
              ▼
    ┌──────────────────────┐
    │ 3. Sipariş Güncelle  │  createOrder / updateOrder
    └──────────┬───────────┘
              │
              ▼
    ┌──────────────────────┐
    │ 4. 200 OK Döndür     │  5 saniye içinde ZORUNLU!
    └──────────────────────┘
```

**WebhookEvent** tablosu:
- `event_id`: UNIQUE (tekrar işlemeyi önler)
- `processing_status`: RECEIVED → PROCESSING → COMPLETED/FAILED/DUPLICATE

---

## 7. FİNANSAL VERİ SİSTEMİ (Hybrid Sync)

### Veri Kaynakları
| Kaynak | Veri | Gecikme | Komisyon |
|--------|------|---------|----------|
| Orders API | Son 90 gün siparişleri | Gerçek zamanlı | TAHMİNİ |
| Settlement API | Tüm finansal işlemler | 3-7 gün | GERÇEK |

### Hybrid Akış
```
Orders API (hızlı, tahmini)
         │
         │ isCommissionEstimated = true
         ▼
    ┌────────────┐
    │  Sipariş   │
    │  Kaydedilir│
    └─────┬──────┘
          │
          │ 3-7 gün sonra
          ▼
Settlement API (yavaş, gerçek)
          │
          │ Reconciliation (08:00)
          ▼
    ┌────────────┐
    │ Komisyon   │  isCommissionEstimated = false
    │ Güncellenir│  dataSource = HYBRID
    └────────────┘
```

---

## 8. RATE LIMITING (Hız Sınırlama)

**TrendyolRateLimiter** - Guava RateLimiter kullanır:

```java
@Component
public class TrendyolRateLimiter {
    // 10 istek/saniye
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    public void acquire() {
        rateLimiter.acquire();  // Permit bekler (minimal block)
    }
}
```

**Tüm Trendyol API çağrılarından önce** `rateLimiter.acquire()` çağrılır.

---

## 9. JSONB KULLANIM ÖZETİ

| Entity | Kolon | İçerik |
|--------|-------|--------|
| User | preferences | `{"language":"tr","theme":"dark"}` |
| Store | credentials | `{"apiKey":"...","sellerId":"123"}` |
| Store | syncPhases | `{"PRODUCTS":"COMPLETED","ORDERS":"ACTIVE"}` |
| Product | costAndStockInfo | Maliyet geçmişi dizisi |
| Order | orderItems | Sipariş kalemleri dizisi |
| Order | financialTransactions | Mutabakat detayları |

---

## 10. API ENDPOİNTLERİ

### Public (JWT gerektirmez)
- `POST /auth/login` - Giriş
- `POST /auth/refresh` - Token yenileme
- `POST /api/webhook/trendyol/{sellerId}` - Webhook alıcı

### Protected (JWT gerektirir)
- `GET /users/profile` - Kullanıcı profili
- `GET/POST/PUT/DELETE /stores/*` - Mağaza CRUD
- `GET /products/*` - Ürünler
- `GET /orders/*` - Siparişler
- `GET /dashboard/stats/{storeId}` - Dashboard istatistikleri
- `GET/POST /expenses/*` - Giderler

### Yetki Kontrolü
```java
// Yöntem 1: @PreAuthorize annotation
@PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
public ResponseEntity<?> getProducts(@PathVariable UUID storeId) { ... }

// Yöntem 2: Manuel kontrol
Long userId = (Long) SecurityContextHolder.getContext()
    .getAuthentication().getPrincipal();
if (!storeService.isStoreOwnedByUser(storeId, userId)) {
    throw new AccessDeniedException("Bu mağazaya erişim yetkiniz yok");
}
```

---

## 11. HATA YÖNETİMİ

**GlobalExceptionHandler** tüm hataları yakalar:

| Exception | HTTP Kodu | Mesaj |
|-----------|-----------|-------|
| MethodArgumentNotValidException | 400 | Field validasyon hataları |
| AccessDeniedException | 403 | "Access denied" |
| StoreNotFoundException | 404 | "Store not found" |
| UserNotFoundException | 404 | "User not found" |
| Exception (catch-all) | 500 | "An unexpected error occurred" |

---

## 12. KRİTİK KISITLAMALAR

### JWT Secret
- **En az 32 karakter** olmalı (HMAC-SHA256 için 256-bit)
- Kısa secret → Login'de 500 hatası (401 değil!)

### Webhook Timing
- **5 saniye içinde 200 OK** dönmeli
- Aksi halde Trendyol retry yapar

### Package Number
- `UNIQUE(store_id, package_no)` constraint
- Trendyol API "id" field'ı (tyOrderNumber değil!)

### VAT Base Amount İsimlendirme HATASI
- Trendyol'un `vatBaseAmount` alanı aslında **KDV ORANI** (yüzde)
- Gerçek KDV matrahı: `price / (1 + vatRate/100)` ile hesaplanmalı

---

## 13. DOSYA YAPISI

```
sellerx-backend/src/main/java/com/ecommerce/sellerx/
├── auth/           # JWT, SecurityConfig, Filter
├── users/          # User entity, service, controller
├── stores/         # Store yönetimi, onboarding
├── products/       # Ürün senkronizasyonu
├── orders/         # Sipariş yönetimi, OrderCostCalculator
├── dashboard/      # İstatistik hesaplama
├── financial/      # Finansal mutabakat
├── expenses/       # Gider takibi
├── trendyol/       # Trendyol API client
├── webhook/        # Webhook alıcı
├── common/         # RateLimiter, GlobalExceptionHandler
└── config/         # Spring config, AsyncConfig
```

---

## Sonuç

SellerX backend'i şu temel bileşenlerden oluşur:

1. **JWT Kimlik Doğrulama**: Cookie tabanlı, 1 saat access + 7 gün refresh token
2. **Çok Katmanlı Mimari**: Controller → Service → Repository
3. **JSONB Esnekliği**: Maliyet geçmişi, sipariş kalemleri, ayarlar
4. **Hybrid Senkronizasyon**: Orders API (hızlı) + Settlement API (doğru)
5. **FIFO Maliyet Hesaplama**: Stok tarihine göre maliyet eşleştirme
6. **Rate Limiting**: 10 req/sec Trendyol API limiti
7. **Webhook İdempotency**: event_id ile tekrar işleme önleme
8. **Zamanlanmış Görevler**: Günlük/saatlik veri senkronizasyonu
