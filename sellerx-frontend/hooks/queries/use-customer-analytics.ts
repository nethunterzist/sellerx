import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from "@tanstack/react-query";
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
  CustomerOrderDto,
  ProductDetailData,
  ProductBuyersPageResponse,
  CustomerOrdersPageResponse,
} from "@/types/customer-analytics";

// Query Keys
export const customerAnalyticsKeys = {
  all: ["customer-analytics"] as const,
  summary: (storeId: string) => [...customerAnalyticsKeys.all, "summary", storeId] as const,
  customers: (storeId: string, page: number, size: number, search: string) =>
    [...customerAnalyticsKeys.all, "customers", storeId, page, size, search] as const,
  customerOrders: (storeId: string, customerId: string) =>
    [...customerAnalyticsKeys.all, "customer-orders", storeId, customerId] as const,
  productRepeat: (storeId: string) =>
    [...customerAnalyticsKeys.all, "product-repeat", storeId] as const,
  productDetail: (storeId: string, barcode: string) =>
    [...customerAnalyticsKeys.all, "product-detail", storeId, barcode] as const,
  productBuyers: (storeId: string, barcode: string) =>
    [...customerAnalyticsKeys.all, "product-buyers", storeId, barcode] as const,
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

// Get all orders for a specific customer
export function useCustomerOrders(
  storeId: string | undefined,
  customerId: string | undefined
) {
  return useQuery({
    queryKey: customerAnalyticsKeys.customerOrders(storeId!, customerId!),
    queryFn: () =>
      apiRequest<CustomerOrderDto[]>(
        `/stores/${storeId}/customer-analytics/customers/${customerId}/orders`
      ),
    enabled: !!storeId && !!customerId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Get paginated customer orders with infinite scroll
export function useCustomerOrdersInfinite(
  storeId: string | undefined,
  customerId: string | undefined,
  pageSize: number = 20
) {
  return useInfiniteQuery({
    queryKey: [...customerAnalyticsKeys.customerOrders(storeId!, customerId!), "infinite"] as const,
    queryFn: ({ pageParam = 0 }) =>
      apiRequest<CustomerOrdersPageResponse>(
        `/stores/${storeId}/customer-analytics/customers/${customerId}/orders?page=${pageParam}&size=${pageSize}`
      ),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.page + 1 : undefined,
    enabled: !!storeId && !!customerId,
    staleTime: 5 * 60 * 1000, // 5 minutes
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

// Get product detail with buyers list
export function useProductDetail(
  storeId: string | undefined,
  barcode: string | undefined
) {
  return useQuery({
    queryKey: customerAnalyticsKeys.productDetail(storeId!, barcode!),
    queryFn: () =>
      apiRequest<ProductDetailData>(
        `/stores/${storeId}/customer-analytics/products/${encodeURIComponent(barcode!)}`
      ),
    enabled: !!storeId && !!barcode,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Get paginated product buyers with infinite scroll
export function useProductBuyersInfinite(
  storeId: string | undefined,
  barcode: string | undefined,
  pageSize: number = 20
) {
  return useInfiniteQuery({
    queryKey: customerAnalyticsKeys.productBuyers(storeId!, barcode!),
    queryFn: ({ pageParam = 0 }) =>
      apiRequest<ProductBuyersPageResponse>(
        `/stores/${storeId}/customer-analytics/products/${encodeURIComponent(barcode!)}/buyers?page=${pageParam}&size=${pageSize}`
      ),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.page + 1 : undefined,
    enabled: !!storeId && !!barcode,
    staleTime: 5 * 60 * 1000, // 5 minutes
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
