# Rate Limiting & API Yönetimi

> Trendyol API limitlerine uyum için rate limiting altyapısı.

## Genel Bakış

Trendyol API'si belirli rate limitlere sahiptir. Bu limitleri aşmamak için `TrendyolRateLimiter` komponenti kullanılır.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         RATE LİMİTİNG MİMARİSİ                          │
└─────────────────────────────────────────────────────────────────────────┘

     Service Layer                   RateLimiter                 Trendyol API
          │                              │                            │
          │  acquire()                   │                            │
          │─────────────────────────────▶│                            │
          │                              │                            │
          │  (bekle - rate limit)        │                            │
          │◀ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│                            │
          │                              │                            │
          │  permit alındı               │                            │
          │◀─────────────────────────────│                            │
          │                              │                            │
          │                          API Call                         │
          │──────────────────────────────────────────────────────────▶│
          │                              │                            │
          │                          Response                         │
          │◀──────────────────────────────────────────────────────────│
          │                              │                            │
```

---

## Trendyol API Limitleri

| API | Rate Limit | Önerilen |
|-----|------------|----------|
| Products | 10 req/sec | 10 req/sec |
| Orders | 10 req/sec | 10 req/sec |
| Financial | 10 req/sec | 10 req/sec |
| Webhooks | Unlimited | - |

**Not**: Limitleri aşmak `429 Too Many Requests` hatası döndürür.

---

## TrendyolRateLimiter Implementasyonu

### Guava RateLimiter Kullanımı

```java
package com.ecommerce.sellerx.common;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Trendyol API çağrıları için rate limiter.
 * Saniyede maksimum 10 istek yapılmasını garantiler.
 */
@Component
@Slf4j
public class TrendyolRateLimiter {

    // Saniyede 10 istek (10 permits per second)
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    /**
     * Bir permit alana kadar bekler (blocking).
     * Bu metod thread-safe'tir.
     *
     * @return Bekleme süresi (saniye)
     */
    public double acquire() {
        double waitTime = rateLimiter.acquire();
        if (waitTime > 0) {
            log.debug("Rate limiter waited {} seconds", waitTime);
        }
        return waitTime;
    }

    /**
     * Permit almayı dener, beklemez (non-blocking).
     *
     * @return Permit alınabildiyse true
     */
    public boolean tryAcquire() {
        return rateLimiter.tryAcquire();
    }

    /**
     * Belirli süre içinde permit almayı dener.
     *
     * @param timeoutMs Maksimum bekleme süresi (milisaniye)
     * @return Permit alınabildiyse true
     */
    public boolean tryAcquire(long timeoutMs) {
        return rateLimiter.tryAcquire(java.time.Duration.ofMillis(timeoutMs));
    }
}
```

### Maven Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.0.0-jre</version>
</dependency>
```

---

## Kullanım Örnekleri

### 1. Scheduled Job'larda Kullanım

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolOrderScheduledService {

    private final TrendyolOrderService orderService;
    private final StoreRepository storeRepository;
    private final TrendyolRateLimiter rateLimiter;

    @Scheduled(cron = "0 15 6 * * ?", zone = "Europe/Istanbul")
    public void syncOrdersForAllTrendyolStores() {
        List<Store> trendyolStores = storeRepository.findByMarketplace("trendyol");

        for (Store store : trendyolStores) {
            // Initial sync tamamlanmamış mağazaları atla
            if (!Boolean.TRUE.equals(store.getInitialSyncCompleted())) {
                log.info("Skipping store {} - initial sync not completed", store.getId());
                continue;
            }

            // Rate limit uygula - her API çağrısından önce
            rateLimiter.acquire();

            try {
                orderService.fetchAndSaveOrdersForStore(store.getId());
            } catch (Exception e) {
                log.error("Failed to sync orders for store: {}", store.getId(), e);
            }
        }
    }
}
```

### 2. Onboarding Service'de Kullanım

```java
@Service
@RequiredArgsConstructor
public class StoreOnboardingService {

    private final TrendyolRateLimiter rateLimiter;

    @Async("onboardingExecutor")
    public void performInitialSync(Store store) {
        try {
            // Ürünler
            updateSyncStatus(store.getId(), "SYNCING_PRODUCTS", null);
            rateLimiter.acquire();  // ← Rate limit
            productService.fetchAndSaveProductsForStore(store.getId());

            // Siparişler
            updateSyncStatus(store.getId(), "SYNCING_ORDERS", null);
            rateLimiter.acquire();  // ← Rate limit
            orderService.fetchAndSaveOrdersForStore(store.getId());

            // Finansal
            updateSyncStatus(store.getId(), "SYNCING_FINANCIAL", null);
            rateLimiter.acquire();  // ← Rate limit
            financialService.fetchAndSaveSettlementsForStore(store.getId());

            markSyncCompleted(store.getId());
        } catch (Exception e) {
            // Hata yönetimi
        }
    }
}
```

### 3. Financial Service'de Kullanım

```java
@Service
@RequiredArgsConstructor
public class TrendyolFinancialSettlementService {

    private final TrendyolRateLimiter rateLimiter;

    public void fetchAndUpdateSettlementsForAllStores() {
        List<Store> stores = storeRepository.findByMarketplace("trendyol");

        for (Store store : stores) {
            // Initial sync kontrolü
            if (!Boolean.TRUE.equals(store.getInitialSyncCompleted())) {
                continue;
            }

            // Rate limit uygula
            rateLimiter.acquire();

            try {
                fetchAndSaveSettlementsForStore(store.getId());
            } catch (Exception e) {
                log.error("Failed to fetch settlements for store: {}", store.getId(), e);
            }
        }
    }
}
```

---

## Rate Limiter Davranışı

### Blocking Mode (acquire)

```
Thread 1: acquire() → hemen geçer (permit var)
Thread 2: acquire() → 100ms bekler
Thread 3: acquire() → 200ms bekler
...
Thread 11: acquire() → 1000ms bekler (1 saniye doldu, yeni cycle)
```

### Non-Blocking Mode (tryAcquire)

```java
if (rateLimiter.tryAcquire()) {
    // Permit alındı, API çağrısı yap
    callTrendyolApi();
} else {
    // Permit yok, daha sonra dene veya hata döndür
    throw new RateLimitExceededException("Too many requests");
}
```

### Timeout Mode

```java
if (rateLimiter.tryAcquire(5000)) {  // 5 saniye bekle
    callTrendyolApi();
} else {
    log.warn("Could not acquire permit within 5 seconds");
    throw new TimeoutException();
}
```

---

## Çoklu Mağaza Senaryosu

### Problem

~2000 mağaza için batch job çalıştığında, tüm istekler aynı anda gitmemeli.

### Çözüm

```java
public void syncAllStores() {
    List<Store> stores = storeRepository.findByMarketplace("trendyol");

    log.info("Starting sync for {} stores", stores.size());

    int successCount = 0;
    int failCount = 0;
    int skippedCount = 0;

    for (Store store : stores) {
        // Initial sync kontrolü
        if (!Boolean.TRUE.equals(store.getInitialSyncCompleted())) {
            skippedCount++;
            continue;
        }

        // Rate limit - otomatik olarak saniyede 10 işlem
        rateLimiter.acquire();

        try {
            syncStore(store);
            successCount++;
        } catch (Exception e) {
            log.error("Sync failed for store: {}", store.getId(), e);
            failCount++;
        }
    }

    log.info("Sync completed. Success: {}, Failed: {}, Skipped: {}",
             successCount, failCount, skippedCount);
}
```

### Performans Hesabı

```
2000 mağaza × 1 API çağrısı = 2000 istek
10 istek/saniye = 200 saniye = ~3.3 dakika

Her mağaza için 3 API çağrısı (products, orders, financial):
2000 × 3 = 6000 istek
6000 / 10 = 600 saniye = 10 dakika
```

---

## Catch-Up Sync (Saatlik)

Webhook'ların kaçırdığı siparişleri yakalamak için saatlik sync:

```java
@Scheduled(cron = "0 0 * * * ?", zone = "Europe/Istanbul")  // Her saat başı
public void catchUpSync() {
    List<Store> stores = storeRepository.findByMarketplace("trendyol");

    LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
    LocalDateTime now = LocalDateTime.now();

    for (Store store : stores) {
        if (!Boolean.TRUE.equals(store.getInitialSyncCompleted())) {
            continue;
        }

        rateLimiter.acquire();

        try {
            // Son 2 saati kontrol et
            orderService.fetchAndSaveOrdersForStoreInRange(
                store.getId(),
                twoHoursAgo,
                now
            );
        } catch (Exception e) {
            log.error("Catch-up sync failed for store: {}", store.getId(), e);
        }
    }
}
```

---

## Monitoring & Logging

### Debug Logging

```java
public double acquire() {
    double waitTime = rateLimiter.acquire();
    if (waitTime > 0) {
        log.debug("Rate limiter waited {} seconds", waitTime);
    }
    return waitTime;
}
```

### Metrics (Opsiyonel)

```java
@Component
public class TrendyolRateLimiter {

    private final AtomicLong totalAcquires = new AtomicLong(0);
    private final AtomicLong totalWaitTime = new AtomicLong(0);

    public double acquire() {
        double waitTime = rateLimiter.acquire();
        totalAcquires.incrementAndGet();
        totalWaitTime.addAndGet((long)(waitTime * 1000));
        return waitTime;
    }

    public RateLimiterStats getStats() {
        return new RateLimiterStats(
            totalAcquires.get(),
            totalWaitTime.get()
        );
    }
}
```

---

## Hata Yönetimi

### 429 Too Many Requests

```java
public void callTrendyolApiWithRetry(Supplier<Response> apiCall) {
    int maxRetries = 3;
    int retryCount = 0;

    while (retryCount < maxRetries) {
        rateLimiter.acquire();

        try {
            Response response = apiCall.get();

            if (response.getStatusCode() == 429) {
                // Rate limit aşıldı, daha uzun bekle
                retryCount++;
                long backoffMs = (long) Math.pow(2, retryCount) * 1000;
                log.warn("Rate limit exceeded, backing off for {} ms", backoffMs);
                Thread.sleep(backoffMs);
                continue;
            }

            return;  // Başarılı
        } catch (Exception e) {
            retryCount++;
            if (retryCount >= maxRetries) {
                throw e;
            }
        }
    }
}
```

### Exponential Backoff

```
Retry 1: 2^1 × 1000 = 2000ms (2 saniye)
Retry 2: 2^2 × 1000 = 4000ms (4 saniye)
Retry 3: 2^3 × 1000 = 8000ms (8 saniye)
```

---

## En İyi Pratikler

### 1. Singleton Kullanımı
`@Component` ile Spring IoC'den inject edilir, singleton olarak çalışır.

### 2. Thread Safety
Guava RateLimiter thread-safe'tir. Çoklu thread'lerden güvenle çağrılabilir.

### 3. Graceful Degradation
Rate limit aşılırsa, hata fırlatmak yerine beklemek tercih edilir.

### 4. Initial Sync Kontrolü
Rate limit kaynaklarını korumak için sadece `initialSyncCompleted = true` olan mağazalar batch job'lara dahil edilir.

### 5. Logging
Production'da `DEBUG` yerine `INFO` seviyesinde log kullanılmalı.

---

## Konfigürasyon Seçenekleri

### Rate Değiştirme (Opsiyonel)

```java
@Component
public class TrendyolRateLimiter {

    @Value("${trendyol.api.rate-limit:10.0}")
    private double permitsPerSecond;

    @PostConstruct
    public void init() {
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }
}
```

### application.yaml

```yaml
trendyol:
  api:
    rate-limit: 10.0  # Saniyede 10 istek
```

---

## Test

### Unit Test

```java
@Test
void shouldRateLimitRequests() {
    TrendyolRateLimiter limiter = new TrendyolRateLimiter();

    long startTime = System.currentTimeMillis();

    // 15 istek yap (10 req/sec = 1.5 saniye sürmeli)
    for (int i = 0; i < 15; i++) {
        limiter.acquire();
    }

    long elapsedTime = System.currentTimeMillis() - startTime;

    // En az 500ms sürmeli (ilk 10 hemen, sonraki 5 için 0.5 saniye)
    assertTrue(elapsedTime >= 500);
}
```
