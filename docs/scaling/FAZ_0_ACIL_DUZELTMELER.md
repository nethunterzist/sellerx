# Faz 0: Acil Düzeltmeler

> **Durum**: ✅ Tamamlandı
> **Tarih**: Şubat 2026
> **Süre**: ~1 Hafta

Bu faz, sistemin ölçeklenmeden önce kritik güvenlik açıklarının ve performans darboğazlarının düzeltilmesini kapsar.

---

## 0.1 Güvenlik Açığı Düzeltmesi ✅

### Problem

`/sync-all` endpoint'i `@PreAuthorize` kontrolü olmadan tanımlanmıştı. Bu, herhangi bir authenticated kullanıcının TÜM store'ların senkronizasyonunu tetikleyebilmesi anlamına geliyordu.

```java
// ÖNCE - Güvenlik açığı
@PostMapping("/sync-all")
public ResponseEntity<String> syncOrdersForAllStores() {
    // Herkes tetikleyebilir!
}
```

### Çözüm

`@PreAuthorize("hasRole('ADMIN')")` eklendi:

```java
// SONRA - Güvenli
@PostMapping("/sync-all")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<String> syncOrdersForAllStores() {
    // Sadece ADMIN kullanıcılar tetikleyebilir
}
```

### Etkilenen Dosyalar

| Dosya | Değişiklik |
|-------|-----------|
| `TrendyolOrderController.java` | `@PreAuthorize("hasRole('ADMIN')")` eklendi |

---

## 0.2 Unbounded Query Düzeltmeleri ✅

### Problem

Admin endpoint'leri pagination olmadan tüm veriyi çekiyordu. 5000 store'da bu memory overflow'a neden olabilirdi.

```java
// ÖNCE - Tehlikeli
public List<Store> getAllStores() {
    return storeRepository.findAll(); // 5000+ kayıt memory'ye!
}
```

### Çözüm

Tüm admin endpoint'lere pagination eklendi:

```java
// SONRA - Güvenli
public Page<Store> getAllStores(Pageable pageable) {
    return storeRepository.findAll(pageable);
}
```

### Etkilenen Dosyalar

| Dosya | Değişiklik |
|-------|-----------|
| `AdminStoreService.java` | `findAll()` → `findAll(pageable)` |
| `AdminStoreController.java` | `Pageable` parametresi eklendi |
| `AdminUserService.java` | Pagination desteği |
| `AdminUserController.java` | `Pageable` parametresi eklendi |

---

## 0.3 Async Sync Endpoint'leri ✅

### Problem

Senkronizasyon işlemleri senkron ve blocking idi. 10,000 ürünlük bir store için sync işlemi dakikalarca request'i bloke ediyordu.

```java
// ÖNCE - Blocking
@PostMapping("/sync/{storeId}")
public ResponseEntity<Void> sync(@PathVariable UUID storeId) {
    productService.syncAllProducts(storeId); // 5+ dakika blok!
    return ResponseEntity.ok().build();
}
```

### Çözüm

Async pattern implementasyonu:

```
Client                    Server                    Background Worker
  |                         |                              |
  |-- POST /sync ---------->|                              |
  |                         |-- Create SyncTask ---------->|
  |<-- 202 Accepted --------|                              |
  |   (taskId: abc123)      |                              |
  |                         |                              |
  |-- GET /sync/status ---->|                              |
  |<-- {status: RUNNING} ---|                              |
  |                         |                              |
  |-- GET /sync/status ---->|                              |
  |<-- {status: COMPLETED} -|                              |
```

### Yeni Bileşenler

#### 1. SyncTask Entity

```java
@Entity
@Table(name = "sync_tasks")
public class SyncTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private SyncType type;  // ORDERS, PRODUCTS, FINANCIAL, ALL

    @Enumerated(EnumType.STRING)
    private SyncTaskStatus status;  // PENDING, RUNNING, COMPLETED, FAILED

    private UUID storeId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private Integer progress;  // 0-100
}
```

#### 2. SyncTaskService

```java
@Service
public class SyncTaskService {

    @Transactional
    public SyncTask createTask(UUID storeId, SyncType type) {
        SyncTask task = SyncTask.builder()
            .storeId(storeId)
            .type(type)
            .status(SyncTaskStatus.PENDING)
            .progress(0)
            .build();
        return syncTaskRepository.save(task);
    }

    @Async("syncTaskExecutor")
    @Transactional
    public void executeAsync(UUID taskId) {
        // Async execution with progress tracking
    }

    public SyncTask getTask(UUID taskId) {
        return syncTaskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found"));
    }
}
```

#### 3. AsyncConfig

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("syncTaskExecutor")
    public Executor syncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("SyncTask-");
        executor.initialize();
        return executor;
    }
}
```

#### 4. Controller Güncellemesi

```java
// SONRA - Non-blocking
@PostMapping("/sync/{storeId}")
public ResponseEntity<SyncTaskDto> startSync(
        @PathVariable UUID storeId,
        @RequestParam(defaultValue = "ALL") SyncType type) {
    SyncTask task = syncTaskService.createTask(storeId, type);
    syncTaskService.executeAsync(task.getId());

    return ResponseEntity.accepted()
        .header("Location", "/api/sync/tasks/" + task.getId())
        .body(SyncTaskDto.from(task));
}

@GetMapping("/sync/tasks/{taskId}")
public ResponseEntity<SyncTaskDto> getTaskStatus(@PathVariable UUID taskId) {
    return ResponseEntity.ok(SyncTaskDto.from(syncTaskService.getTask(taskId)));
}
```

### Etkilenen/Oluşturulan Dosyalar

| Dosya | Tür | Açıklama |
|-------|-----|----------|
| `SyncTask.java` | Yeni | Entity sınıfı |
| `SyncTaskStatus.java` | Yeni | Enum (PENDING, RUNNING, COMPLETED, FAILED) |
| `SyncType.java` | Yeni | Enum (ORDERS, PRODUCTS, FINANCIAL, ALL) |
| `SyncTaskRepository.java` | Yeni | JPA Repository |
| `SyncTaskService.java` | Yeni | Async işlem yönetimi |
| `SyncTaskDto.java` | Yeni | Response DTO |
| `TrendyolProductController.java` | Güncellendi | Async endpoint |
| `TrendyolOrderController.java` | Güncellendi | Async endpoint |
| `AsyncConfig.java` | Güncellendi | Thread pool konfigürasyonu |
| `V111__create_sync_tasks_table.sql` | Yeni | Flyway migration |

### Migration

```sql
-- V111__create_sync_tasks_table.sql
CREATE TABLE sync_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    progress INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sync_tasks_store_id ON sync_tasks(store_id);
CREATE INDEX idx_sync_tasks_status ON sync_tasks(status);
CREATE INDEX idx_sync_tasks_created_at ON sync_tasks(created_at DESC);
```

---

## 0.3.5 Test Düzeltmeleri ✅

### Problem

`AuthServiceTest` sınıfında `SecurityContextHolder.setContext()` ile mock SecurityContext set ediliyordu ancak test sonrası temizlenmiyordu. Bu, test isolation sorununa neden oluyordu.

```java
// ÖNCE - SecurityContext leak
@BeforeEach
void setUp() {
    SecurityContextHolder.setContext(securityContext);
    // @AfterEach yok → context diğer testlere sızıyor!
}
```

### Semptomlar

- `StoreControllerTest$GetSyncProgress` tek başına çalışınca PASS
- `AuthServiceTest` + `StoreControllerTest` birlikte çalışınca FAIL
- Hata: `Status expected:<200> but was:<403>`

### Kök Neden Analizi

1. `AuthServiceTest` mock `SecurityContext` set ediyor
2. Test bittikten sonra temizlenmiyor
3. `StoreControllerTest` gerçek authentication bekliyor
4. Mock context sızıyor → yetkilendirme başarısız → 403

### Çözüm

`@AfterEach` ile SecurityContext temizleme:

```java
// SONRA - Düzgün izolasyon
import org.junit.jupiter.api.AfterEach;

@BeforeEach
void setUp() {
    authService = new AuthService(authenticationManager, userRepository, jwtService);
    testUser = TestDataBuilder.user()
            .id(1L)
            .email("test@example.com")
            .role(Role.USER)
            .build();
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
}

@AfterEach
void tearDown() {
    // Mock context'i temizle - diğer testlere sızmasını önle
    SecurityContextHolder.clearContext();
}
```

### Etkilenen Dosyalar

| Dosya | Değişiklik |
|-------|-----------|
| `AuthServiceTest.java` | `@AfterEach tearDown()` eklendi |

### Doğrulama

```bash
# Tüm testleri çalıştır
./mvnw test

# Sonuç
Tests run: 723, Failures: 0, Errors: 0, Skipped: 0
```

---

## Öğrenilen Dersler

### 1. Test Isolation Önemi

Spring Security testlerinde `SecurityContextHolder` kullanıldığında mutlaka cleanup yapılmalı:

```java
@AfterEach
void tearDown() {
    SecurityContextHolder.clearContext();
}
```

### 2. Async Pattern Faydaları

| Metrik | Önce (Senkron) | Sonra (Async) |
|--------|----------------|---------------|
| Request timeout riski | Yüksek | Yok |
| İstemci bekleme süresi | 5+ dakika | <100ms |
| Progress tracking | Yok | Var (0-100%) |
| Hata yönetimi | Try-catch | Status + errorMessage |

### 3. Lombok Build Sorunları

Lombok ile MapStruct kullanırken `pom.xml`'de sıralama kritik:

```xml
<annotationProcessorPaths>
    <!-- ÖNCE Lombok -->
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </path>
    <!-- SONRA MapStruct -->
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
    </path>
</annotationProcessorPaths>
```

Build sorunlarında:
```bash
rm -rf target && ./mvnw compile
```

---

## Sonraki Adımlar

Faz 0 tamamlandı. Sıradaki fazlar:

1. **Faz 1**: RabbitMQ entegrasyonu - Sync queue'ları
2. **Faz 2**: Resilience4j - Circuit breaker, retry
3. **Faz 3**: WebSocket - Real-time alerts

Detaylar için: [Scaling Plan README](./README.md)
