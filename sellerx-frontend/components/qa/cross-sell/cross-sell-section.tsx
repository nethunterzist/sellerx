"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { CrossSellGlobalToggle } from "./cross-sell-global-toggle";
import { CrossSellRuleList } from "./cross-sell-rule-list";
import { CrossSellRuleBuilder } from "./cross-sell-rule-builder";
import { CrossSellPreviewPanel } from "./cross-sell-preview-panel";
import {
  useCrossSellSettings,
  useCrossSellAnalytics,
} from "@/hooks/queries/use-cross-sell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Plus,
  BarChart3,
  Eye,
  MousePointerClick,
  TrendingUp,
} from "lucide-react";
import { StaggerChildren } from "@/components/motion";
import type { CrossSellRule } from "@/types/cross-sell";

interface CrossSellSectionProps {
  storeId: string;
}

export function CrossSellSection({ storeId }: CrossSellSectionProps) {
  const t = useTranslations("qa.crossSell");
  const { data: settings } = useCrossSellSettings(storeId);
  const { data: analytics } = useCrossSellAnalytics(storeId);

  const [builderOpen, setBuilderOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<CrossSellRule | undefined>();

  const handleEditRule = (rule: CrossSellRule) => {
    setEditingRule(rule);
    setBuilderOpen(true);
  };

  const handleCreateNew = () => {
    setEditingRule(undefined);
    setBuilderOpen(true);
  };

  const handleBuilderClose = (open: boolean) => {
    setBuilderOpen(open);
    if (!open) {
      setEditingRule(undefined);
    }
  };

  return (
    <StaggerChildren className="space-y-6">
      {/* Global toggle */}
      <CrossSellGlobalToggle storeId={storeId} />

      {/* Analytics cards */}
      {analytics && (
        <div className="grid gap-3 grid-cols-2 sm:grid-cols-4">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-2 text-muted-foreground mb-1">
                <Eye className="h-3.5 w-3.5" />
                <span className="text-xs">{t("analytics.impressions")}</span>
              </div>
              <p className="text-xl font-semibold">
                {analytics.totalImpressions.toLocaleString("tr-TR")}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-2 text-muted-foreground mb-1">
                <MousePointerClick className="h-3.5 w-3.5" />
                <span className="text-xs">{t("analytics.clicks")}</span>
              </div>
              <p className="text-xl font-semibold">
                {analytics.totalClicks.toLocaleString("tr-TR")}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-2 text-muted-foreground mb-1">
                <TrendingUp className="h-3.5 w-3.5" />
                <span className="text-xs">{t("analytics.conversions")}</span>
              </div>
              <p className="text-xl font-semibold">
                {analytics.totalConversions.toLocaleString("tr-TR")}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-2 text-muted-foreground mb-1">
                <BarChart3 className="h-3.5 w-3.5" />
                <span className="text-xs">{t("analytics.conversionRate")}</span>
              </div>
              <p className="text-xl font-semibold">
                %{(analytics.overallConversionRate * 100).toFixed(1)}
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Rules section */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-3">
          <CardTitle className="text-base">{t("rulesTitle")}</CardTitle>
          <Button size="sm" onClick={handleCreateNew} className="gap-1.5">
            <Plus className="h-4 w-4" />
            {t("createRule")}
          </Button>
        </CardHeader>
        <CardContent>
          <CrossSellRuleList
            storeId={storeId}
            onEditRule={handleEditRule}
          />
        </CardContent>
      </Card>

      {/* Rule builder modal */}
      <CrossSellRuleBuilder
        open={builderOpen}
        onOpenChange={handleBuilderClose}
        storeId={storeId}
        editingRule={editingRule}
        settings={settings}
      />
    </StaggerChildren>
  );
}
