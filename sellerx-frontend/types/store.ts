/**
 * Store type definitions
 */

export type SyncStatus =
  | "pending"
  | "SYNCING_PRODUCTS"
  | "SYNCING_ORDERS"
  | "SYNCING_HISTORICAL"
  | "SYNCING_FINANCIAL"
  | "SYNCING_GAP"
  | "RECALCULATING_COMMISSIONS"
  | "SYNCING_RETURNS"
  | "SYNCING_QA"
  | "COMPLETED"
  | "PARTIAL_COMPLETE"
  | "CANCELLED"
  | "FAILED";

export type WebhookStatus = "pending" | "active" | "failed" | "inactive";

/**
 * Overall sync status for parallel execution
 */
export type OverallSyncStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "PARTIAL_COMPLETE" | "CANCELLED" | "FAILED";

/**
 * Phase status for individual sync phases
 */
export type PhaseStatusType = "PENDING" | "ACTIVE" | "COMPLETED" | "FAILED";

/**
 * Individual sync phase status
 */
export interface PhaseStatus {
  status: PhaseStatusType;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  progress?: number;
}

/**
 * Sync phases map
 * Keys: PRODUCTS, HISTORICAL, FINANCIAL, GAP, COMMISSIONS, RETURNS, QA
 */
export type SyncPhases = Record<string, PhaseStatus>;

/**
 * Phase names
 */
export type PhaseName =
  | "PRODUCTS"
  | "HISTORICAL"
  | "FINANCIAL"
  | "GAP"
  | "COMMISSIONS"
  | "RETURNS"
  | "QA";

export interface Store {
  id: string;
  userId: number;
  storeName: string;
  marketplace: string;
  credentials?: {
    sellerId: string;
    apiKey: string;
    apiSecret: string;
  };
  createdAt: string;
  updatedAt: string;

  // Webhook status
  webhookId?: string;
  webhookStatus?: WebhookStatus;
  webhookErrorMessage?: string;

  // Legacy sync status (backward compatibility)
  syncStatus?: SyncStatus;
  syncErrorMessage?: string;
  initialSyncCompleted?: boolean;

  // New parallel sync phases
  overallSyncStatus?: OverallSyncStatus;
  syncPhases?: SyncPhases;
}

export interface CreateStoreRequest {
  storeName: string;
  marketplace: string;
  credentials: {
    sellerId: string;
    apiKey: string;
    apiSecret: string;
  };
}

export interface UpdateStoreRequest {
  storeName?: string;
  credentials?: {
    sellerId: string;
    apiKey: string;
    apiSecret: string;
  };
}

/**
 * Get human-readable sync status message
 */
export function getSyncStatusMessage(status: SyncStatus | undefined): string {
  switch (status) {
    case "pending":
      return "Senkronizasyon bekleniyor...";
    case "SYNCING_PRODUCTS":
      return "Ürünler senkronize ediliyor...";
    case "SYNCING_ORDERS":
      return "Siparişler senkronize ediliyor...";
    case "SYNCING_HISTORICAL":
      return "Geçmiş siparişler senkronize ediliyor...";
    case "SYNCING_FINANCIAL":
      return "Finansal veriler senkronize ediliyor...";
    case "SYNCING_GAP":
      return "Son siparişler senkronize ediliyor...";
    case "RECALCULATING_COMMISSIONS":
      return "Komisyonlar hesaplanıyor...";
    case "SYNCING_RETURNS":
      return "İadeler senkronize ediliyor...";
    case "SYNCING_QA":
      return "Soru-cevaplar senkronize ediliyor...";
    case "COMPLETED":
      return "Senkronizasyon tamamlandı";
    case "PARTIAL_COMPLETE":
      return "Senkronizasyon kısmen tamamlandı";
    case "CANCELLED":
      return "Senkronizasyon iptal edildi";
    case "FAILED":
      return "Senkronizasyon başarısız";
    default:
      return "Bilinmeyen durum";
  }
}

/**
 * Check if sync is currently in progress
 */
export function isSyncInProgress(status: SyncStatus | undefined): boolean {
  return status === "pending" ||
         status === "SYNCING_PRODUCTS" ||
         status === "SYNCING_ORDERS" ||
         status === "SYNCING_HISTORICAL" ||
         status === "SYNCING_FINANCIAL" ||
         status === "SYNCING_GAP" ||
         status === "RECALCULATING_COMMISSIONS" ||
         status === "SYNCING_RETURNS" ||
         status === "SYNCING_QA";
}

/**
 * Get sync progress percentage (approximate) - legacy method
 */
export function getSyncProgress(status: SyncStatus | undefined): number {
  switch (status) {
    case "pending":
      return 0;
    case "SYNCING_PRODUCTS":
      return 14;
    case "SYNCING_ORDERS":
      return 28;
    case "SYNCING_HISTORICAL":
      return 42;
    case "SYNCING_FINANCIAL":
      return 56;
    case "SYNCING_GAP":
      return 64;
    case "RECALCULATING_COMMISSIONS":
      return 72;
    case "SYNCING_RETURNS":
      return 82;
    case "SYNCING_QA":
      return 92;
    case "COMPLETED":
      return 100;
    case "FAILED":
      return 0;
    default:
      return 0;
  }
}

/**
 * Phase weights for progress calculation
 */
export const PHASE_WEIGHTS: Record<string, number> = {
  PRODUCTS: 10,
  HISTORICAL: 35,
  FINANCIAL: 15,
  GAP: 10,
  COMMISSIONS: 10,
  RETURNS: 10,
  QA: 10,
};

/**
 * Calculate overall progress from sync phases (parallel sync)
 */
export function getOverallProgress(phases: SyncPhases | undefined): number {
  if (!phases || Object.keys(phases).length === 0) {
    return 0;
  }

  const totalWeight = Object.values(PHASE_WEIGHTS).reduce((a, b) => a + b, 0);
  let completedWeight = 0;

  for (const [phaseName, phaseStatus] of Object.entries(phases)) {
    const weight = PHASE_WEIGHTS[phaseName] || 0;

    if (phaseStatus?.status === "COMPLETED") {
      completedWeight += weight;
    } else if (phaseStatus?.status === "ACTIVE") {
      // Active phases count as 50% complete
      completedWeight += weight / 2;
    }
  }

  return Math.round((completedWeight / totalWeight) * 100);
}

/**
 * Check if sync is in progress (supports both legacy and parallel sync)
 */
export function isSyncInProgressV2(
  overallStatus: OverallSyncStatus | undefined,
  legacyStatus: SyncStatus | undefined
): boolean {
  // Check new parallel status first
  if (overallStatus === "IN_PROGRESS") {
    return true;
  }

  // Fallback to legacy check
  return isSyncInProgress(legacyStatus);
}

/**
 * Get active phases (phases currently running in parallel)
 */
export function getActivePhases(phases: SyncPhases | undefined): string[] {
  if (!phases) return [];

  return Object.entries(phases)
    .filter(([, status]) => status?.status === "ACTIVE")
    .map(([name]) => name);
}

/**
 * Get completed phases
 */
export function getCompletedPhases(phases: SyncPhases | undefined): string[] {
  if (!phases) return [];

  return Object.entries(phases)
    .filter(([, status]) => status?.status === "COMPLETED")
    .map(([name]) => name);
}

/**
 * Get failed phases
 */
export function getFailedPhases(phases: SyncPhases | undefined): string[] {
  if (!phases) return [];

  return Object.entries(phases)
    .filter(([, status]) => status?.status === "FAILED")
    .map(([name]) => name);
}

/**
 * Check if sync completed with partial success
 */
export function isPartialComplete(
  overallStatus: OverallSyncStatus | undefined,
  legacyStatus: SyncStatus | undefined
): boolean {
  return overallStatus === "PARTIAL_COMPLETE" || legacyStatus === "PARTIAL_COMPLETE";
}

/**
 * Check if sync was cancelled
 */
export function isSyncCancelled(
  overallStatus: OverallSyncStatus | undefined,
  legacyStatus: SyncStatus | undefined
): boolean {
  return overallStatus === "CANCELLED" || legacyStatus === "CANCELLED";
}

/**
 * Get overall status message for parallel sync
 */
export function getOverallStatusMessage(status: OverallSyncStatus | undefined): string {
  switch (status) {
    case "PENDING":
      return "Senkronizasyon bekleniyor...";
    case "IN_PROGRESS":
      return "Senkronizasyon devam ediyor...";
    case "COMPLETED":
      return "Senkronizasyon tamamlandı";
    case "PARTIAL_COMPLETE":
      return "Senkronizasyon kısmen tamamlandı";
    case "CANCELLED":
      return "Senkronizasyon iptal edildi";
    case "FAILED":
      return "Senkronizasyon başarısız";
    default:
      return "Bilinmeyen durum";
  }
}
