# SellerX Geliştirme Günlüğü (Changelog)

Bu dosya projedeki tüm önemli değişiklikleri kronolojik olarak takip eder.

---

## [2026-02-04] - Admin Impersonation (Hesabına Gir)

### Özet
Admin panelinden kullanıcıların hesabını **salt okunur (read-only)** olarak yeni sekmede görüntüleme özelliği. Müşteri destek işlemlerinde admin, kullanıcının dashboard'unu doğrudan görebilir; veri değişikliği yapılamaz.

### Backend Değişiklikleri

**Yeni Dosyalar:**
- **`auth/ReadOnlyInterceptor.java`** — `HandlerInterceptor`: readOnly attribute varsa POST/PUT/DELETE/PATCH isteklerini 403 ile reddeder. Allowlist: `/auth/refresh`, `/auth/me`
- **`config/WebConfig.java`** — ReadOnlyInterceptor'ı register eden `WebMvcConfigurer`
- **`admin/ImpersonationLog.java`** — Audit log JPA entity (admin, hedef kullanıcı, IP, user-agent)
- **`admin/ImpersonationLogRepository.java`** — Audit log repository
- **`db/migration/V91__add_impersonation_audit_log.sql`** — `impersonation_logs` tablosu + indexler

**Güncellenen Dosyalar:**
- **`auth/Jwt.java`** — `isImpersonated()`, `getImpersonatedBy()`, `isReadOnly()` claim accessor'ları
- **`auth/JwtService.java`** — `generateImpersonationToken(User, Long)` metodu (impersonatedBy + readOnly claim'leri)
- **`auth/JwtAuthenticationFilter.java`** — Token parse sonrası impersonation claim'lerini request attribute olarak set eder
- **`admin/AdminUserService.java`** — `generateImpersonationToken()` + audit log kaydı
- **`admin/AdminUserController.java`** — `POST /api/admin/users/{id}/impersonate` endpoint
- **`auth/AuthController.java`** — `/me` response'una impersonation metadata ekler
- **`users/UserDto.java`** — `isImpersonated`, `impersonatedBy`, `readOnly` alanları (nullable, @JsonInclude NON_NULL)

### Frontend Değişiklikleri

**Yeni Dosyalar:**
- **`hooks/use-impersonation.ts`** — SessionStorage bazlı state yönetimi (`isImpersonating`, `targetUserName`, `startImpersonation`, `stopImpersonation`)
- **`lib/api/bff-auth.ts`** — BFF route'lar için ortak auth helper; `X-Impersonation-Token` header varsa onu, yoksa cookie'yi kullanır
- **`components/admin/impersonation-banner.tsx`** — Sabit kırmızı banner: "[UserName] hesabını görüntülüyorsunuz - Salt Okunur" + Sonlandır butonu
- **`app/[locale]/(app-shell)/impersonate/page.tsx`** — Init sayfası: URL'den token alır → sessionStorage'a kaydeder → URL temizler → `/dashboard`'a redirect
- **`app/api/admin/users/[id]/impersonate/route.ts`** — BFF impersonation proxy route

**Güncellenen Dosyalar:**
- **`lib/api/client.ts`** — Impersonation token header injection + impersonation modda refresh devre dışı
- **`app/[locale]/(app-shell)/layout.tsx`** — ImpersonationBanner entegrasyonu
- **`app/[locale]/(admin)/admin/users/page.tsx`** — "Hesabına Gir" butonu (LogIn ikonu, onay dialog'u, yeni sekme açar)
- **`hooks/queries/use-admin.ts`** — `useImpersonateUser()` mutation hook
- **Tüm BFF route'lar (`app/api/**/route.ts`)** — `getBackendHeaders()` kullanımına geçirildi (impersonation token forwarding)
- **`messages/tr.json`** — Türkçe çeviriler (impersonation bölümü)
- **`messages/en.json`** — İngilizce çeviriler (impersonation bölümü)

### Veritabanı

**Yeni Tablo: `impersonation_logs`**
- `id`, `admin_user_id`, `target_user_id`, `action`, `ip_address`, `user_agent`, `created_at`
- Indexler: `admin_user_id`, `target_user_id`, `created_at DESC`

### Güvenlik
- JWT token 1 saat ömürlü, refresh yok
- Çift katmanlı salt okunur koruma (frontend UI + backend interceptor)
- Audit log: her impersonation kaydedilir
- SessionStorage: tab-scoped, diğer sekmelere sızmaz
- URL'deki token hemen temizlenir

### Mimari Dokümantasyon
- [Admin Impersonation Dokümantasyonu](architecture/ADMIN_IMPERSONATION.md)

---

## [2026-02-04] - KDV Sayfası: Mal Alış KDV'si (Purchase VAT)

### Özet
KDV sayfasındaki "SMM KDV" (Satılan Malın Maliyeti) hesaplaması kaldırılarak yerine **Mal Alış KDV'si** (Purchase VAT) hesaplaması getirildi. Artık KDV sayfasında satın alma siparişlerinin (PO) stok giriş tarihine göre aylık alış KDV'si hesaplanıyor. Terminoloji Türk Tek Düzen Hesap Planı'na uygun olarak "Mal Alış KDV'si" / "Hesaplanan KDV" şeklinde güncellendi.

### Backend Değişiklikleri

**Yeni DTO'lar:**
- **`PurchaseVatDto.java`** — Mal alış KDV özeti: toplam maliyet (KDV hariç), toplam KDV tutarı, toplam kalem sayısı, oran bazlı gruplama
- **`SalesVatDto.java`** — Satış KDV özeti: toplam satış tutarı (KDV hariç), toplam KDV tutarı, toplam sipariş sayısı, oran bazlı gruplama
- **`PurchaseVatDto.PurchaseVatByRate`** — KDV oran bazlı kırılım (%0, %10, %20 vb.)

**Repository Güncellemesi:**
- **`PurchaseOrderRepository.findClosedWithItemsByStoreId()`** — CLOSED statüdeki PO'ları item'larıyla birlikte fetch eden JPQL query

**Servis Güncellemesi:**
- **`TrendyolInvoiceService.getInvoiceSummary()`** — Purchase VAT hesaplama mantığı eklendi:
  - `stockEntryDate` fallback zinciri: `item.stockEntryDate → PO.stockEntryDate → PO.poDate`
  - Sadece CLOSED PO'lar dahil
  - KDV formülü: `totalCostPerUnit × unitsOrdered × costVatRate / 100`
  - Oran bazlı gruplama

**InvoiceSummaryDto Güncellemesi:**
- `costOfGoodsSold` alanı → `purchaseVat` (PurchaseVatDto) olarak değiştirildi
- `salesVat` (SalesVatDto) alanı eklendi

### Frontend Değişiklikleri

**KDV Sayfası (`app/[locale]/(app-shell)/kdv/page.tsx`):**
- "Mal Alış KDV'si" bölümü eklendi (kalem sayısı, KDV hariç maliyet, KDV tutarı)
- GİDER tablosunda "Mal Alış KDV'si" satırı
- Label değişikliği: "Stok Alım" → "Mal Alış" (Tek Düzen Hesap Planı uyumlu)
- Oran bazlı detay tablosu (%0, %10, %20)

**Type Güncellemesi (`types/financial.ts`):**
- `PurchaseVatData`, `PurchaseVatByRate`, `SalesVatData` TypeScript tipleri eklendi
- `InvoiceSummary` tipinde `purchaseVat` ve `salesVat` alanları

### Testler (6/6 Başarılı)

| Test | Açıklama |
|------|----------|
| `shouldCalculatePurchaseVatForItemsInPeriod` | Dönem içi kalem KDV hesaplama |
| `shouldExcludeItemsOutsidePeriod` | Dönem dışı kalemlerin hariç tutulması |
| `shouldUseItemLevelStockEntryDate` | Item-level stockEntryDate override |
| `shouldFallbackToPoDate` | stockEntryDate null → poDate fallback |
| `shouldGroupByVatRate` | %0, %10, %20 oran bazlı gruplama |
| `shouldReturnZeroWhenNoClosedPOs` | CLOSED PO yokken sıfır dönüş |

### Dosya Değişiklikleri

| Dosya | İşlem |
|-------|-------|
| `financial/dto/PurchaseVatDto.java` | YENİ |
| `financial/dto/SalesVatDto.java` | YENİ |
| `financial/dto/InvoiceSummaryDto.java` | GÜNCELLENDİ |
| `financial/TrendyolInvoiceService.java` | GÜNCELLENDİ |
| `purchasing/PurchaseOrderRepository.java` | GÜNCELLENDİ |
| `TrendyolInvoiceServicePurchaseVatTest.java` | YENİ |
| `sellerx-frontend/app/[locale]/(app-shell)/kdv/page.tsx` | GÜNCELLENDİ |
| `sellerx-frontend/types/financial.ts` | GÜNCELLENDİ |

---

## [2026-01-22] - Backend Test Altyapısı Genişletildi

### Özet
Backend test sayısı ~55'ten **162'ye** çıkarıldı. Spring Boot 3.4 + JUnit 5 + Mockito + TestContainers kullanılarak kapsamlı test altyapısı kuruldu.

### Yeni Test Dosyaları

| Test Sınıfı | Test Sayısı | Açıklama |
|-------------|-------------|----------|
| `JwtServiceTest` | 10 | JWT token oluşturma/doğrulama |
| `AuthControllerTest` | 12 | Login/logout/refresh endpoint'leri |
| `StoreControllerTest` | 19 | Store CRUD + sync yönetimi |
| `TrendyolProductControllerTest` | 15 | Product API testleri |
| `OrderCostCalculatorTest` | 20 | FIFO maliyet hesaplama |
| `DashboardStatsServiceTest` | 15 | İstatistik hesaplama |
| `TrendyolFinancialSettlementServiceTest` | 13 | Financial settlement |
| `CommissionReconciliationServiceTest` | 13 | Komisyon reconciliation |

### Düzeltmeler

**Lombok Uyumluluğu**
- `pom.xml`: Lombok 1.18.36 → 1.18.38 (JDK 21.0.10 uyumluluğu)

**Exception Handling**
- `GlobalExceptionHandler.java`: AccessDeniedException → 403 Forbidden
- `GlobalExceptionHandler.java`: UnauthorizedAccessException → 403 Forbidden
- `GlobalExceptionHandler.java`: StoreNotFoundException → 404 Not Found
- `GlobalExceptionHandler.java`: UserNotFoundException → 404 Not Found

**TestContainer Lifecycle**
- `BaseIntegrationTest.java`: `@DirtiesContext(classMode = AFTER_CLASS)` eklendi
- `application-test.yaml`: HikariCP bağlantı havuzu optimize edildi

**Test Data**
- `StoreControllerTest.java`: JSON polymorphic deserialization düzeltmesi (`"type": "trendyol"`)

### Yeni Dosyalar
- `src/test/java/com/ecommerce/sellerx/common/BaseIntegrationTest.java`
- `src/test/java/com/ecommerce/sellerx/common/BaseControllerTest.java`
- `src/test/java/com/ecommerce/sellerx/common/BaseUnitTest.java`
- `src/test/java/com/ecommerce/sellerx/common/TestDataBuilder.java`
- `src/test/resources/application-test.yaml`
- `docs/TEST_INFRASTRUCTURE.md`

### Testleri Çalıştırma
```bash
cd sellerx-backend
mvn test                           # Tüm testler
mvn test -Dtest=*ControllerTest    # Controller testleri
mvn test -Dtest=*Auth*             # Auth testleri
```

---

## [2026-01-21] - Eğitim Videoları Sistemi

### Yeni Özellikler

#### Backend - Education Videos Module

**Veritabanı**
- `V58__create_education_videos_tables.sql` - 3 yeni tablo oluşturuldu
  - `education_videos` - Eğitim videoları
  - `video_watch_history` - Video izlenme geçmişi
  - `user_notifications` - Kullanıcı bildirimleri

**Entity'ler**
- `EducationVideo.java` - Video entity (YouTube/Uploaded desteği)
- `VideoWatchHistory.java` - İzlenme kayıtları
- `UserNotification.java` - Bildirim entity
- `VideoCategory.java` - Enum (GETTING_STARTED, PRODUCTS, ORDERS, ANALYTICS, SETTINGS)
- `VideoType.java` - Enum (YOUTUBE, UPLOADED)
- `NotificationType.java` - Enum (VIDEO_ADDED, ORDER_UPDATE, vb.)

**Repository'ler**
- `EducationVideoRepository.java` - Video CRUD işlemleri
- `VideoWatchHistoryRepository.java` - İzlenme takibi
- `UserNotificationRepository.java` - Bildirim yönetimi

**Service'ler**
- `EducationVideoService.java` - Video yönetimi, izlenme takibi
- `NotificationService.java` - Bildirim oluşturma ve yönetimi

**Controller'lar**
- `EducationVideoController.java` - Video API endpoint'leri
- `NotificationController.java` - Bildirim API endpoint'leri

**Security**
- `EducationSecurityRules.java` - Public endpoint'ler için security rules
- Video listesi public, CRUD işlemleri admin-only

#### Frontend - Education Videos Integration

**Type Definitions**
- `types/education.ts` - EducationVideo, VideoCategory, VideoWatchStatus
- `types/notification.ts` - Notification, NotificationType

**React Query Hooks**
- `hooks/queries/use-education.ts` - Video CRUD ve izlenme takibi hook'ları
- `hooks/queries/use-notifications.ts` - Bildirim hook'ları

**API Routes (BFF Pattern)**
- `app/api/education/videos/route.ts` - Video listesi ve oluşturma
- `app/api/education/videos/[id]/route.ts` - Video detay, güncelleme, silme
- `app/api/education/videos/[id]/watch/route.ts` - İzlenme işaretleme
- `app/api/education/videos/my-watch-status/route.ts` - İzlenme durumu
- `app/api/notifications/route.ts` - Bildirimler
- `app/api/notifications/unread-count/route.ts` - Okunmamış sayısı
- `app/api/notifications/[id]/read/route.ts` - Okundu işaretleme

**Sayfalar**
- `app/[locale]/(app-shell)/education/page.tsx` - Mock data'dan API'ye geçiş
- `app/[locale]/(app-shell)/admin/education/page.tsx` - Admin panel (boş sayfa, sonra doldurulacak)

**Header Güncellemesi**
- `components/layout/header.tsx` - Eğitim videoları dropdown'ı API'ye bağlandı

### Özellikler

- **Video Yönetimi**: YouTube URL veya dosya upload desteği (upload altyapısı hazır)
- **İzlenme Takibi**: Kullanıcı bazında izlenme durumu
- **Kategori Sistemi**: 5 kategori (Başlangıç, Ürünler, Siparişler, Analitik, Ayarlar)
- **Bildirim Sistemi**: Yeni video eklendiğinde otomatik bildirim
- **Admin Panel**: Video CRUD işlemleri için endpoint'ler hazır (UI sonra eklenecek)

### Bildirim Akışı

Yeni video eklendiğinde:
1. Admin video oluşturur
2. Tüm aktif kullanıcılara otomatik `VIDEO_ADDED` bildirimi gönderilir
3. Frontend header'da badge gösterilir
4. Kullanıcı bildirime tıklayınca video sayfasına yönlendirilir

### Dosya Listesi

**Backend (20+ dosya)**
- Entity'ler, Repository'ler, Service'ler, Controller'lar
- DTO'lar ve Request/Response sınıfları
- Security rules
- Database migration (V58)

**Frontend (10+ dosya)**
- Type definitions
- React Query hooks
- Next.js API routes
- Sayfa güncellemeleri
- Component entegrasyonları

### Dokümantasyon

Detaylı teknik belge: `docs/features/EDUCATION_VIDEOS.md`

---

## [2026-01-19] - Historical Settlement Sync (KRİTİK ÖZELLIK)

### Keşif: Trendyol API Limiti Aşıldı!

**Problem**: Trendyol Orders API sadece son 90 gün veri veriyor. Yeni kullanıcılar SellerX'e abone olduğunda mağazanın açılışından bu yana TÜM verileri görmek istiyor, ama biz sadece son 3 ay verebiliyorduk.

**Çözüm**: Settlements API'nin sipariş seviyesinde veri içerdiği keşfedildi ve 2+ yıl geriye gidebildiği doğrulandı!

### Yeni Özellikler

#### Backend - Historical Settlement Sync
- **`TrendyolHistoricalSettlementService.java`**: 2 yıl geriye gidip tarihi siparişleri çeken servis
- **`HistoricalSyncResult.java`**: Sync sonuç DTO'su
- **`V52__add_data_source_to_orders.sql`**: `data_source` kolonu eklendi (ORDER_API/SETTLEMENT_API)
- **`StoreOnboardingService.java`**: SYNCING_HISTORICAL aşaması eklendi

#### Frontend - Sync Status UI
- **`types/store.ts`**: 3 yeni sync durumu eklendi
- **`sync-status-display.tsx`**: 6 aşamalı onboarding UI

### Yeni Onboarding Akışı

```
1. SYNCING_PRODUCTS           → Ürünler
2. SYNCING_ORDERS             → Son 90 gün (Orders API)
3. SYNCING_HISTORICAL         → 90+ gün öncesi (Settlements API) ← YENİ!
4. SYNCING_FINANCIAL          → Finansal veriler
5. RECALCULATING_COMMISSIONS  → Komisyon hesaplama ← YENİ!
6. SYNCING_QA                 → Soru-cevaplar ← YENİ!
7. COMPLETED
```

### K-Pure Doğrulama Testi

| Metrik | Değer |
|--------|-------|
| İlk Sipariş No | 9966349644 |
| İlk Sipariş Tarihi | 31 Ocak 2025, 18:26:51 |
| Orders API Verileri | 19 Ekim 2025 - 19 Ocak 2026 (12,430 sipariş) |
| Kayıp Tarihi Veriler | 31 Ocak - 19 Ekim 2025 (~10,000 sipariş) |

### Kritik Düzeltme

```java
// YANLIŞ: SellerX'e eklenme tarihi (yeni!)
LocalDateTime syncFrom = store.getCreatedAt();

// DOĞRU: 2 yıl geriye git
LocalDateTime syncFrom = LocalDateTime.now().minusMonths(24);
```

### API Kısıtlamaları (ASLA UNUTMA!)

- `transactionType` parametresi **ZORUNLU** (Sale/Return/Discount/Coupon)
- `size` parametresi **SADECE 500 veya 1000** kabul ediyor
- Tarih aralığı **maksimum 15 gün** olmalı (biz 14 kullanıyoruz)
- Settlement komisyon oranları **GERÇEK**, Orders API'deki **TAHMİNİ**

### Dosyalar

| Dosya | İşlem |
|-------|-------|
| `TrendyolHistoricalSettlementService.java` | YENİ (589 satır) |
| `HistoricalSyncResult.java` | YENİ |
| `V52__add_data_source_to_orders.sql` | YENİ |
| `TrendyolOrder.java` | `dataSource` alanı eklendi |
| `Store.java` | `historicalSyncStatus/Date` eklendi |
| `StoreOnboardingService.java` | SYNCING_HISTORICAL aşaması |
| `types/store.ts` | 3 yeni sync durumu |
| `sync-status-display.tsx` | 6 aşamalı UI |

### Dokümantasyon

Detaylı teknik belge: `docs/architecture/HISTORICAL_SETTLEMENT_SYNC.md`

---

## [2026-01-15] - Webhook Aktivasyonu ve Bug Fix

### Düzeltilen Hatalar

#### Backend - Webhook Security Rules
- **Problem**: Webhook endpoint'i (`/api/webhook/trendyol/{sellerId}`) 401 Unauthorized hatası veriyordu
- **Sebep**: Spring Security tüm endpoint'leri koruma altına alıyordu, webhook public olmalıydı
- **Çözüm**: `WebhookSecurityRules.java` eklendi
  ```java
  @Component
  public class WebhookSecurityRules implements SecurityRules {
      @Override
      public void configure(...) {
          auth
              .requestMatchers(HttpMethod.POST, "/api/webhook/trendyol/**").permitAll()
              .requestMatchers(HttpMethod.GET, "/api/webhook/health").permitAll();
      }
  }
  ```

#### Frontend - Dynamic Route Çakışması
- **Problem**: Next.js başlamıyordu - `'id' !== 'storeId'` hatası
- **Sebep**: Aynı path'te farklı parametre isimleri (`[id]` vs `[storeId]`)
- **Çözüm**:
  - `app/api/stores/[storeId]/` → `app/api/stores/[id]/` altına taşındı
  - `app/api/products/[productId]/` silindi (duplicate)
  - `app/api/products/store/[storeId]/` silindi (duplicate)
  - Webhook route dosyalarında `params.storeId` → `params.id` güncellendi

#### Backend - JWT Secret Hatası
- **Problem**: Login 500 hatası - "key byte array is 104 bits which is not secure enough"
- **Sebep**: JWT_SECRET environment variable eksik veya çok kısa
- **Çözüm**: En az 256-bit (32 byte) JWT secret gerekli
  ```bash
  JWT_SECRET="c2VsbGVyeC1zZWN1cmUtand0LXNlY3JldC1rZXktMjAyNi..." ./mvnw spring-boot:run
  ```

### Test Sonuçları

Webhook sistemi başarıyla test edildi:
- ✅ Sipariş alma: `POST /api/webhook/trendyol/1080066` → 200 OK
- ✅ Event logging: `webhook_events` tablosuna kayıt
- ✅ Duplicate detection: Aynı sipariş tekrar geldiğinde DUPLICATE olarak işaretleniyor
- ✅ İşlem süresi: <100ms

### Dosya Değişiklikleri

| Dosya | İşlem |
|-------|-------|
| `webhook/WebhookSecurityRules.java` | Eklendi |
| `app/api/stores/[id]/webhooks/*` | Taşındı ([storeId] → [id]) |
| `app/api/stores/[storeId]/` | Silindi |
| `app/api/products/[productId]/` | Silindi |
| `app/api/products/store/[storeId]/` | Silindi |

---

## [2026-01-15] - Webhook Implementasyonu

### Eklenen Özellikler

#### Backend (Spring Boot)

**Veritabanı**
- `V20__create_webhook_events_table.sql` - Webhook olayları için yeni tablo
  - Idempotency kontrolü için `event_id` unique index
  - Store bazlı sorgular için `store_id, created_at` composite index
  - İşlem durumu tracking (RECEIVED, PROCESSING, COMPLETED, FAILED, DUPLICATE)

**Entity ve Repository**
- `WebhookEvent.java` - JPA entity
  - ProcessingStatus enum ile durum yönetimi
  - Payload, error_message, processing_time_ms alanları
- `WebhookEventRepository.java` - Repository interface
  - `existsByEventId()` - Hızlı idempotency kontrolü
  - `findByStoreIdOrderByCreatedAtDesc()` - Sayfalı olay listesi
  - `countByStoreIdGroupByStatus()` - Dashboard istatistikleri

**Güvenlik Servisi**
- `WebhookSignatureValidator.java`
  - HMAC-SHA256 ile signature doğrulama
  - Timing-safe karşılaştırma (MessageDigest.isEqual)
  - SHA-256 tabanlı unique event ID oluşturma
  - Konfigürasyon: `app.webhook.signature-validation-enabled`

- `WebhookSecurityRules.java` - Public endpoint güvenlik kuralları
  - `/api/webhook/trendyol/**` - Trendyol webhook receiver (public)
  - `/api/webhook/health` - Webhook health check (public)

**Controller Güncellemeleri**
- `TrendyolWebhookController.java` - Güvenlik iyileştirmeleri
  - Raw payload capture (signature doğrulama için)
  - X-Trendyol-Signature header kontrolü
  - Idempotency kontrolü (duplicate önleme)
  - Tüm olayların webhook_events tablosuna kaydı
  - 5 saniye içinde 200 OK dönme garantisi

**Yönetim API'si**
- `WebhookManagementController.java` - Yeni controller
  - `GET /api/stores/{storeId}/webhooks/status` - Webhook durumu
  - `POST /api/stores/{storeId}/webhooks/enable` - Trendyol'a kayıt
  - `POST /api/stores/{storeId}/webhooks/disable` - Trendyol'dan silme
  - `GET /api/stores/{storeId}/webhooks/events` - Olay geçmişi (sayfalı)
  - `POST /api/stores/{storeId}/webhooks/test` - Test olayı oluşturma

**Konfigürasyon**
- `application.yaml` güncellemesi
  ```yaml
  app:
    webhook:
      enabled: true
      signature-secret: ${WEBHOOK_SIGNATURE_SECRET:}
      signature-validation-enabled: false # Dev için kapalı
  ```

#### Frontend (Next.js/React)

**API Routes**
- `app/api/stores/[storeId]/webhooks/status/route.ts`
- `app/api/stores/[storeId]/webhooks/enable/route.ts`
- `app/api/stores/[storeId]/webhooks/disable/route.ts`
- `app/api/stores/[storeId]/webhooks/events/route.ts`
- `app/api/stores/[storeId]/webhooks/test/route.ts`

**API Client**
- `lib/api/client.ts` - webhookApi eklendi
  - TypeScript interfaces: WebhookStatus, WebhookEvent, WebhookEventsResponse
  - 5 metot: getStatus, enable, disable, getEvents, test

**React Query Hooks**
- `hooks/queries/use-webhooks.ts`
  - `useWebhookStatus(storeId)` - Webhook durumu
  - `useWebhookEvents(storeId, page, size, eventType)` - Olay listesi
  - `useEnableWebhooks()` - Etkinleştirme mutation
  - `useDisableWebhooks()` - Devre dışı bırakma mutation
  - `useTestWebhook()` - Test mutation

**UI Komponenti**
- `components/settings/webhook-settings.tsx`
  - Durum kartı (aktif/pasif gösterimi)
  - İstatistik kartları (toplam, başarılı, başarısız, tekrar)
  - Webhook URL gösterimi ve kopyalama
  - Enable/Disable butonları
  - Test butonu
  - Olay geçmişi tablosu (pagination ile)
  - Bilgilendirme kartı

**Settings Sayfası**
- `app/[locale]/(app-shell)/settings/page.tsx`
  - Yeni "Webhooks" tab'ı eklendi (6. tab)
  - WebhookSettings komponenti entegre edildi

### Teknik Detaylar

**Güvenlik Önlemleri**
- HMAC-SHA256 signature doğrulama
- Timing-safe string karşılaştırma
- Idempotency key ile duplicate önleme
- Store ownership kontrolü (AccessDeniedException)

**Performans**
- Webhook'lar 5 saniye içinde 200 OK dönmeli (Trendyol requirement)
- Duplicate kontrolü için indexed event_id
- Sayfalı sorgular ile bellek optimizasyonu

**Hata Yönetimi**
- İşleme hatalarında bile 200 OK dönüş (retry önleme)
- Detaylı error_message kaydı
- processing_time_ms ile performans izleme

---

## [2026-01-14] - Analytics ve Profit Sayfaları

### Eklenen Özellikler
- Analytics sayfası (satış analizi, grafikler)
- Profit sayfası (kar hesaplama)
- Categories API endpoint
- Orders API güncellemeleri
- Stock-info API endpoint

---

## [2026-01-13] - Proje Analiz Raporu

### Dokümantasyon
- PROJE_ANALIZ_RAPORU.md oluşturuldu
- Mevcut sistem durumu analizi
- Teknik borç tanımlaması

---

## [Initial Commit] - Proje Kurulumu

### Temel Yapı
- Spring Boot 3.4.4 backend
- Next.js 15 frontend
- PostgreSQL 15 veritabanı
- JWT authentication
- Trendyol API entegrasyonu
