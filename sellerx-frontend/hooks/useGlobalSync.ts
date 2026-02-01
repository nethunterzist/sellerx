"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useCallback } from "react";
import { useUserPreferences } from "./queries/use-settings";
import { useSelectedStore } from "./queries/use-stores";
import { useSyncLog } from "@/lib/contexts/sync-log-context";
import {
  SYNC_DATA_TYPES,
  computeDiff,
  generateSyncLogId,
} from "@/lib/sync-logger";
import type { SyncLogEntry, SyncDiff } from "@/types/sync-log";

// Import all query keys from different modules
import { orderKeys } from "./queries/use-orders";
import { productKeys } from "./queries/use-products";
import { dashboardKeys } from "./useDashboardStats";
import { financialKeys } from "./queries/use-financial";
import { returnKeys, claimKeys } from "./queries/use-returns";
import { qaKeys } from "./queries/use-qa";
import { expenseKeys } from "./queries/use-expenses";

const isDev = process.env.NODE_ENV === "development";

/**
 * Global sync hook that periodically refreshes all store-related data.
 *
 * Uses the user's syncInterval preference to determine polling frequency.
 * When triggered, invalidates all React Query caches for the current store,
 * causing automatic refetches of visible queries.
 *
 * In development mode, also logs data changes to SyncLogContext.
 *
 * @example
 * // In app-layout.tsx
 * export function AppLayout({ children }) {
 *   useGlobalSync();
 *   return <>{children}</>;
 * }
 */
export function useGlobalSync() {
  const queryClient = useQueryClient();
  const { data: preferences } = useUserPreferences();
  const { data: selectedStore } = useSelectedStore();
  const { addEntry, isEnabled: syncLogEnabled } = useSyncLog();
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const lastRefreshRef = useRef<number>(0);

  // Capture snapshots of all query data before invalidation
  const captureSnapshots = useCallback(
    (storeId: string): Map<string, unknown> => {
      if (!syncLogEnabled) return new Map();

      const snapshots = new Map<string, unknown>();

      SYNC_DATA_TYPES.forEach((config) => {
        const queryKey = config.queryKeyFn(storeId);
        const data = queryClient.getQueryData(queryKey);
        snapshots.set(config.type, data);
      });

      return snapshots;
    },
    [queryClient, syncLogEnabled]
  );

  // Compute diffs and create log entry
  const createLogEntry = useCallback(
    (
      storeId: string,
      beforeSnapshots: Map<string, unknown>,
      startTime: number
    ): SyncLogEntry => {
      const diffs: SyncDiff[] = [];
      let totalChanges = 0;

      SYNC_DATA_TYPES.forEach((config) => {
        const before = beforeSnapshots.get(config.type);
        const queryKey = config.queryKeyFn(storeId);
        const after = queryClient.getQueryData(queryKey);

        const diff = computeDiff(config.type, config, before, after);
        diffs.push(diff);

        if (diff.hasChanges) {
          totalChanges++;
        }
      });

      return {
        id: generateSyncLogId(),
        timestamp: Date.now(),
        storeId,
        duration: Date.now() - startTime,
        status: "success",
        totalChanges,
        diffs,
      };
    },
    [queryClient]
  );

  // Perform sync with logging
  const performSync = useCallback(
    (storeId: string, logEnabled: boolean) => {
      const startTime = Date.now();
      lastRefreshRef.current = startTime;

      // Capture before snapshots (only in dev with logging enabled)
      const beforeSnapshots = logEnabled ? captureSnapshots(storeId) : new Map();

      if (isDev) {
        console.log(
          `[GlobalSync] Refreshing all data for store ${storeId} at ${new Date(startTime).toLocaleTimeString()}`
        );
      }

      // Invalidate all store-related queries
      queryClient.invalidateQueries({ queryKey: orderKeys.byStore(storeId) });
      queryClient.invalidateQueries({ queryKey: productKeys.byStore(storeId) });
      queryClient.invalidateQueries({ queryKey: dashboardKeys.stats(storeId) });
      queryClient.invalidateQueries({ queryKey: financialKeys.storeStats(storeId) });
      queryClient.invalidateQueries({ queryKey: returnKeys.analyticsByStore(storeId) });
      queryClient.invalidateQueries({ queryKey: claimKeys.lists() });
      queryClient.invalidateQueries({ queryKey: qaKeys.questionsByStore(storeId) });
      queryClient.invalidateQueries({ queryKey: expenseKeys.byStore(storeId) });

      // Log changes after queries have had time to refetch (dev only)
      if (logEnabled && beforeSnapshots.size > 0) {
        // Wait for refetches to complete, then compute diffs
        setTimeout(() => {
          const entry = createLogEntry(storeId, beforeSnapshots, startTime);
          addEntry(entry);

          if (isDev && entry.totalChanges > 0) {
            console.log(`[GlobalSync] ${entry.totalChanges} data types changed`);
          }
        }, 2000); // 2 second delay for refetches
      }
    },
    [queryClient, captureSnapshots, createLogEntry, addEntry]
  );

  useEffect(() => {
    // Clear any existing interval
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }

    const syncInterval = preferences?.syncInterval ?? 60; // Default: 1 minute
    const storeId = selectedStore?.selectedStoreId;

    // 0 = disabled, or no store selected
    if (syncInterval === 0 || !storeId) {
      if (isDev && syncInterval === 0) {
        console.log("[GlobalSync] Auto-refresh disabled by user preference");
      }
      return;
    }

    if (isDev) {
      console.log(
        `[GlobalSync] Starting auto-refresh every ${syncInterval}s for store ${storeId}`
      );
    }

    // Start interval
    intervalRef.current = setInterval(() => {
      performSync(storeId, syncLogEnabled);
    }, syncInterval * 1000);

    // Cleanup on unmount or when dependencies change
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
        if (isDev) {
          console.log("[GlobalSync] Stopped auto-refresh");
        }
      }
    };
  }, [preferences?.syncInterval, selectedStore?.selectedStoreId, performSync, syncLogEnabled]);

  // Return utility functions for manual control
  return {
    /**
     * Get the timestamp of the last refresh
     */
    getLastRefreshTime: () => lastRefreshRef.current,

    /**
     * Check if auto-refresh is currently active
     */
    isActive: () => intervalRef.current !== null,

    /**
     * Force an immediate refresh of all data
     */
    forceRefresh: () => {
      const storeId = selectedStore?.selectedStoreId;
      if (!storeId) return;

      if (isDev) {
        console.log(`[GlobalSync] Force refresh triggered for store ${storeId}`);
      }

      performSync(storeId, syncLogEnabled);
    },
  };
}
