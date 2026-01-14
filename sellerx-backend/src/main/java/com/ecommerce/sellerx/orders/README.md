# Trendyol Orders API

Bu feature Trendyol'dan sipariş verilerini çekip database'e kaydeder.

## API Endpoints

### 1. Sipariş Senkronizasyonu (Tek Store)

Belirli bir store için Trendyol'dan siparişleri çeker ve database'e kaydeder.

```http
POST /api/orders/stores/{storeId}/sync
```

**Örnek:**

```bash
curl -X POST "http://localhost:8080/api/orders/stores/123e4567-e89b-12d3-a456-426614174000/sync"
```

### 1.1. Tüm Store'lar için Manuel Senkronizasyon

Tüm Trendyol store'ları için siparişleri çeker.

```http
POST /api/orders/sync-all
```

**Örnek:**

```bash
curl -X POST "http://localhost:8080/api/orders/sync-all"
```

### 2. Store Siparişlerini Listele

Bir store'un siparişlerini sayfalama ile listeler.

```http
GET /api/orders/stores/{storeId}?page=0&size=20
```

**Örnek:**

```bash
curl "http://localhost:8080/api/orders/stores/123e4567-e89b-12d3-a456-426614174000?page=0&size=20"
```

### 3. Tarih Aralığına Göre Siparişler

Belirli tarih aralığındaki siparişleri getirir.

```http
GET /api/orders/stores/{storeId}/by-date-range?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59&page=0&size=20
```

**Örnek:**

```bash
curl "http://localhost:8080/api/orders/stores/123e4567-e89b-12d3-a456-426614174000/by-date-range?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59"
```

### 4. Statüye Göre Siparişler

Belirli statüdeki siparişleri getirir.

```http
GET /api/orders/stores/{storeId}/by-status?status=Delivered&page=0&size=20
```

**Örnek:**

```bash
curl "http://localhost:8080/api/orders/stores/123e4567-e89b-12d3-a456-426614174000/by-status?status=Delivered"
```

**Mevcut Statüler:**

- `Delivered` - Teslim Edildi
- `Returned` - İade Edildi
- `UnDeliveredAndReturned` - Teslim Edilemedi ve İade Edildi
- `UnPacked` - Paketlenmedi
- `Shipped` - Kargoya Verildi
- `Created` - Oluşturuldu

### 5. Sipariş İstatistikleri

Store için sipariş istatistiklerini getirir.

```http
GET /api/orders/stores/{storeId}/statistics
```

**Örnek:**

```bash
curl "http://localhost:8080/api/orders/stores/123e4567-e89b-12d3-a456-426614174000/statistics"
```

**Yanıt:**

```json
{
  "totalOrders": 150,
  "deliveredOrders": 120,
  "returnedOrders": 10
}
```

## Veri Yapısı

### TrendyolOrder Entity

```sql
CREATE TABLE IF NOT EXISTStrendyol_orders (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id UUID NOT NULL,
    ty_order_number VARCHAR(255) NOT NULL,
    package_no BIGINT NOT NULL,
    order_date TIMESTAMP NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL,
    total_discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_ty_discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    order_items JSONB NOT NULL DEFAULT '[]'::jsonb,
    shipment_package_status VARCHAR(100),
    status VARCHAR(100),
    cargo_deci INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### OrderItem JSONB Yapısı

```json
{
  "barcode": "8809670682118",
  "productName": "Ürün Adı",
  "quantity": 2,
  "unitPriceOrder": 290.01,
  "unitPriceDiscount": 7.99,
  "unitPriceTyDiscount": 0.0,
  "vatBaseAmount": 20.0,
  "price": 282.02,
  "cost": 150.0,
  "costVat": 18
}
```

## Önemli Notlar

1. **Paket Numarası Kontrolü**: Sadece `cargoTrackingNumber` (paket numarası) olan siparişler kaydedilir.

2. **Dublicate Prevention**: Aynı store ve package numarası ile birden fazla kayıt oluşturulmaz.

3. **Maliyet Bilgisi**: Eğer ürün `trendyol_products` tablosunda mevcutsa, maliyet bilgisi otomatik olarak eklenir.

4. **Tarih Dönüşümü**: Trendyol'dan gelen `originShipmentDate` (milisaniye) otomatik olarak `LocalDateTime`'a dönüştürülür.

5. **API Limitleri**: Trendyol API'si varsayılan olarak son 15 günün verilerini döner. Şu anda sayfa başına maksimum 200 kayıt çekiliyor.

## Kullanım Senaryoları

### 1. İlk Senkronizasyon

Yeni bir store ekledikten sonra ilk kez siparişleri çekmek için:

```bash
curl -X POST "http://localhost:8080/api/orders/stores/{storeId}/sync"
```

### 2. Günlük Senkronizasyon (Önerilen)

Cron job veya scheduled task olarak günlük çalıştırılabilir.

### 3. Sipariş Analizi

Frontend'de dashboard oluşturmak için istatistik endpoint'ini kullanın:

```bash
curl "http://localhost:8080/api/orders/stores/{storeId}/statistics"
```

### 4. Sipariş Takibi

Belirli statüdeki siparişleri takip etmek için:

```bash
curl "http://localhost:8080/api/orders/stores/{storeId}/by-status?status=Delivered"
```

## Hata Yönetimi

- **Store bulunamadı**: `Store not found: {storeId}`
- **Trendyol store değil**: `Store is not a Trendyol store`
- **Credentials eksik**: `Trendyol credentials not found`
- **API hatası**: `Failed to fetch orders from Trendyol: {error}`
