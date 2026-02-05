# SellerX Sistem Mimarisi

> Bu dokümantasyon, SellerX platformunun teknik mimarisini ve yeni eklenen özellikleri açıklar.

## İçindekiler

1. [Genel Bakış](#genel-bakış)
2. [Sistem Bileşenleri](#sistem-bileşenleri)
3. [Veri Akışı](#veri-akışı)
4. [Detaylı Dokümantasyon](#detaylı-dokümantasyon)

---

## Genel Bakış

SellerX, Türkiye'deki e-ticaret satıcıları için çok mağazalı yönetim platformudur. Platform, satıcıların Trendyol ve Hepsiburada gibi pazaryerlerindeki ürünlerini, siparişlerini ve finansal verilerini tek bir panelden yönetmesini sağlar.

### Teknoloji Stack'i

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND                                 │
│  Next.js 15 + React 19 + TypeScript + TailwindCSS + Shadcn/ui  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP/REST
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      NEXT.JS API ROUTES                         │
│                    (BFF - Backend for Frontend)                 │
│              Token yönetimi, request proxy, caching             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP/REST + JWT
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT BACKEND                          │
│         Java 21 + Spring Boot 3.4 + Spring Security             │
│                                                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │   Auth   │ │  Store   │ │ Products │ │  Orders  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │Financial │ │ Webhooks │ │ Dashboard│ │ Expenses │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ JDBC
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      POSTGRESQL 15                              │
│            JSONB columns, Flyway migrations                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP/REST (Rate Limited)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    TRENDYOL API                                 │
│              Products, Orders, Financial, Webhooks              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Sistem Bileşenleri

### 1. Mağaza Onboarding Sistemi
Yeni mağaza eklendiğinde otomatik veri senkronizasyonu başlatır.

```
Kullanıcı Mağaza Ekler
         │
         ▼
┌─────────────────┐
│  Store Created  │
│  syncStatus:    │
│   "pending"     │
└────────┬────────┘
         │ @Async
         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ SYNCING_PRODUCTS│ ──▶ │ SYNCING_ORDERS  │ ──▶ │SYNCING_FINANCIAL│
│   (Ürünler)     │     │  (Siparişler)   │     │   (Finansal)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
                                               ┌─────────────────┐
                                               │   COMPLETED     │
                                               │ initialSync:true│
                                               └─────────────────┘
```

**Detay:** [Store Onboarding Dokümantasyonu](./STORE_ONBOARDING.md)

### 2. Komisyon Hesaplama Sistemi
Gerçek komisyon oranlarını finansal verilerden alır, tahmini komisyonları hesaplar.

```
Sipariş Geldiğinde:
┌──────────────────────────────────────────────────────────────┐
│  estimatedCommission = vatBaseAmount × commissionRate / 100  │
│  isCommissionEstimated = true                                │
└──────────────────────────────────────────────────────────────┘

Finansal Mutabakat Geldiğinde:
┌──────────────────────────────────────────────────────────────┐
│  Gerçek komisyon oranı alınır                                │
│  Product.lastCommissionRate güncellenir                      │
│  Order.isCommissionEstimated = false                         │
└──────────────────────────────────────────────────────────────┘
```

**Detay:** [Komisyon Sistemi Dokümantasyonu](./COMMISSION_SYSTEM.md)

### 3. Rate Limiting & API Yönetimi
Trendyol API'sine yapılan istekleri saniyede 10 ile sınırlar.

```
┌─────────────────────────────────────────┐
│         TrendyolRateLimiter             │
│  ┌───────────────────────────────────┐  │
│  │  Guava RateLimiter (10 req/sec)   │  │
│  │  acquire() → blocking wait        │  │
│  │  tryAcquire() → non-blocking      │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
            │
            ▼
    Tüm Trendyol API çağrıları
```

**Detay:** [Rate Limiting Dokümantasyonu](./RATE_LIMITING.md)

### 4. Webhook Sistemi
Trendyol'dan gerçek zamanlı sipariş bildirimleri alır.

```
Trendyol ──webhook──▶ /api/webhook/trendyol/{sellerId}
                              │
                              ▼
                     ┌─────────────────┐
                     │ Signature Check │
                     └────────┬────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │ Idempotency     │
                     │ (event_id)      │
                     └────────┬────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │ Order Update    │
                     └─────────────────┘
```

**Detay:** [Webhook Sistemi Dokümantasyonu](./WEBHOOK_SYSTEM.md)

### 5. Otomatik Stok Artisi Algilama (YENİ)
Trendyol stok artislarini otomatik algilayarak maliyet kaydi olusturan sistem.

```
Trendyol Sync (stok: 250 → 300)
         │
         ▼
┌─────────────────────┐
│ saveOrUpdateProduct()│
│  delta: +50 adet    │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐     ┌─────────────────┐
│  PO ile aciklaniyor │─YES─│      SKIP       │
│  mu? (±2 gün, ±%20) │     │  (PO kaydi var) │
└────────┬────────────┘     └─────────────────┘
         │ NO
         ▼
┌─────────────────────┐     ┌─────────────────┐
│ Son maliyet var mi? │─YES─│ AUTO_DETECTED   │
│                     │     │ kayit olustur   │
└────────┬────────────┘     │ + FIFO + Alert  │
         │ NO               └─────────────────┘
         ▼
┌─────────────────────┐
│  Sadece Alert at    │
│  (HIGH severity)    │
└─────────────────────┘
```

**Detay:** [Otomatik Stok Algilama Dokümantasyonu](../features/AUTO_STOCK_DETECTION.md)

### 6. Uyarı/Alarm Sistemi
Kullanıcı tanımlı kurallar ile proaktif bildirim sistemi.

```
┌─────────────────────────────────────────────────────────────┐
│                    ALERT SİSTEMİ                            │
└─────────────────────────────────────────────────────────────┘

    Kullanıcı                  AlertEngine              Bildirim
        │                           │                       │
        │   Kural Tanımla           │                       │
        │ (Stok < 10 uyarı ver)     │                       │
        │──────────────────────────▶│                       │
        │                           │                       │
        │                    ┌──────┴──────┐                │
        │                    │ Product Sync│                │
        │                    │   sonrası   │                │
        │                    └──────┬──────┘                │
        │                           │                       │
        │                    ┌──────┴──────┐                │
        │                    │  Koşul      │                │
        │                    │ Sağlandı mı?│                │
        │                    └──────┬──────┘                │
        │                           │ EVET                  │
        │                           │──────────────────────▶│
        │                           │                 Email + In-App
        │   Bildirim Al             │                       │
        │◀──────────────────────────│                       │
```

**Özellikler:**
- Sınırsız özel kural tanımlama
- Ürün/Kategori bazlı kapsam
- Cooldown (spam önleme)
- Severity seviyeleri (LOW/MEDIUM/HIGH/CRITICAL)
- Email + In-App bildirimler

**Detay:** [Uyarı Sistemi Dokümantasyonu](./ALERT_SYSTEM.md)

### 7. Admin Impersonation (Hesabına Gir) (YENİ)
Admin panelinden kullanıcıların hesabını salt okunur olarak yeni sekmede görüntüleme.

```
Admin Panel (Sekme 1)              Hedef Kullanıcı Görünümü (Sekme 2)
┌──────────────┐                   ┌──────────────────────┐
│ Admin Users  │                   │ /impersonate?token=..│
│ sayfası      │── Yeni sekme ───▶│ sessionStorage kaydet│
│              │   window.open()  │ /dashboard redirect  │
└──────┬───────┘                   └──────────┬───────────┘
       │                                      │
       │ POST /api/admin/users/{id}/impersonate│ X-Impersonation-Token header
       ▼                                      ▼
┌──────────────┐                   ┌──────────────────────┐
│ Backend      │                   │ ReadOnlyInterceptor  │
│ JWT üretir   │                   │ POST/PUT/DELETE → 403│
│ + audit log  │                   └──────────────────────┘
└──────────────┘
```

**Güvenlik:** JWT 1 saat ömürlü (refresh yok), çift katmanlı read-only (frontend + backend), audit log, sessionStorage (tab-scoped).

**Detay:** [Admin Impersonation Dokümantasyonu](./ADMIN_IMPERSONATION.md)

### 8. Scheduled Jobs (Zamanlanmış Görevler)

Güncel job listesi için: [sprint-3-backend-domains/02-scheduled-jobs.md](../sprint-3-backend-domains/02-scheduled-jobs.md).

| Sınıf | Metod | Zamanlama | Açıklama |
|-------|--------|-----------|----------|
| TrendyolOrderScheduledService | syncAllDataForAllTrendyolStores | Her gün 06:15 | Günlük tam sync: sipariş, ürün, Q&A, iadeler. |
| TrendyolOrderScheduledService | catchUpSync | Her saat :00 | Saatlik catch-up: son 2 saat sipariş + ürün, Q&A, iadeler. |
| HybridSyncScheduleConfig | dailySettlementSync | Her gün 07:00 | Günlük Settlement API sync; gerçek komisyon (son 14 gün). |
| HybridSyncScheduleConfig | dailyCargoShippingSync | Her gün 07:15 | Kargo fatura detayı ve siparişlere gerçek kargo maliyeti. |
| HybridSyncScheduleConfig | updateProductCommissionCache | Her gün 07:30 | Ürün komisyon cache güncellemesi. |
| HybridSyncScheduleConfig | dailyReconciliation | Her gün 08:00 | Tahmini komisyonlu siparişleri Settlement ile eşleştirir. |
| HybridSyncScheduleConfig | dailyDeductionInvoiceSync | Her gün 08:15 | Kesinti ve iade faturaları (son 30 gün). |
| HybridSyncScheduleConfig | hourlyGapFill | Her saat :30 | Orders API'den son siparişler; tahmini komisyon atanır. |
| HybridSyncScheduleConfig | hourlyDeductionInvoiceCatchUp | Her saat :45 | Kesinti/iade faturaları catch-up (son 3 gün). |
| TrendyolFinancialSettlementScheduledService | syncSettlementsForAllStores | Her gün 02:00 | Tüm mağazalar için settlement sync. |

---

## Veri Akışı

### Sipariş Veri Akışı

```
                    ┌─────────────┐
                    │  Trendyol   │
                    │    API      │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Webhook  │    │ Scheduled│    │  Manual  │
    │ (Real-   │    │   Job    │    │   Sync   │
    │  time)   │    │ (Batch)  │    │          │
    └────┬─────┘    └────┬─────┘    └────┬─────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │ TrendyolOrderService│
              │                     │
              │ • Komisyon hesapla  │
              │ • Sipariş kaydet    │
              │ • Stock güncelle    │
              └──────────┬──────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │    PostgreSQL       │
              │  trendyol_orders    │
              └─────────────────────┘
```

### Komisyon Veri Akışı

```
┌─────────────────────────────────────────────────────────────────┐
│                     KOMİSYON HESAPLAMA AKIŞI                    │
└─────────────────────────────────────────────────────────────────┘

1. Sipariş Geldiğinde (Tahmini):
   ┌─────────────┐
   │   Product   │──▶ commissionRate (ürün komisyon oranı)
   └─────────────┘           │
                             │ fallback
                             ▼
   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
   │lastCommRate │ OR │ commRate    │ OR │     0       │
   └─────────────┘    └─────────────┘    └─────────────┘
                             │
                             ▼
   estimatedCommission = vatBaseAmount × rate / 100
   isCommissionEstimated = TRUE

2. Finansal Mutabakat Geldiğinde (Gerçek):
   ┌──────────────────┐
   │ Financial Data   │──▶ Gerçek komisyon oranı
   └──────────────────┘
            │
            ├──▶ Product.lastCommissionRate = gerçek oran
            │    Product.lastCommissionDate = now()
            │
            └──▶ Order.isCommissionEstimated = FALSE
```

---

## Detaylı Dokümantasyon

Bu klasördeki tüm mimari dokümanlar (tek yerden erişim):

| Dokümant | Açıklama |
|----------|----------|
| [ADMIN_IMPERSONATION.md](./ADMIN_IMPERSONATION.md) | Admin Impersonation — kullanıcı hesabına salt okunur giriş, audit log, çift katmanlı koruma |
| [ALERT_SYSTEM.md](./ALERT_SYSTEM.md) | Uyarı/Alarm sistemi — kullanıcı tanımlı kurallar, email + in-app bildirim |
| [AUTO_STOCK_DETECTION.md](../features/AUTO_STOCK_DETECTION.md) | Otomatik stok artisi algilama — maliyet kaydi, PO duplicate onleme, badge |
| [ASYNC_PROCESSING.md](./ASYNC_PROCESSING.md) | Asenkron işlem yönetimi (@Async, AsyncConfig) |
| [BACKEND_ARCHITECTURE.md](./BACKEND_ARCHITECTURE.md) | Backend paket yapısı ve teknoloji stack |
| [BILLING_SYSTEM.md](./BILLING_SYSTEM.md) | Abonelik, ödeme, fatura, iyzico entegrasyonu |
| [COMMISSION_SYSTEM.md](./COMMISSION_SYSTEM.md) | Komisyon hesaplama mantığı (tahmini vs gerçek) |
| [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) | Veritabanı şeması ve Flyway migration'lar |
| [HISTORICAL_SETTLEMENT_SYNC.md](./HISTORICAL_SETTLEMENT_SYNC.md) | **KRİTİK:** 90 günlük API limitini aşarak tarihi veri çekme |
| [ORDER_SYNC_SYSTEM.md](./ORDER_SYNC_SYSTEM.md) | Sipariş senkronizasyonu (webhook + scheduled) |
| [QA.md](./QA.md) | Q&A modülü — soru/cevap, pattern, conflict |
| [RATE_LIMITING.md](./RATE_LIMITING.md) | Trendyol API rate limiting (10 req/sec) |
| [STORE_ONBOARDING.md](./STORE_ONBOARDING.md) | Mağaza ekleme ve otomatik senkronizasyon |
| [STORE_SYNC_GUIDE.md](./STORE_SYNC_GUIDE.md) | Mağaza sync rehberi |
| [SYNC_SYSTEM.md](./SYNC_SYSTEM.md) | Hybrid sync ve scheduled job özeti |
| [TEST_INFRASTRUCTURE.md](./TEST_INFRASTRUCTURE.md) | Test altyapısı — TestContainers, base class'lar |
| [TRENDYOL_API_LIMITS.md](./TRENDYOL_API_LIMITS.md) | Trendyol API limitleri ve workaround'lar |
| [WEBHOOK_SYSTEM.md](./WEBHOOK_SYSTEM.md) | Webhook altyapısı ve idempotency |

---

## Hızlı Başlangıç (Geliştirici İçin)

```bash
# 1. Veritabanını başlat
./db.sh start

# 2. Backend'i başlat (JWT_SECRET gerekli!)
cd sellerx-backend
export JWT_SECRET='sellerx-development-jwt-secret-key-2026-minimum-256-bits-required'
./mvnw spring-boot:run

# 3. Frontend'i başlat
cd sellerx-frontend
npm run dev

# Test kullanıcısı:
# Email: test@test.com
# Password: 123456
```

---

## Versiyon Geçmişi

| Versiyon | Tarih | Değişiklikler |
|----------|-------|---------------|
| 2.3.0 | 2026-02 | **Admin Impersonation eklendi** — kullanıcı hesabına salt okunur giriş, audit log, çift katmanlı koruma |
| 2.2.0 | 2026-02 | **Otomatik Stok Algilama eklendi** — stok artisi algilama, auto maliyet kaydi, PO duplicate onleme |
| 2.1.0 | 2026-01 | **Uyarı/Alarm sistemi eklendi** - kullanıcı tanımlı kurallar, email entegrasyonu |
| 2.0.0 | 2026-01 | Komisyon sistemi, Store onboarding, Rate limiting |
| 1.0.0 | 2025-12 | İlk sürüm |
