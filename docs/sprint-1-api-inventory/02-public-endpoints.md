# Public ve Admin Endpoint Listesi

SecurityRules bean'lerinden çıkarılmıştır. Sıra: featureSecurityRules forEach → anyRequest().authenticated().

## Public (JWT gerektirmez – permitAll)

| HTTP | Path | Kaynak (SecurityRules) |
|------|------|-------------------------|
| POST | /auth/login | AuthSecurityRules |
| POST | /auth/refresh | AuthSecurityRules |
| POST | /auth/logout | AuthSecurityRules |
| POST | /api/webhook/trendyol/** | WebhookSecurityRules |
| POST | /api/webhook/iyzico/** | WebhookSecurityRules, IyzicoSecurityRules |
| POST | /api/webhook/parasut/** | WebhookSecurityRules |
| GET | /api/webhook/health | WebhookSecurityRules |
| GET | /health | HealthSecurityRules |
| GET | / | HealthSecurityRules |
| GET | /actuator/health/** | HealthSecurityRules |
| GET | /actuator/info | HealthSecurityRules |
| GET | /api/education/videos | EducationSecurityRules |
| GET | /api/education/videos/** | EducationSecurityRules |
| GET | /api/referrals/validate/** | ReferralSecurityRules |
| * | /api/test/trendyol-limits/** | TestSecurityRules |
| GET | /api/billing/plans | BillingSecurityRules, IyzicoSecurityRules |
| GET | /api/billing/plans/** | BillingSecurityRules, IyzicoSecurityRules |
| POST | /api/billing/checkout/callback | IyzicoSecurityRules |
| GET | /api/billing/checkout/callback | IyzicoSecurityRules |
| POST | /users | UserSecurityRules (register) |
| GET | /swagger-ui/** | SwaggerSecurityRules |
| GET | /swagger-ui.html | SwaggerSecurityRules |
| GET | /v3/api-docs/** | SwaggerSecurityRules |

**Not:** Iyzico webhook alt path'leri (örn. /api/webhook/iyzico/payment, /threeds, /refund, /card) ve /api/webhook/iyzico/health yukarıdaki POST /api/webhook/iyzico/** ve controller'daki GET /health ile kapsanır.

---

## Admin only (hasRole("ADMIN"))

| Path | Açıklama |
|------|----------|
| /api/admin/** | Tüm admin endpoint'leri (stores, users, billing, activity-logs, orders, products, referrals, dashboard, notifications, support/tickets). |

Admin alt path'leri: /api/admin/stores, /api/admin/users, /api/admin/billing, /api/admin/activity-logs, /api/admin/orders, /api/admin/products, /api/admin/referrals, /api/admin/dashboard, /api/admin/notifications, /api/admin/support/tickets. Detay için [01-backend-endpoints.md](./01-backend-endpoints.md) Admin bölümüne bakın.

---

## Authenticated (varsayılan)

Yukarıdaki public ve admin path'lerin dışındaki **tüm** endpoint'ler JWT gerektirir (Authorization: Bearer veya access_token cookie). Actuator metrics ve loggers da authenticated:

| HTTP | Path | Kaynak |
|------|------|--------|
| * | /actuator/metrics/** | HealthSecurityRules – authenticated |
| * | /actuator/loggers/** | HealthSecurityRules – authenticated |
