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

### 5. Uyarı/Alarm Sistemi (YENİ)
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

### 6. Scheduled Jobs (Zamanlanmış Görevler)

| Job | Zamanlama | Açıklama |
|-----|-----------|----------|
| `syncOrdersForAllTrendyolStores` | Her gün 06:15 | Tüm siparişleri senkronize eder |
| `catchUpSync` | Her saat başı | Son 2 saatlik siparişleri kontrol eder |
| `fetchAndUpdateSettlementsForAllStores` | Her gün 07:00 | Finansal mutabakatları günceller |

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

| Dokümant | Açıklama |
|----------|----------|
| [STORE_ONBOARDING.md](./STORE_ONBOARDING.md) | Mağaza ekleme ve otomatik senkronizasyon |
| [**HISTORICAL_SETTLEMENT_SYNC.md**](./HISTORICAL_SETTLEMENT_SYNC.md) | **KRİTİK: 90 günlük API limitini aşarak tarihi veri çekme** |
| [COMMISSION_SYSTEM.md](./COMMISSION_SYSTEM.md) | Komisyon hesaplama mantığı |
| [RATE_LIMITING.md](./RATE_LIMITING.md) | API rate limiting implementasyonu |
| [WEBHOOK_SYSTEM.md](./WEBHOOK_SYSTEM.md) | Webhook altyapısı |
| [**ALERT_SYSTEM.md**](./ALERT_SYSTEM.md) | **YENİ: Uyarı/Alarm sistemi - kullanıcı tanımlı kurallar** |
| [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) | Veritabanı şeması ve migration'lar |
| [ASYNC_PROCESSING.md](./ASYNC_PROCESSING.md) | Asenkron işlem yönetimi |

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
| 2.1.0 | 2026-01 | **Uyarı/Alarm sistemi eklendi** - kullanıcı tanımlı kurallar, email entegrasyonu |
| 2.0.0 | 2026-01 | Komisyon sistemi, Store onboarding, Rate limiting |
| 1.0.0 | 2025-12 | İlk sürüm |
