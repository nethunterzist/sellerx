"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { GraduationCap, Loader2 } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { usePatterns, useSeniorityStats } from "@/hooks/queries/use-qa";
import { SeniorityStatsCards } from "./seniority-stats-cards";
import { PatternCard } from "./pattern-card";
import type { SeniorityLevel } from "@/types/qa";

interface SeniorityDashboardProps {
  storeId: string;
}

export function SeniorityDashboard({ storeId }: SeniorityDashboardProps) {
  const t = useTranslations("qa.seniority");
  const [filterLevel, setFilterLevel] = useState<SeniorityLevel | "ALL">("ALL");
  const [filterAutoSubmit, setFilterAutoSubmit] = useState<boolean | undefined>(undefined);

  const { data: stats, isLoading: statsLoading } = useSeniorityStats(storeId);
  const { data: patterns, isLoading: patternsLoading } = usePatterns(
    storeId,
    filterLevel === "ALL" ? undefined : filterLevel,
    filterAutoSubmit
  );

  const isLoading = statsLoading || patternsLoading;

  if (isLoading && !stats) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h3 className="text-lg font-semibold flex items-center gap-2">
          <GraduationCap className="h-5 w-5 text-purple-500" />
          {t("title")}
        </h3>
        <p className="text-sm text-muted-foreground">{t("description")}</p>
      </div>

      {/* Stats Cards */}
      {stats && <SeniorityStatsCards stats={stats} />}

      {/* Filters */}
      <div className="flex gap-4 items-center flex-wrap">
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">{t("filter")}:</span>
          <Select
            value={filterLevel}
            onValueChange={(value) => setFilterLevel(value as SeniorityLevel | "ALL")}
          >
            <SelectTrigger className="w-[140px]">
              <SelectValue placeholder={t("all")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">{t("all")}</SelectItem>
              <SelectItem value="JUNIOR">{t("junior")}</SelectItem>
              <SelectItem value="LEARNING">{t("learning")}</SelectItem>
              <SelectItem value="SENIOR">{t("senior")}</SelectItem>
              <SelectItem value="EXPERT">{t("expert")}</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="flex items-center gap-2">
          <Select
            value={filterAutoSubmit === undefined ? "ALL" : filterAutoSubmit ? "true" : "false"}
            onValueChange={(value) => {
              if (value === "ALL") {
                setFilterAutoSubmit(undefined);
              } else {
                setFilterAutoSubmit(value === "true");
              }
            }}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder={t("autoSubmitFilter")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">{t("allAutoSubmit")}</SelectItem>
              <SelectItem value="true">{t("autoSubmitOnly")}</SelectItem>
              <SelectItem value="false">{t("notAutoSubmit")}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Pattern List */}
      {patternsLoading ? (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      ) : !patterns || patterns.length === 0 ? (
        <div className="text-center py-12">
          <GraduationCap className="h-12 w-12 mx-auto text-muted-foreground/50 mb-4" />
          <p className="text-muted-foreground">{t("noPatterns")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {patterns.map((pattern) => (
            <PatternCard key={pattern.id} pattern={pattern} />
          ))}
        </div>
      )}
    </div>
  );
}
