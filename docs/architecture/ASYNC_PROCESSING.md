# Asenkron İşlem Yönetimi

> Spring Boot @Async ile arka plan işlem yönetimi.

## Genel Bakış

SellerX, uzun süren işlemleri arka planda asenkron olarak çalıştırır. Bu sayede kullanıcı arayüzü bloke olmaz ve API'ler hızlı yanıt verir.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       ASYNC MİMARİSİ                                    │
└─────────────────────────────────────────────────────────────────────────┘

    HTTP Request                 Main Thread              Async Thread Pool
         │                           │                           │
         │  POST /stores             │                           │
         │──────────────────────────▶│                           │
         │                           │                           │
         │                    Store kaydet                       │
         │                           │                           │
         │                           │  @Async                   │
         │                           │──────────────────────────▶│
         │                           │                           │ Sync başlat
         │  { id, syncStatus }       │                           │
         │◀──────────────────────────│                           │
         │                           │                           │ Ürünler...
         │  (Response döndü)         │                           │ Siparişler...
         │                           │                           │ Finansal...
         │                           │                           │
         │                           │                           │ ✓ Completed
```

---

## Konfigürasyon

### 1. @EnableAsync Aktivasyonu

```java
// StoreApplication.java
@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync        // ← Async'i aktif et
public class StoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }
}
```

### 2. AsyncConfig - Thread Pool Tanımları

```java
package com.ecommerce.sellerx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Onboarding işlemleri için özel thread pool.
     * Mağaza ekleme sırasında çalışır.
     */
    @Bean(name = "onboardingExecutor")
    public Executor onboardingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Thread havuzu boyutları
        executor.setCorePoolSize(5);      // Minimum thread sayısı
        executor.setMaxPoolSize(10);      // Maximum thread sayısı
        executor.setQueueCapacity(100);   // Kuyruk kapasitesi

        // Thread isimlendirme (debug için)
        executor.setThreadNamePrefix("onboarding-");

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * Genel async işlemler için default pool.
     * Webhook processing, notification vb.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * Default async executor (AsyncConfigurer interface).
     */
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
}
```

---

## Thread Pool Parametreleri

### Parametre Açıklamaları

| Parametre | Değer | Açıklama |
|-----------|-------|----------|
| corePoolSize | 5 | Her zaman çalışan minimum thread |
| maxPoolSize | 10 | Yoğunlukta artırılabilecek max thread |
| queueCapacity | 100 | Tüm thread'ler doluysa bekleyen işler |
| threadNamePrefix | "onboarding-" | Log'larda görünecek isim |

### Çalışma Mantığı

```
1. İş geldi → Core pool'da boş thread var mı?
   ├── EVET → Thread'e ata, çalıştır
   └── HAYIR → Kuyruğa ekle (queue)

2. Kuyruk doldu → Max pool'a kadar yeni thread aç
   ├── maxPoolSize'a ulaşıldı mı?
   │   ├── HAYIR → Yeni thread aç
   │   └── EVET → RejectedExecutionException (veya policy)
```

### Örnek Senaryo

```
onboardingExecutor: core=5, max=10, queue=100

Durum 1: 3 mağaza onboarding yapıyor
→ 3 thread çalışıyor, 2 thread idle

Durum 2: 8 mağaza onboarding yapıyor
→ 5 thread çalışıyor (core), 3 kuyrukta

Durum 3: 15 mağaza onboarding yapıyor
→ 10 thread çalışıyor (max'a ulaştı), 5 kuyrukta

Durum 4: 120 mağaza onboarding yapıyor
→ 10 thread çalışıyor, 100 kuyrukta, 10 REJECT
```

---

## @Async Kullanımı

### Temel Kullanım

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreOnboardingService {

    /**
     * Belirli executor kullanarak async çalıştır.
     */
    @Async("onboardingExecutor")
    public void performInitialSync(Store store) {
        log.info("Starting initial sync for store: {} on thread: {}",
                 store.getId(),
                 Thread.currentThread().getName());

        try {
            // Uzun süren işlemler...
            syncProducts(store);
            syncOrders(store);
            syncFinancial(store);

            log.info("Completed sync for store: {}", store.getId());
        } catch (Exception e) {
            log.error("Sync failed for store: {}", store.getId(), e);
        }
    }
}
```

### Önemli Kurallar

1. **@Async metodu public olmalı**
2. **Self-invocation çalışmaz** (aynı class içinden çağırma)
3. **Return type**: `void`, `Future<T>`, `CompletableFuture<T>`

### Self-Invocation Problemi

```java
// ❌ YANLIŞ - Self-invocation, @Async çalışmaz
@Service
public class MyService {

    public void doSomething() {
        doAsyncWork();  // Bu SYNC çalışır!
    }

    @Async
    public void doAsyncWork() {
        // ...
    }
}

// ✅ DOĞRU - Farklı service'den çağır
@Service
public class CallerService {

    @Autowired
    private AsyncService asyncService;

    public void doSomething() {
        asyncService.doAsyncWork();  // Bu ASYNC çalışır
    }
}

@Service
public class AsyncService {

    @Async
    public void doAsyncWork() {
        // ...
    }
}
```

---

## CompletableFuture ile Sonuç Alma

### Future Döndürme

```java
@Service
public class DataProcessingService {

    @Async("taskExecutor")
    public CompletableFuture<ProcessingResult> processDataAsync(Data data) {
        try {
            ProcessingResult result = processData(data);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            CompletableFuture<ProcessingResult> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
```

### Sonucu Bekleme

```java
@RestController
public class DataController {

    @Autowired
    private DataProcessingService processingService;

    @PostMapping("/process")
    public ResponseEntity<?> processData(@RequestBody Data data) {
        // Async başlat
        CompletableFuture<ProcessingResult> future =
            processingService.processDataAsync(data);

        // Sonucu bekle (timeout ile)
        try {
            ProcessingResult result = future.get(30, TimeUnit.SECONDS);
            return ResponseEntity.ok(result);
        } catch (TimeoutException e) {
            return ResponseEntity.status(408).body("İşlem zaman aşımına uğradı");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("İşlem başarısız");
        }
    }
}
```

### Birden Fazla Async İşlemi Birleştirme

```java
public void processMultipleStores(List<Store> stores) {
    List<CompletableFuture<Void>> futures = stores.stream()
        .map(store -> CompletableFuture.runAsync(
            () -> onboardingService.performInitialSync(store),
            onboardingExecutor
        ))
        .collect(Collectors.toList());

    // Tümünün tamamlanmasını bekle
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> log.info("All stores synced"))
        .exceptionally(ex -> {
            log.error("Some stores failed", ex);
            return null;
        });
}
```

---

## Webhook Processing

### Async Webhook Handler

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolWebhookService {

    /**
     * Webhook'u async olarak işle.
     * Controller hemen 200 döner, işlem arka planda devam eder.
     */
    @Async("taskExecutor")
    public void processWebhookAsync(String sellerId, String payload, String eventId) {
        log.info("Processing webhook async: sellerId={}, eventId={}, thread={}",
                 sellerId, eventId, Thread.currentThread().getName());

        try {
            // İşleme mantığı...
            processPayload(sellerId, payload);

            log.info("Webhook processed successfully: eventId={}", eventId);
        } catch (Exception e) {
            log.error("Webhook processing failed: eventId={}", eventId, e);
        }
    }
}
```

### Controller'da Kullanım

```java
@RestController
public class WebhookController {

    @Autowired
    private TrendyolWebhookService webhookService;

    @PostMapping("/webhook/{sellerId}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable String sellerId,
            @RequestBody String payload) {

        // Hemen async'e gönder
        webhookService.processWebhookAsync(sellerId, payload, eventId);

        // Hemen 200 döndür (Trendyol 5 saniye bekler)
        return ResponseEntity.ok("OK");
    }
}
```

---

## Hata Yönetimi

### Async Exception Handler

```java
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }
}

public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAsyncExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Async method {} threw exception: {}",
                  method.getName(),
                  ex.getMessage(),
                  ex);

        // Opsiyonel: Alert gönder, metric kaydet vb.
    }
}
```

### Try-Catch ile Hata Yakalama

```java
@Async("onboardingExecutor")
public void performInitialSync(Store store) {
    try {
        // İşlemler...
        syncProducts(store);
        syncOrders(store);
        markSyncCompleted(store.getId());

    } catch (TrendyolApiException e) {
        log.error("Trendyol API error for store {}: {}", store.getId(), e.getMessage());
        updateSyncStatus(store.getId(), "FAILED", "Trendyol API hatası: " + e.getMessage());

    } catch (Exception e) {
        log.error("Unexpected error for store {}", store.getId(), e);
        updateSyncStatus(store.getId(), "FAILED", "Beklenmeyen hata: " + e.getMessage());
    }
}
```

---

## Scheduled Jobs ile Entegrasyon

### Async + Scheduled

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledSyncService {

    private final StoreRepository storeRepository;
    private final StoreOnboardingService onboardingService;

    /**
     * Her gün 06:00'da çalışır.
     * Her mağaza için async sync başlatır.
     */
    @Scheduled(cron = "0 0 6 * * ?", zone = "Europe/Istanbul")
    public void dailySync() {
        List<Store> stores = storeRepository.findByInitialSyncCompleted(true);

        log.info("Starting daily sync for {} stores", stores.size());

        for (Store store : stores) {
            // Her mağaza için async işlem başlat
            onboardingService.refreshStoreDataAsync(store);
        }

        log.info("Daily sync jobs submitted");
    }
}
```

---

## Monitoring & Logging

### Thread İsimlendirme

```
# Log çıktısı örneği
2026-01-18 10:30:15 [onboarding-1] INFO  StoreOnboardingService - Starting sync for store: abc123
2026-01-18 10:30:16 [onboarding-2] INFO  StoreOnboardingService - Starting sync for store: def456
2026-01-18 10:30:45 [onboarding-1] INFO  StoreOnboardingService - Completed sync for store: abc123
2026-01-18 10:31:00 [async-1] INFO  WebhookService - Processing webhook: evt_789
```

### Thread Pool Metrics

```java
@Component
@RequiredArgsConstructor
public class ThreadPoolMetrics {

    @Qualifier("onboardingExecutor")
    private final ThreadPoolTaskExecutor onboardingExecutor;

    @Scheduled(fixedRate = 60000)  // Her dakika
    public void logPoolStatus() {
        log.info("Onboarding pool - Active: {}, Pool Size: {}, Queue Size: {}",
                 onboardingExecutor.getActiveCount(),
                 onboardingExecutor.getPoolSize(),
                 onboardingExecutor.getThreadPoolExecutor().getQueue().size());
    }
}
```

---

## Best Practices

### 1. İşlem Süresi Tahmini

```java
// Kısa işlemler (< 1 saniye) → taskExecutor
@Async("taskExecutor")
public void sendNotification(Notification notification) { ... }

// Uzun işlemler (> 10 saniye) → onboardingExecutor
@Async("onboardingExecutor")
public void performInitialSync(Store store) { ... }
```

### 2. Timeout Kullanımı

```java
CompletableFuture<Result> future = asyncService.processAsync(data);

try {
    Result result = future.get(30, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    log.warn("Async operation timed out");
    // Fallback veya hata yönetimi
}
```

### 3. Graceful Shutdown

```java
executor.setWaitForTasksToCompleteOnShutdown(true);
executor.setAwaitTerminationSeconds(60);
```

### 4. İş Önceliklendirme

```java
// Farklı işler için farklı executor'lar
@Async("highPriorityExecutor")   // Core: 10, Max: 20
public void processUrgentTask() { ... }

@Async("lowPriorityExecutor")    // Core: 2, Max: 5
public void processBackgroundTask() { ... }
```

---

## Sorun Giderme

### @Async Çalışmıyor

1. **@EnableAsync var mı?** → StoreApplication.java'da kontrol et
2. **Method public mı?** → Private metodlar çalışmaz
3. **Self-invocation mı?** → Aynı class'tan çağırma

### Thread Pool Tükeniyor

1. **Pool boyutunu artır**: maxPoolSize değerini yükselt
2. **Queue kapasitesini artır**: queueCapacity değerini yükselt
3. **İşlem süresini azalt**: Rate limiting ekle

### Memory Leak

1. **Request scope bean'ler**: Async'te kullanma
2. **Large object'ler**: İşlem bitince null'a ata
3. **Thread local'lar**: Temizle
