import { useQuery } from "@tanstack/react-query";
import { apiRequest, financialApi, type FinancialStats } from "@/lib/api/client";
import type {
  InvoiceSummary,
  InvoiceListResponse,
  CargoItemsResponse,
} from "@/types/financial";

// Financial Query Keys
export const financialKeys = {
  all: ["financial"] as const,
  stats: () => [...financialKeys.all, "stats"] as const,
  storeStats: (storeId: string) => [...financialKeys.stats(), storeId] as const,
  sync: (storeId: string) => [...financialKeys.all, "sync", storeId] as const,
};

// Get financial stats for a store
export function useFinancialStats(storeId: string | undefined) {
  return useQuery({
    queryKey: financialKeys.storeStats(storeId || ""),
    queryFn: () => financialApi.getStats(storeId!),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

// ========================================
// Invoice Dashboard Hooks (Fatura Tipi Dashboard)
// ========================================

// Invoice Query Keys
export const invoiceKeys = {
  all: ["invoices"] as const,
  summary: (storeId: string, startDate: string, endDate: string) =>
    [...invoiceKeys.all, "summary", storeId, startDate, endDate] as const,
  byType: (storeId: string, typeCode: string, startDate: string, endDate: string, page: number, size: number) =>
    [...invoiceKeys.all, "by-type", storeId, typeCode, startDate, endDate, page, size] as const,
  byCategory: (storeId: string, category: string, startDate: string, endDate: string, page: number, size: number) =>
    [...invoiceKeys.all, "by-category", storeId, category, startDate, endDate, page, size] as const,
  list: (storeId: string, startDate: string, endDate: string, page: number, size: number) =>
    [...invoiceKeys.all, "list", storeId, startDate, endDate, page, size] as const,
  cargoItems: (storeId: string, invoiceSerialNumber: string) =>
    [...invoiceKeys.all, "cargo-items", storeId, invoiceSerialNumber] as const,
};

/**
 * Hook to get invoice summary for a store
 */
export function useInvoiceSummary(
  storeId: string | undefined,
  startDate: string,
  endDate: string,
  enabled = true
) {
  return useQuery<InvoiceSummary>({
    queryKey: invoiceKeys.summary(storeId || "", startDate, endDate),
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
 * Hook to get invoices by type code
 */
export function useInvoicesByType(
  storeId: string | undefined,
  typeCode: string,
  startDate: string,
  endDate: string,
  page = 0,
  size = 20,
  enabled = true
) {
  return useQuery<InvoiceListResponse>({
    queryKey: invoiceKeys.byType(storeId || "", typeCode, startDate, endDate, page, size),
    queryFn: () =>
      apiRequest<InvoiceListResponse>(
        `/invoices/stores/${storeId}/by-type/${typeCode}?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
      ),
    enabled: enabled && !!storeId && !!typeCode && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Hook to get invoices by category
 */
export function useInvoicesByCategory(
  storeId: string | undefined,
  category: string,
  startDate: string,
  endDate: string,
  page = 0,
  size = 20,
  enabled = true
) {
  return useQuery<InvoiceListResponse>({
    queryKey: invoiceKeys.byCategory(storeId || "", category, startDate, endDate, page, size),
    queryFn: () =>
      apiRequest<InvoiceListResponse>(
        `/invoices/stores/${storeId}/by-category/${category}?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
      ),
    enabled: enabled && !!storeId && !!category && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Hook to get all invoices with pagination
 */
export function useInvoicesList(
  storeId: string | undefined,
  startDate: string,
  endDate: string,
  page = 0,
  size = 20,
  enabled = true
) {
  return useQuery<InvoiceListResponse>({
    queryKey: invoiceKeys.list(storeId || "", startDate, endDate, page, size),
    queryFn: () =>
      apiRequest<InvoiceListResponse>(
        `/invoices/stores/${storeId}?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
      ),
    enabled: enabled && !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: false,
  });
}

/**
 * Hook to get cargo invoice items (breakdown of a cargo invoice)
 * Returns all shipments within a specific cargo invoice serial number
 */
export function useCargoInvoiceItems(
  storeId: string | undefined,
  invoiceSerialNumber: string | undefined,
  enabled = true
) {
  return useQuery<CargoItemsResponse>({
    queryKey: invoiceKeys.cargoItems(storeId || "", invoiceSerialNumber || ""),
    queryFn: () =>
      apiRequest<CargoItemsResponse>(
        `/invoices/stores/${storeId}/cargo-items/${invoiceSerialNumber}`
      ),
    enabled: enabled && !!storeId && !!invoiceSerialNumber,
    staleTime: 10 * 60 * 1000, // 10 minutes (cargo invoice data doesn't change often)
    refetchOnWindowFocus: false,
  });
}
