/**
 * Buybox Takip Sistemi TypeScript tipleri
 */

export type BuyboxStatus = "WON" | "LOST" | "RISK" | "NO_COMPETITION";

export type BuyboxAlertType =
  | "BUYBOX_LOST"
  | "BUYBOX_WON"
  | "NEW_COMPETITOR"
  | "PRICE_RISK";

export interface MerchantInfo {
  merchantId: number;
  merchantName: string;
  price: number;
  sellerScore: number;
  isWinner: boolean;
  hasStock: boolean;
  isFreeCargo: boolean;
  deliveryDate?: string;
}

export interface BuyboxTrackedProduct {
  id: string;
  storeId: string;
  productId: string;

  // Ürün bilgileri
  productTitle: string;
  productBarcode?: string;
  productImageUrl?: string;
  trendyolProductId: string;

  // Takip ayarları
  isActive: boolean;
  alertOnLoss: boolean;
  alertOnNewCompetitor: boolean;
  alertPriceThreshold: number;

  // Son buybox durumu
  lastStatus?: BuyboxStatus;
  lastWinnerPrice?: number;
  lastWinnerName?: string;
  myPrice?: number;
  priceDifference?: number;
  myPosition?: number;
  totalSellers?: number;
  lastCheckedAt?: string;

  createdAt: string;
  updatedAt: string;
}

export interface BuyboxSnapshot {
  id: string;
  checkedAt: string;
  buyboxStatus: BuyboxStatus;

  winnerMerchantId?: number;
  winnerMerchantName?: string;
  winnerPrice?: number;
  winnerSellerScore?: number;

  myPrice?: number;
  myPosition?: number;
  priceDifference?: number;

  totalSellers?: number;
  lowestPrice?: number;
  highestPrice?: number;
}

export interface BuyboxAlert {
  id: string;
  storeId: string;
  trackedProductId: string;

  alertType: BuyboxAlertType;
  title: string;
  message: string;

  // Ürün bilgileri
  productTitle?: string;
  productImageUrl?: string;

  oldWinnerName?: string;
  newWinnerName?: string;
  priceBefore?: number;
  priceAfter?: number;

  isRead: boolean;
  createdAt: string;
  readAt?: string;
}

export interface BuyboxProductDetail {
  id: string;
  storeId: string;
  productId: string;

  // Ürün bilgileri
  productTitle: string;
  productBarcode?: string;
  productImageUrl?: string;
  trendyolProductId: string;
  trendyolUrl?: string;

  // Takip ayarları
  isActive: boolean;
  alertOnLoss: boolean;
  alertOnNewCompetitor: boolean;
  alertPriceThreshold: number;

  // Mevcut buybox durumu
  currentStatus?: BuyboxStatus;
  winnerPrice?: number;
  winnerName?: string;
  winnerMerchantId?: number;
  winnerSellerScore?: number;
  myPrice?: number;
  myPosition?: number;
  priceDifference?: number;
  totalSellers?: number;
  lowestPrice?: number;
  highestPrice?: number;
  lastCheckedAt?: string;

  // Rakipler
  competitors?: MerchantInfo[];

  // Geçmiş
  history?: BuyboxSnapshot[];

  // Alertler
  recentAlerts?: BuyboxAlert[];

  createdAt: string;
}

export interface BuyboxDashboard {
  storeId: string;

  // Özet istatistikler
  totalTrackedProducts: number;
  wonCount: number;
  lostCount: number;
  riskCount: number;
  noCompetitionCount: number;

  // Okunmamış alert sayısı
  unreadAlertCount: number;

  // Takip edilen ürünler listesi (özet)
  products: BuyboxTrackedProduct[];

  // Son alertler
  recentAlerts: BuyboxAlert[];

  lastUpdatedAt: string;
}

// Request tipleri
export interface AddProductRequest {
  productId: string;
}

export interface UpdateAlertSettingsRequest {
  alertOnLoss?: boolean;
  alertOnNewCompetitor?: boolean;
  alertPriceThreshold?: number;
  isActive?: boolean;
}

// Status helper fonksiyonları
export const getBuyboxStatusLabel = (status: BuyboxStatus): string => {
  const labels: Record<BuyboxStatus, string> = {
    WON: "Kazanıldı",
    LOST: "Kaybedildi",
    RISK: "Risk",
    NO_COMPETITION: "Rekabet Yok",
  };
  return labels[status] || status;
};

export const getBuyboxStatusColor = (
  status: BuyboxStatus
): "success" | "destructive" | "warning" | "secondary" => {
  const colors: Record<
    BuyboxStatus,
    "success" | "destructive" | "warning" | "secondary"
  > = {
    WON: "success",
    LOST: "destructive",
    RISK: "warning",
    NO_COMPETITION: "secondary",
  };
  return colors[status] || "secondary";
};

export const getAlertTypeLabel = (type: BuyboxAlertType): string => {
  const labels: Record<BuyboxAlertType, string> = {
    BUYBOX_LOST: "Buybox Kaybı",
    BUYBOX_WON: "Buybox Kazanımı",
    NEW_COMPETITOR: "Yeni Rakip",
    PRICE_RISK: "Fiyat Riski",
  };
  return labels[type] || type;
};

export const getAlertTypeColor = (
  type: BuyboxAlertType
): "success" | "destructive" | "warning" | "default" => {
  const colors: Record<
    BuyboxAlertType,
    "success" | "destructive" | "warning" | "default"
  > = {
    BUYBOX_LOST: "destructive",
    BUYBOX_WON: "success",
    NEW_COMPETITOR: "warning",
    PRICE_RISK: "warning",
  };
  return colors[type] || "default";
};
