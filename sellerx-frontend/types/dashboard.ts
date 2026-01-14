// Dashboard types matching backend DTOs
import type { ExpenseFrequency } from "./expense";

export type PeriodType = "today" | "yesterday" | "thisMonth" | "lastMonth";

// Re-export for convenience
export type { ExpenseFrequency };

// Backend: OrderProductDetailDto
export interface OrderProductDetail {
  barcode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  cost: number;
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
}

// Backend: ProductDetailDto
export interface ProductDetail {
  productName: string;
  barcode: string;
  image?: string; // ürün görseli URL
  stock: number; // stok adedi
  totalSoldQuantity: number;
  returnQuantity: number;
  revenue: number;
  grossProfit: number;
  estimatedCommission: number;
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
  totalRevenue: number;
  returnCount: number;
  returnCost: number;
  totalProductCosts: number;
  grossProfit: number;
  vatDifference: number;
  totalStoppage: number;
  totalEstimatedCommission: number;
  itemsWithoutCost: number;
  totalExpenseNumber: number;
  totalExpenseAmount: number;
  orders: OrderDetail[];
  products: ProductDetail[];
  expenses: PeriodExpense[];
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
