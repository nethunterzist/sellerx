# Mağaza Onboarding Sistemi

> Yeni mağaza eklendiğinde otomatik veri senkronizasyonu altyapısı.

## Genel Bakış

Kullanıcı yeni bir mağaza eklediğinde, sistem otomatik olarak ürünleri, siparişleri ve finansal verileri Trendyol API'sinden çeker. Bu işlem asenkron olarak arka planda gerçekleşir ve kullanıcı gerçek zamanlı ilerlemeyi takip edebilir.

---

## Mimari

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           ONBOARDING AKIŞI                              │
└─────────────────────────────────────────────────────────────────────────┘

     Frontend                    Backend                      Trendyol
        │                          │                             │
        │  POST /api/stores        │                             │
        │─────────────────────────▶│                             │
        │                          │                             │
        │                   ┌──────┴──────┐                      │
        │                   │ Store Create │                     │
        │                   │ syncStatus:  │                     │
        │                   │  "pending"   │                     │
        │                   └──────┬──────┘                      │
        │                          │                             │
        │  { id, syncStatus }      │ @Async                      │
        │◀─────────────────────────│                             │
        │                          │                             │
        │                   ┌──────┴──────┐                      │
        │                   │ Onboarding  │                      │
        │                   │  Service    │                      │
        │                   └──────┬──────┘                      │
        │                          │                             │
        │  Poll: GET /stores/{id}  │  SYNCING_PRODUCTS           │
        │─────────────────────────▶│─────────────────────────────▶│
        │  { syncStatus }          │◀─────────────────────────────│
        │◀─────────────────────────│                             │
        │                          │                             │
        │  Poll: GET /stores/{id}  │  SYNCING_ORDERS             │
        │─────────────────────────▶│─────────────────────────────▶│
        │  { syncStatus }          │◀─────────────────────────────│
        │◀─────────────────────────│                             │
        │                          │                             │
        │  Poll: GET /stores/{id}  │  SYNCING_FINANCIAL          │
        │─────────────────────────▶│─────────────────────────────▶│
        │  { syncStatus }          │◀─────────────────────────────│
        │◀─────────────────────────│                             │
        │                          │                             │
        │  Poll: GET /stores/{id}  │  COMPLETED                  │
        │─────────────────────────▶│                             │
        │  { syncStatus,           │                             │
        │    initialSyncCompleted  │                             │
        │    = true }              │                             │
        │◀─────────────────────────│                             │
        │                          │                             │
        │  Redirect to Dashboard   │                             │
        │                          │                             │
```

---

## Sync Status Değerleri

| Status | Açıklama | Progress |
|--------|----------|----------|
| `pending` | Senkronizasyon henüz başlamadı | 0% |
| `SYNCING_PRODUCTS` | Ürünler çekiliyor | 25% |
| `SYNCING_ORDERS` | Siparişler çekiliyor | 50% |
| `SYNCING_FINANCIAL` | Finansal veriler çekiliyor | 75% |
| `COMPLETED` | Senkronizasyon tamamlandı | 100% |
| `FAILED` | Senkronizasyon başarısız | - |

---

## Backend Implementasyonu

### 1. Store Entity (stores tablosu)

```java
// Store.java - Yeni alanlar
@Column(name = "sync_status")
private String syncStatus = "pending";

@Column(name = "sync_error_message")
private String syncErrorMessage;

@Column(name = "initial_sync_completed")
private Boolean initialSyncCompleted = false;
```

### 2. AsyncConfig (Thread Pool)

```java
@Configuration
public class AsyncConfig {

    @Bean(name = "onboardingExecutor")
    public Executor onboardingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // Minimum 5 thread
        executor.setMaxPoolSize(10);      // Maximum 10 thread
        executor.setQueueCapacity(100);   // 100 işlem kuyruğu
        executor.setThreadNamePrefix("onboarding-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

### 3. StoreOnboardingService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreOnboardingService {

    private final StoreRepository storeRepository;
    private final TrendyolProductService productService;
    private final TrendyolOrderService orderService;
    private final TrendyolFinancialSettlementService financialService;
    private final TrendyolRateLimiter rateLimiter;

    /**
     * Mağaza için initial sync başlatır.
     * @Async ile ayrı thread'de çalışır.
     */
    @Async("onboardingExecutor")
    public void performInitialSync(Store store) {
        UUID storeId = store.getId();
        log.info("Starting initial sync for store: {}", storeId);

        try {
            // 1. Ürünleri senkronize et
            updateSyncStatus(storeId, "SYNCING_PRODUCTS", null);
            rateLimiter.acquire();
            productService.fetchAndSaveProductsForStore(storeId);

            // 2. Siparişleri senkronize et
            updateSyncStatus(storeId, "SYNCING_ORDERS", null);
            rateLimiter.acquire();
            orderService.fetchAndSaveOrdersForStore(storeId);

            // 3. Finansal verileri senkronize et
            updateSyncStatus(storeId, "SYNCING_FINANCIAL", null);
            rateLimiter.acquire();
            financialService.fetchAndSaveSettlementsForStore(storeId);

            // 4. Tamamlandı
            markSyncCompleted(storeId);
            log.info("Initial sync completed for store: {}", storeId);

        } catch (Exception e) {
            log.error("Initial sync failed for store: {}", storeId, e);
            updateSyncStatus(storeId, "FAILED", e.getMessage());
        }
    }

    private void updateSyncStatus(UUID storeId, String status, String errorMessage) {
        storeRepository.findById(storeId).ifPresent(store -> {
            store.setSyncStatus(status);
            store.setSyncErrorMessage(errorMessage);
            storeRepository.save(store);
        });
    }

    private void markSyncCompleted(UUID storeId) {
        storeRepository.findById(storeId).ifPresent(store -> {
            store.setSyncStatus("COMPLETED");
            store.setSyncErrorMessage(null);
            store.setInitialSyncCompleted(true);
            storeRepository.save(store);
        });
    }
}
```

### 4. StoreService Entegrasyonu

```java
@Service
public class StoreService {

    private final StoreOnboardingService onboardingService;

    @Transactional
    public Store createStore(CreateStoreRequest request, User user) {
        // Store oluştur
        Store store = new Store();
        store.setStoreName(request.getStoreName());
        store.setMarketplace(request.getMarketplace());
        store.setCredentials(request.getCredentials());
        store.setUser(user);
        store.setSyncStatus("pending");

        Store savedStore = storeRepository.save(store);

        // Async olarak initial sync başlat
        onboardingService.performInitialSync(savedStore);

        return savedStore;
    }
}
```

---

## Frontend Implementasyonu

### 1. Store Type (types/store.ts)

```typescript
export type SyncStatus =
  | "pending"
  | "SYNCING_PRODUCTS"
  | "SYNCING_ORDERS"
  | "SYNCING_FINANCIAL"
  | "COMPLETED"
  | "FAILED";

export interface Store {
  id: string;
  userId: number;
  storeName: string;
  marketplace: string;
  syncStatus?: SyncStatus;
  syncErrorMessage?: string;
  initialSyncCompleted?: boolean;
  // ... diğer alanlar
}

// Helper fonksiyonlar
export function getSyncStatusMessage(status: SyncStatus | undefined): string {
  switch (status) {
    case "pending": return "Senkronizasyon bekleniyor...";
    case "SYNCING_PRODUCTS": return "Ürünler senkronize ediliyor...";
    case "SYNCING_ORDERS": return "Siparişler senkronize ediliyor...";
    case "SYNCING_FINANCIAL": return "Finansal veriler senkronize ediliyor...";
    case "COMPLETED": return "Senkronizasyon tamamlandı";
    case "FAILED": return "Senkronizasyon başarısız";
    default: return "Bilinmeyen durum";
  }
}

export function isSyncInProgress(status: SyncStatus | undefined): boolean {
  return status === "pending" ||
         status === "SYNCING_PRODUCTS" ||
         status === "SYNCING_ORDERS" ||
         status === "SYNCING_FINANCIAL";
}

export function getSyncProgress(status: SyncStatus | undefined): number {
  switch (status) {
    case "pending": return 0;
    case "SYNCING_PRODUCTS": return 25;
    case "SYNCING_ORDERS": return 50;
    case "SYNCING_FINANCIAL": return 75;
    case "COMPLETED": return 100;
    default: return 0;
  }
}
```

### 2. SyncStatusDisplay Component

```tsx
// components/stores/sync-status-display.tsx
export function SyncStatusDisplay({ store, onComplete }: SyncStatusDisplayProps) {
  const queryClient = useQueryClient();
  const syncStatus = store.syncStatus;
  const isInProgress = isSyncInProgress(syncStatus);
  const progress = getSyncProgress(syncStatus);

  // Sync devam ederken her 3 saniyede bir poll et
  useEffect(() => {
    if (!isInProgress) {
      if (syncStatus === "COMPLETED" && onComplete) {
        onComplete();
      }
      return;
    }

    const interval = setInterval(() => {
      queryClient.invalidateQueries({ queryKey: storeKeys.detail(store.id) });
    }, 3000);

    return () => clearInterval(interval);
  }, [isInProgress, syncStatus, store.id, queryClient, onComplete]);

  return (
    <div className="space-y-4">
      {/* Status icon ve mesaj */}
      <div className="flex items-center gap-3">
        {getStatusIcon()}
        <p className="font-medium">{getSyncStatusMessage(syncStatus)}</p>
      </div>

      {/* Progress bar */}
      {isInProgress && (
        <Progress value={progress} className="h-2" />
      )}

      {/* Step indicators: Ürünler → Siparişler → Finansal */}
      <div className="flex justify-between gap-2">
        <StepIndicator label="Ürünler" status={getStepStatus("SYNCING_PRODUCTS")} />
        <StepIndicator label="Siparişler" status={getStepStatus("SYNCING_ORDERS")} />
        <StepIndicator label="Finansal" status={getStepStatus("SYNCING_FINANCIAL")} />
      </div>
    </div>
  );
}
```

### 3. New Store Page Kullanımı

```tsx
// app/[locale]/(app-shell)/new-store/page.tsx
export default function NewStorePage() {
  const [createdStoreId, setCreatedStoreId] = useState<string | null>(null);
  const { data: createdStore } = useStore(createdStoreId || "");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    createStoreMutation.mutate(storeData, {
      onSuccess: (data) => {
        if (data?.id) {
          setSelectedStoreMutation.mutate(data.id, {
            onSuccess: () => {
              // Sync status display'i göster
              setCreatedStoreId(data.id);
            },
          });
        }
      },
    });
  };

  // Sync devam ediyorsa overlay göster
  if (createdStoreId && createdStore) {
    return (
      <div className="fixed inset-0 bg-background/90 z-50">
        <Card>
          <CardHeader>
            <CardTitle>Mağaza Senkronizasyonu</CardTitle>
          </CardHeader>
          <CardContent>
            <SyncStatusDisplay
              store={createdStore}
              onComplete={() => router.push("/dashboard")}
            />
          </CardContent>
        </Card>
      </div>
    );
  }

  // Normal form...
}
```

---

## Veritabanı Migration

```sql
-- V46__add_store_sync_tracking.sql

-- Sync tracking alanları
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_status VARCHAR(50) DEFAULT 'pending';
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_error_message TEXT;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS initial_sync_completed BOOLEAN DEFAULT FALSE;

-- Webhook tracking alanları
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_status VARCHAR(50) DEFAULT 'pending';
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_error_message TEXT;
```

---

## Hata Yönetimi

### Başarısız Senkronizasyon

```java
// StoreOnboardingService.java
catch (Exception e) {
    log.error("Initial sync failed for store: {}", storeId, e);

    // Status'u FAILED yap ve hata mesajını kaydet
    updateSyncStatus(storeId, "FAILED", e.getMessage());

    // Kullanıcı daha sonra manuel olarak tekrar deneyebilir
}
```

### Frontend'de Hata Gösterimi

```tsx
// SyncStatusDisplay.tsx
{syncStatus === "FAILED" && store.syncErrorMessage && (
  <div className="text-red-500">
    <p>Hata: {store.syncErrorMessage}</p>
    <Button onClick={handleRetry}>Tekrar Dene</Button>
  </div>
)}
```

---

## Performans Optimizasyonları

### 1. Thread Pool Boyutu
- **Core Pool Size: 5** - Normal yükte 5 eşzamanlı onboarding
- **Max Pool Size: 10** - Yoğun dönemlerde 10'a kadar
- **Queue Capacity: 100** - 100 mağaza kuyruğa alınabilir

### 2. Rate Limiting
Her API çağrısından önce `rateLimiter.acquire()` çağrılır. Bu, Trendyol API limitlerini aşmamayı garantiler.

### 3. Polling Optimizasyonu
Frontend sadece sync devam ederken poll yapar. Tamamlandığında polling durur ve `onComplete` callback çağrılır.

---

## Test Senaryoları

| Senaryo | Beklenen Sonuç |
|---------|----------------|
| Başarılı onboarding | Status: COMPLETED, initialSyncCompleted: true |
| API hatası | Status: FAILED, syncErrorMessage dolu |
| Partial failure | Son başarılı adımda kalır, hata mesajı gösterilir |
| Concurrent onboarding | Thread pool queue'ya eklenir, sırayla işlenir |
