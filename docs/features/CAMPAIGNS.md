# Trendyol Kampanya Takip Sistemi

Bu dokümantasyon SellerX platformundaki reklam kampanyası takip sisteminin teknik detaylarını açıklar.

## Genel Bakış

Kampanya takip sistemi, Trendyol reklam kampanyalarının manuel Excel import yöntemiyle takibini sağlar. Mevcut AdReport (günlük reklam raporu) sisteminin yanına kampanya bazlı detaylı analiz ekler.

> **Not:** Trendyol'un public bir Reklam/Kampanya API'si bulunmamaktadır. Reklam yönetimi sadece Seller Panel UI üzerinden yapılır. Bu nedenle veriler Excel import ile sisteme aktarılır.

```
┌─────────────────┐     Excel      ┌─────────────────┐     Analiz      ┌─────────────────┐
│  Trendyol       │ ────────────▶  │     SellerX     │ ─────────────▶  │    Dashboard    │
│  Seller Panel   │    Export      │     Backend     │    Hesaplama    │    Kampanyalar  │
└─────────────────┘                └─────────────────┘                 └─────────────────┘
```

## Mimari

### Backend Yapısı

```
sellerx-backend/src/main/java/com/ecommerce/sellerx/ads/
├── TrendyolCampaign.java              # Kampanya entity (JPA)
├── TrendyolCampaignRepository.java    # Repository interface
├── CampaignDailyStat.java             # Günlük istatistik entity
├── CampaignDailyStatRepository.java   # Günlük istatistik repository
├── TrendyolCampaignService.java       # İş mantığı servisi
├── TrendyolCampaignController.java    # REST API controller
└── dto/
    ├── CampaignDto.java               # Kampanya DTO
    ├── CampaignSummaryDto.java        # Özet istatistik DTO
    └── CampaignImportRequest.java     # Import request DTO
```

### Frontend Yapısı

```
sellerx-frontend/
├── app/[locale]/(app-shell)/ads/page.tsx       # Tab yapısı (Özet | Kampanyalar)
├── app/api/ads/stores/[storeId]/campaigns/
│   ├── route.ts                                 # GET list, POST create
│   ├── [campaignId]/route.ts                    # GET, PUT, DELETE single
│   ├── [campaignId]/stats/route.ts              # GET daily stats
│   ├── import/route.ts                          # POST Excel import
│   ├── summary/route.ts                         # GET summary stats
│   ├── active/route.ts                          # GET active campaigns
│   ├── daily-stats/route.ts                     # GET all daily stats
│   └── recalculate/route.ts                     # POST recalculate metrics
├── components/ads/
│   ├── campaign-list.tsx                        # Kampanya listesi
│   ├── campaign-stats-cards.tsx                 # Özet kartları
│   ├── campaign-detail-panel.tsx                # Detay paneli
│   └── campaign-import-modal.tsx                # Excel import modal
├── hooks/queries/use-campaigns.ts               # React Query hooks
└── types/campaign.ts                            # TypeScript tipleri
```

## Veritabanı Şeması

### trendyol_campaigns Tablosu (V61)

```sql
CREATE TYPE campaign_type AS ENUM ('PRODUCT', 'STORE', 'INFLUENCER', 'MICRO_INFLUENCER', 'OTHER');
CREATE TYPE campaign_status AS ENUM ('ACTIVE', 'PAUSED', 'COMPLETED', 'DRAFT', 'ARCHIVED');

CREATE TABLE trendyol_campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    campaign_id VARCHAR(100),                    -- Trendyol kampanya ID (opsiyonel)
    campaign_name VARCHAR(500) NOT NULL,
    campaign_type campaign_type NOT NULL DEFAULT 'PRODUCT',
    status campaign_status NOT NULL DEFAULT 'ACTIVE',

    -- Bütçe Bilgileri
    daily_budget DECIMAL(15,2),
    total_budget DECIMAL(15,2),
    spent_amount DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Tarih Bilgileri
    start_date DATE,
    end_date DATE,

    -- Toplam Metrikler (hesaplanan)
    total_impressions BIGINT DEFAULT 0,
    total_clicks BIGINT DEFAULT 0,
    total_orders INTEGER DEFAULT 0,
    total_revenue DECIMAL(15,2) DEFAULT 0,
    total_spend DECIMAL(15,2) DEFAULT 0,

    -- Ortalama Metrikler (hesaplanan)
    avg_ctr DECIMAL(8,4),                        -- Click-Through Rate (%)
    avg_acos DECIMAL(8,4),                       -- Advertising Cost of Sales (%)
    avg_roas DECIMAL(8,4),                       -- Return on Ad Spend
    avg_cpc DECIMAL(10,4),                       -- Cost Per Click
    avg_cpo DECIMAL(10,4),                       -- Cost Per Order

    -- Meta Bilgiler
    raw_data JSONB,                              -- Orijinal import verisi
    import_source VARCHAR(50) DEFAULT 'EXCEL',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_campaigns_store ON trendyol_campaigns(store_id);
CREATE INDEX idx_campaigns_status ON trendyol_campaigns(status);
CREATE INDEX idx_campaigns_dates ON trendyol_campaigns(start_date, end_date);
CREATE UNIQUE INDEX idx_campaigns_store_name ON trendyol_campaigns(store_id, campaign_name);
```

### campaign_daily_stats Tablosu (V62)

```sql
CREATE TABLE campaign_daily_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES trendyol_campaigns(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,

    -- Günlük Metrikler
    impressions BIGINT DEFAULT 0,
    clicks BIGINT DEFAULT 0,
    orders INTEGER DEFAULT 0,
    revenue DECIMAL(15,2) DEFAULT 0,
    spend DECIMAL(15,2) DEFAULT 0,

    -- Hesaplanan Metrikler
    ctr DECIMAL(8,4),                            -- (clicks / impressions) * 100
    acos DECIMAL(8,4),                           -- (spend / revenue) * 100
    roas DECIMAL(8,4),                           -- revenue / spend
    cpc DECIMAL(10,4),                           -- spend / clicks
    cpo DECIMAL(10,4),                           -- spend / orders

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(campaign_id, stat_date)
);

-- Indexes
CREATE INDEX idx_daily_stats_campaign ON campaign_daily_stats(campaign_id);
CREATE INDEX idx_daily_stats_store_date ON campaign_daily_stats(store_id, stat_date);
```

### AdReports Tablosu Güncellemesi (V63)

```sql
ALTER TABLE ad_reports ADD COLUMN campaign_id UUID REFERENCES trendyol_campaigns(id);
CREATE INDEX idx_ad_reports_campaign ON ad_reports(campaign_id);
```

## API Endpoints

### Kampanya Yönetimi

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/api/ads/stores/{storeId}/campaigns` | Kampanya listesi |
| GET | `/api/ads/stores/{storeId}/campaigns/{id}` | Tek kampanya detayı |
| POST | `/api/ads/stores/{storeId}/campaigns` | Yeni kampanya oluştur |
| PUT | `/api/ads/stores/{storeId}/campaigns/{id}` | Kampanya güncelle |
| DELETE | `/api/ads/stores/{storeId}/campaigns/{id}` | Kampanya sil |

### Özet ve İstatistikler

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/api/ads/stores/{storeId}/campaigns/summary` | Özet istatistikler |
| GET | `/api/ads/stores/{storeId}/campaigns/active` | Aktif kampanyalar |
| GET | `/api/ads/stores/{storeId}/campaigns/{id}/stats` | Kampanya günlük istatistikleri |
| GET | `/api/ads/stores/{storeId}/campaigns/daily-stats` | Tüm günlük istatistikler |

### Import ve Hesaplama

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| POST | `/api/ads/stores/{storeId}/campaigns/import` | Excel import |
| POST | `/api/ads/stores/{storeId}/campaigns/recalculate` | Metrikleri yeniden hesapla |

### Response Örnekleri

#### Kampanya Listesi Response

```json
[
  {
    "id": "uuid",
    "storeId": "uuid",
    "campaignId": "trendyol-campaign-id",
    "campaignName": "Ürün Reklamları - Ocak 2026",
    "campaignType": "PRODUCT",
    "status": "ACTIVE",
    "dailyBudget": 500.00,
    "totalBudget": 15000.00,
    "spentAmount": 8750.00,
    "startDate": "2026-01-01",
    "endDate": "2026-01-31",
    "totalImpressions": 125000,
    "totalClicks": 3750,
    "totalOrders": 187,
    "totalRevenue": 28500.00,
    "totalSpend": 8750.00,
    "avgCtr": 3.00,
    "avgAcos": 30.70,
    "avgRoas": 3.26,
    "avgCpc": 2.33,
    "avgCpo": 46.79,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-23T12:00:00"
  }
]
```

#### Summary Response

```json
{
  "totalCampaigns": 5,
  "activeCampaigns": 3,
  "totalSpend": 25000.00,
  "totalRevenue": 75000.00,
  "totalClicks": 12500,
  "totalImpressions": 450000,
  "totalOrders": 625,
  "avgCtr": 2.78,
  "avgAcos": 33.33,
  "avgRoas": 3.00
}
```

#### Import Request

```json
{
  "campaigns": [
    {
      "campaignId": "optional-id",
      "campaignName": "Kampanya Adı",
      "campaignType": "PRODUCT",
      "status": "ACTIVE",
      "dailyBudget": 500,
      "totalBudget": 15000,
      "spentAmount": 0,
      "startDate": "2026-01-01",
      "endDate": "2026-01-31"
    }
  ],
  "dailyStats": [
    {
      "campaignName": "Kampanya Adı",
      "statDate": "2026-01-15",
      "impressions": 5000,
      "clicks": 150,
      "orders": 8,
      "revenue": 1200.00,
      "spend": 350.00
    }
  ]
}
```

## Excel Import Formatı

### Desteklenen Sayfa İsimleri

| Türkçe | İngilizce | İçerik |
|--------|-----------|--------|
| Kampanyalar | Campaigns | Kampanya bilgileri |
| Günlük İstatistik | Daily Stats | Günlük performans verileri |

### Kampanyalar Sayfası Kolonları

| Türkçe Kolon | İngilizce Kolon | Alan Adı | Zorunlu |
|--------------|-----------------|----------|---------|
| Kampanya ID | Campaign ID | campaignId | Hayır |
| Kampanya Adı | Campaign Name | campaignName | Evet |
| Tür | Type | campaignType | Hayır (default: PRODUCT) |
| Durum | Status | status | Hayır (default: ACTIVE) |
| Günlük Bütçe | Daily Budget | dailyBudget | Hayır |
| Toplam Bütçe | Total Budget | totalBudget | Hayır |
| Harcanan | Spent | spentAmount | Hayır |
| Başlangıç | Start Date | startDate | Hayır |
| Bitiş | End Date | endDate | Hayır |

### Günlük İstatistik Sayfası Kolonları

| Türkçe Kolon | İngilizce Kolon | Alan Adı | Zorunlu |
|--------------|-----------------|----------|---------|
| Kampanya Adı | Campaign Name | campaignName | Evet |
| Tarih | Date | statDate | Evet |
| Gösterim | Impressions | impressions | Hayır |
| Tıklama | Clicks | clicks | Hayır |
| Sipariş | Orders | orders | Hayır |
| Gelir | Revenue | revenue | Hayır |
| Harcama | Spend | spend | Hayır |

### Kampanya Türleri

| Değer | Açıklama |
|-------|----------|
| PRODUCT | Ürün Reklamları |
| STORE | Mağaza Reklamları |
| INFLUENCER | Influencer Reklamları |
| MICRO_INFLUENCER | Mikro Influencer Reklamları |
| OTHER | Diğer |

### Kampanya Durumları

| Değer | Açıklama |
|-------|----------|
| ACTIVE | Aktif |
| PAUSED | Duraklatıldı |
| COMPLETED | Tamamlandı |
| DRAFT | Taslak |
| ARCHIVED | Arşivlendi |

## Frontend Kullanımı

### React Query Hooks

```typescript
import {
  useCampaigns,
  useCampaign,
  useCampaignSummary,
  useCampaignStats,
  useImportCampaigns,
  useCreateCampaign,
  useUpdateCampaign,
  useDeleteCampaign,
  useRecalculateCampaignMetrics,
} from "@/hooks/queries/use-campaigns";

// Kampanya listesi
const { data: campaigns, isLoading } = useCampaigns(storeId);

// Özet istatistikler
const { data: summary } = useCampaignSummary(storeId);

// Tek kampanya detayı
const { data: campaign } = useCampaign(storeId, campaignId);

// Kampanya günlük istatistikleri
const { data: stats } = useCampaignStats(storeId, campaignId, startDate, endDate);

// Excel import
const importMutation = useImportCampaigns();
await importMutation.mutateAsync({ storeId, data: importRequest });

// Yeni kampanya
const createMutation = useCreateCampaign();
await createMutation.mutateAsync({ storeId, campaign: newCampaign });

// Güncelleme
const updateMutation = useUpdateCampaign();
await updateMutation.mutateAsync({ storeId, campaignId, campaign: updatedCampaign });

// Silme
const deleteMutation = useDeleteCampaign();
await deleteMutation.mutateAsync({ storeId, campaignId });

// Metrikleri yeniden hesapla
const recalculateMutation = useRecalculateCampaignMetrics();
await recalculateMutation.mutateAsync(storeId);
```

### Component Kullanımı

```tsx
import { CampaignList } from "@/components/ads/campaign-list";
import { CampaignStatsCards } from "@/components/ads/campaign-stats-cards";
import { CampaignDetailPanel } from "@/components/ads/campaign-detail-panel";
import { CampaignImportModal } from "@/components/ads/campaign-import-modal";

// Kampanya listesi
<CampaignList
  campaigns={campaigns}
  isLoading={isLoading}
  onSelectCampaign={(campaign) => setSelectedCampaign(campaign)}
  selectedCampaignId={selectedCampaign?.id}
/>

// Özet kartları
<CampaignStatsCards
  summary={summary}
  isLoading={summaryLoading}
/>

// Detay paneli
{selectedCampaign && (
  <CampaignDetailPanel
    campaign={selectedCampaign}
    storeId={storeId}
    onClose={() => setSelectedCampaign(null)}
  />
)}

// Import modal
<CampaignImportModal
  storeId={storeId}
  isOpen={showImportModal}
  onClose={() => setShowImportModal(false)}
/>
```

## Metrik Hesaplamaları

### CTR (Click-Through Rate)
```
CTR = (Tıklama / Gösterim) × 100
```
Yüksek CTR, reklamın kullanıcılar tarafından ilgi çekici bulunduğunu gösterir.

### ACOS (Advertising Cost of Sales)
```
ACOS = (Reklam Harcaması / Reklam Satışı) × 100
```
Düşük ACOS daha iyi performansı gösterir. Genellikle %30 altı iyi kabul edilir.

### ROAS (Return on Ad Spend)
```
ROAS = Reklam Satışı / Reklam Harcaması
```
Yüksek ROAS daha iyi getiriyi gösterir. ROAS > 1 karlılık anlamına gelir.

### CPC (Cost Per Click)
```
CPC = Reklam Harcaması / Tıklama Sayısı
```
Tıklama başına maliyet. Düşük CPC tercih edilir.

### CPO (Cost Per Order)
```
CPO = Reklam Harcaması / Sipariş Sayısı
```
Sipariş başına reklam maliyeti.

## i18n Çevirileri

Çeviriler `messages/tr.json` ve `messages/en.json` dosyalarında `Ads.tabs` ve `Ads.campaigns` namespace'leri altında bulunur.

### Türkçe Örnek
```json
{
  "Ads": {
    "tabs": {
      "summary": "Özet",
      "campaigns": "Kampanyalar"
    },
    "campaigns": {
      "title": "Kampanya Yönetimi",
      "noCampaigns": "Henüz kampanya yok",
      "status": {
        "active": "Aktif",
        "paused": "Duraklatıldı"
      }
    }
  }
}
```

## Troubleshooting

### Import çalışmıyor

1. Excel dosyasının `.xlsx` veya `.xls` formatında olduğunu kontrol edin
2. Sayfa isimlerinin "Kampanyalar" veya "Campaigns" olduğunu doğrulayın
3. Zorunlu alanların (campaignName, statDate) dolu olduğunu kontrol edin
4. Tarih formatının doğru olduğundan emin olun (YYYY-MM-DD veya Excel tarih formatı)

### Metrikler yanlış görünüyor

1. `/api/ads/stores/{storeId}/campaigns/recalculate` endpoint'ini çağırın
2. Günlük istatistik verilerinin doğru kampanyaya bağlı olduğunu kontrol edin
3. Spend ve revenue değerlerinin 0'dan büyük olduğunu doğrulayın

### Kampanya görünmüyor

1. Store ID'nin doğru olduğundan emin olun
2. Kampanya status'unun ARCHIVED olmadığını kontrol edin
3. Backend loglarında hata olup olmadığını kontrol edin

## Gelecek Geliştirmeler

Trendyol public bir Reklam API'si yayınlarsa:

1. `TrendyolAdsApiService.java` implementasyonu eklenecek
2. Scheduled job ile otomatik senkronizasyon yapılacak
3. Reklam bakiyesi takibi eklenecek
4. Gerçek zamanlı performans güncellemeleri

## İlgili Dosyalar

- **Backend Entity**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/ads/TrendyolCampaign.java`
- **Backend Service**: `sellerx-backend/src/main/java/com/ecommerce/sellerx/ads/TrendyolCampaignService.java`
- **Frontend Page**: `sellerx-frontend/app/[locale]/(app-shell)/ads/page.tsx`
- **Frontend Hooks**: `sellerx-frontend/hooks/queries/use-campaigns.ts`
- **DB Migration**: `sellerx-backend/src/main/resources/db/migration/V61__create_trendyol_campaigns_table.sql`
