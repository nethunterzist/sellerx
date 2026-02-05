import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/api/client";
import type {
  CustomerAnalyticsResponse,
  CustomerListResponse,
  ProductRepeatData,
  CrossSellData,
  BackfillStatus,
  LifecycleStageData,
  CohortData,
  FrequencyDistributionData,
  ClvSummaryData,
} from "@/types/customer-analytics";

// Query Keys
export const customerAnalyticsKeys = {
  all: ["customer-analytics"] as const,
  summary: (storeId: string) => [...customerAnalyticsKeys.all, "summary", storeId] as const,
  customers: (storeId: string, page: number, size: number, search: string) =>
    [...customerAnalyticsKeys.all, "customers", storeId, page, size, search] as const,
  productRepeat: (storeId: string) =>
    [...customerAnalyticsKeys.all, "product-repeat", storeId] as const,
  crossSell: (storeId: string) =>
    [...customerAnalyticsKeys.all, "cross-sell", storeId] as const,
  backfillStatus: (storeId: string) =>
    [...customerAnalyticsKeys.all, "backfill-status", storeId] as const,
  lifecycle: (storeId: string) =>
    [...customerAnalyticsKeys.all, "lifecycle", storeId] as const,
  cohorts: (storeId: string) =>
    [...customerAnalyticsKeys.all, "cohorts", storeId] as const,
  frequencyDistribution: (storeId: string) =>
    [...customerAnalyticsKeys.all, "frequency-distribution", storeId] as const,
  clvSummary: (storeId: string) =>
    [...customerAnalyticsKeys.all, "clv-summary", storeId] as const,
};

// Get analytics summary (summary + segmentation + city + monthly trend)
export function useCustomerAnalyticsSummary(storeId: string | undefined) {
  return useQuery({
    queryKey: customerAnalyticsKeys.summary(storeId!),
    queryFn: () =>
      apiRequest<CustomerAnalyticsResponse>(
        `/stores/${storeId}/customer-analytics/summary`
      ),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Get paginated customer list with optional search
export function useCustomerList(
  storeId: string | undefined,
  page: number = 0,
  size: number = 20,
  search: string = ""
) {
  return useQuery({
    queryKey: customerAnalyticsKeys.customers(storeId!, page, size, search),
    queryFn: () => {
      const searchParam = search.trim() ? `&search=${encodeURIComponent(search.trim())}` : "";
      return apiRequest<CustomerListResponse>(
        `/stores/${storeId}/customer-analytics/customers?page=${page}&size=${size}${searchParam}`
      );
    },
    enabled: !!storeId,
  });
}

// Get product repeat analysis
export function useProductRepeatAnalysis(storeId: string | undefined) {
  return useQuery({
    queryKey: customerAnalyticsKeys.productRepeat(storeId!),
    queryFn: () =>
      apiRequest<ProductRepeatData[]>(
        `/stores/${storeId}/customer-analytics/product-repeat`
      ),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000,
  });
}

// Get cross-sell analysis
export function useCrossSellAnalysis(storeId: string | undefined) {
  return useQuery({
    queryKey: customerAnalyticsKeys.crossSell(storeId!),
    queryFn: () =>
      apiRequest<CrossSellData[]>(
        `/stores/${storeId}/customer-analytics/cross-sell`
      ),
    enabled: !!storeId,
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}

// Get backfill status
export function useBackfillStatus(storeId: string | undefined) {
  return useQuery({
    queryKey: customerAnalyticsKeys.backfillStatus(storeId!),
    queryFn: () =>
      apiRequest<BackfillStatus>(
        `/stores/${storeId}/customer-analytics/backfill-status`
      ),
    enabled: !!storeId,
  });
}

// Trigger backfill mutation
export function useTriggerBackfill() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (storeId: string) =>
      apiRequest<{ status: string; message: string }>(
        `/stores/${storeId}/customer-analytics/trigger-backfill`,
        { method: "POST" }
      ),
    onSuccess: (_, storeId) => {
      queryClient.invalidateQueries({
        queryKey: customerAnalyticsKeys.backfillStatus(storeId),
      });
    },
  });
}

// ============== Advanced Analytics Hooks ==============

// Get customer lifecycle stages
export function useLifecycleStages(storeId: string | undefined) {
  return useQuery({
    queryKey: customerAnalyticsKeys.lifecycle(storeId!),
    queryFn: () =>
      apiRequest<LifecycleStageData[]>(
        `/stores/${storeId}/customer-analytics/lifecycle`
      ),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Get cohort retention analysis
export function useCohortAnalysis(storeId: string | undefined) {
  return useQuery({
    queryKey: customerAnalyticsKeys.cohorts(storeId!),
    queryFn: () =>
      apiRequest<CohortData[]>(
        `/stores/${storeId}/customer-analytics/cohorts`
      ),
    enabled: !!storeId,
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}

// Get purchase frequency distribution
export function useFrequencyDistribution(storeId: string | undefined) {
  return useQuery({
    queryKey: customerAnalyticsKeys.frequencyDistribution(storeId!),
    queryFn: () =>
      apiRequest<FrequencyDistributionData[]>(
        `/stores/${storeId}/customer-analytics/frequency-distribution`
      ),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Get CLV summary statistics
export function useClvSummary(storeId: string | undefined) {
  return useQuery({
    queryKey: customerAnalyticsKeys.clvSummary(storeId!),
    queryFn: () =>
      apiRequest<ClvSummaryData>(
        `/stores/${storeId}/customer-analytics/clv-summary`
      ),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}
