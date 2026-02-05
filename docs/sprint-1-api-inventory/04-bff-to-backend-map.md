# BFF – Backend Eşlemesi (Mevcut Kod)

01-backend-endpoints.md ile 03-bff-routes.md karşılaştırması. BFF path + method → backend full path + method. **Doc, mevcut koda göre;** kod değişince doc güncellenir.

## Özet

- Çoğu BFF route, backend path ile aynı path'e istek atıyor (BFF `[param]` → backend `{param}`).
- Auth login/logout/refresh: BFF cookie forwarding yapıyor; backend path aynı.
- Aşağıda BFF'nin **gerçekten çağırdığı** path ile backend'de **tanımlı** path yan yana; mevcut davranış böyle.

---

## BFF → Backend path (mevcut kod)

| BFF path | HTTP | BFF'nin çağırdığı path (mevcut) | Backend'de tanımlı path |
|----------|------|---------------------------------|--------------------------|
| /api/orders/stores/[storeId]/sync | POST | /orders/stores/{storeId}/sync | /api/orders/stores/{storeId}/sync |
| /api/orders/stores/[storeId]/by-date-range | GET | /orders/stores/{storeId}/by-date-range | /api/orders/stores/{storeId}/by-date-range |

Diğer tüm BFF route'lar 03-bff-routes.md'deki backend path ile 01-backend-endpoints.md'deki full path ile aynı (Auth, Stores, Dashboard, Products, Orders [sync/by-date-range hariç], Financial, Invoices, Purchasing, Suppliers, Categories, Expenses, Alerts, Stock Tracking, Returns, QA, Referrals, Support, Billing, Education, Notifications, Users, AI, Knowledge, Currency, Admin).

---

## Backend'de var, BFF'de yok (sadece backend veya harici)

- **Health:** GET /health, GET / → Load balancer / probe.
- **Actuator:** /actuator/health/**, /actuator/info (public); /actuator/metrics/**, /actuator/loggers/** (authenticated).
- **Webhook (public):** POST /api/webhook/trendyol/{sellerId}, POST /api/webhook/iyzico/**, GET /api/webhook/health → Trendyol / Iyzico tarafından çağrılır.
- **Swagger:** /swagger-ui/**, /v3/api-docs/**.
- **Test:** /api/test/trendyol-limits/** → BFF'den kullanılmıyor.

---

## Özel durumlar (mevcut davranış)

1. **Auth login / logout / refresh:** BFF backend'e proxy ediyor, Set-Cookie'leri client'a iletiyor. Backend path: /auth/login, /auth/logout, /auth/refresh.
2. **Register:** BFF /api/auth/register → backend POST /users (public).
3. **selected-store:** BFF /api/users/selected-store GET ve PUT; backend /users/selected-store GET ve POST tanımlı.
