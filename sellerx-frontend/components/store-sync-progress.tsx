"use client";

import { useStoreSyncProgress } from "@/hooks/queries/use-store-sync-progress";
import { Progress } from "@/components/ui/progress";

interface StoreSyncProgressProps {
  storeId: string;
}

/**
 * Component to display historical sync progress with date-based information.
 * Shows current processing date, percentage, and chunk progress.
 */
export function StoreSyncProgress({ storeId }: StoreSyncProgressProps) {
  const { data: progress, isLoading } = useStoreSyncProgress(storeId);

  if (isLoading || !progress) {
    return null;
  }

  // Only show during historical sync
  if (progress.syncStatus !== "SYNCING_HISTORICAL") {
    return null;
  }

  const formatDate = (date: string | null) => {
    if (!date) return "";
    try {
      return new Date(date).toLocaleDateString("tr-TR", {
        year: "numeric",
        month: "long",
        day: "numeric",
      });
    } catch {
      return "";
    }
  };

  const currentDateText = progress.currentProcessingDate
    ? `${formatDate(progress.currentProcessingDate)} verileri işleniyor...`
    : "Başlatılıyor...";

  return (
    <div className="space-y-2 rounded-lg border p-4">
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-foreground">{currentDateText}</span>
        <span className="text-muted-foreground">{progress.percentage}%</span>
      </div>
      <Progress value={progress.percentage} className="h-2" />
      <div className="flex items-center justify-between text-xs text-muted-foreground">
        <span>
          {progress.completedChunks ?? 0} / {progress.totalChunks ?? 0} paket
          tamamlandı
        </span>
        {progress.checkpointDate && (
          <span className="text-xs">
            Son kontrol noktası: {formatDate(progress.checkpointDate)}
          </span>
        )}
      </div>
    </div>
  );
}
