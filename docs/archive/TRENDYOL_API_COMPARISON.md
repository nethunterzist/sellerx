# Trendyol API Karşılaştırma Raporu

**Tarih**: 2026-01-23
**Platform Amacı**: Gelir-Gider Takibi ve Karlılık Analizi

---

## Platform Kapsamı

SellerX bir **satış yönetim aracı değil**, **finansal analiz platformudur**:

- ✅ Satışları ve siparişleri izleme
- ✅ Komisyon ve kesintileri hesaplama
- ✅ Kar/zarar analizi
- ✅ Maliyet takibi (ürün maliyetleri)
- ✅ Gider yönetimi
- ✅ Finansal raporlama

**Kapsam Dışı** (Satıcılar Trendyol'dan yapıyor):
- ❌ Ürün ekleme/güncelleme/silme
- ❌ Fiyat ve stok güncelleme
- ❌ Kargo işlemleri ve etiket oluşturma
- ❌ E-fatura gönderimi

---

## MEVCUT API'LER - Tam Kapsam ✅

### 1. Sipariş Verisi (Satış Takibi)

| Endpoint | Dosya | Kullanım |
|----------|-------|----------|
| `GET /integration/order/sellers/{sellerId}/orders` | `TrendyolOrderService.java` | Tüm siparişleri çekme |

**Çekilen Veriler**:
- Sipariş tutarı (brüt, net, indirimler)
- Sipariş durumu
- Ürün detayları
- Sipariş tarihi
- Şehir/ilçe bilgisi

---

### 2. Finansal Mutabakat (Gerçek Komisyon Verileri) ⭐

| Endpoint | Dosya | Kullanım |
|----------|-------|----------|
| `GET /integration/finance/che/sellers/{sellerId}/settlements` | `TrendyolFinancialSettlementService.java` | Satış, iade, indirim, kupon, erken ödeme |
| `GET /integration/finance/che/sellers/{sellerId}/otherfinancials` | `TrendyolOtherFinancialsService.java` | Tevkifat, hak ediş, kesinti faturaları |
| `GET /integration/finance/che/sellers/{sellerId}/cargo-invoice/{serial}/items` | `TrendyolOtherFinancialsService.java` | Kargo fatura detayları |

**Çekilen Veriler**:
- **Sale**: Satış geliri, komisyon, KDV
- **Return**: İade kesintileri
- **Discount**: Satıcı indirimleri
- **Coupon**: Kupon maliyetleri
- **EarlyPayment**: Erken ödeme kesintileri
- **Stoppage**: Tevkifat tutarları
- **PaymentOrder**: Hak ediş ödemeleri
- **DeductionInvoices**: Kesinti faturaları
- **CargoInvoice**: Kargo maliyetleri

---

### 3. Ürün Verisi (Maliyet Girişi İçin)

| Endpoint | Dosya | Kullanım |
|----------|-------|----------|
| `GET /integration/product/sellers/{sellerId}/products` | `TrendyolProductService.java` | Ürün listesi ve komisyon oranları |

**Çekilen Veriler**:
- Ürün bilgileri (barkod, isim, fiyat)
- Komisyon oranı (tahmini)
- KDV oranı
- Kargo hacim ağırlığı

**SellerX'te Eklenen**:
- Maliyet bilgisi (FIFO ile takip)
- Stok maliyeti geçmişi

---

### 4. İade Verileri (Zarar Takibi)

| Endpoint | Dosya | Kullanım |
|----------|-------|----------|
| `GET /integration/order/sellers/{sellerId}/claims` | `TrendyolClaimsService.java` | İade talepleri |
| `PUT .../claims/{claimId}/items/approve` | `TrendyolClaimsService.java` | İade onaylama |
| `POST .../claims/{claimId}/issue` | `TrendyolClaimsService.java` | İade reddetme |

**Çekilen Veriler**:
- İade tutarı
- İade nedeni
- İade durumu

---

### 5. Müşteri Soruları (Opsiyonel)

| Endpoint | Dosya | Kullanım |
|----------|-------|----------|
| `GET /integration/qna/sellers/{sellerId}/questions/filter` | `TrendyolQaService.java` | Müşteri soruları |
| `POST .../questions/{questionId}/answers` | `TrendyolQaService.java` | Cevap gönderme |

---

### 6. Webhook (Gerçek Zamanlı Güncelleme)

| Endpoint | Dosya | Kullanım |
|----------|-------|----------|
| `POST /integration/webhook/sellers/{sellerId}/webhooks` | `TrendyolWebhookManagementService.java` | Webhook oluşturma |
| `DELETE .../webhooks/{webhookId}` | `TrendyolWebhookManagementService.java` | Webhook silme |

**Alınan Bildirimler**:
- Sipariş durumu değişiklikleri
- Yeni siparişler

---

## GELİR-GİDER TAKİBİ İÇİN VERİ AKIŞI

```
┌─────────────────────────────────────────────────────────────────┐
│                        GELİR KAYNAKLARI                         │
├─────────────────────────────────────────────────────────────────┤
│  Orders API          → Brüt satış tutarı                        │
│  Settlements API     → Net satış (komisyon sonrası)             │
│  PaymentOrder        → Hak ediş ödemeleri                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        GİDER KALEMLERİ                          │
├─────────────────────────────────────────────────────────────────┤
│  Settlements API     → Komisyon kesintisi                       │
│  Settlements API     → İade kesintileri                         │
│  Settlements API     → Kupon maliyetleri                        │
│  Settlements API     → Erken ödeme kesintileri                  │
│  OtherFinancials     → Tevkifat (Stoppage)                      │
│  OtherFinancials     → Kesinti faturaları                       │
│  CargoInvoice        → Kargo maliyetleri                        │
│  Products (manuel)   → Ürün maliyetleri (FIFO)                  │
│  StoreExpenses       → Diğer giderler (manuel giriş)            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      NET KAR HESAPLAMA                          │
├─────────────────────────────────────────────────────────────────┤
│  Net Kar = Brüt Satış                                           │
│          - Komisyon                                             │
│          - Ürün Maliyeti                                        │
│          - Kargo Maliyeti                                       │
│          - Tevkifat                                             │
│          - İade Zararı                                          │
│          - Kupon Maliyeti                                       │
│          - Diğer Giderler                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## EKSİK OLAN / GELİŞTİRİLEBİLECEK API'LER

### 🟡 Reklam Harcamaları (Trendyol Ads)

**Durum**: Trendyol'un reklam API'si henüz SellerX'e entegre değil.

| Potansiyel Endpoint | Açıklama |
|---------------------|----------|
| Reklam kampanyaları | Kampanya bazlı harcamalar |
| Reklam performansı | Tıklama, gösterim, dönüşüm |
| ROAS hesaplama | Reklam getirisi |

**Not**: Trendyol Ads API'si için ayrı bir başvuru gerekebilir.

---

### 🟡 Daha Detaylı Kargo Verileri

| Potansiyel Veri | Açıklama |
|-----------------|----------|
| Kargo desi bilgisi | Paket bazlı desi |
| Kargo firması | Hangi firma ile gönderildi |
| Teslimat süresi | Ortalama teslimat süresi |

---

### 🟢 Webhook Yönetimi (Opsiyonel)

| Endpoint | Açıklama |
|----------|----------|
| `GET .../webhooks` | Mevcut webhook listesi |
| `PUT .../webhooks/{id}/activate` | Webhook aktif etme |
| `PUT .../webhooks/{id}/deactivate` | Webhook devre dışı bırakma |

---

## MEVCUT SİSTEM YETERLİLİĞİ

| Alan | Durum | Açıklama |
|------|-------|----------|
| Satış takibi | ✅ Tam | Orders API ile çekiliyor |
| Komisyon hesaplama | ✅ Tam | Settlements API ile gerçek veriler |
| İade takibi | ✅ Tam | Claims API + Settlements Return |
| Maliyet takibi | ✅ Tam | Manuel giriş + FIFO hesaplama |
| Kargo maliyeti | ✅ Tam | Cargo Invoice API |
| Tevkifat | ✅ Tam | OtherFinancials Stoppage |
| Hak ediş | ✅ Tam | OtherFinancials PaymentOrder |
| Gider yönetimi | ✅ Tam | StoreExpenses (manuel) |
| Reklam harcamaları | ⚠️ Eksik | Trendyol Ads API entegrasyonu yok |

---

## SONUÇ

**SellerX, gelir-gider takibi ve karlılık analizi için gerekli tüm kritik Trendyol API'lerine sahip.**

Mevcut entegrasyonlar:
- ✅ Sipariş verileri (satış geliri)
- ✅ Finansal mutabakat (gerçek komisyon, kesintiler)
- ✅ Kargo faturaları (kargo maliyeti)
- ✅ İade verileri (zarar takibi)
- ✅ Ürün verileri (maliyet girişi)

**Tek potansiyel eksik**: Trendyol Ads (reklam harcamaları) API entegrasyonu.

---

*Bu rapor 2026-01-23 tarihinde, SellerX'in gelir-gider takip platformu olduğu göz önünde bulundurularak güncellenmiştir.*
