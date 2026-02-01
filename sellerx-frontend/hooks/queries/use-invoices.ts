import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/api/client";
import type {
  InvoiceSummary,
  InvoicePage,
  InvoiceSyncResponse,
  InvoiceFilters,
  CargoInvoiceItemsResponse,
  InvoiceItemsResponse,
  CommissionInvoiceItemsResponse,
  CategoryCargoItemsPage,
  CategoryCommissionItemsPage,
  AggregatedProductsResponse,
  ProductCommissionBreakdown,
  ProductCargoBreakdown,
} from "@/types/invoice";

// Re-export types for convenience
export type {
  CargoInvoiceItemsResponse,
  InvoiceItemsResponse,
  CommissionInvoiceItemsResponse,
  CategoryCargoItemsPage,
  CategoryCommissionItemsPage,
  AggregatedProductsResponse,
  ProductCommissionBreakdown,
  ProductCargoBreakdown,
};

// Invoice Query Keys
export const invoiceKeys = {
  all: ["invoices"] as const,
  summary: () => [...invoiceKeys.all, "summary"] as const,
  storeSummary: (storeId: string, startDate: string, endDate: string) =>
    [...invoiceKeys.summary(), storeId, startDate, endDate] as const,
  byType: () => [...invoiceKeys.all, "by-type"] as const,
  storeByType: (
    storeId: string,
    typeCode: string,
    startDate: string,
    endDate: string,
    page: number,
    size: number
  ) =>
    [...invoiceKeys.byType(), storeId, typeCode, startDate, endDate, page, size] as const,
  byCategory: () => [...invoiceKeys.all, "by-category"] as const,
  storeByCategory: (
    storeId: string,
    category: string,
    startDate: string,
    endDate: string,
    page: number,
    size: number
  ) =>
    [...invoiceKeys.byCategory(), storeId, category, startDate, endDate, page, size] as const,
  allInvoices: () => [...invoiceKeys.all, "list"] as const,
  storeAllInvoices: (
    storeId: string,
    startDate: string,
    endDate: string,
    page: number,
    size: number
  ) =>
    [...invoiceKeys.allInvoices(), storeId, startDate, endDate, page, size] as const,
  sync: (storeId: string) => [...invoiceKeys.all, "sync", storeId] as const,
  cargoItems: () => [...invoiceKeys.all, "cargo-items"] as const,
  storeCargoItems: (storeId: string, invoiceSerialNumber: string) =>
    [...invoiceKeys.cargoItems(), storeId, invoiceSerialNumber] as const,
  invoiceItems: () => [...invoiceKeys.all, "items"] as const,
  storeInvoiceItems: (storeId: string, invoiceSerialNumber: string) =>
    [...invoiceKeys.invoiceItems(), storeId, invoiceSerialNumber] as const,
  commissionItems: () => [...invoiceKeys.all, "commission-items"] as const,
  storeCommissionItems: (storeId: string, invoiceSerialNumber: string) =>
    [...invoiceKeys.commissionItems(), storeId, invoiceSerialNumber] as const,
  // Category-level queries (all items in date range, not per invoice)
  categoryCargoItems: () => [...invoiceKeys.all, "category-cargo-items"] as const,
  storeCategoryCargoItems: (
    storeId: string,
    startDate: string,
    endDate: string,
    page: number,
    size: number
  ) =>
    [...invoiceKeys.categoryCargoItems(), storeId, startDate, endDate, page, size] as const,
  categoryCommissionItems: () => [...invoiceKeys.all, "category-commission-items"] as const,
  storeCategoryCommissionItems: (
    storeId: string,
    startDate: string,
    endDate: string,
    page: number,
    size: number
  ) =>
    [...invoiceKeys.categoryCommissionItems(), storeId, startDate, endDate, page, size] as const,
  aggregatedProducts: () => [...invoiceKeys.all, "aggregated-products"] as const,
  storeAggregatedProducts: (
    storeId: string,
    category: string,
    startDate: string,
    endDate: string
  ) =>
    [...invoiceKeys.aggregatedProducts(), storeId, category, startDate, endDate] as const,
  productCommissionBreakdown: () => [...invoiceKeys.all, "product-commission-breakdown"] as const,
  storeProductCommissionBreakdown: (
    storeId: string,
    barcode: string,
    startDate: string,
    endDate: string
  ) =>
    [...invoiceKeys.productCommissionBreakdown(), storeId, barcode, startDate, endDate] as const,
  productCargoBreakdown: () => [...invoiceKeys.all, "product-cargo-breakdown"] as const,
  storeProductCargoBreakdown: (
    storeId: string,
    barcode: string,
    startDate: string,
    endDate: string
  ) =>
    [...invoiceKeys.productCargoBreakdown(), storeId, barcode, startDate, endDate] as const,
};

/**
 * Get invoice summary for a store
 * Returns all invoice types with totals and counts
 */
export function useInvoiceSummary(
  storeId: string | undefined,
  startDate: string,
  endDate: string,
  enabled = true
) {
  return useQuery<InvoiceSummary>({
    queryKey: invoiceKeys.storeSummary(storeId || "", startDate, endDate),
    queryFn: () =>
      apiRequest<InvoiceSummary>(
        `/invoices/stores/${storeId}/summary?startDate=${startDate}&endDate=${endDate}`
      ),
    enabled: enabled && !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Get invoices by type code with pagination
 */
export function useInvoicesByType(
  storeId: string | undefined,
  typeCode: string | undefined,
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20,
  enabled = true
) {
  return useQuery<InvoicePage>({
    queryKey: invoiceKeys.storeByType(
      storeId || "",
      typeCode || "",
      startDate,
      endDate,
      page,
      size
    ),
    queryFn: () =>
      apiRequest<InvoicePage>(
        `/invoices/stores/${storeId}/by-type/${typeCode}?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
      ),
    enabled: enabled && !!storeId && !!typeCode && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Get invoices by category with pagination
 */
export function useInvoicesByCategory(
  storeId: string | undefined,
  category: string | undefined,
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20,
  enabled = true
) {
  return useQuery<InvoicePage>({
    queryKey: invoiceKeys.storeByCategory(
      storeId || "",
      category || "",
      startDate,
      endDate,
      page,
      size
    ),
    queryFn: () =>
      apiRequest<InvoicePage>(
        `/invoices/stores/${storeId}/by-category/${category}?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
      ),
    enabled: enabled && !!storeId && !!category && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Get all invoices with pagination
 */
export function useAllInvoices(
  storeId: string | undefined,
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20,
  enabled = true
) {
  return useQuery<InvoicePage>({
    queryKey: invoiceKeys.storeAllInvoices(storeId || "", startDate, endDate, page, size),
    queryFn: () =>
      apiRequest<InvoicePage>(
        `/invoices/stores/${storeId}?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
      ),
    enabled: enabled && !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Sync invoices from Trendyol API
 */
export function useSyncInvoices() {
  const queryClient = useQueryClient();

  return useMutation<
    InvoiceSyncResponse,
    Error,
    { storeId: string; startDate?: string; endDate?: string }
  >({
    mutationFn: ({ storeId, startDate, endDate }) => {
      const params = new URLSearchParams();
      if (startDate) params.append("startDate", startDate);
      if (endDate) params.append("endDate", endDate);
      const queryString = params.toString();
      const url = `/invoices/stores/${storeId}/sync${queryString ? `?${queryString}` : ""}`;
      return apiRequest<InvoiceSyncResponse>(url, { method: "POST" });
    },
    onSuccess: (data, { storeId }) => {
      // Invalidate all invoice-related queries for this store
      queryClient.invalidateQueries({ queryKey: invoiceKeys.all });
    },
  });
}

/**
 * Helper hook to get invoices with filters
 */
export function useInvoicesWithFilters(
  storeId: string | undefined,
  filters: InvoiceFilters,
  enabled = true
) {
  const { startDate = "", endDate = "", typeCode, category, page = 0, size = 20 } = filters;

  // Determine which query to use based on filters
  if (typeCode) {
    return useInvoicesByType(storeId, typeCode, startDate, endDate, page, size, enabled);
  }

  if (category) {
    return useInvoicesByCategory(storeId, category, startDate, endDate, page, size, enabled);
  }

  return useAllInvoices(storeId, startDate, endDate, page, size, enabled);
}

/**
 * Get cargo invoice items for a specific invoice serial number
 * Shows all shipments within a cargo invoice (Kargo Fatura breakdown)
 */
export function useCargoInvoiceItems(
  storeId: string | undefined,
  invoiceSerialNumber: string | undefined,
  enabled = true
) {
  return useQuery<CargoInvoiceItemsResponse>({
    queryKey: invoiceKeys.storeCargoItems(storeId || "", invoiceSerialNumber || ""),
    queryFn: () =>
      apiRequest<CargoInvoiceItemsResponse>(
        `/invoices/stores/${storeId}/cargo-items/${invoiceSerialNumber}`
      ),
    enabled: enabled && !!storeId && !!invoiceSerialNumber,
    staleTime: 10 * 60 * 1000, // 10 minutes (cargo items don't change often)
    refetchOnWindowFocus: false,
  });
}

/**
 * Get generic invoice items for a specific invoice serial number
 * Shows all orders/items within an invoice (for CEZA, KOMISYON, etc. types)
 */
export function useInvoiceItems(
  storeId: string | undefined,
  invoiceSerialNumber: string | undefined,
  enabled = true
) {
  return useQuery<InvoiceItemsResponse>({
    queryKey: invoiceKeys.storeInvoiceItems(storeId || "", invoiceSerialNumber || ""),
    queryFn: () =>
      apiRequest<InvoiceItemsResponse>(
        `/invoices/stores/${storeId}/items/${invoiceSerialNumber}`
      ),
    enabled: enabled && !!storeId && !!invoiceSerialNumber,
    staleTime: 10 * 60 * 1000, // 10 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Get commission invoice items for a specific invoice serial number
 * Shows all orders within a commission invoice with order-level commission breakdown
 * (Similar to Trendyol Excel export)
 */
export function useCommissionInvoiceItems(
  storeId: string | undefined,
  invoiceSerialNumber: string | undefined,
  enabled = true
) {
  return useQuery<CommissionInvoiceItemsResponse>({
    queryKey: invoiceKeys.storeCommissionItems(storeId || "", invoiceSerialNumber || ""),
    queryFn: () =>
      apiRequest<CommissionInvoiceItemsResponse>(
        `/invoices/stores/${storeId}/commission-items/${invoiceSerialNumber}`
      ),
    enabled: enabled && !!storeId && !!invoiceSerialNumber,
    staleTime: 10 * 60 * 1000, // 10 minutes (commission data doesn't change often)
    refetchOnWindowFocus: false,
  });
}

// ========================================
// Category-Level Hooks (All items in date range, not per invoice)
// Used for "Fatura Kalemleri" and "Ürünler" tabs in category view
// ========================================

/**
 * Get all cargo invoice items by date range (category-level)
 * Shows all cargo shipments in a date range for the "Fatura Kalemleri" tab
 */
export function useCategoryCargoItems(
  storeId: string | undefined,
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20,
  enabled = true
) {
  return useQuery<CategoryCargoItemsPage>({
    queryKey: invoiceKeys.storeCategoryCargoItems(
      storeId || "",
      startDate,
      endDate,
      page,
      size
    ),
    queryFn: () =>
      apiRequest<CategoryCargoItemsPage>(
        `/invoices/stores/${storeId}/category/cargo-items?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
      ),
    enabled: enabled && !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Get all commission invoice items by date range (category-level)
 * Shows all commission deductions in a date range for the "Fatura Kalemleri" tab
 */
export function useCategoryCommissionItems(
  storeId: string | undefined,
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20,
  enabled = true
) {
  return useQuery<CategoryCommissionItemsPage>({
    queryKey: invoiceKeys.storeCategoryCommissionItems(
      storeId || "",
      startDate,
      endDate,
      page,
      size
    ),
    queryFn: () =>
      apiRequest<CategoryCommissionItemsPage>(
        `/invoices/stores/${storeId}/category/commission-items?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
      ),
    enabled: enabled && !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Get aggregated products by category and date range
 * Shows products grouped by barcode for the "Ürünler" tab
 */
export function useAggregatedProducts(
  storeId: string | undefined,
  category: "KARGO" | "KOMISYON",
  startDate: string,
  endDate: string,
  enabled = true
) {
  return useQuery<AggregatedProductsResponse>({
    queryKey: invoiceKeys.storeAggregatedProducts(
      storeId || "",
      category,
      startDate,
      endDate
    ),
    queryFn: () =>
      apiRequest<AggregatedProductsResponse>(
        `/invoices/stores/${storeId}/category/${category}/products?startDate=${startDate}&endDate=${endDate}`
      ),
    enabled: enabled && !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Get commission breakdown by transaction type for a specific product
 * Shows breakdown of Sale, Coupon, Discount for "Detay" panel in KOMISYON Ürünler tab
 */
export function useProductCommissionBreakdown(
  storeId: string | undefined,
  barcode: string | undefined,
  startDate: string,
  endDate: string,
  enabled = true
) {
  return useQuery<ProductCommissionBreakdown>({
    queryKey: invoiceKeys.storeProductCommissionBreakdown(
      storeId || "",
      barcode || "",
      startDate,
      endDate
    ),
    queryFn: () =>
      apiRequest<ProductCommissionBreakdown>(
        `/invoices/stores/${storeId}/products/${encodeURIComponent(barcode || "")}/commission-breakdown?startDate=${startDate}&endDate=${endDate}`
      ),
    enabled: enabled && !!storeId && !!barcode && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Get cargo breakdown by shipment for a specific product
 * Shows breakdown of cargo costs for "Detay" panel in KARGO Ürünler tab
 */
export function useProductCargoBreakdown(
  storeId: string | undefined,
  barcode: string | undefined,
  startDate: string,
  endDate: string,
  enabled = true
) {
  return useQuery<ProductCargoBreakdown>({
    queryKey: invoiceKeys.storeProductCargoBreakdown(
      storeId || "",
      barcode || "",
      startDate,
      endDate
    ),
    queryFn: () =>
      apiRequest<ProductCargoBreakdown>(
        `/invoices/stores/${storeId}/products/${encodeURIComponent(barcode || "")}/cargo-breakdown?startDate=${startDate}&endDate=${endDate}`
      ),
    enabled: enabled && !!storeId && !!barcode && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}
