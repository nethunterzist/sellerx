import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/api/client";
import { dashboardKeys } from "@/hooks/useDashboardStats";
import { orderKeys } from "@/hooks/queries/use-orders";

// Financial Sync Response
export interface FinancialSyncResponse {
  success: boolean;
  message: string;
  recordsProcessed: number;
  recordsUpdated: number;
}

// Financial Query Keys
export const financialKeys = {
  all: ["financial"] as const,
  sync: (storeId: string) => [...financialKeys.all, "sync", storeId] as const,
};

// Sync financial data from Trendyol
export function useSyncFinancial() {
  const queryClient = useQueryClient();

  return useMutation<FinancialSyncResponse, Error, string>({
    mutationFn: (storeId: string) =>
      apiRequest<FinancialSyncResponse>(
        `/financial/stores/${storeId}/sync`,
        { method: "POST" }
      ),
    onSuccess: (data, storeId) => {
      // Invalidate dashboard stats as financial data has changed
      queryClient.invalidateQueries({ queryKey: dashboardKeys.stats(storeId) });
      // Invalidate orders as they may have updated financial info
      queryClient.invalidateQueries({ queryKey: orderKeys.byStore(storeId) });
    },
  });
}
