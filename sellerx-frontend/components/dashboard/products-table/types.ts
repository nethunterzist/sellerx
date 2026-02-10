/**
 * Types for ProductsTable component
 * Extracted from products-table.tsx for better modularity
 */

export interface ProductStatus {
  onSale: boolean;
  approved: boolean;
  hasActiveCampaign: boolean;
  archived: boolean;
  blacklisted: boolean;
  rejected?: boolean;
}

export interface CostHistoryItem {
  stockDate: string;
  quantity: number;
  unitCost: number;
  costVatRate?: number;
  usedQuantity?: number;
}

export interface Product {
  id: string;
  name: string;
  sku: string;
  image?: string;
  cogs: number;
  stock: number;
  marketplace: "trendyol" | "hepsiburada";
  unitsSold: number;
  refunds: number;
  sales: number;
  grossProfit: number;
  netProfit: number;
  margin: number;
  roi: number;
  commission?: number;

  // Discounts & Coupons
  sellerDiscount?: number;
  platformDiscount?: number;
  couponDiscount?: number;
  totalDiscount?: number;

  // Net Revenue
  netRevenue?: number;

  // Costs
  productCost?: number;
  shippingCost?: number;
  refundCost?: number;

  // Rates
  refundRate?: number;
  profitMargin?: number;

  // Return details
  returnQuantity?: number;

  // Additional data from TrendyolProduct
  categoryName?: string;

  // Advertising metrics
  cpc?: number; // Cost Per Click (TL)
  cvr?: number; // Conversion Rate (e.g., 0.018 = 1.8%)
  advertisingCostPerSale?: number; // Ad Cost = CPC / CVR
  acos?: number; // ACOS = (advertisingCostPerSale / salePrice) * 100
  totalAdvertisingCost?: number; // Total advertising cost
  brand?: string;
  salePrice?: number;
  vatRate?: number;
  commissionRate?: number;
  trendyolQuantity?: number;
  productUrl?: string;
  status?: ProductStatus;
  costHistory?: CostHistoryItem[];
}

export interface ProductsTableProps {
  products?: Product[];
  orders?: import("@/types/dashboard").OrderDetail[];
  isLoading?: boolean;
  startDate?: string; // ISO date for invoice queries
  endDate?: string; // ISO date for invoice queries
}

export interface ColumnConfig {
  id: string;
  label: string;
  defaultVisible: boolean;
  alwaysVisible?: boolean;
}

export type SortField =
  | "name"
  | "unitsSold"
  | "sales"
  | "grossProfit"
  | "netProfit"
  | "margin"
  | "roi"
  | "stock"
  | "refunds"
  | "commission"
  | "acos";

export type OrderSortField =
  | "orderDate"
  | "totalPrice"
  | "profit"
  | "margin"
  | "roi";

export type SortDirection = "asc" | "desc";

export interface OrderItemWithDetails {
  orderNumber: string;
  orderDate: string;
  quantity: number;
  productName: string;
  barcode: string;
  productUrl?: string;
  totalPrice: number;
  cost: number;
  commission: number;
  profit: number;
}
