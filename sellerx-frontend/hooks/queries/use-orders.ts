import { useQuery } from "@tanstack/react-query";
import { ordersApi, apiRequest } from "@/lib/api/client";
import type { TrendyolOrder, OrderStatistics, OrderStatus } from "@/types/order";
import type { PagedResponse } from "@/types/api";

// Order Query Keys
export const orderKeys = {
  all: ["orders"] as const,
  byStore: (storeId: string) => [...orderKeys.all, "store", storeId] as const,
  byStorePaginated: (storeId: string, page: number, size: number) =>
    [...orderKeys.all, "store", storeId, "paginated", { page, size }] as const,
  byDateRange: (storeId: string, startDate: string, endDate: string) =>
    [...orderKeys.all, "dateRange", storeId, startDate, endDate] as const,
  byDateRangePaginated: (storeId: string, startDate: string, endDate: string, page: number, size: number) =>
    [...orderKeys.all, "dateRange", storeId, startDate, endDate, "page", page, "size", size] as const,
  byStatus: (storeId: string, status: string) =>
    [...orderKeys.all, "byStatus", storeId, status] as const,
  byStatusPaginated: (storeId: string, status: string, page: number, size: number) =>
    [...orderKeys.all, "byStatus", storeId, status, "page", page, "size", size] as const,
  statistics: (storeId: string) =>
    [...orderKeys.all, "statistics", storeId] as const,
  statisticsByDateRange: (storeId: string, startDate: string, endDate: string) =>
    [...orderKeys.all, "statistics", storeId, "dateRange", startDate, endDate] as const,
};

// Get orders by store with pagination
export function useOrdersByStore(
  storeId: string | undefined,
  page = 0,
  size = 20,
) {
  return useQuery<PagedResponse<TrendyolOrder>>({
    queryKey: orderKeys.byStorePaginated(storeId!, page, size),
    queryFn: () => ordersApi.getByStore(storeId!, page, size),
    enabled: !!storeId,
    staleTime: 2 * 60 * 1000, // 2 dakika
    gcTime: 10 * 60 * 1000, // 10 dakika cache'te tut
  });
}

// Get orders by date range
export function useOrdersByDateRange(
  storeId: string | undefined,
  startDate: string | undefined,
  endDate: string | undefined,
  page = 0,
  size = 20,
) {
  return useQuery<PagedResponse<TrendyolOrder>>({
    queryKey: orderKeys.byDateRangePaginated(storeId!, startDate!, endDate!, page, size),
    queryFn: () => ordersApi.getByDateRange(storeId!, startDate!, endDate!, page, size),
    enabled: !!storeId && !!startDate && !!endDate,
    staleTime: 2 * 60 * 1000,
  });
}

// Get order statistics
export function useOrderStatistics(storeId: string | undefined) {
  return useQuery<OrderStatistics>({
    queryKey: orderKeys.statistics(storeId!),
    queryFn: () => apiRequest<OrderStatistics>(`/orders/stores/${storeId}/statistics`),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000, // 5 dakika
  });
}

// Get order statistics by date range
export function useOrderStatisticsByDateRange(
  storeId: string | undefined,
  startDate: string | undefined,
  endDate: string | undefined
) {
  return useQuery<OrderStatistics>({
    queryKey: orderKeys.statisticsByDateRange(storeId!, startDate!, endDate!),
    queryFn: () =>
      apiRequest<OrderStatistics>(
        `/orders/stores/${storeId}/statistics/by-date-range?startDate=${encodeURIComponent(startDate!)}&endDate=${encodeURIComponent(endDate!)}`
      ),
    enabled: !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 dakika
  });
}

// Get orders by status
export function useOrdersByStatus(
  storeId: string | undefined,
  status: OrderStatus | undefined,
  page = 0,
  size = 20
) {
  return useQuery<PagedResponse<TrendyolOrder>>({
    queryKey: orderKeys.byStatusPaginated(storeId!, status || "", page, size),
    queryFn: () =>
      apiRequest<PagedResponse<TrendyolOrder>>(
        `/orders/stores/${storeId}/by-status?status=${status}&page=${page}&size=${size}`
      ),
    enabled: !!storeId && !!status,
    staleTime: 2 * 60 * 1000,
  });
}
