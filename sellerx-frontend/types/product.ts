// Product types matching backend DTOs

// Backend: CostAndStockInfoDto
export interface CostAndStockInfo {
  quantity: number;
  unitCost: number;
  costVatRate: number;
  stockDate: string; // ISO date string (YYYY-MM-DD)
  usedQuantity: number;
  costSource?: 'AUTO_DETECTED' | 'MANUAL' | 'PURCHASE_ORDER' | null;
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
}

export interface ProductFilters {
  search?: string;
  onSale?: boolean;
  approved?: boolean;
  archived?: boolean;
  hasStock?: boolean;
  minPrice?: number;
  maxPrice?: number;
}
