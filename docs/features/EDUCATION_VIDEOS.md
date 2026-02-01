# Eğitim Videoları Sistemi

Bu dokümantasyon, SellerX platformunda eğitim videoları yönetimi, izlenme takibi ve bildirim sistemi için oluşturulan backend ve frontend altyapısını açıklamaktadır.

**Son Güncelleme**: 2026-01-21  
**Versiyon**: 1.0

---

## İçindekiler

1. [Genel Bakış](#1-genel-bakış)
2. [Backend Yapısı](#2-backend-yapısı)
3. [Frontend Yapısı](#3-frontend-yapısı)
4. [API Referansı](#4-api-referansı)
5. [Veritabanı Şeması](#5-veritabanı-şeması)
6. [Bildirim Sistemi](#6-bildirim-sistemi)
7. [Geliştirici Notları](#7-geliştirici-notları)

---

## 1. Genel Bakış

Eğitim videoları sistemi, platform kullanıcılarının eğitim içeriklerine erişmesini, videoları izlemesini ve izlenme durumunu takip etmesini sağlar. Admin kullanıcıları videoları yönetebilir ve yeni video eklendiğinde tüm kullanıcılara otomatik bildirim gönderilir.

### Özellikler

- **Video Yönetimi**: YouTube URL veya dosya upload desteği
- **İzlenme Takibi**: Kullanıcı bazında izlenme durumu
- **Kategori Sistemi**: Videolar kategorilere ayrılır (Başlangıç, Ürünler, Siparişler, vb.)
- **Bildirim Sistemi**: Yeni video eklendiğinde otomatik bildirim
- **Admin Panel**: Video CRUD işlemleri (UI sonra eklenecek)

---

## 2. Backend Yapısı

### 2.1 Entity'ler

#### EducationVideo
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/education/EducationVideo.java`

```java
- id: UUID (Primary Key)
- title: String (Video başlığı)
- description: String (Video açıklaması)
- category: VideoCategory (Enum)
- duration: String (Format: "5:30")
- videoUrl: String (YouTube embed URL veya dosya path)
- thumbnailUrl: String (Thumbnail görsel URL)
- videoType: VideoType (Enum: YOUTUBE, UPLOADED)
- order: Integer (Sıralama)
- isActive: Boolean (Aktif/Pasif)
- createdBy: User (Admin kullanıcı)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

**Index'ler:**
- `idx_education_video_category`: Kategori filtreleme için
- `idx_education_video_active_order`: Aktif videoları order'a göre sıralama için

#### VideoWatchHistory
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/education/VideoWatchHistory.java`

```java
- id: UUID (Primary Key)
- user: User (Foreign Key)
- video: EducationVideo (Foreign Key)
- watchedAt: LocalDateTime (İzlenme zamanı)
- watchedDuration: Integer (Saniye, ileride progress tracking için)
```

**Unique Constraint**: (userId, videoId) - Bir kullanıcı bir videoyu sadece bir kez izleyebilir

#### UserNotification
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/notifications/UserNotification.java`

```java
- id: UUID (Primary Key)
- user: User (Foreign Key)
- type: NotificationType (Enum: VIDEO_ADDED, ORDER_UPDATE, vb.)
- title: String
- message: String
- link: String (Optional, video URL için)
- read: Boolean (Okundu mu?)
- createdAt: LocalDateTime
```

**Index**: `idx_notification_user_read_created` - Kullanıcı bildirimlerini okunmamış/yeni sıralı getirmek için

### 2.2 Repository'ler

#### EducationVideoRepository
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/education/EducationVideoRepository.java`

```java
findByIsActiveTrueOrderByOrderAsc() - Tüm aktif videolar (sıralı)
findByCategoryAndIsActiveTrueOrderByOrderAsc(VideoCategory) - Kategoriye göre aktif videolar
findById(UUID) - Video detayı
findAllByOrderByOrderAsc() - Admin için tüm videolar
```

#### VideoWatchHistoryRepository
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/education/VideoWatchHistoryRepository.java`

```java
findByUser(User) - Kullanıcının izlediği tüm videolar
findByVideo(EducationVideo) - Video için tüm izlenme kayıtları
existsByUserAndVideo(User, EducationVideo) - İzlenme kontrolü
findByUserAndVideo(User, EducationVideo) - Belirli izlenme kaydı
findWatchedVideoIdsByUser(User) - İzlenen video ID'lerini getir
```

#### UserNotificationRepository
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/notifications/UserNotificationRepository.java`

```java
findByUserOrderByCreatedAtDesc(User) - Kullanıcı bildirimleri (yeni önce)
findByUserAndReadFalseOrderByCreatedAtDesc(User) - Okunmamış bildirimler
countByUserAndReadFalse(User) - Okunmamış sayısı
markAsRead(UUID, User) - Okundu işaretle
markAllAsRead(User) - Tümünü okundu işaretle
```

### 2.3 Service'ler

#### EducationVideoService
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/education/EducationVideoService.java`

**Metodlar:**
- `getAllVideos()` - Tüm aktif videoları getir (order'a göre sıralı)
- `getVideosByCategory(VideoCategory)` - Kategoriye göre filtreleme
- `getVideoById(UUID)` - Video detayı
- `createVideo(CreateVideoRequest, Long)` - Yeni video ekle (Admin), yeni video eklendiğinde otomatik bildirim oluşturur
- `updateVideo(UUID, UpdateVideoRequest)` - Video güncelle (Admin)
- `deleteVideo(UUID)` - Video sil (Soft delete - isActive=false)
- `markAsWatched(UUID, Long)` - Video izlendi olarak işaretle
- `getUserWatchStatus(Long)` - Kullanıcının izlenme durumu

#### NotificationService
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/notifications/NotificationService.java`

**Metodlar:**
- `createNotification(Long, NotificationType, String, String, String)` - Yeni bildirim oluştur
- `createVideoAddedNotification(EducationVideo)` - Yeni video eklendiğinde tüm kullanıcılara bildirim gönder
- `getUserNotifications(Long)` - Kullanıcı bildirimleri
- `markAsRead(UUID, Long)` - Bildirimi okundu işaretle
- `getUnreadCount(Long)` - Okunmamış bildirim sayısı

### 2.4 Controller'lar

#### EducationVideoController
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/education/EducationVideoController.java`

**Base Path**: `/api/education/videos`

**Endpoint'ler:**

| Method | Endpoint | Auth | Açıklama |
|--------|----------|------|----------|
| GET | `/api/education/videos` | Public | Tüm aktif videolar |
| GET | `/api/education/videos/{id}` | Public | Video detayı |
| GET | `/api/education/videos/category/{category}` | Public | Kategoriye göre videolar |
| GET | `/api/education/videos/my-watch-status` | Authenticated | Kullanıcının izlenme durumu |
| POST | `/api/education/videos/{id}/watch` | Authenticated | Video izlendi |
| POST | `/api/education/videos` | Admin | Yeni video ekle |
| PUT | `/api/education/videos/{id}` | Admin | Video güncelle |
| DELETE | `/api/education/videos/{id}` | Admin | Video sil |

#### NotificationController
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/notifications/NotificationController.java`

**Base Path**: `/api/notifications`

**Endpoint'ler:**

| Method | Endpoint | Auth | Açıklama |
|--------|----------|------|----------|
| GET | `/api/notifications` | Authenticated | Kullanıcı bildirimleri |
| GET | `/api/notifications/unread-count` | Authenticated | Okunmamış bildirim sayısı |
| PUT | `/api/notifications/{id}/read` | Authenticated | Bildirimi okundu işaretle |

### 2.5 Security Rules

#### EducationSecurityRules
**Dosya**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/education/EducationSecurityRules.java`

- Video listesi ve detay endpoint'leri public (authentication gerekmez)
- İzlenme takibi endpoint'leri authenticated kullanıcılar için
- CRUD işlemleri sadece ADMIN rolü için (`@PreAuthorize("hasRole('ADMIN')")`)

---

## 3. Frontend Yapısı

### 3.1 Type Definitions

#### Education Types
**Dosya**: `sellerx-frontend/types/education.ts`

```typescript
type VideoCategory = 'GETTING_STARTED' | 'PRODUCTS' | 'ORDERS' | 'ANALYTICS' | 'SETTINGS'
type VideoType = 'YOUTUBE' | 'UPLOADED'

interface EducationVideo {
  id: string
  title: string
  description: string
  category: VideoCategory
  duration: string
  thumbnailUrl?: string
  videoUrl: string
  videoType: VideoType
  order: number
  isActive: boolean
  createdAt: string
  updatedAt: string
  watched?: boolean // Frontend'de hesaplanır
}

interface VideoWatchStatus {
  watchedVideoIds: string[]
}
```

#### Notification Types
**Dosya**: `sellerx-frontend/types/notification.ts`

```typescript
type NotificationType = 'VIDEO_ADDED' | 'ORDER_UPDATE' | 'STOCK_ALERT' | 'SYSTEM' | 'SUCCESS' | 'WARNING'

interface Notification {
  id: string
  type: NotificationType
  title: string
  message: string
  link?: string
  read: boolean
  createdAt: string
}
```

### 3.2 React Query Hooks

#### use-education.ts
**Dosya**: `sellerx-frontend/hooks/queries/use-education.ts`

**Hook'lar:**
- `useEducationVideos()` - Tüm videolar
- `useEducationVideo(id)` - Video detayı
- `useVideosByCategory(category)` - Kategoriye göre
- `useMyWatchStatus()` - İzlenme durumu
- `useMarkVideoAsWatched()` - İzlenme işaretleme mutation
- `useCreateVideo()` - Admin: video ekleme
- `useUpdateVideo()` - Admin: video güncelleme
- `useDeleteVideo()` - Admin: video silme

#### use-notifications.ts
**Dosya**: `sellerx-frontend/hooks/queries/use-notifications.ts`

**Hook'lar:**
- `useNotifications()` - Bildirimler
- `useUnreadCount()` - Okunmamış sayısı
- `useMarkAsRead()` - Okundu işaretleme

### 3.3 Next.js API Routes (BFF Pattern)

Tüm API route'ları backend'e proxy yaparak çalışır.

**Route'lar:**
- `app/api/education/videos/route.ts` - GET (all), POST (create)
- `app/api/education/videos/[id]/route.ts` - GET, PUT, DELETE
- `app/api/education/videos/[id]/watch/route.ts` - POST (mark as watched)
- `app/api/education/videos/my-watch-status/route.ts` - GET (watch status)
- `app/api/notifications/route.ts` - GET (notifications)
- `app/api/notifications/unread-count/route.ts` - GET (unread count)
- `app/api/notifications/[id]/read/route.ts` - PUT (mark as read)

### 3.4 Sayfalar

#### Education Page
**Dosya**: `sellerx-frontend/app/[locale]/(app-shell)/education/page.tsx`

- Mock data yerine API'den veri çeker
- Kategori filtreleme
- İzlenme durumu gösterimi
- Video oynatma modal'ı
- URL parametresi ile video açma (`?video={id}`)

#### Admin Panel
**Dosya**: `sellerx-frontend/app/[locale]/(app-shell)/admin/education/page.tsx`

- Şu an boş sayfa (UI sonra eklenecek)
- Admin kontrolü gerekli

### 3.5 Header Entegrasyonu

**Dosya**: `sellerx-frontend/components/layout/header.tsx`

- Eğitim videoları dropdown menüsü
- Son 3 video gösterimi
- İzlenmemiş video sayısı badge
- API'den veri çekme

---

## 4. API Referansı

### 4.1 Education Videos API

#### GET /api/education/videos
Tüm aktif videoları getirir.

**Response:**
```json
[
  {
    "id": "uuid",
    "title": "SellerX'e Hoş Geldiniz",
    "description": "...",
    "category": "GETTING_STARTED",
    "duration": "5:30",
    "videoUrl": "https://www.youtube.com/embed/...",
    "thumbnailUrl": "https://...",
    "videoType": "YOUTUBE",
    "order": 1,
    "isActive": true,
    "createdAt": "2026-01-21T...",
    "updatedAt": "2026-01-21T..."
  }
]
```

#### POST /api/education/videos
Yeni video ekle (Admin only).

**Request:**
```json
{
  "title": "Yeni Video",
  "description": "...",
  "category": "GETTING_STARTED",
  "duration": "5:30",
  "videoUrl": "https://www.youtube.com/embed/...",
  "thumbnailUrl": "https://...",
  "videoType": "YOUTUBE",
  "order": 1,
  "isActive": true
}
```

**Response:** EducationVideoDto

#### POST /api/education/videos/{id}/watch
Video izlendi olarak işaretle (Authenticated).

**Response:** 200 OK

#### GET /api/education/videos/my-watch-status
Kullanıcının izlenme durumu (Authenticated).

**Response:**
```json
{
  "watchedVideoIds": ["uuid1", "uuid2"]
}
```

### 4.2 Notifications API

#### GET /api/notifications
Kullanıcı bildirimlerini getir (Authenticated).

**Response:**
```json
[
  {
    "id": "uuid",
    "type": "VIDEO_ADDED",
    "title": "Yeni Eğitim Videosu: ...",
    "message": "...",
    "link": "/education?video=uuid",
    "read": false,
    "createdAt": "2026-01-21T..."
  }
]
```

#### GET /api/notifications/unread-count
Okunmamış bildirim sayısı (Authenticated).

**Response:**
```json
5
```

---

## 5. Veritabanı Şeması

### Migration Dosyası
**Dosya**: `sellerx-backend/src/main/resources/db/migration/V58__create_education_videos_tables.sql`

### Tablolar

#### education_videos
```sql
CREATE TABLE education_videos (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    duration VARCHAR(20) NOT NULL,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    video_type VARCHAR(20) NOT NULL,
    video_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

**Index'ler:**
- `idx_education_video_category` - Category filtreleme
- `idx_education_video_active_order` - Aktif videoları sıralama

#### video_watch_history
```sql
CREATE TABLE video_watch_history (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    video_id UUID NOT NULL REFERENCES education_videos(id) ON DELETE CASCADE,
    watched_at TIMESTAMP NOT NULL DEFAULT NOW(),
    watched_duration INTEGER,
    UNIQUE(user_id, video_id)
);
```

**Index'ler:**
- `idx_video_watch_user` - User bazlı sorgular
- `idx_video_watch_video` - Video bazlı sorgular

#### user_notifications
```sql
CREATE TABLE user_notifications (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link TEXT,
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

**Index:**
- `idx_notification_user_read_created` - Kullanıcı bildirimleri sorguları

---

## 6. Bildirim Sistemi

### Yeni Video Eklendiğinde

1. Admin yeni video oluşturur (`POST /api/education/videos`)
2. `EducationVideoService.createVideo()` çağrılır
3. Video veritabanına kaydedilir
4. `NotificationService.createVideoAddedNotification()` çağrılır
5. Tüm aktif kullanıcılar için `VIDEO_ADDED` tipinde bildirim oluşturulur
6. Frontend header'da badge gösterilir
7. Kullanıcı bildirime tıklayınca `/education?video={id}` sayfasına yönlendirilir

### Bildirim İçeriği

```java
type: VIDEO_ADDED
title: "Yeni Eğitim Videosu: {video.title}"
message: {video.description} veya varsayılan mesaj
link: "/education?video={video.id}"
```

---

## 7. Geliştirici Notları

### Video Upload

Şu anda video upload altyapısı hazır ancak implementation yapılmadı:
- `videoType` enum'ı `UPLOADED` değerini destekliyor
- `videoUrl` field'ı hem YouTube hem dosya path için kullanılabilir
- File upload endpoint'i eklenmemiş (ileride eklenecek)

### Admin Panel

Admin panel UI'ı boş bırakıldı, sonra doldurulacak:
- Sayfa: `/admin/education`
- CRUD işlemleri için hook'lar hazır
- Sadece admin role kontrolü gerekli

### İzlenme Takibi

Şu anda boolean (watched/not watched) olarak çalışıyor:
- `VideoWatchHistory` entity'sinde `watchedDuration` field'ı var (ileride progress tracking için)
- Video oynatıldığında `markAsWatched()` çağrılıyor
- Frontend'de izlenme durumu badge olarak gösteriliyor

### Mock Data'dan Migration

Eski mock data (`lib/mock/education-videos.ts`) hala mevcut ancak kullanılmıyor:
- Frontend artık API'den veri çekiyor
- Mock data silinebilir veya test için tutulabilir

### Enum Değerleri

Backend enum'ları büyük harf:
- `GETTING_STARTED`, `PRODUCTS`, `ORDERS`, `ANALYTICS`, `SETTINGS`
- Frontend'de de aynı format kullanılıyor

### Security

- Video listesi public (authentication gerekmez)
- İzlenme takibi authenticated kullanıcılar için
- CRUD işlemleri sadece ADMIN için
- Notification endpoint'leri authenticated kullanıcılar için

---

## 8. Gelecek Geliştirmeler

1. **Video Upload**: Dosya upload endpoint'i ve storage entegrasyonu
2. **Progress Tracking**: Video izlenme ilerlemesi (izlenen süre/yüzde)
3. **Video Önerileri**: Kullanıcıya özel video önerileri
4. **Video Arama**: Arama ve filtreleme özellikleri
5. **İstatistikler**: Video izlenme istatistikleri (admin için)
6. **Email/Push Bildirimleri**: Bildirim sistemine email ve push notification desteği

---

## 9. Test

### Test Endpoint'leri

```bash
# Tüm videoları getir (public)
curl http://localhost:8080/api/education/videos

# Video izlenme durumu (authenticated)
curl -H "Authorization: Bearer {token}" \
  http://localhost:8080/api/education/videos/my-watch-status

# Yeni video ekle (admin)
curl -X POST -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{...}' \
  http://localhost:8080/api/education/videos
```

---

## 10. Dosya Listesi

### Backend Dosyaları

```
sellerx-backend/src/main/java/com/ecommerce/sellerx/
├── education/
│   ├── EducationVideo.java
│   ├── EducationVideoRepository.java
│   ├── EducationVideoService.java
│   ├── EducationVideoController.java
│   ├── EducationVideoDto.java
│   ├── CreateVideoRequest.java
│   ├── UpdateVideoRequest.java
│   ├── VideoWatchHistory.java
│   ├── VideoWatchHistoryRepository.java
│   ├── VideoWatchStatusDto.java
│   ├── VideoCategory.java (enum)
│   ├── VideoType.java (enum)
│   └── EducationSecurityRules.java
└── notifications/
    ├── UserNotification.java
    ├── UserNotificationRepository.java
    ├── NotificationService.java
    ├── NotificationController.java
    ├── NotificationDto.java
    └── NotificationType.java (enum)

sellerx-backend/src/main/resources/db/migration/
└── V58__create_education_videos_tables.sql
```

### Frontend Dosyaları

```
sellerx-frontend/
├── types/
│   ├── education.ts
│   └── notification.ts
├── hooks/queries/
│   ├── use-education.ts
│   └── use-notifications.ts
├── app/api/
│   ├── education/videos/
│   │   ├── route.ts
│   │   ├── [id]/
│   │   │   ├── route.ts
│   │   │   └── watch/route.ts
│   │   └── my-watch-status/route.ts
│   └── notifications/
│       ├── route.ts
│       ├── unread-count/route.ts
│       └── [id]/read/route.ts
├── app/[locale]/(app-shell)/
│   ├── education/page.tsx (güncellendi)
│   └── admin/education/page.tsx (yeni)
└── components/layout/
    └── header.tsx (güncellendi)
```

---

**Not**: Bu dokümantasyon, eğitim videoları sisteminin ilk versiyonunu (v1.0) açıklamaktadır. Sistem geliştikçe bu dokümantasyon güncellenecektir.
