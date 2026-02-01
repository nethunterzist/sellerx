// Dashboard types matching backend DTOs
import type { ExpenseFrequency } from "./expense";

export type PeriodType = "today" | "yesterday" | "thisMonth" | "lastMonth";

// Custom date range types
export type DateRangePreset =
  | "default"      // 5-card: today, yesterday, mtd, mtdForecast, lastMonth
  | "basic"        // 4-card: today, yesterday, mtd, lastMonth
  | "days"         // 5-card: today, yesterday, 7d, 14d, 30d
  | "weeks"        // 4-card: this week, last week, 2w ago, 3w ago
  | "months"       // 4-card: this month, last month, 2m ago, 3m ago
  | "daysAgo"      // 4-card: today, yesterday, 2d ago, 3d ago
  | "weekDays"     // 4-card: today, yesterday, 7d ago, 8d ago
  | "quarters"     // 4-card: this quarter, last quarter, 2q ago, 3q ago
  | "last7days"    // Single card: last 7 days
  | "last14days"   // Single card: last 14 days
  | "last30days"   // Single card: last 30 days
  | "thisQuarter"  // Single card: this quarter
  | "lastQuarter"  // Single card: last quarter
  | "custom";

// Comparison mode for period analysis
export type ComparisonMode =
  | "none"
  | "previous_period"
  | "same_period_last_year";

// Period definition for dynamic card rendering
export interface PeriodDefinition {
  id: string;
  label: string;
  shortLabel: string;
  dateRange: string; // Display date range, e.g., "16 Ocak 2026"
  color: "blue" | "teal" | "green" | "orange" | "purple" | "pink";
}

// Period preset with multiple periods
export interface PeriodPreset {
  id: DateRangePreset;
  label: string;
  periodCount: number; // Number of cards to show (4 or 5)
}

export interface DateRangeParams {
  startDate: string; // ISO date: "2025-01-01"
  endDate: string; // ISO date: "2025-01-15"
  periodLabel?: string;
}

// Dashboard view types
export type DashboardViewType = "tiles" | "chart" | "pl" | "trends" | "cities";

// Custom date range for filters
export interface CustomDateRange {
  startDate: string; // ISO date: "2025-01-01"
  endDate: string;
  label: string; // Display label: "Son 7 Gün" or "10 Oca - 16 Oca 2025"
}

// Tab-specific filter state
export interface TabFilterState {
  selectedProducts: string[];
  selectedPeriodGroup: DateRangePreset;
  customDateRange: CustomDateRange | null;
  selectedPeriod: PeriodType;
  selectedComparison: ComparisonMode;
  selectedCurrency: string;
}

// All tabs' filter states
export type TabFiltersState = Record<DashboardViewType, TabFilterState>;

// Filter configuration per view
export interface ViewFilterConfig {
  usesProducts: boolean;
  usesDateRange: boolean;
  usesCurrency: boolean;
  usesComparison: boolean;
  singleProductOnly?: boolean;
}

// Re-export for convenience
export type { ExpenseFrequency };

// Multi-period support for dynamic preset ranges
export interface PeriodRange {
  startDate: string; // ISO date: "2025-01-01"
  endDate: string;   // ISO date: "2025-01-15"
  label: string;     // Full label: "Bu Ay"
  shortLabel: string; // Short label for cards: "Bu Ay"
  color: "blue" | "teal" | "green" | "orange" | "purple" | "pink";
}

export interface MultiPeriodConfig {
  preset: DateRangePreset;
  periods: PeriodRange[];
}

// Multi-period stats response (array of DashboardStats with labels)
export interface MultiPeriodStats {
  periods: Array<{
    stats: DashboardStats;
    label: string;
    shortLabel: string;
    dateRange: string;
    color: "blue" | "teal" | "green" | "orange" | "purple" | "pink";
  }>;
  isLoading: boolean;
  error: Error | null;
}

// Backend: OrderProductDetailDto
export interface OrderProductDetail {
  barcode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  cost: number;
  totalCost: number;
  commission: number;
  profit: number;
}

// Backend: OrderDetailDto
export interface OrderDetail {
  orderNumber: string;
  orderDate: string; // ISO datetime string
  products: OrderProductDetail[];
  totalPrice: number;
  returnPrice: number;
  revenue: number;
  grossProfit: number;
  stoppage: number;
  estimatedCommission: number;
  estimatedShippingCost: number; // Kargo maliyeti
}

// OrderDetailPanel için genişletilmiş sipariş verisi
export interface OrderDetailPanelData {
  orderNumber: string;
  orderDate: string;
  products: OrderProductDetail[];
  // Fiyatlandırma
  totalPrice: number;
  returnPrice: number;
  revenue: number;
  // Maliyetler
  totalProductCost: number;
  estimatedCommission: number;
  estimatedShippingCost: number; // Kargo maliyeti
  stoppage: number;
  // Kâr
  grossProfit: number;
  netProfit: number;
  profitMargin: number;
  roi: number;
  // Durum (opsiyonel)
  status?: string;
  shipmentInfo?: {
    cargoCompany?: string;
    trackingNumber?: string;
  };
}

// Backend: ProductDetailDto
export interface ProductDetail {
  productName: string;
  barcode: string;
  brand?: string; // marka
  image?: string; // ürün görseli URL
  productUrl?: string; // Trendyol ürün sayfası URL'i
  stock: number; // stok adedi
  totalSoldQuantity: number;
  returnQuantity: number;
  revenue: number;
  grossProfit: number;
  estimatedCommission: number;

  // ============== YENİ ALANLAR (Ürün Bazlı Metrikler) ==============

  // İndirimler
  sellerDiscount?: number;       // Satıcı indirimi
  platformDiscount?: number;     // Platform indirimi
  couponDiscount?: number;       // Kupon indirimi
  totalDiscount?: number;        // Toplam indirim

  // Net Ciro
  netRevenue?: number;           // Net ciro = brüt ciro - indirimler

  // Maliyetler
  productCost?: number;          // Ürün maliyeti (FIFO)
  shippingCost?: number;         // Kargo maliyeti (sipariş bazlı)

  // İade
  refundCost?: number;           // İade maliyeti
  refundRate?: number;           // İade oranı (%)

  // Kargo tahmini durumu
  isShippingEstimated?: boolean; // true: tahmini kargo, false: gerçek kargo faturası

  // Net Kar ve Metrikler
  netProfit?: number;            // Net kar
  profitMargin?: number;         // Kar marjı (%)
  roi?: number;                  // ROI (%)

  // Kategori
  categoryName?: string;         // Ürün kategorisi

  // Sipariş sayısı
  orderCount?: number;           // Sipariş adedi
}

// Backend: PeriodExpenseDto
export interface PeriodExpense {
  expenseName: string;
  expenseQuantity: number;
  expenseTotal: number;
  expenseFrequency: ExpenseFrequency;
}

// Backend: DashboardStatsDto
export interface DashboardStats {
  period: PeriodType;
  totalOrders: number;
  totalProductsSold: number;
  netUnitsSold?: number;        // Net Satış Adedi = Brüt Satış - İade Adedi
  totalRevenue: number;
  returnCount: number;
  returnCost: number;
  totalProductCosts: number;
  grossProfit: number;
  netProfit: number;        // Net Kar = brüt kar - komisyon - stopaj - iade maliyeti - giderler
  profitMargin: number;     // Kar Marjı % = (brüt kar / ciro) * 100
  vatDifference: number;
  totalStoppage: number;
  totalEstimatedCommission: number;
  itemsWithoutCost: number;
  totalExpenseNumber: number;
  totalExpenseAmount: number;
  orders: OrderDetail[];
  products: ProductDetail[];
  expenses: PeriodExpense[];

  // ============== YENİ ALANLAR (32 Metrik) ==============

  // ============== KESİLEN FATURALAR (Dashboard Kartları için) ==============
  // Toplam kesilen faturalar: REKLAM + CEZA + ULUSLARARASI + DIGER - IADE
  // NOT: KOMISYON ve KARGO faturaları HARİÇ (bunlar sipariş bazlı hesaplanıyor)
  invoicedDeductions?: number;        // Toplam kesilen faturalar

  // Fatura Kategorileri Detayı (Detay modalı için)
  invoicedAdvertisingFees?: number;   // REKLAM kategorisi
  invoicedPenaltyFees?: number;       // CEZA kategorisi
  invoicedInternationalFees?: number; // ULUSLARARASI kategorisi
  invoicedOtherFees?: number;         // DIGER kategorisi
  invoicedRefunds?: number;           // IADE kategorisi (pozitif değer)

  // Eski alan - uyumluluk için korunuyor
  invoicedAdvertisingCost?: number;

  // İndirimler & Kuponlar
  totalSellerDiscount?: number;    // Satıcı indirimi
  totalPlatformDiscount?: number;  // Platform indirimi (Trendyol)
  totalCouponDiscount?: number;    // Kupon indirimi

  // Kargo Maliyetleri
  totalShippingCost?: number;      // Kargo gönderim maliyeti
  totalShippingIncome?: number;    // Kargo geliri (alıcıdan alınan)

  // Platform Ücretleri (15 kategori - TrendyolStoppage description'dan parse)
  internationalServiceFee?: number;  // Uluslararası Hizmet Bedeli
  overseasOperationFee?: number;     // Yurt Dışı Operasyon Bedeli
  terminDelayFee?: number;           // Termin Gecikme Bedeli
  platformServiceFee?: number;       // Platform Hizmet Bedeli
  invoiceCreditFee?: number;         // Fatura Kontör Satış Bedeli
  unsuppliedFee?: number;            // Tedarik Edememe
  azOverseasOperationFee?: number;   // AZ-Yurtdışı Operasyon Bedeli
  azPlatformServiceFee?: number;     // AZ-Platform Hizmet Bedeli
  packagingServiceFee?: number;      // Paketleme Hizmet Bedeli
  warehouseServiceFee?: number;      // Depo Hizmet Bedeli
  callCenterFee?: number;            // Çağrı Merkezi Bedeli
  photoShootingFee?: number;         // Fotoğraf Çekim Bedeli
  integrationFee?: number;           // Entegrasyon Bedeli
  storageServiceFee?: number;        // Depolama Hizmet Bedeli
  otherPlatformFees?: number;        // Diğer Platform Ücretleri

  // Erken Ödeme Maliyeti (Settlement API'den)
  earlyPaymentFee?: number;

  // Gider Kategorileri (StoreExpense category bazlı)
  officeExpenses?: number;       // Ofis giderleri (eski - uyumluluk için)
  packagingExpenses?: number;    // Ambalaj/Paketleme giderleri (eski - uyumluluk için)
  accountingExpenses?: number;   // Muhasebe giderleri (eski - uyumluluk için)
  otherExpenses?: number;        // Diğer giderler (eski - uyumluluk için)

  // Dinamik gider kategorileri - yeni kategoriler otomatik desteklenir
  expensesByCategory?: Record<string, number>;

  // İade Detayları
  refundRate?: number;           // İade oranı (%)

  // Net Ciro (Brüt Ciro - İndirimler)
  netRevenue?: number;

  // ROI (Return on Investment)
  roi?: number;
}

// Backend: DashboardStatsResponse
export interface DashboardStatsResponse {
  today: DashboardStats;
  yesterday: DashboardStats;
  thisMonth: DashboardStats;
  lastMonth: DashboardStats;
  storeId: string;
  calculatedAt: string; // ISO datetime string
}

// P&L Multi-Period Types
export type PLPeriodType = "monthly" | "weekly" | "daily";

export type PLPeriodPreset =
  | "last12months"      // Son 12 ay, aylık
  | "last3monthsWeekly" // Son 3 ay, haftalık
  | "last30days"        // Son 30 gün, günlük
  | "custom";

export interface PLPeriodPresetConfig {
  id: PLPeriodPreset;
  label: string;
  periodType: PLPeriodType;
  periodCount: number;
}

// Backend: PeriodStatsDto (single period for multi-period response)
export interface PeriodStats {
  periodLabel: string;      // "Ara 2025", "Hafta 52", "17 Oca"
  startDate: string;        // ISO date: "2025-01-01"
  endDate: string;          // ISO date: "2025-01-31"
  totalOrders: number;
  totalProductsSold: number;
  totalRevenue: number;
  returnCount: number;
  returnCost: number;
  totalProductCosts: number;
  grossProfit: number;
  vatDifference: number;
  totalStoppage: number;
  totalEstimatedCommission: number;
  netProfit: number;        // grossProfit - commission - stoppage - returnCost - expenses
  profitMargin: number;     // (grossProfit / revenue) * 100
  roi: number;              // (netProfit / productCosts) * 100
  itemsWithoutCost: number;
  totalExpenseNumber: number;
  totalExpenseAmount: number;

  // ============== YENİ ALANLAR (32 Metrik için genişletilmiş) ==============

  // Faturalı Reklam (Trendyol DeductionInvoices API'den)
  invoicedAdvertisingCost?: number;

  // İndirimler & Kuponlar
  totalSellerDiscount?: number;
  totalPlatformDiscount?: number;
  totalCouponDiscount?: number;

  // Kargo
  totalShippingCost?: number;
  totalShippingIncome?: number;

  // Platform Ücretleri (15 kategori)
  internationalServiceFee?: number;
  overseasOperationFee?: number;
  terminDelayFee?: number;
  platformServiceFee?: number;
  invoiceCreditFee?: number;
  unsuppliedFee?: number;
  azOverseasOperationFee?: number;
  azPlatformServiceFee?: number;
  packagingServiceFee?: number;
  warehouseServiceFee?: number;
  callCenterFee?: number;
  photoShootingFee?: number;
  integrationFee?: number;
  storageServiceFee?: number;
  otherPlatformFees?: number;

  // Erken Ödeme
  earlyPaymentFee?: number;

  // Gider Kategorileri
  officeExpenses?: number;
  packagingExpenses?: number;
  accountingExpenses?: number;
  otherExpenses?: number;

  // İade
  refundRate?: number;

  // Net Ciro
  netRevenue?: number;
}

// Backend: MultiPeriodStatsResponse
export interface MultiPeriodStatsResponse {
  periods: PeriodStats[];   // Stats for each period (most recent first)
  total: PeriodStats;       // Aggregated totals
  storeId: string;
  periodType: PLPeriodType;
  periodCount: number;
  calculatedAt: string;
}

// P&L Preset configurations
export const PL_PERIOD_PRESETS: PLPeriodPresetConfig[] = [
  { id: "last12months", label: "Son 12 ay, aylık", periodType: "monthly", periodCount: 12 },
  { id: "last3monthsWeekly", label: "Son 3 ay, haftalık", periodType: "weekly", periodCount: 13 },
  { id: "last30days", label: "Son 30 gün, günlük", periodType: "daily", periodCount: 30 },
];
