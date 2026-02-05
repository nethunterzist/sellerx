# Doc → Kod Eşlemesi (Mevcut Kod)

Her sprint ve ana doc dosyası hangi kod alanını dokümante ediyor.

| Sprint / Dosya | Dokümante edilen kod alanı |
|----------------|----------------------------|
| **Sprint 1** sprint-1-api-inventory | |
| 01-backend-endpoints.md | sellerx-backend/**/*Controller.java — endpoint'ler, HTTP, path |
| 02-public-endpoints.md | SecurityRules + permitAll/hasRole — public ve Admin endpoint'ler |
| 03-bff-routes.md | sellerx-frontend/app/api/**/route.ts — BFF route'ları |
| 04-bff-to-backend-map.md | BFF route → backend path eşlemesi |
| **Sprint 2** sprint-2-db-schema | |
| 01-migration-list.md | sellerx-backend/src/main/resources/db/migration/*.sql |
| 02-entity-table-map.md | sellerx-backend/**/*.java @Entity sınıfları |
| 03-schema-overview.md | Tablolar, ilişkiler, JSONB, Flyway özeti |
| **Sprint 3** sprint-3-backend-domains | |
| 01-domain-list.md | sellerx-backend/… paketleri (auth, orders, financial vb.) |
| 02-scheduled-jobs.md | @Scheduled, @SchedulerLock kullanan sınıflar |
| 03-schedule-overview.md | Job zaman çizelgesi özeti |
| **Sprint 4** sprint-4-frontend-pages | |
| 01-page-list.md | sellerx-frontend/app/[locale]/**/page.tsx |
| 02-feature-map.md | Sayfa → feature grupları, hooks |
| 03-route-structure.md | [locale], layout grupları, dinamik segmentler |
| **Sprint 5** sprint-5-ui-components | |
| 01-component-list.md | sellerx-frontend/components/** (ui hariç klasörler) |
| 02-ui-primitives.md | sellerx-frontend/components/ui/*.tsx |
| 03-styles-overview.md | app/globals.css, app/theme.css, Tailwind v4 |
| **Sprint 6** sprint-6-cross-cutting | |
| 01-auth-overview.md | auth paketi, SecurityConfig, middleware.ts, lib/api/client.ts |
| 02-security-rules.md | SecurityRules implementasyonları |
| 03-env-and-deploy.md | Env değişkenleri, db.sh, start-sellerx.sh, docker-compose, .github/workflows/ci.yml |
| **Sprint 7** sprint-7-doc-map | |
| 01-doc-to-code.md | Bu tablo (doc → kod) |
| 02-code-to-doc.md | Kod alanı → doc (ters indeks) |
| 03-doc-architecture.md | docs yapısı, navigasyon, güncelleme kuralları |
