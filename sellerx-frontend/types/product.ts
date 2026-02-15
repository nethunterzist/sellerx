// Product types matching backend DTOs

// Backend: CostAndStockInfoDto
export interface CostAndStockInfo {
  quantity: number;
  unitCost: number;
  costVatRate: number;
  stockDate: string; // ISO date string (YYYY-MM-DD)
  usedQuantity: number;
  costSource?: 'AUTO_DETECTED' | 'MANUAL' | 'PURCHASE_ORDER' | null;

  // ============== Döviz Kuru Desteği (Excel F1, F2, F4) ==============
  currency?: 'TRY' | 'USD' | 'EUR' | null; // Para birimi (null = TRY)
  exchangeRate?: number | null; // Döviz kuru (örn: 44.0 TL/$)
  foreignCost?: number | null; // Yabancı para cinsinden maliyet (örn: 10 $)

  // ============== ÖTV Desteği (Excel F5) ==============
  otvRate?: number | null; // Özel Tüketim Vergisi oranı (örn: 0.2 = %20)

}

// Backend: TrendyolProductDto
export interface TrendyolProduct {
  id: string; // UUID
  storeId: string;
  productId: string;
  barcode: string;
  title: string;
  categoryName: string;
  brand: string;
  brandId: number;
  pimCategoryId: number;
  image: string;
  productUrl: string;
  salePrice: number;
  vatRate: number;
  trendyolQuantity: number;
  commissionRate: number;
  dimensionalWeight: number;
  shippingVolumeWeight: number;
  approved: boolean;
  archived: boolean;
  blacklisted: boolean;
  rejected: boolean;
  onSale: boolean;
  hasActiveCampaign: boolean;
  costAndStockInfo: CostAndStockInfo[];
  hasAutoDetectedCost?: boolean;
  createdAt: string; // ISO datetime
  updatedAt: string; // ISO datetime

  // ============== Reklam Metrikleri (Excel C23, C24) ==============
  cpc?: number | null; // Cost Per Click (TL) - ürün varsayılan değeri
  cvr?: number | null; // Conversion Rate (örn: 0.018 = %1.8) - ürün varsayılan değeri
  advertisingCostPerSale?: number | null; // Hesaplanan: cpc / cvr
  acos?: number | null; // ACOS: (advertisingCostPerSale / salePrice) * 100

  // ============== Döviz Kuru (Excel F1) ==============
  defaultCurrency?: 'TRY' | 'USD' | 'EUR' | null; // Varsayılan para birimi
  defaultExchangeRate?: number | null; // Varsayılan döviz kuru

  // ============== ÖTV (Excel F5) ==============
  otvRate?: number | null; // Özel Tüketim Vergisi oranı

  // ============== Kargo Maliyeti ==============
  lastShippingCostPerUnit?: number | null; // Son kargo faturasından hesaplanan birim kargo maliyeti

  // ============== Komisyon ==============
  lastCommissionRate?: number | null; // Son komisyon faturasından gelen oran (%)

  // ============== Buybox Bilgileri ==============
  buyboxOrder?: number | null;
  buyboxPrice?: number | null;
  hasMultipleSeller?: boolean;
  buyboxUpdatedAt?: string | null;
}

// Backend: ProductListResponse
export interface ProductListResponse {
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  isFirst: boolean;
  isLast: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  products: TrendyolProduct[];
}

// Backend: SyncProductsResponse
export interface SyncProductsResponse {
  success: boolean;
  message: string;
  totalFetched: number;
  totalSaved: number;
  totalUpdated: number;
  totalSkipped: number;
}

// Request types for product operations
export interface UpdateCostAndStockRequest {
  quantity: number;
  unitCost: number;
  costVatRate: number;
  stockDate: string;

  // ============== Döviz Kuru Desteği (Excel F1, F2, F4) ==============
  currency?: 'TRY' | 'USD' | 'EUR' | null;
  exchangeRate?: number | null;
  foreignCost?: number | null;

  // ============== ÖTV Desteği (Excel F5) ==============
  otvRate?: number | null;

  // ============== Reklam Metrikleri (Excel C23, C24) ==============
  cpc?: number | null;
  cvr?: number | null;
}

export interface ProductFilters {
  search?: string;
  minStock?: number;
  maxStock?: number;
  minPrice?: number;
  maxPrice?: number;
  minCommission?: number;
  maxCommission?: number;
  minCost?: number;
  maxCost?: number;
}

// Buybox types
export interface BuyboxSummary {
  totalProducts: number;
  buyboxWinning: number;
  buyboxLosing: number;
  withCompetitors: number;
  noCompetition: number;
  notChecked: number;
  winRate: number;
}

export type BuyboxStatus = "WINNING" | "LOSING" | "NO_COMPETITION" | "NOT_CHECKED";

export interface BuyboxProduct {
  productId: string;
  barcode: string;
  title: string;
  image: string;
  productUrl: string | null;
  salePrice: number;
  buyboxOrder: number | null;
  buyboxPrice: number | null;
  hasMultipleSeller: boolean;
  priceDifference: number | null;
  buyboxStatus: BuyboxStatus;
  buyboxUpdatedAt: string | null;
}

export interface BuyboxProductsResponse {
  content: BuyboxProduct[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
