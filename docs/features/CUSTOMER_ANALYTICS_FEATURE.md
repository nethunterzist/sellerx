# Müşteri Analizi Özelliği - CTO Sunum Dokümanı

**Tarih**: Şubat 2026
**Hazırlayan**: SellerX Geliştirme Ekibi
**Durum**: Onay Bekliyor

---

## 1. YÖNETİCİ ÖZETİ

### Problem
Mevcut sistemde "tekrar müşteri" hesaplaması **ürün adedi** bazında yapılıyordu. Bu, tek siparişte birden fazla ürün alan müşterileri yanlışlıkla "tekrar müşteri" olarak sınıflandırıyordu.

### Çözüm
Algoritma **sipariş sayısı** bazlı olarak düzeltildi. Artık bir müşterinin "tekrar müşteri" sayılması için **en az 2 ayrı sipariş** vermesi gerekiyor.

### Etki
| Metrik | Eski (Yanlış) | Yeni (Doğru) | Fark |
|--------|---------------|--------------|------|
| Tekrar Müşteri | 4,502 | 1,223 | ↓ %73 |
| Tekrar Oranı | %34.4 | %9.33 | ↓ 25 puan |

**Not**: Düşüş beklenen bir sonuçtur. Yeni metrik gerçek müşteri sadakatini yansıtmaktadır.

---

## 2. PROBLEM TANIMI

### 2.1 Mevcut Durum (Hatalı)

```
Eski Algoritma: item_count > 1

Örnek Senaryo:
┌─────────────┬─────────────┬────────────┬─────────────────┐
│ Müşteri     │ Sipariş     │ Ürün Adedi │ Eski Sonuç      │
├─────────────┼─────────────┼────────────┼─────────────────┤
│ Ahmet       │ 1 sipariş   │ 5 ürün     │ ✅ Tekrar (!)   │
│ Mehmet      │ 1 sipariş   │ 3 ürün     │ ✅ Tekrar (!)   │
│ Ayşe        │ 2 sipariş   │ 2 ürün     │ ✅ Tekrar       │
└─────────────┴─────────────┴────────────┴─────────────────┘

Sorun: Ahmet ve Mehmet hiç geri dönmedi ama "tekrar müşteri" sayıldı!
```

### 2.2 İş Etkisi

| Etkilenen Alan | Sonuç |
|----------------|-------|
| **Pazarlama Kararları** | Yanlış retention metrikleri → hatalı kampanya stratejileri |
| **Müşteri Segmentasyonu** | Şişirilmiş sadakat oranları → gerçekçi olmayan hedefler |
| **Yönetim Raporları** | Yanıltıcı KPI'lar → hatalı stratejik kararlar |
| **Ürün Analizi** | Hangi ürünler sadakat yaratıyor? → bilinmiyor |

---

## 3. ÇÖZÜM

### 3.1 Yeni Algoritma

```
Yeni Algoritma: order_count >= 2

Tanım: Bir müşteri, EN AZ 2 AYRI SİPARİŞ verdiyse "tekrar müşteri"dir.

Örnek Senaryo:
┌─────────────┬─────────────┬────────────┬─────────────────┐
│ Müşteri     │ Sipariş     │ Ürün Adedi │ Yeni Sonuç      │
├─────────────┼─────────────┼────────────┼─────────────────┤
│ Ahmet       │ 1 sipariş   │ 5 ürün     │ ❌ Tek seferlik │
│ Mehmet      │ 1 sipariş   │ 3 ürün     │ ❌ Tek seferlik │
│ Ayşe        │ 2 sipariş   │ 2 ürün     │ ✅ Tekrar       │
└─────────────┴─────────────┴────────────┴─────────────────┘

Sonuç: Sadece gerçekten geri dönen müşteriler sayılıyor.
```

### 3.2 Teknik Değişiklikler

#### Backend (7 Lokasyon)

| Dosya | Metod | Değişiklik |
|-------|-------|------------|
| `TrendyolOrderRepository.java` | `getCustomerAnalyticsSummary()` | `item_count > 1` → `order_count >= 2` |
| `TrendyolOrderRepository.java` | `getCustomerSegmentation()` | Ürün bazlı → Sipariş bazlı segmentler |
| `TrendyolOrderRepository.java` | `getCityRepeatAnalysis()` | `item_count > 1` → `order_count >= 2` |
| `TrendyolOrderRepository.java` | `getProductRepeatAnalysis()` | `purchase_count > 1` → `purchase_count >= 2` |
| `TrendyolOrderRepository.java` | `getPurchaseFrequencyDistribution()` | Ürün bazlı → Sipariş bazlı bucket'lar |
| `CustomerAnalyticsService.java` | `calculateFrequencyScore()` | RFM Frequency skorlama güncellendi |

#### SQL Değişikliği Örneği

```sql
-- ÖNCE (Yanlış)
WITH customer_stats AS (
    SELECT customer_id,
           COUNT(*) as order_count,
           SUM(item_count) as item_count  -- Toplam ÜRÜN sayısı
    FROM orders GROUP BY customer_id
)
SELECT COUNT(*) FILTER (WHERE item_count > 1) as repeat_customers
FROM customer_stats;

-- SONRA (Doğru)
WITH customer_stats AS (
    SELECT customer_id,
           COUNT(*) as order_count  -- Toplam SİPARİŞ sayısı
    FROM orders GROUP BY customer_id
)
SELECT COUNT(*) FILTER (WHERE order_count >= 2) as repeat_customers
FROM customer_stats;
```

### 3.3 Yeni Müşteri Segmentasyonu

| Eski Segment | Yeni Segment | Anlam |
|--------------|--------------|-------|
| 1-5 ürün | 1 sipariş | Yeni müşteri |
| 6-15 ürün | 2-3 sipariş | Başlangıç sadakat |
| 16+ ürün | 4-6 sipariş | Sadık müşteri |
| - | 7+ sipariş | Çok sadık müşteri |

### 3.4 Yeni RFM Frequency Skorlaması

| Sipariş Sayısı | Frequency Skoru | Segment Adı |
|----------------|-----------------|-------------|
| 1 | 1 | Yeni |
| 2-3 | 2 | Başlangıç |
| 4-5 | 3 | Orta |
| 6-9 | 4 | Sadık |
| 10+ | 5 | Çok Sadık |

---

## 4. VERİ DOĞRULAMA (k-pure Mağazası)

### 4.1 Ham Veri Analizi

```
Toplam Sipariş:        29,806
├── Geçerli Sipariş:   29,238 (%98.1)
├── İptal/Teslim Edilmedi: 568 (%1.9)

Customer ID Durumu:
├── Dolu:              15,231 (%51.1)
└── NULL:              14,575 (%48.9) ← Kasım 2025 öncesi API sınırlaması
```

### 4.2 Doğrulanmış Metrikler

| Segment | Müşteri Sayısı | Yüzde |
|---------|----------------|-------|
| 1 sipariş | 11,888 | %89.6 |
| 2-3 sipariş | 1,154 | %8.7 |
| 4-6 sipariş | 63 | %0.5 |
| 7+ sipariş | 6 | %0.05 |
| **TOPLAM** | **13,111** | **%100** |

### 4.3 UI vs Database Doğrulaması

| Metrik | UI'da Görünen | DB Sorgusu | Durum |
|--------|---------------|------------|-------|
| Toplam Müşteri | 13,110 | 13,111 | ✅ Eşleşiyor |
| Tekrar Müşteri | 1,223 | 1,223 | ✅ Tam eşleşme |
| Tekrar Oranı | %9.33 | %9.33 | ✅ Tam eşleşme |

### 4.4 En Sadık Müşteriler

| Sıra | Sipariş Sayısı | Toplam Harcama | Ortalama Sepet |
|------|----------------|----------------|----------------|
| 1 | 11 sipariş | ₺37,631 | ₺3,421 |
| 2 | 9 sipariş | ₺11,050 | ₺1,228 |
| 3 | 9 sipariş | ₺891 | ₺99 |
| 4 | 8 sipariş | ₺14,018 | ₺1,752 |
| 5 | 8 sipariş | ₺8,502 | ₺1,063 |

---

## 5. NULL CUSTOMER_ID AÇIKLAMASI

### 5.1 Kök Neden Analizi

```
Dönem Analizi:
┌─────────────────────┬───────────────┬────────────────────┐
│ Dönem               │ Sipariş       │ customer_id Durumu │
├─────────────────────┼───────────────┼────────────────────┤
│ 2025-11-04 öncesi   │ 14,509        │ %100 NULL          │
│ 2025-11-04 sonrası  │ 15,301        │ %99.6 DOLU         │
└─────────────────────┴───────────────┴────────────────────┘
```

**Sonuç**: Trendyol API'si Kasım 2025'ten önce `customerId` alanını döndürmüyordu. Bu bir kod hatası değil, API sınırlamasıdır.

### 5.2 Mevcut Durum

- Kasım 2025 sonrası siparişler için müşteri analizi %99.6 doğrulukla çalışıyor
- Eski siparişler için customer_id bilgisi yok (API sınırlaması)
- Backfill denendi ancak Trendyol API geriye dönük customer_id sağlamıyor

---

## 6. TEST SONUÇLARI

### 6.1 Birim Testleri

```bash
./mvnw test -Dtest=CustomerAnalyticsServiceTest
./mvnw test -Dtest=CustomerAnalyticsControllerTest

Sonuç: ✅ Tüm testler geçti
```

### 6.2 Entegrasyon Testleri

- ✅ API endpoint'leri doğru değerler döndürüyor
- ✅ Segmentasyon doğru hesaplanıyor
- ✅ RFM skorları doğru atanıyor
- ✅ Şehir bazlı analiz doğru çalışıyor

---

## 7. KARŞILAŞTIRMALI ANALİZ

### 7.1 E-ticaret Benchmark

| Sektör | Ortalama Tekrar Oranı | k-pure Oranı |
|--------|----------------------|--------------|
| Genel E-ticaret | %20-30 | - |
| Moda/Giyim | %15-25 | - |
| Kozmetik | %25-35 | - |
| **k-pure (Yeni)** | - | **%9.33** |

**Yorum**: %9.33 e-ticaret ortalamasının altında. Bu, retention stratejilerine odaklanma fırsatı gösteriyor.

### 7.2 Önce/Sonra Karşılaştırma

```
                    ESKİ                          YENİ
              ┌─────────────┐                ┌─────────────┐
Tekrar        │   %34.4     │    →           │   %9.33     │
Oranı         │  (Yanlış)   │                │  (Doğru)    │
              └─────────────┘                └─────────────┘
                    ↓                              ↓
Yorumlama:    "Harika!"                     "Geliştirme alanı var"
              (Yanıltıcı)                   (Gerçekçi)
```

---

## 8. İŞ ETKİSİ VE ÖNERİLER

### 8.1 Fırsatlar

1. **Retention Kampanyaları**: Gerçek tekrar oranını artırmak için hedefli kampanyalar
2. **Müşteri Segmentasyonu**: Doğru RFM skorlarıyla kişiselleştirilmiş pazarlama
3. **Ürün Analizi**: Hangi ürünler tekrar satın almayı tetikliyor?
4. **Şehir Bazlı Strateji**: Tekrar oranı yüksek şehirlere odaklanma

### 8.2 Dikkat Edilecekler

1. **Geçmiş Raporlar**: Eski raporlarla karşılaştırma yapılmamalı
2. **Hedef Revizyonu**: KPI hedefleri yeni metriğe göre güncellenmeli
3. **İletişim**: Kullanıcılara metrik değişikliği açıklanmalı

---

## 9. SUNUM AKIŞI (CTO için)

### Slide 1: Problem
- "Tekrar müşteri" tanımı yanlıştı
- Ürün sayısı ≠ Müşteri sadakati

### Slide 2: Çözüm
- Yeni algoritma: 2+ sipariş = tekrar müşteri
- E-ticaret standardına uygun

### Slide 3: Etki
- %34.4 → %9.33 (gerçek değer)
- Düşüş beklenen ve doğru

### Slide 4: Doğrulama
- DB sorguları ile doğrulandı
- Testler geçti

### Slide 5: Sonraki Adımlar
- Onay bekliyor
- Retention stratejisi fırsatı

---

## 10. ONAY TALEBİ

### Yapılan Değişiklikler
- [x] Backend algoritma düzeltmesi (7 lokasyon)
- [x] RFM skorlama güncellemesi
- [x] Segmentasyon güncellemesi
- [x] Birim testleri
- [x] Veri doğrulama

### Onay Gerekli
- [ ] CTO onayı
- [ ] Production deployment

### Riskler
| Risk | Olasılık | Etki | Mitigasyon |
|------|----------|------|------------|
| Kullanıcı şikayeti (metrik düştü) | Orta | Düşük | Açıklama mesajı |
| Eski raporlarla tutarsızlık | Yüksek | Düşük | Versiyon notu |

---

## 11. EK: ALGORİTMA DETAYI

### Adım Adım Hesaplama

```
ADIM 1: Siparişleri Al
────────────────────────────────────────
SELECT * FROM orders
WHERE status NOT IN ('Cancelled', 'UnDelivered')
  AND customer_id IS NOT NULL

ADIM 2: Müşteri Bazında Grupla
────────────────────────────────────────
SELECT customer_id, COUNT(*) as order_count
FROM orders
GROUP BY customer_id

ADIM 3: Tekrar Müşterileri Filtrele
────────────────────────────────────────
WHERE order_count >= 2

ADIM 4: Metrikleri Hesapla
────────────────────────────────────────
total_customers = COUNT(DISTINCT customer_id)
repeat_customers = COUNT(*) WHERE order_count >= 2
repeat_rate = repeat_customers / total_customers * 100
```

### Görsel Örnek

```
Müşteri A: [Sipariş 1] [Sipariş 2] [Sipariş 3] → 3 sipariş → ✅ TEKRAR
Müşteri B: [Sipariş 1]                         → 1 sipariş → ❌ TEK
Müşteri C: [Sipariş 1] [Sipariş 2]             → 2 sipariş → ✅ TEKRAR
Müşteri D: [Sipariş 1 (5 ürün)]                → 1 sipariş → ❌ TEK

Toplam: 4 müşteri
Tekrar: 2 müşteri (A ve C)
Oran: 2/4 = %50
```

---

**Doküman Sonu**

*Bu doküman CTO onayı için hazırlanmıştır. Sorularınız için geliştirme ekibiyle iletişime geçebilirsiniz.*
