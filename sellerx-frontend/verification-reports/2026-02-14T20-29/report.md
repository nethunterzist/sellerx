# SellerX Dogrulama Raporu

Tarih: 14.02.2026 23:30:17
Magaza: Test Magazasi (test@test.com)

## Ozet

- Toplam kontrol: 42
- Eslesen: 24 (57.1%)
- Uyari: 2
- Hata: 16

## Dashboard Kartları

| # | Alan | DB | SellerX | Trendyol | Sapma | Sonuc |
|---|------|-----|---------|----------|-------|-------|
| 1 | Bugun - Ciro | 161.773,14 | 161.773,14 | 0,00 | %100 | ERR |
| 2 | Bugun - Siparis Sayisi | 122,00 | 122,00 | - | %0 | OK |
| 3 | Bugun - Iade | 0,00 | 0,00 | - | %0 | OK |
| 4 | Dun - Ciro | 269.612,43 | 269.612,43 | - | %0 | OK |
| 5 | Dun - Siparis Sayisi | 195,00 | 195,00 | - | %0 | OK |
| 6 | Bu Ay - Ciro | 3.034.482,21 | 2.822.120,36 | - | %7 | ERR |
| 7 | Bu Ay - Siparis Sayisi | 2.396,00 | 2.226,00 | - | %7.1 | ERR |
| 8 | Bu Ay - Iade | 0,00 | 0,00 | - | %0 | OK |
| 9 | Bu Ay - Brut Kar | 1.641.985,70 | 1.965.952,36 | - | %19.73 | ERR |
| 10 | Gecen Ay - Ciro | 8.557.537,78 | 8.514.762,19 | - | %0.5 | WARN |
| 11 | Gecen Ay - Siparis Sayisi | 7.058,00 | 6.999,00 | - | %0.84 | ERR |

**Sonuc**: 5/11 eslesen, 1 uyari, 5 hata

## Dashboard Urun Tablosu (Ilk 10)

| # | Alan | DB | SellerX | Trendyol | Sapma | Sonuc |
|---|------|-----|---------|----------|-------|-------|

**Sonuc**: 0/0 eslesen

## Donem Detay Modali (Bu Ay)

| # | Alan | DB | SellerX | Trendyol | Sapma | Sonuc |
|---|------|-----|---------|----------|-------|-------|
| 1 | Bu Ay Modal - Ciro | 3.060.521,91 | 14,00 | - | %100 | ERR |
| 2 | Bu Ay Modal - Siparis | 2.412,00 | 14,00 | - | %99.42 | ERR |
| 3 | Bu Ay Modal - Komisyon | 477.608,04 | 14,00 | - | %100 | ERR |
| 4 | Bu Ay Modal - Kargo | 0,00 | 14,00 | - | %100 | ERR |

**Sonuc**: 0/4 eslesen, 4 hata

## Siparisler Sayfasi

| # | Alan | DB | SellerX | Trendyol | Sapma | Sonuc |
|---|------|-----|---------|----------|-------|-------|
| 1 | Son 7 Gun - Siparis Sayisi (DB vs FE tablo) | 10,00 | 5,00 | - | %50 | ERR |
| 2 | Siparis #10966065613 - Tutar | 899,20 | 899,20 | - | %0 | OK |
| 3 | Siparis #10966046045 - Tutar | 899,20 | 899,20 | - | %0 | OK |
| 4 | Siparis #10966044785 - Tutar | 2.419,10 | 2.419,10 | - | %0 | OK |
| 5 | Siparis #10966019238 - Tutar | 1.349,10 | 1.349,10 | - | %0 | OK |
| 6 | Siparis #10966011466 - Tutar | 2.419,10 | 2.419,10 | - | %0 | OK |

**Sonuc**: 5/6 eslesen, 1 hata

## Faturalar Sayfasi (Son 30 Gun)

| # | Alan | DB | SellerX | Trendyol | Sapma | Sonuc |
|---|------|-----|---------|----------|-------|-------|
| 1 | Fatura - KOMISYON Toplam | 865.145,23 | 865.145,23 | - | %0 | OK |
| 2 | Fatura - KOMISYON Adet | 8,00 | 8,00 | - | %0 | OK |
| 3 | Fatura - KARGO Toplam | 516.232,05 | 516.232,05 | - | %0 | OK |
| 4 | Fatura - KARGO Adet | 17,00 | 17,00 | - | %0 | OK |
| 5 | Fatura - CEZA Toplam | 1.100,00 | 1.100,00 | - | %0 | OK |
| 6 | Fatura - CEZA Adet | 4,00 | 4,00 | - | %0 | OK |
| 7 | Fatura - IADE Toplam | 374,50 | 1.906,91 | - | %409.19 | ERR |
| 8 | Fatura - IADE Adet | 1,00 | 3,00 | - | %200 | ERR |
| 9 | Fatura - REKLAM Toplam | 196.308,33 | 196.808,33 | - | %0.25 | WARN |
| 10 | Fatura - REKLAM Adet | 14,00 | 13,00 | - | %7.14 | ERR |
| 11 | Fatura - ULUSLARARASI Toplam | 6.484,91 | 6.484,91 | - | %0 | OK |
| 12 | Fatura - ULUSLARARASI Adet | 11,00 | 9,00 | - | %18.18 | ERR |
| 13 | Fatura - PLATFORM_UCRETLERI Toplam | 62.729,33 | 62.729,33 | - | %0 | OK |
| 14 | Fatura - PLATFORM_UCRETLERI Adet | 17,00 | 17,00 | - | %0 | OK |
| 15 | Fatura - DIGER Toplam | 2.811,00 | 3.843,41 | - | %36.73 | ERR |
| 16 | Fatura - DIGER Adet | 3,00 | 4,00 | - | %33.33 | ERR |
| 17 | Toplam Fatura Sayisi (DB) | 75,00 | - | - | %0 | OK |

**Sonuc**: 10/17 eslesen, 1 uyari, 6 hata

## İadeler Sayfası (Son 3 Yıl)

| # | Alan | DB | SellerX | Trendyol | Sapma | Sonuc |
|---|------|-----|---------|----------|-------|-------|
| 1 | İade Adedi (Son 3 Yıl) | 1,00 | 1,00 | - | %0 | OK |
| 2 | İade Oranı (%) | 0,00 | 0,00 | - | %0 | OK |
| 3 | Toplam İade Kaybi (FE Only) | - | 151,50 | - | %0 | OK |

**Sonuc**: 3/3 eslesen

## Ürünler Sayfası (İlk 10)

| # | Alan | DB | SellerX | Trendyol | Sapma | Sonuc |
|---|------|-----|---------|----------|-------|-------|
| 1 | Ürün Sayısı (DB vs FE tablo) | 10,00 | 10,00 | - | %0 | OK |

**Sonuc**: 1/1 eslesen

