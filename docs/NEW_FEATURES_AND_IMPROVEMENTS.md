# SellerX - Yeni Özellikler ve İyileştirmeler

> **Tarih:** 22 Ocak 2026
> **Versiyon:** 1.0
> **Durum:** Planlama Aşaması

---

## İçindekiler

1. [Sistem Özeti](#1-sistem-özeti)
2. [Kritik Düzeltmeler (P0)](#2-kritik-düzeltmeler-p0)
3. [Yüksek Öncelikli İyileştirmeler (P1)](#3-yüksek-öncelikli-iyileştirmeler-p1)
4. [Orta Öncelikli İyileştirmeler (P2)](#4-orta-öncelikli-iyileştirmeler-p2)
5. [Düşük Öncelikli İyileştirmeler (P3)](#5-düşük-öncelikli-iyileştirmeler-p3)
6. [Yeni Özellik Önerileri](#6-yeni-özellik-önerileri)
7. [Teknik Borç](#7-teknik-borç)
8. [Uygulama Yol Haritası](#8-uygulama-yol-haritası)

---

## 1. Sistem Özeti

### 1.1 Mevcut Durum

| Metrik | Backend | Frontend |
|--------|---------|----------|
| **Teknoloji** | Spring Boot 3.4.4 + Java 21 | Next.js 15 + React 19 + TypeScript |
| **Dosya Sayısı** | 270+ Java dosyası | 8,990+ TS/TSX dosyası |
| **LOC (Kod Satırı)** | 31,829 | ~50,000+ |
| **Modül Sayısı** | 18 | 14 |
| **Test Coverage** | ~5% (3 test dosyası) | 0% |
| **Veritabanı** | PostgreSQL 15 | - |
| **Migration Sayısı** | 58 Flyway migration | - |
| **Tamamlanma Oranı** | ~85% | ~80% |

### 1.2 Mevcut Modüller

#### Backend Modülleri
| Modül | Durum | Açıklama |
|-------|-------|----------|
| `auth` | ✅ 100% | JWT authentication, refresh token |
| `users` | ✅ 100% | Kullanıcı yönetimi, tercihler |
| `stores` | ✅ 95% | Mağaza yönetimi, paralel sync |
| `products` | ✅ 90% | Ürün sync, maliyet geçmişi (JSONB) |
| `orders` | ✅ 95% | Sipariş sync, FIFO maliyet |
| `dashboard` | ⚠️ 80% | Çoklu dönem istatistikleri |
| `financial` | ✅ 90% | Settlement, komisyon reconciliation |
| `expenses` | ✅ 100% | Gider takibi, KDV mahsuplaşması |
| `purchasing` | ✅ 95% | Satın alma siparişleri, raporlar |
| `returns` | ⚠️ 75% | İade takibi, analitik |
| `qa` | ⚠️ 70% | Müşteri soruları, AI entegrasyonu |
| `ads` | ⚠️ 85% | Reklam raporları |
| `webhook` | ⚠️ 85% | Real-time event handling |
| `billing` | ⚠️ 60% | Abonelik, ödeme (iyzico) |
| `ai` | ⚠️ 60% | OpenAI entegrasyonu |
| `education` | ✅ 90% | Eğitim videoları |
| `activity_log` | ✅ 100% | Audit trail |
| `currency` | ✅ 100% | Döviz kurları |

#### Frontend Sayfaları
| Sayfa | Durum | Açıklama |
|-------|-------|----------|
| `/dashboard` | ✅ 95% | 5 görünüm (Tiles, Chart, P&L, Trends, Cities) |
| `/products` | ✅ 90% | Ürün listesi, maliyet düzenleme, Excel import/export |
| `/orders` | ✅ 90% | Sipariş listesi, filtreleme, sync |
| `/expenses` | ✅ 95% | Gider CRUD, grafikler |
| `/purchasing` | ✅ 95% | PO yönetimi, raporlar |
| `/returns` | ⚠️ 85% | İade talepleri, analitik |
| `/qa` | ⚠️ 80% | Sorular, AI önerileri, çakışmalar |
| `/ads` | ⚠️ 85% | Reklam analizi, Excel import |
| `/financial` | ⚠️ 80% | Settlement, KDV |
| `/billing` | ⚠️ 70% | Abonelik, ödeme yöntemleri |
| `/analytics` | ⚠️ 75% | Gelişmiş analitik |
| `/settings` | ✅ 90% | 9 tab'lı ayarlar |
| `/profile` | ⚠️ 50% | Minimal implementasyon |

### 1.3 Güçlü Yanlar

1. **Paralel Sync Mimarisi** - Store onboarding 15-20 dakikada tamamlanıyor (45 dk yerine)
2. **FIFO Maliyet Hesaplama** - Profesyonel seviyede stok maliyet takibi
3. **Komisyon Reconciliation** - Tahmini vs gerçek komisyon karşılaştırması
4. **Rate Limiting** - Trendyol API'ye güvenli erişim (10 req/sec)
5. **Webhook Altyapısı** - Real-time sipariş güncellemeleri
6. **Modern UI** - Shadcn/ui + Tailwind ile profesyonel arayüz
7. **i18n Desteği** - Türkçe/İngilizce
8. **React Query** - Akıllı cache ve veri yönetimi

---

## 2. Kritik Düzeltmeler (P0)

### 2.1 Net Kar Hesaplama Hatası

**Durum:** ❌ KRİTİK
**Konum:** `sellerx-backend/src/main/java/com/ecommerce/sellerx/dashboard/DashboardStatsService.java`

#### Problem
Mevcut kar hesaplama eksik kalemler içeriyor:

```java
// MEVCUT (YANLIŞ)
grossProfit = revenue - productCost

// OLMASI GEREKEN
netProfit = revenue
          - productCost
          - commission
          - shippingCost
          - returnCost
          - stoppage
          - expenses
```

#### Eksik Kalemler
| Kalem | Durum | Açıklama |
|-------|-------|----------|
| Ürün Maliyeti | ✅ | FIFO ile hesaplanıyor |
| Komisyon | ⚠️ | Var ama düşülmüyor |
| Kargo (Müşteriye) | ❌ | Hiç hesaplanmıyor |
| İade Maliyeti | ⚠️ | 50 TL sabit kodlanmış |
| Stopaj | ⚠️ | Var ama her zaman düşülmüyor |
| Giderler | ⚠️ | Ayrı gösteriliyor, kar'dan düşülmüyor |
| Ambalaj | ❌ | Hiç hesaplanmıyor |

#### Çözüm Adımları
```
1. DashboardStatsService.java'da calculateStats metodunu güncelle
2. Tüm maliyet kalemlerini topla
3. Net kar = Gelir - Tüm Maliyetler
4. Frontend'de ayrı ayrı göster
5. Test yaz
```

#### Tahmini Süre: 2-3 gün

---

### 2.2 Test Coverage = 0%

**Durum:** ❌ KRİTİK
**Risk:** Production'da bug'lar geç fark ediliyor

#### Mevcut Test Dosyaları
```
Backend:
- StoreRepositoryTest.java (sadece bu var)
- Toplam: 3 test dosyası

Frontend:
- Hiç test yok
```

#### Hedef Coverage
| Modül | Hedef | Öncelik |
|-------|-------|---------|
| Auth | %90 | P0 |
| Orders (FIFO) | %85 | P0 |
| Financial | %80 | P0 |
| Dashboard Stats | %80 | P1 |
| Products | %70 | P1 |
| Billing | %90 | P1 |

#### Çözüm Adımları

**Backend:**
```
1. JUnit 5 + Mockito yapılandırması
2. Test container'ları (PostgreSQL)
3. Servis testleri (unit)
4. Repository testleri (integration)
5. Controller testleri (MockMvc)
```

**Frontend:**
```
1. Jest + React Testing Library kurulumu
2. Hook testleri
3. Component testleri
4. E2E testleri (Playwright)
```

#### Tahmini Süre: 1-2 hafta

---

### 2.3 İade Maliyeti Sabit Kodlanmış

**Durum:** ❌ KRİTİK
**Konum:** `DashboardStatsService.java:47`

```java
// MEVCUT (YANLIŞ)
private static final double RETURN_COST_PER_ITEM = 50.0; // Hardcoded!

// OLMASI GEREKEN
// Dinamik hesaplama:
// - Ürün maliyeti
// - Gidiş kargo
// - Dönüş kargo
// - Komisyon kaybı
// - Ambalaj
```

#### Çözüm
```
1. ReturnRecord entity'sinde maliyet breakdown'ı zaten var
2. Bu verileri kullan
3. Ürün bazlı iade maliyeti hesapla
4. Dashboard'da doğru göster
```

#### Tahmini Süre: 1-2 gün

---

## 3. Yüksek Öncelikli İyileştirmeler (P1)

### 3.1 iyzico Ödeme Entegrasyonu

**Durum:** ⚠️ %40 tamamlanmış
**Konum:** `sellerx-backend/src/main/java/com/ecommerce/sellerx/billing/iyzico/`

#### Mevcut Durum
- Framework mevcut (11 dosya)
- API çağrıları implement edilmemiş
- 3D Secure tamamlanmamış

#### Eksikler
```
1. iyzico API credentials konfigürasyonu
2. 3D Secure completion logic
3. Webhook handler (ödeme bildirimleri)
4. Retry logic (başarısız ödemeler)
5. Refund işlemleri
6. Subscription billing automation
```

#### TODO'lar (kodda)
```java
// BillingCheckoutService.java
// TODO: Implement 3DS completion logic with iyzico

// InvoiceService.java
// TODO: Generate or retrieve PDF URL from storage
```

#### Çözüm Adımları
```
1. iyzico sandbox hesabı kur
2. API credentials'ı environment'a ekle
3. Payment service'i implement et
4. 3DS flow'u tamamla
5. Webhook endpoint'i ekle
6. Test et (sandbox)
7. Production'a geç
```

#### Tahmini Süre: 1 hafta

---

### 3.2 TypeScript Strict Mode

**Durum:** ⚠️ Kapalı
**Konum:** `sellerx-frontend/tsconfig.json`

#### Mevcut Konfigürasyon
```json
{
  "compilerOptions": {
    "strict": false,           // ❌ Kapalı
    "noImplicitAny": false,    // ❌ any'ye izin veriyor
  }
}
```

```javascript
// next.config.ts
typescript: {
  ignoreBuildErrors: true,  // ❌ Build hataları görmezden geliniyor
}
```

#### Risk
- Runtime type hataları
- Null/undefined hataları
- Missing property hataları

#### Çözüm Adımları
```
1. strict: true yap
2. Build hatalarını listele
3. Dosya dosya düzelt
4. ignoreBuildErrors: false yap
5. CI'da type check ekle
```

#### Tahmini Süre: 3-5 gün

---

### 3.3 AI Cevap Sistemi Aktivasyonu

**Durum:** ⚠️ %60 tamamlanmış
**Konum:** `sellerx-backend/src/main/java/com/ecommerce/sellerx/ai/`

#### Mevcut Durum
```java
// TrendyolQaService.java:212
// TODO: Enable actual submission when AI feature is ready

// TrendyolQaService.java:272
// TODO: Enable actual submission when AI feature is ready
```

#### Framework Mevcut
- `OpenAiService.java` - GPT entegrasyonu
- `AiContextBuilder.java` - Prompt oluşturma
- `AiAnswerService.java` - Cevap üretme
- `StoreAiSettings.java` - Mağaza bazlı ayarlar

#### Eksikler
```
1. OpenAI API key konfigürasyonu
2. Rate limiting (mağaza başına)
3. Cevap onay workflow'u
4. Human feedback loop
5. Cost tracking (token usage)
```

#### Çözüm Adımları
```
1. OpenAI API key'i environment'a ekle
2. AiAnswerService'i test et
3. Frontend'de onay UI'ı ekle
4. Auto-submit özelliğini aktif et
5. Token usage tracking ekle
```

#### Tahmini Süre: 3-5 gün

---

### 3.4 PDF Fatura Üretimi

**Durum:** ⚠️ Eksik
**Konum:** `sellerx-backend/src/main/java/com/ecommerce/sellerx/billing/InvoiceService.java`

```java
// TODO: Generate or retrieve PDF URL from storage
```

#### Çözüm Seçenekleri
1. **iTextPDF** - Java PDF kütüphanesi
2. **JasperReports** - Template-based
3. **Flying Saucer** - HTML to PDF

#### Gereksinimler
```
1. Fatura şablonu (HTML/template)
2. PDF generator service
3. Storage (S3 veya local)
4. Download endpoint
5. E-fatura entegrasyonu (GIB)
```

#### Tahmini Süre: 3-5 gün

---

## 4. Orta Öncelikli İyileştirmeler (P2)

### 4.1 Webhook Yönetim UI

**Durum:** Backend var, frontend yok

#### Mevcut Backend
```
- WebhookManagementController.java
- Enable/disable endpoints
- Test endpoint
- Event history
```

#### Eksik Frontend
```
Settings > Webhooks sayfasına eklenecek:
1. Webhook durumu (aktif/pasif toggle)
2. Son 50 event listesi
3. Event detay modal
4. Test webhook butonu
5. Webhook URL gösterimi
```

#### Tahmini Süre: 2-3 gün

---

### 4.2 İade Sebep Analizi

**Durum:** ⚠️ Eksik
**Konum:** `ReturnAnalyticsService.java:231`

```java
// TODO: Add reason tracking
```

#### Eklenecekler
```
1. İade sebebi kategorileri
2. Sebep bazlı istatistikler
3. Ürün bazlı iade sebepleri
4. Trend analizi
5. Öneri sistemi (yüksek iade oranı uyarısı)
```

#### Tahmini Süre: 2-3 gün

---

### 4.3 Stok Tahmini (Forecasting)

**Durum:** ❌ Yok

#### Özellikler
```
1. Satış hızı hesaplama (son 7/30/90 gün)
2. "X gün sonra biter" tahmini
3. Reorder point (yeniden sipariş noktası)
4. Mevsimsellik analizi
5. Düşük stok uyarıları
```

#### Algoritma
```
dailySalesRate = totalSold / numberOfDays
daysUntilStockout = currentStock / dailySalesRate
reorderPoint = dailySalesRate * leadTimeDays * safetyFactor
```

#### Tahmini Süre: 1 hafta

---

### 4.4 Rapor Dışa Aktarma

**Durum:** ⚠️ Sadece Excel import var

#### Eklenecek Export Formatları
```
1. PDF rapor (dashboard, P&L, stok değerleme)
2. CSV export (ürünler, siparişler, giderler)
3. Excel export (detaylı raporlar)
4. API export (JSON)
```

#### Zamanlanmış Raporlar
```
1. Günlük özet email
2. Haftalık performans raporu
3. Aylık P&L raporu
4. Özel tarih aralığı
```

#### Tahmini Süre: 1 hafta

---

### 4.5 Bildirim Merkezi

**Durum:** ⚠️ Minimal implementasyon

#### Bildirim Türleri
```
1. Düşük stok uyarısı
2. Yüksek iade oranı uyarısı
3. Sipariş durumu değişikliği
4. Ödeme bildirimleri
5. Komisyon değişikliği
6. Sync hataları
7. Webhook hataları
```

#### Kanallar
```
1. In-app notifications
2. Email
3. Push notification (opsiyonel)
4. SMS (opsiyonel)
```

#### Tahmini Süre: 1 hafta

---

## 5. Düşük Öncelikli İyileştirmeler (P3)

### 5.1 Redis Cache

**Durum:** ❌ Yok

#### Cache Edilecek Veriler
```
1. Ürün listesi (5 dakika TTL)
2. Dashboard istatistikleri (1 dakika TTL)
3. Komisyon oranları (1 saat TTL)
4. Kategori listesi (1 gün TTL)
5. Döviz kurları (1 saat TTL)
```

#### Beklenen İyileşme
- API response time: %50-70 azalma
- Database load: %40-60 azalma

#### Tahmini Süre: 3-5 gün

---

### 5.2 Mobile Optimizasyon

**Durum:** ⚠️ Kısmi

#### Eksikler
```
1. Dashboard mobile view
2. Hamburger menu
3. Touch-friendly butonlar
4. Swipe gestures
5. Mobile-specific navigation
```

#### Tahmini Süre: 1 hafta

---

### 5.3 Performance Optimizasyonu

#### Frontend
```
1. Bundle size analizi (webpack-bundle-analyzer)
2. Code splitting per page
3. Virtual scrolling (uzun listeler)
4. Image lazy loading
5. Chart memoization
```

#### Backend
```
1. Query optimization
2. N+1 query fix
3. Index ekleme
4. Connection pooling tune
5. Async processing genişletme
```

#### Tahmini Süre: 1 hafta

---

### 5.4 Error Tracking (Sentry)

**Durum:** ❌ Yok

#### Eklenecekler
```
1. Frontend Sentry SDK
2. Backend Sentry SDK
3. Error grouping
4. Performance monitoring
5. Release tracking
6. User feedback
```

#### Tahmini Süre: 2-3 gün

---

### 5.5 API Dokümantasyonu

**Durum:** ❌ Yok

#### Seçenekler
```
1. Swagger/OpenAPI (Spring Boot)
2. Postman collection
3. API Blueprint
```

#### Tahmini Süre: 3-5 gün

---

## 6. Yeni Özellik Önerileri

### 6.1 Gelişmiş Karlılık Dashboard'u

**Öncelik:** Yüksek
**Değer:** Satıcıların gerçek karını görmesi

#### Özellikler
```
Her ürün için:
├── Satış fiyatı
├── Maliyet (FIFO)
├── Komisyon
├── Kargo maliyeti
├── Net kar
├── Kar marjı %
├── Trend (↑↓→)
└── ROI
```

#### Görünümler
```
1. Ürün bazlı kar tablosu
2. Kategori bazlı kar grafiği
3. Zaman serisi (günlük/haftalık/aylık)
4. Kar/zarar analizi
5. Break-even analizi
```

---

### 6.2 Rekabet Analizi

**Öncelik:** Orta
**Değer:** Pazar pozisyonu anlama

#### Özellikler
```
1. Rakip fiyat takibi (scraping veya API)
2. Fiyat karşılaştırma grafikleri
3. Pazar payı tahmini
4. Fiyat optimizasyon önerileri
5. Trend analizi
```

#### Not
Trendyol API'de rakip fiyat verisi yok, harici çözüm gerekebilir.

---

### 6.3 Otomatik Fiyatlandırma

**Öncelik:** Orta
**Değer:** Dinamik fiyat optimizasyonu

#### Özellikler
```
1. Minimum kar marjı belirleme
2. Stok durumuna göre fiyat ayarlama
   - Düşük stok → Fiyat artır
   - Yüksek stok → İndirim öner
3. Kampanya dönemlerinde otomatik indirim
4. Rakip fiyatına göre ayarlama
5. A/B test (farklı fiyatlar)
```

#### Kurallar
```yaml
rules:
  - if: stock < 10 AND demand = high
    action: increase_price 5%
  - if: stock > 100 AND days_in_stock > 60
    action: suggest_discount 10%
  - if: competitor_price < my_price - 10%
    action: alert_user
```

---

### 6.4 Tedarikçi Yönetimi

**Öncelik:** Orta
**Değer:** Tedarik zinciri optimizasyonu

#### Özellikler
```
1. Tedarikçi CRUD
2. Tedarikçi bazlı PO takibi
3. Performans skorları
   - Teslimat süresi
   - Kalite (iade oranı)
   - Fiyat rekabetçiliği
4. Fiyat karşılaştırma
5. Lead time takibi
6. Tedarikçi bazlı raporlar
```

---

### 6.5 Çoklu Pazar Yeri Desteği

**Öncelik:** Düşük (gelecek versiyon)
**Değer:** Tek panelden tüm pazarları yönetme

#### Platformlar
```
1. Hepsiburada
2. Amazon TR
3. N11
4. GittiGidiyor
5. Çiçeksepeti
```

#### Mimari Değişiklik
```
Store entity → Platform enum (TRENDYOL, HEPSIBURADA, AMAZON, etc.)
Her platform için ayrı:
- API client
- Sync service
- Webhook handler
- Rate limiter
```

---

### 6.6 Mobil Uygulama

**Öncelik:** Düşük (gelecek versiyon)
**Değer:** Hareket halinde yönetim

#### Teknoloji Seçenekleri
```
1. React Native (kod paylaşımı)
2. Flutter
3. Native (iOS/Android)
```

#### MVP Özellikler
```
1. Dashboard görüntüleme
2. Sipariş listesi
3. Push bildirimler
4. Hızlı stok güncelleme
```

---

### 6.7 Entegrasyonlar

#### E-Fatura (GIB)
```
- Otomatik e-fatura oluşturma
- GIB'e gönderme
- Fatura arşivleme
```

#### Muhasebe Yazılımları
```
- Parasut entegrasyonu
- Logo entegrasyonu
- Mikro entegrasyonu
```

#### Kargo Firmaları
```
- Yurtiçi Kargo API
- Aras Kargo API
- MNG Kargo API
- Otomatik kargo takibi
```

---

## 7. Teknik Borç

### 7.1 Kod Kalitesi

| Sorun | Dosya/Konum | Öncelik |
|-------|-------------|---------|
| TypeScript strict: false | tsconfig.json | P1 |
| ignoreBuildErrors: true | next.config.ts | P1 |
| Test coverage 0% | Tüm proje | P0 |
| Hardcoded return cost | DashboardStatsService:47 | P0 |
| TODO comments | 15+ lokasyon | P2 |
| N+1 queries | Order fetch | P3 |
| Missing indexes | Bazı tablolar | P3 |

### 7.2 Güvenlik

| Sorun | Açıklama | Öncelik |
|-------|----------|---------|
| Webhook signature optional | Zorunlu olmalı | P2 |
| No per-user rate limiting | Sadece global var | P2 |
| Missing audit for sensitive ops | Password change, etc. | P2 |
| JWT secret min length | 32 karakter zorunlu ama hata mesajı belirsiz | P3 |

### 7.3 Performance

| Sorun | Açıklama | Öncelik |
|-------|----------|---------|
| No caching | Redis yok | P3 |
| No virtual scrolling | Uzun listeler yavaş | P3 |
| Multiple chart re-renders | Memoization eksik | P3 |
| Large bundle size | Analiz yapılmamış | P3 |

---

## 8. Uygulama Yol Haritası

### Faz 1: Kritik Düzeltmeler (1-2 Hafta)

```
Hafta 1:
├── [ ] Net kar hesaplama düzeltmesi
├── [ ] İade maliyeti dinamik yapma
├── [ ] Test altyapısı kurulumu (Jest, JUnit)
└── [ ] Kritik modüller için testler

Hafta 2:
├── [ ] TypeScript strict mode
├── [ ] Build hatalarını düzeltme
└── [ ] CI/CD'ye test ekleme
```

### Faz 2: Ödeme ve AI (1-2 Hafta)

```
Hafta 3:
├── [ ] iyzico sandbox kurulumu
├── [ ] 3D Secure implementasyonu
├── [ ] Ödeme webhook'u
└── [ ] Subscription automation

Hafta 4:
├── [ ] AI cevap sistemi aktivasyonu
├── [ ] OpenAI API entegrasyonu
├── [ ] Cevap onay workflow'u
└── [ ] PDF fatura üretimi
```

### Faz 3: UI/UX İyileştirmeleri (1 Hafta)

```
Hafta 5:
├── [ ] Webhook yönetim UI
├── [ ] Bildirim merkezi
├── [ ] İade sebep analizi
├── [ ] Mobile responsive düzeltmeleri
└── [ ] Error tracking (Sentry)
```

### Faz 4: Analitik ve Raporlama (1-2 Hafta)

```
Hafta 6-7:
├── [ ] Stok tahmini (forecasting)
├── [ ] Gelişmiş karlılık dashboard'u
├── [ ] Rapor dışa aktarma (PDF, CSV)
├── [ ] Zamanlanmış raporlar
└── [ ] API dokümantasyonu
```

### Faz 5: Performans ve Optimizasyon (1 Hafta)

```
Hafta 8:
├── [ ] Redis cache entegrasyonu
├── [ ] Query optimizasyonu
├── [ ] Bundle size optimizasyonu
├── [ ] Virtual scrolling
└── [ ] Performance monitoring
```

### Faz 6: Yeni Özellikler (2-4 Hafta)

```
Hafta 9-12:
├── [ ] Tedarikçi yönetimi
├── [ ] Otomatik fiyatlandırma kuralları
├── [ ] Rekabet analizi (opsiyonel)
├── [ ] E-fatura entegrasyonu
└── [ ] Muhasebe yazılımı entegrasyonu
```

---

## Öncelik Özet Tablosu

| ID | Görev | Öncelik | Süre | Etki |
|----|-------|---------|------|------|
| 2.1 | Net kar hesaplama | P0 | 2-3 gün | Kritik |
| 2.2 | Test coverage | P0 | 1-2 hafta | Kritik |
| 2.3 | İade maliyeti fix | P0 | 1-2 gün | Kritik |
| 3.1 | iyzico entegrasyonu | P1 | 1 hafta | Yüksek |
| 3.2 | TypeScript strict | P1 | 3-5 gün | Yüksek |
| 3.3 | AI aktivasyonu | P1 | 3-5 gün | Yüksek |
| 3.4 | PDF fatura | P1 | 3-5 gün | Yüksek |
| 4.1 | Webhook UI | P2 | 2-3 gün | Orta |
| 4.2 | İade sebep analizi | P2 | 2-3 gün | Orta |
| 4.3 | Stok tahmini | P2 | 1 hafta | Orta |
| 4.4 | Rapor export | P2 | 1 hafta | Orta |
| 4.5 | Bildirim merkezi | P2 | 1 hafta | Orta |
| 5.1 | Redis cache | P3 | 3-5 gün | Düşük |
| 5.2 | Mobile optimize | P3 | 1 hafta | Düşük |
| 5.3 | Performance | P3 | 1 hafta | Düşük |
| 5.4 | Sentry | P3 | 2-3 gün | Düşük |
| 5.5 | API docs | P3 | 3-5 gün | Düşük |

---

## Notlar

- Bu döküman canlıdır ve güncellenecektir
- Her görev tamamlandığında checkbox işaretlenecek
- Öncelikler iş ihtiyaçlarına göre değişebilir
- Tahmini süreler tek geliştirici içindir

---

*Son güncelleme: 22 Ocak 2026*
