# Faz 2: Resilience4j Entegrasyonu

## Tamamlanma Tarihi
2026-02-12

## Genel Bakış

Resilience4j Spring Boot 3 entegrasyonu ile 4 ana resilience pattern implementasyonu yapıldı:
- **Circuit Breaker**: Hatalı servisleri izole eder
- **Retry**: Exponential backoff ile otomatik yeniden deneme
- **Rate Limiter**: Non-blocking API rate limiting
- **Bulkhead**: Concurrent call sınırlaması

## Eklenen Bağımlılıklar

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

## Konfigürasyon (application.yaml)

### Circuit Breaker Ayarları
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50        # %50 hata → OPEN
        waitDurationInOpenState: 30s    # 30 saniye bekle
        permittedNumberOfCallsInHalfOpenState: 3
    instances:
      trendyolApi:
        baseConfig: default
      trendyolSync:
        slidingWindowSize: 20
        failureRateThreshold: 40
        waitDurationInOpenState: 60s    # Sync için daha uzun
```

### Retry Ayarları
```yaml
  retry:
    instances:
      trendyolApi:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.HttpServerErrorException
```

### Rate Limiter Ayarları (Non-Blocking)
```yaml
  ratelimiter:
    instances:
      trendyolApi:
        limitForPeriod: 10              # Saniyede 10 istek
        limitRefreshPeriod: 1s
        timeoutDuration: 0ms            # Non-blocking - hemen fail
```

### Bulkhead Ayarları
```yaml
  bulkhead:
    instances:
      trendyolSync:
        maxConcurrentCalls: 50          # Max 50 concurrent sync
        maxWaitDuration: 0ms            # Non-blocking
      trendyolApi:
        maxConcurrentCalls: 100         # Max 100 concurrent API call
```

### Time Limiter Ayarları
```yaml
  timelimiter:
    instances:
      trendyolApi:
        timeoutDuration: 30s
      trendyolSync:
        timeoutDuration: 180s           # 3 dakika sync için
```

## Oluşturulan Dosyalar

### 1. Resilience4jConfig.java
**Konum**: `src/main/java/com/ecommerce/sellerx/config/Resilience4jConfig.java`

Circuit breaker, rate limiter, retry ve bulkhead bean tanımları:
- Event publisher'lar ile state transition logging
- Micrometer metrics entegrasyonu
- Custom bean'ler: `trendyolApiCircuitBreaker`, `trendyolSyncCircuitBreaker`, vb.

### 2. ResilienceMetricsService.java
**Konum**: `src/main/java/com/ecommerce/sellerx/config/ResilienceMetricsService.java`

Admin monitoring için servis:
- `getCircuitBreakerStatus()`: Tüm circuit breaker'ların durumu
- `getRateLimiterStatus()`: Rate limiter istatistikleri
- `getBulkheadStatus()`: Bulkhead kullanım durumu
- `getFullStatus()`: Birleşik durum raporu
- `resetCircuitBreaker()`: Manual circuit breaker reset
- `closeCircuitBreaker()`: Zorla CLOSED state'e geçiş

### 3. ResilientApiClient.java
**Konum**: `src/main/java/com/ecommerce/sellerx/resilience/ResilientApiClient.java`

Annotation-based resilience wrapper:
```java
@CircuitBreaker(name = "trendyolApi", fallbackMethod = "apiCallFallback")
@Retry(name = "trendyolApi")
@RateLimiter(name = "trendyolApi")
@Bulkhead(name = "trendyolApi")
@TimeLimiter(name = "trendyolApi")
public <T> T executeApiCall(UUID storeId, Supplier<T> apiCall)
```

Metodlar:
- `executeApiCall()`: API çağrıları için (5 pattern)
- `executeSyncOperation()`: Sync operasyonları için (4 pattern)
- `executeAsyncApiCall()`: Async operasyonlar için CompletableFuture
- `get()`, `post()`, `put()`: Convenience metodlar

### 4. ResilientApiException.java
**Konum**: `src/main/java/com/ecommerce/sellerx/resilience/ResilientApiException.java`

Resilience hataları için custom exception:
```java
public enum FailureType {
    CIRCUIT_OPEN,       // Circuit açık, istek reddedildi
    RATE_LIMIT_EXCEEDED,// Rate limit aşıldı
    BULKHEAD_FULL,      // Concurrent limit doldu
    TIMEOUT,            // Timeout aşıldı
    RETRY_EXHAUSTED,    // Tüm retry'lar tükendi
    AUTH_ERROR,         // 401/403 hatası (retry yapılmaz)
    ERROR               // Genel hata
}
```

Helper metodlar:
- `isRetryable()`: Hata retry edilebilir mi?
- `getSuggestedWaitMs()`: Tavsiye edilen bekleme süresi

### 5. AdminResilienceController.java
**Konum**: `src/main/java/com/ecommerce/sellerx/admin/AdminResilienceController.java`

Admin endpoints:
| Endpoint | Method | Açıklama |
|----------|--------|----------|
| `/api/admin/resilience/status` | GET | Tüm resilience durumu |
| `/api/admin/resilience/circuit-breakers` | GET | Circuit breaker durumları |
| `/api/admin/resilience/rate-limiters` | GET | Rate limiter durumları |
| `/api/admin/resilience/bulkheads` | GET | Bulkhead durumları |
| `/api/admin/resilience/circuit-breakers/{name}/reset` | POST | Circuit reset |
| `/api/admin/resilience/circuit-breakers/{name}/close` | POST | Force close |

### 6. ResilienceHealthIndicator.java
**Konum**: `src/main/java/com/ecommerce/sellerx/config/ResilienceHealthIndicator.java`

Spring Actuator health endpoint entegrasyonu:
- Circuit breaker'lar CLOSED → UP
- Circuit breaker'lar OPEN → DOWN (details ile)

## Test Dosyaları

| Test Dosyası | Test Sayısı | Kapsam |
|--------------|-------------|--------|
| `Resilience4jConfigTest.java` | 7 | Config bean oluşturma |
| `ResilienceMetricsServiceTest.java` | 10 | Metrics service metodları |
| `ResilientApiClientTest.java` | 8 | API client metodları |
| `ResilientApiExceptionTest.java` | 17 | Exception behavior |
| **TOPLAM** | **42** | |

## Mevcut Sistemle Entegrasyon

### Mevcut ResilientSyncService
Mevcut `ResilientSyncService.java` kendi 4-seviye korumasını kullanıyor:
- Level 1: Try-Catch
- Level 2: Timeout (3 dakika)
- Level 3: Semaphore Bulkhead
- Level 4: Circuit Breaker

**Strateji**: Mevcut servis çalışmaya devam eder. Yeni `ResilientApiClient` paralel olarak kullanılabilir. Gelecekte migration yapılabilir.

### Mevcut TrendyolRateLimiter
Per-store Guava RateLimiter (10 req/sec) çalışmaya devam eder. Resilience4j rate limiter global koruma sağlar.

## Monitoring

### Actuator Endpoints
```bash
# Health check (circuit breaker durumu dahil)
curl localhost:8080/actuator/health

# Resilience4j metrics
curl localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
curl localhost:8080/actuator/metrics/resilience4j.ratelimiter.available.permissions
```

### Admin Panel Endpoints
```bash
# Tüm durum
curl localhost:8080/api/admin/resilience/status

# Circuit breaker reset
curl -X POST localhost:8080/api/admin/resilience/circuit-breakers/trendyolApi/reset
```

## Davranış Özeti

### Circuit Breaker State Transitions
```
CLOSED --[%50 hata]--> OPEN --[30s]--> HALF_OPEN --[3 başarılı]--> CLOSED
                                            |
                                            v
                                    [1 hata]--> OPEN
```

### Retry Backoff Pattern
```
İstek 1: Başarısız → 500ms bekle
İstek 2: Başarısız → 1000ms bekle (500 × 2)
İstek 3: Başarısız → ResilientApiException(RETRY_EXHAUSTED)
```

### Rate Limiter Behavior
```
0-10 istek/saniye: OK
11+ istek/saniye: ResilientApiException(RATE_LIMIT_EXCEEDED)
                  (Non-blocking, hemen fail)
```

## Notlar

1. **Annotation Order**: Circuit Breaker → Retry → Rate Limiter → Bulkhead → Time Limiter
2. **Non-Blocking Design**: Rate limiter ve bulkhead timeout 0ms - thread bloklamaz
3. **Fallback Strategy**: Exception fırlatır, caller handle eder
4. **Metrics**: Micrometer entegrasyonu ile Prometheus/Grafana ready
