# SellerX Optimizasyon Durumu

**Son Güncelleme**: 10 Şubat 2026
**Hazırlayan**: Claude Code
**Durum**: Faz 0-2 TAMAMLANDI, Azure migration bekliyor

---

## Durum Özeti

| Faz | Durum | Mağaza Kapasitesi |
|-----|-------|-------------------|
| ✅ Faz 0 | Tamamlandı | 20 mağaza (launch) |
| ✅ Faz 1 | Tamamlandı | 50 mağaza |
| ✅ Faz 2 | Tamamlandı | 100-300 mağaza |
| ⏳ Faz 3 | Azure'da yapılacak | 300+ mağaza |
| ⏳ Faz 4 | Azure'da yapılacak | 500+ mağaza |
| ⏳ Faz 5 | Azure'da yapılacak | 1500+ mağaza |

---

## ✅ TAMAMLANAN İŞLER

### Faz 0: Acil Düzeltmeler

#### 1. Thread Pool Artırımı
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/config/AsyncConfig.java`

```java
// onboardingExecutor
executor.setCorePoolSize(20);   // 5 → 20
executor.setMaxPoolSize(50);    // 10 → 50
executor.setQueueCapacity(500); // 100 → 500

// taskExecutor
executor.setCorePoolSize(10);
executor.setMaxPoolSize(30);
executor.setQueueCapacity(200);
```

#### 2. HikariCP Connection Pool
**Dosya**: `sellerx-backend/src/main/resources/application.yaml`

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 40
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

#### 3. Frontend Cache Key Düzeltmesi
**Dosya**: `sellerx-frontend/hooks/queries/use-orders.ts`

```typescript
export const orderKeys = {
  all: ["orders"] as const,
  byStore: (storeId: string) => [...orderKeys.all, "store", storeId] as const,
  byStorePaginated: (storeId: string, page: number, size: number) =>
    [...orderKeys.all, "store", storeId, "paginated", { page, size }] as const,
  byDateRange: (storeId: string, startDate: string, endDate: string) =>
    [...orderKeys.all, "dateRange", storeId, startDate, endDate] as const,
  byDateRangePaginated: (storeId: string, startDate: string, endDate: string, page: number, size: number) =>
    [...orderKeys.all, "dateRange", storeId, startDate, endDate, "page", page, "size", size] as const,
  byStatus: (storeId: string, status: string) =>
    [...orderKeys.all, "byStatus", storeId, status] as const,
  byStatusPaginated: (storeId: string, status: string, page: number, size: number) =>
    [...orderKeys.all, "byStatus", storeId, status, "page", page, "size", size] as const,
  statistics: (storeId: string) =>
    [...orderKeys.all, "statistics", storeId] as const,
};
```

---

### Faz 1: Veritabanı İndeksleri

**Dosya**: `sellerx-backend/src/main/resources/db/migration/V100__expression_indexes_for_scaling.sql`

```sql
-- JSONB Expression Indexes
CREATE INDEX IF NOT EXISTS idx_orders_order_number_expr
    ON trendyol_orders USING btree ((order_items->0->>'orderNumber'));

CREATE INDEX IF NOT EXISTS idx_orders_line_item_id_expr
    ON trendyol_orders USING btree ((order_items->0->>'lineItemId'));

CREATE INDEX IF NOT EXISTS idx_orders_barcode_jsonb
    ON trendyol_orders USING gin ((order_items) jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_products_last_cost_expr
    ON trendyol_products USING btree ((cost_and_stock_info->-1->>'unitCost'));

-- Composite Indexes
CREATE INDEX IF NOT EXISTS idx_orders_store_date_status_composite
    ON trendyol_orders(store_id, order_date DESC, status);

CREATE INDEX IF NOT EXISTS idx_orders_store_city_date
    ON trendyol_orders(store_id, shipment_city, order_date DESC);

CREATE INDEX IF NOT EXISTS idx_orders_customer_lifecycle
    ON trendyol_orders(store_id, customer_id, order_date DESC);

CREATE INDEX IF NOT EXISTS idx_invoices_category_date
    ON trendyol_invoices(store_id, invoice_category, invoice_date DESC);

CREATE INDEX IF NOT EXISTS idx_invoices_number
    ON trendyol_invoices(store_id, invoice_number);
```

---

### Faz 2: Paralel Sync & Error Isolation

#### 1. ParallelStoreSyncService
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/orders/ParallelStoreSyncService.java`

**Özellikler:**
- FixedThreadPool (15 thread) - kontrollü parallelism
- 50'lik batch'ler halinde işleme
- Per-store timeout (2 dakika)
- Micrometer metrikleri

**Konfigürasyon** (`application.yaml`):
```yaml
sellerx:
  sync:
    parallel:
      enabled: true
      threads: 15
    batch:
      size: 50
    store:
      timeout:
        seconds: 120
```

#### 2. ResilientSyncService
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/orders/ResilientSyncService.java`

**4-Seviye Koruma:**
| Level | Ne Yapar? | Koruma |
|-------|-----------|--------|
| Level 1: Try-Catch | Her store hatası izole | Per-store izolasyon |
| Level 2: Timeout | 3 dakika timeout | Sonsuz bekleme yok |
| Level 3: Bulkhead | Max 1 concurrent per store | Kaynak izolasyonu |
| Level 4: Circuit Breaker | 50 ardışık hata → OPEN | Cascading failure koruması |

**Circuit Breaker Davranışı:**
- 50 ardışık hata → Circuit OPEN (tüm istekleri reddet)
- 1 dakika sonra → HALF-OPEN (5 test isteği)
- Test başarılı → CLOSED (normal çalışma)

#### 3. TrendyolOrderScheduledService Entegrasyonu
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/orders/TrendyolOrderScheduledService.java`

```java
@Value("${sellerx.sync.parallel.enabled:true}")
private boolean parallelSyncEnabled;

// Daily sync at 6:15 AM
if (parallelSyncEnabled) {
    resilientSyncService.syncAllWithResilience(false);
} else {
    syncStoresWithRateLimit(false); // Legacy mode
}
```

---

## Yeni Metrikler (Actuator)

```bash
# Paralel sync metrikleri
curl localhost:8080/actuator/metrics/sellerx.parallel.sync.duration
curl localhost:8080/actuator/metrics/sellerx.parallel.sync.active

# Circuit breaker durumu
curl localhost:8080/actuator/metrics/sellerx.circuit.state

# Timeout ve bulkhead
curl localhost:8080/actuator/metrics/sellerx.resilient.timeout
curl localhost:8080/actuator/metrics/sellerx.resilient.bulkhead.rejected
```

---

## ⏳ AZURE'DA YAPILACAK İŞLER

### Faz 3: Redis Cache (100+ mağaza)

**Ne:** Azure Cache for Redis
**Maliyet:** ~$50-100/ay

**Yapılacaklar:**
1. Azure Cache for Redis oluştur
2. `RedisConfig.java` ekle
3. Dashboard stats caching (@Cacheable)
4. Frontend batch endpoint

```java
@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5));
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

---

### Faz 4: Message Queue (300+ mağaza)

**Ne:** Azure Service Bus veya RabbitMQ
**Maliyet:** ~$20-50/ay

**Yapılacaklar:**
1. Queue sistemi kur
2. `SyncQueueProducer.java` - Scheduler sadece queue'ya mesaj atar
3. `SyncQueueConsumer.java` - Worker'lar mesajları işler
4. Dead Letter Queue + Retry pattern

```
Scheduler ──▶ Queue ──▶ Worker Pool (10 worker)
                 │
                 ▼ (5x retry sonrası)
            Dead Letter Queue
```

---

### Faz 5: Database Scaling (500+ mağaza)

**Ne:** Azure Database for PostgreSQL + Read Replicas
**Maliyet:** ~$100-200/ay

**Yapılacaklar:**
1. Read replica oluştur
2. Query routing (write → primary, read → replica)
3. Hash partitioning (store_id bazlı 32 partition)

```sql
CREATE TABLE trendyol_orders_partitioned (
    LIKE trendyol_orders INCLUDING ALL
) PARTITION BY HASH (store_id);

-- 32 partition oluştur
CREATE TABLE trendyol_orders_p0 PARTITION OF trendyol_orders_partitioned
    FOR VALUES WITH (MODULUS 32, REMAINDER 0);
-- ... p1-p31
```

---

## Doğrulama Komutları

### Index Kullanımı Kontrolü
```sql
EXPLAIN ANALYZE
SELECT * FROM trendyol_orders
WHERE order_items->0->>'orderNumber' = '123456789';
-- Beklenen: "Index Scan using idx_orders_order_number_expr"
```

### Paralel Sync Testi
```bash
# Manuel sync tetikle
curl -X POST localhost:8080/api/admin/sync/all

# Metrikleri kontrol et
curl localhost:8080/actuator/metrics/sellerx.parallel.sync.duration
```

### Circuit Breaker Durumu
```bash
# Circuit state'leri gör (open/closed/half_open)
curl localhost:8080/actuator/metrics/sellerx.circuit.state?tag=state:open
```

---

## Özet

**Tamamlanan:**
- ✅ Thread pool scaling (5→20, 10→50)
- ✅ HikariCP connection pool (40 max)
- ✅ Frontend cache key fix
- ✅ 10 database index
- ✅ Parallel sync service (15 thread, 50 batch)
- ✅ Circuit breaker (50 failure threshold)
- ✅ Error isolation (4-level protection)

**Azure'da Yapılacak:**
- ⏳ Redis Cache
- ⏳ Message Queue
- ⏳ Read Replicas
- ⏳ Database Partitioning

**Mevcut Kapasite:** 100-300 mağaza (Faz 0-2 ile)

---

*Bu dosyayı Azure migration'da Claude'a ver - kaldığın yerden devam eder.*
