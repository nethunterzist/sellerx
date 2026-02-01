"use client";

/**
 * SyncLogPanel - Development-only UI for viewing sync history
 * Shows what data changed during each auto-refresh cycle
 */

import { useState } from "react";
import { useSyncLog } from "@/lib/contexts/sync-log-context";
import { formatSyncTime, formatDuration } from "@/lib/sync-logger";
import { SettingsSection } from "./settings-section";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Trash2, ChevronDown, ChevronRight, RefreshCw } from "lucide-react";
import { cn } from "@/lib/utils";
import type { SyncLogEntry } from "@/types/sync-log";

// Only render in development
const isDev = process.env.NODE_ENV === "development";

export function SyncLogPanel() {
  // Don't render anything in production
  if (!isDev) return null;

  return <SyncLogPanelContent />;
}

function SyncLogPanelContent() {
  const { state, clearLogs, isEnabled } = useSyncLog();
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  if (!isEnabled) return null;

  const toggleExpanded = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  return (
    <SettingsSection
      title="Senkronizasyon Geçmişi"
      description="Son 50 otomatik senkronizasyon döngüsü (sadece geliştirme modunda görünür)"
      action={
        state.entries.length > 0 ? (
          <Button
            variant="outline"
            size="sm"
            onClick={clearLogs}
            className="text-destructive hover:text-destructive"
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Logları Temizle
          </Button>
        ) : null
      }
    >
      {state.entries.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          <RefreshCw className="h-8 w-8 mx-auto mb-2 opacity-50" />
          <p>Henüz senkronizasyon kaydı yok.</p>
          <p className="text-sm mt-1">
            Otomatik yenileme aktifken burada değişiklikler görünecek.
          </p>
        </div>
      ) : (
        <div className="space-y-2 max-h-[400px] overflow-y-auto">
          {state.entries.map((entry) => (
            <SyncLogEntryItem
              key={entry.id}
              entry={entry}
              isExpanded={expandedIds.has(entry.id)}
              onToggle={() => toggleExpanded(entry.id)}
            />
          ))}
        </div>
      )}
    </SettingsSection>
  );
}

interface SyncLogEntryItemProps {
  entry: SyncLogEntry;
  isExpanded: boolean;
  onToggle: () => void;
}

function SyncLogEntryItem({ entry, isExpanded, onToggle }: SyncLogEntryItemProps) {
  const hasChanges = entry.totalChanges > 0;

  return (
    <Collapsible open={isExpanded} onOpenChange={onToggle}>
      <div
        className={cn(
          "border rounded-lg transition-colors",
          hasChanges ? "border-primary/30 bg-primary/5" : "border-border"
        )}
      >
        <CollapsibleTrigger asChild>
          <button className="w-full flex items-center justify-between p-3 text-left hover:bg-muted/50 rounded-lg">
            <div className="flex items-center gap-3">
              {isExpanded ? (
                <ChevronDown className="h-4 w-4 text-muted-foreground" />
              ) : (
                <ChevronRight className="h-4 w-4 text-muted-foreground" />
              )}
              <span className="font-mono text-sm">
                {formatSyncTime(entry.timestamp)}
              </span>
              <span className="text-xs text-muted-foreground">
                ({formatDuration(entry.duration)})
              </span>
            </div>
            <div className="flex items-center gap-2">
              {hasChanges ? (
                <Badge variant="default" className="bg-primary">
                  {entry.totalChanges} değişiklik
                </Badge>
              ) : (
                <Badge variant="secondary">Değişiklik yok</Badge>
              )}
            </div>
          </button>
        </CollapsibleTrigger>

        <CollapsibleContent>
          <div className="px-3 pb-3 pt-1 border-t border-border/50">
            <div className="space-y-1.5">
              {entry.diffs.map((diff) => (
                <div
                  key={diff.dataType}
                  className={cn(
                    "flex items-center gap-2 text-sm py-1 px-2 rounded",
                    diff.hasChanges ? "bg-primary/10" : "opacity-60"
                  )}
                >
                  <span className="w-5">{diff.icon}</span>
                  <span className="font-medium min-w-[100px]">{diff.label}:</span>
                  <span
                    className={cn(
                      diff.hasChanges ? "text-foreground" : "text-muted-foreground"
                    )}
                  >
                    {diff.summary}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </CollapsibleContent>
      </div>
    </Collapsible>
  );
}
