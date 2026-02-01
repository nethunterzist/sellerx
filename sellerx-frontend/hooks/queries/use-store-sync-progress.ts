import { useQuery } from "@tanstack/react-query";
import { storeApi } from "@/lib/api/client";
import { SyncStatus, isSyncInProgress } from "@/types/store";

export interface StoreSyncProgress {
  syncStatus: string;
  currentProcessingDate: string | null;
  completedChunks: number | null;
  totalChunks: number | null;
  percentage: number;
  checkpointDate: string | null;
  startDate: string | null;
  // Parallel sync fields
  syncPhases?: Record<string, { status: string; startedAt?: string; completedAt?: string; errorMessage?: string; progress?: number }>;
  overallSyncStatus?: string;
}

// Query keys for store sync progress
export const storeSyncProgressKeys = {
  all: ["store-sync-progress"] as const,
  byStore: (storeId: string) => [...storeSyncProgressKeys.all, storeId] as const,
};

/**
 * Hook to fetch store sync progress.
 * Automatically refetches every 3 seconds when any sync is in progress.
 */
export function useStoreSyncProgress(storeId: string | undefined) {
  return useQuery<StoreSyncProgress>({
    queryKey: storeSyncProgressKeys.byStore(storeId!),
    queryFn: () => storeApi.getSyncProgress(storeId!),
    enabled: !!storeId,
    refetchInterval: (query) => {
      const data = query.state.data as StoreSyncProgress | undefined;
      // If any sync is in progress, refetch every 3 seconds
      const syncStatus = data?.syncStatus as SyncStatus | undefined;
      return isSyncInProgress(syncStatus) ? 3000 : false;
    },
    staleTime: 0, // Always consider data stale to allow refetching
  });
}
