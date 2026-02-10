import type { DashboardStats, DashboardStatsResponse, ProductDetail } from "@/types/dashboard";

/**
 * Filters DashboardStats by selected product barcodes and recalculates totals.
 *
 * When products are filtered:
 * - Product-based metrics are recalculated from filtered products
 * - Store-level metrics (invoicedDeductions, expenses, stoppage) are set to 0
 *   because they cannot be attributed to specific products
 */
export function filterStatsByProducts(
  stats: DashboardStats | undefined,
  selectedBarcodes: string[]
): DashboardStats | undefined {
  if (!stats) return undefined;
  if (selectedBarcodes.length === 0) return stats; // No filter = return original

  // Filter products by barcode
  const filteredProducts = stats.products?.filter((p) =>
    selectedBarcodes.includes(p.barcode)
  ) || [];

  // If no products match, return stats with zeroed values
  if (filteredProducts.length === 0) {
    return createEmptyStats(stats);
  }

  // Recalculate totals from filtered products
  const totals = calculateTotalsFromProducts(filteredProducts);

  return {
    ...stats,
    products: filteredProducts,
    // Recalculated product-based totals
    totalOrders: totals.totalOrders,
    totalProductsSold: totals.totalProductsSold,
    netUnitsSold: totals.totalProductsSold - totals.returnCount,
    totalRevenue: totals.totalRevenue,
    returnCount: totals.returnCount,
    returnCost: totals.returnCost,
    totalProductCosts: totals.totalProductCosts,
    grossProfit: totals.grossProfit,
    netProfit: totals.netProfit,
    totalEstimatedCommission: totals.totalEstimatedCommission,
    totalShippingCost: totals.totalShippingCost,
    itemsWithoutCost: totals.itemsWithoutCost,
    // Recalculated derived metrics
    profitMargin: totals.totalRevenue > 0
      ? (totals.grossProfit / totals.totalRevenue) * 100
      : 0,
    roi: totals.totalProductCosts > 0
      ? (totals.netProfit / totals.totalProductCosts) * 100
      : 0,
    refundRate: totals.totalProductsSold > 0
      ? (totals.returnCount / totals.totalProductsSold) * 100
      : 0,
    // Discounts
    totalSellerDiscount: totals.totalSellerDiscount,
    totalPlatformDiscount: totals.totalPlatformDiscount,
    totalCouponDiscount: totals.totalCouponDiscount,
    netRevenue: totals.netRevenue,
    // Store-level metrics - set to 0 when filtering by products
    // These cannot be attributed to specific products
    invoicedDeductions: 0,
    invoicedAdvertisingFees: 0,
    invoicedPenaltyFees: 0,
    invoicedInternationalFees: 0,
    invoicedOtherFees: 0,
    invoicedRefunds: 0,
    totalExpenseNumber: 0,
    totalExpenseAmount: 0,
    totalStoppage: 0,
    vatDifference: 0,
    // Keep orders as-is (filtering orders by product would require additional logic)
    orders: stats.orders,
    // Clear expenses since they're store-level
    expenses: [],
    expensesByCategory: {},
  };
}

/**
 * Filters all periods in DashboardStatsResponse by selected product barcodes
 */
export function filterStatsResponseByProducts(
  response: DashboardStatsResponse | undefined,
  selectedBarcodes: string[]
): DashboardStatsResponse | undefined {
  if (!response) return undefined;
  if (selectedBarcodes.length === 0) return response;

  return {
    ...response,
    today: filterStatsByProducts(response.today, selectedBarcodes)!,
    yesterday: filterStatsByProducts(response.yesterday, selectedBarcodes)!,
    thisMonth: filterStatsByProducts(response.thisMonth, selectedBarcodes)!,
    lastMonth: filterStatsByProducts(response.lastMonth, selectedBarcodes)!,
  };
}

/**
 * Calculates totals from an array of ProductDetail
 */
function calculateTotalsFromProducts(products: ProductDetail[]) {
  let totalOrders = 0;
  let totalProductsSold = 0;
  let totalRevenue = 0;
  let returnCount = 0;
  let returnCost = 0;
  let totalProductCosts = 0;
  let grossProfit = 0;
  let netProfit = 0;
  let totalEstimatedCommission = 0;
  let totalShippingCost = 0;
  let itemsWithoutCost = 0;
  let totalSellerDiscount = 0;
  let totalPlatformDiscount = 0;
  let totalCouponDiscount = 0;

  for (const p of products) {
    totalOrders += p.orderCount ?? 0;
    totalProductsSold += p.totalSoldQuantity ?? 0;
    totalRevenue += p.revenue ?? 0;
    returnCount += p.returnQuantity ?? 0;
    returnCost += p.refundCost ?? 0;
    totalProductCosts += p.productCost ?? 0;
    grossProfit += p.grossProfit ?? 0;
    netProfit += p.netProfit ?? 0;
    totalEstimatedCommission += p.estimatedCommission ?? 0;
    totalShippingCost += p.shippingCost ?? 0;
    totalSellerDiscount += p.sellerDiscount ?? 0;
    totalPlatformDiscount += p.platformDiscount ?? 0;
    totalCouponDiscount += p.couponDiscount ?? 0;

    // Count items without cost
    if (!p.productCost || p.productCost === 0) {
      itemsWithoutCost += p.totalSoldQuantity ?? 0;
    }
  }

  const netRevenue = totalRevenue - totalSellerDiscount - totalPlatformDiscount - totalCouponDiscount;

  return {
    totalOrders,
    totalProductsSold,
    totalRevenue,
    returnCount,
    returnCost,
    totalProductCosts,
    grossProfit,
    netProfit,
    totalEstimatedCommission,
    totalShippingCost,
    itemsWithoutCost,
    totalSellerDiscount,
    totalPlatformDiscount,
    totalCouponDiscount,
    netRevenue,
  };
}

/**
 * Creates stats with zero values but preserves the period
 */
function createEmptyStats(originalStats: DashboardStats): DashboardStats {
  return {
    ...originalStats,
    products: [],
    totalOrders: 0,
    totalProductsSold: 0,
    netUnitsSold: 0,
    totalRevenue: 0,
    returnCount: 0,
    returnCost: 0,
    totalProductCosts: 0,
    grossProfit: 0,
    netProfit: 0,
    profitMargin: 0,
    vatDifference: 0,
    totalStoppage: 0,
    totalEstimatedCommission: 0,
    itemsWithoutCost: 0,
    totalExpenseNumber: 0,
    totalExpenseAmount: 0,
    totalShippingCost: 0,
    invoicedDeductions: 0,
    totalSellerDiscount: 0,
    totalPlatformDiscount: 0,
    totalCouponDiscount: 0,
    netRevenue: 0,
    roi: 0,
    refundRate: 0,
    orders: originalStats.orders, // Keep orders for reference
    expenses: [],
    expensesByCategory: {},
  };
}
