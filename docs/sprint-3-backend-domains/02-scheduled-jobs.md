# Scheduled Job'lar (Mevcut Kod)

Tüm `@Scheduled` metodları: sınıf, metod, cron/fixedRate, zone, ShedLock name, kısa açıklama. Kaynak: backend Java dosyalarında `@Scheduled` geçen sınıflar.

| Sınıf | Metod | Cron / fixedRate | Zone | ShedLock name | Açıklama |
|-------|--------|------------------|------|---------------|----------|
| HybridSyncScheduleConfig | dailySettlementSync | 0 0 7 * * * | Europe/Istanbul | dailySettlementSync | Günlük Settlement API sync; gerçek komisyon verisi (son 14 gün). |
| HybridSyncScheduleConfig | dailyCargoShippingSync | 0 15 7 * * * | Europe/Istanbul | dailyCargoShippingSync | Kargo fatura detayı ve siparişlere gerçek kargo maliyeti yazılır. |
| HybridSyncScheduleConfig | updateProductCommissionCache | 0 30 7 * * * | Europe/Istanbul | updateProductCommissionCache | Ürün komisyon cache güncellemesi (settlement işlemede yapılıyor; slot ek bakım için). |
| HybridSyncScheduleConfig | dailyReconciliation | 0 0 8 * * * | Europe/Istanbul | dailyReconciliation | Tahmini komisyonlu siparişleri Settlement verisiyle eşleştirir, gerçek komisyon yazar. |
| HybridSyncScheduleConfig | dailyDeductionInvoiceSync | 0 15 8 * * * | Europe/Istanbul | dailyDeductionInvoiceSync | Kesinti ve iade faturaları (son 30 gün) OtherFinancials API’den çekilir. |
| HybridSyncScheduleConfig | hourlyGapFill | 0 30 * * * * | Europe/Istanbul | hourlyGapFill | Orders API’den son siparişler çekilir; tahmini komisyon (barcode referansı) atanır. |
| HybridSyncScheduleConfig | hourlyDeductionInvoiceCatchUp | 0 45 * * * * | Europe/Istanbul | hourlyDeductionInvoiceCatchUp | Kesinti/iade faturaları catch-up (son 3 gün). |
| HybridSyncScheduleConfig | logGapAnalysisStatus | 0 0 */6 * * * | Europe/Istanbul | logGapAnalysisStatus | Gap analizi durumu loglanır (Settlement vs Orders API). |
| TrendyolOrderScheduledService | syncAllDataForAllTrendyolStores | 0 15 6 * * ? | Europe/Istanbul | syncAllDataForAllTrendyolStores | Günlük tam sync: sipariş, ürün, Q&A, iadeler. |
| TrendyolOrderScheduledService | catchUpSync | 0 0 * * * ? | Europe/Istanbul | catchUpSync | Saatlik catch-up: son 2 saat sipariş + ürün, Q&A, iadeler. |
| TrendyolFinancialSettlementScheduledService | syncSettlementsForAllStores | 0 0 2 * * * | — | — | Tüm mağazalar için settlement sync (günlük 02:00). |
| TrendyolFinancialSettlementScheduledService | syncSettlementsRegularly | fixedRate 6h (21_600_000 ms) | — | — | 6 saatte bir settlement sync. |
| StockTrackingScheduledService | checkAllTrackedProducts | 0 30 * * * ? | Europe/Istanbul | — | Takip edilen ürünlerin stok kontrolü (saat başı :30). |
| StockTrackingScheduledService | cleanupOldSnapshots | 0 0 3 * * ? | Europe/Istanbul | — | Eski snapshot'lar silinir (30 gün retention). |
| CurrencyService | updateExchangeRates | 0 0 10 * * ? | Europe/Istanbul | — | TCMB'den döviz kurları güncellenir (USD/TRY, EUR/TRY). |
| PatternDiscoveryService | runDailyPatternAnalysis | 0 0 3 * * * | — | — | Günlük pattern analizi; sorulardan bilgi önerileri üretilir. |
| SeniorityService | reviewAutoSubmitEligibility | 0 0 * * * * | — | — | Saatlik: bekleyen pattern’ler için auto-submit uygunluğu incelenir. |

**Toplam:** 17 scheduled metod. ShedLock kullananlar: HybridSyncScheduleConfig (8), TrendyolOrderScheduledService (2). ShedLockConfig: defaultLockAtMostFor = 10m; lock provider: JdbcTemplate (shedlock tablosu).
