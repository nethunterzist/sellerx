# Komisyon Hesaplama Sistemi

> Trendyol siparişleri için komisyon hesaplama ve takip altyapısı.

## Genel Bakış

SellerX, siparişler için iki aşamalı komisyon hesaplama sistemi kullanır:

1. **Tahmini Komisyon**: Sipariş geldiğinde, ürünün bilinen komisyon oranıyla hesaplanır
2. **Gerçek Komisyon**: Finansal mutabakat verisi geldiğinde, gerçek oranla güncellenir

---

## Komisyon Formülü

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         KOMİSYON FORMÜLÜ                                │
│                                                                         │
│   estimatedCommission = vatBaseAmount × commissionRate / 100            │
│                                                                         │
│   Örnek:                                                                │
│   vatBaseAmount = 1000 TL (KDV hariç tutar)                            │
│   commissionRate = 15%                                                  │
│   estimatedCommission = 1000 × 15 / 100 = 150 TL                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### Neden vatBaseAmount?

| Alan | Açıklama | Komisyon Hesabında Kullanılır |
|------|----------|-------------------------------|
| `unitPriceOrder` | Liste fiyatı | ❌ |
| `unitPriceDiscount` | İndirimli fiyat | ❌ |
| `vatBaseAmount` | KDV hariç satış tutarı | ✅ |

Trendyol, komisyonu KDV hariç tutar üzerinden hesaplar. Bu yüzden `vatBaseAmount` kullanılır.

---

## Komisyon Oranı Fallback Mantığı

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    KOMİSYON ORANI SIRALAMA                              │
│                                                                         │
│   1. lastCommissionRate   → Son bilinen gerçek oran (finansaldan)      │
│   2. commissionRate       → Ürün komisyon oranı (ürün bilgisinden)     │
│   3. 0                    → Hiç oran yoksa 0 kullan                    │
└─────────────────────────────────────────────────────────────────────────┘
```

```java
// TrendyolOrderService.java
private BigDecimal calculateCommission(String barcode, BigDecimal vatBaseAmount, UUID storeId) {
    // 1. Önce ürünü bul
    Optional<TrendyolProduct> productOpt = productRepository
        .findByStoreIdAndBarcode(storeId, barcode);

    if (productOpt.isEmpty()) {
        return BigDecimal.ZERO;
    }

    TrendyolProduct product = productOpt.get();

    // 2. Komisyon oranını belirle (fallback sırası)
    BigDecimal rate = BigDecimal.ZERO;

    if (product.getLastCommissionRate() != null) {
        // Finansal veriden gelen son bilinen oran (en güvenilir)
        rate = product.getLastCommissionRate();
    } else if (product.getCommissionRate() != null) {
        // Ürün bilgisinden gelen oran
        rate = product.getCommissionRate();
    }

    // 3. Komisyonu hesapla: vatBaseAmount × rate / 100
    return vatBaseAmount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
}
```

---

## Veri Modeli

### TrendyolProduct (Yeni Alanlar)

```java
@Entity
@Table(name = "trendyol_products")
public class TrendyolProduct {

    // Mevcut alanlar
    @Column(name = "commission_rate")
    private BigDecimal commissionRate;      // Ürün API'sinden gelen oran

    // YENİ: Finansal veriden gelen son bilinen oran
    @Column(name = "last_commission_rate")
    private BigDecimal lastCommissionRate;

    // YENİ: Son komisyon güncelleme tarihi
    @Column(name = "last_commission_date")
    private LocalDateTime lastCommissionDate;
}
```

### TrendyolOrder (Yeni Alan)

```java
@Entity
@Table(name = "trendyol_orders")
public class TrendyolOrder {

    // Mevcut alan
    @Column(name = "estimated_commission")
    private BigDecimal estimatedCommission;

    // YENİ: Komisyonun tahmini mi yoksa gerçek mi olduğunu belirtir
    @Column(name = "is_commission_estimated")
    private Boolean isCommissionEstimated = true;
}
```

---

## İki Aşamalı Komisyon Akışı

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    AŞAMA 1: SİPARİŞ GELDİĞİNDE                         │
└─────────────────────────────────────────────────────────────────────────┘

Sipariş API'den gelir
        │
        ▼
┌─────────────────────┐
│ Ürünü barcode ile   │
│ veritabanında bul   │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐     ┌─────────────────────┐
│ lastCommissionRate  │ OR  │   commissionRate    │ → rate
│    (varsa)          │     │     (varsa)         │
└─────────────────────┘     └─────────────────────┘
          │
          ▼
┌─────────────────────────────────────────┐
│ estimatedCommission = vatBaseAmount     │
│                       × rate / 100      │
│ isCommissionEstimated = TRUE            │
└─────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                 AŞAMA 2: FİNANSAL VERİ GELDİĞİNDE                       │
└─────────────────────────────────────────────────────────────────────────┘

Financial Settlement API'den gelir
        │
        ▼
┌─────────────────────┐
│ Her settlement için │
│ siparişi bul        │
└─────────┬───────────┘
          │
          ├──▶ Order bulundu
          │         │
          │         ▼
          │    ┌─────────────────────────────┐
          │    │ order.isCommissionEstimated │
          │    │        = FALSE              │
          │    └─────────────────────────────┘
          │
          └──▶ Ürünü barcode ile bul
                    │
                    ▼
               ┌─────────────────────────────────┐
               │ product.lastCommissionRate      │
               │        = settlement'tan gelen   │
               │          gerçek komisyon oranı  │
               │                                 │
               │ product.lastCommissionDate      │
               │        = NOW()                  │
               └─────────────────────────────────┘
```

---

## Kod Implementasyonu

### 1. Sipariş Kaydederken

```java
// TrendyolOrderService.java
private TrendyolOrder processOrderLine(OrderLineDto line, UUID storeId, Store store) {
    TrendyolOrder order = new TrendyolOrder();

    // Temel bilgileri set et
    order.setOrderNumber(line.getOrderNumber());
    order.setBarcode(line.getBarcode());
    order.setVatBaseAmount(line.getVatBaseAmount());

    // Komisyonu hesapla
    BigDecimal commission = calculateCommission(
        line.getBarcode(),
        line.getVatBaseAmount(),
        storeId
    );

    order.setEstimatedCommission(commission);
    order.setIsCommissionEstimated(true);  // Başlangıçta tahmini

    return order;
}
```

### 2. Finansal Mutabakat Geldiğinde

```java
// TrendyolFinancialSettlementService.java
private void processSettlement(SettlementDto settlement, UUID storeId) {
    String orderNumber = settlement.getOrderNumber();
    String barcode = settlement.getBarcode();
    BigDecimal actualCommissionRate = settlement.getCommissionRate();

    // 1. Siparişi bul ve isCommissionEstimated'ı false yap
    orderRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
        .ifPresent(order -> {
            order.setIsCommissionEstimated(false);
            orderRepository.save(order);
        });

    // 2. Ürünün lastCommissionRate'ini güncelle
    productRepository.findByStoreIdAndBarcode(storeId, barcode)
        .ifPresent(product -> {
            product.setLastCommissionRate(actualCommissionRate);
            product.setLastCommissionDate(LocalDateTime.now());
            productRepository.save(product);
        });
}
```

---

## Veritabanı Migration

```sql
-- V45__add_commission_tracking_fields.sql

-- Ürünlere son komisyon bilgisi ekle
ALTER TABLE trendyol_products
ADD COLUMN IF NOT EXISTS last_commission_rate DECIMAL(5,2);

ALTER TABLE trendyol_products
ADD COLUMN IF NOT EXISTS last_commission_date TIMESTAMP;

-- Siparişlere komisyon tahmini flag'i ekle
ALTER TABLE trendyol_orders
ADD COLUMN IF NOT EXISTS is_commission_estimated BOOLEAN DEFAULT TRUE;

-- Performans için index
CREATE INDEX IF NOT EXISTS idx_orders_commission_estimated
ON trendyol_orders(is_commission_estimated);

CREATE INDEX IF NOT EXISTS idx_products_store_barcode
ON trendyol_products(store_id, barcode);
```

---

## Komisyon Raporlama

### Dashboard'da Gösterim

```typescript
// Tahmini vs Gerçek komisyon ayrımı
interface OrderCommission {
  orderId: string;
  estimatedCommission: number;
  isCommissionEstimated: boolean;
}

// Dashboard'da uyarı göster
{order.isCommissionEstimated && (
  <Badge variant="warning">
    Tahmini Komisyon
  </Badge>
)}
```

### Toplam Komisyon Hesabı

```java
// DashboardStatsService.java
public CommissionStats getCommissionStats(UUID storeId, LocalDate startDate, LocalDate endDate) {
    // Tahmini komisyon toplamı
    BigDecimal estimatedTotal = orderRepository
        .sumEstimatedCommissionByStoreAndDateRange(storeId, startDate, endDate, true);

    // Gerçek (doğrulanmış) komisyon toplamı
    BigDecimal confirmedTotal = orderRepository
        .sumEstimatedCommissionByStoreAndDateRange(storeId, startDate, endDate, false);

    return new CommissionStats(estimatedTotal, confirmedTotal);
}
```

---

## Komisyon Doğruluk Takibi

### Tahmini vs Gerçek Karşılaştırma

```sql
-- Komisyon doğruluk oranını hesapla
SELECT
    COUNT(*) as total_orders,
    COUNT(*) FILTER (WHERE is_commission_estimated = false) as confirmed_orders,
    ROUND(
        COUNT(*) FILTER (WHERE is_commission_estimated = false)::decimal /
        COUNT(*)::decimal * 100, 2
    ) as confirmation_rate
FROM trendyol_orders
WHERE store_id = :storeId
  AND order_date BETWEEN :startDate AND :endDate;
```

### Örnek Sonuç

| Metrik | Değer |
|--------|-------|
| Toplam Sipariş | 1,000 |
| Doğrulanmış Sipariş | 850 |
| Doğrulama Oranı | 85% |

---

## Edge Case'ler

### 1. Ürün Bulunamadığında

```java
if (productOpt.isEmpty()) {
    log.warn("Product not found for barcode: {}, using 0 commission", barcode);
    return BigDecimal.ZERO;
}
```

### 2. Hiç Komisyon Oranı Yokken

```java
if (rate.compareTo(BigDecimal.ZERO) == 0) {
    log.info("No commission rate for product: {}", barcode);
}
// Yine de 0 komisyon ile devam et
```

### 3. Finansal Veri Geç Geldiğinde

Finansal veri genellikle siparişten 7-14 gün sonra gelir. Bu süre zarfında:
- Sipariş `isCommissionEstimated = true` ile kalır
- Dashboard'da "Tahmini" etiketi gösterilir
- Finansal veri geldiğinde otomatik güncellenir

---

## Performans Notları

1. **Index Kullanımı**: `store_id + barcode` composite index ile hızlı lookup
2. **Batch Processing**: Finansal veriler batch olarak işlenir
3. **Caching**: Sık kullanılan ürün komisyon oranları cache'lenebilir

---

## Test Senaryoları

| Senaryo | Girdi | Beklenen Çıktı |
|---------|-------|----------------|
| Normal sipariş | vatBase=1000, rate=15% | commission=150 |
| Oran yok | vatBase=1000, rate=null | commission=0 |
| Fallback | lastRate=12%, rate=15% | commission kullanır 12% |
| Finansal güncelleme | settlement rate=14% | lastCommissionRate=14% |
