# Otomatik Stok Artisi Algilama (Auto Stock Detection)

Trendyol uzerinden gelen stok artislarini otomatik algilayarak kullaniciya **onay bildirimi** gonderen sistem. Kullanici onaylarsa maliyet kaydi olusturulur, reddederse hicbir islem yapilmaz.

## Genel Bakis

Trendyol stok miktari arttiginda (orn: 250 -> 300) sistem otomatik olarak:

1. Delta hesaplar (+50 adet)
2. Satin alma siparisi (PO) ile aciklanip aciklanamayacagini kontrol eder
3. Son bilinen maliyeti alert data'sina ekler
4. `PENDING_APPROVAL` (onay bekleyen) bildirim olusturur
5. Kullanici bildirim merkezinden **Onayla** veya **Reddet** secer

```
Trendyol Sync
     |
     v
saveOrUpdateProduct()
     |
     |  eski stok: 250
     |  yeni stok: 300
     |  delta: +50
     v
AutoStockDetectionService.handleStockIncrease()
     |
     +---> PO ile aciklanabiliyor mu? (son 2 gun, +/-%20)
     |        |
     |        +-- EVET --> SKIP (PO zaten kaydi olusturdu)
     |        |
     |        +-- HAYIR --> devam
     |
     +---> PENDING_APPROVAL bildirim olustur
     |        |
     |        +-- Son maliyet VAR --> Alert data'ya ekle (hasCostInfo: true)
     |        |                       Severity: MEDIUM
     |        |
     |        +-- Son maliyet YOK --> hasCostInfo: false
     |                                Severity: HIGH
     |
     v
Bildirim Merkezi (Kullanici karari)
     |
     +-- ONAYLA --> CostAndStockInfo olustur (AUTO_DETECTED)
     |              FIFO redistribution tetikle
     |              Alert status: APPROVED
     |
     +-- REDDET --> Hicbir kayit olusturulmaz
                    Alert status: DISMISSED
```

## Neden Onay Bazli?

**Eski Sistem (v1):** Stok artisi algilaninca otomatik maliyet kaydi olusturuluyordu. Problem: Iadeler, stok transferleri veya Trendyol tarafindaki duzeltmeler de stok artisi olarak algilanip yanlis maliyet kaydi olusturuyordu.

**Yeni Sistem (v2):** Karar kullaniciya birakildi. Stok artisi algilaninca bildirim gelir, kullanici mal girisi ise onaylar, iade veya baska sebep ise reddeder. Boylece yanlis maliyet kaydi olusma riski ortadan kalkar.

## Backend Yapisi

Paket: `sellerx-backend/src/main/java/com/ecommerce/sellerx/products/`

### Ana Siniflar

| Sinif | Sorumluluk |
|-------|-----------|
| **AutoStockDetectionService** | Stok artisi algilama, PENDING_APPROVAL bildirim olusturma, PO duplicate kontrolu |
| **AlertHistoryService** | Onaylama (approve) ve reddetme (dismiss) islemleri, maliyet kaydi olusturma |
| **AlertHistoryController** | Approve/Dismiss REST endpoint'leri |
| **TrendyolProductService** | `saveOrUpdateProduct()` hook'u -- stok artisi tetikleyicisi |
| **TrendyolProductMapper** | `hasAutoDetectedCost` hesaplama |

### AutoStockDetectionService Metodlari

- `handleStockIncrease(product, oldQuantity, newQuantity)` -- Ana metod. Delta hesaplar, PO kontrolu yapar, PENDING_APPROVAL bildirim olusturur. **Maliyet kaydi olusturmaz.**
- `isExplainedByRecentPO(product, delta)` -- Son 2 gunde CLOSED olan PO'larin item'larini kontrol eder. Ayni urun + miktar +/-%20 icindeyse `true` doner (duplicate onleme).
- `getLastKnownCost(product)` -- `costAndStockInfo` listesinden en son tarihli, `unitCost != null` olan kaydi bulur. Onay icin alert data'sina eklenir.

### AlertHistoryService Metodlari (Onay/Red)

- `approveStockAlert(UUID alertId)` -- PENDING_APPROVAL alert'i onaylar:
  1. Alert'i bul, status kontrolu yap
  2. Alert data'sindan productId, delta, unitCost, costVatRate al
  3. CostAndStockInfo olustur (costSource: AUTO_DETECTED)
  4. Product'a ekle, kaydet
  5. FIFO redistribution tetikle
  6. Alert status -> APPROVED

- `dismissStockAlert(UUID alertId)` -- PENDING_APPROVAL alert'i reddeder:
  1. Alert'i bul, status kontrolu yap
  2. Alert status -> DISMISSED
  3. Hicbir maliyet kaydi olusturulmaz

### Alert Data Yapisi (JSONB)

PENDING_APPROVAL alert olusturulurken `data` alanina onay icin gerekli tum bilgiler eklenir:

```json
{
  "productId": "uuid-string",
  "barcode": "8680001234567",
  "productName": "Urun Adi",
  "delta": 50,
  "imageUrl": "https://...",
  "pendingApproval": true,
  "hasCostInfo": true,
  "unitCost": 45.00,
  "costVatRate": 20
}
```

| Alan | Tip | Aciklama |
|------|-----|----------|
| `productId` | String (UUID) | Urune ait ID |
| `barcode` | String | Urun barkodu |
| `productName` | String | Urun adi |
| `delta` | Integer | Stok artis miktari |
| `imageUrl` | String | Urun gorseli |
| `pendingApproval` | Boolean | Her zaman `true` |
| `hasCostInfo` | Boolean | Son bilinen maliyet var mi? |
| `unitCost` | Double | Son bilinen birim maliyet (hasCostInfo=true ise) |
| `costVatRate` | Integer | Son bilinen KDV orani (hasCostInfo=true ise) |

### Alert Status Degerleri

| Deger | Anlam | Aciklama |
|-------|-------|----------|
| `INFO` | Bilgilendirme | Default. Kural bazli normal alertler |
| `PENDING_APPROVAL` | Onay bekliyor | Stok artisi algilandi, kullanici karari bekleniyor |
| `APPROVED` | Onaylandi | Kullanici onayladi, maliyet kaydi olusturuldu |
| `DISMISSED` | Reddedildi | Kullanici reddetti, hicbir islem yapilmadi |

### costSource Alani

`CostAndStockInfo` JSONB icerisinde `costSource` alani ile kaydin kaynagi takip edilir:

| Deger | Kaynak | Aciklama |
|-------|--------|----------|
| `AUTO_DETECTED` | Onay sonrasi | Kullanici stok artisi bildirimini onaylayinca olusturulur |
| `MANUAL` | Kullanici | Kullanici maliyet modalindan veya bulk update ile girer |
| `PURCHASE_ORDER` | Satin alma | PO kapatilinca PurchaseOrderService tarafindan olusturulur |
| `null` | Eski kayitlar | Migration oncesi mevcut kayitlar (backward compatible) |

### Hook: TrendyolProductService.saveOrUpdateProduct()

```
// 1. Eski stok degerini yakala (updateProductFields ONCESINDE)
int oldQuantity = product.getTrendyolQuantity();
int newQuantity = apiProduct.getQuantity();

// 2. Normal updateProductFields + save

// 3. previousTrendyolQuantity guncelle
product.setPreviousTrendyolQuantity(oldQuantity);

// 4. Mevcut urunlerde stok ARTISI varsa tetikle
if (!isNew && newQuantity > oldQuantity) {
    autoStockDetectionService.handleStockIncrease(product, oldQuantity, newQuantity);
}
```

- `try-catch` ile sarmalandi -- auto-detection hatasi sync akisini kirmiyor.
- Yeni urunlerde (`isNew = true`) tetiklenmiyor (ilk onboarding sync'i atlanir).

### API Endpoint'leri

| Method | Endpoint | Aciklama |
|--------|----------|----------|
| `PUT` | `/api/alerts/{id}/approve` | PENDING_APPROVAL alert'i onayla (maliyet kaydi olusturur) |
| `PUT` | `/api/alerts/{id}/dismiss` | PENDING_APPROVAL alert'i reddet (hicbir islem yapilmaz) |

**Approve Response (200 OK):**
```json
{
  "id": "uuid",
  "alertType": "STOCK",
  "title": "Stok Artisi Algilandi",
  "status": "APPROVED",
  "severity": "MEDIUM",
  "read": true
}
```

**Hata Durumlari:**
- 404: Alert bulunamadi
- 400: Alert PENDING_APPROVAL degil (zaten onaylanmis/reddedilmis)

### Badge Temizleme Mekanizmasi

Kullanici maliyet modalinden yeni kayit eklediginde veya mevcut kaydi duzenlediginde:

1. `updateCostAndStock()` ve `addStockInfo()` metodlari `clearAutoDetectedFlags()` cagirir
2. Tum `AUTO_DETECTED` kayitlar `MANUAL` olarak guncellenir
3. Mapper `hasAutoDetectedCost = false` hesaplar -> badge kaybolur

## Veritabani

### Migration: V87 (previousTrendyolQuantity)

```sql
ALTER TABLE trendyol_products
  ADD COLUMN IF NOT EXISTS previous_trendyol_quantity INTEGER DEFAULT 0;

UPDATE trendyol_products
  SET previous_trendyol_quantity = COALESCE(trendyol_quantity, 0);
```

### Migration: V88 (alert_history status alani)

```sql
ALTER TABLE alert_history ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'INFO';
```

- Mevcut alertler `INFO` olarak kalir (backward compatible)
- Yeni stok artisi alertleri `PENDING_APPROVAL` olarak olusturulur

### Model Degisiklikleri

| Entity | Alan | Tip | Aciklama |
|--------|------|-----|----------|
| `TrendyolProduct` | `previousTrendyolQuantity` | `Integer` | Onceki Trendyol stok miktari |
| `CostAndStockInfo` | `costSource` | `String` | Kayit kaynagi (AUTO_DETECTED, MANUAL, PURCHASE_ORDER) |
| `TrendyolProductDto` | `hasAutoDetectedCost` | `Boolean` | Otomatik algilanan maliyet var mi? |
| `AlertHistory` | `status` | `String` | Alert durumu (INFO, PENDING_APPROVAL, APPROVED, DISMISSED) |

## Frontend

### Bildirim Merkezi -- Onay/Red Butonlari

**Dosya:** `sellerx-frontend/components/alerts/notification-center.tsx`

`alert.status === 'PENDING_APPROVAL'` oldugunda aksiyon kartı gosterilir:

```
+-------------------------------------------+
| [amber border-left]                       |
| STOK  Stok Artisi Algilandi               |
|                                           |
| Urun X icin +20 adet stok artisi         |
| tespit edildi. Mal girisi olarak          |
| kayit olusturulsun mu?                    |
|                                           |
|  [Onayla]  [Reddet]                       |
|     veya                                  |
|  [Maliyet Gir]  [Reddet]                 |
|  (maliyet yoksa)                          |
|                                           |
| Magaza Adi - 2 saat once                  |
+-------------------------------------------+
```

**Buton Davranislari:**
- **Onayla** (yesil, `hasCostInfo: true`): `useApproveStockAlert` mutation cagirir -> maliyet kaydi olusturulur
- **Maliyet Gir** (mavi, `hasCostInfo: false`): Urunler sayfasina yonlendirir -> kullanici once maliyet girer
- **Reddet** (kirmizi outline): `useDismissStockAlert` mutation cagirir -> bildirim kapatilir
- PENDING_APPROVAL alertlerde "okundu" butonu gosterilmez (approve/dismiss zaten okundu isareti koyar)

### React Query Hooks

**Dosya:** `sellerx-frontend/hooks/queries/use-alerts.ts`

```typescript
// Mevcut hook'lar (degismedi)
useAlertRules()
useUnreadAlerts()
useUnreadAlertCount()
useMarkAlertAsRead()
useMarkAllAlertsAsRead()

// Yeni hook'lar (onay bazli sistem)
useApproveStockAlert()   // PUT /api/alerts/{id}/approve
useDismissStockAlert()   // PUT /api/alerts/{id}/dismiss
```

Her iki mutation da basarili olunca su query key'leri invalidate eder:
- `['alerts', 'history']`
- `['alerts', 'unread']`
- `['alerts', 'unread-count']`
- `['alerts', 'stats']`

### Next.js BFF Routes

**Dosyalar:**
- `sellerx-frontend/app/api/alerts/[id]/approve/route.ts` -- PUT proxy
- `sellerx-frontend/app/api/alerts/[id]/dismiss/route.ts` -- PUT proxy

Standart BFF pattern: access_token cookie'sini okuyup backend'e proxy.

### Types

**Dosya:** `sellerx-frontend/types/alert.ts`

```typescript
export type AlertStatus = 'INFO' | 'PENDING_APPROVAL' | 'APPROVED' | 'DISMISSED';

export interface AlertHistory {
  // ... mevcut alanlar
  status?: AlertStatus;
}
```

### Urunler Sayfasi -- Badge

**Dosya:** `sellerx-frontend/app/[locale]/(app-shell)/products/page.tsx`

`hasAutoDetectedCost === true` olan urunlerde amber badge gosterilir:
- Sparkles ikonu + "Otomatik maliyet algilandi" tooltip
- Badge'e tiklaninca maliyet duzenleme modali acilir

**Not:** Onay bazli sistemde badge **onaydan sonra** gorunur (dogru davranis). Kullanici bildirimden onaylayinca CostAndStockInfo olusur, mapper badge'i hesaplar.

### Maliyet Gecmisi Timeline -- Kaynak Etiketleri

**Dosya:** `sellerx-frontend/components/products/cost-history-timeline.tsx`

Her maliyet kaydinda `costSource` gosterilir:
- `AUTO_DETECTED` -> Amber badge "Otomatik" (Sparkles ikonu)
- `PURCHASE_ORDER` -> Mavi badge "Satin Alma" (ShoppingCart ikonu)
- `MANUAL` / `null` -> Etiket gosterilmez (backward compatible)

## PO Duplicate Onleme

Stok artisi bir satin alma siparisi (PO) kapatilmasi sonucu olabilir. Bu durumda cift kayit olusmasin diye:

```
isExplainedByRecentPO(product, delta):
  1. Son 2 gun icinde CLOSED olan PO'lari bul
  2. Her PO'nun item'larini kontrol et
  3. Ayni urun barcode'u + miktar +/-%20 tolerans icindeyse -> TRUE (skip)
  4. Hicbiri eslesmediyse -> FALSE (PENDING_APPROVAL bildirim olustur)
```

**Ornek:**
- PO: 50 adet kapatildi (dun)
- Trendyol sync: +48 adet artis
- 48 = 50 (+/-%20 icinde) -> PO ile aciklandi, bildirim olusturulmaz

## Edge Case'ler ve Kararlar

| Durum | Karar |
|-------|-------|
| Hic maliyet kaydi yoksa | PENDING_APPROVAL alert (HIGH severity, hasCostInfo: false) |
| Ayni gun PO + auto-detect cakismasi | PO duplicate check (+/-2 gun, +/-%20 miktar) onler |
| Ilk onboarding sync (tum urunler yeni) | `isNew = true` -> auto-detection atlanir |
| Iade kaynakli stok artisi | Kullanici Reddet'e basarak yanlis kayit olusmasini onler |
| unitCost = 0 olan son maliyet | hasCostInfo: true, unitCost: 0 ile bildirim (kullanici onaylarsa 0 maliyet kaydi olusur) |
| Kullanici onayladi ama urun silinmis | 404 hatasi doner |
| Zaten onaylanmis/reddedilmis alert | 400 hatasi doner ("Alert is not pending approval") |

## Testler

**Dosya:** `sellerx-backend/src/test/java/com/ecommerce/sellerx/products/AutoStockDetectionServiceTest.java`

### AutoStockDetectionService Testleri

| # | Test | Beklenen |
|---|------|----------|
| 1 | Stok artisi + mevcut maliyet | PENDING_APPROVAL alert, hasCostInfo: true, CostAndStockInfo **olusturulmaz** |
| 2 | Stok artisi + maliyet yok | PENDING_APPROVAL alert (HIGH severity), hasCostInfo: false |
| 3 | Stok azalmasi veya degisim yok | Hicbir sey tetiklenmez |
| 4 | Son 2 gunde PO CLOSED (benzer miktar) | Skip (bildirim yok) |
| 5 | PO miktari cok farkli (+/-%20 disi) | PENDING_APPROVAL alert olusturur |
| 6 | PO 3+ gun once CLOSED | PENDING_APPROVAL alert olusturur |
| 7 | Birden fazla maliyet kaydi | En son tarihlisini kullanir |
| 8+ | getLastKnownCost, isExplainedByRecentPO detay testleri | Cesitli senaryolar |

### AlertHistoryService Testleri

| # | Test | Beklenen |
|---|------|----------|
| 1 | Approve: maliyet var | CostAndStockInfo olusur, FIFO tetiklenir, status=APPROVED |
| 2 | Approve: maliyet yok | Kayit olusturulmaz, status=APPROVED |
| 3 | Dismiss | Hicbir kayit olusturulmaz, status=DISMISSED |
| 4 | Approve: zaten approved/dismissed | IllegalStateException |
| 5 | Approve: urun bulunamadi | AlertNotFoundException |

```bash
cd sellerx-backend && ./mvnw test -Dtest=AutoStockDetectionServiceTest
cd sellerx-backend && ./mvnw test -Dtest=AlertHistoryServiceTest
```

## Referanslar

- Alert sistemi: [architecture/ALERT_SYSTEM.md](../architecture/ALERT_SYSTEM.md)
- Satin alma (PO): [features/PURCHASING.md](PURCHASING.md)
- Veritabani semasi: [architecture/DATABASE_SCHEMA.md](../architecture/DATABASE_SCHEMA.md)
- Urun stok yonetimi: [sprint-3-backend-domains/01-domain-list.md](../sprint-3-backend-domains/01-domain-list.md) (products paketi)
- Backend endpoint envanteri: [sprint-1-api-inventory/01-backend-endpoints.md](../sprint-1-api-inventory/01-backend-endpoints.md)
