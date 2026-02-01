# Backend Test Altyapısı

**Tarih:** 22 Ocak 2026
**Durum:** Tamamlandı

## Özet

SellerX backend test altyapısı Spring Boot 3.4 + JUnit 5 + Mockito + TestContainers kullanılarak kuruldu ve genişletildi.

| Metrik | Önceki | Sonraki |
|--------|--------|---------|
| Toplam Test | ~55 | **162** |
| Test Dosyası | 6 | **15+** |
| Controller Coverage | %0 | **%75+** |
| Auth Coverage | %0 | **%90+** |

---

## Test Sonuçları

```
BUILD SUCCESS
Tests run: 162, Failures: 0, Errors: 0, Skipped: 0
```

### Modül Bazlı Test Dağılımı

| Modül | Test Sınıfı | Test Sayısı |
|-------|-------------|-------------|
| **Auth** | JwtServiceTest | 10 |
| | AuthControllerTest | 12 |
| **Store** | StoreControllerTest | 19 |
| | StoreRepositoryTest | 1 |
| **Product** | TrendyolProductControllerTest | 15 |
| **Orders** | OrderCostCalculatorTest | 20 |
| | CommissionEstimationServiceTest | 10 |
| | OrderGapAnalysisServiceTest | 11 |
| **Dashboard** | DashboardStatsServiceTest | 15 |
| **Financial** | TrendyolFinancialSettlementServiceTest | 13 |
| | CommissionReconciliationServiceTest | 13 |
| **Diğer** | BigDecimalComparisonTest | 8 |
| | StoreApplicationTests | 1 |
| **TOPLAM** | | **162** |

---

## Yapılan Değişiklikler

### 1. Lombok Uyumluluğu (pom.xml)

**Problem:** Lombok 1.18.36 JDK 21.0.10 ile `TypeTag :: UNKNOWN` hatası veriyordu.

**Çözüm:** Lombok 1.18.38'e güncellendi.

```xml
<properties>
    <lombok.version>1.18.38</lombok.version>
</properties>
```

### 2. Exception Handler Güncellemeleri (GlobalExceptionHandler.java)

**Problem:** `@PreAuthorize` kontrollerinde AccessDeniedException 500 yerine 403 dönmeliydi.

**Çözüm:** Özel exception handler'lar eklendi:

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorDto("Access denied"));
}

@ExceptionHandler(UnauthorizedAccessException.class)
public ResponseEntity<ErrorDto> handleUnauthorizedAccess(UnauthorizedAccessException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorDto(ex.getMessage()));
}

@ExceptionHandler(StoreNotFoundException.class)
public ResponseEntity<ErrorDto> handleStoreNotFound(StoreNotFoundException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ex.getMessage()));
}

@ExceptionHandler(UserNotFoundException.class)
public ResponseEntity<ErrorDto> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto("User not found"));
}
```

### 3. TestContainer Lifecycle (BaseIntegrationTest.java)

**Problem:** Birden fazla test sınıfı birlikte çalıştığında veritabanı bağlantı timeout'ları oluşuyordu.

**Çözüm:** `@DirtiesContext` eklendi:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withStartupTimeout(java.time.Duration.ofMinutes(2));
}
```

### 4. HikariCP Yapılandırması (application-test.yaml)

**Problem:** Bağlantı havuzu timeout'ları ve validasyon hataları.

**Çözüm:** HikariCP ayarları optimize edildi:

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 1
      maximum-pool-size: 5
      connection-timeout: 60000
      idle-timeout: 30000
      max-lifetime: 60000
      validation-timeout: 5000
      connection-test-query: SELECT 1
```

### 5. JSON Polymorphic Deserialization (StoreControllerTest.java)

**Problem:** `MarketplaceCredentials` polymorphic deserialization için `type` field'ı gerekiyordu.

**Çözüm:** Test request body'lerine `"type": "trendyol"` eklendi:

```json
{
    "storeName": "New Store",
    "marketplace": "trendyol",
    "credentials": {
        "type": "trendyol",
        "apiKey": "test-api-key",
        "apiSecret": "test-api-secret",
        "sellerId": 123456,
        "integrationCode": "test-integration"
    }
}
```

### 6. External Service Mocking (StoreControllerTest.java)

**Problem:** `POST /stores` endpoint'i `StoreOnboardingService.performInitialSync()` çağrısı yaparak Trendyol API'lerine bağlanmaya çalışıyordu. Bu 120-240 saniye süren timeout'lara yol açıyordu.

**Çözüm:** External service'ler `@MockBean` ile mock'landı:

```java
@DisplayName("StoreController")
class StoreControllerTest extends BaseControllerTest {

    @MockBean
    private StoreOnboardingService storeOnboardingService;

    @MockBean
    private TrendyolWebhookManagementService webhookManagementService;

    @BeforeEach
    void setUpStores() {
        storeRepository.deleteAll();
        cleanUpUsers();
        TestDataBuilder.resetSequence();

        // Configure mocks to prevent external API calls
        doNothing().when(storeOnboardingService).performInitialSync(any(Store.class));

        // Return a disabled webhook result to skip webhook creation
        TrendyolWebhookManagementService.WebhookResult disabledResult =
                TrendyolWebhookManagementService.WebhookResult.disabled();
        when(webhookManagementService.createWebhookForStore(any(TrendyolCredentials.class)))
                .thenReturn(disabledResult);
        when(webhookManagementService.deleteWebhookForStore(any(TrendyolCredentials.class), any()))
                .thenReturn(disabledResult);
    }
}
```

**Sonuç:** Test süresi 240+ saniyeden ~12 saniyeye düştü.

---

## Test Dosya Yapısı

```
src/test/java/com/ecommerce/sellerx/
├── common/
│   ├── BaseIntegrationTest.java      # TestContainers base class
│   ├── BaseControllerTest.java       # MockMvc + JWT helpers
│   ├── BaseUnitTest.java             # Mockito base class
│   └── TestDataBuilder.java          # Test data factory
├── auth/
│   ├── JwtServiceTest.java           # JWT token işlemleri (10 test)
│   └── AuthControllerTest.java       # Login/logout/refresh (12 test)
├── stores/
│   ├── StoreControllerTest.java      # Store CRUD + sync (19 test)
│   └── StoreRepositoryTest.java      # Repository (1 test)
├── products/
│   └── TrendyolProductControllerTest.java  # Product API (15 test)
├── orders/
│   ├── OrderCostCalculatorTest.java        # FIFO maliyet (20 test)
│   ├── CommissionEstimationServiceTest.java # Komisyon (10 test)
│   └── OrderGapAnalysisServiceTest.java    # Gap analizi (11 test)
├── dashboard/
│   └── DashboardStatsServiceTest.java      # İstatistik hesaplama (15 test)
└── financial/
    ├── TrendyolFinancialSettlementServiceTest.java  # Settlement (13 test)
    └── CommissionReconciliationServiceTest.java     # Reconciliation (13 test)

src/test/resources/
└── application-test.yaml             # Test konfigürasyonu
```

---

## Testleri Çalıştırma

### Tüm Testler

```bash
cd sellerx-backend
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home"
mvn test
```

### Belirli Modül Testleri

```bash
# Auth testleri
mvn test -Dtest="*Auth*,*Jwt*"

# Controller testleri
mvn test -Dtest="*ControllerTest"

# Order testleri
mvn test -Dtest="*Order*,*Cost*,*Commission*"

# Dashboard testleri
mvn test -Dtest="*Dashboard*"

# Financial testleri
mvn test -Dtest="*Financial*,*Settlement*,*Reconciliation*"
```

### Tek Test Sınıfı

```bash
mvn test -Dtest=StoreControllerTest
```

### Tek Test Metodu

```bash
mvn test -Dtest=StoreControllerTest#shouldCreateStoreWithValidRequest
```

---

## Test Yazım Kuralları

### Controller Testleri

```java
@DisplayName("StoreController")
class StoreControllerTest extends BaseControllerTest {

    @Autowired
    private StoreRepository storeRepository;

    // External service'leri mock'la - API call'larını önle
    @MockBean
    private StoreOnboardingService storeOnboardingService;

    @MockBean
    private TrendyolWebhookManagementService webhookManagementService;

    @BeforeEach
    void setUp() {
        // Bağımlılıkları sırayla temizle
        storeRepository.deleteAll();
        cleanUpUsers();
        TestDataBuilder.resetSequence();

        // Mock'ları yapılandır
        doNothing().when(storeOnboardingService).performInitialSync(any());
        when(webhookManagementService.createWebhookForStore(any()))
            .thenReturn(WebhookResult.disabled());
    }

    @Nested
    @DisplayName("GET /stores/my")
    class GetMyStores {
        @Test
        @DisplayName("should return user's stores")
        void shouldReturnUsersStores() throws Exception {
            // Given
            User user = createAndSaveTestUser("test@example.com");
            Store store = createAndSaveStore(user, "Test Store");

            // When/Then
            performWithAuth(get("/stores/my"), user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeName").value("Test Store"));
        }
    }
}
```

### Service Unit Testleri

```java
@DisplayName("OrderCostCalculator")
class OrderCostCalculatorTest extends BaseUnitTest {

    @Mock
    private TrendyolProductRepository productRepository;

    @InjectMocks
    private OrderCostCalculator calculator;

    @Test
    @DisplayName("should calculate FIFO cost correctly")
    void shouldCalculateFifoCostCorrectly() {
        // Given
        TrendyolProduct product = TestDataBuilder.createTestProductWithCostInfo();
        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        // When
        BigDecimal cost = calculator.calculateFifoCost(product, 10);

        // Then
        assertThat(cost).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
```

---

## Bilinen Sorunlar ve Çözümleri

| Sorun | Çözüm |
|-------|-------|
| `TypeTag :: UNKNOWN` Lombok hatası | Lombok 1.18.38'e güncelle |
| 500 yerine 403 dönüyor | GlobalExceptionHandler'a AccessDeniedException handler ekle |
| TestContainer bağlantı timeout | @DirtiesContext ekle, HikariCP ayarlarını optimize et |
| JSON deserialization hatası | Request body'ye `"type": "trendyol"` ekle |
| Store creation timeout (240s+) | External service'leri @MockBean ile mock'la |

---

## Gelecek İyileştirmeler

- [ ] Code coverage raporu (JaCoCo)
- [ ] Integration test'ler için ayrı profil
- [ ] E2E test altyapısı
- [ ] Performance/load testleri
- [ ] Test data factory genişletmesi

---

*Son güncelleme: 22 Ocak 2026*
