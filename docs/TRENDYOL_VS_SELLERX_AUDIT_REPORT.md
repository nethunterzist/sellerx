# Trendyol vs SellerX - Veri Dogrulama Raporu

**Tarih**: 14 Subat 2026
**Magaza**: K-PURE (Seller ID: 1080066)
**Store ID**: `0824e453-3761-4cdf-96c9-3215a7770618`
**Kapsam**: Tum zamanlar + son 90 gun odakli
**Yontem**: Playwright ile Trendyol partner panel scrape + PostgreSQL DB sorgusu

---

## Ozet Skor Karti

| Kategori | Durum | Oncelik |
|----------|-------|---------|
| Bugunun Cirosu | ✅ Birebir Eslesme | - |
| Urun Sayisi & Durumu | ✅ Birebir Eslesme | - |
| Fatura (Deduction) Sayisi | ✅ 1 fark (726 vs 727) | - |
| 7g/30g Ciro | ⚠️ %5-7 fark | Orta |
| Siparis Sayisi (all-time) | ⚠️ Buyuk fark (31K vs 20K) | Arastir |
| Iptal Siparis | ⚠️ 168 fark | Orta |
| Hak Edis Sync | ❌ 18 gun geride | Yuksek |
| Iade/Claim Verisi | ❌ 389 vs 161, status farki | Yuksek |
| return_records Tablosu | ❌ Tamamen Bos (0 kayit) | Kritik |
| Musteri Sorulari | ❌ 489 vs 2,343 | Orta |

---

## Kategori 1: Dashboard Ciro Metrikleri

### Karsilastirma

| Metrik | Trendyol | SellerX (gross_amount) | SellerX (total_price excl cancel) | En Yakin Fark | Durum |
|--------|----------|----------------------|-----------------------------------|--------------|-------|
| Bugun ciro | 73,271₺ | 92,042₺ | **73,270.89₺** | **-0.11₺** | ✅ |
| Son 7 gun | 1,496,422₺ | 2,159,766₺ | **1,604,536₺** | +108,114₺ (+7.2%) | ⚠️ |
| Son 30 gun | 5,575,897₺ | 7,517,997₺ | **5,896,404₺** | +320,507₺ (+5.7%) | ⚠️ |

### Analiz

**Bugunun cirosu MUKEMMEL eslesiyor!** Trendyol'un gosterdigi deger = `SUM(total_price) WHERE status != 'Cancelled'`

- `gross_amount` KDV dahil brut tutar - Trendyol bunu gostermiyor
- `total_price` KDV haric net tutar - Trendyol'un gosterdigi deger BU
- Iptal edilmis siparisler ciro hesabindan dislaniyor

**7 ve 30 gunluk fark nedeni**: Trendyol muhtemelen iade edilen siparisleri de ciro hesabindan disliyor. SellerX'te `status != 'Cancelled'` filtresi yeterli degil, `Returned` durumlu siparisler de dislanmali.

### Aksiyon
- [ ] Dashboard revenue hesaplamasi icin `total_price` kullanilmali (gross_amount degil)
- [ ] Iptal VE iade siparisler ciro hesabindan dislanmali

---

## Kategori 2: Siparis Verileri

### Toplam Siparis

| Metrik | Trendyol | SellerX DB | Fark | Durum |
|--------|----------|------------|------|-------|
| Toplam siparis | 20,559 | 31,277 | +10,718 (+52%) | ⚠️ |
| Toplam paket | 19,776 | - | - | - |
| Iptal siparis | 767 | 599 | -168 (-21.9%) | ⚠️ |

### Siparis Durum Dagilimi

| Durum | Trendyol | SellerX DB (shipment_package_status) |
|-------|----------|-------------------------------------|
| Teslim Edilen (Delivered) | 19,513 | 26,985 |
| Tasima Durumunda (Shipped) | 244 | 1,373 |
| Yeni | 17 | - |
| Isleme Alinan | 2 | - |
| ReadyToShip | - | 1,313 |
| Picking | - | 991 |
| AtCollectionPoint | - | 12 |
| Cancelled | 767 | 600 |

### Analiz

**Buyuk fark aciklamasi**: Trendyol partner panelindeki "Siparislerim" sayfasi tum gecmisi gostermiyor olabilir. DB'deki en eski siparis **2025-01-31**, Trendyol muhtemelen son 12 ay veya belirli bir limitle gosteriyor.

**Iptal farki (767 vs 599)**: Trendyol tarafinda daha fazla iptal gorunuyor. Muhtemel nedenler:
1. SellerX'in sync etmedigi eski iptaller
2. Farkli iptal hesaplama metodolojisi
3. Trendyol'un Azerbaycan + Avrupa pazarlarindaki iptalleri de saymasi

### Aksiyon
- [ ] Trendyol'un Siparislerim sayfasinin ne kadardik gecmisi gosterdigini arastir
- [ ] Cancelled siparis sync mekanizmasini kontrol et

---

## Kategori 3: Urun & Stok Verileri

### Karsilastirma

| Metrik | Trendyol | SellerX DB | Fark | Durum |
|--------|----------|------------|------|-------|
| Toplam urun | 257 | 264 (257 approved) | 0 (approved) | ✅ |
| Satista urun | 49 | 49 (on_sale=true) | 0 | ✅ |
| Satista degil | 208 | 215 | -7 | ⚠️ |

### Analiz

**Toplam urun ve satista urun BIREBIR eslesiyor!** Trendyol 257 toplam gosterirken, DB'de 264 kayit var ancak 257'si `approved=true`. Trendyol onaylanmamis urunleri gostermiyor.

7 ekstra kayit DB'de (`264-257=7`) muhtemelen reddedilmis veya onay bekleyen urunler.

---

## Kategori 4: Fatura Listeleme (Deduction Invoices)

### Karsilastirma

| Metrik | Trendyol | SellerX DB | Fark | Durum |
|--------|----------|------------|------|-------|
| Toplam fatura sayisi | 726 | 727 | -1 (-0.1%) | ✅ |

### Fatura Tipi Dagilimi (SellerX DB - Top 10)

| Fatura Tipi | Adet | Toplam Borc |
|-------------|------|------------|
| Komisyon Faturasi | 129 | 4,716,009.96₺ |
| Kargo Fatura | 186 | 1,860,476.68₺ |
| Reklam Bedeli | 43 | 247,915.00₺ |
| Platform Hizmet Bedeli | 157 | 245,881.28₺ |
| Sabit Butceli Influencer Reklam | 2 | 193,800.00₺ |
| Komisyonlu Influencer Reklam | 11 | 153,172.49₺ |
| Erken Odeme Kesinti | 5 | 53,275.77₺ |
| Fatura Kontor Satis Bedeli | 31 | 12,484.00₺ |
| AZ-Uluslararasi Hizmet Bedeli | 26 | 8,422.17₺ |
| Uluslararasi Hizmet Bedeli | 59 | 5,549.11₺ |

### Analiz

**Neredeyse birebir eslesme!** 726 vs 727 - sadece 1 fatura fark. Bu muhtemelen:
- Sync sirasinda yeni eklenmis/silinmis bir fatura
- Veya tarih siniri farki

Trendyol'un "Fatura Listeleme" sayfasi = SellerX'teki `trendyol_deduction_invoices` tablosu.

Not: SellerX'teki ayri `trendyol_invoices` tablosunda sadece 9 kayit var (REKLAM=3, CEZA=2, ULUSLARARASI=2, DIGER=1, IADE=1). Bu tablo farkli bir amaca hizmet ediyor.

---

## Kategori 5: Kargo Maliyetleri

### Karsilastirma

| Metrik | SellerX DB |
|--------|------------|
| Kargo fatura sayisi (cargo_invoices) | 24,590 |
| Toplam kargo maliyeti | 1,859,589.73₺ |
| Kargo (deduction_invoices'tan) | 186 fatura, 1,860,476.68₺ |

### Analiz

Kargo maliyeti iki farkli tabloda tutuluyor:
- `trendyol_cargo_invoices`: 24,590 satirlik detay (siparis bazli)
- `trendyol_deduction_invoices` (Kargo Fatura tipi): 186 toplu fatura

Tutarlar cok yakin: **1,859,589.73₺ vs 1,860,476.68₺** (886.95₺ fark, %0.05)

Trendyol Fatura Listeleme sayfasindaki kargo faturalari da deduction tablosuyla uyumlu.

---

## Kategori 6: Hak Edis (Payment Orders)

### Karsilastirma

| Metrik | Trendyol | SellerX DB | Fark | Durum |
|--------|----------|------------|------|-------|
| Guncel bakiye | 6,056,115.03₺ | - | - | - |
| Gorunen gecmis odemeler | 15 | 5 (ayni donemde) | -10 | ❌ |
| DB'deki toplam hak edis | - | 109 | - | - |
| DB'deki toplam tutar | - | 12,963,845.90₺ | - | - |

### Son Odemeler Detay

| Tarih | Trendyol (TR+AZ ayri) | SellerX DB |
|-------|----------------------|------------|
| 12/02/2026 | 789,105.82₺ + 31,432.51₺ | ❌ YOK |
| 09/02/2026 | 540,465.66₺ + 18,032.13₺ | ❌ YOK |
| 05/02/2026 | 524,286.23₺ + 9,321.34₺ | ❌ YOK |
| 02/02/2026 | 787,656.79₺ + 8,587.26₺ | ❌ YOK |
| 29/01/2026 | 232,830.80₺ + 3,123.69₺ | ❌ YOK |
| 26/01/2026 | 374,762.75₺ + 3,431.07₺ | ✅ 378,193.82₺ |
| 22/01/2026 | 303,692.89₺ + 257.06₺ | ✅ 303,949.95₺ |
| 19/01/2026 | 328,050.38₺ | ✅ 328,050.38₺ |

### Analiz

**KRITIK SORUN: Hak edis sync'i 18 gun geride!**

- DB'deki son hak edis tarihi: **26 Ocak 2026**
- Trendyol'da gorunen son odeme: **12 Subat 2026**
- Eksik 10 odeme (29 Ocak - 12 Subat arasi)

Mevcut verilerde eslesme mukemmel:
- 26/01: Trendyol 374,762.75+3,431.07 = 378,193.82₺ vs DB 378,193.82₺ ✅
- 22/01: Trendyol 303,692.89+257.06 = 303,949.95₺ vs DB 303,949.95₺ ✅
- 19/01: Trendyol 328,050.38₺ = DB 328,050.38₺ ✅

Not: Trendyol Turkiye+Korfez ve Azerbaycan odemelerini ayri gosteriyor, SellerX bunlari birlestiriyor.

### Aksiyon
- [ ] **ACIL**: Hak edis sync job'unu kontrol et - neden 18 gundur calismiyor?
- [ ] Sync log'larini incele
- [ ] Manuel sync tetikle

---

## Kategori 7: Iade / Claim Verileri

### Karsilastirma

| Metrik | Trendyol | SellerX DB (claims) | SellerX DB (return_records) | Durum |
|--------|----------|--------------------|-----------------------------|-------|
| Toplam iade | 161 (157 paket) | 389 | 0 | ❌ |
| Onaylanan | 148 | - | - | - |
| Reddedilen | 6 | - | - | - |
| Tum status "Created" | - | 389 (hepsi "Created") | - | ❌ |

### Analiz

**IKI KRITIK SORUN:**

1. **Claim sayisi uyusmuyor**: Trendyol 161, DB 389
   - DB'deki tum claim'ler "Created" statusunde (Ocak 21 - Subat 14 arasi)
   - Trendyol'da farkli statusler var (Onaylanan, Reddedilen)
   - DB muhtemelen claim status guncellemelerini sync etmiyor

2. **return_records tablosu tamamen BOS (0 kayit)**
   - Bu tablo iade maliyetlerini hesaplamak icin kullaniliyor
   - Bos olmasi = iade maliyet hesabi CALISMIYOR
   - Dashboard'daki kar/zarar hesaplamasi yanlis olacak

### Aksiyon
- [ ] **KRITIK**: return_records tablosunun neden bos oldugunu arastir
- [ ] Claim status sync mekanizmasini duzelt
- [ ] Claim sayisi farkinin kaynagini bul (muhtemelen sync filtresi sorunu)

---

## Kategori 8: Stopaj / Tevkifat

### SellerX DB Verileri

| Metrik | SellerX DB |
|--------|------------|
| Toplam stopaj sayisi | 155 |
| Toplam stopaj tutari | 150,750.56₺ |

### Analiz

Trendyol'un Hesap Hareketleri sayfasi SPA yukleme sorunu nedeniyle acilamadi. Stopaj verisi icin Trendyol tarafi karsilastirilamadi.

---

## Kategori 9: Musteri Sorulari

### Karsilastirma

| Metrik | Trendyol | SellerX DB | Fark | Durum |
|--------|----------|------------|------|-------|
| Toplam soru | 2,343 | 489 | -1,854 (-79%) | ❌ |
| Cevaplanan | 2,328 | 488 (ANSWERED) | -1,840 | ❌ |
| Bekleyen | 0 | 1 (PENDING) | - | ⚠️ |
| Cevaplanmayan | 10 | - | - | - |
| Reddedilen | 5 | - | - | - |

### Analiz

**Buyuk fark**: SellerX sadece 489 soru sync etmis, Trendyol'da 2,343 var.

Muhtemel nedenler:
1. Soru sync'i sadece son X gun/ay kapsaminda calisiyor
2. Sadece belirli soru tiplerini (urun sorulari vs siparis sorulari) sync ediyor
3. Trendyol API'nin soru endpoint'inde pagination limiti

### Aksiyon
- [ ] Soru sync mekanizmasini kontrol et - kac gunluk veri cekiyor?
- [ ] API pagination limitini arastir

---

## Kategori 10: Dashboard Metrikleri (Ozet)

| Metrik | Trendyol | SellerX (optimal hesaplama) | Fark | Durum |
|--------|----------|---------------------------|------|-------|
| Bugun ciro | 73,271₺ | 73,270.89₺ | -0.11₺ | ✅ |
| Bugun siparis | - | 58 | - | - |
| Bekleyen siparis | 19 | - | - | - |
| Bekleyen iade | 0 | - | - | - |

---

## Kritik Bulgular Ozeti

### ✅ Basarili Eslesmeler (Dogru Calisan)
1. **Bugunun cirosu birebir eslesiyor** - `total_price` kullanildiginda ve iptal siparisler dislandiginda
2. **Urun sayisi ve durumu eslesiyor** - 257 toplam, 49 satista
3. **Fatura (deduction) sayisi neredeyse birebir** - 726 vs 727
4. **Hak edis tutarlari birebir** - Sync edilen kayitlarda kurusuna kadar eslesme
5. **Kargo maliyet tutarlari uyumlu** - %0.05 fark

### ⚠️ Arastirilmasi Gereken Farklar
1. **7g/30g ciro %5-7 farkli** - Iade siparislerin cirodan dislanmasi gerekiyor
2. **Toplam siparis sayisi farki** - Trendyol sayfasinin gosterim limiti arastirilmali
3. **Iptal siparis 168 fark** - Sync mekanizmasi kontrol edilmeli

### ❌ Kritik Sorunlar (ACIL Aksiyon Gerektiren)
1. **Hak edis sync'i 18 gun geride** - Son ödeme: 26 Ocak, bugun: 14 Subat
2. **return_records tablosu tamamen BOS** - Iade maliyet hesabi calismiyor
3. **Claim status guncellenmiyor** - Tum 389 claim "Created" statusunde
4. **Claim sayisi uyusmuyor** - DB 389 vs Trendyol 161
5. **Musteri sorulari %79 eksik** - 489 vs 2,343

---

## Teknik Notlar

### Ciro Hesaplama Formulu
```
Trendyol Dashboard Ciro = SUM(total_price) WHERE status != 'Cancelled' [AND muhtemelen status != 'Returned']
```
- `gross_amount` = KDV dahil brut tutar (Trendyol bunu gostermiyor)
- `total_price` = KDV haric / iskontolu net tutar (Trendyol'un gosterdigi)

### Trendyol Partner Panel Yapisi
- React SPA, veriler XHR ile yukleniyorcurl
- Fatura Listeleme: `/payments/invoice` → `trendyol_deduction_invoices`
- Odemeler: `/payments/my-payments` → `trendyol_payment_orders`
- Siparislerim: `/orders/list` → `trendyol_orders`
- Hesap Hareketleri: `/payments/account-activities` → SPA yukleme sorunu (acilamadi)

### DB Tablo Eslesmesi
| Trendyol Sayfasi | SellerX Tablosu | Eslesme Durumu |
|------------------|-----------------|----------------|
| Fatura Listeleme | trendyol_deduction_invoices | ✅ 726 vs 727 |
| Odemeler | trendyol_payment_orders | ⚠️ Sync geride |
| Siparislerim | trendyol_orders | ⚠️ Sayi farki |
| Iade Taleplerim | trendyol_claims | ❌ Sayi + status farki |
| - | return_records | ❌ BOS |
| Urunlerim | trendyol_products | ✅ Eslesiyor |
| Musteri Sorulari | trendyol_questions | ❌ %79 eksik |
| Kargo Faturalari | trendyol_cargo_invoices | ✅ Uyumlu |
| Stopaj | trendyol_stoppages | ? Karsilastirilamadi |

---

## Oncelikli Aksiyon Plani

### P0 - Acil (Bugun)
1. Hak edis sync job'unu kontrol et ve tetikle
2. return_records tablosunun neden bos oldugunu arastir

### P1 - Yuksek Oncelik (Bu Hafta)
3. Claim status sync mekanizmasini duzelt
4. Dashboard ciro hesaplamasini `total_price` bazli yap
5. Iade siparisleri ciro hesabindan disla

### P2 - Orta Oncelik (Gelecek Hafta)
6. Musteri sorulari sync kapsamini genislet
7. Iptal siparis farkini arastir
8. Stopaj verisi icin Hesap Hareketleri karsilastirmasi (manuel)

### P3 - Dusuk Oncelik
9. Siparis sayisi farkinin kok nedenini bul
10. Fatura sayisi 1'lik farkini arastir
