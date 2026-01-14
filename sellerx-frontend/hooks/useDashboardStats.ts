import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "@/lib/api/client";
import type {
  DashboardStats,
  DashboardStatsResponse,
  OrderDetail,
  ProductDetail,
  PeriodExpense,
} from "@/types/dashboard";

// Dashboard Query Keys
export const dashboardKeys = {
  all: ["dashboard"] as const,
  stats: (storeId: string) => [...dashboardKeys.all, "stats", storeId] as const,
};

export const useDashboardStats = (storeId?: string) => {
  return useQuery<DashboardStatsResponse>({
    queryKey: dashboardKeys.stats(storeId!),
    queryFn: () => dashboardApi.getStats(storeId!),
    enabled: !!storeId,
    refetchInterval: 5 * 60 * 1000, // 5 dakikada bir yenile
    staleTime: 2 * 60 * 1000, // 2 dakika boyunca fresh kabul et
    gcTime: 10 * 60 * 1000, // 10 dakika cache'te tut
    retry: 3, // 3 kez tekrar dene
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000), // Exponential backoff
    refetchOnWindowFocus: false, // Pencere odaklandığında yenileme
    refetchOnReconnect: true, // İnternet bağlantısı yenilendiğinde çalıştır
  });
};

// Re-export types for backwards compatibility
export type {
  DashboardStats,
  DashboardStatsResponse,
  OrderDetail,
  ProductDetail,
  PeriodExpense as ExpenseDetail,
};
