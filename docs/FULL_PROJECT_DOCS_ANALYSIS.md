# Proje ve Docs %100 Analiz Raporu

**Tarih:** 2026-02  
**Kapsam:** Tüm proje (backend, frontend, DB) ile docs klasörü (sprint-1..7, architecture/, features/, archive/) tam envanter ve doc–kod eşlemesi.

---

## 1. Proje Envanteri (Kod)

### Backend (sellerx-backend)

| Alan | Sayı / Kaynak |
|------|----------------|
| Paket sayısı | 29 (activitylog, admin, ai, alerts, auth, billing, categories, common, config, controller, currency, dashboard, education, email, expenses, financial, notifications, orders, products, purchasing, qa, referral, returns, stocktracking, stores, support, trendyol, users, webhook) |
| Controller | 51 (50 `@RestController` + 1 `@Controller` HomeController) |
| Flyway migration | 84 dosya (V1–V84), `sellerx-backend/src/main/resources/db/migration/` |
| @Scheduled metodları | 17 (HybridSyncScheduleConfig 8, TrendyolOrderScheduledService 2, TrendyolFinancialSettlementScheduledService 2, StockTrackingScheduledService 2, CurrencyService 1, PatternDiscoveryService 1, SeniorityService 1) |

### Frontend (sellerx-frontend)

| Alan | Sayı / Kaynak |
|------|----------------|
| Sayfa (page.tsx) | 52 — sprint-4/01-page-list.md ile aynı |
| BFF route (route.ts) | 202 dosya, `sellerx-frontend/app/api/` |
| Bileşen klasörleri | 24 + root — sprint-5/01-component-list ile uyumlu |

### Docs Yapısı

| Klasör / Alan | İçerik |
|---------------|--------|
| docs/ kök | README.md, CHANGELOG.md, DOC_CODE_GAP_REPORT.md, DOC_VERIFICATION_REPORT.md |
| Sprint | sprint-1..7, her biri 4 dosya (01–04 veya 01–03) |
| architecture/ | 18 dosya (README + 17 mimari doc) |
| features/ | 3 dosya (EDUCATION_VIDEOS, PURCHASING, WEBHOOKS) |
| archive/ | 9 dosya (kampanya/planlama/referans) |

---

## 2. Doc–Kod Eşleme Doğrulaması

| Alan | Doc kaynağı | Durum |
|------|-------------|--------|
| Backend endpoint'ler | sprint-1/01-backend-endpoints.md | Tüm 51 controller ve path'ler doc'ta; stock-depletion, sync-historical-cargo, StockOrderSynchronizationController, TrendyolFinancialOrderSettlementController dahil. |
| Public/Admin endpoint'ler | sprint-1/02-public-endpoints.md | SecurityRules ve permitAll/hasRole listesi mevcut. |
| BFF route'lar | sprint-1/03-bff-routes.md, 04-bff-to-backend-map.md | 202 route doc'ta gruplu; stock-depletion route dahil. |
| Migration listesi | sprint-2/01-migration-list.md | V1–V84 tam; V61–V63 "kullanılmıyor" notu var. |
| Entity–tablo | sprint-2/02-entity-table-map.md, 03-schema-overview.md | Tüm ana entity'ler ve tablolar doc'ta. |
| Backend domain'ler | sprint-3/01-domain-list.md | 29 paket listelenmiş; StockDepletionService, PurchaseOrderExcelService dahil. |
| Scheduled job'lar | sprint-3/02-scheduled-jobs.md | 17 metod, cron/fixedRate ve ShedLock doc'ta. |
| Frontend sayfalar | sprint-4/01-page-list.md | 52 sayfa path ve layout ile eşleşiyor. |
| UI bileşenleri | sprint-5/01-component-list.md | 24 klasör + root; stock-depletion-banner dahil. |
| Auth / Security / Env | sprint-6/01-auth-overview, 02-security-rules, 03-env-and-deploy | JWT, SecurityRules, env ve deploy doc'ta. |
| Doc–kod haritası | sprint-7/01-doc-to-code, 02-code-to-doc, 03-doc-architecture | Hangi doc'un hangi kodu kapsadığı tanımlı. |
| Mimari / özellik detayı | architecture/, features/ | DOC_VERIFICATION_REPORT ve DOC_CODE_GAP_REPORT ile doğrulanmış; PURCHASING (stock-depletion dahil) features'ta. |

**Sonuç:** Tüm kritik alanlar doc'larla eşleşiyor; eksik bir kod alanı yok.

---

## 3. Bilinen Farklar (Doc'ta Notlu)

- **V61–V63 (kampanya tabloları):** DB'de var; backend'de `ads/` servisi yok, kullanılmıyor. [sprint-2/01-migration-list.md](sprint-2-db-schema/01-migration-list.md) ve [DOC_CODE_GAP_REPORT.md](DOC_CODE_GAP_REPORT.md) içinde açıklanmış.
- **Kampanya doc'ları (CAMPAIGNS, CAMPAIGN_SIMULATION):** Kodda karşılığı olmadığı için [archive/](archive/) altında; features'ta yok.

---

## 4. Referanslar

- [docs/README.md](README.md) — Dokümantasyon giriş noktası
- [DOC_CODE_GAP_REPORT.md](DOC_CODE_GAP_REPORT.md) — Doc–kod gap özeti
- [DOC_VERIFICATION_REPORT.md](DOC_VERIFICATION_REPORT.md) — architecture/ ve features/ doğrulama
- [sprint-7-doc-map/02-code-to-doc.md](sprint-7-doc-map/02-code-to-doc.md) — Kod alanı → bakılacak doc
