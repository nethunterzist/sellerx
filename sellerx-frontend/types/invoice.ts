/**
 * Invoice types for Trendyol invoice management
 * Matches backend DTOs in com.ecommerce.sellerx.financial.dto
 */

// Invoice type codes from Trendyol
export const INVOICE_TYPE_CODES = {
  // Komisyon
  PLATFORM_HIZMET: 'PLATFORM_HIZMET',
  AZ_KOMISYON: 'AZ_KOMISYON',
  AZ_KOMISYON_GELIRI: 'AZ_KOMISYON_GELIRI',

  // Kargo
  KARGO_FATURA: 'KARGO_FATURA',
  AZ_KARGO: 'AZ_KARGO',
  MP_KARGO_ITIRAZ_IADE: 'MP_KARGO_ITIRAZ_IADE',

  // Uluslararası
  ULUSLARARASI_HIZMET: 'ULUSLARARASI_HIZMET',
  AZ_YURTDISI_OPERASYON: 'AZ_YURTDISI_OPERASYON',
  AZ_PLATFORM_HIZMET: 'AZ_PLATFORM_HIZMET',
  AZ_ULUSLARARASI_HIZMET: 'AZ_ULUSLARARASI_HIZMET',
  YURTDISI_OPERASYON_IADE: 'YURTDISI_OPERASYON_IADE',

  // Ceza/Kesinti
  TEDARIK_EDEMEME: 'TEDARIK_EDEMEME',
  TERMIN_GECIKME: 'TERMIN_GECIKME',
  EKSIK_URUN: 'EKSIK_URUN',
  YANLIS_URUN: 'YANLIS_URUN',
  KUSURLU_URUN: 'KUSURLU_URUN',

  // Reklam
  REKLAM_BEDELI: 'REKLAM_BEDELI',
  SABIT_INFLUENCER_REKLAM: 'SABIT_INFLUENCER_REKLAM',
  KOMISYONLU_INFLUENCER_REKLAM: 'KOMISYONLU_INFLUENCER_REKLAM',

  // Diğer
  KURUMSAL_KAMPANYA: 'KURUMSAL_KAMPANYA',
  ERKEN_ODEME_KESINTI: 'ERKEN_ODEME_KESINTI',
  FATURA_KONTOR_SATIS: 'FATURA_KONTOR_SATIS',
  MUSTERI_DUYURULARI: 'MUSTERI_DUYURULARI',
  TEX_TAZMIN: 'TEX_TAZMIN',

  // İade (Satıcı Trendyol'a keser)
  TZM_TAZMIN: 'TZM_TAZMIN',
  KRM_KURUMSAL_FATURA_FARKI: 'KRM_KURUMSAL_FATURA_FARKI',
  DIF_KARGO_ITIRAZ: 'DIF_KARGO_ITIRAZ',
} as const;

export type InvoiceTypeCode = (typeof INVOICE_TYPE_CODES)[keyof typeof INVOICE_TYPE_CODES];

// Invoice categories
export const INVOICE_CATEGORIES = {
  KOMISYON: 'KOMISYON',
  KARGO: 'KARGO',
  ULUSLARARASI: 'ULUSLARARASI',
  CEZA: 'CEZA',
  REKLAM: 'REKLAM',
  DIGER: 'DIGER',
  IADE: 'IADE',
} as const;

export type InvoiceCategory = (typeof INVOICE_CATEGORIES)[keyof typeof INVOICE_CATEGORIES];

/**
 * Invoice type statistics for dashboard cards
 */
export interface InvoiceTypeStats {
  invoiceTypeCode: string;
  invoiceType: string;
  invoiceCategory: string;
  isDeduction: boolean;
  invoiceCount: number;
  totalAmount: number;
  totalVatAmount: number;
  vatRate?: number;
  icon: string;
  color: string;
}

/**
 * Category summary for category breakdown
 */
export interface CategorySummary {
  category: string;
  invoiceCount: number;
  totalAmount: number;
  totalVatAmount: number;
}

/**
 * Invoice summary response from API
 */
/**
 * Cost of Goods Sold data for KDV page
 */
export interface CostOfGoodsSold {
  totalCostIncludingVat: number;
  totalCostVatAmount: number;
  totalItemsSold: number;
  itemsWithoutCost: number;
  itemsWithoutCostVat: number;
}

/**
 * Sales VAT breakdown by rate (e.g., 1%, 10%, 20%)
 */
export interface SalesVatByRate {
  vatRate: number;
  salesAmount: number;
  vatAmount: number;
  itemCount: number;
}

/**
 * Sales VAT summary for KDV page
 */
export interface SalesVat {
  totalSalesAmount: number;
  totalVatAmount: number;
  totalItemsSold: number;
  itemsWithoutVatRate: number;
  byRate: SalesVatByRate[];
}

export interface InvoiceSummary {
  storeId: string;
  periodStart: string;
  periodEnd: string;
  totalDeductions: number;
  totalRefunds: number;
  netAmount: number;
  totalInvoiceCount: number;
  invoicesByType: InvoiceTypeStats[];
  invoicesByCategory: CategorySummary[];
  costOfGoodsSold?: CostOfGoodsSold;
  salesVat?: SalesVat;
}

/**
 * Invoice detail for table display
 */
export interface InvoiceDetail {
  id: string;
  invoiceNumber: string;
  invoiceType: string;
  invoiceTypeCode: string;
  invoiceCategory: string;
  invoiceDate: string;
  amount: number;
  vatAmount: number;
  vatRate: number;
  baseAmount: number;
  isDeduction: boolean;
  orderNumber: string | null;
  shipmentPackageId: number | null;
  paymentOrderId: number | null;
  barcode: string | null;
  productName: string | null;
  desi: number | null;
  description: string | null;
  details: Record<string, unknown> | null;
}

/**
 * Paginated invoice response
 */
export interface InvoicePage {
  content: InvoiceDetail[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/**
 * Sync response from API
 */
export interface InvoiceSyncResponse {
  success: boolean;
  message: string;
  syncedCount: number;
  startDate: string;
  endDate: string;
}

/**
 * Invoice filter options
 */
export interface InvoiceFilters {
  startDate?: string;
  endDate?: string;
  typeCode?: string;
  category?: string;
  page?: number;
  size?: number;
}

/**
 * Get icon name for invoice category
 */
export function getCategoryIcon(category: string): string {
  switch (category) {
    case INVOICE_CATEGORIES.KOMISYON:
      return 'receipt';
    case INVOICE_CATEGORIES.KARGO:
      return 'truck';
    case INVOICE_CATEGORIES.ULUSLARARASI:
      return 'globe';
    case INVOICE_CATEGORIES.CEZA:
      return 'alert-triangle';
    case INVOICE_CATEGORIES.REKLAM:
      return 'megaphone';
    case INVOICE_CATEGORIES.IADE:
      return 'refresh-ccw';
    default:
      return 'file-text';
  }
}

/**
 * Get color for deduction/refund status
 */
export function getInvoiceColor(isDeduction: boolean): string {
  return isDeduction ? 'red' : 'green';
}

/**
 * Get Turkish display name for invoice category
 */
export function getCategoryDisplayName(category: string): string {
  switch (category) {
    case INVOICE_CATEGORIES.KOMISYON:
      return 'Komisyon';
    case INVOICE_CATEGORIES.KARGO:
      return 'Kargo';
    case INVOICE_CATEGORIES.ULUSLARARASI:
      return 'Uluslararası';
    case INVOICE_CATEGORIES.CEZA:
      return 'Ceza/Kesinti';
    case INVOICE_CATEGORIES.REKLAM:
      return 'Reklam';
    case INVOICE_CATEGORIES.DIGER:
      return 'Diğer';
    case INVOICE_CATEGORIES.IADE:
      return 'Geri Yatan Ödeme';
    default:
      return category;
  }
}

/**
 * Fix corrupted Turkish characters in invoice type names
 * Trendyol API sometimes sends corrupted encoding for Turkish characters
 */
const INVOICE_TYPE_CORRECTIONS: Record<string, string> = {
  'AZ-YURTDÕ_Õ OPERASYON BEDELI %18': 'AZ-Yurtdışı Operasyon Bedeli %18',
  'YURTDÕ_Õ OPERASYON BEDELI': 'Yurtdışı Operasyon Bedeli',
  'YURTDI_I OPERASYON': 'Yurtdışı Operasyon',
};

export function fixInvoiceTypeName(invoiceType: string): string {
  if (!invoiceType) return invoiceType;

  // Check for exact match first
  if (INVOICE_TYPE_CORRECTIONS[invoiceType]) {
    return INVOICE_TYPE_CORRECTIONS[invoiceType];
  }

  // Check for partial matches (case-insensitive)
  const upperType = invoiceType.toUpperCase();
  for (const [corrupted, fixed] of Object.entries(INVOICE_TYPE_CORRECTIONS)) {
    if (upperType.includes(corrupted.toUpperCase())) {
      return invoiceType.replace(new RegExp(corrupted, 'gi'), fixed);
    }
  }

  return invoiceType;
}

// ========================================
// Cargo Invoice Item Types (Kargo Fatura Detayı)
// ========================================

/**
 * Individual cargo invoice item (shipment within a cargo invoice)
 * Represents one shipment's cargo cost within a larger cargo invoice
 */
export interface CargoInvoiceItem {
  id: string;
  invoiceSerialNumber: string;
  orderNumber?: string;
  shipmentPackageId?: number;
  amount: number;
  desi?: number;
  shipmentPackageType?: string;
  vatRate?: number;
  vatAmount?: number;
  invoiceDate?: string;
  barcode?: string;
  productName?: string;
  productImageUrl?: string;
  cargoCompany?: string;
  createdAt?: string;
}

/**
 * Cargo items response (breakdown of a cargo invoice)
 * Shows all shipments within a specific cargo invoice serial number
 */
export interface CargoInvoiceItemsResponse {
  invoiceSerialNumber: string;
  itemCount: number;
  totalAmount: number;
  totalVatAmount: number;
  items: CargoInvoiceItem[];
}

// ========================================
// Generic Invoice Item Types (Genel Fatura Detayi)
// For CEZA, KOMISYON, and other non-cargo invoice types
// ========================================

/**
 * Generic invoice item (order within an invoice)
 * Used for CEZA (penalty), KOMISYON, and other non-cargo invoice types
 */
export interface InvoiceItem {
  id: string;
  invoiceSerialNumber: string;
  orderNumber?: string;
  shipmentPackageId?: number;
  paymentOrderId?: number;
  amount: number;
  vatAmount?: number;
  vatRate?: number;
  transactionDate?: string;
  transactionType?: string;
  description?: string;
  createdAt?: string;
}

/**
 * Invoice items response (breakdown of a generic invoice)
 * Shows all orders/items within a specific invoice serial number
 */
export interface InvoiceItemsResponse {
  invoiceSerialNumber: string;
  itemCount: number;
  totalAmount: number;
  totalVatAmount: number;
  items: InvoiceItem[];
}

/**
 * Invoice type codes that support detail view (have order-level data)
 */
export const DETAIL_SUPPORTED_TYPE_CODES = [
  // KARGO types (use CargoItemsModal)
  'KARGO_FATURA',
  'AZ_KARGO',
  'KARGO_ITIRAZ_IADE',
  // CEZA types (use InvoiceItemsModal)
  'TEDARIK_EDEMEME',
  'TERMIN_GECIKME',
  'EKSIK_URUN',
  'YANLIS_URUN',
  'KUSURLU_URUN',
] as const;

/**
 * KARGO type codes (use cargo-items endpoint)
 */
export const KARGO_TYPE_CODES = [
  'KARGO_FATURA',
  'AZ_KARGO',
  'KARGO_ITIRAZ_IADE',
] as const;

/**
 * Check if invoice type supports detail view
 */
export function supportsDetailView(typeCode: string): boolean {
  return DETAIL_SUPPORTED_TYPE_CODES.includes(typeCode as any);
}

/**
 * Check if invoice type is a KARGO type (uses cargo-items endpoint)
 */
export function isKargoType(typeCode: string): boolean {
  return KARGO_TYPE_CODES.includes(typeCode as any);
}

// ========================================
// Commission Invoice Item Types (Komisyon Fatura Detayı)
// ========================================

/**
 * KOMISYON type codes (use commission-items endpoint)
 */
export const KOMISYON_TYPE_CODES = [
  'PLATFORM_HIZMET',
  'AZ_KOMISYON',
  'AZ_KOMISYON_GELIRI',
  'KOMISYON_FATURASI',
] as const;

/**
 * Check if invoice type is a KOMISYON type (uses commission-items endpoint)
 */
export function isKomisyonType(typeCode: string): boolean {
  return KOMISYON_TYPE_CODES.includes(typeCode as any);
}

/**
 * Individual commission invoice item (order within a commission invoice)
 * Data comes from trendyol_orders.financial_transactions JSONB column
 */
export interface CommissionInvoiceItem {
  orderNumber: string;
  orderDate: string;
  barcode?: string;
  productName?: string;
  commissionRate?: number;
  commissionAmount?: number;
  sellerRevenue?: number;
  trendyolRevenue?: number;
  transactionType?: string;
  // Yeni alanlar - Trendyol Excel export uyumu
  recordId?: string;          // Kayıt No
  transactionDate?: string;   // İşlem Tarihi
  paymentPeriod?: number;     // Vade Süresi (gün)
  paymentDate?: string;       // Vade Tarihi
  totalAmount?: number;       // Toplam Tutar
}

/**
 * Commission items response (breakdown of a commission invoice)
 * Shows all orders within a specific commission invoice serial number
 */
export interface CommissionInvoiceItemsResponse {
  invoiceSerialNumber: string;
  itemCount: number;
  totalCommission: number;
  totalSellerRevenue: number;
  items: CommissionInvoiceItem[];
}

/**
 * Update supportsDetailView to include KOMISYON types
 */
export function supportsDetailViewExtended(typeCode: string): boolean {
  return isKargoType(typeCode) || isKomisyonType(typeCode);
}

// ========================================
// Aggregated Invoice Product Types (Ürün Bazlı Aggregasyon)
// For "Ürünler" tab in invoice detail panel
// ========================================

/**
 * Aggregated product data from invoice items
 * Groups invoice items by barcode (SKU) and shows totals
 * Used in "Ürünler" tab when user clicks on KARGO or KOMISYON invoice cards
 */
export interface AggregatedInvoiceProduct {
  barcode: string;
  productName: string;
  productImage?: string;
  productUrl?: string;
  /** Number of invoice items (orders/shipments) this product appears in */
  itemCount: number;
  /** Total amount deducted/refunded for this product */
  totalAmount: number;
  /** Total desi (volumetric weight) - only for KARGO invoices */
  totalDesi?: number;
  /** Total commission amount - only for KOMISYON invoices */
  totalCommission?: number;
}

// ========================================
// Category-Level Invoice Item Types (Kategori Bazlı Tüm Kalemler)
// For "Fatura Kalemleri" and "Ürünler" tabs in category view
// ========================================

/**
 * Aggregated product data from backend AggregatedProductDto
 * Groups invoice items by barcode (SKU) and shows totals for a category
 */
export interface AggregatedProduct {
  barcode: string;
  productName: string;
  productImageUrl?: string;
  productUrl?: string;
  /** Total quantity (number of invoice items/shipments for this product) */
  totalQuantity: number;
  /** Total amount deducted/charged for this product */
  totalAmount: number;
  /** Total VAT amount for this product */
  totalVatAmount: number;
  /** Number of distinct invoices containing this product */
  invoiceCount: number;
  /** Total desi (volumetric weight) - only for KARGO invoices */
  totalDesi?: number;
  /** Total commission amount - only for KOMISYON invoices (NET: Satış - İndirim - Kupon) */
  totalCommission?: number;
  // ================================================================================
  // Commission breakdown fields (only for KOMISYON category)
  // Net Commission = Satış - İndirim - Kupon (İade excluded from commission invoice)
  // ================================================================================
  /** Total sale commission (Satış) - positive, goes to Trendyol */
  saleCommission?: number;
  /** Total discount commission (İndirim) - stored as positive, but subtracted in net calculation */
  discountCommission?: number;
  /** Total coupon commission (Kupon) - stored as positive, but subtracted in net calculation */
  couponCommission?: number;
}

/**
 * Response wrapper for aggregated products by category
 */
export interface AggregatedProductsResponse {
  products: AggregatedProduct[];
  totalProducts: number;
  totalAmount: number;
  totalVatAmount: number;
}

/**
 * Paginated cargo items response for category view
 * All cargo invoice items in a date range (not per invoice)
 */
export interface CategoryCargoItemsPage {
  content: CargoInvoiceItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/**
 * Paginated commission items response for category view
 * All commission invoice items (deductions) in a date range
 */
export interface CategoryCommissionItemsPage {
  content: InvoiceDetail[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ========================================
// Product Commission Breakdown Types
// For "Detay" panel in KOMISYON Ürünler tab
// ========================================

/**
 * Transaction type breakdown item
 * Shows commission breakdown by transaction type (Satış, Kupon, İndirim)
 */
export interface TransactionTypeBreakdownItem {
  transactionType: string;
  transactionTypeDisplay: string;
  itemCount: number;
  totalCommission: number;
  totalVatAmount: number;
  averageCommissionRate?: number;
}

/**
 * Product commission breakdown response
 * Shows commission breakdown by transaction type for a specific product
 * Net Commission = Satış - İndirim - Kupon (İade excluded from commission invoice)
 */
export interface ProductCommissionBreakdown {
  barcode: string;
  productName: string;
  productImageUrl?: string;
  productUrl?: string;
  /** Net commission (Satış - İndirim - Kupon) */
  totalCommission: number;
  /** Net VAT amount */
  totalVatAmount: number;
  totalItemCount: number;
  orderCount: number;
  breakdown: TransactionTypeBreakdownItem[];
  // ================================================================================
  // Individual transaction type totals
  // Net Commission = Satış - İndirim - Kupon (İade excluded from commission invoice)
  // ================================================================================
  /** Total sale commission (Satış) - positive, goes to Trendyol */
  saleCommission?: number;
  /** Total discount commission (İndirim) - stored as positive, but SUBTRACTED in net calculation */
  discountCommission?: number;
  /** Total coupon commission (Kupon) - stored as positive, but SUBTRACTED in net calculation */
  couponCommission?: number;
  /** Total return commission (İade) - tracked but EXCLUDED from commission invoice calculation */
  returnCommission?: number;
}

// ========================================
// Product Cargo Breakdown Types
// For "Detay" panel in KARGO Ürünler tab
// ========================================

/**
 * Individual cargo shipment detail
 * Shows one shipment's cargo cost within the breakdown
 */
export interface CargoShipmentDetail {
  orderNumber: string;
  shipmentPackageId?: number;
  invoiceSerialNumber: string;
  amount: number;
  vatAmount?: number;
  desi?: number;
  invoiceDate: string;
  cargoCompany?: string;
}

/**
 * Product cargo breakdown response
 * Shows cargo cost breakdown with shipment list for a specific product
 */
export interface ProductCargoBreakdown {
  barcode: string;
  productName: string;
  productImageUrl?: string;
  productUrl?: string;
  /** Total cargo cost amount for this product */
  totalAmount: number;
  /** Total VAT amount for this product's cargo */
  totalVatAmount: number;
  /** Total desi (volumetric weight) for all shipments */
  totalDesi: number;
  /** Total number of shipments/cargo invoices for this product */
  totalShipmentCount: number;
  /** Number of distinct orders for this product */
  orderCount: number;
  /** Average desi per shipment */
  averageDesi: number;
  /** Average cost per shipment */
  averageCostPerShipment: number;
  /** List of recent shipments (limited to last 50) */
  shipments: CargoShipmentDetail[];
}
