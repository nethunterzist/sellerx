"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Loader2, CheckCircle2, XCircle, Package, ShoppingCart, DollarSign, History, Calculator, MessageCircleQuestion } from "lucide-react";
import { cn } from "@/lib/utils";
import { Progress } from "@/components/ui/progress";
import {
  Store,
  SyncStatus,
  getSyncStatusMessage,
  isSyncInProgress,
  getSyncProgress,
} from "@/types/store";
import { storeKeys } from "@/hooks/queries/use-stores";

interface SyncStatusDisplayProps {
  store: Store;
  onComplete?: () => void;
  className?: string;
}

/**
 * Displays real-time sync status for a store.
 * Polls the store API to get updated sync status.
 */
export function SyncStatusDisplay({
  store,
  onComplete,
  className,
}: SyncStatusDisplayProps) {
  const queryClient = useQueryClient();
  const syncStatus = store.syncStatus;
  const isInProgress = isSyncInProgress(syncStatus);
  const progress = getSyncProgress(syncStatus);
  const message = getSyncStatusMessage(syncStatus);

  // Poll for updates while sync is in progress
  useEffect(() => {
    if (!isInProgress) {
      if (syncStatus === "COMPLETED" && onComplete) {
        onComplete();
      }
      return undefined;
    }

    const interval = setInterval(() => {
      // Invalidate store query to get fresh status
      queryClient.invalidateQueries({ queryKey: storeKeys.detail(store.id) });
      queryClient.invalidateQueries({ queryKey: storeKeys.my() });
    }, 3000); // Poll every 3 seconds

    return () => clearInterval(interval);
  }, [isInProgress, syncStatus, store.id, queryClient, onComplete]);

  const getStatusIcon = () => {
    switch (syncStatus) {
      case "SYNCING_PRODUCTS":
        return <Package className="h-6 w-6 text-orange-500 animate-pulse" />;
      case "SYNCING_ORDERS":
        return <ShoppingCart className="h-6 w-6 text-orange-500 animate-pulse" />;
      case "SYNCING_HISTORICAL":
        return <History className="h-6 w-6 text-orange-500 animate-pulse" />;
      case "SYNCING_FINANCIAL":
        return <DollarSign className="h-6 w-6 text-orange-500 animate-pulse" />;
      case "RECALCULATING_COMMISSIONS":
        return <Calculator className="h-6 w-6 text-orange-500 animate-pulse" />;
      case "SYNCING_QA":
        return <MessageCircleQuestion className="h-6 w-6 text-orange-500 animate-pulse" />;
      case "COMPLETED":
        return <CheckCircle2 className="h-6 w-6 text-green-500" />;
      case "FAILED":
        return <XCircle className="h-6 w-6 text-red-500" />;
      default:
        return <Loader2 className="h-6 w-6 text-orange-500 animate-spin" />;
    }
  };

  return (
    <div className={cn("space-y-4", className)}>
      <div className="flex items-center gap-3">
        {getStatusIcon()}
        <div className="flex-1">
          <p className="font-medium">{message}</p>
          {syncStatus === "FAILED" && store.syncErrorMessage && (
            <p className="text-sm text-red-500 mt-1">{store.syncErrorMessage}</p>
          )}
        </div>
      </div>

      {isInProgress && (
        <div className="space-y-2">
          <Progress value={progress} className="h-2" />
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>
              {syncStatus === "SYNCING_PRODUCTS" && "1/6"}
              {syncStatus === "SYNCING_ORDERS" && "2/6"}
              {syncStatus === "SYNCING_HISTORICAL" && "3/6"}
              {syncStatus === "SYNCING_FINANCIAL" && "4/6"}
              {syncStatus === "RECALCULATING_COMMISSIONS" && "5/6"}
              {syncStatus === "SYNCING_QA" && "6/6"}
              {syncStatus === "pending" && "0/6"}
            </span>
            <span>{progress}%</span>
          </div>
        </div>
      )}

      {/* Step indicators */}
      <div className="grid grid-cols-3 gap-2">
        <StepIndicator
          label="Ürünler"
          icon={<Package className="h-4 w-4" />}
          status={getStepStatus(syncStatus, "SYNCING_PRODUCTS")}
        />
        <StepIndicator
          label="Siparişler"
          icon={<ShoppingCart className="h-4 w-4" />}
          status={getStepStatus(syncStatus, "SYNCING_ORDERS")}
        />
        <StepIndicator
          label="Geçmiş"
          icon={<History className="h-4 w-4" />}
          status={getStepStatus(syncStatus, "SYNCING_HISTORICAL")}
        />
        <StepIndicator
          label="Finansal"
          icon={<DollarSign className="h-4 w-4" />}
          status={getStepStatus(syncStatus, "SYNCING_FINANCIAL")}
        />
        <StepIndicator
          label="Komisyon"
          icon={<Calculator className="h-4 w-4" />}
          status={getStepStatus(syncStatus, "RECALCULATING_COMMISSIONS")}
        />
        <StepIndicator
          label="S&C"
          icon={<MessageCircleQuestion className="h-4 w-4" />}
          status={getStepStatus(syncStatus, "SYNCING_QA")}
        />
      </div>
    </div>
  );
}

function getStepStatus(
  currentStatus: SyncStatus | undefined,
  stepStatus: SyncStatus
): "pending" | "active" | "completed" | "failed" {
  const order: SyncStatus[] = [
    "pending",
    "SYNCING_PRODUCTS",
    "SYNCING_ORDERS",
    "SYNCING_HISTORICAL",
    "SYNCING_FINANCIAL",
    "RECALCULATING_COMMISSIONS",
    "SYNCING_QA",
    "COMPLETED",
  ];

  if (currentStatus === "FAILED") return "failed";
  if (currentStatus === "COMPLETED") return "completed";

  const currentIndex = order.indexOf(currentStatus || "pending");
  const stepIndex = order.indexOf(stepStatus);

  if (currentIndex === stepIndex) return "active";
  if (currentIndex > stepIndex) return "completed";
  return "pending";
}

interface StepIndicatorProps {
  label: string;
  icon: React.ReactNode;
  status: "pending" | "active" | "completed" | "failed";
}

function StepIndicator({ label, icon, status }: StepIndicatorProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center gap-1 px-3 py-2 rounded-lg flex-1",
        status === "active" && "bg-orange-50 dark:bg-orange-900/20",
        status === "completed" && "bg-green-50 dark:bg-green-900/20",
        status === "failed" && "bg-red-50 dark:bg-red-900/20",
        status === "pending" && "bg-muted/50"
      )}
    >
      <div
        className={cn(
          "p-2 rounded-full",
          status === "active" && "bg-orange-100 dark:bg-orange-800 text-orange-600",
          status === "completed" && "bg-green-100 dark:bg-green-800 text-green-600",
          status === "failed" && "bg-red-100 dark:bg-red-800 text-red-600",
          status === "pending" && "bg-muted text-muted-foreground"
        )}
      >
        {status === "active" ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : status === "completed" ? (
          <CheckCircle2 className="h-4 w-4" />
        ) : status === "failed" ? (
          <XCircle className="h-4 w-4" />
        ) : (
          icon
        )}
      </div>
      <span
        className={cn(
          "text-xs font-medium",
          status === "active" && "text-orange-600",
          status === "completed" && "text-green-600",
          status === "failed" && "text-red-600",
          status === "pending" && "text-muted-foreground"
        )}
      >
        {label}
      </span>
    </div>
  );
}
