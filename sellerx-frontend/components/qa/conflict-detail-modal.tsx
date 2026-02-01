"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { X, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useResolveConflict, useDismissConflict } from "@/hooks/queries/use-qa";
import type { ConflictAlert, ConflictType, ConflictSeverity } from "@/types/qa";

interface ConflictDetailModalProps {
  conflict: ConflictAlert | null;
  open: boolean;
  onClose: () => void;
}

export function ConflictDetailModal({
  conflict,
  open,
  onClose,
}: ConflictDetailModalProps) {
  const t = useTranslations("qa.conflicts");
  const [resolutionNotes, setResolutionNotes] = useState("");

  const resolveMutation = useResolveConflict();
  const dismissMutation = useDismissConflict();

  if (!conflict) return null;

  const getSeverityStyles = (severity: ConflictSeverity) => {
    switch (severity) {
      case "CRITICAL":
        return "bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-300";
      case "HIGH":
        return "bg-orange-100 text-orange-800 dark:bg-orange-900/50 dark:text-orange-300";
      case "MEDIUM":
        return "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/50 dark:text-yellow-300";
      case "LOW":
        return "bg-blue-100 text-blue-800 dark:bg-blue-900/50 dark:text-blue-300";
      default:
        return "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300";
    }
  };

  const getTypeLabel = (type: ConflictType) => {
    switch (type) {
      case "LEGAL_RISK":
        return t("legalRisk");
      case "HEALTH_SAFETY":
        return t("healthSafety");
      case "KNOWLEDGE_CONFLICT":
        return t("knowledgeConflict");
      case "BRAND_INCONSISTENCY":
        return t("brandInconsistency");
      default:
        return type;
    }
  };

  const handleResolve = async () => {
    try {
      await resolveMutation.mutateAsync({
        conflictId: conflict.id,
        resolutionNotes: resolutionNotes || undefined,
      });
      setResolutionNotes("");
      onClose();
    } catch (error) {
      console.error("Failed to resolve conflict:", error);
    }
  };

  const handleDismiss = async () => {
    try {
      await dismissMutation.mutateAsync(conflict.id);
      onClose();
    } catch (error) {
      console.error("Failed to dismiss conflict:", error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle className="flex items-center justify-between">
            <span>{t("conflictDetails")}</span>
            <Button variant="ghost" size="icon" onClick={onClose}>
              <X className="h-4 w-4" />
            </Button>
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* Type and Severity */}
          <div className="flex items-center gap-4">
            <div>
              <span className="text-sm text-muted-foreground">{t("conflictType")}:</span>
              <p className="font-medium">{getTypeLabel(conflict.conflictType)}</p>
            </div>
            <div>
              <span className="text-sm text-muted-foreground">{t("severity")}:</span>
              <p>
                <span className={`px-2 py-1 text-xs font-bold rounded-full uppercase ${getSeverityStyles(conflict.severity)}`}>
                  {t(conflict.severity.toLowerCase())}
                </span>
              </p>
            </div>
          </div>

          {/* Customer Question */}
          {conflict.customerQuestion && (
            <div>
              <span className="text-sm text-muted-foreground">{t("customerQuestion")}:</span>
              <p className="font-medium bg-muted p-2 rounded mt-1">
                "{conflict.customerQuestion}"
              </p>
            </div>
          )}

          {/* Conflict Sources */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Source A */}
            <div className="space-y-2">
              <span className="text-sm text-muted-foreground flex items-center gap-2">
                {conflict.sourceAType === "KNOWLEDGE_BASE" ? (
                  <>📚 {t("knowledgeBase")}</>
                ) : conflict.sourceAType === "AI_RESPONSE" ? (
                  <>🤖 {t("aiResponse")}</>
                ) : (
                  <>📄 {conflict.sourceAType}</>
                )}
              </span>
              <div className="bg-muted/50 p-3 rounded-lg text-sm min-h-[100px]">
                {conflict.sourceAContent}
              </div>
            </div>

            {/* VS Divider */}
            {conflict.sourceBContent && (
              <>
                <div className="hidden md:flex items-center justify-center">
                  <span className="text-2xl font-bold text-muted-foreground">VS</span>
                </div>

                {/* Source B */}
                <div className="space-y-2">
                  <span className="text-sm text-muted-foreground flex items-center gap-2">
                    {conflict.sourceBType === "TRENDYOL_DATA" ? (
                      <>🛒 {t("trendyolData")}</>
                    ) : conflict.sourceBType === "AI_RESPONSE" ? (
                      <>🤖 {t("aiResponse")}</>
                    ) : (
                      <>📄 {conflict.sourceBType}</>
                    )}
                  </span>
                  <div className="bg-muted/50 p-3 rounded-lg text-sm min-h-[100px]">
                    {conflict.sourceBContent}
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Detected Keywords */}
          {conflict.detectedKeywords && conflict.detectedKeywords.length > 0 && (
            <div>
              <span className="text-sm text-muted-foreground">{t("detectedKeywords")}:</span>
              <div className="flex flex-wrap gap-1 mt-1">
                {conflict.detectedKeywords.map((keyword, idx) => (
                  <span
                    key={idx}
                    className="px-2 py-1 text-xs bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300 rounded"
                  >
                    {keyword}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Resolution Notes */}
          <div>
            <label className="text-sm font-medium mb-1 block">
              {t("resolutionNotes")}
            </label>
            <Textarea
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              placeholder={t("resolutionNotesPlaceholder")}
              rows={3}
            />
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button
              variant="outline"
              onClick={handleDismiss}
              disabled={dismissMutation.isPending}
            >
              {dismissMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin mr-1" />
              ) : null}
              {t("dismiss")}
            </Button>
            <Button
              onClick={handleResolve}
              disabled={resolveMutation.isPending}
            >
              {resolveMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin mr-1" />
              ) : null}
              {t("markResolved")}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
