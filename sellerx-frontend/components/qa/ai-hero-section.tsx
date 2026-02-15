"use client";

import { useTranslations } from "next-intl";
import { useUpdateAiSettings } from "@/hooks/queries/use-ai";
import { NumberTicker } from "@/components/motion/number-ticker";
import { FadeIn, StaggerChildren } from "@/components/motion";
import { Switch } from "@/components/ui/switch";
import { Card, CardContent } from "@/components/ui/card";
import type { AiSettings } from "@/types/ai";
import type { QaStats } from "@/types/qa";
import { Sparkles, Clock, MessageSquare, TrendingUp, Loader2 } from "lucide-react";

interface AiHeroSectionProps {
  storeId: string;
  aiSettings: AiSettings | undefined;
  stats: QaStats | undefined;
  isLoading: boolean;
}

export function AiHeroSection({ storeId, aiSettings, stats, isLoading }: AiHeroSectionProps) {
  const t = useTranslations("qa.hero");
  const updateSettings = useUpdateAiSettings();

  const isActive = aiSettings?.aiEnabled ?? false;

  const handleToggle = (checked: boolean) => {
    updateSettings.mutate({ storeId, data: { aiEnabled: checked } });
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-16">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </CardContent>
      </Card>
    );
  }

  const metrics = [
    {
      icon: MessageSquare,
      label: t("answered"),
      value: stats?.answeredQuestions ?? 0,
    },
    {
      icon: Clock,
      label: t("pending"),
      value: stats?.pendingQuestions ?? 0,
    },
    {
      icon: TrendingUp,
      label: t("avgConfidence"),
      value: Math.round((aiSettings?.confidenceThreshold ?? 0) * 100),
      suffix: "%",
    },
    {
      icon: Clock,
      label: t("timeSaved"),
      value: (stats?.answeredQuestions ?? 0) * 2,
      suffix: " dk",
    },
  ];

  return (
    <FadeIn>
      <Card className="overflow-hidden">
        {/* Gradient Header */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-6 py-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-white/15 p-2">
                <Sparkles className="h-5 w-5 text-white" />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-white">{t("title")}</h2>
                <div className="flex items-center gap-2 mt-0.5">
                  <span
                    className={`h-2 w-2 rounded-full ${
                      isActive ? "bg-green-400 animate-pulse" : "bg-gray-400"
                    }`}
                  />
                  <span className="text-sm text-white/80">
                    {isActive ? t("aiActive") : t("aiInactive")}
                  </span>
                </div>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Switch
                checked={isActive}
                onCheckedChange={handleToggle}
                disabled={updateSettings.isPending}
              />
            </div>
          </div>
        </div>

        {/* Metrics Grid */}
        <CardContent className="p-4">
          <StaggerChildren className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            {metrics.map((metric) => (
              <div
                key={metric.label}
                className="rounded-lg border bg-muted/30 p-3 text-center"
              >
                <metric.icon className="mx-auto mb-1.5 h-4 w-4 text-muted-foreground" />
                <div className="text-2xl font-bold tracking-tight">
                  <NumberTicker value={metric.value} suffix={metric.suffix} />
                </div>
                <p className="text-xs text-muted-foreground mt-0.5">{metric.label}</p>
              </div>
            ))}
          </StaggerChildren>
        </CardContent>
      </Card>
    </FadeIn>
  );
}
