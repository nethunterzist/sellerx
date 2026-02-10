"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { cn } from "@/lib/utils";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import {
  Store,
  ChevronDown,
  Loader2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Ban,
  Package,
  History,
  DollarSign,
  Calculator,
  MessageCircleQuestion,
  Plus,
  Clock,
  RotateCcw,
  StopCircle,
} from "lucide-react";
import Link from "next/link";
import { useMyStores, useSetSelectedStore, useCancelSync, useRetrySync, storeKeys } from "@/hooks/queries/use-stores";
import { useStoreSyncProgress, storeSyncProgressKeys } from "@/hooks/queries/use-store-sync-progress";
import {
  Store as StoreType,
  SyncStatus,
  SyncPhases,
  PhaseStatus,
  isSyncInProgress,
  getSyncProgress,
  getOverallProgress,
  isPartialComplete,
  isSyncCancelled,
  getFailedPhases,
} from "@/types/store";

interface StoreSelectorDropdownProps {
  selectedStoreId: string | undefined;
  selectedStore: StoreType | undefined;
}

// Sync phases configuration - matches backend phase names
const SYNC_PHASES = [
  { phase: "PRODUCTS", label: "Ürünler", icon: Package, group: "main" },
  { phase: "HISTORICAL", label: "Geçmiş", icon: History, group: "critical" },
  { phase: "FINANCIAL", label: "Finansal", icon: DollarSign, group: "critical" },
  { phase: "GAP", label: "Son Günler", icon: Clock, group: "critical" },
  { phase: "COMMISSIONS", label: "Komisyon", icon: Calculator, group: "critical" },
  { phase: "RETURNS", label: "İadeler", icon: RotateCcw, group: "parallel" },
  { phase: "QA", label: "S&C", icon: MessageCircleQuestion, group: "parallel" },
] as const;

export function StoreSelectorDropdown({
  selectedStoreId,
  selectedStore,
}: StoreSelectorDropdownProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  const { data: myStores, isLoading: storesLoading } = useMyStores();
  const setSelectedStoreMutation = useSetSelectedStore();
  const cancelSyncMutation = useCancelSync();
  const retrySyncMutation = useRetrySync();

  // Get sync progress for selected store
  const { data: syncProgress } = useStoreSyncProgress(selectedStoreId);

  // Determine if using new parallel sync or legacy
  const syncPhases = syncProgress?.syncPhases as SyncPhases | undefined;
  const overallSyncStatus = syncProgress?.overallSyncStatus as string | undefined;
  const hasParallelSync = syncPhases && Object.keys(syncPhases).length > 0;

  // Legacy status fallback
  const legacySyncStatus = selectedStore?.syncStatus as SyncStatus | undefined;

  // Check if sync is in progress (supports both systems)
  const isInProgress = hasParallelSync
    ? overallSyncStatus === "IN_PROGRESS"
    : isSyncInProgress(legacySyncStatus);

  // Calculate progress (supports both systems)
  const progress = hasParallelSync
    ? getOverallProgress(syncPhases)
    : getSyncProgress(legacySyncStatus);

  // Poll for sync updates when sync is in progress
  useEffect(() => {
    if (!isInProgress || !selectedStoreId) return undefined;

    const interval = setInterval(() => {
      queryClient.invalidateQueries({ queryKey: storeKeys.detail(selectedStoreId) });
      queryClient.invalidateQueries({ queryKey: storeKeys.my() });
      queryClient.invalidateQueries({ queryKey: storeKeys.selected() });
      queryClient.invalidateQueries({ queryKey: storeSyncProgressKeys.byStore(selectedStoreId) });
    }, 3000);

    return () => clearInterval(interval);
  }, [isInProgress, selectedStoreId, queryClient]);

  const handleSelectStore = (storeId: string) => {
    setSelectedStoreMutation.mutate(storeId, {
      onSuccess: () => {
        setOpen(false);
        queryClient.invalidateQueries({ queryKey: storeKeys.selected() });
        router.refresh();
      },
    });
  };

  // Get phase status from syncPhases (new parallel sync)
  const getPhaseStatus = (phaseName: string): "pending" | "active" | "completed" | "failed" => {
    if (hasParallelSync && syncPhases) {
      const phaseData = syncPhases[phaseName] as PhaseStatus | undefined;
      if (!phaseData) return "pending";

      switch (phaseData.status) {
        case "ACTIVE":
          return "active";
        case "COMPLETED":
          return "completed";
        case "FAILED":
          return "failed";
        default:
          return "pending";
      }
    }

    // Fallback to legacy status mapping
    if (legacySyncStatus === "FAILED") return "failed";
    if (legacySyncStatus === "COMPLETED") return "completed";

    // Map legacy status to phase
    const legacyToPhase: Record<string, string> = {
      SYNCING_PRODUCTS: "PRODUCTS",
      SYNCING_HISTORICAL: "HISTORICAL",
      SYNCING_FINANCIAL: "FINANCIAL",
      SYNCING_GAP: "GAP",
      RECALCULATING_COMMISSIONS: "COMMISSIONS",
      SYNCING_RETURNS: "RETURNS",
      SYNCING_QA: "QA",
    };

    const currentPhase = legacyToPhase[legacySyncStatus || ""];
    const phaseOrder: string[] = SYNC_PHASES.map((p) => p.phase);
    const currentIndex = phaseOrder.indexOf(currentPhase);
    const phaseIndex = phaseOrder.indexOf(phaseName);

    if (currentPhase === phaseName) return "active";
    if (currentIndex > phaseIndex) return "completed";
    return "pending";
  };

  // Count active phases for display
  const activePhaseCount = hasParallelSync
    ? Object.values(syncPhases || {}).filter((p) => (p as PhaseStatus)?.status === "ACTIVE").length
    : isInProgress ? 1 : 0;

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          className="h-8 gap-2 text-sm font-normal max-w-[200px]"
        >
          {/* Store Icon with Sync Indicator */}
          <div className="relative flex-shrink-0">
            {isInProgress ? (
              <Loader2 className="h-4 w-4 text-orange-500 animate-spin" />
            ) : isPartialComplete(overallSyncStatus as any, legacySyncStatus) ? (
              <AlertTriangle className="h-4 w-4 text-yellow-500" />
            ) : isSyncCancelled(overallSyncStatus as any, legacySyncStatus) ? (
              <Ban className="h-4 w-4 text-gray-500" />
            ) : overallSyncStatus === "COMPLETED" || legacySyncStatus === "COMPLETED" ? (
              <Store className="h-4 w-4 text-green-500" />
            ) : overallSyncStatus === "FAILED" || legacySyncStatus === "FAILED" ? (
              <Store className="h-4 w-4 text-red-500" />
            ) : (
              <Store className="h-4 w-4 text-[#1D70F1]" />
            )}
          </div>

          {/* Store Name (hidden on mobile) */}
          <span className="hidden sm:inline truncate">
            {selectedStore?.storeName || "Mağaza Seç"}
          </span>

          {/* Sync Percentage Badge (only when syncing) */}
          {isInProgress && (
            <span className="text-xs text-orange-600 font-medium">
              {progress}%
            </span>
          )}

          <ChevronDown className="h-3.5 w-3.5 text-muted-foreground flex-shrink-0" />
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-80 p-0">
        {/* Sync Progress Panel (shown when syncing) */}
        {isInProgress && selectedStore && (
          <div className="p-4 border-b border-border bg-orange-50/50 dark:bg-orange-900/10">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-foreground">
                  Senkronizasyon
                </span>
                {/* Show parallel indicator when multiple phases active */}
                {activePhaseCount > 1 && (
                  <span className="text-xs bg-orange-200 dark:bg-orange-800/50 text-orange-700 dark:text-orange-300 px-1.5 py-0.5 rounded">
                    {activePhaseCount} paralel
                  </span>
                )}
              </div>
              <span className="text-xs text-orange-600 font-medium">
                {progress}%
              </span>
            </div>

            <Progress value={progress} className="h-1.5 mb-3" />

            {/* Phase Indicators Grid - supports multiple active */}
            <div className="grid grid-cols-3 gap-1.5">
              {SYNC_PHASES.map((phase) => {
                const status = getPhaseStatus(phase.phase);
                const Icon = phase.icon;
                return (
                  <div
                    key={phase.phase}
                    className={cn(
                      "flex flex-col items-center gap-1 px-2 py-1.5 rounded text-xs",
                      status === "active" && "bg-orange-100 dark:bg-orange-800/30",
                      status === "completed" && "bg-green-100 dark:bg-green-800/30",
                      status === "failed" && "bg-red-100 dark:bg-red-800/30",
                      status === "pending" && "bg-muted/30"
                    )}
                  >
                    <div
                      className={cn(
                        "p-1 rounded",
                        status === "active" && "text-orange-600",
                        status === "completed" && "text-green-600",
                        status === "failed" && "text-red-600",
                        status === "pending" && "text-muted-foreground"
                      )}
                    >
                      {status === "active" ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : status === "completed" ? (
                        <CheckCircle2 className="h-3.5 w-3.5" />
                      ) : status === "failed" ? (
                        <XCircle className="h-3.5 w-3.5" />
                      ) : (
                        <Icon className="h-3.5 w-3.5" />
                      )}
                    </div>
                    <span
                      className={cn(
                        "font-medium text-center",
                        status === "active" && "text-orange-600",
                        status === "completed" && "text-green-600",
                        status === "failed" && "text-red-600",
                        status === "pending" && "text-muted-foreground"
                      )}
                    >
                      {phase.label}
                    </span>
                  </div>
                );
              })}
            </div>

            {/* Current Processing Info */}
            {syncProgress?.currentProcessingDate && (
              <p className="text-xs text-muted-foreground mt-2 text-center">
                {new Date(syncProgress.currentProcessingDate).toLocaleDateString("tr-TR", {
                  year: "numeric",
                  month: "short",
                  day: "numeric",
                })} verileri işleniyor...
              </p>
            )}

            {/* Cancel Button */}
            <Button
              variant="ghost"
              size="sm"
              className="w-full mt-3 text-red-600 hover:text-red-700 hover:bg-red-50"
              onClick={() => selectedStoreId && cancelSyncMutation.mutate(selectedStoreId)}
              disabled={cancelSyncMutation.isPending}
            >
              {cancelSyncMutation.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <StopCircle className="h-4 w-4 mr-2" />
              )}
              Senkronizasyonu İptal Et
            </Button>
          </div>
        )}

        {/* Partial Complete or Cancelled Status Panel */}
        {selectedStore && !isInProgress && (isPartialComplete(overallSyncStatus as any, legacySyncStatus) || isSyncCancelled(overallSyncStatus as any, legacySyncStatus)) && (
          <div className={cn(
            "p-4 border-b border-border",
            isPartialComplete(overallSyncStatus as any, legacySyncStatus) && "bg-yellow-50/50 dark:bg-yellow-900/10",
            isSyncCancelled(overallSyncStatus as any, legacySyncStatus) && "bg-gray-50/50 dark:bg-gray-900/10"
          )}>
            <div className="flex items-center gap-2 mb-2">
              {isPartialComplete(overallSyncStatus as any, legacySyncStatus) ? (
                <>
                  <AlertTriangle className="h-4 w-4 text-yellow-600" />
                  <span className="text-sm font-medium text-yellow-700 dark:text-yellow-400">
                    Kısmen Tamamlandı
                  </span>
                </>
              ) : (
                <>
                  <Ban className="h-4 w-4 text-gray-500" />
                  <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
                    İptal Edildi
                  </span>
                </>
              )}
            </div>

            {/* Show failed phases if partial complete */}
            {isPartialComplete(overallSyncStatus as any, legacySyncStatus) && syncPhases && (
              <div className="mb-2">
                <p className="text-xs text-muted-foreground mb-1">Başarısız fazlar:</p>
                <div className="flex flex-wrap gap-1">
                  {getFailedPhases(syncPhases).map((phase) => (
                    <span
                      key={phase}
                      className="text-xs bg-red-100 text-red-700 px-1.5 py-0.5 rounded"
                    >
                      {SYNC_PHASES.find((p) => p.phase === phase)?.label || phase}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Retry Button */}
            <Button
              variant="outline"
              size="sm"
              className="w-full mt-2"
              onClick={() => selectedStoreId && retrySyncMutation.mutate(selectedStoreId)}
              disabled={retrySyncMutation.isPending}
            >
              {retrySyncMutation.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <RotateCcw className="h-4 w-4 mr-2" />
              )}
              Tekrar Dene
            </Button>
          </div>
        )}

        {/* Store List Header */}
        <div className="flex items-center justify-between px-3 py-2 border-b border-border">
          <span className="font-medium text-sm text-foreground">Mağazalarınız</span>
          <span className="text-xs text-muted-foreground">
            {myStores?.length || 0} mağaza
          </span>
        </div>

        {/* Store List */}
        <div className="max-h-[200px] overflow-y-auto">
          {storesLoading ? (
            <div className="flex items-center justify-center py-6">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ) : myStores && myStores.length > 0 ? (
            myStores.map((store: StoreType) => {
              const isSelected = store.id === selectedStoreId;
              const storeIsInProgress = store.overallSyncStatus === "IN_PROGRESS" ||
                isSyncInProgress(store.syncStatus);
              const storeCompleted = store.overallSyncStatus === "COMPLETED" ||
                store.syncStatus === "COMPLETED";
              const storePartialComplete = isPartialComplete(
                store.overallSyncStatus as any,
                store.syncStatus
              );
              const storeCancelled = isSyncCancelled(
                store.overallSyncStatus as any,
                store.syncStatus
              );
              const storeFailed = store.overallSyncStatus === "FAILED" ||
                store.syncStatus === "FAILED";

              return (
                <button
                  key={store.id}
                  onClick={() => handleSelectStore(store.id)}
                  disabled={setSelectedStoreMutation.isPending}
                  className={cn(
                    "w-full flex items-center gap-3 px-3 py-2.5 hover:bg-muted/50 transition-colors text-left",
                    isSelected && "bg-blue-50 dark:bg-blue-900/20"
                  )}
                >
                  {/* Marketplace Icon */}
                  <div
                    className={cn(
                      "h-8 w-8 rounded flex items-center justify-center text-xs font-bold text-white flex-shrink-0",
                      store.marketplace === "trendyol" ? "bg-[#F27A1A]" : "bg-[#FF6000]"
                    )}
                  >
                    {store.marketplace === "trendyol" ? "T" : "H"}
                  </div>

                  {/* Store Info */}
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-sm text-foreground truncate">
                      {store.storeName}
                    </p>
                    <p className="text-xs text-muted-foreground capitalize">
                      {store.marketplace}
                    </p>
                  </div>

                  {/* Status Indicator */}
                  {storeIsInProgress ? (
                    <Loader2 className="h-4 w-4 text-orange-500 animate-spin flex-shrink-0" />
                  ) : storePartialComplete ? (
                    <AlertTriangle className="h-4 w-4 text-yellow-500 flex-shrink-0" />
                  ) : storeCancelled ? (
                    <Ban className="h-4 w-4 text-gray-500 flex-shrink-0" />
                  ) : storeCompleted ? (
                    <CheckCircle2 className="h-4 w-4 text-green-500 flex-shrink-0" />
                  ) : storeFailed ? (
                    <XCircle className="h-4 w-4 text-red-500 flex-shrink-0" />
                  ) : isSelected ? (
                    <CheckCircle2 className="h-4 w-4 text-[#1D70F1] flex-shrink-0" />
                  ) : null}
                </button>
              );
            })
          ) : (
            <div className="text-center py-6">
              <Store className="h-8 w-8 mx-auto text-muted-foreground opacity-50 mb-2" />
              <p className="text-sm text-muted-foreground">Henüz mağaza yok</p>
            </div>
          )}
        </div>

        <DropdownMenuSeparator className="m-0" />

        {/* Add Store Button */}
        <div className="p-2">
          <Button
            variant="ghost"
            size="sm"
            className="w-full justify-start text-[#1D70F1]"
            asChild
            onClick={() => setOpen(false)}
          >
            <Link href="/new-store">
              <Plus className="h-4 w-4 mr-2" />
              Yeni Mağaza Ekle
            </Link>
          </Button>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
