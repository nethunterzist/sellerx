"use client";

import { useImpersonation } from "@/hooks/use-impersonation";
import { AlertTriangle, X, Eye } from "lucide-react";
import { useTranslations } from "next-intl";

export function ImpersonationBanner() {
  const { isImpersonating, targetUserName, targetUserEmail, stopImpersonation } =
    useImpersonation();
  const t = useTranslations("impersonation");

  if (!isImpersonating) return null;

  return (
    <div className="fixed top-0 left-0 right-0 z-[100] bg-red-600 text-white px-4 py-2.5 flex items-center justify-between shadow-lg">
      <div className="flex items-center gap-3">
        <Eye className="h-4 w-4 shrink-0" />
        <span className="text-sm font-medium">
          {t("banner", {
            name: targetUserName || targetUserEmail || "?",
          })}
        </span>
        <span className="inline-flex items-center gap-1 rounded bg-red-800/50 px-2 py-0.5 text-xs font-medium">
          <AlertTriangle className="h-3 w-3" />
          {t("readOnly")}
        </span>
      </div>
      <button
        onClick={stopImpersonation}
        className="flex items-center gap-1.5 rounded bg-white/20 px-3 py-1 text-sm font-medium hover:bg-white/30 transition-colors"
      >
        <X className="h-3.5 w-3.5" />
        {t("stop")}
      </button>
    </div>
  );
}
