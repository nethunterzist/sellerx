# SellerX Tech Stack

SellerX tech stack

---

## Backend (Java/Spring Boot)

| Kategori                 | Teknoloji                      | Versiyon       |
| ------------------------ | ------------------------------ | -------------- |
| **Dil**                  | Java                           | 21             |
| **Framework**            | Spring Boot                    | 3.4.4          |
| **Web**                  | Spring Web (REST API)          | -              |
| **Güvenlik**             | Spring Security + JWT (jjwt)   | 0.12.6         |
| **Veritabanı**           | PostgreSQL                     | 15 (Alpine)    |
| **ORM**                  | Spring Data JPA + Hibernate    | -              |
| **Migration**            | Flyway                         | 10.20.1        |
| **Build Tool**           | Maven                          | 3.9            |
| **Code Generation**      | Lombok, MapStruct              | 1.18.30, 1.6.3 |
| **Validation**           | Spring Boot Starter Validation | -              |
| **Config**               | Spring Dotenv                  | 4.0.0          |
| **Hibernate Extensions** | hibernate-types-60             | 2.21.1         |

### Backend Modülleri

```
src/main/java/com/ecommerce/sellerx/
├── auth/           # Kimlik doğrulama
├── users/          # Kullanıcı yönetimi
├── stores/         # Mağaza yönetimi
├── products/       # Ürün yönetimi
├── orders/         # Sipariş yönetimi
├── dashboard/      # Dashboard verileri
├── financial/      # Finansal veriler
├── expenses/       # Gider yönetimi
├── trendyol/       # Trendyol entegrasyonu
├── webhook/        # Webhook işlemleri
├── categories/     # Kategori yönetimi
├── common/         # Ortak utility sınıfları
└── config/         # Konfigürasyon sınıfları
```

---

## Frontend (Next.js/React)

| Kategori             | Teknoloji                  | Versiyon        |
| -------------------- | -------------------------- | --------------- |
| **Dil**              | TypeScript                 | 5.x             |
| **Framework**        | Next.js                    | 15.3.2          |
| **UI Library**       | React                      | 19.1.0          |
| **Styling**          | Tailwind CSS               | 4.x             |
| **UI Components**    | Radix UI (Shadcn/ui)       | -               |
| **State Management** | TanStack React Query + SWR | 5.83.0 / 2.3.3  |
| **Form Handling**    | React Hook Form + Zod      | 7.56.3 / 3.24.4 |
| **i18n**             | next-intl                  | 4.1.0           |
| **Theme**            | next-themes                | 0.4.6           |
| **Icons**            | Lucide React, React Icons  | -               |
| **Date**             | date-fns                   | 4.1.0           |
| **Carousel**         | Embla Carousel             | 8.6.0           |
| **Toast**            | Sonner                     | 2.0.3           |
| **Linting**          | ESLint + Prettier          | 9.x / 3.5.3     |

### Frontend Yapısı

```
sellerx-frontend/
├── app/                    # Next.js App Router
│   ├── [locale]/          # Çoklu dil routing
│   │   ├── (app-shell)/   # Ana uygulama layout
│   │   └── (auth)/        # Auth sayfaları
│   └── api/               # API Routes
├── components/            # React bileşenleri
│   ├── ui/               # Shadcn/ui bileşenleri
│   ├── auth/             # Auth bileşenleri
│   ├── dashboard/        # Dashboard bileşenleri
│   ├── products/         # Ürün bileşenleri
│   └── forms/            # Form bileşenleri
├── hooks/                # Custom React hooks
│   └── queries/          # React Query hooks
├── lib/                  # Utility fonksiyonları
│   ├── api/              # API client
│   ├── auth/             # Auth utilities
│   ├── store/            # State management
│   ├── utils/            # Helper fonksiyonları
│   └── validators/       # Zod schemas
├── i18n/                 # Internationalization config
└── messages/             # Çeviri dosyaları (en.json, tr.json)
```

---

## DevOps & Infrastructure

| Kategori                  | Teknoloji              |
| ------------------------- | ---------------------- |
| **Containerization**      | Docker                 |
| **Orchestration**         | Docker Compose         |
| **Database**              | PostgreSQL 15 Alpine   |
| **Backend Runtime**       | Eclipse Temurin JRE 21 |
| **Frontend Runtime**      | Node.js 20 Alpine      |
| **Deployment (Frontend)** | Vercel                 |
| **Deployment (Backend)**  | Railway                |

## TODO Özeti

| Modül            | Backend       | Frontend      | Durum              |
| ---------------- | ------------- | ------------- | ------------------ |
| **Auth**         | ✅ Tamamlandı | ✅ Tamamlandı | 🟢 Çalışıyor       |
| **Users**        | ✅ Tamamlandı | ✅ Tamamlandı | 🟢 Çalışıyor       |
| **Stores**       | ✅ Tamamlandı | ✅ Tamamlandı | 🟢 Çalışıyor       |
| **Products**     | ✅ Tamamlandı | ✅ Tamamlandı | 🟢 Çalışıyor       |
| **Orders**       | ⚠️ Kısmen     | ⚠️ Kısmen     | 🟡 Devam Ediyor    |
| **Financial**    | ⚠️ Kısmen     | ❌ Başlanmadı | 🟡 Devam Ediyor    |
| **Dashboard**    | ✅ Tamamlandı | ⚠️ Kısmen     | 🟡 Devam Ediyor    |
| **Expenses**     | ⚠️ Kısmen     | ⚠️ Kısmen     | 🟡 Devam Ediyor    |
| **Webhook**      | ⚠️ Başlandı   | ❌ -          | 🟡 Devam Ediyor    |
| **Subscription** | ❌ Yok        | ❌ Yok        | 🔴 Yok             |
| **i18n**         | -             | ⚠️ Kısmen     | 🟡 Eksik Çeviriler |

---

## Technical Debt

- [ ] TypeScript strict mode aktifleştirme (şu an `false`)
- [ ] ESLint hatalarını düzeltme (build sırasında ignore ediliyor)
- [ ] Test coverage artırma
- [ ] API error handling standardizasyonu
- [ ] Logging ve monitoring ekleme
- [ ] Environment variable yönetimini iyileştirme
