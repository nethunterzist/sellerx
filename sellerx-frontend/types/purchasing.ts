export type PurchaseOrderStatus = 'DRAFT' | 'ORDERED' | 'SHIPPED' | 'CLOSED';

export interface PurchaseOrderItem {
  id: number;
  productId: string;
  productName: string;
  productBarcode: string;
  productImage?: string;
  unitsOrdered: number;
  unitsPerBox?: number;
  boxesOrdered?: number;
  boxDimensions?: string;
  manufacturingCostPerUnit: number;
  manufacturingCostSupplierCurrency?: number;
  transportationCostPerUnit: number;
  totalCostPerUnit: number;
  totalCost: number;
  hsCode?: string;
  labels?: string;
  stockEntryDate?: string;
  comment?: string;
}

export interface PurchaseOrder {
  id: number;
  poNumber: string;
  poDate: string;
  estimatedArrival?: string;
  stockEntryDate?: string;
  status: PurchaseOrderStatus;
  supplierName?: string;
  supplierId?: number;
  supplierCurrency?: string;
  exchangeRate?: number;
  parentPoId?: number;
  carrier?: string;
  trackingNumber?: string;
  comment?: string;
  transportationCost: number;
  totalCost: number;
  totalUnits: number;
  items: PurchaseOrderItem[];
  itemCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface PurchaseOrderSummary {
  id: number;
  poNumber: string;
  poDate: string;
  estimatedArrival?: string;
  stockEntryDate?: string;
  status: PurchaseOrderStatus;
  supplierName?: string;
  supplierId?: number;
  supplierCurrency?: string;
  parentPoId?: number;
  totalCost: number;
  totalUnits: number;
  itemCount: number;
}

export interface StatusStats {
  count: number;
  totalCost: number;
  totalUnits: number;
}

export interface PurchaseOrderStats {
  draft: StatusStats;
  ordered: StatusStats;
  shipped: StatusStats;
  closed: StatusStats;
}

export interface CreatePurchaseOrderRequest {
  poDate?: string;
  estimatedArrival?: string;
  stockEntryDate?: string;
  supplierName?: string;
  supplierId?: number;
  supplierCurrency?: string;
  exchangeRate?: number;
  carrier?: string;
  trackingNumber?: string;
  comment?: string;
}

export interface UpdatePurchaseOrderRequest {
  poDate?: string;
  estimatedArrival?: string;
  stockEntryDate?: string;
  supplierName?: string;
  supplierId?: number;
  supplierCurrency?: string;
  exchangeRate?: number;
  carrier?: string;
  trackingNumber?: string;
  comment?: string;
  transportationCost?: number;
}

export interface AddPurchaseOrderItemRequest {
  productId: string;
  unitsOrdered: number;
  unitsPerBox?: number;
  boxesOrdered?: number;
  boxDimensions?: string;
  manufacturingCostPerUnit: number;
  manufacturingCostSupplierCurrency?: number;
  transportationCostPerUnit?: number;
  hsCode?: string;
  labels?: string;
  stockEntryDate?: string;
  comment?: string;
}

// Attachment types
export interface Attachment {
  id: number;
  purchaseOrderId: number;
  fileName: string;
  fileType?: string;
  fileSize?: number;
  uploadedAt: string;
}

// Split PO request
export interface SplitPurchaseOrderRequest {
  itemIds: number[];
}

export interface UpdateStatusRequest {
  status: PurchaseOrderStatus;
}

// === Report Types ===

// Product Cost History
export interface CostEntry {
  stockDate: string;
  quantity: number;
  usedQuantity: number;
  remainingQuantity: number;
  unitCost: number;
  vatRate: number;
  totalValue: number;
  usagePercentage: number;
  purchaseOrderId?: number;
  purchaseOrderNumber?: string;
}

export interface ProductCostHistoryResponse {
  productId: string;
  productName: string;
  barcode: string;
  productImage?: string;
  entries: CostEntry[];
  averageCost: number;
  weightedAverageCost: number;
  totalValue: number;
  totalQuantity: number;
  remainingQuantity: number;
}

// FIFO Analysis
export interface OrderAllocation {
  orderNumber: string;
  orderDate: string;
  quantity: number;
  costPerUnit: number;
  salePrice: number;
  profit: number;
}

export interface FifoLot {
  stockDate: string;
  originalQuantity: number;
  usedQuantity: number;
  remainingQuantity: number;
  unitCost: number;
  vatRate: number;
  allocations: OrderAllocation[];
}

export interface FifoAnalysisResponse {
  barcode: string;
  productName: string;
  productImage?: string;
  lots: FifoLot[];
  totalCost: number;
  totalRevenue: number;
  totalProfit: number;
  profitMargin: number;
}

// Stock Valuation
export interface ProductValuation {
  productId: string;
  productName: string;
  barcode: string;
  productImage?: string;
  quantity: number;
  fifoValue: number;
  averageCost: number;
  oldestStockDate?: string;
  daysInStock: number;
  stockDepleted?: boolean;
}

export interface AgingBreakdown {
  days0to30: number;
  days30to60: number;
  days60to90: number;
  days90plus: number;
}

export interface StockValuationResponse {
  products: ProductValuation[];
  totalValue: number;
  totalQuantity: number;
  aging: AgingBreakdown;
}

// Profitability Analysis
export interface ProductProfitability {
  productId?: string;
  productName: string;
  barcode: string;
  productImage?: string;
  quantitySold: number;
  revenue: number;
  cost: number;
  profit: number;
  margin: number;
  costEstimated?: boolean;
}

export interface DailyProfit {
  date: string;
  revenue: number;
  cost: number;
  profit: number;
  margin: number;
  orderCount: number;
}

export interface ProfitabilityResponse {
  startDate: string;
  endDate: string;
  totalRevenue: number;
  totalCost: number;
  grossProfit: number;
  grossMargin: number;
  totalOrders: number;
  totalQuantitySold: number;
  topProfitable: ProductProfitability[];
  leastProfitable: ProductProfitability[];
  dailyTrend: DailyProfit[];
}

// Purchase Summary
export interface SupplierBreakdown {
  supplierName: string;
  totalAmount: number;
  orderCount: number;
  totalUnits: number;
  percentage: number;
}

export interface ProductPurchase {
  productId?: string;
  productName: string;
  barcode: string;
  productImage?: string;
  totalUnits: number;
  totalAmount: number;
  averageCost: number;
  costChange: number;
}

export interface MonthlyPurchase {
  year: number;
  month: number;
  monthName: string;
  totalAmount: number;
  totalUnits: number;
  orderCount: number;
}

export interface PurchaseSummaryResponse {
  startDate: string;
  endDate: string;
  totalPurchaseAmount: number;
  totalUnits: number;
  totalOrders: number;
  averageCostPerUnit: number;
  supplierBreakdown: SupplierBreakdown[];
  topProductsByAmount: ProductPurchase[];
  monthlyTrend: MonthlyPurchase[];
}

// Stock Depletion
export interface DepletedProduct {
  productId: string;
  productName: string;
  barcode: string;
  productImage?: string;
  lastStockDate?: string;
}
