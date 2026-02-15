// Financial types matching backend DTOs

// Transaction status from backend
export type TransactionStatus =
  | "SOLD" | "RETURNED" | "DISCOUNT" | "COUPON" | "CANCELLED"
  | "EARLY_PAYMENT"
  | "DISCOUNT_CANCEL" | "COUPON_CANCEL"
  | "MANUAL_REFUND" | "MANUAL_REFUND_CANCEL"
  | "TY_DISCOUNT" | "TY_DISCOUNT_CANCEL"
  | "TY_COUPON" | "TY_COUPON_CANCEL"
  | "PROVISION_POSITIVE" | "PROVISION_NEGATIVE"
  | "COMMISSION_POSITIVE" | "COMMISSION_NEGATIVE";

// Financial settlement item from Trendyol API
export interface FinancialSettlementItem {
  id: string;
  barcode: string;
  transactionType: string;
  status: TransactionStatus;
  receiptId: number;
  debt: number;
  credit: number;
  paymentPeriod: number;
  commissionRate: number;
  commissionAmount: number;
  commissionInvoiceSerialNumber?: string;
  sellerRevenue: number;
  paymentOrderId: number;
  shipmentPackageId: number;
}

// Transaction summary per order item
export interface FinancialOrderItemSummary {
  totalPrice: number;
  totalDiscount: number;
  totalCoupon: number;
  totalCommission: number;
  finalPrice: number;
  netAmount: number;
  soldQuantity: number;
  returnedQuantity: number;
}

// Financial data per product in an order
export interface FinancialOrderItemData {
  barcode: string;
  transactions: FinancialSettlementItem[];
  transactionSummary: FinancialOrderItemSummary;
}

// Order-level transaction summary
export interface FinancialOrderSummary {
  totalPrice: number;
  totalDiscount: number;
  totalCoupon: number;
  totalCommission: number;
  finalPrice: number;
  netAmount: number;
  totalSoldQuantity: number;
  totalReturnedQuantity: number;
  uniqueProductCount: number;
}

// Financial summary for a store
export interface FinancialSummary {
  storeId: string;
  periodStart: string;
  periodEnd: string;
  totalSales: number;
  totalCommission: number;
  totalDiscounts: number;
  totalCoupons: number;
  totalReturns: number;
  netRevenue: number;
  averageCommissionRate: number;
  settledOrdersCount: number;
  pendingOrdersCount: number;
}

// Product-level commission summary
export interface ProductCommissionSummary {
  barcode: string;
  productName: string;
  image?: string;
  categoryName?: string;
  salesCount: number;
  totalSales: number;
  commissionRate: number;
  totalCommission: number;
  netRevenue: number;
}

// Financial sync response
export interface FinancialSyncResponse {
  success: boolean;
  message: string;
  storeId: string;
  recordsProcessed: number;
  recordsUpdated: number;
}

// Financial data response for a store
export interface FinancialDataResponse {
  summary: FinancialSummary;
  productCommissions: ProductCommissionSummary[];
  recentTransactions: FinancialSettlementItem[];
}

// ========================================
// VAT Reconciliation Types (KDV Mahsuplasmasi)
// ========================================

// Income breakdown for VAT reconciliation
export interface VatIncomeBreakdown {
  salesVat: number;       // Satislardan gelen KDV
  orderCount: number;     // Siparis sayisi
  totalSales: number;     // Toplam satis tutari
}

// Expense breakdown for VAT reconciliation
export interface VatExpenseBreakdown {
  commissionVat: number;       // Komisyon KDV'si
  commissionBase: number;      // Komisyon tutari (KDV haric)
  cargoVat: number;            // Kargo KDV'si
  cargoBase: number;           // Kargo tutari (KDV haric)
  adVat: number;               // Reklam KDV'si
  adBase: number;              // Reklam tutari (KDV haric)
  stoppageAmount: number;      // Stopaj kesintisi (KDV'siz)
  manualExpenseVat: number;    // Manuel gider KDV'si
  manualExpenseBase: number;   // Manuel gider tutari
  productCostVat: number;      // Urun maliyeti KDV'si
  productCostBase: number;     // Urun maliyeti tutari
}

// Full VAT breakdown
export interface VatBreakdown {
  income: VatIncomeBreakdown;
  expenses: VatExpenseBreakdown;
}

// VAT Reconciliation result
export interface VatReconciliationResult {
  incomeVat: number;       // Hesaplanan KDV (Gelir)
  expenseVat: number;      // Indirilecek KDV (Gider)
  netVat: number;          // Odenecek/Devreden KDV
  status: 'PAYABLE' | 'DEDUCTIBLE';  // Odenecek or Devreden
  breakdown: VatBreakdown;
  periodStart: string;
  periodEnd: string;
}

// ========================================
// Settlement Verification Types (Hak Edis Kontrolu)
// ========================================

// Settlement breakdown for a payment order
export interface SettlementBreakdown {
  saleAmount: number;
  saleCount: number;
  returnAmount: number;
  returnCount: number;
  discountAmount: number;
  couponAmount: number;
  commissionAmount: number;
  cargoAmount: number;
  stoppageAmount: number;
}

// Payment order verification status
export type PaymentOrderStatus = 'MATCHED' | 'UNDERPAID' | 'OVERPAID' | 'PENDING';

// Payment order verification result
export interface PaymentOrderVerification {
  paymentOrderId: number;
  paymentDate: string;
  expectedAmount: number;      // Calculated from settlements
  actualAmount: number;        // From PaymentOrder transaction
  discrepancy: number;         // expected - actual
  status: PaymentOrderStatus;
  settlementBreakdown: SettlementBreakdown;
}

// Verification statistics
export interface VerificationStats {
  totalPaymentOrders: number;
  matchedCount: number;
  underpaidCount: number;
  overpaidCount: number;
  pendingCount: number;
  totalAmount: number;
  totalDiscrepancy: number;
  periodStart: string;
  periodEnd: string;
}

// Settlement verification response
export interface SettlementVerificationResponse {
  verifications: PaymentOrderVerification[];
  total: number;
  periodStart: string;
  periodEnd: string;
}

// Discrepancies response
export interface DiscrepanciesResponse {
  discrepancies: PaymentOrderVerification[];
  total: number;
}

// Sync result for other financials
export interface OtherFinancialsSyncResult {
  stoppageCount: number;
  paymentOrderCount: number;
  cargoInvoiceCount: number;
  cashAdvanceCount: number;
  wireTransferCount: number;
  incomingTransferCount: number;
  commissionAgreementCount: number;
  financialItemCount: number;
  startDate: string;
  endDate: string;
}

// Full sync and verify response
export interface SyncAndVerifyResponse {
  syncResult: OtherFinancialsSyncResult;
  verificationSummary: {
    total: number;
    matched: number;
    underpaid: number;
    overpaid: number;
  };
  verifications: PaymentOrderVerification[];
  periodStart: string;
  periodEnd: string;
}

// Discrepancy alert for notifications
export interface DiscrepancyAlert {
  id: string;
  storeId: string;
  paymentOrderId: number;
  discrepancyAmount: number;
  status: PaymentOrderStatus;
  createdAt: string;
  isRead: boolean;
}

// ========================================
// Settlement Control Types (Hakediş Kontrol Tipleri)
// ========================================

// Control type selector options
export type ControlType = 'all' | 'cargo-overcharge' | 'return-refund' | 'sale-shortfall';

// Cargo Overcharge status
export type CargoOverchargeStatus = 'OVERCHARGED' | 'NORMAL';

// Return Commission Refund status
export type ReturnRefundStatus = 'MISSING_REFUND' | 'PARTIAL_REFUND' | 'REFUNDED';

// Sale Shortfall status
export type SaleShortfallStatus = 'SHORTFALL' | 'MATCHED';

// 1. Cargo Overcharge Detection result (Fazla Kargo Faturası)
export interface CargoOverchargeResult {
  cargoInvoiceId: string;
  invoiceSerialNumber: string;
  shipmentPackageId: number;
  orderNumber: string;
  invoiceDate: string;
  desi: number;
  expectedAmount: number;
  actualAmount: number;
  overchargeAmount: number;
  status: CargoOverchargeStatus;
  shipmentPackageType?: string;
  vatAmount?: number;
  vatRate?: number;
}

// 2. Return Commission Refund Detection result (Eksik İade Komisyon İadesi)
export interface ReturnCommissionRefundResult {
  paymentOrderId: number | null;
  orderNumber: string;
  packageNo: number;
  returnDate: string;
  returnAmount: number;
  commissionRate: number;
  expectedRefund: number;
  actualRefund: number;
  missingRefund: number;
  status: ReturnRefundStatus;
  returnCount: number;
  productName?: string;
  barcode?: string;
}

// 3. Sale Shortfall Detection result (Eksik Satış Bedeli)
export interface SaleShortfallResult {
  paymentOrderId: number;
  orderNumber: string | null;
  packageNo: number | null;
  saleDate: string;
  expectedSaleAmount: number;
  actualSaleAmount: number;
  shortfallAmount: number;
  status: SaleShortfallStatus;
  grossAmount: number;
  commissionAmount: number;
  cargoAmount: number;
  stoppageAmount: number;
  discountAmount: number;
  itemCount: number;
  productSummary?: string;
}

// Control Type Statistics
export interface ControlTypeStats {
  controlType: string;
  totalChecked: number;
  issuesFound: number;
  normalCount: number;
  totalIssueAmount: number;
  averageIssueAmount: number;
  maxIssueAmount: number;
  periodStart: string;
  periodEnd: string;
  issuePercentage: number;
}

// Response types for control type endpoints
export interface CargoOverchargeResponse {
  results: CargoOverchargeResult[];
  total: number;
  issuesFound: number;
  normalCount: number;
  periodStart: string;
  periodEnd: string;
}

export interface ReturnRefundResponse {
  results: ReturnCommissionRefundResult[];
  total: number;
  issuesFound: number;
  normalCount: number;
  periodStart: string;
  periodEnd: string;
}

export interface SaleShortfallResponse {
  results: SaleShortfallResult[];
  total: number;
  issuesFound: number;
  normalCount: number;
  periodStart: string;
  periodEnd: string;
}

export interface AllControlStatsResponse {
  stats: ControlTypeStats[];
  summary: {
    totalIssues: number;
    totalChecked: number;
  };
  periodStart: string;
  periodEnd: string;
}

// ========================================
// Invoice Dashboard Types (Fatura Tipi Dashboard)
// ========================================

// Invoice type stats
export interface InvoiceTypeStats {
  invoiceTypeCode: string;
  invoiceType: string;
  invoiceCategory: string;
  isDeduction: boolean;
  invoiceCount: number;
  totalAmount: number;
  totalVatAmount: number;
}

// Category summary
export interface InvoiceCategorySummary {
  category: string;
  invoiceCount: number;
  totalAmount: number;
  totalVatAmount: number;
}

// Invoice summary response
export interface InvoiceSummary {
  storeId: string;
  periodStart: string;
  periodEnd: string;
  totalDeductions: number;
  totalRefunds: number;
  netAmount: number;
  totalInvoiceCount: number;
  invoicesByType: InvoiceTypeStats[];
  invoicesByCategory: InvoiceCategorySummary[];
}

// Invoice detail for list display
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
  orderNumber?: string;
  shipmentPackageId?: number;
  paymentOrderId?: number;
  barcode?: string;
  productName?: string;
  desi?: number;
  description?: string;
  details?: Record<string, unknown>;
}

// Paginated invoice response
export interface InvoiceListResponse {
  content: InvoiceDetail[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Invoice sync response
export interface InvoiceSyncResponse {
  success: boolean;
  message: string;
  syncedCount: number;
  startDate: string;
  endDate: string;
}

// ========================================
// Cargo Invoice Item Types (Kargo Fatura Detayi)
// ========================================

// Individual cargo invoice item (shipment within a cargo invoice)
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
  cargoCompany?: string;
  createdAt?: string;
}

// Cargo items response (breakdown of a cargo invoice)
export interface CargoItemsResponse {
  invoiceSerialNumber: string;
  itemCount: number;
  totalAmount: number;
  totalVatAmount: number;
  items: CargoInvoiceItem[];
}
