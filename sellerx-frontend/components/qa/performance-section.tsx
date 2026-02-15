"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  useSeniorityStats,
  usePatterns,
  useConflicts,
  useConflictStats,
  useQaStats,
  usePromotePattern,
  useDemotePattern,
  useEnableAutoSubmit,
} from "@/hooks/queries/use-qa";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { NumberTicker } from "@/components/motion/number-ticker";
import { StaggerChildren } from "@/components/motion";
import { ConflictAlertBanner } from "./conflict-alert-banner";
import { ConflictDetailModal } from "./conflict-detail-modal";
import { SeniorityProgress, getSeniorityLevel } from "./seniority-progress";
import {
  TrendingUp,
  Rocket,
  AlertTriangle,
  GraduationCap,
  Loader2,
  ArrowDown,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import type { SeniorityLevel, ConflictAlert, QaPattern } from "@/types/qa";

interface PerformanceSectionProps {
  storeId: string;
}

function PatternItem({ pattern }: { pattern: QaPattern }) {
  const t = useTranslations("qa.seniority");
  const [expanded, setExpanded] = useState(false);
  const promoteMutation = usePromotePattern();
  const demoteMutation = useDemotePattern();
  const autoSubmitMutation = useEnableAutoSubmit();

  const seniorityConfig: Record<SeniorityLevel, { icon: string; color: string }> = {
    JUNIOR: { icon: "○", color: "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300" },
    LEARNING: { icon: "◐", color: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300" },
    SENIOR: { icon: "●", color: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300" },
    EXPERT: { icon: "★", color: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300" },
  };

  const { icon, color } = seniorityConfig[pattern.seniorityLevel];
  const confidencePercent = Math.round(pattern.confidenceScore * 100);
  const approvalPercent = Math.round(pattern.approvalRate * 100);

  return (
    <div className="group border rounded-lg overflow-hidden bg-card hover:shadow-sm transition-shadow duration-200">
      <div
        className="p-3 cursor-pointer hover:bg-muted/30 transition-colors"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <SeniorityProgress
              level={pattern.seniorityLevel}
              progress={confidencePercent}
              variant="ring"
              size="sm"
              showLabel={false}
            />
            <div className="flex flex-col gap-1">
              <span className="text-xs font-medium">{t(pattern.seniorityLevel.toLowerCase())}</span>
              <span className="text-xs text-muted-foreground">{confidencePercent}% {t("confidence")}</span>
            </div>
            {pattern.isAutoSubmitEligible && (
              <Badge variant="outline" className="text-xs bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400">
                Auto
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">
              {t("approvalRate")}: {approvalPercent}%
            </span>
            {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          </div>
        </div>
        <p className="mt-1.5 text-sm font-medium line-clamp-1">"{pattern.canonicalQuestion}"</p>
      </div>

      {expanded && (
        <div className="p-3 border-t bg-muted/20 space-y-3">
          {pattern.canonicalAnswer && (
            <p className="text-sm bg-background p-2 rounded border line-clamp-3">
              {pattern.canonicalAnswer}
            </p>
          )}
          <div className="flex gap-2 flex-wrap">
            {pattern.seniorityLevel !== "EXPERT" && (
              <Button
                size="sm"
                variant="outline"
                onClick={() => promoteMutation.mutate(pattern.id)}
                disabled={promoteMutation.isPending}
              >
                {promoteMutation.isPending ? (
                  <Loader2 className="h-3 w-3 animate-spin mr-1" />
                ) : (
                  <Rocket className="h-3 w-3 mr-1" />
                )}
                {t("promote")}
              </Button>
            )}
            {pattern.seniorityLevel !== "JUNIOR" && (
              <Button
                size="sm"
                variant="outline"
                onClick={() => demoteMutation.mutate({ patternId: pattern.id })}
                disabled={demoteMutation.isPending}
              >
                {demoteMutation.isPending ? (
                  <Loader2 className="h-3 w-3 animate-spin mr-1" />
                ) : (
                  <ArrowDown className="h-3 w-3 mr-1" />
                )}
                {t("demote")}
              </Button>
            )}
            {pattern.seniorityLevel === "EXPERT" && !pattern.isAutoSubmitEligible && (
              <Button
                size="sm"
                onClick={() => autoSubmitMutation.mutate(pattern.id)}
                disabled={autoSubmitMutation.isPending}
              >
                {autoSubmitMutation.isPending ? (
                  <Loader2 className="h-3 w-3 animate-spin mr-1" />
                ) : (
                  <Rocket className="h-3 w-3 mr-1" />
                )}
                {t("activateNow")}
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export function PerformanceSection({ storeId }: PerformanceSectionProps) {
  const t = useTranslations("qa.performance");
  const tSeniority = useTranslations("qa.seniority");
  const tConflicts = useTranslations("qa.conflicts");

  const { data: seniorityStats } = useSeniorityStats(storeId);
  const { data: conflictStats } = useConflictStats(storeId);
  const { data: conflicts } = useConflicts(storeId);
  const { data: qaStats } = useQaStats(storeId);

  const [filterLevel, setFilterLevel] = useState<SeniorityLevel | "ALL">("ALL");
  const [filterAutoSubmit, setFilterAutoSubmit] = useState<boolean | undefined>(undefined);
  const [selectedConflict, setSelectedConflict] = useState<ConflictAlert | null>(null);
  const [conflictModalOpen, setConflictModalOpen] = useState(false);

  const { data: patterns, isLoading: patternsLoading } = usePatterns(
    storeId,
    filterLevel === "ALL" ? undefined : filterLevel,
    filterAutoSubmit
  );

  const activeConflicts = conflicts?.filter((c) => c.status === "ACTIVE") || [];

  const avgApprovalRate =
    patterns && patterns.length > 0
      ? Math.round(
          (patterns.reduce((sum, p) => sum + p.approvalRate, 0) / patterns.length) * 100
        )
      : 0;

  return (
    <StaggerChildren className="space-y-6">
      {/* Summary Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card className="glass-card hover:shadow-md transition-shadow duration-200">
          <CardContent className="pt-4 pb-3 px-4 text-center">
            <p className="text-xs text-muted-foreground mb-1">{t("totalPatterns")}</p>
            <NumberTicker
              value={seniorityStats?.totalPatterns ?? 0}
              className="text-2xl font-bold"
            />
          </CardContent>
        </Card>
        <Card className="glass-card hover:shadow-md transition-shadow duration-200">
          <CardContent className="pt-4 pb-3 px-4 text-center">
            <p className="text-xs text-muted-foreground mb-1">{t("autoSubmitEligible")}</p>
            <NumberTicker
              value={seniorityStats?.autoSubmitEligibleCount ?? 0}
              className="text-2xl font-bold text-green-600"
            />
          </CardContent>
        </Card>
        <Card className="glass-card hover:shadow-md transition-shadow duration-200">
          <CardContent className="pt-4 pb-3 px-4 text-center">
            <p className="text-xs text-muted-foreground mb-1">{t("activeAlerts")}</p>
            <NumberTicker
              value={conflictStats?.totalActive ?? 0}
              className="text-2xl font-bold text-orange-600"
            />
          </CardContent>
        </Card>
        <Card className="glass-card hover:shadow-md transition-shadow duration-200">
          <CardContent className="pt-4 pb-3 px-4 text-center">
            <p className="text-xs text-muted-foreground mb-1">{t("avgApprovalRate")}</p>
            <NumberTicker
              value={avgApprovalRate}
              suffix="%"
              className="text-2xl font-bold text-blue-600"
            />
          </CardContent>
        </Card>
      </div>

      {/* Seniority Status */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <GraduationCap className="h-5 w-5 text-purple-500" />
            {t("seniorityStatus")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Distribution Bar */}
          {seniorityStats && seniorityStats.totalPatterns > 0 && (
            <div className="space-y-2">
              <div className="flex h-3 rounded-full overflow-hidden">
                {seniorityStats.juniorCount > 0 && (
                  <div
                    className="bg-gray-400"
                    style={{ width: `${(seniorityStats.juniorCount / seniorityStats.totalPatterns) * 100}%` }}
                  />
                )}
                {seniorityStats.learningCount > 0 && (
                  <div
                    className="bg-blue-400"
                    style={{ width: `${(seniorityStats.learningCount / seniorityStats.totalPatterns) * 100}%` }}
                  />
                )}
                {seniorityStats.seniorCount > 0 && (
                  <div
                    className="bg-green-400"
                    style={{ width: `${(seniorityStats.seniorCount / seniorityStats.totalPatterns) * 100}%` }}
                  />
                )}
                {seniorityStats.expertCount > 0 && (
                  <div
                    className="bg-yellow-400"
                    style={{ width: `${(seniorityStats.expertCount / seniorityStats.totalPatterns) * 100}%` }}
                  />
                )}
              </div>
              <div className="flex justify-between text-xs text-muted-foreground">
                <span>○ Junior: {seniorityStats.juniorCount}</span>
                <span>◐ Learning: {seniorityStats.learningCount}</span>
                <span>● Senior: {seniorityStats.seniorCount}</span>
                <span>★ Expert: {seniorityStats.expertCount}</span>
              </div>
            </div>
          )}

          {/* Filters */}
          <div className="flex gap-3 items-center flex-wrap">
            <Select
              value={filterLevel}
              onValueChange={(v) => setFilterLevel(v as SeniorityLevel | "ALL")}
            >
              <SelectTrigger className="w-[140px]">
                <SelectValue placeholder={tSeniority("all")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">{tSeniority("all")}</SelectItem>
                <SelectItem value="JUNIOR">{tSeniority("junior")}</SelectItem>
                <SelectItem value="LEARNING">{tSeniority("learning")}</SelectItem>
                <SelectItem value="SENIOR">{tSeniority("senior")}</SelectItem>
                <SelectItem value="EXPERT">{tSeniority("expert")}</SelectItem>
              </SelectContent>
            </Select>

            <div className="flex items-center gap-2">
              <Switch
                checked={filterAutoSubmit === true}
                onCheckedChange={(v) => setFilterAutoSubmit(v ? true : undefined)}
              />
              <span className="text-sm text-muted-foreground">
                {tSeniority("autoSubmitOnly")}
              </span>
            </div>
          </div>

          {/* Pattern List */}
          {patternsLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : !patterns || patterns.length === 0 ? (
            <div className="text-center py-8">
              <GraduationCap className="h-10 w-10 mx-auto text-muted-foreground/50 mb-3" />
              <p className="text-sm text-muted-foreground">{tSeniority("noPatterns")}</p>
            </div>
          ) : (
            <div className="space-y-2">
              {patterns.map((pattern) => (
                <PatternItem key={pattern.id} pattern={pattern} />
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Alerts */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-yellow-500" />
            {t("alerts")}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {activeConflicts.length === 0 ? (
            <div className="text-center py-8">
              <AlertTriangle className="h-10 w-10 mx-auto text-muted-foreground/50 mb-3" />
              <p className="text-sm text-muted-foreground">{t("noAlerts")}</p>
            </div>
          ) : (
            <div className="space-y-3">
              {activeConflicts
                .sort((a, b) => {
                  const order = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
                  return order[a.severity] - order[b.severity];
                })
                .map((conflict) => (
                  <ConflictAlertBanner
                    key={conflict.id}
                    alert={conflict}
                    onViewDetails={() => {
                      setSelectedConflict(conflict);
                      setConflictModalOpen(true);
                    }}
                  />
                ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ConflictDetailModal
        conflict={selectedConflict}
        open={conflictModalOpen}
        onClose={() => {
          setConflictModalOpen(false);
          setSelectedConflict(null);
        }}
      />
    </StaggerChildren>
  );
}
