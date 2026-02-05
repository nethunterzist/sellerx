# Veritabanı Şeması

> SellerX PostgreSQL veritabanı yapısı ve migration'ları.

## Genel Bakış

SellerX, PostgreSQL 15 kullanır. Flyway ile migration yönetimi yapılır.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        VERİTABANI ŞEMASI                                │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│    users     │────▶│    stores    │────▶│ trendyol_products│
└──────────────┘     └──────────────┘     └──────────────────┘
       │                    │                      │
       │                    │                      │
       ▼                    ▼                      ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│store_expenses│     │trendyol_orders│    │  webhook_events  │
└──────────────┘     └──────────────┘     └──────────────────┘
```

---

## Ana Tablolar

### 1. users

Kullanıcı hesapları.

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) DEFAULT 'USER',
    selected_store_id UUID,
    preferences JSONB,              -- Kullanıcı tercihleri
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
```

| Kolon | Tip | Açıklama |
|-------|-----|----------|
| id | BIGSERIAL | Primary key |
| email | VARCHAR(255) | Unique email |
| password | VARCHAR(255) | BCrypt hash |
| role | VARCHAR(50) | USER, ADMIN |
| selected_store_id | UUID | Aktif mağaza FK |
| preferences | JSONB | Tema, dil vb. |

### 2. stores

Mağaza tanımları ve Trendyol API credentials.

```sql
CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),
    store_name VARCHAR(255) NOT NULL,
    marketplace VARCHAR(50) NOT NULL,         -- trendyol, hepsiburada
    credentials JSONB,                         -- API keys (encrypted)

    -- Webhook tracking
    webhook_id VARCHAR(255),
    webhook_status VARCHAR(50) DEFAULT 'pending',
    webhook_error_message TEXT,

    -- Sync tracking
    sync_status VARCHAR(50) DEFAULT 'pending',
    sync_error_message TEXT,
    initial_sync_completed BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_stores_user ON stores(user_id);
CREATE INDEX idx_stores_marketplace ON stores(marketplace);
```

#### credentials JSONB Yapısı

```json
{
  "sellerId": "123456",
  "apiKey": "abc123...",
  "apiSecret": "xyz789..."
}
```

#### sync_status Değerleri

| Status | Açıklama |
|--------|----------|
| pending | Henüz başlamadı |
| SYNCING_PRODUCTS | Ürünler çekiliyor |
| SYNCING_ORDERS | Siparişler çekiliyor |
| SYNCING_FINANCIAL | Finansal veriler çekiliyor |
| COMPLETED | Tamamlandı |
| FAILED | Başarısız |

### 3. trendyol_products

Trendyol ürün kataloğu.

```sql
CREATE TABLE trendyol_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,

    -- Trendyol identifiers
    trendyol_product_id BIGINT,
    barcode VARCHAR(100) NOT NULL,
    product_code VARCHAR(100),

    -- Product info
    title VARCHAR(500),
    brand VARCHAR(255),
    category_name VARCHAR(500),
    description TEXT,
    images JSONB,                    -- Array of image URLs

    -- Pricing
    sale_price DECIMAL(15,2),
    list_price DECIMAL(15,2),
    vat_rate INTEGER,

    -- Stock
    quantity INTEGER DEFAULT 0,

    -- Commission
    commission_rate DECIMAL(5,2),
    last_commission_rate DECIMAL(5,2),    -- Finansaldan gelen son oran
    last_commission_date TIMESTAMP,        -- Son komisyon güncelleme tarihi

    -- Cost tracking (JSONB for history)
    cost_and_stock_info JSONB,

    -- Status
    on_sale BOOLEAN DEFAULT TRUE,
    archived BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(store_id, barcode)
);

CREATE INDEX idx_products_store ON trendyol_products(store_id);
CREATE INDEX idx_products_barcode ON trendyol_products(barcode);
CREATE INDEX idx_products_store_barcode ON trendyol_products(store_id, barcode);
```

#### cost_and_stock_info JSONB Yapısı

```json
{
  "currentCost": 150.00,
  "currentStock": 25,
  "costHistory": [
    {
      "cost": 145.00,
      "date": "2026-01-15T10:00:00Z",
      "note": "İlk maliyet"
    },
    {
      "cost": 150.00,
      "date": "2026-01-18T14:30:00Z",
      "note": "Tedarikçi fiyat artışı"
    }
  ]
}
```

### 4. trendyol_orders

Trendyol siparişleri.

```sql
CREATE TABLE trendyol_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,

    -- Trendyol identifiers
    order_number VARCHAR(100) NOT NULL,
    line_id VARCHAR(100),
    barcode VARCHAR(100),

    -- Order info
    product_name VARCHAR(500),
    quantity INTEGER DEFAULT 1,

    -- Pricing (TL)
    unit_price_order DECIMAL(15,2),       -- Liste fiyatı
    unit_price_discount DECIMAL(15,2),     -- İndirimli fiyat
    vat_base_amount DECIMAL(15,2),         -- KDV hariç tutar (komisyon hesabı için)
    discount DECIMAL(15,2),

    -- Commission
    estimated_commission DECIMAL(15,2),
    is_commission_estimated BOOLEAN DEFAULT TRUE,

    -- Status
    order_status VARCHAR(100),
    package_status VARCHAR(100),

    -- Dates
    order_date TIMESTAMP,
    shipment_date TIMESTAMP,
    delivery_date TIMESTAMP,

    -- Address (şehir bazlı analitik için)
    city VARCHAR(100),
    district VARCHAR(255),

    -- Order items (ek bilgiler için JSONB)
    order_items JSONB,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(store_id, order_number, line_id)
);

CREATE INDEX idx_orders_store ON trendyol_orders(store_id);
CREATE INDEX idx_orders_date ON trendyol_orders(order_date);
CREATE INDEX idx_orders_status ON trendyol_orders(order_status);
CREATE INDEX idx_orders_barcode ON trendyol_orders(barcode);
CREATE INDEX idx_orders_commission_estimated ON trendyol_orders(is_commission_estimated);
```

### 5. webhook_events

Webhook audit log ve idempotency.

```sql
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) UNIQUE,          -- Idempotency key
    seller_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100),
    payload TEXT,
    received_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'received'  -- received, processed, failed
);

CREATE INDEX idx_webhook_events_seller ON webhook_events(seller_id);
CREATE INDEX idx_webhook_events_event_id ON webhook_events(event_id);
CREATE INDEX idx_webhook_events_received ON webhook_events(received_at);
```

### 6. store_expenses

Mağaza giderleri.

```sql
CREATE TABLE store_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    category_id UUID REFERENCES expense_categories(id),
    description VARCHAR(500),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'TRY',
    expense_date DATE NOT NULL,
    frequency VARCHAR(50),                  -- once, daily, weekly, monthly, yearly
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_expenses_store ON store_expenses(store_id);
CREATE INDEX idx_expenses_date ON store_expenses(expense_date);
```

---

## Migration Listesi

### Komisyon Sistemi Migration'ları

```sql
-- V45__add_commission_tracking_fields.sql

-- Ürünlere son komisyon bilgisi ekle
ALTER TABLE trendyol_products
ADD COLUMN IF NOT EXISTS last_commission_rate DECIMAL(5,2);

ALTER TABLE trendyol_products
ADD COLUMN IF NOT EXISTS last_commission_date TIMESTAMP;

-- Siparişlere komisyon tahmini flag'i ekle
ALTER TABLE trendyol_orders
ADD COLUMN IF NOT EXISTS is_commission_estimated BOOLEAN DEFAULT TRUE;

-- Performans index'leri
CREATE INDEX IF NOT EXISTS idx_orders_commission_estimated
ON trendyol_orders(is_commission_estimated);

CREATE INDEX IF NOT EXISTS idx_products_store_barcode
ON trendyol_products(store_id, barcode);
```

### Alert Status Migration

```sql
-- V88__add_alert_status_field.sql

-- Onay bazlı stok algılama sistemi için status alanı
ALTER TABLE alert_history ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'INFO';

-- Değerler: INFO (default), PENDING_APPROVAL, APPROVED, DISMISSED
-- Mevcut alertler INFO olarak kalır (backward compatible)
-- Stok artışı algılama alertleri PENDING_APPROVAL olarak oluşturulur
```

### Store Sync Tracking Migration'ları

```sql
-- V46__add_store_sync_tracking.sql

-- Webhook tracking
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_status VARCHAR(50) DEFAULT 'pending';
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_error_message TEXT;

-- Sync tracking
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_status VARCHAR(50) DEFAULT 'pending';
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_error_message TEXT;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS initial_sync_completed BOOLEAN DEFAULT FALSE;
```

---

## JSONB Kullanımı

### Neden JSONB?

1. **Esneklik**: Şema değişikliği olmadan yeni alanlar eklenebilir
2. **Tarihçe**: Maliyet/stok değişiklik geçmişi saklanabilir
3. **Index**: GIN index ile hızlı arama

### JSONB Sorgulama Örnekleri

```sql
-- Mevcut maliyeti al
SELECT
    id,
    barcode,
    cost_and_stock_info->>'currentCost' as current_cost
FROM trendyol_products
WHERE store_id = :storeId;

-- Maliyet geçmişini al
SELECT
    barcode,
    jsonb_array_elements(cost_and_stock_info->'costHistory') as history_entry
FROM trendyol_products
WHERE store_id = :storeId;

-- Belirli maliyetten yüksek ürünler
SELECT *
FROM trendyol_products
WHERE (cost_and_stock_info->>'currentCost')::decimal > 100;
```

### JSONB Güncelleme

```sql
-- Mevcut maliyeti güncelle
UPDATE trendyol_products
SET cost_and_stock_info = jsonb_set(
    cost_and_stock_info,
    '{currentCost}',
    '175.00'::jsonb
)
WHERE id = :productId;

-- Maliyet geçmişine yeni kayıt ekle
UPDATE trendyol_products
SET cost_and_stock_info = jsonb_set(
    cost_and_stock_info,
    '{costHistory}',
    cost_and_stock_info->'costHistory' || '[{"cost": 175.00, "date": "2026-01-18T10:00:00Z"}]'::jsonb
)
WHERE id = :productId;
```

---

## Index Stratejisi

### Temel Index'ler

| Tablo | Index | Kolon(lar) | Neden |
|-------|-------|------------|-------|
| users | idx_users_email | email | Login lookup |
| stores | idx_stores_user | user_id | Kullanıcının mağazaları |
| products | idx_products_store_barcode | store_id, barcode | Komisyon hesaplama |
| orders | idx_orders_store | store_id | Mağaza siparişleri |
| orders | idx_orders_date | order_date | Tarih bazlı sorgular |
| orders | idx_orders_commission_estimated | is_commission_estimated | Komisyon raporlama |

### Composite Index'ler

```sql
-- Mağaza + tarih aralığı sorguları için
CREATE INDEX idx_orders_store_date ON trendyol_orders(store_id, order_date);

-- Mağaza + barcode lookup için
CREATE INDEX idx_products_store_barcode ON trendyol_products(store_id, barcode);
```

---

## Veritabanı Bağlantısı

### Docker ile Başlatma

```bash
# db.sh script'i
./db.sh start    # PostgreSQL container başlat
./db.sh connect  # psql shell'e bağlan
./db.sh reset    # Veritabanını sıfırla
```

### docker-compose.db.yml

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    container_name: sellerx-db
    environment:
      POSTGRES_DB: sellerx_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - sellerx_db_data:/var/lib/postgresql/data

volumes:
  sellerx_db_data:
```

### Spring Boot Bağlantısı

```yaml
# application.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sellerx_db
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway migration kullanıldığı için
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration
```

---

## Backup & Recovery

### Manuel Backup

```bash
# Docker container'dan backup
docker exec sellerx-db pg_dump -U postgres sellerx_db > backup.sql

# Restore
docker exec -i sellerx-db psql -U postgres sellerx_db < backup.sql
```

### Otomatik Backup (Cron)

```bash
# /etc/cron.d/sellerx-backup
0 2 * * * docker exec sellerx-db pg_dump -U postgres sellerx_db | gzip > /backups/sellerx_$(date +\%Y\%m\%d).sql.gz
```

---

## Performans İzleme

### Yavaş Sorguları Bul

```sql
-- pg_stat_statements extension gerekli
SELECT
    query,
    calls,
    total_time,
    mean_time,
    rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

### Tablo Boyutları

```sql
SELECT
    relname as table_name,
    pg_size_pretty(pg_total_relation_size(relid)) as total_size
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

### Index Kullanımı

```sql
SELECT
    indexrelname as index_name,
    idx_scan as times_used,
    pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```
