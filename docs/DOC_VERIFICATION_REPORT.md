# Doc–Kod Doğrulama Raporu

**Tarih:** 2026-02  
**Kapsam:** `docs/architecture/` ve `docs/features/` altındaki dokümanların mevcut kodla karşılaştırılması.

---

## Özet

| Kategori | Güncel | Güncellendi | Obsolete / Archive |
|----------|--------|-------------|-------------------------|
| architecture/ | 16 | 1 (README Scheduled Jobs) | 0 |
| features/ | 2 | 0 | 0 (CAMPAIGNS, CAMPAIGN_SIMULATION archive'a taşındı) |

---

## architecture/

| Dosya | Sonuç | Açıklama |
|-------|--------|----------|
| ALERT_SYSTEM.md | Güncel | alerts/ paketi, AlertRule, AlertEngine, AlertHistory ile uyumlu. |
| ASYNC_PROCESSING.md | Güncel | @Async, AsyncConfig kodla uyumlu. |
| BACKEND_ARCHITECTURE.md | Güncel | Paket yapısı ve stack güncel. |
| BILLING_SYSTEM.md | Güncel | billing/ paketi, Subscription, Invoice ile uyumlu. |
| BUYBOX_SYSTEM.md | Güncel | buybox/ paketi, BuyboxTrackedProduct, BuyboxService ile uyumlu. |
| COMMISSION_SYSTEM.md | Güncel | Komisyon formülü ve fallback mantığı kodla aynı. |
| DATABASE_SCHEMA.md | Güncel | Flyway migration’lar ve entity–tablo eşlemesi güncel. |
| HISTORICAL_SETTLEMENT_SYNC.md | Güncel | financial/ historical sync, 90 gün limiti workaround mevcut. |
| ORDER_SYNC_SYSTEM.md | Güncel | TrendyolOrderService, webhook + scheduled akış güncel. |
| QA.md | Güncel | qa/ paketi, TrendyolQaService ile uyumlu. |
| RATE_LIMITING.md | Güncel | TrendyolRateLimiter (10 req/sec) doğru. |
| **README.md** | **Güncellendi** | Scheduled Jobs tablosu sprint-3 (02-scheduled-jobs.md) ile güncellendi: syncAllDataForAllTrendyolStores 06:15, catchUpSync saatlik, dailySettlementSync 07:00, HybridSyncScheduleConfig job’ları eklendi; tam liste için sprint-3 linki eklendi. |
| STORE_ONBOARDING.md | Güncel | StoreOnboardingService, sync_status, fazlar kodla uyumlu. |
| STORE_SYNC_GUIDE.md | Güncel | stores/ sync, STORE_ONBOARDING ile tutarlı. |
| SYNC_SYSTEM.md | Güncel | Hybrid sync ve job’lar kodla uyumlu. |
| TEST_INFRASTRUCTURE.md | Güncel | BaseIntegrationTest, BaseControllerTest, TestContainers güncel. |
| TRENDYOL_API_LIMITS.md | Güncel | API limitleri metni güncel. |
| WEBHOOK_SYSTEM.md | Güncel | webhook/ paketi, TrendyolWebhookController, event_id idempotency kodla aynı. |

---

## features/

| Dosya | Sonuç | Açıklama |
|-------|--------|----------|
| EDUCATION_VIDEOS.md | Güncel | education/ paketi, EducationVideo, EducationVideoController, /api/education/videos ile uyumlu. |
| WEBHOOKS.md | Güncel | webhook/ paketi, WebhookManagementController, path ve yetki doc ile uyumlu. |

**Not:** CAMPAIGNS.md ve CAMPAIGN_SIMULATION.md kodda karşılığı olmadığı için archive'a taşındı (docs/archive/).

---

## Yapılan Değişiklikler

1. **docs/architecture/README.md**  
   - “Scheduled Jobs” bölümündeki tablo kaldırıldı; yerine sprint-3’e referans ve güncel job listesi (TrendyolOrderScheduledService, HybridSyncScheduleConfig, TrendyolFinancialSettlementScheduledService) eklendi.

2. **Kampanya doc'ları (CAMPAIGNS.md, CAMPAIGN_SIMULATION.md)**  
   - Kodda karşılığı olmadığı için docs/archive/ altına taşındı; features/ altında artık yok.

---

## Referans

- Plan: `architecture_features_doc_verification` (docs/architecture/ ve docs/features/ doğrulama).
- Güncel job listesi: [sprint-3-backend-domains/02-scheduled-jobs.md](sprint-3-backend-domains/02-scheduled-jobs.md).
