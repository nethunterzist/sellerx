# Stopaj (Tevkifat) Sistemi

> Trendyol siparişleri için stopaj (gelir vergisi tevkifatı) hesaplama, eşleştirme ve takip altyapısı.

## Genel Bakış

SellerX, siparişler için **üç aşamalı** stopaj hesaplama sistemi kullanır:

1. **Tahmini Stopaj**: Sipariş geldiğinde, KDV hariç tutar üzerinden %1 olarak hesaplanır
2. **Gerçek Stopaj**: Financial API'den (OtherFinancials) veri geldiğinde, `orderNumber` ve `shipmentPackageId` ile eşleştirilerek güncellenir
3. **Enrichment**: Sipariş servis edilirken, gerçek değer varsa kullanılır

Bu sistem, **Komisyon** ve **Kargo** ile aynı pattern'i takip eder.

---

## Yasal Dayanak

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           YASAL BİLGİLER                                │
│                                                                         │
│   Yasal Dayanak:  22/12/2024 tarihli 9284 Sayılı Cumhurbaşkanı Kararı  │
│   Yürürlük:       1 Ocak 2025                                          │
│   Oran:           %1 (Sabit)                                           │
│   Matrah:         KDV HARİÇ satış tutarı                               │
└─────────────────────────────────────────────────────────────────────────┘
```

| Bilgi | Değer |
|-------|-------|
| **Yasal Oran** | %1 (Sabit) |
| **Yasal Dayanak** | 22/12/2024 tarihli 9284 Sayılı Cumhurbaşkanı Kararı |
| **Yürürlük** | 1 Ocak 2025 |
| **Hesaplama Matrahı** | **KDV HARİÇ** satış tutarı |

---

## Stopaj Formülü

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          STOPAJ FORMÜLÜ                                 │
│                                                                         │
│   stopaj = KDV_HARİÇ_TUTAR × %1                                        │
│                                                                         │
│   KDV_HARİÇ_TUTAR = totalPrice / (1 + KDV_ORANI)                       │
│                                                                         │
│   Örnek (%20 KDV):                                                     │
│   totalPrice = 1.200 TL (KDV dahil)                                    │
│   kdvHariç = 1.200 / 1.20 = 1.000 TL                                   │
│   stopaj = 1.000 × 0.01 = 10,00 TL                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

### KDV Oranlarına Göre Hesaplama

| KDV Oranı | KDV Dahil Tutar | KDV Hariç Tutar | Stopaj (%1) |
|-----------|-----------------|-----------------|-------------|
| %20 | 1.200 TL | 1.000 TL | 10,00 TL |
| %10 | 1.100 TL | 1.000 TL | 10,00 TL |
| %1 | 1.010 TL | 1.000 TL | 10,00 TL |

### Kritik Düzeltme

**Eski (Yanlış) Hesaplama:**
```
stopaj = totalPrice × %1 = KDV_DAHİL_TUTAR × 0.01
```

**Yeni (Doğru) Hesaplama:**
```
stopaj = (totalPrice / (1 + KDV_ORANI)) × %1 = KDV_HARİÇ_TUTAR × 0.01
```

| Senaryo | Değer |
|---------|-------|
| Satış Tutarı (KDV Dahil) | 1.000 TL |
| KDV Oranı | %20 |
| KDV Hariç Tutar | 833,33 TL |
| **Eski Hesaplama (Yanlış)** | 1.000 × 0.01 = **10,00 TL** |
| **Yeni Hesaplama (Doğru)** | 833,33 × 0.01 = **8,33 TL** |
| **Fark** | +1,67 TL fazla (%20 hata) |

---

## Üç Aşamalı Veri Akışı

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              TRENDYOL API                               │
├─────────────────────────────────┬───────────────────────────────────────┤
│         Orders API              │       OtherFinancials API             │
│    /sapigw/suppliers/.../orders │  /integration/finance/otherfinancials │
│                                 │       ?transactionType=Stoppage       │
│                                 │                                       │
│  totalPrice (KDV dahil)         │  orderNumber ✅                       │
│  orderItems[].saleVatRate       │  shipmentPackageId ✅                 │
│                                 │  amount (gerçek stopaj)               │
└────────────────┬────────────────┴──────────────────┬────────────────────┘
                 │                                   │
                 ▼                                   ▼
┌─────────────────────────────────┐  ┌─────────────────────────────────────┐
│   TrendyolOrderService.java     │  │  TrendyolOtherFinancialsService.java│
│   convertApiResponseToEntity()  │  │  syncStoppages() + saveStoppage()   │
│                                 │  │                                     │
│ 1. Her ürün için KDV hariç      │  │ 1. orderNumber + shipmentPackageId  │
│    tutar hesapla                │  │    ile TrendyolStoppage'a kaydet    │
│ 2. stoppage = kdvHaric × 0.01   │  │ 2. Eşleşen siparişi bul             │
│ 3. isStoppageEstimated = true   │  │ 3. order.stoppage = gerçek değer    │
│                                 │  │ 4. isStoppageEstimated = false      │
└────────────────┬────────────────┘  └──────────────────┬──────────────────┘
                 │                                      │
                 ▼                                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            trendyol_orders                              │
│                                                                         │
│  stoppage (DECIMAL 15,2)        - Tahmini veya gerçek değer            │
│  is_stoppage_estimated (BOOL)   - true: tahmini, false: Financial'dan  │
│  stoppage_difference (DECIMAL)  - Fark takibi (gerçek - tahmini)       │
└────────────────┬────────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         TrendyolOrderService.java                       │
│                         enrichOrderDto()                                │
│                                                                         │
│  if (isStoppageEstimated && packageNo != null) {                       │
│      → trendyol_stoppages tablosunda ara                               │
│      → Bulursa: gerçek değeri kullan, flag'i false yap                 │
│  }                                                                      │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Veri Modeli

### TrendyolOrder (Yeni Alanlar)

```java
@Entity
@Table(name = "trendyol_orders")
public class TrendyolOrder {

    // Mevcut stopaj alanı
    @Column(name = "stoppage", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal stoppage = BigDecimal.ZERO;

    // YENİ: Stopajın tahmini mi yoksa gerçek mi olduğunu belirtir
    @Column(name = "is_stoppage_estimated")
    @Builder.Default
    private Boolean isStoppageEstimated = true;

    // YENİ: Tahmini ve gerçek stopaj arasındaki fark
    @Column(name = "stoppage_difference", precision = 15, scale = 2)
    private BigDecimal stoppageDifference;
}
```

### TrendyolStoppage (Yeni Alanlar)

```java
@Entity
@Table(name = "trendyol_stoppages")
public class TrendyolStoppage {

    // Mevcut alanlar...

    // YENİ: Sipariş eşleştirme alanları
    @Column(name = "order_number", length = 50)
    private String orderNumber;

    @Column(name = "shipment_package_id")
    private Long shipmentPackageId;
}
```

### TrendyolOrderDto (Yeni Alanlar)

```java
@Builder
public record TrendyolOrderDto(
    // Mevcut alanlar...

    @JsonProperty("stoppage")
    BigDecimal stoppage,

    @JsonProperty("isStoppageEstimated")
    Boolean isStoppageEstimated,

    @JsonProperty("stoppageDifference")
    BigDecimal stoppageDifference
) {}
```

---

## Kod İmplementasyonu

### 1. Tahmini Stopaj Hesaplama

```java
// TrendyolOrderService.java
private BigDecimal calculateEstimatedStoppage(List<OrderItem> orderItems, BigDecimal totalPrice) {
    // Eğer ürün detayı yoksa, %20 KDV varsayımıyla hesapla
    if (orderItems == null || orderItems.isEmpty()) {
        return totalPrice
                .divide(BigDecimal.valueOf(1.20), 6, RoundingMode.HALF_UP)
                .multiply(FinancialConstants.STOPPAGE_RATE_DECIMAL)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Her ürün için KDV hariç tutarı hesapla ve topla
    BigDecimal totalVatExcluded = BigDecimal.ZERO;

    for (OrderItem item : orderItems) {
        // KDV oranını al (yoksa %20 varsay)
        BigDecimal vatRate = item.getSaleVatRate() != null
                ? BigDecimal.valueOf(item.getSaleVatRate())
                : BigDecimal.valueOf(20);

        // Ürün fiyatı × miktar
        BigDecimal itemPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
        int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
        BigDecimal totalItemPrice = itemPrice.multiply(BigDecimal.valueOf(quantity));

        // KDV hariç tutar = totalItemPrice / (1 + vatRate/100)
        BigDecimal divisor = BigDecimal.ONE.add(
            vatRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );
        BigDecimal vatExcludedPrice = totalItemPrice.divide(divisor, 2, RoundingMode.HALF_UP);

        totalVatExcluded = totalVatExcluded.add(vatExcludedPrice);
    }

    // Stopaj = KDV Hariç Toplam × %1
    return totalVatExcluded
            .multiply(FinancialConstants.STOPPAGE_RATE_DECIMAL)
            .setScale(2, RoundingMode.HALF_UP);
}
```

### 2. Sipariş Kaydederken

```java
// TrendyolOrderService.java - convertApiResponseToEntity()
BigDecimal stoppage = calculateEstimatedStoppage(orderItems, totalPrice);

order.setStoppage(stoppage);
order.setIsStoppageEstimated(true);  // Başlangıçta tahmini
```

### 3. Financial API'den Gerçek Stopaj Kaydı

```java
// TrendyolOtherFinancialsService.java
private int saveStoppage(Store store, TrendyolOtherFinancialsItem item) {
    // ... mevcut doğrulama kodu ...

    BigDecimal finalAmount = amount.abs();

    TrendyolStoppage stoppage = TrendyolStoppage.builder()
        .store(store)
        .transactionId(item.getId())
        .transactionDate(transactionDate)
        .amount(finalAmount)
        .invoiceSerialNumber(item.getInvoiceSerialNumber())
        .paymentOrderId(item.getPaymentOrderId())
        .description(item.getDescription())
        // YENİ: Sipariş eşleştirme alanları
        .orderNumber(item.getOrderNumber())
        .shipmentPackageId(item.getShipmentPackageId())
        .rawData(...)
        .build();

    stoppageRepository.save(stoppage);

    // YENİ: Siparişi gerçek stopaj ile güncelle
    if (item.getOrderNumber() != null) {
        updateOrderWithRealStoppage(store, item.getOrderNumber(),
            item.getShipmentPackageId(), finalAmount);
    }

    return 1;
}

private void updateOrderWithRealStoppage(Store store, String orderNumber,
                                          Long shipmentPackageId, BigDecimal realStoppage) {
    // Önce packageId ile dene
    if (shipmentPackageId != null) {
        orderRepository.findByTyOrderNumberAndPackageNoAndStore(orderNumber, shipmentPackageId, store)
            .ifPresent(order -> updateOrderStoppageFields(order, realStoppage));
        return;
    }

    // PackageId yoksa orderNumber ile tek sipariş bul
    List<TrendyolOrder> orders = orderRepository
        .findByStoreIdAndTyOrderNumber(store.getId(), orderNumber);

    if (orders.size() == 1) {
        updateOrderStoppageFields(orders.get(0), realStoppage);
    }
}

private void updateOrderStoppageFields(TrendyolOrder order, BigDecimal realStoppage) {
    BigDecimal estimated = order.getStoppage() != null ? order.getStoppage() : BigDecimal.ZERO;
    BigDecimal difference = realStoppage.subtract(estimated);

    order.setStoppage(realStoppage);
    order.setIsStoppageEstimated(false);
    order.setStoppageDifference(difference);

    orderRepository.save(order);
}
```

### 4. Enrichment (Sipariş Servis Edilirken)

```java
// TrendyolOrderService.java - enrichOrderDto()
private TrendyolOrderDto enrichOrderDto(TrendyolOrderDto dto, UUID storeId) {
    // ... mevcut kargo ve komisyon kodu ...

    // Stopaj enrichment (kargo pattern'i gibi)
    BigDecimal stoppage = dto.stoppage();
    Boolean isStoppageEstimated = dto.isStoppageEstimated();
    BigDecimal stoppageDifference = dto.stoppageDifference();

    if (dto.packageNo() != null && Boolean.TRUE.equals(isStoppageEstimated)) {
        // Gerçek stopaj ara
        List<TrendyolStoppage> stoppages = stoppageRepository
            .findByStoreIdAndShipmentPackageId(storeId, dto.packageNo());

        if (!stoppages.isEmpty()) {
            BigDecimal realStoppage = stoppages.stream()
                .map(TrendyolStoppage::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal estimatedStoppage = stoppage != null ? stoppage : BigDecimal.ZERO;
            stoppageDifference = realStoppage.subtract(estimatedStoppage);
            stoppage = realStoppage;
            isStoppageEstimated = false;
        }
    }

    return TrendyolOrderDto.builder()
        // ... diğer alanlar ...
        .stoppage(stoppage)
        .isStoppageEstimated(isStoppageEstimated)
        .stoppageDifference(stoppageDifference)
        .build();
}
```

---

## Veritabanı Migration

```sql
-- V118__add_stoppage_order_matching_fields.sql

-- TrendyolStoppage tablosuna sipariş eşleştirme alanları ekle
ALTER TABLE trendyol_stoppages ADD COLUMN IF NOT EXISTS order_number VARCHAR(50);
ALTER TABLE trendyol_stoppages ADD COLUMN IF NOT EXISTS shipment_package_id BIGINT;

-- Eşleştirme için index'ler
CREATE INDEX IF NOT EXISTS idx_stoppage_store_order
ON trendyol_stoppages(store_id, order_number);

CREATE INDEX IF NOT EXISTS idx_stoppage_store_package
ON trendyol_stoppages(store_id, shipment_package_id);

-- TrendyolOrder tablosuna flag ekle
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS is_stoppage_estimated BOOLEAN DEFAULT true;
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS stoppage_difference DECIMAL(15,2);
```

---

## Repository Sorguları

```java
// TrendyolStoppageRepository.java

// Sipariş eşleştirme sorguları
List<TrendyolStoppage> findByStoreIdAndOrderNumber(UUID storeId, String orderNumber);

List<TrendyolStoppage> findByStoreIdAndShipmentPackageId(UUID storeId, Long shipmentPackageId);

// Toplu eşleştirme için (henüz eşleştirilmemiş stopajlar)
@Query("SELECT s FROM TrendyolStoppage s WHERE s.store.id = :storeId " +
       "AND s.orderNumber IS NOT NULL AND s.shipmentPackageId IS NOT NULL")
List<TrendyolStoppage> findMatchableStoppages(@Param("storeId") UUID storeId);

// Sipariş bazında toplam stopaj
@Query("SELECT COALESCE(SUM(s.amount), 0) FROM TrendyolStoppage s " +
       "WHERE s.store.id = :storeId AND s.orderNumber = :orderNumber")
BigDecimal sumAmountByStoreAndOrderNumber(
    @Param("storeId") UUID storeId,
    @Param("orderNumber") String orderNumber);

// Package bazında toplam stopaj
@Query("SELECT COALESCE(SUM(s.amount), 0) FROM TrendyolStoppage s " +
       "WHERE s.store.id = :storeId AND s.shipmentPackageId = :shipmentPackageId")
BigDecimal sumAmountByStoreAndShipmentPackageId(
    @Param("storeId") UUID storeId,
    @Param("shipmentPackageId") Long shipmentPackageId);
```

---

## Komisyon ve Kargo ile Karşılaştırma

| Özellik | Komisyon | Kargo | Stopaj |
|---------|----------|-------|--------|
| **Tahmini Değer** | `estimatedCommission` | `estimatedShippingCost` | `stoppage` |
| **Flag** | `isCommissionEstimated` | `isShippingEstimated` | `isStoppageEstimated` |
| **Fark Takibi** | `commissionDifference` | - | `stoppageDifference` |
| **Gerçek Değer Kaynağı** | Financial Settlement API | TrendyolCargoInvoice | TrendyolStoppage (OtherFinancials) |
| **Eşleştirme Alanları** | `orderNumber`, `packageId` | `orderNumber`, `shipmentPackageId` | `orderNumber`, `shipmentPackageId` |
| **Hesaplama Formülü** | `vatBaseAmount × rate / 100` | Desi bazlı | `kdvHaricTutar × 0.01` |

---

## Edge Case'ler

### 1. KDV Oranı Bilinmediğinde

```java
// Default %20 KDV varsay
BigDecimal vatRate = item.getSaleVatRate() != null
    ? BigDecimal.valueOf(item.getSaleVatRate())
    : BigDecimal.valueOf(20);
```

### 2. Ürün Detayı Olmadığında

```java
if (orderItems == null || orderItems.isEmpty()) {
    // %20 KDV varsayımıyla hesapla
    return totalPrice
        .divide(BigDecimal.valueOf(1.20), 6, RoundingMode.HALF_UP)
        .multiply(FinancialConstants.STOPPAGE_RATE_DECIMAL)
        .setScale(2, RoundingMode.HALF_UP);
}
```

### 3. Financial Veri Geç Geldiğinde

Financial veri genellikle siparişten 7-14 gün sonra gelir. Bu süre zarfında:
- Sipariş `isStoppageEstimated = true` ile kalır
- Dashboard'da "Tahmini" etiketi gösterilebilir
- Financial veri geldiğinde otomatik güncellenir

### 4. Birden Fazla Stopaj Kaydı

Bir sipariş için birden fazla stopaj kaydı gelebilir. `enrichOrderDto()` bu değerleri toplar:

```java
BigDecimal realStoppage = stoppages.stream()
    .map(TrendyolStoppage::getAmount)
    .filter(Objects::nonNull)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

---

## Frontend Entegrasyonu (Opsiyonel)

```typescript
// Tahmini vs Gerçek stopaj ayrımı
interface OrderStoppage {
  orderId: string;
  stoppage: number;
  isStoppageEstimated: boolean;
  stoppageDifference?: number;
}

// Dashboard'da uyarı göster
{order.isStoppageEstimated && (
  <Badge variant="warning">
    Tahmini Stopaj
  </Badge>
)}

// Fark gösterimi (pozitif: underestimated, negatif: overestimated)
{order.stoppageDifference && (
  <span className={order.stoppageDifference > 0 ? "text-red-500" : "text-green-500"}>
    {order.stoppageDifference > 0 ? "+" : ""}{order.stoppageDifference} TL
  </span>
)}
```

---

## Test Senaryoları

| Senaryo | Girdi | Beklenen Çıktı |
|---------|-------|----------------|
| %20 KDV'li ürün | totalPrice=1200, vatRate=20 | stoppage=10.00 |
| %10 KDV'li ürün | totalPrice=1100, vatRate=10 | stoppage=10.00 |
| Karışık KDV | item1=%20, item2=%10 | Her ürün ayrı hesaplanıp toplanır |
| KDV oranı yok | vatRate=null | %20 varsayılır |
| Ürün detayı yok | orderItems=null | totalPrice/1.20 × 0.01 |
| Financial güncelleme | real=8.50, estimated=10.00 | difference=-1.50 |

---

## Performans Notları

1. **Index Kullanımı**: `(store_id, order_number)` ve `(store_id, shipment_package_id)` composite index'ler
2. **Batch Processing**: Financial veriler batch olarak işlenir
3. **Lazy Enrichment**: Stopaj enrichment sadece sipariş servis edilirken yapılır

---

## Monitoring Metrikleri

| Metrik | Açıklama |
|--------|----------|
| `stoppage.estimated.total` | Tahmini stopaj toplamı |
| `stoppage.confirmed.total` | Gerçek (doğrulanmış) stopaj toplamı |
| `stoppage.difference.average` | Ortalama tahmin farkı |
| `stoppage.confirmation.rate` | Doğrulama oranı (%) |

---

## İlgili Dosyalar

### Backend
- `TrendyolOrder.java` - Entity: `isStoppageEstimated`, `stoppageDifference`
- `TrendyolStoppage.java` - Entity: `orderNumber`, `shipmentPackageId`
- `TrendyolOrderService.java` - `calculateEstimatedStoppage()`, `enrichOrderDto()`
- `TrendyolOtherFinancialsService.java` - `saveStoppage()`, `updateOrderWithRealStoppage()`
- `TrendyolStoppageRepository.java` - Eşleştirme sorguları
- `TrendyolOrderDto.java` - DTO: `isStoppageEstimated`, `stoppageDifference`

### Migration
- `V118__add_stoppage_order_matching_fields.sql`

### İlgili Dokümantasyon
- [COMMISSION_SYSTEM.md](COMMISSION_SYSTEM.md) - Komisyon sistemi (benzer pattern)

---

## Tarihçe

| Tarih | Değişiklik |
|-------|------------|
| 2025-02-13 | İlk versiyon: Stopaj tahmin, eşleştirme ve enrichment sistemi |
