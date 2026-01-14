import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ordersApi, apiRequest } from "@/lib/api/client";
import type { TrendyolOrder, SyncOrdersResponse, OrderStatistics, OrderStatus } from "@/types/order";
import type { PagedResponse } from "@/types/api";
import { dashboardKeys } from "@/hooks/useDashboardStats";

// Order Query Keys
export const orderKeys = {
  all: ["orders"] as const,
  byStore: (storeId: string) => [...orderKeys.all, "store", storeId] as const,
  byStorePaginated: (storeId: string, page: number, size: number) =>
    [...orderKeys.all, "store", storeId, "paginated", { page, size }] as const,
  byDateRange: (storeId: string, startDate: string, endDate: string) =>
    [...orderKeys.all, "dateRange", storeId, startDate, endDate] as const,
  byStatus: (storeId: string, status: string) =>
    [...orderKeys.all, "byStatus", storeId, status] as const,
  statistics: (storeId: string) =>
    [...orderKeys.all, "statistics", storeId] as const,
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
    queryKey: [...orderKeys.byDateRange(storeId!, startDate!, endDate!), { page, size }],
    queryFn: () => ordersApi.getByDateRange(storeId!, startDate!, endDate!, page, size),
    enabled: !!storeId && !!startDate && !!endDate,
    staleTime: 2 * 60 * 1000,
  });
}

// Sync orders from Trendyol
export function useSyncOrders() {
  const queryClient = useQueryClient();

  return useMutation<SyncOrdersResponse, Error, string>({
    mutationFn: (storeId: string) => ordersApi.sync(storeId),
    onSuccess: (data, storeId) => {
      // Invalidate orders for this store
      queryClient.invalidateQueries({ queryKey: orderKeys.byStore(storeId) });
      // Invalidate dashboard stats as order data has changed
      queryClient.invalidateQueries({ queryKey: dashboardKeys.stats(storeId) });
    },
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

// Get orders by status
export function useOrdersByStatus(
  storeId: string | undefined,
  status: OrderStatus | undefined,
  page = 0,
  size = 20
) {
  return useQuery<PagedResponse<TrendyolOrder>>({
    queryKey: [...orderKeys.byStatus(storeId!, status || ""), { page, size }],
    queryFn: () =>
      apiRequest<PagedResponse<TrendyolOrder>>(
        `/orders/stores/${storeId}/by-status?status=${status}&page=${page}&size=${size}`
      ),
    enabled: !!storeId && !!status,
    staleTime: 2 * 60 * 1000,
  });
}
