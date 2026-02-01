# Buybox Takip Sistemi

## Genel Bakış

Buybox takip sistemi, satıcıların Trendyol'da ürünlerinin buybox durumunu izlemelerine, rakiplerini takip etmelerine ve fiyat değişikliklerinden haberdar olmalarına olanak tanır.

### Temel Özellikler
- **Ürün Takibi**: Mağaza başına 10 ürüne kadar takip
- **Otomatik Kontrol**: 12 saatte bir otomatik buybox durumu güncelleme
- **Manuel Kontrol**: İstenildiğinde anında buybox kontrolü
- **Rakip Analizi**: Tüm rakiplerin fiyat ve satıcı puanı bilgileri
- **Alert Sistemi**: Buybox kaybı/kazanımı, yeni rakip, fiyat riski bildirimleri
- **Tarihsel Veri**: Son 30 kontrolün geçmişi

---

## Mimari

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND                                    │
├─────────────────────────────────────────────────────────────────────────┤
│  Pages                    │  Components              │  Hooks            │
│  ├── /buybox (liste)      │  ├── BuyboxStatusCard    │  ├── useBuybox-  │
│  └── /buybox/[id] (detay) │  ├── BuyboxProductTable  │  │   Dashboard    │
│                           │  ├── BuyboxAddModal      │  ├── useBuybox-  │
│                           │  ├── BuyboxCompetitors   │  │   Products     │
│                           │  ├── BuyboxHistory       │  ├── useAddProduct│
│                           │  └── BuyboxAlertSettings │  └── useRemove... │
├─────────────────────────────────────────────────────────────────────────┤
│                         Next.js API Routes (BFF)                         │
│  /api/buybox/stores/[storeId]/dashboard                                  │
│  /api/buybox/stores/[storeId]/products                                   │
│  /api/buybox/stores/[storeId]/alerts                                     │
│  /api/buybox/products/[trackedProductId]                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              BACKEND                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  Controller               │  Service                 │  Scheduled        │
│  BuyboxController         │  BuyboxService           │  BuyboxScheduled- │
│  └── REST endpoints       │  ├── addProductToTrack   │  Service          │
│      (JWT protected)      │  ├── removeProduct       │  └── 12 saatte    │
│                           │  ├── getProductDetail    │      bir kontrol  │
│                           │  ├── checkBuyboxFor-     │                   │
│                           │  │   Product             │                   │
│                           │  └── createAlert         │                   │
├─────────────────────────────────────────────────────────────────────────┤
│                        TrendyolBuyboxClient                              │
│  └── Trendyol Product Detail API çağrıları                               │
│      URL: https://apigw.trendyol.com/discovery-web-productgw-service/    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            DATABASE                                      │
├─────────────────────────────────────────────────────────────────────────┤
│  buybox_tracked_products  │  buybox_snapshots        │  buybox_alerts    │
│  ├── id (UUID)            │  ├── id (UUID)           │  ├── id (UUID)    │
│  ├── store_id (FK)        │  ├── tracked_product_id  │  ├── store_id     │
│  ├── product_id (FK)      │  ├── checked_at          │  ├── tracked_-    │
│  ├── barcode              │  ├── buybox_status       │  │   product_id   │
│  ├── current_status       │  ├── winner_merchant_id  │  ├── alert_type   │
│  ├── is_active            │  ├── winner_price        │  ├── message      │
│  ├── alert_on_lost        │  ├── my_price            │  ├── is_read      │
│  └── alert_on_competitor  │  └── competitors (JSON)  │  └── created_at   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Veritabanı Şeması

### buybox_tracked_products
Takip edilen ürünlerin ana tablosu.

```sql
CREATE TABLE buybox_tracked_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES trendyol_products(id) ON DELETE CASCADE,
    barcode VARCHAR(100) NOT NULL,
    content_id BIGINT,                    -- Trendyol content ID

    -- Mevcut Durum
    current_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    winner_merchant_id VARCHAR(50),
    winner_merchant_name VARCHAR(255),
    winner_price DECIMAL(12,2),
    winner_seller_score DECIMAL(5,2),
    my_price DECIMAL(12,2),
    my_position INTEGER,
    total_sellers INTEGER,
    lowest_price DECIMAL(12,2),
    highest_price DECIMAL(12,2),

    -- Takip Ayarları
    is_active BOOLEAN NOT NULL DEFAULT true,
    alert_on_lost BOOLEAN NOT NULL DEFAULT true,
    alert_on_won BOOLEAN NOT NULL DEFAULT true,
    alert_on_new_competitor BOOLEAN NOT NULL DEFAULT false,
    price_alert_threshold DECIMAL(12,2),  -- Fiyat eşiği (altına düşerse alert)

    -- Zaman Damgaları
    last_checked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(store_id, product_id)
);
```

### buybox_snapshots
Her kontrol sonucunun tarihsel kaydı.

```sql
CREATE TABLE buybox_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracked_product_id UUID NOT NULL REFERENCES buybox_tracked_products(id) ON DELETE CASCADE,
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    buybox_status VARCHAR(30) NOT NULL,
    winner_merchant_id VARCHAR(50),
    winner_merchant_name VARCHAR(255),
    winner_price DECIMAL(12,2),
    winner_seller_score DECIMAL(5,2),

    my_price DECIMAL(12,2),
    my_position INTEGER,
    price_difference DECIMAL(12,2),
    total_sellers INTEGER,
    lowest_price DECIMAL(12,2),
    highest_price DECIMAL(12,2),

    competitors JSONB  -- Tüm rakiplerin anlık verisi
);
```

### buybox_alerts
Kullanıcıya gönderilen bildirimler.

```sql
CREATE TABLE buybox_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    tracked_product_id UUID NOT NULL REFERENCES buybox_tracked_products(id) ON DELETE CASCADE,

    alert_type VARCHAR(30) NOT NULL,  -- BUYBOX_LOST, BUYBOX_WON, NEW_COMPETITOR, PRICE_RISK
    message TEXT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    price_change DECIMAL(12,2),

    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### İndeksler
```sql
-- Performans indeksleri
CREATE INDEX idx_buybox_tracked_store ON buybox_tracked_products(store_id);
CREATE INDEX idx_buybox_tracked_active ON buybox_tracked_products(store_id, is_active);
CREATE INDEX idx_buybox_snapshots_product ON buybox_snapshots(tracked_product_id);
CREATE INDEX idx_buybox_snapshots_time ON buybox_snapshots(tracked_product_id, checked_at DESC);
CREATE INDEX idx_buybox_alerts_store ON buybox_alerts(store_id);
CREATE INDEX idx_buybox_alerts_unread ON buybox_alerts(store_id, is_read) WHERE is_read = false;
```

---

## Buybox Durumları

| Durum | Açıklama | Renk |
|-------|----------|------|
| `WON` | Buybox sizde | Yeşil (success) |
| `LOST` | Başka satıcı kazandı | Kırmızı (destructive) |
| `RISK` | Fiyatınız yüksek, risk altında | Sarı (warning) |
| `NO_COMPETITION` | Tek satıcı sizsiniz | Mavi (secondary) |
| `UNKNOWN` | Henüz kontrol edilmedi | Gri (outline) |

### Durum Hesaplama Mantığı (BuyboxService.java)

```java
private BuyboxStatus determineBuyboxStatus(BuyboxData data, String myMerchantId) {
    // Rakip yoksa: NO_COMPETITION
    if (data.getTotalSellers() <= 1) {
        return BuyboxStatus.NO_COMPETITION;
    }

    // Kazanan biz miyiz?
    if (myMerchantId.equals(data.getWinnerMerchantId())) {
        return BuyboxStatus.WON;
    }

    // Fiyatımız kazanandan ne kadar yüksek?
    BigDecimal priceDiff = data.getMyPrice().subtract(data.getWinnerPrice());
    BigDecimal percentDiff = priceDiff.divide(data.getWinnerPrice(), 4, RoundingMode.HALF_UP)
                                       .multiply(BigDecimal.valueOf(100));

    // %5'ten az fark varsa: RISK (çok yakınsınız)
    if (percentDiff.compareTo(BigDecimal.valueOf(5)) < 0) {
        return BuyboxStatus.RISK;
    }

    // %5'ten fazla fark: LOST
    return BuyboxStatus.LOST;
}
```

---

## Alert Türleri

| Tür | Tetikleyici | Varsayılan |
|-----|-------------|------------|
| `BUYBOX_LOST` | Durum WON → LOST/RISK | Aktif |
| `BUYBOX_WON` | Durum LOST/RISK → WON | Aktif |
| `NEW_COMPETITOR` | Yeni satıcı girişi | Pasif |
| `PRICE_RISK` | Fiyat eşiğinin altına düşüş | Pasif |

### Alert Oluşturma Mantığı

```java
private void createAlertsIfNeeded(BuyboxTrackedProduct tracked,
                                   BuyboxStatus oldStatus,
                                   BuyboxStatus newStatus,
                                   BuyboxData data) {

    // Buybox kaybı alertı
    if (tracked.isAlertOnLost() &&
        oldStatus == BuyboxStatus.WON &&
        (newStatus == BuyboxStatus.LOST || newStatus == BuyboxStatus.RISK)) {

        createAlert(tracked, BuyboxAlertType.BUYBOX_LOST,
            String.format("'%s' ürününde buybox kaybedildi. " +
                          "Kazanan: %s (%.2f TL)",
                          tracked.getProduct().getTitle(),
                          data.getWinnerMerchantName(),
                          data.getWinnerPrice()));
    }

    // Buybox kazanımı alertı
    if (tracked.isAlertOnWon() &&
        (oldStatus == BuyboxStatus.LOST || oldStatus == BuyboxStatus.RISK) &&
        newStatus == BuyboxStatus.WON) {

        createAlert(tracked, BuyboxAlertType.BUYBOX_WON,
            String.format("'%s' ürününde buybox kazanıldı!",
                          tracked.getProduct().getTitle()));
    }

    // Yeni rakip alertı
    if (tracked.isAlertOnNewCompetitor() &&
        data.getTotalSellers() > tracked.getTotalSellers()) {

        createAlert(tracked, BuyboxAlertType.NEW_COMPETITOR,
            String.format("'%s' ürününe yeni rakip girdi. " +
                          "Toplam satıcı: %d",
                          tracked.getProduct().getTitle(),
                          data.getTotalSellers()));
    }

    // Fiyat riski alertı
    if (tracked.getPriceAlertThreshold() != null &&
        data.getWinnerPrice().compareTo(tracked.getPriceAlertThreshold()) < 0) {

        createAlert(tracked, BuyboxAlertType.PRICE_RISK,
            String.format("'%s' ürününde rakip fiyatı belirlenen " +
                          "eşiğin (%.2f TL) altına düştü: %.2f TL",
                          tracked.getProduct().getTitle(),
                          tracked.getPriceAlertThreshold(),
                          data.getWinnerPrice()));
    }
}
```

---

## API Endpoints

### Backend REST API (BuyboxController.java)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/buybox/stores/{storeId}/dashboard` | Dashboard istatistikleri |
| GET | `/buybox/stores/{storeId}/products` | Takip edilen ürünler listesi |
| POST | `/buybox/stores/{storeId}/products` | Ürün takibe ekle |
| DELETE | `/buybox/stores/{storeId}/products/{trackedProductId}` | Ürünü takipten çıkar |
| GET | `/buybox/products/{trackedProductId}` | Ürün detayı (rakipler + geçmiş) |
| PUT | `/buybox/products/{trackedProductId}/settings` | Alert ayarlarını güncelle |
| POST | `/buybox/products/{trackedProductId}/check` | Manuel buybox kontrolü |
| GET | `/buybox/stores/{storeId}/alerts` | Okunmamış alertler |
| POST | `/buybox/stores/{storeId}/alerts/mark-read` | Alertleri okundu işaretle |

### Frontend API Routes

```
sellerx-frontend/app/api/buybox/
├── stores/
│   └── [storeId]/
│       ├── dashboard/route.ts    → GET /buybox/stores/{storeId}/dashboard
│       ├── products/route.ts     → GET, POST /buybox/stores/{storeId}/products
│       │   └── [trackedProductId]/route.ts → DELETE
│       └── alerts/
│           ├── route.ts          → GET /buybox/stores/{storeId}/alerts
│           └── mark-read/route.ts → POST
└── products/
    └── [trackedProductId]/
        ├── route.ts              → GET /buybox/products/{trackedProductId}
        ├── settings/route.ts     → PUT
        └── check/route.ts        → POST
```

---

## Trendyol API Entegrasyonu

### TrendyolBuyboxClient.java

Buybox verileri Trendyol'un **Product Detail API**'sinden alınır. Bu API, ürün sayfasındaki tüm satıcı bilgilerini döndürür.

```java
public class TrendyolBuyboxClient {

    // API URL (contentId = Trendyol ürün ID'si)
    private static final String PRODUCT_DETAIL_URL =
        "https://apigw.trendyol.com/discovery-web-productgw-service/api/productDetail/{contentId}";

    public BuyboxData fetchBuyboxData(Long contentId, String myMerchantId) {
        // 1. API'yi çağır
        String url = PRODUCT_DETAIL_URL.replace("{contentId}", contentId.toString());
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        // 2. Response'u parse et
        JsonNode root = response.getBody();
        JsonNode result = root.path("result");

        // 3. merchantListings içinden tüm satıcıları al
        JsonNode merchantListings = result.path("merchantListings");

        // 4. Her satıcının bilgilerini çıkar
        List<MerchantInfo> merchants = new ArrayList<>();
        for (JsonNode merchant : merchantListings) {
            MerchantInfo info = MerchantInfo.builder()
                .merchantId(merchant.path("merchantId").asText())
                .merchantName(merchant.path("merchantName").asText())
                .price(merchant.path("price").path("sellingPrice").decimalValue())
                .discountedPrice(merchant.path("price").path("discountedPrice").decimalValue())
                .sellerScore(merchant.path("sellerScore").decimalValue())
                .hasStock(merchant.path("hasStock").asBoolean())
                .isWinner(merchant.path("isWinner").asBoolean())
                .deliveryDate(merchant.path("deliveryDate").asText())
                .build();
            merchants.add(info);
        }

        // 5. Kazananı ve benim bilgilerimi bul
        MerchantInfo winner = merchants.stream()
            .filter(MerchantInfo::isWinner)
            .findFirst()
            .orElse(null);

        MerchantInfo me = merchants.stream()
            .filter(m -> m.getMerchantId().equals(myMerchantId))
            .findFirst()
            .orElse(null);

        // 6. BuyboxData oluştur
        return BuyboxData.builder()
            .contentId(contentId)
            .totalSellers(merchants.size())
            .winnerMerchantId(winner != null ? winner.getMerchantId() : null)
            .winnerMerchantName(winner != null ? winner.getMerchantName() : null)
            .winnerPrice(winner != null ? winner.getDiscountedPrice() : null)
            .winnerSellerScore(winner != null ? winner.getSellerScore() : null)
            .myPrice(me != null ? me.getDiscountedPrice() : null)
            .myPosition(calculatePosition(merchants, myMerchantId))
            .lowestPrice(calculateLowestPrice(merchants))
            .highestPrice(calculateHighestPrice(merchants))
            .competitors(merchants)
            .build();
    }
}
```

### Rate Limiting

Trendyol API çağrıları `TrendyolRateLimiter` üzerinden geçer:
- **Limit**: 10 istek/saniye
- **Scheduled Job**: API çağrıları arasında 100ms bekleme

---

## Scheduled Job

### BuyboxScheduledService.java

12 saatte bir tüm aktif ürünlerin buybox durumunu kontrol eder.

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class BuyboxScheduledService {

    private final BuyboxTrackedProductRepository trackedRepository;
    private final BuyboxService buyboxService;

    // Her 12 saatte bir çalışır (00:00 ve 12:00)
    @Scheduled(cron = "0 0 */12 * * ?", zone = "Europe/Istanbul")
    public void checkAllBuyboxProducts() {
        log.info("Starting scheduled buybox check...");

        List<BuyboxTrackedProduct> activeProducts =
            trackedRepository.findByIsActiveTrue();

        int successCount = 0;
        int errorCount = 0;

        for (BuyboxTrackedProduct tracked : activeProducts) {
            try {
                buyboxService.checkBuyboxForProduct(tracked);
                successCount++;

                // Rate limiting: 100ms bekleme
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Failed to check buybox for product {}: {}",
                    tracked.getId(), e.getMessage());
                errorCount++;
            }
        }

        log.info("Scheduled buybox check completed. " +
                 "Success: {}, Errors: {}", successCount, errorCount);
    }
}
```

---

## Frontend Yapısı

### Sayfalar

#### /buybox (Ana Liste Sayfası)
```
┌─────────────────────────────────────────────────────────────┐
│  📊 Dashboard İstatistikleri                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ Toplam   │ │ Kazanılan│ │ Kaybedilen│ │ Risk     │       │
│  │ Takip: 5 │ │ WON: 2   │ │ LOST: 1   │ │ RISK: 1  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
├─────────────────────────────────────────────────────────────┤
│  🔔 Alert Banner (okunmamış alert varsa)                    │
│  "3 okunmamış bildirim var" [Tümünü Gör]                    │
├─────────────────────────────────────────────────────────────┤
│  📋 Takip Edilen Ürünler                    [+ Ürün Ekle]   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Görsel │ Ürün Adı        │ Durum │ Fiyat │ Sıra │ ⚙️ │  │
│  │ [img]  │ iPhone 15 Case  │ 🟢WON │ 149₺  │ 1/5  │ ➡️ │  │
│  │ [img]  │ Samsung Kılıf   │ 🔴LOST│ 89₺   │ 3/8  │ ➡️ │  │
│  │ [img]  │ Tablet Stand    │ 🟡RISK│ 199₺  │ 2/4  │ ➡️ │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### /buybox/[id] (Ürün Detay Sayfası)
```
┌─────────────────────────────────────────────────────────────┐
│  ← [Geri]           iPhone 15 Case             [🔄][🗑️]    │
│  [img]  Barcode: 12345678  🟢 WON                          │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │Sizin     │ │Kazanan   │ │Sıranız   │ │Fiyat     │       │
│  │Fiyatınız │ │Fiyat     │ │          │ │Farkı     │       │
│  │149,00 ₺  │ │149,00 ₺  │ │1/5       │ │0,00 ₺    │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
├─────────────────────────────────────────────────────────────┤
│  [Rakipler (5)] [Geçmiş (30)] [Ayarlar]                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Satıcı        │ Fiyat  │ Puan │ Stok │ Kazanan │     │  │
│  │ Siz (MağazaX) │ 149 ₺  │ 9.2  │ ✓    │ 👑      │     │  │
│  │ RakipA        │ 155 ₺  │ 8.8  │ ✓    │         │     │  │
│  │ RakipB        │ 159 ₺  │ 9.0  │ ✓    │         │     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### React Query Hooks (use-buybox.ts)

```typescript
// Query Key Factory
const buyboxKeys = {
  all: ["buybox"] as const,
  dashboard: (storeId: string) => [...buyboxKeys.all, "dashboard", storeId],
  products: (storeId: string) => [...buyboxKeys.all, "products", storeId],
  productDetail: (id: string) => [...buyboxKeys.all, "detail", id],
  alerts: (storeId: string) => [...buyboxKeys.all, "alerts", storeId],
};

// Dashboard verisi
export function useBuyboxDashboard(storeId: string | undefined) {
  return useQuery({
    queryKey: buyboxKeys.dashboard(storeId!),
    queryFn: () => fetchBuyboxDashboard(storeId!),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000,  // 5 dakika cache
  });
}

// Ürün ekleme mutation
export function useAddProductToTrack(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) => addProductToTrack(storeId!, productId),
    onSuccess: () => {
      // Dashboard ve ürün listesini yenile
      queryClient.invalidateQueries({ queryKey: buyboxKeys.dashboard(storeId!) });
      queryClient.invalidateQueries({ queryKey: buyboxKeys.products(storeId!) });
    },
  });
}

// Ürün silme mutation
export function useRemoveProductFromTrack(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (trackedProductId: string) =>
      removeProductFromTrack(storeId!, trackedProductId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: buyboxKeys.dashboard(storeId!) });
      queryClient.invalidateQueries({ queryKey: buyboxKeys.products(storeId!) });
    },
  });
}
```

### TypeScript Types (types/buybox.ts)

```typescript
// Buybox durumları
export type BuyboxStatus = "WON" | "LOST" | "RISK" | "NO_COMPETITION" | "UNKNOWN";

// Alert türleri
export type BuyboxAlertType = "BUYBOX_LOST" | "BUYBOX_WON" | "NEW_COMPETITOR" | "PRICE_RISK";

// Rakip bilgisi
export interface MerchantInfo {
  merchantId: string;
  merchantName: string;
  price: number;
  discountedPrice: number;
  sellerScore: number;
  hasStock: boolean;
  isWinner: boolean;
  deliveryDate?: string;
}

// Takip edilen ürün
export interface BuyboxTrackedProduct {
  id: string;
  productId: string;
  barcode: string;
  productName: string;
  imageUrl?: string;
  currentStatus: BuyboxStatus;
  winnerMerchantId?: string;
  winnerMerchantName?: string;
  winnerPrice?: number;
  myPrice?: number;
  myPosition?: number;
  totalSellers?: number;
  priceDifference?: number;
  isActive: boolean;
  alertOnLost: boolean;
  alertOnWon: boolean;
  alertOnNewCompetitor: boolean;
  priceAlertThreshold?: number;
  lastCheckedAt?: string;
}

// Dashboard istatistikleri
export interface BuyboxDashboard {
  totalTracked: number;
  wonCount: number;
  lostCount: number;
  riskCount: number;
  noCompetitionCount: number;
  unknownCount: number;
  unreadAlertCount: number;
}

// Ürün detayı (rakipler + geçmiş dahil)
export interface BuyboxProductDetail extends BuyboxTrackedProduct {
  storeId: string;
  productUrl?: string;
  winnerSellerScore?: number;
  lowestPrice?: number;
  highestPrice?: number;
  competitors: MerchantInfo[];
  history: BuyboxSnapshot[];
  myMerchantId: string;
}
```

---

## Kısıtlamalar ve Limitler

| Kısıt | Değer | Açıklama |
|-------|-------|----------|
| Maksimum Takip Edilen Ürün | 10 / mağaza | `MAX_TRACKED_PRODUCTS = 10` |
| Snapshot Geçmişi | 30 kayıt | `HISTORY_LIMIT = 30` |
| Otomatik Kontrol Sıklığı | 12 saat | Cron: `0 0 */12 * * ?` |
| API Rate Limit | 10 req/sec | TrendyolRateLimiter |
| Scheduled Job Delay | 100ms | API çağrıları arası bekleme |

---

## Hata Yönetimi

### Backend Exceptions

```java
// Ürün bulunamadı
throw new EntityNotFoundException("Tracked product not found: " + trackedProductId);

// Kullanıcı yetkisi yok
throw new AccessDeniedException("User does not have access to this store");

// Limit aşıldı
throw new BusinessException("Maximum tracked products limit (10) reached for this store");

// Ürün zaten takipte
throw new BusinessException("Product is already being tracked");
```

### Frontend Error Handling

```typescript
const addProduct = useAddProductToTrack(storeId);

const handleAdd = async (productId: string) => {
  try {
    await addProduct.mutateAsync(productId);
    toast.success("Ürün takibe eklendi");
  } catch (error: any) {
    if (error.message?.includes("limit")) {
      toast.error("Maksimum 10 ürün takip edebilirsiniz");
    } else if (error.message?.includes("already")) {
      toast.error("Bu ürün zaten takipte");
    } else {
      toast.error("Ürün eklenirken hata oluştu");
    }
  }
};
```

---

## Geliştirme Notları

### Yeni Özellik Eklerken

1. **Backend**: `BuyboxService.java`'ya iş mantığını ekle
2. **Controller**: `BuyboxController.java`'ya endpoint ekle
3. **Frontend API Route**: `app/api/buybox/...` altına route ekle
4. **React Query Hook**: `use-buybox.ts`'ye hook ekle
5. **Component**: İlgili component'ı güncelle

### Test Senaryoları

- [ ] Ürün ekleme (normal durum)
- [ ] Ürün ekleme (limit aşımı - 10 ürün)
- [ ] Ürün ekleme (zaten takipte)
- [ ] Ürün silme (liste sayfasından)
- [ ] Ürün silme (detay sayfasından)
- [ ] Manuel buybox kontrolü
- [ ] Alert oluşturma (buybox kaybı)
- [ ] Alert oluşturma (buybox kazanımı)
- [ ] Alert ayarları güncelleme
- [ ] Scheduled job çalışması

### Bilinen Sorunlar ve Çözümler

| Sorun | Çözüm |
|-------|-------|
| Görsel görünmüyor | `product.image` kullan (`imageUrl` değil) |
| "Takipten Çıkar" çalışmıyor | `mutateAsync(trackedProductId)` parametre geçir |
| 500 hatası (Backend) | Authentication pattern'ı `getAuthenticatedUser()` kullan |

---

## İlgili Dosyalar

### Backend
```
sellerx-backend/src/main/java/com/ecommerce/sellerx/buybox/
├── BuyboxController.java           # REST endpoints
├── BuyboxService.java              # İş mantığı (608 satır)
├── BuyboxScheduledService.java     # Scheduled job
├── TrendyolBuyboxClient.java       # Trendyol API client
├── BuyboxTrackedProduct.java       # Entity
├── BuyboxSnapshot.java             # Entity
├── BuyboxAlert.java                # Entity
├── BuyboxTrackedProductRepository.java
├── BuyboxSnapshotRepository.java
├── BuyboxAlertRepository.java
└── dto/
    ├── BuyboxDashboardDto.java
    ├── BuyboxTrackedProductDto.java
    ├── BuyboxProductDetailDto.java
    ├── BuyboxSnapshotDto.java
    ├── BuyboxAlertDto.java
    ├── AddProductRequest.java
    └── UpdateAlertSettingsRequest.java
```

### Frontend
```
sellerx-frontend/
├── app/[locale]/(app-shell)/buybox/
│   ├── page.tsx                    # Ana liste sayfası
│   └── [id]/page.tsx               # Detay sayfası
├── app/api/buybox/                 # API routes
├── components/buybox/
│   ├── index.ts                    # Barrel export
│   ├── BuyboxStatusCards.tsx       # Dashboard kartları
│   ├── BuyboxProductTable.tsx      # Ürün tablosu
│   ├── BuyboxAddProductModal.tsx   # Ürün ekleme modal
│   ├── BuyboxCompetitorsTable.tsx  # Rakip tablosu
│   ├── BuyboxHistoryTable.tsx      # Geçmiş tablosu
│   └── BuyboxAlertSettings.tsx     # Alert ayarları formu
├── hooks/queries/use-buybox.ts     # React Query hooks
└── types/buybox.ts                 # TypeScript types
```

### Database
```
sellerx-backend/src/main/resources/db/migration/
└── V72__create_buybox_tables.sql   # Tablo ve indeks tanımları
```
