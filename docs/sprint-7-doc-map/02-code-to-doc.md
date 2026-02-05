# Kod → Doc Eşlemesi (Mevcut Kod)

Kod alanına göre hangi doc'a bakılacak.

| Kod alanı | Bakılacak doc |
|------------|----------------|
| Backend controller, endpoint path, HTTP metot | sprint-1-api-inventory/01-backend-endpoints.md |
| Public (JWT yok) veya Admin-only endpoint | sprint-1-api-inventory/02-public-endpoints.md |
| BFF route (app/api/**/route.ts) | sprint-1-api-inventory/03-bff-routes.md, 04-bff-to-backend-map.md |
| Flyway migration (V*.sql) | sprint-2-db-schema/01-migration-list.md |
| JPA Entity, tablo, kolon, JSONB | sprint-2-db-schema/02-entity-table-map.md, 03-schema-overview.md |
| Backend paket (auth, orders, financial vb.) | sprint-3-backend-domains/01-domain-list.md |
| @Scheduled, ShedLock job | sprint-3-backend-domains/02-scheduled-jobs.md, 03-schedule-overview.md |
| Next.js sayfa (page.tsx), path, layout | sprint-4-frontend-pages/01-page-list.md, 02-feature-map.md, 03-route-structure.md |
| Feature bileşenleri (components/dashboard, purchasing vb.) | sprint-5-ui-components/01-component-list.md |
| Shadcn/ui (components/ui/*) | sprint-5-ui-components/02-ui-primitives.md |
| Tailwind, globals.css, theme.css | sprint-5-ui-components/03-styles-overview.md |
| JWT, cookie, middleware, token refresh | sprint-6-cross-cutting/01-auth-overview.md |
| SecurityRules, permitAll, hasRole | sprint-6-cross-cutting/02-security-rules.md |
| Env değişkenleri, Docker, CI | sprint-6-cross-cutting/03-env-and-deploy.md |
| Doc yapısı, hangi doc nerede | sprint-7-doc-map/01-doc-to-code.md, 02-code-to-doc.md, 03-doc-architecture.md |

**Kullanım:** Kodda değişiklik yaparken yukarıdaki alana göre ilgili doc'u güncelle (docs README Güncelleme Kuralı).
