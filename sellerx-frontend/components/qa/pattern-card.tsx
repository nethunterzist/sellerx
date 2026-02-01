"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  ChevronDown,
  ChevronUp,
  Rocket,
  ArrowDown,
  Settings,
  Loader2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  usePromotePattern,
  useDemotePattern,
  useEnableAutoSubmit,
} from "@/hooks/queries/use-qa";
import type { QaPattern, SeniorityLevel } from "@/types/qa";

interface PatternCardProps {
  pattern: QaPattern;
}

export function PatternCard({ pattern }: PatternCardProps) {
  const t = useTranslations("qa.seniority");
  const [expanded, setExpanded] = useState(false);

  const promoteMutation = usePromotePattern();
  const demoteMutation = useDemotePattern();
  const enableAutoSubmitMutation = useEnableAutoSubmit();

  const getSeniorityBadge = (level: SeniorityLevel) => {
    const config = {
      JUNIOR: { icon: "○", color: "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300" },
      LEARNING: { icon: "◐", color: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300" },
      SENIOR: { icon: "●", color: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300" },
      EXPERT: { icon: "★", color: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300" },
    };

    const { icon, color } = config[level];

    return (
      <span className={`px-2 py-1 text-xs font-medium rounded-full ${color} flex items-center gap-1`}>
        {icon} {t(level.toLowerCase())}
      </span>
    );
  };

  const getAutoSubmitStatus = () => {
    if (pattern.isAutoSubmitEligible) {
      return (
        <span className="px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300">
          {t("autoSubmit")}: {t("active")}
        </span>
      );
    }

    if (pattern.seniorityLevel === "EXPERT" || pattern.seniorityLevel === "SENIOR") {
      return (
        <span className="px-2 py-1 text-xs font-medium rounded-full bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300">
          {t("autoSubmit")}: {t("ready")}
        </span>
      );
    }

    return null;
  };

  const handlePromote = async () => {
    try {
      await promoteMutation.mutateAsync(pattern.id);
    } catch (error) {
      console.error("Failed to promote pattern:", error);
    }
  };

  const handleDemote = async () => {
    try {
      await demoteMutation.mutateAsync({ patternId: pattern.id });
    } catch (error) {
      console.error("Failed to demote pattern:", error);
    }
  };

  const handleEnableAutoSubmit = async () => {
    try {
      await enableAutoSubmitMutation.mutateAsync(pattern.id);
    } catch (error) {
      console.error("Failed to enable auto-submit:", error);
    }
  };

  const confidencePercent = Math.round(pattern.confidenceScore * 100);
  const approvalPercent = Math.round(pattern.approvalRate * 100);

  return (
    <div className="border rounded-lg overflow-hidden bg-card">
      {/* Header */}
      <div
        className="p-4 cursor-pointer hover:bg-muted/30 transition-colors"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            {getSeniorityBadge(pattern.seniorityLevel)}
            <span className="text-sm text-muted-foreground">
              {confidencePercent}% {t("confidence")}
            </span>
          </div>
          {expanded ? (
            <ChevronUp className="h-5 w-5 text-muted-foreground" />
          ) : (
            <ChevronDown className="h-5 w-5 text-muted-foreground" />
          )}
        </div>

        <p className="mt-2 font-medium line-clamp-2">
          "{pattern.canonicalQuestion}"
        </p>

        <div className="mt-2 flex items-center gap-4 text-sm text-muted-foreground">
          <span>
            {t("approvals")}: {pattern.approvalCount}/{pattern.totalReviews}
            {pattern.totalReviews > 0 && ` (${approvalPercent}%)`}
          </span>
          {getAutoSubmitStatus()}
        </div>
      </div>

      {/* Expanded Content */}
      {expanded && (
        <div className="p-4 border-t bg-muted/20 space-y-4">
          {/* Canonical Answer */}
          {pattern.canonicalAnswer && (
            <div>
              <p className="text-sm font-medium text-muted-foreground mb-1">
                {t("canonicalAnswer")}
              </p>
              <p className="text-sm bg-background p-3 rounded border">
                {pattern.canonicalAnswer}
              </p>
            </div>
          )}

          {/* Metrics */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">{t("occurrences")}</p>
              <p className="font-semibold">{pattern.occurrenceCount}</p>
            </div>
            <div>
              <p className="text-muted-foreground">{t("approvals")}</p>
              <p className="font-semibold text-green-600">{pattern.approvalCount}</p>
            </div>
            <div>
              <p className="text-muted-foreground">{t("rejections")}</p>
              <p className="font-semibold text-red-600">{pattern.rejectionCount}</p>
            </div>
            <div>
              <p className="text-muted-foreground">{t("modifications")}</p>
              <p className="font-semibold text-yellow-600">{pattern.modificationCount}</p>
            </div>
          </div>

          {/* Last Human Review */}
          {pattern.lastHumanReview && (
            <p className="text-xs text-muted-foreground">
              {t("lastReview")}:{" "}
              {new Date(pattern.lastHumanReview).toLocaleDateString("tr-TR", {
                day: "numeric",
                month: "long",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
              })}
            </p>
          )}

          {/* Actions */}
          <div className="flex gap-2 pt-2">
            {pattern.seniorityLevel !== "EXPERT" && (
              <Button
                size="sm"
                variant="outline"
                onClick={handlePromote}
                disabled={promoteMutation.isPending}
              >
                {promoteMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-1" />
                ) : (
                  <Rocket className="h-4 w-4 mr-1" />
                )}
                {t("promote")}
              </Button>
            )}

            {pattern.seniorityLevel !== "JUNIOR" && (
              <Button
                size="sm"
                variant="outline"
                onClick={handleDemote}
                disabled={demoteMutation.isPending}
              >
                {demoteMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-1" />
                ) : (
                  <ArrowDown className="h-4 w-4 mr-1" />
                )}
                {t("demote")}
              </Button>
            )}

            {pattern.seniorityLevel === "EXPERT" &&
              !pattern.isAutoSubmitEligible && (
                <Button
                  size="sm"
                  onClick={handleEnableAutoSubmit}
                  disabled={enableAutoSubmitMutation.isPending}
                >
                  {enableAutoSubmitMutation.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin mr-1" />
                  ) : (
                    <Rocket className="h-4 w-4 mr-1" />
                  )}
                  {t("activateNow")}
                </Button>
              )}

            <Button size="sm" variant="ghost">
              <Settings className="h-4 w-4 mr-1" />
              {t("settings")}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
