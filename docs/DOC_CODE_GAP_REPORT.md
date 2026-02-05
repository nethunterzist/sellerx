# Doc–Kod Gap Raporu

**Tarih:** 2026-02  
**Kapsam:** Tüm proje (backend, frontend, DB) ile dokümantasyon (sprint-1..7, architecture/, features/) karşılaştırması.

---

## Özet

| Kontrol | Sonuç |
|---------|--------|
| Backend endpoint (sprint-1 01-backend-endpoints.md) | Uyumlu; doc ile kod eşleşiyor. |
| BFF route (sprint-1 03-bff-routes, 04-bff-to-backend-map) | Uyumlu; stock-depletion dahil listelenmiş. |
| Flyway migration (sprint-2 01-migration-list.md) | Uyumlu; V1–V84, 84 dosya doc ile aynı. |
| Frontend sayfa (sprint-4 01-page-list.md) | Uyumlu; 54 sayfa doc ile aynı. |
| Scheduled job (sprint-3 02-scheduled-jobs.md) | Uyumlu; 18 metod doc ile aynı. |
| architecture/ ve features/ | [DOC_VERIFICATION_REPORT.md](DOC_VERIFICATION_REPORT.md) ile doğrulanmış. |

---

## Doc'da var, kodda yok

Kampanya doc'ları (CAMPAIGNS.md, CAMPAIGN_SIMULATION.md) kodda karşılığı olmadığı için **archive'a taşındı**; features/ altında artık yok. Eski planlama/referans için: [archive/CAMPAIGNS.md](archive/CAMPAIGNS.md), [archive/CAMPAIGN_SIMULATION.md](archive/CAMPAIGN_SIMULATION.md).

| Madde | Açıklama |
|-------|----------|
| Campaign tabloları (V61–V63) | DB'de `trendyol_campaigns`, `campaign_daily_stats`, `ad_reports.campaign_id` mevcut; backend'de bu tabloları kullanan servis/API yok (kullanılmıyor). Migration listesinde isimleri var. |

---

## Kodda var, doc'da yok / doc'da eksik

Purchasing / Stock depletion için derin doc eklendi: [features/PURCHASING.md](features/PURCHASING.md). Bu madde kapatıldı; proje–doc uyumlu.

---

## Sprint envanter doğrulama özeti

- **sprint-1 (Backend endpoint):** Tüm Controller’lar ve endpoint’ler doc’ta; multi-period, ActivityLog, stock-depletion, sync-historical-cargo, StockOrderSynchronizationController, TrendyolFinancialOrderSettlementController dahil.
- **sprint-1 (BFF):** app/api/**/route.ts ile 03-bff-routes ve 04-bff-to-backend-map uyumlu; stock-depletion route doc’ta.
- **sprint-2 (Migration):** db/migration/ altında 84 V*.sql dosyası; 01-migration-list.md V1–V84 ile aynı.
- **sprint-3 (Domain):** 30 paket listelenmiş; ads/ yok (doğru). **sprint-3 (Scheduled job):** 18 @Scheduled metodu doc’ta; HybridSyncScheduleConfig, TrendyolOrderScheduledService, TrendyolFinancialSettlementScheduledService, StockTrackingScheduledService, BuyboxScheduledService, CurrencyService, PatternDiscoveryService, SeniorityService.
- **sprint-4 (Sayfa):** 54 page.tsx; path’ler doc ile aynı. Stock-depletion için ayrı sayfa yok (sadece dashboard banner); doc’ta da yok, tutarlı.

---

## Önerilen doc güncellemeleri (isteğe bağlı)

1. **Campaign tabloları:** sprint-2 03-schema-overview veya architecture/DATABASE_SCHEMA.md’de "V61–V63 campaign tabloları şu an backend servisi olmadığı için kullanılmıyor" cümlesi eklenebilir.
2. **Purchasing/stock-depletion:** Eklendi: [features/PURCHASING.md](features/PURCHASING.md).

---

## Referans

- [DOC_VERIFICATION_REPORT.md](DOC_VERIFICATION_REPORT.md) — architecture/ ve features/ doğrulama özeti.
- [sprint-7-doc-map/02-code-to-doc.md](sprint-7-doc-map/02-code-to-doc.md) — Kod alanı → bakılacak doc.
