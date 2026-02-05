# SellerX Billing Sistemi - Kapsamli Teknik Dokumantasyon

Bu dokuman, SellerX e-ticaret yonetim platformunun abonelik ve faturalandirma sistemini detayli olarak aciklamaktadir. Yeni bir yazilimci bu dokumani okuyarak sistemi rahatlica anlayip gelistirebilir.

**Son Guncelleme**: 2026-01-18
**Versiyon**: 1.0

---

## Icindekiler

1. [Genel Bakis](#1-genel-bakis)
2. [Veritabani Semasi](#2-veritabani-semasi)
3. [Backend Mimarisi](#3-backend-mimarisi)
4. [Subscription Lifecycle](#4-subscription-lifecycle)
5. [Odeme Akislari](#5-odeme-akislari)
6. [Frontend Mimarisi](#6-frontend-mimarisi)
7. [Feature Gating](#7-feature-gating)
8. [API Referansi](#8-api-referansi)
9. [Konfigurasyon](#9-konfigurasyon)
10. [Test ve Debug](#10-test-ve-debug)

---

## 1. Genel Bakis

### 1.1 Sistem Mimarisi

```
+------------------+     +------------------+     +------------------+
|    Frontend      |     |     Backend      |     |    Database      |
|    (Next.js)     |<--->|  (Spring Boot)   |<--->|   (PostgreSQL)   |
+------------------+     +------------------+     +------------------+
         |                       |
         |                       v
         |               +------------------+
         |               |     iyzico       |
         +-------------->|  (Payment GW)    |
                         +------------------+
```

### 1.2 Teknoloji Stack

| Katman | Teknoloji | Versiyon |
|--------|-----------|----------|
| Frontend | Next.js + React + TypeScript | 15.x + 19.x |
| Backend | Spring Boot + Java | 3.4.4 + 21 |
| Database | PostgreSQL | 15 |
| Payment Gateway | iyzico | REST API |
| E-Fatura | Parasut | REST API (devre disi) |

### 1.3 Temel Kavramlar

| Kavram | Aciklama |
|--------|----------|
| **Subscription** | Kullanicinin abonelik kaydi (1 kullanici = 1 abonelik) |
| **Plan** | Abonelik plani (FREE, STARTER, PRO, ENTERPRISE) |
| **Price** | Fiyatlandirma (MONTHLY, QUARTERLY, SEMIANNUAL, YEARLY) |
| **Invoice** | Fatura kaydi (her odeme donemi icin) |
| **Payment Method** | Kayitli odeme yontemi (kredi karti) |
| **Feature** | Plan ozelligi (max_stores, ai_qa_responses, vb.) |
| **Trial** | 14 gunluk ucretsiz deneme suresi |
| **Grace Period** | Odeme basarisizligi sonrasi 3 gunluk ek sure |

### 1.4 Plan Ozeti

| Plan | Aylik Fiyat | Max Magaza | AI QA | Webhook | API |
|------|-------------|------------|-------|---------|-----|
| FREE | 0 TL | 1 | 10 | - | - |
| STARTER | 299 TL | 3 | 100 | - | - |
| PRO | 599 TL | 10 | 500 | + | + |
| ENTERPRISE | 1.499 TL | Sinirsiz | Sinirsiz | + | + |

---

## 2. Veritabani Semasi

### 2.1 Entity-Relationship Diyagrami

```
+------------------+       +----------------------+       +-------------------+
|      users       |       | subscription_plans   |       | subscription_     |
+------------------+       +----------------------+       |     prices        |
| id (PK)          |       | id (PK)              |       +-------------------+
| email            |       | code (UNIQUE)        |       | id (PK)           |
| name             |       | name                 |       | plan_id (FK)      |
| ...              |       | max_stores           |       | billing_cycle     |
+--------+---------+       | features (JSONB)     |       | price_amount      |
         |                 +----------+-----------+       | discount_pct      |
         |                            |                   +---------+---------+
         | 1:1                        | 1:N                         |
         v                            v                             |
+------------------+       +----------------------+                  |
|  subscriptions   |<------| (plan_id, price_id)  |<-----------------+
+------------------+       +----------------------+
| id (PK)          |
| user_id (FK,UQ)  |-----> users.id
| plan_id (FK)     |-----> subscription_plans.id
| price_id (FK)    |-----> subscription_prices.id
| status           |       STATUS: PENDING_PAYMENT | TRIAL | ACTIVE |
| billing_cycle    |               PAST_DUE | SUSPENDED | CANCELLED | EXPIRED
| trial_start/end  |
| period_start/end |
| grace_period_end |
| iyzico_ref       |
| cancel_at_end    |
| downgrade_to_*   |
+--------+---------+
         |
         | 1:N
         v
+------------------+       +----------------------+       +-------------------+
|    invoices      |       | payment_transactions |       | payment_methods   |
+------------------+       +----------------------+       +-------------------+
| id (PK)          |       | id (PK)              |       | id (PK)           |
| subscription_id  |<----->| invoice_id (FK)      |       | user_id (FK)      |
| user_id (FK)     |       | payment_method_id    |------>| iyzico_card_token |
| invoice_number   |       | amount               |       | card_last_four    |
| status           |       | status               |       | card_brand        |
| subtotal         |       | iyzico_payment_id    |       | card_exp_month    |
| tax_amount       |       | iyzico_conv_id       |       | card_exp_year     |
| total_amount     |       | failure_code/msg     |       | is_default        |
| line_items(JSONB)|       | attempt_number       |       +-------------------+
| billing_address  |       | next_retry_at        |
+--------+---------+       +----------------------+
         |
         | 1:1
         v
+------------------+       +----------------------+       +-------------------+
|   e_invoices     |       | subscription_events  |       |  plan_features    |
+------------------+       +----------------------+       +-------------------+
| id (PK)          |       | id (PK)              |       | id (PK)           |
| invoice_id (FK)  |       | subscription_id (FK) |       | plan_id (FK)      |
| parasut_id       |       | event_type           |       | feature_code      |
| status           |       | previous_status      |       | feature_type      |
| invoice_type     |       | new_status           |       | limit_value       |
| tax_number       |       | previous_plan_id     |       | is_enabled        |
| xml_content      |       | new_plan_id          |       +-------------------+
| pdf_url          |       | metadata (JSONB)     |
+------------------+       +----------------------+       +-------------------+
                                                          |  feature_usage    |
                                                          +-------------------+
                                                          | id (PK)           |
                                                          | user_id (FK)      |
                                                          | feature_code      |
                                                          | usage_count       |
                                                          | period_start/end  |
                                                          +-------------------+
```

### 2.2 Tablo Detaylari

#### 2.2.1 subscription_plans

Abonelik planlarini tanimlar.

```sql
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,      -- FREE, STARTER, PRO, ENTERPRISE
    name VARCHAR(100) NOT NULL,            -- Gorunen ad
    description TEXT,                       -- Plan aciklamasi
    max_stores INTEGER,                     -- NULL = sinirsiz
    features JSONB,                         -- Ozellik bayraklari
    is_active BOOLEAN DEFAULT true,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- features JSONB ornegi:
{
    "advanced_analytics": true,
    "webhooks": true,
    "api_access": true,
    "priority_support": false,
    "parasut_integration": true
}
```

#### 2.2.2 subscription_prices

Her plan icin fiyatlandirma secenekleri.

```sql
CREATE TABLE subscription_prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    billing_cycle VARCHAR(20) NOT NULL,    -- MONTHLY, QUARTERLY, SEMIANNUAL, YEARLY
    price_amount DECIMAL(10,2) NOT NULL,   -- TL cinsinden fiyat
    discount_percentage DECIMAL(5,2) DEFAULT 0,  -- Uzun donem indirimi
    currency VARCHAR(3) DEFAULT 'TRY',
    is_active BOOLEAN DEFAULT true,
    UNIQUE(plan_id, billing_cycle)
);

-- Ornek fiyatlar:
-- STARTER: 299 TL/ay, 807.30 TL/3ay (%10), 1435.20 TL/6ay (%20)
-- PRO:     599 TL/ay, 1617.30 TL/3ay (%10), 2875.20 TL/6ay (%20)
```

#### 2.2.3 subscriptions

Kullanici abonelik kayitlari (1 kullanici = 1 abonelik).

```sql
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    price_id UUID NOT NULL REFERENCES subscription_prices(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    billing_cycle VARCHAR(20) NOT NULL,

    -- Trial suresi
    trial_start_date TIMESTAMP,
    trial_end_date TIMESTAMP,

    -- Mevcut faturalandirma donemi
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,

    -- Odeme basarisizligi sonrasi ek sure (3 gun)
    grace_period_end TIMESTAMP,

    -- iyzico kart-on-file referansi
    iyzico_subscription_reference VARCHAR(500),

    -- Iptal yonetimi
    cancel_at_period_end BOOLEAN DEFAULT false,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    auto_renew BOOLEAN DEFAULT true,

    -- Planlanmis downgrade
    downgrade_to_plan_id UUID REFERENCES subscription_plans(id),
    downgrade_to_price_id UUID REFERENCES subscription_prices(id),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_status CHECK (status IN (
        'PENDING_PAYMENT', 'TRIAL', 'ACTIVE', 'PAST_DUE',
        'SUSPENDED', 'CANCELLED', 'EXPIRED'
    ))
);
```

#### 2.2.4 payment_methods

iyzico ile tokenize edilmis kart bilgileri.

```sql
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) DEFAULT 'CREDIT_CARD',  -- CREDIT_CARD, DEBIT_CARD
    provider VARCHAR(50) DEFAULT 'iyzico',

    -- iyzico tokenizasyon (ASLA ham kart bilgisi saklanmaz!)
    iyzico_card_user_key VARCHAR(500),
    iyzico_card_token VARCHAR(500) NOT NULL,

    -- Goruntuleme icin maskeli bilgiler
    card_last_four VARCHAR(4) NOT NULL,      -- **** **** **** 1234
    card_brand VARCHAR(50),                   -- Visa, Mastercard, Troy
    card_family VARCHAR(100),
    card_holder_name VARCHAR(200),
    card_exp_month INTEGER NOT NULL,
    card_exp_year INTEGER NOT NULL,
    card_bank_name VARCHAR(100),

    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 2.2.5 invoices

Fatura kayitlari.

```sql
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    invoice_number VARCHAR(50) NOT NULL UNIQUE,  -- INV-2026-000001
    invoice_series VARCHAR(10) DEFAULT 'SEL',
    status VARCHAR(20) DEFAULT 'DRAFT',          -- DRAFT, PENDING, PAID, FAILED, REFUNDED, VOID

    -- Tutarlar (TL)
    subtotal DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,2) DEFAULT 20,            -- %20 KDV
    tax_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'TRY',

    -- Donem bilgileri
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,
    due_date TIMESTAMP NOT NULL,                  -- Vade tarihi (7 gun)
    paid_at TIMESTAMP,                            -- Odeme tarihi

    -- JSONB alanlar
    line_items JSONB,                             -- Fatura kalemleri
    billing_address JSONB,                        -- Fatura adresi snapshot
    notes TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- line_items JSONB ornegi:
[
    {
        "description": "Pro Plan - Aylik",
        "quantity": 1,
        "unitPrice": 599,
        "amount": 599
    }
]

-- billing_address JSONB ornegi:
{
    "name": "Ahmet Yilmaz",
    "email": "ahmet@example.com",
    "phone": "+905551234567",
    "address": "Ataturk Cad. No:123",
    "city": "Istanbul",
    "postalCode": "34000",
    "country": "TR"
}
```

#### 2.2.6 payment_transactions

Odeme islem kayitlari.

```sql
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    payment_method_id UUID REFERENCES payment_methods(id),

    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'TRY',
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED
    provider VARCHAR(50) DEFAULT 'iyzico',

    -- iyzico referanslari
    iyzico_payment_id VARCHAR(500),
    iyzico_conversation_id VARCHAR(500),    -- Idempotency key
    iyzico_payment_transaction_id VARCHAR(500),

    -- Hata bilgileri
    provider_response JSONB,                 -- Tam iyzico yaniti
    failure_code VARCHAR(100),
    failure_message TEXT,

    -- Retry mekanizmasi
    attempt_number INTEGER DEFAULT 1,        -- Max 3 deneme
    next_retry_at TIMESTAMP,                 -- Sonraki deneme zamani

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 2.2.7 plan_features

Plan bazli ozellik tanimlari.

```sql
CREATE TABLE plan_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    feature_code VARCHAR(100) NOT NULL,      -- max_stores, ai_qa_responses, vb.
    feature_name VARCHAR(200) NOT NULL,      -- Gorunen ad
    feature_type VARCHAR(20) NOT NULL,       -- BOOLEAN, LIMIT, UNLIMITED
    limit_value INTEGER,                      -- LIMIT tipi icin max deger
    is_enabled BOOLEAN DEFAULT true,
    UNIQUE(plan_id, feature_code)
);

-- Ornek veriler:
-- FREE:       max_stores=1,  ai_qa_responses=10,  advanced_analytics=false
-- STARTER:   max_stores=3,  ai_qa_responses=100, advanced_analytics=true
-- PRO:       max_stores=10, ai_qa_responses=500, webhook_support=true, api_access=true
-- ENTERPRISE: max_stores=UNLIMITED, ai_qa_responses=UNLIMITED, priority_support=true
```

#### 2.2.8 feature_usage

Kullanici bazli ozellik kullanim takibi.

```sql
CREATE TABLE feature_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature_code VARCHAR(100) NOT NULL,
    usage_count INTEGER DEFAULT 0,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    UNIQUE(user_id, feature_code, period_start)
);
```

#### 2.2.9 subscription_events

Abonelik degisiklik audit log'u.

```sql
CREATE TABLE subscription_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),

    event_type VARCHAR(50) NOT NULL,         -- CREATED, ACTIVATED, UPGRADED, vb.
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    previous_plan_id UUID REFERENCES subscription_plans(id),
    new_plan_id UUID REFERENCES subscription_plans(id),
    metadata JSONB,                           -- Ek bilgiler

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_event_type CHECK (event_type IN (
        'CREATED', 'ACTIVATED', 'CANCELLED', 'REACTIVATED',
        'UPGRADED', 'DOWNGRADED', 'TRIAL_STARTED', 'TRIAL_ENDED',
        'PAYMENT_SUCCEEDED', 'PAYMENT_FAILED', 'RENEWED',
        'SUSPENDED', 'RESUMED', 'EXPIRED'
    ))
);
```

### 2.3 Indexler

```sql
-- subscriptions
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_period_end ON subscriptions(current_period_end);
CREATE INDEX idx_subscriptions_trial_end ON subscriptions(trial_end_date) WHERE trial_end_date IS NOT NULL;
CREATE INDEX idx_subscriptions_grace_end ON subscriptions(grace_period_end) WHERE grace_period_end IS NOT NULL;

-- invoices
CREATE INDEX idx_invoices_user_id ON invoices(user_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);

-- payment_transactions
CREATE INDEX idx_transactions_invoice_id ON payment_transactions(invoice_id);
CREATE INDEX idx_transactions_iyzico_conv_id ON payment_transactions(iyzico_conversation_id);
CREATE INDEX idx_transactions_next_retry ON payment_transactions(next_retry_at) WHERE next_retry_at IS NOT NULL;

-- payment_methods
CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);
CREATE INDEX idx_payment_methods_default ON payment_methods(user_id, is_default) WHERE is_default = true;
```

---

## 3. Backend Mimarisi

### 3.1 Dosya Yapisi

```
sellerx-backend/src/main/java/com/ecommerce/sellerx/billing/
├── entity/
│   ├── Subscription.java
│   ├── SubscriptionPlan.java
│   ├── SubscriptionPrice.java
│   ├── Invoice.java
│   ├── PaymentTransaction.java
│   ├── PaymentMethod.java
│   ├── EInvoice.java
│   ├── PlanFeature.java
│   ├── FeatureUsage.java
│   └── SubscriptionEvent.java
├── enums/
│   ├── SubscriptionStatus.java
│   ├── InvoiceStatus.java
│   ├── PaymentStatus.java
│   ├── BillingCycle.java
│   ├── FeatureType.java
│   └── SubscriptionEventType.java
├── repository/
│   ├── SubscriptionRepository.java
│   ├── SubscriptionPlanRepository.java
│   ├── SubscriptionPriceRepository.java
│   ├── InvoiceRepository.java
│   ├── PaymentTransactionRepository.java
│   ├── PaymentMethodRepository.java
│   └── ...
├── service/
│   ├── SubscriptionService.java
│   ├── PaymentService.java
│   ├── InvoiceService.java
│   ├── PaymentMethodService.java
│   ├── PaymentRetryService.java
│   └── FeatureService.java
├── controller/
│   ├── BillingController.java           -- /api/billing/plans
│   ├── SubscriptionController.java      -- /api/billing/subscription
│   ├── CheckoutController.java          -- /api/billing/checkout
│   ├── InvoiceController.java           -- /api/billing/invoices
│   ├── PaymentMethodController.java     -- /api/billing/payment-methods
│   └── FeatureController.java           -- /api/billing/features
├── iyzico/
│   ├── IyzicoApiClient.java
│   ├── IyzicoWebhookService.java
│   ├── IyzicoWebhookController.java
│   ├── IyzicoConfig.java
│   └── dto/
│       ├── IyzicoPaymentRequest.java
│       ├── IyzicoPaymentResult.java
│       ├── IyzicoCardRequest.java
│       └── IyzicoThreeDsResult.java
├── dto/
│   ├── CreateSubscriptionRequest.java
│   ├── SubscriptionDto.java
│   ├── SubscriptionPlanDto.java
│   ├── ChangePlanRequest.java
│   ├── CancelSubscriptionRequest.java
│   └── ...
└── config/
    ├── SubscriptionConfig.java
    ├── IyzicoConfig.java
    └── BillingSecurityRules.java
```

### 3.2 Entity Siniflari

#### Subscription.java

```java
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_id")
    private SubscriptionPrice price;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    private LocalDateTime trialStartDate;
    private LocalDateTime trialEndDate;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime gracePeriodEnd;

    private String iyzicoSubscriptionReference;

    private boolean cancelAtPeriodEnd = false;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private boolean autoRenew = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "downgrade_to_plan_id")
    private SubscriptionPlan downgradeToPlan;

    // Helper metodlar
    public boolean hasAccess() {
        return status == SubscriptionStatus.TRIAL
            || status == SubscriptionStatus.ACTIVE
            || status == SubscriptionStatus.PAST_DUE;
    }

    public boolean isInTrial() {
        return status == SubscriptionStatus.TRIAL
            && trialEndDate != null
            && LocalDateTime.now().isBefore(trialEndDate);
    }

    public boolean needsRenewal() {
        return currentPeriodEnd != null
            && LocalDateTime.now().isAfter(currentPeriodEnd);
    }

    public boolean isInGracePeriod() {
        return status == SubscriptionStatus.PAST_DUE
            && gracePeriodEnd != null
            && LocalDateTime.now().isBefore(gracePeriodEnd);
    }

    public int getTrialDaysRemaining() {
        if (!isInTrial()) return 0;
        return (int) ChronoUnit.DAYS.between(LocalDateTime.now(), trialEndDate);
    }
}
```

#### SubscriptionStatus.java (Enum)

```java
public enum SubscriptionStatus {
    PENDING_PAYMENT,  // Ilk odeme bekleniyor
    TRIAL,            // 14 gunluk deneme suresi
    ACTIVE,           // Aktif abonelik
    PAST_DUE,         // Odeme basarisiz, 3 gun grace period
    SUSPENDED,        // Grace period bitti, erisim askiya alindi
    CANCELLED,        // Iptal edildi
    EXPIRED;          // 30+ gun odeme yok, suresi doldu

    public boolean hasAccess() {
        return this == TRIAL || this == ACTIVE || this == PAST_DUE;
    }

    public boolean canRenew() {
        return this == ACTIVE || this == PAST_DUE || this == SUSPENDED;
    }

    public boolean canCancel() {
        return this == TRIAL || this == ACTIVE || this == PAST_DUE;
    }
}
```

### 3.3 Service Katmani

#### SubscriptionService.java

```java
@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionEventRepository eventRepository;

    // Abonelik olusturma (14 gun trial ile)
    public Subscription createSubscription(User user, String planCode, BillingCycle cycle) {
        SubscriptionPlan plan = planRepository.findByCode(planCode)
            .orElseThrow(() -> new NotFoundException("Plan not found"));

        SubscriptionPrice price = plan.getPrices().stream()
            .filter(p -> p.getBillingCycle() == cycle)
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Price not found"));

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setPrice(price);
        subscription.setBillingCycle(cycle);
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscription.setTrialStartDate(LocalDateTime.now());
        subscription.setTrialEndDate(LocalDateTime.now().plusDays(14));
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(14));

        subscription = subscriptionRepository.save(subscription);
        recordEvent(subscription, SubscriptionEventType.TRIAL_STARTED, null, null);

        return subscription;
    }

    // Abonelik aktivasyonu (odeme sonrasi)
    public Subscription activateSubscription(UUID subscriptionId) {
        Subscription subscription = findById(subscriptionId);

        SubscriptionStatus previousStatus = subscription.getStatus();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(calculatePeriodEnd(subscription.getBillingCycle()));
        subscription.setGracePeriodEnd(null);

        subscription = subscriptionRepository.save(subscription);
        recordEvent(subscription, SubscriptionEventType.ACTIVATED, previousStatus, SubscriptionStatus.ACTIVE);

        return subscription;
    }

    // Plan yukseltme (hemen uygulanan)
    public Subscription upgradePlan(UUID subscriptionId, String newPlanCode, BillingCycle newCycle) {
        Subscription subscription = findById(subscriptionId);
        SubscriptionPlan oldPlan = subscription.getPlan();
        SubscriptionPlan newPlan = planRepository.findByCode(newPlanCode)
            .orElseThrow(() -> new NotFoundException("Plan not found"));

        // Upgrade kontrolu
        if (newPlan.getSortOrder() <= oldPlan.getSortOrder()) {
            throw new BadRequestException("Can only upgrade to higher tier plan");
        }

        subscription.setPlan(newPlan);
        subscription.setPrice(findPrice(newPlan, newCycle));
        subscription.setBillingCycle(newCycle);

        subscription = subscriptionRepository.save(subscription);
        recordEvent(subscription, SubscriptionEventType.UPGRADED, oldPlan, newPlan);

        return subscription;
    }

    // Plan dusurme (donem sonunda uygulanacak)
    public Subscription schedulePlanDowngrade(UUID subscriptionId, String newPlanCode, BillingCycle newCycle) {
        Subscription subscription = findById(subscriptionId);
        SubscriptionPlan newPlan = planRepository.findByCode(newPlanCode)
            .orElseThrow(() -> new NotFoundException("Plan not found"));

        subscription.setDowngradeToPlan(newPlan);
        subscription.setDowngradeToPrice(findPrice(newPlan, newCycle));

        return subscriptionRepository.save(subscription);
    }

    // Abonelik iptali (donem sonunda)
    public Subscription cancelSubscription(UUID subscriptionId, String reason) {
        Subscription subscription = findById(subscriptionId);

        subscription.setCancelAtPeriodEnd(true);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setCancellationReason(reason);

        subscription = subscriptionRepository.save(subscription);
        recordEvent(subscription, SubscriptionEventType.CANCELLED, null, null);

        return subscription;
    }

    // Odeme basarisizligi (PAST_DUE durumuna gecis)
    public Subscription markPastDue(UUID subscriptionId) {
        Subscription subscription = findById(subscriptionId);

        SubscriptionStatus previousStatus = subscription.getStatus();
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscription.setGracePeriodEnd(LocalDateTime.now().plusDays(3)); // 3 gun ek sure

        subscription = subscriptionRepository.save(subscription);
        recordEvent(subscription, SubscriptionEventType.PAYMENT_FAILED, previousStatus, SubscriptionStatus.PAST_DUE);

        return subscription;
    }

    // Askiya alma (grace period bitti)
    public Subscription suspendSubscription(UUID subscriptionId) {
        Subscription subscription = findById(subscriptionId);

        subscription.setStatus(SubscriptionStatus.SUSPENDED);

        subscription = subscriptionRepository.save(subscription);
        recordEvent(subscription, SubscriptionEventType.SUSPENDED, SubscriptionStatus.PAST_DUE, SubscriptionStatus.SUSPENDED);

        return subscription;
    }

    // Yenileme (sonraki doneme gecis)
    public Subscription renewSubscription(UUID subscriptionId) {
        Subscription subscription = findById(subscriptionId);

        // Downgrade varsa uygula
        if (subscription.getDowngradeToPlan() != null) {
            subscription.setPlan(subscription.getDowngradeToPlan());
            subscription.setPrice(subscription.getDowngradeToPrice());
            subscription.setDowngradeToPlan(null);
            subscription.setDowngradeToPrice(null);
        }

        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(calculatePeriodEnd(subscription.getBillingCycle()));

        subscription = subscriptionRepository.save(subscription);
        recordEvent(subscription, SubscriptionEventType.RENEWED, null, null);

        return subscription;
    }

    // Zamanlanmis gorevler icin sorgular
    public List<Subscription> findTrialsEndingBefore(LocalDateTime date) {
        return subscriptionRepository.findByStatusAndTrialEndDateBefore(
            SubscriptionStatus.TRIAL, date
        );
    }

    public List<Subscription> findSubscriptionsToRenew() {
        return subscriptionRepository.findByStatusInAndCurrentPeriodEndBefore(
            List.of(SubscriptionStatus.ACTIVE), LocalDateTime.now()
        );
    }

    public List<Subscription> findGracePeriodExpired() {
        return subscriptionRepository.findByStatusAndGracePeriodEndBefore(
            SubscriptionStatus.PAST_DUE, LocalDateTime.now()
        );
    }

    private LocalDateTime calculatePeriodEnd(BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> LocalDateTime.now().plusMonths(1);
            case QUARTERLY -> LocalDateTime.now().plusMonths(3);
            case SEMIANNUAL -> LocalDateTime.now().plusMonths(6);
            case YEARLY -> LocalDateTime.now().plusYears(1);
        };
    }

    private void recordEvent(Subscription subscription, SubscriptionEventType eventType,
                            Object previous, Object current) {
        SubscriptionEvent event = new SubscriptionEvent();
        event.setSubscription(subscription);
        event.setUser(subscription.getUser());
        event.setEventType(eventType);
        // ... diger alanlar
        eventRepository.save(event);
    }
}
```

#### PaymentService.java

```java
@Service
@Transactional
public class PaymentService {

    private final IyzicoApiClient iyzicoClient;
    private final PaymentTransactionRepository transactionRepository;
    private final InvoiceService invoiceService;
    private final SubscriptionService subscriptionService;

    // Kayitli kart ile odeme
    public PaymentResult processPayment(Invoice invoice, PaymentMethod paymentMethod) {
        // Idempotency key olustur
        String conversationId = UUID.randomUUID().toString();

        // Transaction kaydi olustur
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setInvoice(invoice);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setAmount(invoice.getTotalAmount());
        transaction.setStatus(PaymentStatus.PROCESSING);
        transaction.setIyzicoConversationId(conversationId);
        transaction = transactionRepository.save(transaction);

        try {
            // iyzico API cagir
            IyzicoPaymentResult result = iyzicoClient.createPayment(
                paymentMethod.getIyzicoCardToken(),
                paymentMethod.getIyzicoCardUserKey(),
                invoice.getTotalAmount(),
                conversationId,
                buildBuyerInfo(invoice.getUser()),
                buildBasketItems(invoice)
            );

            if (result.isSuccess()) {
                // Basarili odeme
                transaction.setStatus(PaymentStatus.SUCCESS);
                transaction.setIyzicoPaymentId(result.getPaymentId());
                transaction.setProviderResponse(result.getRawResponse());
                transactionRepository.save(transaction);

                // Fatura ve aboneligi guncelle
                invoiceService.markAsPaid(invoice.getId());
                subscriptionService.activateSubscription(invoice.getSubscription().getId());

                return PaymentResult.success(result.getPaymentId());
            } else {
                // Basarisiz odeme
                return handlePaymentFailure(transaction, invoice, result);
            }
        } catch (Exception e) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureMessage(e.getMessage());
            transactionRepository.save(transaction);
            throw new PaymentException("Payment processing failed", e);
        }
    }

    // Yeni kart ile odeme (opsiyonel kaydetme)
    public PaymentResult processPaymentWithNewCard(Invoice invoice, CardDetails cardDetails,
                                                    boolean saveCard) {
        String conversationId = UUID.randomUUID().toString();

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setInvoice(invoice);
        transaction.setAmount(invoice.getTotalAmount());
        transaction.setStatus(PaymentStatus.PROCESSING);
        transaction.setIyzicoConversationId(conversationId);
        transaction = transactionRepository.save(transaction);

        try {
            IyzicoPaymentResult result = iyzicoClient.createPaymentWithCard(
                cardDetails,
                invoice.getTotalAmount(),
                conversationId,
                buildBuyerInfo(invoice.getUser()),
                buildBasketItems(invoice),
                saveCard
            );

            if (result.isSuccess()) {
                transaction.setStatus(PaymentStatus.SUCCESS);
                transaction.setIyzicoPaymentId(result.getPaymentId());
                transactionRepository.save(transaction);

                // Kart kaydetme istendiyse
                if (saveCard && result.getCardToken() != null) {
                    saveCardFromPaymentResult(invoice.getUser(), result);
                }

                invoiceService.markAsPaid(invoice.getId());
                subscriptionService.activateSubscription(invoice.getSubscription().getId());

                return PaymentResult.success(result.getPaymentId());
            } else {
                return handlePaymentFailure(transaction, invoice, result);
            }
        } catch (Exception e) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureMessage(e.getMessage());
            transactionRepository.save(transaction);
            throw new PaymentException("Payment processing failed", e);
        }
    }

    private PaymentResult handlePaymentFailure(PaymentTransaction transaction,
                                                Invoice invoice, IyzicoPaymentResult result) {
        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setFailureCode(result.getErrorCode());
        transaction.setFailureMessage(result.getErrorMessage());
        transaction.setProviderResponse(result.getRawResponse());

        // Retry mekanizmasi
        if (transaction.getAttemptNumber() < 3) {
            transaction.setNextRetryAt(LocalDateTime.now().plusDays(1)); // 24 saat sonra
        }

        transactionRepository.save(transaction);

        // Fatura ve aboneligi guncelle
        invoiceService.markAsFailed(invoice.getId());
        subscriptionService.markPastDue(invoice.getSubscription().getId());

        return PaymentResult.failure(result.getErrorCode(), result.getErrorMessage());
    }
}
```

### 3.4 iyzico Entegrasyonu

#### IyzicoApiClient.java

```java
@Component
public class IyzicoApiClient {

    private final RestTemplate restTemplate;
    private final IyzicoConfig config;

    // iyzico Authentication Header
    private HttpHeaders createAuthHeaders(String payload) {
        String randomString = generateRandomString();
        String hashString = config.getApiKey() + randomString + config.getSecretKey() + payload;
        String hash = sha1(hashString);
        String authString = config.getApiKey() + ":" + hash + ":" + randomString;
        String authorization = "IYZWS " + Base64.getEncoder().encodeToString(authString.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // Odeme olusturma (tokenize kart ile)
    public IyzicoPaymentResult createPayment(String cardToken, String cardUserKey,
                                              BigDecimal amount, String conversationId,
                                              BuyerInfo buyer, List<BasketItem> items) {
        Map<String, Object> request = new HashMap<>();
        request.put("locale", "tr");
        request.put("conversationId", conversationId);
        request.put("price", amount.toString());
        request.put("paidPrice", amount.toString());
        request.put("currency", "TRY");
        request.put("installment", 1);

        // Kart bilgisi (token)
        Map<String, Object> paymentCard = new HashMap<>();
        paymentCard.put("cardToken", cardToken);
        paymentCard.put("cardUserKey", cardUserKey);
        request.put("paymentCard", paymentCard);

        // Alici bilgileri
        request.put("buyer", buyer.toMap());

        // Adres bilgileri
        request.put("billingAddress", buyer.getAddress().toMap());
        request.put("shippingAddress", buyer.getAddress().toMap());

        // Sepet icerigi
        request.put("basketItems", items.stream().map(BasketItem::toMap).toList());

        String payload = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(payload, createAuthHeaders(payload));

        ResponseEntity<String> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/payment/auth",
            entity,
            String.class
        );

        return parsePaymentResult(response.getBody());
    }

    // 3D Secure baslat
    public IyzicoThreeDsResult initiate3DSecure(CardDetails card, BigDecimal amount,
                                                 String conversationId, String callbackUrl,
                                                 BuyerInfo buyer, List<BasketItem> items) {
        Map<String, Object> request = new HashMap<>();
        request.put("locale", "tr");
        request.put("conversationId", conversationId);
        request.put("price", amount.toString());
        request.put("paidPrice", amount.toString());
        request.put("currency", "TRY");
        request.put("callbackUrl", callbackUrl);

        // Kart bilgisi (yeni kart)
        Map<String, Object> paymentCard = new HashMap<>();
        paymentCard.put("cardHolderName", card.getCardHolderName());
        paymentCard.put("cardNumber", card.getCardNumber());
        paymentCard.put("expireMonth", card.getExpireMonth());
        paymentCard.put("expireYear", card.getExpireYear());
        paymentCard.put("cvc", card.getCvc());
        request.put("paymentCard", paymentCard);

        request.put("buyer", buyer.toMap());
        request.put("billingAddress", buyer.getAddress().toMap());
        request.put("shippingAddress", buyer.getAddress().toMap());
        request.put("basketItems", items.stream().map(BasketItem::toMap).toList());

        String payload = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(payload, createAuthHeaders(payload));

        ResponseEntity<String> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/payment/3dsecure/initialize",
            entity,
            String.class
        );

        return parseThreeDsResult(response.getBody());
    }

    // Kart kaydetme
    public IyzicoCardResult storeCard(String email, CardDetails card) {
        Map<String, Object> request = new HashMap<>();
        request.put("locale", "tr");
        request.put("conversationId", UUID.randomUUID().toString());
        request.put("email", email);

        Map<String, Object> cardInfo = new HashMap<>();
        cardInfo.put("cardAlias", card.getCardAlias());
        cardInfo.put("cardHolderName", card.getCardHolderName());
        cardInfo.put("cardNumber", card.getCardNumber());
        cardInfo.put("expireMonth", card.getExpireMonth());
        cardInfo.put("expireYear", card.getExpireYear());
        request.put("card", cardInfo);

        String payload = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(payload, createAuthHeaders(payload));

        ResponseEntity<String> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/cardstorage/card",
            entity,
            String.class
        );

        return parseCardResult(response.getBody());
    }

    // Kayitli kartlari getir
    public List<IyzicoCardInfo> getStoredCards(String cardUserKey) {
        Map<String, Object> request = new HashMap<>();
        request.put("locale", "tr");
        request.put("conversationId", UUID.randomUUID().toString());
        request.put("cardUserKey", cardUserKey);

        String payload = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(payload, createAuthHeaders(payload));

        ResponseEntity<String> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/cardstorage/cards",
            entity,
            String.class
        );

        return parseCardList(response.getBody());
    }

    // Iade
    public IyzicoRefundResult refundPayment(String paymentId, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("locale", "tr");
        request.put("conversationId", UUID.randomUUID().toString());
        request.put("paymentTransactionId", paymentId);
        request.put("price", amount.toString());
        request.put("currency", "TRY");

        String payload = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(payload, createAuthHeaders(payload));

        ResponseEntity<String> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/payment/refund",
            entity,
            String.class
        );

        return parseRefundResult(response.getBody());
    }
}
```

#### IyzicoWebhookService.java

```java
@Service
public class IyzicoWebhookService {

    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final IyzicoConfig config;

    // Webhook imza dogrulama
    public boolean validateSignature(String payload, String signature) {
        String expectedSignature = HmacUtils.hmacSha256Hex(config.getWebhookSecret(), payload);
        return MessageDigest.isEqual(
            signature.getBytes(StandardCharsets.UTF_8),
            expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Odeme webhook'u isleme
    @Transactional
    public void processPaymentWebhook(WebhookPayload payload) {
        // Idempotency kontrolu
        String eventId = payload.getEventId() != null ?
            payload.getEventId() :
            UUID.randomUUID().toString();

        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate webhook ignored: {}", eventId);
            return;
        }

        // Webhook kaydini olustur
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setEventId(eventId);
        event.setEventType(payload.getEventType());
        event.setPayload(objectMapper.writeValueAsString(payload));
        event.setReceivedAt(LocalDateTime.now());
        event.setProcessingStatus("PROCESSING");
        webhookEventRepository.save(event);

        try {
            switch (payload.getStatus()) {
                case "SUCCESS" -> handlePaymentSuccess(payload);
                case "FAILURE" -> handlePaymentFailure(payload);
            }

            event.setProcessingStatus("COMPLETED");
            event.setProcessedAt(LocalDateTime.now());
        } catch (Exception e) {
            event.setProcessingStatus("FAILED");
            event.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            webhookEventRepository.save(event);
        }
    }

    private void handlePaymentSuccess(WebhookPayload payload) {
        PaymentTransaction transaction = transactionRepository
            .findByIyzicoConversationId(payload.getConversationId())
            .orElseThrow(() -> new NotFoundException("Transaction not found"));

        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setIyzicoPaymentId(payload.getPaymentId());
        transactionRepository.save(transaction);

        Invoice invoice = transaction.getInvoice();
        invoiceService.markAsPaid(invoice.getId());
        subscriptionService.activateSubscription(invoice.getSubscription().getId());
    }

    private void handlePaymentFailure(WebhookPayload payload) {
        PaymentTransaction transaction = transactionRepository
            .findByIyzicoConversationId(payload.getConversationId())
            .orElseThrow(() -> new NotFoundException("Transaction not found"));

        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setFailureCode(payload.getErrorCode());
        transaction.setFailureMessage(payload.getErrorMessage());

        // Retry planla (max 3 deneme)
        if (transaction.getAttemptNumber() < 3) {
            transaction.setNextRetryAt(LocalDateTime.now().plusDays(1));
        }
        transactionRepository.save(transaction);

        Invoice invoice = transaction.getInvoice();
        invoiceService.markAsFailed(invoice.getId());
        subscriptionService.markPastDue(invoice.getSubscription().getId());
    }
}
```

---

## 4. Subscription Lifecycle

### 4.1 Durum Makinesi

```
                                    +-------------------+
                                    |   KULLANICI       |
                                    |   KAYIT OLUR      |
                                    +--------+----------+
                                             |
                                             v
                        +--------------------+--------------------+
                        |           PENDING_PAYMENT               |
                        |   (Ilk odeme bekleniyor)                |
                        +--------------------+--------------------+
                                             |
                           Trial baslatilirsa (14 gun)
                                             |
                                             v
+---------------------------+       +--------+----------+
|      TRIAL                |       |                   |
|  (14 gun ucretsiz)        |<------+  hasAccess: TRUE  |
|  hasAccess: TRUE          |       +-------------------+
+-----------+---------------+
            |
            | Trial bitis + odeme
            v
+-----------+---------------+       +-------------------+
|      ACTIVE               |       |                   |
|  (Aktif abonelik)         |<------+  Odeme basarili   |
|  hasAccess: TRUE          |       +-------------------+
+-----------+---------------+
            |
            | Odeme basarisiz
            v
+-----------+---------------+       +-------------------+
|      PAST_DUE             |       |   Grace Period    |
|  (3 gun ek sure)          |<------+   (3 gun)         |
|  hasAccess: TRUE          |       +-------------------+
+-----------+---------------+
            |
            | 3 gun + 3 retry basarisiz
            v
+-----------+---------------+       +-------------------+
|      SUSPENDED            |       |   Erisim          |
|  (Askiya alindi)          |<------+   KAPATILDI       |
|  hasAccess: FALSE         |       +-------------------+
+-----------+---------------+
            |
            | 30 gun odeme yok
            v
+-----------+---------------+
|      EXPIRED              |
|  (Suresi doldu)           |
|  hasAccess: FALSE         |
+---------------------------+


         Kullanici iptal ederse:

+-----------+---------------+       +-------------------+
|    ACTIVE/TRIAL/PAST_DUE  |       |  cancel_at_       |
|                           |------>|  period_end=true  |
+-----------+---------------+       +-------------------+
            |
            | Donem sonu
            v
+-----------+---------------+
|      CANCELLED            |
|  (Iptal edildi)           |
|  hasAccess: FALSE         |
+---------------------------+
```

### 4.2 Durum Aciklamalari

| Status | hasAccess | Aciklama |
|--------|-----------|----------|
| `PENDING_PAYMENT` | FALSE | Kullanici kayit oldu, henuz trial/odeme yok |
| `TRIAL` | TRUE | 14 gunluk ucretsiz deneme suresi aktif |
| `ACTIVE` | TRUE | Odeme yapildi, abonelik aktif |
| `PAST_DUE` | TRUE | Odeme basarisiz, 3 gun ek sure (kullanici hala erisebilir) |
| `SUSPENDED` | FALSE | Grace period bitti, erisim askiya alindi |
| `CANCELLED` | FALSE | Kullanici iptal etti, donem sonunda sonlandi |
| `EXPIRED` | FALSE | 30+ gun odeme yok, abonelik sona erdi |

### 4.3 Zamanlanmis Gorevler (Cron Jobs)

```java
@Component
public class BillingScheduledTasks {

    // Her gun saat 09:00 - Trial kontrol
    @Scheduled(cron = "0 0 9 * * *")
    public void checkTrialExpirations() {
        List<Subscription> expiringTrials = subscriptionService
            .findTrialsEndingBefore(LocalDateTime.now());

        for (Subscription subscription : expiringTrials) {
            // Trial sona erdi, odeme al veya PENDING_PAYMENT'a gec
            try {
                processTrialEnd(subscription);
            } catch (Exception e) {
                log.error("Trial processing failed for {}", subscription.getId(), e);
            }
        }
    }

    // Her gun saat 06:00 - Yenileme kontrol
    @Scheduled(cron = "0 0 6 * * *")
    public void checkRenewals() {
        List<Subscription> toRenew = subscriptionService.findSubscriptionsToRenew();

        for (Subscription subscription : toRenew) {
            try {
                processRenewal(subscription);
            } catch (Exception e) {
                log.error("Renewal failed for {}", subscription.getId(), e);
            }
        }
    }

    // Her 30 dakika - Odeme retry
    @Scheduled(cron = "0 */30 * * * *")
    public void retryFailedPayments() {
        List<PaymentTransaction> toRetry = transactionRepository
            .findByStatusAndNextRetryAtBefore(PaymentStatus.FAILED, LocalDateTime.now());

        for (PaymentTransaction transaction : toRetry) {
            if (transaction.getAttemptNumber() >= 3) continue;

            try {
                retryPayment(transaction);
            } catch (Exception e) {
                log.error("Payment retry failed for {}", transaction.getId(), e);
            }
        }
    }

    // Her gun saat 03:00 - Askiya alinanlar kontrol
    @Scheduled(cron = "0 0 3 * * *")
    public void checkSuspendedSubscriptions() {
        // Grace period biten PAST_DUE'lari SUSPENDED yap
        List<Subscription> gracePeriodExpired = subscriptionService.findGracePeriodExpired();
        for (Subscription subscription : gracePeriodExpired) {
            subscriptionService.suspendSubscription(subscription.getId());
        }

        // 30 gundir SUSPENDED olanlari EXPIRED yap
        List<Subscription> toExpire = subscriptionService
            .findSuspendedOlderThan(LocalDateTime.now().minusDays(30));
        for (Subscription subscription : toExpire) {
            subscriptionService.expireSubscription(subscription.getId());
        }
    }
}
```

---

## 5. Odeme Akislari

### 5.1 Yeni Kart ile Odeme

```
+-------------+     +-------------+     +-------------+     +-------------+
|   Frontend  |     |   Backend   |     |   iyzico    |     |   Banka     |
+------+------+     +------+------+     +------+------+     +------+------+
       |                   |                   |                   |
       | 1. Checkout baslatma                  |                   |
       |   (plan, kart bilgileri)              |                   |
       +------------------>|                   |                   |
       |                   |                   |                   |
       |                   | 2. Invoice olustur                    |
       |                   | 3. Transaction kaydi                  |
       |                   |                   |                   |
       |                   | 4. 3D Secure baslatma                 |
       |                   +------------------>|                   |
       |                   |                   |                   |
       |                   | 5. 3DS HTML content                   |
       |                   |<------------------+                   |
       |                   |                   |                   |
       | 6. 3DS iframe goster                  |                   |
       |<------------------+                   |                   |
       |                   |                   |                   |
       | 7. Kullanici SMS onay                 |                   |
       +--------------------------------------+------------------>|
       |                   |                   |                   |
       |                   |                   | 8. Banka dogrulama|
       |                   |                   |<------------------+
       |                   |                   |                   |
       | 9. Callback (success/fail)            |                   |
       |<------------------+-------------------+                   |
       |                   |                   |                   |
       | 10. 3DS tamamlama istegi              |                   |
       +------------------>|                   |                   |
       |                   |                   |                   |
       |                   | 11. Odeme tamamla |                   |
       |                   +------------------>|                   |
       |                   |                   |                   |
       |                   | 12. Sonuc         |                   |
       |                   |<------------------+                   |
       |                   |                   |                   |
       |                   | 13. Transaction guncelle              |
       |                   |     Invoice PAID                      |
       |                   |     Subscription ACTIVE               |
       |                   |                   |                   |
       | 14. Basari mesaji |                   |                   |
       |<------------------+                   |                   |
       |                   |                   |                   |
```

### 5.2 Kayitli Kart ile Odeme

```
+-------------+     +-------------+     +-------------+
|   Frontend  |     |   Backend   |     |   iyzico    |
+------+------+     +------+------+     +------+------+
       |                   |                   |
       | 1. Odeme istegi   |                   |
       |   (paymentMethodId)                   |
       +------------------>|                   |
       |                   |                   |
       |                   | 2. Kayitli karti bul
       |                   |    (cardToken, cardUserKey)
       |                   |                   |
       |                   | 3. Invoice olustur
       |                   | 4. Transaction kaydi
       |                   |                   |
       |                   | 5. Token ile odeme
       |                   +------------------>|
       |                   |                   |
       |                   | 6. Sonuc          |
       |                   |<------------------+
       |                   |                   |
       |                   | 7. SUCCESS: Invoice PAID
       |                   |            Subscription ACTIVE
       |                   |    FAIL: Invoice FAILED
       |                   |          Subscription PAST_DUE
       |                   |          Retry planla
       |                   |                   |
       | 8. Sonuc mesaji   |                   |
       |<------------------+                   |
       |                   |                   |
```

### 5.3 Retry Mekanizmasi

```
Odeme Basarisiz (Gun 0)
        |
        v
   Transaction: attempt=1, status=FAILED
   Invoice: status=FAILED
   Subscription: status=PAST_DUE, grace_period_end=Gun+3
        |
        | 24 saat sonra (Gun 1)
        v
   Retry #2 (Cron Job)
   Transaction: attempt=2
        |
        +---> SUCCESS: Odeme basarili, ACTIVE
        |
        +---> FAIL: next_retry_at=Gun+2
              |
              | 24 saat sonra (Gun 2)
              v
         Retry #3 (Son deneme)
         Transaction: attempt=3
              |
              +---> SUCCESS: Odeme basarili, ACTIVE
              |
              +---> FAIL: Max retry ulasildi
                    |
                    | Gun 3 (Grace period bitis)
                    v
              Subscription: SUSPENDED
              (Kullanici artik sisteme erisemez)
                    |
                    | 30 gun sonra
                    v
              Subscription: EXPIRED
```

### 5.4 Webhook Isleme

```java
@RestController
@RequestMapping("/api/billing/webhook/iyzico")
public class IyzicoWebhookController {

    // Public endpoint - JWT gerektirmez
    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Iyzico-Signature") String signature) {

        // 1. Imza dogrula
        if (!webhookService.validateSignature(payload, signature)) {
            return ResponseEntity.status(401).build();
        }

        // 2. 5 saniye icinde 200 don (iyzico gerekliligi)
        CompletableFuture.runAsync(() -> {
            try {
                webhookService.processPaymentWebhook(parsePayload(payload));
            } catch (Exception e) {
                log.error("Webhook processing failed", e);
            }
        });

        return ResponseEntity.ok().build();
    }

    @PostMapping("/3ds-callback")
    public ResponseEntity<Void> handle3DSCallback(@RequestBody String payload) {
        webhookService.process3DsCallback(parsePayload(payload));
        return ResponseEntity.ok().build();
    }
}
```

---

## 6. Frontend Mimarisi

### 6.1 Dosya Yapisi

```
sellerx-frontend/
├── app/
│   ├── api/billing/                     # BFF API routes
│   │   ├── checkout/
│   │   │   ├── start/route.ts
│   │   │   └── complete-3ds/route.ts
│   │   ├── features/
│   │   │   ├── route.ts
│   │   │   └── [code]/route.ts
│   │   ├── invoices/
│   │   │   ├── route.ts
│   │   │   └── [id]/route.ts
│   │   ├── payment-methods/
│   │   │   ├── route.ts
│   │   │   └── [id]/
│   │   │       ├── route.ts
│   │   │       └── default/route.ts
│   │   ├── plans/route.ts
│   │   └── subscription/
│   │       ├── route.ts
│   │       ├── trial/route.ts
│   │       ├── plan/route.ts
│   │       ├── cancel/route.ts
│   │       └── reactivate/route.ts
│   └── [locale]/(app-shell)/billing/
│       ├── page.tsx                      # Billing dashboard
│       └── checkout/page.tsx             # Checkout flow
├── components/billing/
│   ├── index.ts
│   ├── pricing-card.tsx
│   ├── subscription-status-card.tsx
│   ├── payment-method-card.tsx
│   ├── add-card-modal.tsx
│   ├── invoice-list.tsx
│   └── feature-gate.tsx
├── hooks/queries/
│   └── use-billing.ts                    # React Query hooks
├── types/
│   └── billing.ts                        # TypeScript tipleri
└── lib/api/
    └── client.ts                         # API client
```

### 6.2 Type Tanimlari

```typescript
// types/billing.ts

// Enums
export type BillingCycle = 'MONTHLY' | 'QUARTERLY' | 'SEMIANNUAL' | 'YEARLY';
export type SubscriptionStatus =
  'PENDING_PAYMENT' | 'TRIAL' | 'ACTIVE' | 'PAST_DUE' |
  'SUSPENDED' | 'CANCELLED' | 'EXPIRED';
export type InvoiceStatus = 'DRAFT' | 'PENDING' | 'PAID' | 'FAILED' | 'VOID' | 'REFUNDED';
export type FeatureType = 'BOOLEAN' | 'LIMIT' | 'UNLIMITED';

// Plan Types
export interface SubscriptionPlan {
  id: string;
  code: string;                    // FREE, STARTER, PRO, ENTERPRISE
  name: string;
  description: string | null;
  maxStores: number | null;        // null = sinirsiz
  features: Record<string, any>;
  sortOrder: number;
}

export interface SubscriptionPrice {
  id: string;
  billingCycle: BillingCycle;
  price: number;
  discountPercentage: number | null;
  monthlyEquivalent?: number;
  currency: string;
}

export interface PlanWithPrices extends SubscriptionPlan {
  prices: SubscriptionPrice[];
}

// Subscription
export interface Subscription {
  id: string;
  userId: number;
  status: SubscriptionStatus;
  planCode: string;
  planName: string;
  maxStores: number | null;
  billingCycle: BillingCycle;
  price: number | null;
  currency: string;
  trialEndDate: string | null;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
  autoRenew: boolean;
  hasDowngradeScheduled: boolean;
  isInTrial: boolean;
  hasAccess: boolean;
  daysRemaining: number;
  trialDaysRemaining: number;
}

// Payment Method
export interface PaymentMethod {
  id: string;
  type: 'CREDIT_CARD' | 'DEBIT_CARD';
  cardAlias: string | null;
  cardLastFour: string;
  cardBrand: string;
  cardFamily: string | null;
  cardBankName: string | null;
  cardExpMonth: number;
  cardExpYear: number;
  isDefault: boolean;
  createdAt: string;
}

// Invoice
export interface Invoice {
  id: string;
  invoiceNumber: string;
  status: InvoiceStatus;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
  currency: string;
  billingPeriodStart: string;
  billingPeriodEnd: string;
  dueDate: string;
  paidAt: string | null;
  lineItems: InvoiceLineItem[];
}

// Feature
export interface FeatureInfo {
  featureCode: string;
  enabled: boolean;
  featureType: FeatureType;
  limit: number | null;
  currentUsage: number | null;
}

// Checkout
export interface CheckoutRequest {
  planCode: string;
  billingCycle: BillingCycle;
  paymentMethodId?: string;
  cardDetails?: {
    cardHolderName: string;
    cardNumber: string;
    expireMonth: string;
    expireYear: string;
    cvc: string;
  };
}

export interface CheckoutResponse {
  success: boolean;
  status: 'SUCCESS' | 'TRIAL_STARTED' | 'REQUIRES_3DS' | 'ERROR';
  subscriptionId?: string;
  paymentId?: string;
  requires3DS: boolean;
  threeDSHtmlContent?: string;
  errorMessage?: string;
}

// Paginated Response
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
```

### 6.3 React Query Hooks

```typescript
// hooks/queries/use-billing.ts

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { billingApi } from '@/lib/api/client';

// Query Keys
export const billingKeys = {
  all: ['billing'] as const,
  plans: () => [...billingKeys.all, 'plans'] as const,
  subscription: () => [...billingKeys.all, 'subscription'] as const,
  paymentMethods: () => [...billingKeys.all, 'payment-methods'] as const,
  invoices: (page?: number) => [...billingKeys.all, 'invoices', { page }] as const,
  features: () => [...billingKeys.all, 'features'] as const,
};

// ========== Plans (Public) ==========
export function usePlans() {
  return useQuery({
    queryKey: billingKeys.plans(),
    queryFn: billingApi.getPlans,
    staleTime: 1000 * 60 * 60, // 1 saat - planlar sik degismez
  });
}

// ========== Subscription ==========
export function useSubscription() {
  return useQuery({
    queryKey: billingKeys.subscription(),
    queryFn: async () => {
      try {
        return await billingApi.getSubscription();
      } catch (error: any) {
        if (error.status === 404) return null; // Abonelik yok
        throw error;
      }
    },
    staleTime: 1000 * 60 * 5, // 5 dakika
  });
}

export function useStartTrial() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: billingApi.startTrial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
    },
  });
}

export function useChangePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: billingApi.changePlan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
      queryClient.invalidateQueries({ queryKey: billingKeys.invoices() });
    },
  });
}

export function useCancelSubscription() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: billingApi.cancelSubscription,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
    },
  });
}

// ========== Payment Methods ==========
export function usePaymentMethods() {
  return useQuery({
    queryKey: billingKeys.paymentMethods(),
    queryFn: billingApi.getPaymentMethods,
    staleTime: 1000 * 60 * 5,
  });
}

export function useAddPaymentMethod() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: billingApi.addPaymentMethod,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.paymentMethods() });
    },
  });
}

export function useDeletePaymentMethod() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: billingApi.deletePaymentMethod,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.paymentMethods() });
    },
  });
}

// ========== Invoices ==========
export function useInvoices(page: number = 0, size: number = 10) {
  return useQuery({
    queryKey: billingKeys.invoices(page),
    queryFn: () => billingApi.getInvoices(page, size),
    staleTime: 1000 * 60 * 5,
  });
}

// ========== Checkout ==========
export function useCheckout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: billingApi.startCheckout,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
      queryClient.invalidateQueries({ queryKey: billingKeys.paymentMethods() });
    },
  });
}

// ========== Features ==========
export function useFeatures() {
  return useQuery({
    queryKey: billingKeys.features(),
    queryFn: billingApi.getFeatures,
    staleTime: 1000 * 60 * 5,
  });
}

// ========== Utility Hooks ==========
export function useHasAccess() {
  const { data: subscription, isLoading } = useSubscription();
  return {
    hasAccess: subscription?.hasAccess ?? false,
    isInTrial: subscription?.isInTrial ?? false,
    subscription,
    isLoading,
  };
}

export function useCanAddStore(currentStoreCount: number) {
  const { data: subscription } = useSubscription();
  if (!subscription) {
    return { canAdd: currentStoreCount < 1, maxStores: 1 };
  }
  const maxStores = subscription.maxStores;
  if (maxStores === null) {
    return { canAdd: true, maxStores: Infinity };
  }
  return { canAdd: currentStoreCount < maxStores, maxStores };
}
```

### 6.4 Billing Components

#### PricingCard.tsx

```tsx
'use client';

interface PricingCardProps {
  plan: PlanWithPrices;
  selectedCycle?: BillingCycle;
  isCurrentPlan?: boolean;
  onSelect?: (planCode: string) => void;
}

export function PricingCard({ plan, selectedCycle = 'MONTHLY', isCurrentPlan, onSelect }: PricingCardProps) {
  const price = plan.prices?.find(p => p.billingCycle === selectedCycle);
  const monthlyPrice = price?.price ?? 0;
  const monthlyEquivalent = price?.monthlyEquivalent ?? monthlyPrice;
  const discount = price?.discountPercentage ?? 0;

  const features = plan.featuresMap ?? plan.features ?? {};

  return (
    <Card className={cn(
      "relative",
      plan.isPopular && "border-primary scale-105",
      isCurrentPlan && "border-green-500"
    )}>
      {plan.isPopular && (
        <div className="absolute -top-3 left-1/2 -translate-x-1/2">
          <Badge>En Populer</Badge>
        </div>
      )}

      <CardHeader>
        <CardTitle>{plan.name}</CardTitle>
        <CardDescription>{plan.description}</CardDescription>
      </CardHeader>

      <CardContent>
        <div className="text-3xl font-bold">
          {formatCurrency(monthlyEquivalent, 'TRY')}
          <span className="text-sm font-normal">/ay</span>
        </div>

        {discount > 0 && (
          <Badge variant="secondary">%{discount} indirim</Badge>
        )}

        <ul className="mt-4 space-y-2">
          <li className="flex items-center gap-2">
            <Check className="h-4 w-4 text-green-500" />
            {plan.maxStores === null ? 'Sinirsiz magaza' : `${plan.maxStores} magaza`}
          </li>
          {features.advanced_analytics && (
            <li className="flex items-center gap-2">
              <Check className="h-4 w-4 text-green-500" />
              Gelismis analitik
            </li>
          )}
          {features.webhook_support && (
            <li className="flex items-center gap-2">
              <Check className="h-4 w-4 text-green-500" />
              Webhook destegi
            </li>
          )}
          {features.api_access && (
            <li className="flex items-center gap-2">
              <Check className="h-4 w-4 text-green-500" />
              API erisimi
            </li>
          )}
        </ul>
      </CardContent>

      <CardFooter>
        <Button
          className="w-full"
          onClick={() => onSelect?.(plan.code)}
          disabled={isCurrentPlan}
        >
          {isCurrentPlan ? 'Mevcut Plan' : 'Sec'}
        </Button>
      </CardFooter>
    </Card>
  );
}
```

#### FeatureGate.tsx

```tsx
'use client';

interface FeatureGateProps {
  feature: string;
  children: ReactNode;
  fallback?: ReactNode;
  onUpgrade?: () => void;
}

export function FeatureGate({ feature, children, fallback, onUpgrade }: FeatureGateProps) {
  const { data: features, isLoading } = useFeatures();

  if (isLoading) return <Skeleton className="h-20 w-full" />;

  const featureInfo = features?.find(f => f.featureCode === feature);
  const hasAccess = featureInfo?.enabled ?? false;

  if (hasAccess) return <>{children}</>;

  return fallback ?? (
    <Card className="border-dashed">
      <CardContent className="flex flex-col items-center justify-center py-8">
        <Lock className="h-12 w-12 text-muted-foreground mb-4" />
        <h3 className="font-semibold">Premium Ozellik</h3>
        <p className="text-sm text-muted-foreground mb-4">
          Bu ozellige erismek icin planinizy yukseltin.
        </p>
        <Button onClick={onUpgrade}>Plani Yukselt</Button>
      </CardContent>
    </Card>
  );
}

// Limit bazli kontrol
interface FeatureLimitGateProps {
  feature: string;
  currentUsage: number;
  children: ReactNode;
  onLimitReached?: () => void;
}

export function FeatureLimitGate({ feature, currentUsage, children, onLimitReached }: FeatureLimitGateProps) {
  const { data: features } = useFeatures();

  const featureInfo = features?.find(f => f.featureCode === feature);

  if (!featureInfo) return <>{children}</>;
  if (featureInfo.featureType === 'UNLIMITED' || featureInfo.featureType === 'BOOLEAN') {
    return <>{children}</>;
  }

  const limit = featureInfo.limit ?? 0;
  if (currentUsage < limit) return <>{children}</>;

  return (
    <Card className="border-orange-500">
      <CardContent className="py-6">
        <AlertTriangle className="h-8 w-8 text-orange-500 mb-2" />
        <h3 className="font-semibold">Limite Ulasildi</h3>
        <p className="text-sm text-muted-foreground">
          {currentUsage} / {limit} kullanim
        </p>
        <Button variant="outline" onClick={onLimitReached}>Limiti Arttir</Button>
      </CardContent>
    </Card>
  );
}

// Magaza limiti kontrolu
interface StoreGateProps {
  currentStoreCount: number;
  children: ReactNode;
  onUpgrade?: () => void;
}

export function StoreGate({ currentStoreCount, children, onUpgrade }: StoreGateProps) {
  const { data: subscription } = useSubscription();

  const maxStores = subscription?.maxStores;

  if (maxStores === null) return <>{children}</>;
  if (currentStoreCount < maxStores) return <>{children}</>;

  return (
    <Card className="border-dashed border-orange-500">
      <CardContent className="py-8 text-center">
        <Store className="h-12 w-12 text-orange-500 mx-auto mb-4" />
        <h3 className="font-semibold">Magaza Limitine Ulasildi</h3>
        <p className="text-sm text-muted-foreground mb-4">
          {currentStoreCount} / {maxStores} magaza kullaniyor
        </p>
        <Button onClick={onUpgrade}>Plani Yukselt</Button>
      </CardContent>
    </Card>
  );
}
```

---

## 7. Feature Gating

### 7.1 Feature Kodlari

| Kod | Tip | Aciklama |
|-----|-----|----------|
| `max_stores` | LIMIT | Maksimum magaza sayisi |
| `ai_qa_responses` | LIMIT | Aylik AI QA yanit limiti |
| `advanced_analytics` | BOOLEAN | Gelismis analitik paneli |
| `webhook_support` | BOOLEAN | Webhook entegrasyonu |
| `api_access` | BOOLEAN | API erisimi |
| `priority_support` | BOOLEAN | Oncelikli destek |
| `parasut_integration` | BOOLEAN | Parasut e-fatura |

### 7.2 Plan-Feature Matrisi

| Feature | FREE | STARTER | PRO | ENTERPRISE |
|---------|------|---------|-----|------------|
| max_stores | 1 | 3 | 10 | UNLIMITED |
| ai_qa_responses | 10 | 100 | 500 | UNLIMITED |
| advanced_analytics | - | + | + | + |
| webhook_support | - | - | + | + |
| api_access | - | - | + | + |
| priority_support | - | - | - | + |
| parasut_integration | - | + | + | + |

### 7.3 Backend Feature Kontrolu

```java
@Service
public class FeatureService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanFeatureRepository featureRepository;
    private final FeatureUsageRepository usageRepository;

    public boolean hasFeature(Long userId, String featureCode) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
            .orElse(null);

        if (subscription == null || !subscription.hasAccess()) {
            return false;
        }

        PlanFeature feature = featureRepository
            .findByPlanIdAndFeatureCode(subscription.getPlan().getId(), featureCode)
            .orElse(null);

        if (feature == null) return false;

        return feature.isEnabled();
    }

    public FeatureAccessResult checkFeatureAccess(Long userId, String featureCode) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("Subscription not found"));

        PlanFeature feature = featureRepository
            .findByPlanIdAndFeatureCode(subscription.getPlan().getId(), featureCode)
            .orElseThrow(() -> new NotFoundException("Feature not found"));

        if (!feature.isEnabled()) {
            return FeatureAccessResult.denied("Bu ozellik planiniaza dahil degil");
        }

        if (feature.getFeatureType() == FeatureType.BOOLEAN) {
            return FeatureAccessResult.allowed();
        }

        if (feature.getFeatureType() == FeatureType.UNLIMITED) {
            return FeatureAccessResult.allowed();
        }

        // LIMIT tipi - kullanim kontrolu
        int currentUsage = getCurrentUsage(userId, featureCode);
        int limit = feature.getLimitValue();

        if (currentUsage >= limit) {
            return FeatureAccessResult.limitReached(
                "Aylik limit asildi: " + currentUsage + "/" + limit,
                limit,
                currentUsage
            );
        }

        return FeatureAccessResult.allowed(limit, currentUsage);
    }

    @Transactional
    public void incrementUsage(Long userId, String featureCode) {
        LocalDateTime periodStart = LocalDateTime.now()
            .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime periodEnd = periodStart.plusMonths(1);

        FeatureUsage usage = usageRepository
            .findByUserIdAndFeatureCodeAndPeriodStart(userId, featureCode, periodStart)
            .orElseGet(() -> {
                FeatureUsage newUsage = new FeatureUsage();
                newUsage.setUserId(userId);
                newUsage.setFeatureCode(featureCode);
                newUsage.setPeriodStart(periodStart);
                newUsage.setPeriodEnd(periodEnd);
                newUsage.setUsageCount(0);
                return newUsage;
            });

        usage.setUsageCount(usage.getUsageCount() + 1);
        usageRepository.save(usage);
    }
}
```

### 7.4 Frontend Kullanim Ornekleri

```tsx
// Magaza olusturma sayfasinda
function NewStorePage() {
  const { data: stores } = useStores();
  const router = useRouter();

  return (
    <StoreGate
      currentStoreCount={stores?.length ?? 0}
      onUpgrade={() => router.push('/billing')}
    >
      <CreateStoreForm />
    </StoreGate>
  );
}

// Gelismis analitik sayfasinda
function AnalyticsPage() {
  return (
    <FeatureGate
      feature="advanced_analytics"
      onUpgrade={() => router.push('/billing')}
    >
      <AdvancedAnalyticsDashboard />
    </FeatureGate>
  );
}

// AI QA yanit gonderirken
function QAResponseForm() {
  const { data: features } = useFeatures();
  const currentUsage = useQAUsageCount();

  return (
    <FeatureLimitGate
      feature="ai_qa_responses"
      currentUsage={currentUsage}
      onLimitReached={() => router.push('/billing')}
    >
      <AIResponseForm />
    </FeatureLimitGate>
  );
}
```

---

## 8. API Referansi

### 8.1 Plans API (Public)

#### GET /api/billing/plans

Tum aktif planlari listeler.

**Authentication**: Gerektirmez

**Response**:
```json
[
  {
    "id": "uuid",
    "code": "STARTER",
    "name": "Starter",
    "description": "Baslangic plani - 3 magaza",
    "maxStores": 3,
    "featuresMap": {
      "max_stores": 3,
      "ai_qa_responses": 100,
      "advanced_analytics": true,
      "parasut_integration": true
    },
    "sortOrder": 1,
    "prices": [
      {
        "id": "uuid",
        "billingCycle": "MONTHLY",
        "price": 299.00,
        "discountPercentage": 0,
        "monthlyEquivalent": 299.00,
        "currency": "TRY"
      },
      {
        "id": "uuid",
        "billingCycle": "QUARTERLY",
        "price": 807.30,
        "discountPercentage": 10,
        "monthlyEquivalent": 269.10,
        "currency": "TRY"
      }
    ]
  }
]
```

### 8.2 Subscription API

#### GET /api/billing/subscription

Mevcut kullanicinin aboneligini getirir.

**Authentication**: Bearer Token (JWT)

**Response**:
```json
{
  "id": "uuid",
  "userId": 1,
  "status": "TRIAL",
  "planCode": "STARTER",
  "planName": "Starter",
  "maxStores": 3,
  "billingCycle": "MONTHLY",
  "price": 299.00,
  "currency": "TRY",
  "trialEndDate": "2026-02-01T00:00:00Z",
  "currentPeriodStart": "2026-01-18T00:00:00Z",
  "currentPeriodEnd": "2026-02-01T00:00:00Z",
  "cancelAtPeriodEnd": false,
  "autoRenew": true,
  "hasDowngradeScheduled": false,
  "isInTrial": true,
  "hasAccess": true,
  "daysRemaining": 14,
  "trialDaysRemaining": 14
}
```

#### POST /api/billing/subscription/trial

Trial baslatir.

**Request**:
```json
{
  "planCode": "STARTER",
  "billingCycle": "MONTHLY"
}
```

#### PUT /api/billing/subscription/plan

Plan degistirir.

**Request**:
```json
{
  "planCode": "PRO",
  "billingCycle": "MONTHLY"
}
```

#### POST /api/billing/subscription/cancel

Aboneligi iptal eder (donem sonunda).

**Request**:
```json
{
  "reason": "Daha fazla ihtiyacim yok",
  "immediate": false
}
```

### 8.3 Payment Methods API

#### GET /api/billing/payment-methods

Kayitli kartlari listeler.

**Response**:
```json
[
  {
    "id": "uuid",
    "type": "CREDIT_CARD",
    "cardAlias": "Is Bankasi Kartim",
    "cardLastFour": "1234",
    "cardBrand": "MASTERCARD",
    "cardFamily": "Bonus",
    "cardBankName": "Turkiye Is Bankasi",
    "cardExpMonth": 12,
    "cardExpYear": 2027,
    "isDefault": true,
    "createdAt": "2026-01-15T10:00:00Z"
  }
]
```

#### POST /api/billing/payment-methods

Yeni kart ekler.

**Request**:
```json
{
  "cardAlias": "Is Bankasi Kartim",
  "cardHolderName": "AHMET YILMAZ",
  "cardNumber": "5528790000000008",
  "expireMonth": "12",
  "expireYear": "27"
}
```

### 8.4 Invoices API

#### GET /api/billing/invoices

Faturalari listeler (paginated).

**Query Params**: `?page=0&size=10`

**Response**:
```json
{
  "content": [
    {
      "id": "uuid",
      "invoiceNumber": "INV-2026-000001",
      "status": "PAID",
      "subtotal": 249.17,
      "taxRate": 20,
      "taxAmount": 49.83,
      "totalAmount": 299.00,
      "currency": "TRY",
      "billingPeriodStart": "2026-01-01T00:00:00Z",
      "billingPeriodEnd": "2026-02-01T00:00:00Z",
      "dueDate": "2026-01-08T00:00:00Z",
      "paidAt": "2026-01-01T10:30:00Z",
      "lineItems": [
        {
          "description": "Starter Plan - Aylik",
          "quantity": 1,
          "unitPrice": 249.17,
          "amount": 249.17
        }
      ]
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "first": true,
  "last": true,
  "empty": false
}
```

### 8.5 Checkout API

#### POST /api/billing/checkout/start

Checkout baslatir.

**Request (Kayitli kart ile)**:
```json
{
  "planCode": "PRO",
  "billingCycle": "MONTHLY",
  "paymentMethodId": "uuid"
}
```

**Request (Yeni kart ile)**:
```json
{
  "planCode": "PRO",
  "billingCycle": "MONTHLY",
  "cardDetails": {
    "cardHolderName": "AHMET YILMAZ",
    "cardNumber": "5528790000000008",
    "expireMonth": "12",
    "expireYear": "27",
    "cvc": "123"
  }
}
```

**Response (Basarili)**:
```json
{
  "success": true,
  "status": "SUCCESS",
  "subscriptionId": "uuid",
  "paymentId": "iyzico-payment-id",
  "requires3DS": false
}
```

**Response (3DS gerekli)**:
```json
{
  "success": false,
  "status": "REQUIRES_3DS",
  "requires3DS": true,
  "threeDSHtmlContent": "<html>...</html>"
}
```

### 8.6 Features API

#### GET /api/billing/features

Tum ozellikleri listeler.

**Response**:
```json
[
  {
    "code": "max_stores",
    "name": "Maksimum Magaza",
    "hasAccess": true,
    "limit": 3,
    "currentUsage": 1,
    "remaining": 2
  },
  {
    "code": "ai_qa_responses",
    "name": "AI QA Yanit",
    "hasAccess": true,
    "limit": 100,
    "currentUsage": 45,
    "remaining": 55
  },
  {
    "code": "advanced_analytics",
    "name": "Gelismis Analitik",
    "hasAccess": true,
    "limit": null,
    "currentUsage": null,
    "remaining": null
  }
]
```

### 8.7 Hata Kodlari

| HTTP Status | Kod | Aciklama |
|-------------|-----|----------|
| 400 | INVALID_REQUEST | Gecersiz istek |
| 401 | UNAUTHORIZED | Yetkilendirme hatasi |
| 403 | FORBIDDEN | Erisim engellendi |
| 404 | NOT_FOUND | Kaynak bulunamadi |
| 409 | CONFLICT | Cakisma (ornegin: zaten abone) |
| 422 | PAYMENT_FAILED | Odeme basarisiz |
| 500 | INTERNAL_ERROR | Sunucu hatasi |

---

## 9. Konfigurasyon

### 9.1 Environment Variables

#### Backend (application.yaml)

```yaml
# iyzico Ayarlari
iyzico:
  api-key: ${IYZICO_API_KEY}
  secret-key: ${IYZICO_SECRET_KEY}
  base-url: ${IYZICO_BASE_URL:https://sandbox-api.iyzipay.com}
  callback-url: ${IYZICO_CALLBACK_URL:http://localhost:8080/api/billing/webhook/iyzico}
  webhook-secret: ${IYZICO_WEBHOOK_SECRET}

# Abonelik Ayarlari
subscription:
  trial-days: 14
  grace-period-days: 3
  max-retry-attempts: 3

# Fatura Ayarlari
invoice:
  series: "SEL"
  tax-rate: 20  # %20 KDV
```

#### Frontend (.env.local)

```env
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 9.2 iyzico Sandbox vs Production

| Ortam | Base URL | Test Karti |
|-------|----------|------------|
| Sandbox | https://sandbox-api.iyzipay.com | 5528790000000008 |
| Production | https://api.iyzipay.com | Gercek kartlar |

### 9.3 Zamanlanmis Gorevler (Cron)

| Gorev | Zamanlama | Aciklama |
|-------|-----------|----------|
| Trial kontrol | Her gun 09:00 | Trial biten abonelikleri kontrol et |
| Yenileme kontrol | Her gun 06:00 | Yenilenmesi gereken abonelikleri kontrol et |
| Odeme retry | Her 30 dakika | Basarisiz odemeleri tekrar dene |
| Askiya alma | Her gun 03:00 | Grace period biten abonelikleri askiya al |

---

## 10. Test ve Debug

### 10.1 iyzico Sandbox Test Kartlari

| Kart No | Sonuc | Aciklama |
|---------|-------|----------|
| 5528790000000008 | Basarili | Normal islem |
| 5400360000000003 | Basarisiz | Yetersiz bakiye |
| 5406670000000009 | 3DS Gerekli | 3D Secure zorunlu |

**Tum kartlar icin**:
- Son Kullanma: Gelecek tarih (ornegin: 12/2027)
- CVV: 123

### 10.2 Test Senaryolari

#### Senaryo 1: Basarili Trial Baslama
```bash
curl -X POST http://localhost:8080/api/billing/subscription/trial \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"planCode":"STARTER","billingCycle":"MONTHLY"}'
```

#### Senaryo 2: Kayitli Kart ile Odeme
```bash
curl -X POST http://localhost:8080/api/billing/checkout/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"planCode":"PRO","billingCycle":"MONTHLY","paymentMethodId":"uuid"}'
```

#### Senaryo 3: Feature Kontrolu
```bash
curl http://localhost:8080/api/billing/features \
  -H "Authorization: Bearer $TOKEN"
```

### 10.3 Yayin Hatalar ve Cozumler

#### Hata: 401 Unauthorized
**Neden**: JWT token gecersiz veya suresi dolmus
**Cozum**: Yeniden login olun veya token refresh yapin

#### Hata: 404 Subscription not found
**Neden**: Kullanicinin aboneligi yok
**Cozum**: Oncelikle trial baslatin: `POST /api/billing/subscription/trial`

#### Hata: Payment failed - Card declined
**Neden**: Kart reddedildi
**Cozum**: Farkli kart deneyin veya sandbox test karti kullanin

#### Hata: 3DS callback failed
**Neden**: 3D Secure dogrulamasi basarisiz
**Cozum**: Callback URL'in dogru konfigüre edildiginden emin olun

### 10.4 Log Analizi

Backend loglari:
```bash
# Odeme loglari
grep "PaymentService" /path/to/logs/app.log

# iyzico API cagrilari
grep "IyzicoApiClient" /path/to/logs/app.log

# Webhook loglari
grep "IyzicoWebhookService" /path/to/logs/app.log
```

### 10.5 Veritabani Kontrol Sorgulari

```sql
-- Kullanicinin aboneligini kontrol et
SELECT s.*, p.code as plan_code, p.name as plan_name
FROM subscriptions s
JOIN subscription_plans p ON s.plan_id = p.id
WHERE s.user_id = 1;

-- Son odemeleri kontrol et
SELECT pt.*, i.invoice_number, i.status as invoice_status
FROM payment_transactions pt
JOIN invoices i ON pt.invoice_id = i.id
WHERE i.user_id = 1
ORDER BY pt.created_at DESC
LIMIT 10;

-- Feature kullanim durumu
SELECT * FROM feature_usage
WHERE user_id = 1
AND period_start >= DATE_TRUNC('month', CURRENT_DATE);

-- Abonelik event history
SELECT * FROM subscription_events
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 20;
```

---

## Ek: Hizli Baslangic Kontrol Listesi

Yeni gelistirici icin:

- [ ] PostgreSQL calisyor mu? (`./db.sh status`)
- [ ] Backend calisyor mu? (`curl http://localhost:8080/actuator/health`)
- [ ] Frontend calisyor mu? (`curl http://localhost:3000`)
- [ ] iyzico sandbox credentials ayarlandi mi? (`application.yaml`)
- [ ] Test kullanici olusturuldu mu? (`test@test.com / 123456`)
- [ ] Trial baslatabiliyor musunuz?
- [ ] Fatura listesi donuyor mu?
- [ ] Feature listesi donuyor mu?

---

**Dokuman Sonu**

Bu dokuman SellerX Billing sisteminin tam teknik aciklamasini icermektedir. Sorulariniz icin gelistirici ekibine danisabilirsiniz.
