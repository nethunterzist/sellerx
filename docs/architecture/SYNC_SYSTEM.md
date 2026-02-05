# Senkronizasyon Sistemi

SellerX platformunun Trendyol API ile veri senkronizasyonu mimarisi.

## Genel Bakis

Sistem uc katmanli senkronizasyon stratejisi kullanir:

1. **Webhook (Anlik)**: Trendyol'dan gelen siparis bildirimleri
2. **Polling (Saatlik/Gunluk)**: Zamanlanmis API cagrilari ile veri cekme
3. **Store Onboarding (Tek seferlik)**: Yeni magaza eklendiginde gecmis verilerin toplu cekilmesi

---

## Zamanlanmis Isler (Scheduled Jobs)

Tum zamanlar **Europe/Istanbul** saat dilimine goredir. Dagitik kilit yonetimi icin **ShedLock** kullanilir.

### Gunluk Schedule

```
06:15  syncAllDataForAllTrendyolStores    Siparis + Urun + Q&A + Iade (full sync)
07:00  dailySettlementSync                 Finansal mutabakat (gercek komisyon)
07:15  dailyCargoShippingSync              Kargo fatura detaylari
07:30  updateProductCommissionCache        Urun komisyon orani cache
08:00  dailyReconciliation                 Tahmini komisyonlari gercek verilerle eslestir
08:15  dailyDeductionInvoiceSync           Kesinti faturalari (reklam, ceza, uluslararasi)
10:00  currencyRateUpdate                  Doviz kuru guncelleme (TCMB)
```

### Saatlik Schedule

```
:00    catchUpSync                         Siparis + Urun + Q&A + Iade (catch-up)
:00    seniorityCalculation                Seniority hesaplama
:30    hourlyGapFill                       Eksik siparisleri tahmini komisyonla doldur
:30    stockTracking                       Stok takip snapshot
:45    hourlyDeductionInvoiceCatchUp       Kesinti faturalari catch-up
```

### Diger Periyodik Isler

```
Her 6 saat    logGapAnalysisStatus         Gap analizi durum loglama
Her 6 saat    retryFailedSettlements       Basarisiz settlement yeniden deneme
Her 12 saat   buyboxTracking               BuyBox takibi
03:00         dailyStockReport             Stok gunluk rapor
03:00         patternDiscovery             Q&A pattern kesfetme
```

---

## Veri Tipleri ve Senkronizasyon Detaylari

### 1. Siparisler (Orders)

**Dosyalar**:
- `TrendyolOrderScheduledService.java` — Zamanlanmis isler
- `TrendyolOrderService.java` — Sync mantigi
- `HybridSyncScheduleConfig.java` — Gap fill ve reconciliation

**Senkronizasyon Yontemleri**:

| Yontem | Frekans | Lookback | Aciklama |
|--------|---------|----------|----------|
| Webhook | Anlik | - | Trendyol siparis bildirimlerini push eder |
| `catchUpSync` | Saatlik (:00) | 2 saat | Webhook kacirmalarina karsi yedek |
| `syncAllDataForAllTrendyolStores` | Gunluk 06:15 | ~3 ay | Tam gecmis sync |
| `hourlyGapFill` | Saatlik (:30) | Son birkac gun | Eksik siparisleri tahmini komisyonla doldur |

**Veri Akisi**:
```
Trendyol Orders API
    |
    v
fetchAndSaveOrdersForStore()        — Tam sync (3 aylik, 15 gunluk chunk'lar)
fetchAndSaveOrdersForStoreInRange()  — Catch-up (son 2 saat)
    |
    v
TrendyolOrder entity
    - dataSource: ORDER_API | SETTLEMENT_API | HYBRID
    - isCommissionEstimated: true (tahmini) | false (gercek)
    - order_items: JSONB (urun detaylari)
```

**Komisyon Akisi**:
1. Siparis geldiginde `isCommissionEstimated = true`, urunun `lastCommissionRate` ile hesaplanir
2. 3-7 gun sonra Settlement API'den gercek komisyon gelir
3. `dailyReconciliation` (08:00) tahmini degerleri gercek degerlerle gunceller
4. `isCommissionEstimated = false`, `dataSource = HYBRID` olur

### 2. Urunler (Products)

**Dosyalar**:
- `TrendyolOrderScheduledService.java` — Schedule (siparis sync ile birlikte)
- `TrendyolProductService.java` — Sync mantigi

**Senkronizasyon**:

| Yontem | Frekans | Aciklama |
|--------|---------|----------|
| `syncAllDataForAllTrendyolStores` | Gunluk 06:15 | Tam urun katalogu sync |
| `catchUpSync` | Saatlik (:00) | Urun degisikliklerini yakala |

**Cekilen Veriler**:
- Urun adi, barkod, model kodu
- Fiyat, stok, durum (aktif/pasif)
- Komisyon orani (son settlement'tan)
- Maliyet ve stok gecmisi (JSONB)

### 3. Finansal Mutabakat (Settlements)

**Dosyalar**:
- `HybridSyncScheduleConfig.java` — `dailySettlementSync`
- `TrendyolFinancialSettlementService.java` — Sync mantigi
- `CommissionReconciliationService.java` — Eslestirme mantigi

**Senkronizasyon**:

| Yontem | Frekans | Lookback | Aciklama |
|--------|---------|----------|----------|
| `dailySettlementSync` | Gunluk 07:00 | 14 gun | Gercek komisyon verileri |
| `dailyReconciliation` | Gunluk 08:00 | - | ORDER_API siparislerini settlement ile eslestirir |
| `retryFailedSettlements` | 6 saatte bir | - | Basarisiz denemeleri tekrarlar |

**Neden 14 gun?**: Trendyol settlement verisini 3-7 gun gecikmeyle yayinlar. 14 gunluk pencere tum geciken verileri yakalar.

**Hybrid Sync Sistemi**:
```
Settlement API ──> Gercek komisyon (3-7 gun gecikme)
                        |
                        v
                   Reconciliation ──> ORDER_API siparislerini guncelle
                        |
                        v
Orders API ──────> Tahmini komisyon (anlik)
                        |
                        v
                   Gap Fill ──> Eksik siparisleri tahmini degerle doldur
```

### 4. Kargo Faturalari (Cargo Invoices)

**Dosyalar**:
- `HybridSyncScheduleConfig.java` — `dailyCargoShippingSync`
- `TrendyolOtherFinancialsService.java` — Sync mantigi

**Senkronizasyon**:

| Yontem | Frekans | Lookback | Aciklama |
|--------|---------|----------|----------|
| `dailyCargoShippingSync` | Gunluk 07:15 | 14 gun | Siparis bazli gercek kargo maliyetleri |
| Onboarding | Tek seferlik | Tum gecmis | Tam kargo gecmisi (en eski siparisten itibaren) |

**Veri Akisi**:
```
Trendyol OtherFinancials API
    -> /otherfinancials?transactionType=DeductionInvoices (seri no'lari cek)
    -> /cargo-invoice/{serialNumber}/items (detay cek)
    |
    v
TrendyolCargoInvoice entity
    - Siparis bazli kargo maliyeti
    - Desi, KDV orani, KDV tutari
    |
    v
TrendyolOrder.shippingCost guncellenir
    - isShippingEstimated = false
```

### 5. Kesinti Faturalari (Deduction Invoices)

**Dosyalar**:
- `HybridSyncScheduleConfig.java` — `dailyDeductionInvoiceSync`, `hourlyDeductionInvoiceCatchUp`
- `TrendyolOtherFinancialsService.java` — `syncDeductionInvoices()`, `syncReturnInvoices()`

**Senkronizasyon**:

| Yontem | Frekans | Lookback | Aciklama |
|--------|---------|----------|----------|
| `dailyDeductionInvoiceSync` | Gunluk 08:15 | 30 gun | Tum kesinti ve iade faturalari |
| `hourlyDeductionInvoiceCatchUp` | Saatlik (:45) | 3 gun | Yeni kesintileri hizli yakala |
| Onboarding | Tek seferlik | 12 ay | Tam gecmis |

**Kesinti Kategorileri**:

| Kategori | Ornek Islem Tipleri |
|----------|---------------------|
| **Reklam (REKLAM)** | Reklam Bedeli, Influencer Reklam Bedeli |
| **Ceza (CEZA)** | Tedarik Edememe, Termin Gecikme, Yanlış/Kusurlu/Eksik Urun |
| **Uluslararasi (ULUSLARARASI)** | Uluslararasi Hizmet Bedeli, Yurtdisi Operasyon |
| **Diger (DIGER)** | Platform bedelleri, erken odeme kesintisi vb. |
| **Iade (IADE)** | Yurtdisi Operasyon Iade, Kargo Itiraz Iade, Tazmin |

**Dashboard Hesaplamasi**:
```
Kesilen Faturalar = REKLAM + CEZA + ULUSLARARASI + DIGER - IADE
```

**Neden 30 gun lookback?**: Reklam bedelleri ve cezalar olayin gerceklesmesinden gunler veya haftalar sonra faturalanabilir. 30 gunluk pencere gec gelen faturalari yakalar.

### 6. Q&A (Musteri Sorulari)

**Dosyalar**:
- `TrendyolOrderScheduledService.java` — Schedule
- `TrendyolQaService.java` — Sync mantigi

| Yontem | Frekans | Aciklama |
|--------|---------|----------|
| `syncAllDataForAllTrendyolStores` | Gunluk 06:15 | Tam Q&A sync |
| `catchUpSync` | Saatlik (:00) | Yeni sorulari yakala |

### 7. Iadeler (Returns/Claims)

**Dosyalar**:
- `TrendyolOrderScheduledService.java` — Schedule
- `TrendyolClaimsService.java` — Sync mantigi

| Yontem | Frekans | Aciklama |
|--------|---------|----------|
| `syncAllDataForAllTrendyolStores` | Gunluk 06:15 | Tam iade sync |
| `catchUpSync` | Saatlik (:00) | Yeni iadeleri yakala |

### 8. BuyBox Takibi

**Dosyalar**:
- `BuyboxScheduledService.java`

| Yontem | Frekans | Aciklama |
|--------|---------|----------|
| BuyBox polling | 12 saatte bir | Rakip fiyat ve BuyBox durumu |

### 9. Stok Takibi

**Dosyalar**:
- `StockTrackingScheduledService.java`

| Yontem | Frekans | Aciklama |
|--------|---------|----------|
| Stok snapshot | Saatlik (:30) | Stok seviyesi kaydı |
| Gunluk rapor | 03:00 | Gunluk stok ozeti |

### 10. Doviz Kuru

**Dosyalar**:
- `CurrencyService.java`

| Yontem | Frekans | Aciklama |
|--------|---------|----------|
| TCMB sync | Gunluk 10:00 | USD/EUR/GBP kurlarini cek |

---

## Webhook Sistemi

**Dosyalar**:
- `TrendyolWebhookController.java` — Endpoint
- `TrendyolWebhookService.java` — Isleme mantigi
- `WebhookSecurityRules.java` — Public erisim izni

**Endpoint**: `POST /api/webhook/trendyol/{sellerId}` (JWT gerektirmez)

**Akis**:
```
Trendyol push bildirimi
    |
    v
1. HMAC-SHA256 imza dogrulama
2. Payload parse
3. Idempotency kontrolu (eventId: sellerId + orderNumber + status + timestamp)
4. WebhookEvent tablosuna kayit
5. TrendyolWebhookService.processWebhookOrder() ile siparis guncelleme
6. 200 OK donusu (5 saniye icinde - Trendyol gereksinimu)
```

**Guclu Yanlar**:
- Anlik siparis guncelleme
- Idempotent (ayni event tekrar islenmez)
- Audit log (tum eventler WebhookEvent tablosunda)

**Zayif Yanlar**:
- Sadece siparis olaylari icin (urun degisiklikleri webhook ile gelmez)
- Webhook basarisiz olursa bir sonraki saatlik polling'e kadar veri gelmez

---

## Store Onboarding Synci

**Dosya**: `StoreOnboardingService.java`

Yeni magaza eklendiginde coklu thread ile paralel sync yapilir:

```
PHASE 1: PRODUCTS (Sirali)
    Tum urun katalogu cekilir
    |
    v
PHASE 2: PARALEL CALISMA
    |
    |── Thread A: Kritik zincir (sirali)
    |   1. HISTORICAL  — Gecmis siparis cekilmesi
    |   2. FINANCIAL    — Settlement + PaymentOrders + Stoppages
    |                     + Kargo faturalari (tam gecmis)
    |                     + Kesinti faturalari (12 ay)
    |   3. GAP FILL     — Eksik siparisleri doldur
    |   4. COMMISSIONS  — Komisyon cache guncelle
    |
    |── Thread B: Iadeler (bagimsiz)
    |
    |── Thread C: Q&A (bagimsiz)
    |
    v
COMPLETED — initialSyncCompleted = true
```

**Onboarding tamamlanana kadar** zamanlanmis isler bu store'u atlar (`initialSyncCompleted = false`).

---

## Trendyol API Kisitlamalari

### Rate Limiting
- **Limit**: 10 istek/saniye (store bazli)
- **Uygulama**: `TrendyolRateLimiter` (Guava RateLimiter)
- **Sayfa araligi**: 200ms bekleme (`Thread.sleep`)

### Tarih Araligi Kisitlamalari

| API | Maksimum Aralik | Uygulama |
|-----|-----------------|----------|
| Orders API | ~3 ay | 15 gunluk chunk'lara bolunur |
| OtherFinancials (Deduction) | 15 gun | 14 gunluk chunk'lara bolunur |
| Settlement API | 90 gun | Tarihsel sync workaround'u var |

### Idempotency

| Veri Tipi | Benzersizlik Kontrolu |
|-----------|-----------------------|
| Siparisler | `orderNumber` |
| Kesinti faturalari | `storeId + trendyolId` |
| Kargo faturalari | `storeId + invoiceSerialNumber + shipmentPackageId` |
| Webhook eventleri | `eventId` (sellerId + orderNumber + status + timestamp) |
| Settlement | `storeId + transactionDate + orderNumber` |

---

## Dagitik Kilit Yonetimi (ShedLock)

Tum zamanlanmis isler **ShedLock** ile korunur. Bu sayede:
- Birden fazla uygulama instance'i calistiginda is tekrarlanmaz
- Uzun suren isler icin `lockAtMostFor` ile otomatik kilit birakma
- Cok kisa calisan islerde `lockAtLeastFor` ile minimum bekleme

```java
@SchedulerLock(
    name = "dailyDeductionInvoiceSync",
    lockAtLeastFor = "5m",     // En az 5 dakika kilit tut
    lockAtMostFor = "60m"      // En fazla 60 dakika sonra birak
)
```

---

## Hata Yonetimi

### Store Bazli Izolasyon
Tum zamanlanmis islerde her store icin ayri try/catch blogu vardir. Bir store'un hatasi diger store'lari etkilemez:

```java
for (Store store : stores) {
    try {
        // sync islemi
        successCount++;
    } catch (Exception e) {
        failCount++;
        log.error("Sync failed for store {}: {}", store.getId(), e.getMessage());
    }
}
```

### Metrikler (Micrometer)
- `sellerx.orders.sync.duration` — Siparis sync suresi
- `sellerx.orders.sync` (tag: result=success|failure) — Basari/basarisizlik sayaci
- `trendyol.api.calls` — Trendyol API cagri sayisi
- `webhook.events` — Webhook event sayisi

### Loglama
- Her scheduled job baslangic ve bitis logu yazar
- Store bazli debug log'lar
- Basarisizliklar ERROR seviyesinde loglanir
- Gap analizi 6 saatte bir durum raporu verir

---

## Gorsel Ozet: Gunluk Sync Zaman Cizelgesi

```
03:00  ████ Stok Rapor + Pattern Discovery
06:15  ████████████ Full Sync (Siparis + Urun + Q&A + Iade)
07:00  ██████ Settlement Sync (gercek komisyon)
07:15  ████ Kargo Fatura Sync
07:30  ██ Komisyon Cache Guncelle
08:00  ████ Reconciliation (tahmini → gercek)
08:15  ██████ Kesinti Fatura Sync
10:00  ██ Doviz Kuru
 :00   ██████ Saatlik Catch-up (Siparis + Urun + Q&A + Iade)
 :30   ████ Gap Fill + Stok Takip
 :45   ████ Kesinti Fatura Catch-up
```

---

## Ilgili Dokumantasyon

- [STORE_ONBOARDING.md](STORE_ONBOARDING.md) — Magaza onboarding akisi
- [COMMISSION_SYSTEM.md](COMMISSION_SYSTEM.md) — Tahmini vs gercek komisyon
- [WEBHOOK_SYSTEM.md](WEBHOOK_SYSTEM.md) — Webhook isleme detaylari
- [HISTORICAL_SETTLEMENT_SYNC.md](HISTORICAL_SETTLEMENT_SYNC.md) — 90 gun API limiti workaround
- [RATE_LIMITING.md](RATE_LIMITING.md) — Trendyol API rate limiting
- [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) — Veritabani semasi
