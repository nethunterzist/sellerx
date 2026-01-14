# SellerX Proje Analiz Raporu

## 1. PROJE GENEL BAKIŞ

**SellerX**, Trendyol gibi e-ticaret platformları için geliştirilmiş bir **mağaza yönetim ve analiz platformu**dur. Proje, satıcıların ürünlerini, siparişlerini, finansal verilerini ve giderlerini yönetmelerini sağlar.

### Proje Yapısı
- **Backend**: Spring Boot (Java 21) - REST API
- **Frontend**: Next.js 15 (React 19, TypeScript) - Modern web uygulaması
- **Veritabanı**: PostgreSQL 15
- **Containerization**: Docker & Docker Compose
- **Deployment**: Vercel (Frontend), Railway (Backend)

---

## 2. BACKEND YAPISI (Spring Boot)

### 2.1 Ana Uygulama
- **Entry Point**: `StoreApplication.java`
- **Timezone**: Europe/Istanbul (Türkiye saati)
- **Scheduling**: Aktif (periyodik görevler için)

### 2.2 Modüller ve Endpoint'ler

#### 🔐 Auth Modülü (`/auth`)
- `POST /auth/login` - Kullanıcı girişi (JWT token döner)
- `POST /auth/logout` - Çıkış yapma
- `POST /auth/refresh` - Token yenileme
- `GET /auth/me` - Kullanıcı bilgilerini getirme

**Güvenlik:**
- JWT tabanlı kimlik doğrulama
- BCrypt şifre hashleme
- Refresh token cookie'de saklanıyor
- Access token 1 saat geçerli
- Refresh token 7 gün geçerli

#### 👥 Users Modülü (`/users`)
- `GET /users` - Tüm kullanıcıları listele
- `GET /users/{id}` - Kullanıcı detayı
- `POST /users` - Yeni kullanıcı kaydı (public)
- `PUT /users/{id}` - Kullanıcı güncelle
- `DELETE /users/{id}` - Kullanıcı sil
- `POST /users/{id}/change-password` - Şifre değiştir
- `GET /users/selected-store` - Seçili mağazayı getir
- `POST /users/selected-store` - Mağaza seç

**Özellikler:**
- Kullanıcılar birden fazla mağazaya sahip olabilir
- Seçili mağaza sistemi var (selected_store_id)
- Role-based access control (Role enum)

#### 🏪 Stores Modülü (`/stores`)
- `GET /stores/my` - Kullanıcının mağazaları
- `GET /stores` - Tüm mağazalar (admin)
- `GET /stores/{id}` - Mağaza detayı
- `POST /stores` - Yeni mağaza oluştur
- `PUT /stores/{id}` - Mağaza güncelle
- `DELETE /stores/{id}` - Mağaza sil
- `GET /stores/test-connection` - Trendyol bağlantı testi

**Özellikler:**
- Her mağaza bir marketplace'e bağlı (şu an sadece Trendyol)
- Credentials JSONB formatında saklanıyor
- Webhook ID desteği var
- İlk mağaza oluşturulduğunda otomatik seçili hale geliyor

#### 🛍️ Products Modülü (`/products`)
- `POST /products/sync/{storeId}` - Trendyol'dan ürünleri senkronize et
- `GET /products/store/{storeId}` - Mağazanın ürünlerini getir
- `GET /products/store/{storeId}/all` - Tüm ürünler (sayfalama ile)
- `PUT /products/{productId}/cost-and-stock` - Maliyet ve stok güncelle
- `POST /products/{productId}/stock-info` - Stok bilgisi ekle
- `PUT /products/{productId}/stock-info/{stockDate}` - Stok bilgisi güncelle
- `DELETE /products/{productId}/stock-info/{stockDate}` - Stok bilgisi sil

**Özellikler:**
- Trendyol API'den ürün çekme
- Maliyet ve stok geçmişi JSONB formatında
- Barkod, kategori, marka bilgileri
- KDV oranı ve satış fiyatı takibi

#### 📦 Orders Modülü (`/api/orders`)
- `POST /api/orders/stores/{storeId}/sync` - Siparişleri senkronize et
- `POST /api/orders/sync-all` - Tüm mağazalar için senkronizasyon
- `GET /api/orders/stores/{storeId}` - Siparişleri listele (sayfalama)
- `GET /api/orders/stores/{storeId}/by-date-range` - Tarih aralığına göre
- `GET /api/orders/stores/{storeId}/by-status` - Statüye göre
- `GET /api/orders/stores/{storeId}/statistics` - Sipariş istatistikleri

**Sipariş Statüleri:**
- Delivered (Teslim Edildi)
- Returned (İade Edildi)
- UnDeliveredAndReturned
- UnPacked (Paketlenmedi)
- Shipped (Kargoya Verildi)
- Created (Oluşturuldu)

#### 📊 Dashboard Modülü (`/dashboard`)
- `GET /dashboard/stats` - Dashboard istatistikleri
- `GET /dashboard/stats/{storeId}` - Belirli mağaza için istatistikler

**Dashboard Verileri:**
- Bugün, dün, aya kadar, geçen ay verileri
- Ciro, sipariş sayısı, satılan birim
- İade sayısı ve oranı
- Reklam maliyeti
- Tahmini ödeme tutarı
- Brüt kar, net kar
- ROI (Yatırım Getirisi)
- Kâr marjı
- Detaylı maliyet analizi (kupon, yurt dışı operasyon, ambalaj vb.)

#### 💰 Financial Modülü (`/api/financial`)
- `POST /api/financial/stores/{storeId}/sync` - Finansal verileri senkronize et

#### 💸 Expenses Modülü (`/expenses`)
- `GET /expenses/categories` - Gider kategorileri
- `GET /expenses/store/{storeId}` - Mağaza giderleri
- `POST /expenses/store/{storeId}` - Yeni gider ekle
- `PUT /expenses/store/{storeId}/{expenseId}` - Gider güncelle
- `DELETE /expenses/store/{storeId}/{expenseId}` - Gider sil

**Gider Kategorileri:**
- Frequency enum (Daily, Weekly, Monthly, Yearly, OneTime)

#### 🏷️ Categories Modülü (`/api/categories`)
- `GET /api/categories` - Tüm kategorileri getir
- `POST /api/categories/bulk-insert` - Toplu kategori ekleme

#### 🔔 Webhook Modülü (`/api/webhook`)
- `POST /api/webhook/trendyol/{sellerId}` - Trendyol webhook alıcı
- `GET /api/webhook/health` - Webhook sağlık kontrolü

**Özellikler:**
- Webhook varsayılan olarak kapalı (development için)
- API key ile korumalı

#### 🔧 Trendyol Modülü (`/trendyol`)
- `GET /trendyol/test-connection` - Trendyol API bağlantı testi

#### ❤️ Health Modülü
- `GET /health` - Uygulama sağlık kontrolü
- `GET /` - Ana sayfa

### 2.3 Veritabanı Yapısı (Flyway Migrations)

**Tablolar:**
1. **users** - Kullanıcılar (id, name, email, password, role, selected_store_id)
2. **stores** - Mağazalar (id, user_id, store_name, marketplace, credentials, webhook_id, created_at, updated_at)
3. **trendyol_products** - Ürünler (id, store_id, product_id, barcode, title, category_name, sale_price, vat_rate, quantity, cost_and_stock_info JSONB)
4. **trendyol_orders** - Siparişler (id, store_id, ty_order_number, package_no, order_date, gross_amount, total_discount, order_items JSONB, status)
5. **trendyol_categories** - Kategoriler
6. **expense_categories** - Gider kategorileri
7. **store_expenses** - Mağaza giderleri

**Önemli Özellikler:**
- UUID kullanımı (stores, products, orders)
- JSONB kolonlar (credentials, order_items, cost_and_stock_info)
- Foreign key constraints
- Index'ler performans için
- ON DELETE CASCADE

### 2.4 Güvenlik Yapısı

**SecurityConfig:**
- CORS yapılandırması (localhost:3000, frontend:3000)
- JWT Authentication Filter
- Stateless session yönetimi
- BCrypt password encoder
- Feature-based security rules

**Public Endpoints:**
- `/users` (POST) - Kayıt
- `/auth/login` - Giriş
- `/health` - Sağlık kontrolü
- `/` - Ana sayfa

**Protected Endpoints:**
- Tüm diğer endpoint'ler JWT token gerektirir

---

## 3. FRONTEND YAPISI (Next.js)

### 3.1 Routing Yapısı

**App Router Kullanılıyor:**
```
app/
├── [locale]/              # Çoklu dil desteği (tr, en)
│   ├── (app-shell)/      # Ana uygulama sayfaları
│   │   ├── dashboard/    # Dashboard sayfası
│   │   ├── main-page/    # Ana sayfa
│   │   ├── products/     # Ürünler sayfası
│   │   ├── profile/       # Profil sayfası
│   │   ├── settings/     # Ayarlar sayfası
│   │   └── new-store/    # Yeni mağaza ekleme
│   ├── (auth)/           # Auth sayfaları
│   │   ├── sign-in/      # Giriş sayfası
│   │   └── register/     # Kayıt sayfası
│   ├── layout.tsx        # Root layout
│   └── page.tsx          # Ana sayfa (dashboard'a yönlendirir)
```

### 3.2 Sayfalar ve Özellikleri

#### 🏠 Dashboard (`/dashboard`)
- Bugün, dün, aya kadar, geçen ay istatistikleri
- Ciro, sipariş, kar/zarar grafikleri
- Detaylı maliyet analizi
- SalesDashboardClient component kullanıyor

#### 🛍️ Products (`/products`)
- Ürün listesi tablosu
- Trendyol'dan senkronizasyon butonu
- Ürün detayları ve stok bilgileri
- Maliyet güncelleme

#### 👤 Profile (`/profile`)
- Kullanıcı bilgileri
- Profil güncelleme

#### ⚙️ Settings (`/settings`)
- Uygulama ayarları

#### 🏪 New Store (`/new-store`)
- Yeni mağaza ekleme formu
- Trendyol credentials girişi
- Bağlantı testi

#### 🔐 Sign In (`/sign-in`)
- Email/şifre ile giriş
- JWT token yönetimi
- Callback URL desteği

#### 📝 Register (`/register`)
- Yeni kullanıcı kaydı

### 3.3 Component Yapısı

**UI Components (Shadcn/ui):**
- 28 adet UI component (button, card, dialog, table, sidebar vb.)
- Radix UI tabanlı
- Tailwind CSS ile stillendirilmiş

**Özel Components:**
- `app-sidebar.tsx` - Ana sidebar menü
- `store-switcher.tsx` - Mağaza seçici
- `nav-user.tsx` - Kullanıcı menüsü
- `sales-dashboard/` - Dashboard bileşenleri
- `products-table/` - Ürün tablosu
- `order-detail-dialog/` - Sipariş detay dialogu
- `forms/` - Form bileşenleri

### 3.4 API İletişimi

**API Client (`lib/api/client.ts`):**
- Next.js API routes üzerinden backend'e bağlanıyor
- Otomatik token refresh mekanizması
- 401 hatasında otomatik yenileme
- Cookie-based authentication

**API Routes:**
- `/api/auth/*` - Auth işlemleri
- `/api/stores/*` - Mağaza işlemleri
- `/api/products/*` - Ürün işlemleri
- `/api/dashboard/*` - Dashboard verileri
- `/api/users/*` - Kullanıcı işlemleri

### 3.5 State Management

- **TanStack React Query** - Server state yönetimi
- **SWR** - Veri çekme ve cache
- **React Hook Form** - Form state yönetimi
- **Zod** - Form validasyonu

### 3.6 Internationalization (i18n)

- **next-intl** kullanılıyor
- Desteklenen diller: Türkçe (tr), İngilizce (en)
- Çeviri dosyaları: `messages/tr.json`, `messages/en.json`
- Locale routing: `/[locale]/...`

### 3.7 Middleware

**Authentication Middleware:**
- Public route kontrolü
- JWT token doğrulama
- Otomatik sign-in yönlendirme
- Callback URL desteği

**i18n Middleware:**
- Locale yönlendirme
- Default locale: tr

### 3.8 Styling

- **Tailwind CSS 4.x** - Utility-first CSS
- **next-themes** - Dark/Light mode desteği
- **Shadcn/ui** - Component library
- **Lucide React** - İkonlar

---

## 4. DOCKER YAPISI

### 4.1 Docker Compose (`docker-compose.dev.yml`)

**Servisler:**
1. **postgres** - PostgreSQL 15 Alpine
   - Port: 5432
   - Database: sellerx_db
   - User: postgres / Password: 123123

2. **backend** - Spring Boot
   - Port: 8080
   - Profile: docker
   - Hot reload: Yok (yeniden build gerekir)

3. **frontend** - Next.js
   - Port: 3000
   - Hot reload: Aktif
   - Volume mount: Source code

**Network:**
- sellerx-network (bridge)

**Volumes:**
- postgres_data - Database verileri

### 4.2 Dockerfile'lar

**Backend Dockerfile:**
- Eclipse Temurin JRE 21
- Maven build
- Multi-stage build

**Frontend Dockerfile.dev:**
- Node.js 20 Alpine
- Development mode
- Hot reload desteği

---

## 5. TEKNOLOJİ STACK DETAYLARI

### Backend
- Java 21
- Spring Boot 3.4.4
- Spring Security + JWT (jjwt 0.12.6)
- PostgreSQL 15
- Spring Data JPA + Hibernate
- Flyway 10.20.1 (Migration)
- Maven 3.9
- Lombok 1.18.30
- MapStruct 1.6.3
- Spring Dotenv 4.0.0

### Frontend
- Next.js 15.3.2
- React 19.1.0
- TypeScript 5.x
- Tailwind CSS 4.x
- Shadcn/ui (Radix UI)
- TanStack React Query 5.83.0
- SWR 2.3.3
- React Hook Form 7.56.3
- Zod 3.24.4
- next-intl 4.1.0
- next-themes 0.4.6
- date-fns 4.1.0

---

## 6. ÖNEMLİ ÖZELLİKLER

### ✅ Tamamlanmış Özellikler
- Kullanıcı kayıt/giriş sistemi
- JWT authentication
- Çoklu mağaza desteği
- Mağaza seçme sistemi
- Trendyol ürün senkronizasyonu
- Ürün maliyet ve stok yönetimi
- Sipariş senkronizasyonu ve listeleme
- Dashboard istatistikleri
- Gider yönetimi
- Kategori yönetimi
- Çoklu dil desteği (TR/EN)
- Dark/Light mode

### ⚠️ Kısmen Tamamlanmış
- Sipariş detayları (bazı özellikler eksik)
- Finansal modül (backend var, frontend yok)
- Dashboard (bazı grafikler eksik)
- Giderler (frontend kısmen)

### ❌ Eksik Özellikler
- Subscription/Abonelik sistemi
- Webhook frontend entegrasyonu
- Raporlar modülü
- Test coverage (çok düşük)

---

## 7. TEKNİK BORÇ (Technical Debt)

1. **TypeScript strict mode kapalı** - `tsconfig.json`'da `strict: false`
2. **ESLint hataları ignore ediliyor** - Build sırasında
3. **TypeScript hataları ignore ediliyor** - `ignoreBuildErrors: true`
4. **Test coverage çok düşük** - Sadece 3 test dosyası var
5. **API error handling standardizasyonu eksik**
6. **Logging ve monitoring eksik**
7. **Environment variable yönetimi iyileştirilebilir**

---

## 8. GÜVENLİK NOTLARI

### ⚠️ Dikkat Edilmesi Gerekenler
1. **JWT Secret** - Docker compose'da hardcoded (production'da environment variable kullanılmalı)
2. **Database Password** - Development'ta "123123" (production'da güçlü şifre)
3. **CORS** - Şu an localhost'a açık (production'da sınırlandırılmalı)
4. **Webhook** - Varsayılan olarak kapalı (iyi)
5. **Password Policy** - Sadece minimum 6 karakter (güçlendirilebilir)

---

## 9. DEPLOYMENT

### Frontend (Vercel)
- Next.js standalone output
- Environment variables gerekli
- `vercel.json` yapılandırması var

### Backend (Railway)
- Spring Boot JAR
- PostgreSQL database
- Environment variables gerekli

---

## 10. GELİŞTİRME ORTAMI

### Gereksinimler
- Docker Desktop
- Git
- VS Code (önerilen)

### Başlatma
```bash
docker-compose -f docker-compose.dev.yml up --build -d
```

### Erişim
- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- Database: localhost:5432

---

## 11. ÖNERİLER

### Kısa Vadeli
1. TypeScript strict mode'u aç
2. ESLint hatalarını düzelt
3. Test coverage artır
4. API error handling standardize et

### Orta Vadeli
1. Subscription sistemi ekle
2. Raporlar modülü tamamla
3. Webhook frontend entegrasyonu
4. Logging ve monitoring ekle

### Uzun Vadeli
1. Multi-marketplace desteği (Hepsiburada, N11 vb.)
2. Mobile app (React Native)
3. Real-time notifications
4. Advanced analytics

---

## 12. PROJE DURUMU ÖZETİ

**Genel Durum:** 🟡 **Orta Seviye Geliştirme Aşamasında**

- ✅ Temel özellikler çalışıyor
- ⚠️ Bazı modüller eksik
- ❌ Production'a hazır değil (güvenlik ve test eksikleri var)
- 📊 Kod kalitesi: Orta (technical debt var)

**Kullanılabilirlik:** Development ve test için uygun, production için iyileştirme gerekli.
