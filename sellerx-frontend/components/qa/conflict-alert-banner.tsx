"use client";

import { X, AlertTriangle, Scale, Heart, BookX, Tag } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { ConflictAlert, ConflictType, ConflictSeverity } from "@/types/qa";

interface ConflictAlertBannerProps {
  alert: ConflictAlert;
  onResolve?: () => void;
  onDismiss?: () => void;
  onViewDetails?: () => void;
}

export function ConflictAlertBanner({
  alert,
  onResolve,
  onDismiss,
  onViewDetails,
}: ConflictAlertBannerProps) {
  const t = useTranslations("qa.conflicts");

  const getSeverityStyles = (severity: ConflictSeverity) => {
    switch (severity) {
      case "CRITICAL":
        return "bg-red-50 border-red-200 dark:bg-red-950/30 dark:border-red-900";
      case "HIGH":
        return "bg-orange-50 border-orange-200 dark:bg-orange-950/30 dark:border-orange-900";
      case "MEDIUM":
        return "bg-yellow-50 border-yellow-200 dark:bg-yellow-950/30 dark:border-yellow-900";
      case "LOW":
        return "bg-blue-50 border-blue-200 dark:bg-blue-950/30 dark:border-blue-900";
      default:
        return "bg-gray-50 border-gray-200 dark:bg-gray-950/30 dark:border-gray-800";
    }
  };

  const getSeverityBadge = (severity: ConflictSeverity) => {
    const styles = {
      CRITICAL: "bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-300",
      HIGH: "bg-orange-100 text-orange-800 dark:bg-orange-900/50 dark:text-orange-300",
      MEDIUM: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/50 dark:text-yellow-300",
      LOW: "bg-blue-100 text-blue-800 dark:bg-blue-900/50 dark:text-blue-300",
    };

    return (
      <span className={`px-2 py-1 text-xs font-bold rounded-full uppercase ${styles[severity]}`}>
        {t(severity.toLowerCase())}
      </span>
    );
  };

  const getTypeIcon = (type: ConflictType) => {
    switch (type) {
      case "LEGAL_RISK":
        return <Scale className="h-5 w-5" />;
      case "HEALTH_SAFETY":
        return <Heart className="h-5 w-5" />;
      case "KNOWLEDGE_CONFLICT":
        return <BookX className="h-5 w-5" />;
      case "BRAND_INCONSISTENCY":
        return <Tag className="h-5 w-5" />;
      default:
        return <AlertTriangle className="h-5 w-5" />;
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

  return (
    <div className={`border rounded-lg p-4 ${getSeverityStyles(alert.severity)}`}>
      <div className="flex items-start gap-3">
        <div className="flex-shrink-0 text-red-500">
          {getTypeIcon(alert.conflictType)}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            {getSeverityBadge(alert.severity)}
            <span className="text-sm font-medium">
              {getTypeLabel(alert.conflictType)}
            </span>
          </div>

          {alert.customerQuestion && (
            <p className="text-sm text-muted-foreground mb-2">
              {t("customerQuestion")}: "{alert.customerQuestion}"
            </p>
          )}

          {alert.conflictType === "LEGAL_RISK" || alert.conflictType === "HEALTH_SAFETY" ? (
            <p className="text-sm text-red-700 dark:text-red-400">
              {t("cannotAutoAnswer")}
            </p>
          ) : (
            <p className="text-sm text-muted-foreground line-clamp-2">
              {alert.sourceAContent}
            </p>
          )}

          {alert.detectedKeywords && alert.detectedKeywords.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-2">
              {alert.detectedKeywords.map((keyword, idx) => (
                <span
                  key={idx}
                  className="px-2 py-0.5 text-xs bg-gray-200 dark:bg-gray-700 rounded"
                >
                  {keyword}
                </span>
              ))}
            </div>
          )}
        </div>

        <div className="flex-shrink-0 flex items-start gap-2">
          {onViewDetails && (
            <Button
              size="sm"
              variant="outline"
              onClick={onViewDetails}
            >
              {t("viewDetails")}
            </Button>
          )}
          {onResolve && (
            <Button
              size="sm"
              variant="default"
              onClick={onResolve}
            >
              {t("resolve")}
            </Button>
          )}
          {onDismiss && (
            <Button
              size="sm"
              variant="ghost"
              onClick={onDismiss}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
