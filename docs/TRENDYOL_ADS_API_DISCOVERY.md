# Trendyol Reklam API'leri - Keşif Raporu

**Tarih**: 24 Ocak 2026
**Durum**: ✅ Tüm API'ler Test Edildi ve Çalışıyor

## Özet

Trendyol Partner Panel'in dahili API'leri reverse engineering ile keşfedildi. Bu API'ler resmi olarak dokümante edilmemiş olsa da, çalışıyor ve SellerX entegrasyonu için kullanılabilir.

## ⚠️ Önemli Uyarılar

1. **Resmi Olmayan API'ler**: Bu endpoint'ler Trendyol'un dahili kullanımı için tasarlanmış olup, herhangi bir uyarı olmaksızın değişebilir.
2. **Rate Limiting**: Trendyol API'lerinde 10 req/sec limit var, aşırı kullanımda hesap askıya alınabilir.
3. **Authentication**: Tüm istekler `auth_token` cookie'sinden alınan JWT Bearer token gerektirir.
4. **CORS**: Bu API'ler doğrudan browser'dan çağrılamaz, backend proxy gerektirir.

---

## 🔐 Authentication

Tüm API istekleri için:

```
Authorization: Bearer {auth_token}
Content-Type: application/json
Accept: application/json
```

`auth_token` değeri Trendyol Partner Panel'e giriş yapıldığında cookie olarak set edilir.

---

## 📊 API Endpoint'leri

### 1. Ürün Reklamları (Product Ads)

**Service**: `discovery-productads-sellereditorbff-service`

#### Reklam Listesi
```http
POST https://apigw.trendyol.com/partner/discovery-productads-sellereditorbff-service/a/search
Content-Type: application/json

{
  "page": 0,
  "size": 20
}
```

**Response**:
```json
{
  "content": [
    {
      "advertId": "uuid",
      "name": "Reklam Adı",
      "status": "STARTED|STOPPED|PENDING",
      "budget": {
        "daily": 500,
        "total": 10000
      },
      "performance": {
        "impressions": 12500,
        "clicks": 350,
        "ctr": 2.8,
        "spend": 1250.50,
        "revenue": 8500.00,
        "roi": 6.8,
        "cpc": 3.57
      },
      "products": [...],
      "startDate": "2026-01-01T00:00:00",
      "endDate": "2026-01-31T23:59:59"
    }
  ],
  "totalElements": 15,
  "totalPages": 1
}
```

#### Bütçe Bilgisi
```http
GET https://apigw.trendyol.com/partner/discovery-productads-sellereditorbff-service/budget
```

**Response**:
```json
{
  "sellerId": 1080066,
  "usableAmount": 7412.25,
  "totalSpentAmount": 141902.75,
  "totalActiveAllocationAmount": 57500,
  "totalDepositedAmount": 19365,
  "totalCreditCardDepositedAmount": 185700,
  "currency": "₺"
}
```

---

### 2. Mağaza Reklamları (Store Ads)

**Service**: `discovery-storeads-sellereditorbff-service`

#### Reklam Listesi
```http
POST https://apigw.trendyol.com/partner/discovery-storeads-sellereditorbff-service/a/search
Content-Type: application/json

{
  "page": 0,
  "size": 20,
  "kinds": ["STORE_ADS"]
}
```

**Response**:
```json
{
  "content": [
    {
      "advertId": "uuid",
      "name": "Mağaza-26.11.2025 18:38",
      "status": "STOPPED",
      "kind": "STORE_ADS",
      "budget": {...},
      "performance": {...}
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

#### Reklam Sayısı
```http
GET https://apigw.trendyol.com/partner/discovery-storeads-sellereditorbff-service/a/counts?kind=STORE_ADS
```

---

### 3. Influencer Reklamları (Affiliate Ads)

**Service**: `discovery-affiliateads-sc-editorbff-service`

#### Reklam Listesi
```http
GET https://apigw.trendyol.com/partner/discovery-affiliateads-sc-editorbff-service/v2/influencer-a?itemCount=10&pageIndex=1
```

**Response**:
```json
{
  "results": [
    {
      "advertId": "uuid",
      "sellerId": 1080066,
      "type": "INFLUENCER",
      "kind": "COMMISSION",
      "subKind": "SELECTED_PRODUCTS",
      "name": "Influencer-20.01.2026 00:12",
      "status": "STOPPING",
      "startDate": "2026-01-20T00:00:00.000+03:00",
      "endDate": "2026-01-26T23:59:59.999+03:00",
      "budget": {
        "currency": "TRY",
        "rateAmount": 15,
        "totalAmountText": "%15"
      },
      "storefront": {
        "id": 1,
        "culture": "tr-TR",
        "countryName": "Türkiye"
      }
    }
  ],
  "totalCount": 26,
  "pageCount": 3
}
```

#### Reklam Sayısı
```http
GET https://apigw.trendyol.com/partner/discovery-affiliateads-sc-editorbff-service/v2/influencer-a/count
```

#### Status Değerleri
- `PREPARING`, `PENDING`, `IN_PROGRESS`, `FINISHED`
- `REJECTED_SELLER`, `REJECTED_PROD`, `REJECTED_DATE`
- `MINUS_ONE`, `PROCESSING`, `STOPPING`, `STOPPED`, `CANCELLED`

---

### 4. Entegre Reklamlar (Meta/TAA Ads)

**Service**: `discovery-integratedads-sellereditorbff-service`

#### Reklam Listesi
```http
POST https://apigw.trendyol.com/partner/discovery-integratedads-sellereditorbff-service/taas/all
Content-Type: application/json

{
  "page": 0,
  "size": 20
}
```

**Response**:
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Meta Campaign",
      "status": "ACTIVE",
      "platform": "META",
      "budget": {...},
      "performance": {...}
    }
  ],
  "totalElements": 0,
  "totalPages": 0
}
```

---

## 🔧 SellerX Entegrasyon Önerisi

### Backend Proxy Yapısı

```java
// TrendyolAdsService.java
@Service
public class TrendyolAdsService {

    private final TrendyolRateLimiter rateLimiter;

    public ProductAdsResponse getProductAds(Store store, int page, int size) {
        rateLimiter.acquire();

        String url = "https://apigw.trendyol.com/partner/discovery-productads-sellereditorbff-service/a/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(store.getCredentials().getAuthToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("page", page, "size", size);

        return restTemplate.postForObject(url, new HttpEntity<>(body, headers), ProductAdsResponse.class);
    }

    public BudgetResponse getBudget(Store store) {
        rateLimiter.acquire();

        String url = "https://apigw.trendyol.com/partner/discovery-productads-sellereditorbff-service/budget";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(store.getCredentials().getAuthToken());

        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), BudgetResponse.class).getBody();
    }
}
```

### Auth Token Yönetimi

Sorun: `auth_token` partner.trendyol.com'a login ile alınıyor, API credentials ile değil.

**Çözüm Seçenekleri**:

1. **Manuel Token Girişi**: Kullanıcıdan browser'dan auth_token'ı kopyalamasını iste
2. **Headless Browser**: Selenium/Playwright ile otomatik login ve token alma
3. **Browser Extension**: Chrome extension ile token yakalama

### Önerilen Yaklaşım

```
Kullanıcı Flow:
1. SellerX'te "Reklam Verilerini Bağla" butonuna tıklar
2. Trendyol Partner Panel'e yönlendirilir (yeni sekme)
3. Giriş yapar
4. Browser extension token'ı yakalar ve SellerX'e gönderir
5. Token DB'de encrypted olarak saklanır
6. Scheduled job ile reklam verileri sync edilir
```

---

## 📈 Veri Modeli Önerisi

```sql
-- Reklam kampanyaları
CREATE TABLE trendyol_ad_campaigns (
    id UUID PRIMARY KEY,
    store_id BIGINT REFERENCES stores(id),
    advert_id VARCHAR(100) NOT NULL,
    ad_type VARCHAR(50) NOT NULL, -- PRODUCT, STORE, INFLUENCER, META
    name VARCHAR(255),
    status VARCHAR(50),
    budget JSONB,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(store_id, advert_id)
);

-- Günlük performans verileri
CREATE TABLE trendyol_ad_daily_stats (
    id BIGSERIAL PRIMARY KEY,
    campaign_id UUID REFERENCES trendyol_ad_campaigns(id),
    stat_date DATE NOT NULL,
    impressions BIGINT DEFAULT 0,
    clicks BIGINT DEFAULT 0,
    spend DECIMAL(12,2) DEFAULT 0,
    revenue DECIMAL(12,2) DEFAULT 0,
    orders INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(campaign_id, stat_date)
);

-- Bütçe snapshot'ları
CREATE TABLE trendyol_ad_budget_snapshots (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT REFERENCES stores(id),
    snapshot_date TIMESTAMP DEFAULT NOW(),
    usable_amount DECIMAL(12,2),
    total_spent DECIMAL(12,2),
    total_deposited DECIMAL(12,2),
    raw_response JSONB
);
```

---

## 🚀 Sonraki Adımlar

1. **[Yüksek Öncelik]** Auth token yönetimi stratejisi belirlenmeli
2. **[Orta Öncelik]** Backend service ve repository oluşturulmalı
3. **[Orta Öncelik]** Scheduled sync job implement edilmeli
4. **[Düşük Öncelik]** Frontend dashboard UI tasarlanmalı

---

## 📝 Test Notları

| API | Endpoint | Status | Notlar |
|-----|----------|--------|--------|
| Product Ads Search | POST /a/search | ✅ 200 | Çalışıyor |
| Product Ads Budget | GET /budget | ✅ 200 | Detaylı bütçe bilgisi |
| Store Ads Search | POST /a/search | ✅ 200 | `kinds: ["STORE_ADS"]` gerekli |
| Store Ads Count | GET /a/counts | ✅ 200 | Çalışıyor |
| Influencer Ads List | GET /v2/influencer-a | ✅ 200 | Pagination ile |
| Influencer Ads Count | GET /v2/influencer-a/count | ✅ 200 | Çalışıyor |
| Meta/TAA Ads | POST /taas/all | ✅ 200 | Bu satıcıda veri yok |

**Test Tarihi**: 24 Ocak 2026
**Test Edilen Hesap**: Seller ID 1080066
