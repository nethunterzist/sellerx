"use client";

/**
 * SyncLogContext - Provides sync logging state for development mode
 * Tracks data changes during auto-refresh cycles
 * Persisted in sessionStorage within tab session
 */

import {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  useMemo,
  type ReactNode,
} from "react";
import type { SyncLogEntry, SyncLogState, SyncLogContextValue } from "@/types/sync-log";

const isDev = process.env.NODE_ENV === "development";
const STORAGE_KEY = "sellerx-sync-log";
const MAX_ENTRIES = 50;

// Initial state
const initialState: SyncLogState = {
  entries: [],
  isCapturing: false,
  lastSyncTime: null,
};

// Context
const SyncLogContext = createContext<SyncLogContextValue | undefined>(undefined);

// Provider
interface SyncLogProviderProps {
  children: ReactNode;
}

export function SyncLogProvider({ children }: SyncLogProviderProps) {
  const [state, setState] = useState<SyncLogState>(initialState);

  // Load from sessionStorage on mount (dev only)
  useEffect(() => {
    if (!isDev) return;

    try {
      const stored = sessionStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as Partial<SyncLogState>;
        setState((prev) => ({
          ...prev,
          entries: parsed.entries || [],
          lastSyncTime: parsed.lastSyncTime ?? null,
        }));
      }
    } catch (error) {
      // Ignore parse errors
      console.warn("[SyncLog] Failed to load from sessionStorage:", error);
    }
  }, []);

  // Persist to sessionStorage on changes (dev only)
  useEffect(() => {
    if (!isDev) return;

    try {
      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          entries: state.entries,
          lastSyncTime: state.lastSyncTime,
        })
      );
    } catch (error) {
      // Ignore storage errors (quota exceeded, etc.)
      console.warn("[SyncLog] Failed to save to sessionStorage:", error);
    }
  }, [state.entries, state.lastSyncTime]);

  // Add new log entry
  const addEntry = useCallback((entry: SyncLogEntry) => {
    if (!isDev) return;

    setState((prev) => {
      const newEntries = [entry, ...prev.entries].slice(0, MAX_ENTRIES);
      return {
        ...prev,
        entries: newEntries,
        lastSyncTime: entry.timestamp,
        isCapturing: false,
      };
    });
  }, []);

  // Clear all logs
  const clearLogs = useCallback(() => {
    if (!isDev) return;

    setState((prev) => ({
      ...prev,
      entries: [],
    }));

    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Ignore
    }
  }, []);

  const value = useMemo<SyncLogContextValue>(
    () => ({
      state,
      addEntry,
      clearLogs,
      isEnabled: isDev,
    }),
    [state, addEntry, clearLogs]
  );

  return <SyncLogContext.Provider value={value}>{children}</SyncLogContext.Provider>;
}

// Hook
export function useSyncLog(): SyncLogContextValue {
  const context = useContext(SyncLogContext);
  if (!context) {
    // Return no-op context if used outside provider (graceful degradation)
    return {
      state: initialState,
      addEntry: () => {},
      clearLogs: () => {},
      isEnabled: false,
    };
  }
  return context;
}
