# Zaman Çizelgesi Özeti (Europe/Istanbul)

Scheduled job'ların günlük/saatlik dağılımı. Kaynak: [02-scheduled-jobs.md](02-scheduled-jobs.md).

## Günlük Zaman Sırası

| Saat | Job(lar) | ShedLock |
|------|----------|----------|
| 02:00 | TrendyolFinancialSettlementScheduledService.syncSettlementsForAllStores | — |
| 03:00 | StockTrackingScheduledService.cleanupOldSnapshots, PatternDiscoveryService.runDailyPatternAnalysis | — |
| 06:15 | TrendyolOrderScheduledService.syncAllDataForAllTrendyolStores | syncAllDataForAllTrendyolStores |
| 07:00 | HybridSyncScheduleConfig.dailySettlementSync | dailySettlementSync |
| 07:15 | HybridSyncScheduleConfig.dailyCargoShippingSync | dailyCargoShippingSync |
| 07:30 | HybridSyncScheduleConfig.updateProductCommissionCache | updateProductCommissionCache |
| 08:00 | HybridSyncScheduleConfig.dailyReconciliation | dailyReconciliation |
| 08:15 | HybridSyncScheduleConfig.dailyDeductionInvoiceSync | dailyDeductionInvoiceSync |
| 10:00 | CurrencyService.updateExchangeRates | — |

## Saatlik ve Periyodik

| Periyot | Job(lar) | ShedLock |
|---------|----------|----------|
| Her saat :00 | TrendyolOrderScheduledService.catchUpSync, SeniorityService.reviewAutoSubmitEligibility | catchUpSync (sadece order sync) |
| Her saat :30 | HybridSyncScheduleConfig.hourlyGapFill, StockTrackingScheduledService.checkAllTrackedProducts | hourlyGapFill |
| Her saat :45 | HybridSyncScheduleConfig.hourlyDeductionInvoiceCatchUp | hourlyDeductionInvoiceCatchUp |
| Her 6 saat | HybridSyncScheduleConfig.logGapAnalysisStatus | logGapAnalysisStatus |
| fixedRate 6h | TrendyolFinancialSettlementScheduledService.syncSettlementsRegularly | — |

## ShedLock

- **Kullanan job'lar:** HybridSyncScheduleConfig (8 metod), TrendyolOrderScheduledService (2 metod). Çoklu instance’da aynı job yalnızca bir node’da çalışır.
- **Konfig:** [ShedLockConfig](sellerx-backend/src/main/java/com/ecommerce/sellerx/config/ShedLockConfig.java): `@EnableSchedulerLock(defaultLockAtMostFor = "10m")`, LockProvider = JdbcTemplateLockProvider (shedlock tablosu, DB zamanı).
- **Lock süreleri:** Her job’da `lockAtLeastFor` / `lockAtMostFor` ayrı tanımlı (örn. dailySettlementSync 5m–60m, catchUpSync 2m–30m).
