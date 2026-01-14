# sellerx-backend

## 🛍️ Products Feature

Mağazaların ürünlerini Trendyol'dan çekip database'e kaydetmek için ürün yönetimi sistemi eklenmiştir.

### Yeni Ürün Endpoint'leri:

#### 1. Trendyol'dan Ürünleri Senkronize Et

- **Endpoint:** `POST /products/sync/{storeId}`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
  ```json
  {
    "success": true,
    "message": "Products synced successfully",
    "totalFetched": 150,
    "totalSaved": 100,
    "totalUpdated": 50
  }
  ```

#### 2. Mağazanın Ürünlerini Getir

- **Endpoint:** `GET /products/store/{storeId}`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
  ```json
  [
    {
      "id": "product-uuid",
      "storeId": "store-uuid",
      "productId": "trendyol-product-id",
      "barcode": "1234567890",
      "title": "Ürün Adı",
      "categoryName": "Kategori",
      "brand": "Marka",
      "salePrice": 150.0,
      "vatRate": 18,
      "quantity": 100,
      "costAndStockInfo": [
        {
          "quantity": 100,
          "unitCost": 75.0,
          "costVatRate": 18,
          "stockDate": "2024-01-15T10:00:00"
        }
      ]
    }
  ]
  ```

#### 3. Ürün Maliyet ve Stok Bilgisi Güncelle

- **Endpoint:** `PUT /products/{productId}/cost-and-stock`
- **Headers:** `Authorization: Bearer {token}`
- **Body (JSON):**
  ```json
  {
    "quantity": 100,
    "unitCost": 75.5,
    "costVatRate": 18,
    "stockDate": "2024-01-15T10:00:00"
  }
  ```

### Database Yapısı

`trendyol_products` tablosu oluşturulmuştur:

- `id`: UUID (Primary Key)
- `store_id`: UUID (Foreign Key to stores table)
- `product_id`: Trendyol ürün ID'si
- `barcode`: Ürün barkodu
- `title`: Ürün adı
- `category_name`: Kategori adı
- `brand`: Marka
- `sale_price`: Satış fiyatı
- `vat_rate`: KDV oranı
- `quantity`: Mevcut stok miktarı
- `cost_and_stock_info`: JSONB - Maliyet ve stok geçmişi

### Maliyet ve Stok Yönetimi

Ürünlerin maliyet ve stok bilgileri JSONB formatında saklanır:

```json
[
  {
    "quantity": 100,
    "unitCost": 75.5,
    "costVatRate": 18,
    "stockDate": "2024-01-15T10:00:00Z"
  }
]
```

Bu yapı sayesinde:

- Farklı tarihlerdeki maliyet değişiklikleri takip edilebilir
- Stok hareketleri geçmişi tutulabilir
- Detaylı kar/zarar analizleri yapılabilir

## 🏪 Store Selection System

Bu backend'e store selection (mağaza seçme) sistemi eklenmiştir. Bu sistem kullanıcıların birden fazla mağazası olduğunda, hangi mağaza ile çalışmak istediklerini seçmelerini sağlar.

### Yeni Endpoint'ler:

#### 1. Seçili Mağazayı Getir

- **Endpoint:** `GET /users/selected-store`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
  ```json
  {
    "selectedStoreId": "store-uuid" // Seçili mağaza UUID'si, seçili değilse boş string
  }
  ```

#### 2. Mağaza Seç

- **Endpoint:** `POST /users/selected-store`
- **Headers:** `Authorization: Bearer {token}`
- **Body (JSON):**
  ```json
  {
    "storeId": "store-uuid" // Seçilmek istenen mağaza UUID'si
  }
  ```
- **Response:**
  ```json
  {
    "success": true // İşlem başarılı olduğunda true döner
  }
  ```

#### 3. Dashboard İstatistikleri (Detaylı Trendyol Benzeri)

- **Endpoint:** `GET /dashboard/stats`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
  ```json
  {
    "storeId": "selected-store-uuid", // Seçili mağaza ID'si
    "today": {
      "date": "15 Aralık 2024", // Bugünün tarihi
      "revenue": 694.06, // Günlük toplam ciro
      "currency": "TL", // Para birimi
      "orders": 51, // Net sipariş sayısı
      "units": 54, // Toplam satılan birim
      "returns": 6, // İade sayısı
      "adCost": 4.31, // Reklam maliyeti
      "estimatedPayment": 430.43, // Tahmini ödeme tutarı
      "grossProfit": 184.66, // Brüt kar
      "netProfit": 33.66, // Net kar
      "details": {
        "grossRevenue": 694.06, // Brüt ciro
        "grossSalesCount": 54, // Brüt satış adedi
        "netRevenue": 694.06, // Net ciro
        "netSalesCount": 51, // Net satış adedi
        "adExpenses": -4.31, // Reklam giderleri
        "couponDetails": {
          "total": 0.0, // Toplam kupon tutarı
          "adSpending": -0.81, // Reklam harcaması
          "discount": 0.0, // İndirim tutarı
          "coupon": 0.0 // Kupon tutarı
        },
        "productCost": -103.68, // Ürün maliyeti
        "shippingCost": -25.5, // Kargo maliyeti
        "returnCost": -12.0, // İade maliyeti
        "otherExpenses": -8.0, // Diğer giderler
        "internationalDetails": {
          "total": 0.0, // Toplam yurt dışı operasyon bedeli
          "internationalServiceFee": 0.0, // Uluslararası hizmet bedeli
          "internationalOperationFee": 0.0, // Yurt dışı operasyon bedeli
          "termDelayFee": 0.0, // Termin gecikme bedeli
          "platformServiceFee": 0.0, // Platform hizmet bedeli
          "invoiceCounterSalesFee": 0.0, // Fatura kontör satış bedeli
          "supplyFailureFee": 0.0, // Tedarik edememe bedeli
          "azInternationalOperationFee": 0.0, // AZ-Yurtdışı operasyon bedeli
          "azPlatformServiceFee": 0.0, // AZ-Platform hizmet bedeli
          "eCommerceWithholdingExpense": 0.0 // E-Ticaret stopaj gideri
        },
        "extraExpenses": -3.5, // Ekstra giderler
        "packagingDetails": {
          "total": 1.0, // Toplam ambalaj gideri
          "officeExpense": 0.0, // Ofis gideri
          "packaging": 1.0, // Ambalaj maliyeti
          "accountingEtc": 2.0 // Muhasebe vb. giderler
        },
        "trendyolCommissionAmount": -104.11, // Trendyol komisyon tutarı
        "vatDifference": -15.2, // KDV farkı / KDV gideri
        "trendyolCommissionRate": 15.0, // Trendyol komisyon oranı (%)
        "returnRate": 11.11, // İade oranı (%)
        "netProfit": 33.66, // Net kâr
        "roi": 32.46, // Yatırım getirisi (ROI) (%)
        "profitMargin": 4.85 // Kâr marjı (%)
      }
    },
    "yesterday": {
      /* Dün için aynı yapıda veriler */
    },
    "monthToDate": {
      /* Aya kadar için aynı yapıda veriler */
    },
    "lastMonth": {
      /* Geçen ay için aynı yapıda veriler */
    }
  }
  ```

📊 **Dashboard Özellikleri:**

- ✅ Bugün, dün, aya kadar, geçen ay verileri
- ✅ Her dönem için detaylı maliyet analizi
- ✅ ROI, kar marjı, iade oranı hesaplamaları
- ✅ Trendyol komisyon ve maliyet detayları
- ✅ **Detaylı Kupon Analizi:** Reklam harcaması, indirim, kupon ayrımı
- ✅ **Kapsamlı Yurt Dışı Operasyon Giderleri:** 10+ farklı ücret türü
- ✅ **Ambalaj ve Ekstra Gider Detayları:** Ofis, muhasebe, ambalaj ayrımı
- ✅ **Gerçekçi Mock Data:** Trendyol API benzeri yapı (gelecek entegrasyona hazır)

### 📋 Detaylı Veri Yapısı:

#### 🎫 Kupon Detayları (`couponDetails`):

- `adSpending` - Reklam Harcaması
- `discount` - İndirim
- `coupon` - Kupon

#### 🌍 Yurt Dışı Operasyon (`internationalDetails`):

- `internationalServiceFee` - Uluslararası Hizmet Bedeli
- `termDelayFee` - Termin Gecikme Bedeli
- `platformServiceFee` - Platform Hizmet Bedeli
- `supplyFailureFee` - Tedarik Edememe
- `eCommerceWithholdingExpense` - E-Ticaret Stopaj Gideri
- ve 5+ ek ücret türü...

#### 📦 Ambalaj Detayları (`packagingDetails`):

- `officeExpense` - Ofis Gideri
- `packaging` - Ambalaj
- `accountingEtc` - Muhasebe vb.

### 🔧 Database Değişiklikleri:

- `users` tablosuna `selected_store_id` kolonu eklendi
- Foreign key constraint ile `stores` tablosuna bağlandı

### 🚀 Özellikler:

- ✅ Kullanıcı ilk mağazasını oluştururken otomatik seçili hale gelir
- ✅ Mağaza sahipliği kontrolü yapılır
- ✅ JWT token ile kimlik doğrulama
- ✅ Tüm store-specific endpoint'ler seçili mağaza ile çalışır

---

## API Kullanımı (Authentication & Kullanıcı İşlemleri)

### 1. Kayıt Ol (Register)

- **Endpoint:** `POST /users`
- **Body (JSON):**
  ```json
  {
    "name": "Mehmet", // Kullanıcı adı
    "email": "mehmet@example.com", // E-posta adresi (benzersiz olmalı)
    "password": "123456" // Şifre (en az 6 karakter)
  }
  ```
- **Açıklama:** Herkese açık. Yeni kullanıcı oluşturur.

### 2. Giriş Yap (Login)

- **Endpoint:** `POST /auth/login`
- **Body (JSON):**
  ```json
  {
    "email": "mehmet@example.com", // Kayıtlı e-posta adresi
    "password": "123456" // Kullanıcının şifresi
  }
  ```
- **Açıklama:** Doğru bilgilerle giriş yapınca response olarak access token döner. Ayrıca refreshToken cookie olarak gelir.

### 3. Kullanıcıları Listele

- **Endpoint:** `GET /users`
- **Header:**
  ```
  Authorization: Bearer <access_token>
  ```
- **Açıklama:** Login sonrası dönen token ile erişilir.

### 4. Kendi Bilgini Görüntüle

- **Endpoint:** `GET /auth/me`
- **Header:**
  ```
  Authorization: Bearer <access_token>
  ```
- **Açıklama:** Giriş yapan kullanıcının bilgilerini döner.

### 5. Token Yenile (Refresh)

- **Endpoint:** `POST /auth/refresh`
- **Açıklama:** Access token süresi biterse, refreshToken cookie'si ile yeni access token alınır.

---

### Postman Collection

Tüm bu işlemleri kolayca test etmek için hazır Postman collection dosyasını kullanabilirsin:

- `SellerX-Postman-Collection.json` dosyasını Postman'a import et.
- Login sonrası access token otomatik olarak diğer isteklerde kullanılır.
