// Stock Tracking Types for competitor stock monitoring

export enum StockAlertType {
  OUT_OF_STOCK = "OUT_OF_STOCK",
  LOW_STOCK = "LOW_STOCK",
  BACK_IN_STOCK = "BACK_IN_STOCK",
  STOCK_INCREASED = "STOCK_INCREASED",
}

export enum StockAlertSeverity {
  LOW = "LOW",
  MEDIUM = "MEDIUM",
  HIGH = "HIGH",
  CRITICAL = "CRITICAL",
}

export interface TrackedProduct {
  id: string;
  storeId: string;
  trendyolProductId: number;
  productUrl: string;
  productName: string;
  brandName: string;
  imageUrl: string;
  lastStockQuantity: number | null;
  lastPrice: number | null;
  lastCheckedAt: string | null;
  isActive: boolean;
  alertOnOutOfStock: boolean;
  alertOnLowStock: boolean;
  lowStockThreshold: number;
  alertOnStockIncrease: boolean;
  alertOnBackInStock: boolean;
  createdAt: string;
  updatedAt: string;
  unreadAlertCount?: number;
}

export interface StockSnapshot {
  id: string;
  trackedProductId: string;
  quantity: number;
  inStock: boolean;
  price: number | null;
  previousQuantity: number | null;
  quantityChange: number | null;
  checkedAt: string;
}

export interface StockAlert {
  id: string;
  trackedProductId: string;
  productName: string;
  productImageUrl: string;
  alertType: StockAlertType;
  severity: StockAlertSeverity;
  title: string;
  message: string;
  oldQuantity: number | null;
  newQuantity: number | null;
  threshold: number | null;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface StockStatistics {
  minStock: number;
  maxStock: number;
  avgStock: number;
  totalChecks: number;
  outOfStockCount: number;
  lastOutOfStock: string | null;
}

export interface TrackedProductDetail {
  product: TrackedProduct;
  snapshots: StockSnapshot[];
  alerts: StockAlert[];
  statistics: StockStatistics;
}

export interface StockTrackingDashboard {
  totalTrackedProducts: number;
  activeTrackedProducts: number;
  outOfStockProducts: number;
  lowStockProducts: number;
  outOfStockAlertsToday: number;
  lowStockAlertsToday: number;
  backInStockAlertsToday: number;
  totalUnreadAlerts: number;
  recentAlerts: StockAlert[];
  outOfStockList: TrackedProduct[];
  lowStockList: TrackedProduct[];
}

// Request DTOs
export interface AddTrackedProductRequest {
  productUrl: string;
  alertOnOutOfStock?: boolean;
  alertOnLowStock?: boolean;
  lowStockThreshold?: number;
  alertOnStockIncrease?: boolean;
  alertOnBackInStock?: boolean;
}

export interface UpdateAlertSettingsRequest {
  alertOnOutOfStock?: boolean;
  alertOnLowStock?: boolean;
  lowStockThreshold?: number;
  alertOnStockIncrease?: boolean;
  alertOnBackInStock?: boolean;
  isActive?: boolean;
}

// Product preview before adding to tracking
export interface ProductPreview {
  productId: number | null;
  productName: string | null;
  brandName: string | null;
  imageUrl: string | null;
  price: number | null;
  quantity: number | null;
  inStock: boolean | null;
  isValid: boolean;
  errorMessage?: string;
}

// Helper functions for UI
export function getAlertTypeLabel(type: StockAlertType): string {
  const labels: Record<StockAlertType, string> = {
    [StockAlertType.OUT_OF_STOCK]: "Stok Tükendi",
    [StockAlertType.LOW_STOCK]: "Düşük Stok",
    [StockAlertType.BACK_IN_STOCK]: "Stok Geri Geldi",
    [StockAlertType.STOCK_INCREASED]: "Stok Artışı",
  };
  return labels[type];
}

export function getAlertSeverityColor(severity: StockAlertSeverity): string {
  const colors: Record<StockAlertSeverity, string> = {
    [StockAlertSeverity.LOW]: "text-blue-600 bg-blue-100",
    [StockAlertSeverity.MEDIUM]: "text-yellow-600 bg-yellow-100",
    [StockAlertSeverity.HIGH]: "text-orange-600 bg-orange-100",
    [StockAlertSeverity.CRITICAL]: "text-red-600 bg-red-100",
  };
  return colors[severity];
}

export function getAlertTypeIcon(type: StockAlertType): string {
  const icons: Record<StockAlertType, string> = {
    [StockAlertType.OUT_OF_STOCK]: "package-x",
    [StockAlertType.LOW_STOCK]: "alert-triangle",
    [StockAlertType.BACK_IN_STOCK]: "package-check",
    [StockAlertType.STOCK_INCREASED]: "trending-up",
  };
  return icons[type];
}
