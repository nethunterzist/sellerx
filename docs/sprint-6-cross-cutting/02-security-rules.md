# Backend Security Rules (Mevcut Kod)

SecurityRules implementasyonları: permitAll, authenticated, hasRole. Kaynak: [sellerx-backend](sellerx-backend/src/main/java/com/ecommerce/sellerx/). SecurityConfig tüm SecurityRules bean'lerini toplar; sıra önemli (önce permitAll, sonra hasRole, en sonda anyRequest().authenticated()).

| Sınıf | Path / kural | Erişim |
|-------|----------------|--------|
| AuthSecurityRules | POST /auth/login, /auth/refresh, /auth/logout | permitAll |
| UserSecurityRules | POST /users | permitAll (kayıt). Diğer store-scoped erişim @PreAuthorize canAccessStore. |
| WebhookSecurityRules | POST /api/webhook/trendyol/**, POST /api/webhook/iyzico/**, POST /api/webhook/parasut/**, GET /api/webhook/health | permitAll |
| HealthSecurityRules | GET /health, /, /actuator/health/**, /actuator/info | permitAll. /actuator/metrics/**, /actuator/loggers/** authenticated |
| SwaggerSecurityRules | /swagger-ui/**, /swagger-ui.html, /v3/api-docs/** | permitAll |
| EducationSecurityRules | GET /api/education/videos, GET /api/education/videos/** | permitAll |
| ReferralSecurityRules | GET /api/referrals/validate/** | permitAll |
| BillingSecurityRules | GET /api/billing/plans, GET /api/billing/plans/** | permitAll |
| IyzicoSecurityRules | POST /api/webhook/iyzico/**, POST|GET /api/billing/checkout/callback, GET /api/billing/plans, GET /api/billing/plans/** | permitAll |
| AdminSecurityRules | /api/admin/** | hasRole("ADMIN") |
| TestSecurityRules | /api/test/trendyol-limits/** | permitAll (geçici test) |

**Method-level:** @PreAuthorize("hasRole('ADMIN')") Admin controller'larda; @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)") store-scoped endpoint'lerde (purchasing, qa, webhook management vb.).

**SupportSecurityRules:** SecurityRules implemente etmez; sadece javadoc ile support/admin endpoint güvenliği açıklanır (JWT + Admin controller'da hasRole).
