"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useQaStats, useSuggestions, useConflicts } from "@/hooks/queries/use-qa";
import { useAiSettings } from "@/hooks/queries/use-ai";
import { AnswerFlowSection } from "@/components/qa/answer-flow-section";
import { KnowledgeHub } from "@/components/qa/knowledge-hub";
import { RulesSection } from "@/components/qa/rules-section";
import { PerformanceSection } from "@/components/qa/performance-section";
import { CrossSellSection } from "@/components/qa/cross-sell";
import { QaSegmentedControl, type QaTabValue } from "@/components/qa/qa-segmented-control";
import { Card, CardContent } from "@/components/ui/card";
import {
  Zap,
  Brain,
  Shield,
  BarChart3,
  MessageSquare,
  ShoppingBag,
} from "lucide-react";
import {
  StatCardSkeleton,
  TableSkeleton,
} from "@/components/ui/skeleton-blocks";

function QaPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-3 grid-cols-2 sm:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => <StatCardSkeleton key={i} />)}
      </div>
      <TableSkeleton columns={5} rows={8} />
    </div>
  );
}

export default function QaPage() {
  const t = useTranslations("qa");
  const { data: selectedStore } = useSelectedStore();

  const [activeTab, setActiveTab] = useState<QaTabValue>("answer-flow");

  const storeId = selectedStore?.selectedStoreId;

  const { isLoading: statsLoading } = useQaStats(storeId || "");
  const { data: aiSettings, isLoading: aiLoading } = useAiSettings(storeId);
  const { data: suggestions } = useSuggestions(storeId || "", "PENDING");
  const { data: conflicts } = useConflicts(storeId || "");

  const suggestionsCount = suggestions?.length ?? 0;
  const activeConflictsCount = conflicts?.filter((c) => c.status === "ACTIVE").length ?? 0;
  const aiEnabled = aiSettings?.aiEnabled ?? false;

  if (!storeId) {
    return (
      <div className="container mx-auto py-8">
        <Card>
          <CardContent className="p-8 text-center text-muted-foreground">
            <MessageSquare className="h-12 w-12 mx-auto mb-4 opacity-50" />
            <p>{t("noStoreSelected")}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (statsLoading && aiLoading) {
    return <QaPageSkeleton />;
  }

  const tabs = [
    {
      id: "answer-flow" as QaTabValue,
      icon: Zap,
      label: t("mainTabs.answerFlow"),
    },
    {
      id: "ai-brain" as QaTabValue,
      icon: Brain,
      label: t("mainTabs.aiBrain"),
      badge: suggestionsCount > 0 ? suggestionsCount : undefined,
    },
    {
      id: "rules" as QaTabValue,
      icon: Shield,
      label: t("mainTabs.rules"),
    },
    {
      id: "performance" as QaTabValue,
      icon: BarChart3,
      label: t("mainTabs.performance"),
      badge: activeConflictsCount > 0 ? activeConflictsCount : undefined,
      badgeVariant: "destructive" as const,
    },
    {
      id: "cross-sell" as QaTabValue,
      icon: ShoppingBag,
      label: t("mainTabs.crossSell"),
    },
  ];

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950">
      {/* Segmented Control Navigation */}
      <QaSegmentedControl
        tabs={tabs}
        activeTab={activeTab}
        onTabChange={setActiveTab}
      />

      {/* Tab Content */}
      <div className="container mx-auto py-6">
        {activeTab === "answer-flow" && (
          <AnswerFlowSection storeId={storeId} aiEnabled={aiEnabled} />
        )}
        {activeTab === "ai-brain" && (
          <KnowledgeHub storeId={storeId} />
        )}
        {activeTab === "rules" && (
          <RulesSection storeId={storeId} />
        )}
        {activeTab === "performance" && (
          <PerformanceSection storeId={storeId} />
        )}
        {activeTab === "cross-sell" && (
          <CrossSellSection storeId={storeId} />
        )}
      </div>
    </div>
  );
}
