"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { CrossSellRuleCard } from "./cross-sell-rule-card";
import {
  useCrossSellRules,
  useDeleteCrossSellRule,
  useUpdateCrossSellRule,
  useReorderCrossSellRules,
} from "@/hooks/queries/use-cross-sell";
import { Loader2, Inbox } from "lucide-react";
import { toast } from "sonner";
import type { CrossSellRule } from "@/types/cross-sell";

interface CrossSellRuleListProps {
  storeId: string;
  onEditRule: (rule: CrossSellRule) => void;
}

export function CrossSellRuleList({
  storeId,
  onEditRule,
}: CrossSellRuleListProps) {
  const t = useTranslations("qa.crossSell.ruleList");
  const { data: rules, isLoading } = useCrossSellRules(storeId);
  const deleteMutation = useDeleteCrossSellRule();
  const updateMutation = useUpdateCrossSellRule();
  const reorderMutation = useReorderCrossSellRules();

  const [deletingId, setDeletingId] = useState<string | null>(null);

  const handleDelete = async (ruleId: string) => {
    if (deletingId) return;

    // Simple confirmation
    const confirmed = window.confirm(t("deleteConfirm"));
    if (!confirmed) return;

    setDeletingId(ruleId);
    try {
      await deleteMutation.mutateAsync({ ruleId, storeId });
      toast.success(t("deleteSuccess"));
    } catch {
      toast.error(t("deleteError"));
    } finally {
      setDeletingId(null);
    }
  };

  const handleToggleStatus = async (ruleId: string, active: boolean) => {
    try {
      await updateMutation.mutateAsync({
        ruleId,
        storeId,
        data: { status: active ? "ACTIVE" : "INACTIVE" },
      });
      toast.success(
        active ? t("activated") : t("deactivated")
      );
    } catch {
      toast.error(t("toggleError"));
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!rules || rules.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
        <Inbox className="h-12 w-12 mb-3 opacity-30" />
        <p className="text-sm">{t("empty")}</p>
        <p className="text-xs mt-1">{t("emptyHint")}</p>
      </div>
    );
  }

  // Sort by priority
  const sortedRules = [...rules].sort((a, b) => a.priority - b.priority);

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          {t("count", { count: rules.length })}
        </p>
      </div>
      <div className="space-y-2">
        {sortedRules.map((rule) => (
          <CrossSellRuleCard
            key={rule.id}
            rule={rule}
            onEdit={onEditRule}
            onDelete={handleDelete}
            onToggleStatus={handleToggleStatus}
          />
        ))}
      </div>
    </div>
  );
}
