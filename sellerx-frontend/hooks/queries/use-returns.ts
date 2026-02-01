import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { returnsApi, claimsApi } from "@/lib/api/client";
import type {
  ReturnAnalyticsResponse,
  TrendyolClaim,
  ClaimsPage,
  ClaimsSyncResponse,
  ClaimActionResponse,
  ClaimIssueReason,
  ClaimsStats,
  BulkActionResponse,
} from "@/types/returns";

// Return Query Keys
export const returnKeys = {
  all: ["returns"] as const,
  analytics: () => [...returnKeys.all, "analytics"] as const,
  analyticsByStore: (storeId: string) =>
    [...returnKeys.analytics(), storeId] as const,
  analyticsByStoreAndRange: (storeId: string, startDate: string, endDate: string) =>
    [...returnKeys.analyticsByStore(storeId), { startDate, endDate }] as const,
};

// Claims Query Keys
export const claimKeys = {
  all: ["claims"] as const,
  lists: () => [...claimKeys.all, "list"] as const,
  list: (storeId: string, filter?: string, page?: number) =>
    [...claimKeys.lists(), { storeId, filter, page }] as const,
  details: () => [...claimKeys.all, "detail"] as const,
  detail: (storeId: string, claimId: string) =>
    [...claimKeys.details(), storeId, claimId] as const,
  stats: (storeId: string) => [...claimKeys.all, "stats", storeId] as const,
  issueReasons: () => [...claimKeys.all, "issue-reasons"] as const,
};

// Get return analytics for a date range
export function useReturnAnalytics(
  storeId: string | undefined,
  startDate: string,
  endDate: string,
) {
  return useQuery<ReturnAnalyticsResponse>({
    queryKey: returnKeys.analyticsByStoreAndRange(storeId!, startDate, endDate),
    queryFn: () => returnsApi.getAnalytics(storeId!, startDate, endDate),
    enabled: !!storeId && !!startDate && !!endDate,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Get return analytics for current month
export function useCurrentMonthReturnAnalytics(storeId: string | undefined) {
  return useQuery<ReturnAnalyticsResponse>({
    queryKey: [...returnKeys.analyticsByStore(storeId!), "current-month"],
    queryFn: () => returnsApi.getCurrentMonthAnalytics(storeId!),
    enabled: !!storeId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Get return analytics for last 30 days
export function useLast30DaysReturnAnalytics(storeId: string | undefined) {
  return useQuery<ReturnAnalyticsResponse>({
    queryKey: [...returnKeys.analyticsByStore(storeId!), "last-30-days"],
    queryFn: () => returnsApi.getLast30DaysAnalytics(storeId!),
    enabled: !!storeId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// =====================================================
// Claims Hooks
// =====================================================

// Get claims list with pagination
export function useClaims(
  storeId: string | undefined,
  filter?: string,
  page: number = 0,
  size: number = 20
) {
  return useQuery<ClaimsPage>({
    queryKey: claimKeys.list(storeId!, filter, page),
    queryFn: () => claimsApi.getClaims(storeId!, filter, page, size),
    enabled: !!storeId,
    staleTime: 1 * 60 * 1000, // 1 minute
  });
}

// Get single claim detail
export function useClaimDetail(
  storeId: string | undefined,
  claimId: string | undefined
) {
  return useQuery<TrendyolClaim>({
    queryKey: claimKeys.detail(storeId!, claimId!),
    queryFn: () => claimsApi.getClaim(storeId!, claimId!),
    enabled: !!storeId && !!claimId,
    staleTime: 1 * 60 * 1000,
  });
}

// Get claims statistics
export function useClaimsStats(storeId: string | undefined) {
  return useQuery<ClaimsStats>({
    queryKey: claimKeys.stats(storeId!),
    queryFn: () => claimsApi.getStats(storeId!),
    enabled: !!storeId,
    staleTime: 1 * 60 * 1000,
  });
}

// Get claim issue reasons
export function useClaimIssueReasons() {
  return useQuery<ClaimIssueReason[]>({
    queryKey: claimKeys.issueReasons(),
    queryFn: () => claimsApi.getIssueReasons(),
    staleTime: 24 * 60 * 60 * 1000, // 24 hours (static data)
  });
}

// Sync claims mutation
export function useSyncClaims() {
  const queryClient = useQueryClient();

  return useMutation<ClaimsSyncResponse, Error, string>({
    mutationFn: (storeId: string) => claimsApi.syncClaims(storeId),
    onSuccess: (_, storeId) => {
      // Invalidate all claims queries for this store
      queryClient.invalidateQueries({ queryKey: claimKeys.lists() });
      queryClient.invalidateQueries({ queryKey: claimKeys.stats(storeId) });
    },
  });
}

// Approve claim mutation
export function useApproveClaim() {
  const queryClient = useQueryClient();

  return useMutation<
    ClaimActionResponse,
    Error,
    { storeId: string; claimId: string; claimLineItemIds: string[] }
  >({
    mutationFn: ({ storeId, claimId, claimLineItemIds }) =>
      claimsApi.approveClaim(storeId, claimId, claimLineItemIds),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: claimKeys.lists() });
      queryClient.invalidateQueries({ queryKey: claimKeys.stats(storeId) });
    },
  });
}

// Reject claim mutation
export function useRejectClaim() {
  const queryClient = useQueryClient();

  return useMutation<
    ClaimActionResponse,
    Error,
    {
      storeId: string;
      claimId: string;
      reasonId: number;
      claimItemIds: string[];
      description?: string;
    }
  >({
    mutationFn: ({ storeId, claimId, reasonId, claimItemIds, description }) =>
      claimsApi.rejectClaim(storeId, claimId, reasonId, claimItemIds, description),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: claimKeys.lists() });
      queryClient.invalidateQueries({ queryKey: claimKeys.stats(storeId) });
    },
  });
}

// Bulk approve claims mutation
export function useBulkApproveClaims() {
  const queryClient = useQueryClient();

  return useMutation<
    BulkActionResponse,
    Error,
    {
      storeId: string;
      claims: { claimId: string; claimLineItemIds: string[] }[];
    }
  >({
    mutationFn: ({ storeId, claims }) =>
      claimsApi.bulkApproveClaims(storeId, claims),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: claimKeys.lists() });
      queryClient.invalidateQueries({ queryKey: claimKeys.stats(storeId) });
    },
  });
}
