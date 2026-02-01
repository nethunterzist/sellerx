// Sync Log Types - Development-only logging for auto-refresh system

export type SyncDataType =
  | "orders"
  | "products"
  | "dashboard"
  | "financial"
  | "returns"
  | "claims"
  | "qa"
  | "expenses"
  | "ads";

export interface SyncDataTypeConfig {
  type: SyncDataType;
  label: string;
  icon: string;
  queryKeyFn: (storeId: string) => readonly unknown[];
}

export interface SyncDiff {
  dataType: SyncDataType;
  label: string;
  icon: string;
  hasChanges: boolean;
  summary: string;
  details?: {
    added?: number;
    updated?: number;
    removed?: number;
    metrics?: Array<{
      field: string;
      label: string;
      from: number | string | null;
      to: number | string | null;
      formatted?: string;
    }>;
  };
}

export interface SyncLogEntry {
  id: string;
  timestamp: number;
  storeId: string;
  duration: number;
  status: "success" | "partial" | "error";
  totalChanges: number;
  diffs: SyncDiff[];
  error?: string;
}

export interface SyncLogState {
  entries: SyncLogEntry[];
  isCapturing: boolean;
  lastSyncTime: number | null;
}

export interface SyncLogContextValue {
  state: SyncLogState;
  addEntry: (entry: SyncLogEntry) => void;
  clearLogs: () => void;
  isEnabled: boolean;
}
