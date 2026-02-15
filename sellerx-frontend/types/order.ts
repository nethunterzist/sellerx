// Order types matching backend DTOs

// Backend: OrderItemDto
export interface OrderItem {
  barcode: string;
  productName: string;
  quantity: number;
  unitPriceOrder: number;
  unitPriceDiscount: number;
  unitPriceTyDiscount: number;
  vatBaseAmount: number;
  price: number;
  cost: number;
  costVat: number;
  stockDate: string; // ISO date string
  estimatedCommissionRate: number;
  unitEstimatedCommission: number;
}

// Commission source type
// INVOICE: From Financial/Settlement API (lastCommissionRate) - most accurate
// REFERENCE: From Product API (commissionRate) - category default
// NONE: No commission data available
// MANUAL: Manually entered (legacy)
export type CommissionSource = "INVOICE" | "REFERENCE" | "NONE" | "MANUAL";

// Backend: TrendyolOrderDto
export interface TrendyolOrder {
  id: string; // UUID
  storeId: string;
  tyOrderNumber: string;
  packageNo: number;
  orderDate: string; // ISO datetime
  grossAmount: number;
  totalDiscount: number;
  totalTyDiscount: number;
  totalPrice: number;
  stoppage: number;
  estimatedCommission: number;
  estimatedShippingCost: number;
  isCommissionEstimated: boolean; // true = tahmini, false = gerçek (settlement geldi)
  isShippingEstimated?: boolean;
  returnShippingCost?: number; // İade kargo maliyeti (gerçek veya tahmini)
  isReturnShippingEstimated?: boolean; // true = gönderi kargosundan tahmin, false = gerçek iade faturası
  transactionStatus?: string; // "SETTLED" when settled
  platformServiceFee?: number; // Platform hizmet bedeli from deduction invoices
  commissionSource?: CommissionSource; // Where commission data comes from
  orderItems: OrderItem[];
  shipmentPackageStatus: string;
  status: string;
  cargoDeci: number;
  createdAt: string; // ISO datetime
  updatedAt: string; // ISO datetime
}

// Order status types
export type OrderStatus =
  | "Created"
  | "Picking"
  | "Invoiced"
  | "Shipped"
  | "Delivered"
  | "Cancelled"
  | "UnDelivered"
  | "Returned";

export type ShipmentStatus =
  | "Created"
  | "Picking"
  | "Invoiced"
  | "Shipped"
  | "Delivered"
  | "UnDelivered"
  | "Returned"
  | "Repack"
  | "UnDeliveredAndReturned"
  | "UnSupplied"
  | "Cancelled";

// Request types for order operations
export interface OrderFilters {
  status?: OrderStatus;
  shipmentStatus?: ShipmentStatus;
  startDate?: string;
  endDate?: string;
  search?: string;
}

// Sync response
export interface SyncOrdersResponse {
  success: boolean;
  message: string;
  totalFetched: number;
  totalSaved: number;
  totalUpdated: number;
  totalSkipped: number;
}

// Order statistics response from backend
export interface OrderStatistics {
  totalOrders: number;
  pendingOrders: number;
  shippedOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  returnedOrders: number;
  totalRevenue: number;
  averageOrderValue: number;
}

// Status labels for UI
export const orderStatusLabels: Record<OrderStatus, string> = {
  Created: "Oluşturuldu",
  Picking: "Hazırlanıyor",
  Invoiced: "Faturalı",
  Shipped: "Kargoda",
  Delivered: "Teslim Edildi",
  Cancelled: "İptal",
  UnDelivered: "Teslim Edilemedi",
  Returned: "İade",
};
