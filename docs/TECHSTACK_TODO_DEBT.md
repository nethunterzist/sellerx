# SellerX Tech Stack, TODO ve Teknik Borç

Güncel teknoloji listesi, modül durumu ve teknik borç. Detaylı envanter: [sprint-3-backend-domains/01-domain-list.md](sprint-3-backend-domains/01-domain-list.md), [sprint-4-frontend-pages/](sprint-4-frontend-pages/).

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
| **Code Generation**      | Lombok, MapStruct              | 1.18.38, 1.6.3 |
| **Validation**           | Spring Boot Starter Validation | -              |
| **Config**               | Spring Dotenv                  | 4.0.0          |
| **Hibernate Extensions** | hibernate-types-60             | 2.21.1         |
| **Test**                 | JUnit 5, TestContainers, Mockito | ~619 test    |

### Backend paketleri (29 paket)

Core: auth, users, stores, config, common, controller.
Admin: admin.
Domain: orders, products, financial, categories, dashboard, purchasing, expenses, billing, webhook, trendyol, alerts, qa, returns, stocktracking, activitylog, education, notifications, referral, support, email, currency, ai.

Tam liste: [sprint-3-backend-domains/01-domain-list.md](sprint-3-backend-domains/01-domain-list.md).

---

## Frontend (Next.js/React)

| Kategori             | Teknoloji                  | Versiyon        |
| -------------------- | -------------------------- | --------------- |
| **Dil**              | TypeScript                 | 5.x             |
| **Framework**        | Next.js                    | 15.3.2          |
| **UI Library**       | React                      | 19.1.0          |
| **Styling**          | Tailwind CSS               | 4.x             |
| **UI Components**    | Radix UI (Shadcn/ui)       | -               |
| **State / Server**   | TanStack React Query       | 5.83.0          |
| **Form**             | React Hook Form + Zod      | 7.56.3 / 3.24.4 |
| **i18n**             | next-intl                  | 4.1.0           |
| **Theme**            | next-themes                | 0.4.6           |
| **Charts**           | recharts                   | 3.6.0           |
| **Icons**            | Lucide React               | -               |
| **Date**             | date-fns                   | 4.1.0           |
| **Carousel**         | Embla Carousel             | 8.6.0           |
| **Toast**            | Sonner                     | 2.0.3           |
| **Test**             | Vitest                     | -               |

### Frontend yapısı

```
sellerx-frontend/
├── app/
│   ├── [locale]/          # i18n (tr/en)
│   │   ├── (app-shell)/   # Ana uygulama
│   │   ├── (admin)/       # Admin panel
│   │   ├── (auth)/        # Giriş, kayıt
│   │   └── (public)/      # pricing vb.
│   └── api/               # BFF route'ları (route.ts)
├── components/            # UI + feature bileşenleri
├── hooks/queries/         # React Query hooks (use-*.ts)
├── lib/
│   ├── api/               # API client, auth
│   ├── auth/              # Auth cache, middleware
│   ├── store/             # Mağaza yardımcıları
│   ├── utils/             # Helper'lar
│   └── validators/        # Zod şemaları
└── messages/              # tr.json, en.json
```

---

## DevOps ve altyapı

| Kategori                  | Teknoloji              |
| ------------------------- | ---------------------- |
| **Containerization**      | Docker                 |
| **Orchestration**         | Docker Compose         |
| **Database**              | PostgreSQL 15 Alpine   |
| **Backend Runtime**       | Eclipse Temurin JRE 21 |
| **Frontend Runtime**      | Node.js 20 Alpine      |
| **Deployment (Frontend)** | Vercel                 |
| **Deployment (Backend)**  | Railway                |

---

## Modül durumu (özet)

| Modül           | Backend       | Frontend      | Durum           |
| --------------- | ------------- | ------------- | --------------- |
| **Auth**        | Tamamlandı   | Tamamlandı   | Çalışıyor       |
| **Users**       | Tamamlandı   | Tamamlandı   | Çalışıyor       |
| **Stores**      | Tamamlandı   | Tamamlandı   | Çalışıyor       |
| **Products**    | Tamamlandı   | Tamamlandı   | Çalışıyor       |
| **Orders**      | Tamamlandı   | Tamamlandı   | Çalışıyor       |
| **Financial**   | Tamamlandı   | Tamamlandı   | Fatura, mutabakat, KDV |
| **Dashboard**   | Tamamlandı   | Tamamlandı   | Çalışıyor       |
| **Expenses**    | Tamamlandı   | Tamamlandı   | Çalışıyor       |
| **Billing**     | Tamamlandı   | Tamamlandı   | Abonelik, ödeme, iyzico |
| **Webhook**     | Tamamlandı   | Tamamlandı   | Ayarlar, event listesi |
| **Purchasing**  | Tamamlandı   | Tamamlandı   | PO, tedarikçi, raporlar |
| **Alerts**      | Tamamlandı   | Tamamlandı   | Kurallar, bildirimler   |
| **Stock Tracking** | Tamamlandı | Tamamlandı   | Takip, uyarılar         |
| **QA**          | Tamamlandı   | Tamamlandı   | Soru/cevap, pattern, conflict |
| **Returns**     | Tamamlandı   | Tamamlandı   | Claims, analitik        |
| **Education**   | Tamamlandı   | Tamamlandı   | Videolar, izleme        |
| **Support**     | Tamamlandı   | Tamamlandı   | Talepler, mesajlar      |
| **Referral**    | Tamamlandı   | Tamamlandı   | Davet kodu              |
| **Admin**       | Tamamlandı   | Tamamlandı   | Mağaza, kullanıcı, sipariş vb. |

---

## Technical Debt

- [ ] TypeScript strict mode aktifleştirme (şu an `false`)
- [ ] ESLint/TypeScript hatalarını düzeltme (build’de ignore ediliyor)
- [ ] Frontend test coverage artırma (Vitest var, kapsam düşük; backend ~619 test)
- [ ] API error handling standardizasyonu
- [ ] Logging ve monitoring iyileştirme
- [ ] Net kâr hesabı: komisyon, kesinti, iade maliyetleri çıkarılmalı (CLAUDE.md Known Debt)
