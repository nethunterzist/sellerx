# Kampanya Karlılık Simülasyonu (Campaign Profitability Simulation)

**Durum:** Planlanıyor (Ertelendi)
**Araştırma Tarihi:** Ocak 2026
**Referans:** Melontik "Kâr Marjı Listesi" → "Kampanya Karlılık Simülasyonu"

---

## Özellik Açıklaması

Satıcıların Trendyol kampanyalarına katılmadan önce ürün bazlı kârlılık simülasyonu yapmalarını sağlayan özellik. Kampanya parametreleri girilerek hangi ürünlerin kampanyada kârlı, hangilerinin zararlı olacağı önceden hesaplanır.

## Melontik Analizi

### Kampanya Parametreleri

Melontik'in kullandığı parametreler:

| Parametre | Açıklama | Değerler |
|-----------|----------|----------|
| Promosyon Tipi | Kampanya türü | Dropdown seçimi |
| Kampanya Bölgesi | Hedef pazar | Mikro İhracat, Türkiye, Trendyol Plus |
| Trendyol Karşılama Oranı | Trendyol'un indirimden karşıladığı % | 0-100% |
| Min Sepet Tutarı | Kampanya için minimum sepet | TL cinsinden |
| İndirim Yüzdesi | Ürüne uygulanacak indirim | % |

### Melontik UI Yapısı

3 Tab'lı sistem:
1. **Avantajlı Ürün Excel Yükle** - Trendyol'dan indirilen avantajlı ürün listesi
2. **Kampanya Seçim** - Manuel parametre girişi
3. **Kampanya Excel Yükle** - Kampanya detay Excel'i

### Melontik Workflow

```
Kullanıcı → Trendyol Satıcı Paneli'nden Excel indir
         → Melontik'e Excel yükle
         → Melontik frontend'de hesaplama yap
         → Kârlı/Zararlı ürünleri göster
```

**Önemli:** Melontik API kullanmıyor, Excel yükleme yöntemi kullanıyor.

---

## Trendyol API Araştırması

### filterProducts API Response

```json
{
  "id": 123456,
  "title": "Ürün Adı",
  "productMainId": "ABC123",
  "barcode": "1234567890123",
  "brand": "Marka",
  "categoryName": "Kategori",
  "listPrice": 100.0,
  "salePrice": 80.0,
  "vatRate": 8,
  "hasActiveCampaign": false,
  "onSale": true,
  "quantity": 50
}
```

### Bulgular

| Aranan | API'de Var mı? | Notlar |
|--------|----------------|--------|
| Kampanya detayları | ❌ | API'de yok |
| Kampanya indirim oranı | ❌ | API'de yok |
| Trendyol katkı payı | ❌ | API'de yok |
| Kampanya bölgesi | ❌ | API'de yok |
| `hasActiveCampaign` | ✅ | Sadece boolean flag |
| Komisyon oranı | ⚠️ | Financial API'de, ürün API'sinde yok |

### Sonuç

**Trendyol'da Kampanya API'si YOKTUR.**

Sadece `hasActiveCampaign: true/false` şeklinde bir flag mevcut. Kampanya parametreleri (indirim oranı, Trendyol katkı payı, bölge, min sepet tutarı) Trendyol API'sinde bulunmuyor.

---

## SellerX Backend Mevcut Durum

### Kullanılabilir Veriler

| Veri | Dosya | Alan | Durum |
|------|-------|------|-------|
| Komisyon oranı | `TrendyolProduct.java` | `commissionRate` | ✅ Mevcut |
| Son komisyon | `TrendyolProduct.java` | `lastCommissionRate` | ✅ Mevcut |
| Ürün maliyeti | `CostAndStockInfo.java` | `unitCost` | ✅ Mevcut |
| Maliyet KDV | `CostAndStockInfo.java` | `costVatRate` | ✅ Mevcut |
| Satış fiyatı | `TrendyolProduct.java` | `salePrice` | ✅ Mevcut |
| Liste fiyatı | `TrendyolProduct.java` | `listPrice` | ✅ Mevcut |
| KDV oranı | `TrendyolProduct.java` | `vatRate` | ✅ Mevcut |

### Backend Gereksinimi

**Yeni backend geliştirme GEREKMİYOR.** Tüm hesaplamalar frontend'de mevcut verilerle yapılabilir.

---

## Uygulama Planı

### Seçenek A: Basit Yaklaşım (Önerilen)

**Avantajlar:**
- Hızlı geliştirme
- Backend değişikliği yok
- Kullanıcı dostu

**Akış:**
```
1. Kullanıcı kampanya parametrelerini manuel girer:
   - İndirim yüzdesi (%)
   - Trendyol katkı oranı (%)
   - Kampanya bölgesi (opsiyonel)

2. Frontend mevcut ürün verilerini çeker:
   - salePrice, listPrice
   - commissionRate
   - unitCost (maliyet)
   - vatRate

3. Frontend her ürün için hesaplar:
   - Kampanyalı fiyat = salePrice × (1 - indirim%)
   - Trendyol katkısı = İndirim tutarı × Trendyol katkı%
   - Satıcı indirimi = İndirim tutarı × (1 - Trendyol katkı%)
   - Net gelir = Kampanyalı fiyat - Komisyon - KDV
   - Kâr/Zarar = Net gelir - Maliyet

4. Sonuçları tablo olarak göster:
   - Kârlı ürünler (yeşil)
   - Zararlı ürünler (kırmızı)
   - Kâr marjı sıralaması
```

### Seçenek B: Excel Yükleme (Melontik Tarzı)

**Avantajlar:**
- Trendyol Excel formatıyla uyumlu
- Toplu işlem yapılabilir

**Dezavantajlar:**
- Daha karmaşık geliştirme
- Excel parse işlemi gerekli
- Kullanıcı için ekstra adım

**Akış:**
```
1. Kullanıcı Trendyol Satıcı Paneli'nden Excel indirir
2. Excel'i SellerX'e yükler
3. Backend Excel'i parse eder
4. Frontend sonuçları gösterir
```

---

## Dosya Yapısı (Planlanan)

```
sellerx-frontend/
├── app/[locale]/(app-shell)/
│   └── campaign-simulation/
│       └── page.tsx
├── components/campaign/
│   ├── campaign-params-form.tsx
│   ├── simulation-results-table.tsx
│   ├── profit-loss-indicator.tsx
│   └── index.ts
├── hooks/queries/
│   └── use-campaign-simulation.ts (opsiyonel)
├── types/
│   └── campaign.ts
└── messages/
    ├── tr.json (campaign section eklenecek)
    └── en.json (campaign section eklenecek)
```

---

## Hesaplama Formülleri

### Kampanyalı Fiyat
```
kampanyaliFiyat = salePrice × (1 - indirimYuzdesi / 100)
```

### İndirim Tutarı
```
indirimTutari = salePrice - kampanyaliFiyat
```

### Trendyol Katkısı
```
trendyolKatkisi = indirimTutari × (trendyolKatki / 100)
```

### Satıcı İndirimi
```
saticiIndirimi = indirimTutari - trendyolKatkisi
```

### Komisyon (Kampanyalı)
```
komisyon = kampanyaliFiyat × (commissionRate / 100)
```

### KDV
```
kdv = kampanyaliFiyat × (vatRate / 100)
```

### Net Gelir
```
netGelir = kampanyaliFiyat - komisyon
// Not: KDV gelire dahil, ayrıca düşülmez
```

### Kâr/Zarar
```
kar = netGelir - unitCost
karMarji = (kar / kampanyaliFiyat) × 100
```

---

## UI Mockup

```
┌─────────────────────────────────────────────────────────────────┐
│  Kampanya Karlılık Simülasyonu                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │ İndirim %   │ │ Trendyol    │ │ Bölge       │               │
│  │ [___15___]  │ │ Katkı %     │ │ [Türkiye▼]  │               │
│  │             │ │ [___50___]  │ │             │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
│                                                                 │
│  [Simüle Et]                                                    │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│  Özet: 45 ürün kârlı ✅ | 12 ürün zararlı ❌                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Ürün        │ Fiyat  │ Kamp.  │ Maliyet │ Kâr    │ Marj │   │
│  │             │        │ Fiyat  │         │        │      │   │
│  ├─────────────┼────────┼────────┼─────────┼────────┼──────┤   │
│  │ Ürün A      │ 100₺   │ 85₺    │ 40₺     │ +25₺   │ 29%  │   │
│  │ Ürün B      │ 50₺    │ 42.5₺  │ 45₺     │ -12₺   │ -28% │   │
│  │ ...         │        │        │         │        │      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Notlar

1. **Komisyon oranları:** Ürün bazlı `commissionRate` kullanılacak. Eğer yoksa kategori bazlı varsayılan oran kullanılabilir.

2. **Maliyet verisi:** `costAndStockInfo` JSONB'den en güncel maliyet alınacak. Maliyet girilmemiş ürünler için uyarı gösterilecek.

3. **Trendyol katkı oranları:** Gerçek kampanyalarda %0 ile %70 arasında değişiyor. Varsayılan %50 olabilir.

4. **Gelecek geliştirme:** Excel import özelliği sonradan eklenebilir.

---

## Referanslar

- Melontik Kampanya Simülasyonu sayfası
- Trendyol Developers API Docs: https://developers.trendyol.com/docs/intro
- Trendyol filterProducts API response
