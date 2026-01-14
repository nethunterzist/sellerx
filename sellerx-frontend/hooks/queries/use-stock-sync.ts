import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/api/client";

// Stock Sync Status Response
export interface StockSyncStatus {
  storeId: string;
  lastSyncTime: string | null;
  status: "IDLE" | "IN_PROGRESS" | "COMPLETED" | "FAILED";
  message?: string;
  productsProcessed: number;
  productsUpdated: number;
}

// Stock Sync Response
export interface StockSyncResponse {
  success: boolean;
  message: string;
  productsProcessed: number;
  productsUpdated: number;
  errors: string[];
}

// Stock Sync Query Keys
export const stockSyncKeys = {
  all: ["stock-sync"] as const,
  status: (storeId: string) => [...stockSyncKeys.all, "status", storeId] as const,
};

// Get stock sync status
export function useStockSyncStatus(storeId: string | undefined) {
  return useQuery<StockSyncStatus>({
    queryKey: stockSyncKeys.status(storeId!),
    queryFn: () =>
      apiRequest<StockSyncStatus>(`/orders/stock-sync/status/${storeId}`),
    enabled: !!storeId,
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: (query) => {
      // Refetch every 5 seconds if sync is in progress
      const data = query.state.data;
      if (data?.status === "IN_PROGRESS") {
        return 5000;
      }
      return false;
    },
  });
}

// Sync stock from orders
export function useSyncStockOrders() {
  const queryClient = useQueryClient();

  return useMutation<StockSyncResponse, Error, string>({
    mutationFn: (storeId: string) =>
      apiRequest<StockSyncResponse>(
        `/orders/stock-sync/synchronize/${storeId}`,
        { method: "POST" }
      ),
    onSuccess: (data, storeId) => {
      // Invalidate stock sync status
      queryClient.invalidateQueries({ queryKey: stockSyncKeys.status(storeId) });
      // Invalidate products as stock data has changed
      queryClient.invalidateQueries({ queryKey: ["products", "store", storeId] });
    },
  });
}
