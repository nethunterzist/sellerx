"use client";

import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";
import type { LucideIcon } from "lucide-react";

interface FooterStat {
  label: string;
  value: string | number;
}

interface MetricCardProps {
  title: string;
  subtitle?: string;
  icon: LucideIcon;
  headerColor: string;
  metricLabel?: string;
  metricValue: string | number;
  metricColor?: string;
  metricSuffix?: string;
  footerStats?: FooterStat[];
  children?: React.ReactNode;
  isLoading?: boolean;
}

export function MetricCard({
  title,
  subtitle,
  icon: Icon,
  headerColor,
  metricLabel,
  metricValue,
  metricColor,
  metricSuffix,
  footerStats,
  children,
  isLoading,
}: MetricCardProps) {
  if (isLoading) {
    return <MetricCardSkeleton />;
  }

  return (
    <div className="flex flex-col rounded-lg overflow-hidden min-w-[220px] flex-1 bg-card border border-border shadow-sm hover:shadow-md transition-all">
      {/* Colored Header */}
      <div className={cn("px-4 py-3", headerColor)}>
        <div className="flex items-center gap-2">
          <Icon className="h-4 w-4 text-white/80" />
          <h3 className="text-sm font-semibold text-white">{title}</h3>
        </div>
        {subtitle && (
          <p className="text-xs text-white/70 mt-0.5">{subtitle}</p>
        )}
      </div>

      {/* Body */}
      <div className="p-4 flex flex-col flex-1">
        {metricLabel && (
          <span className="text-xs text-muted-foreground">{metricLabel}</span>
        )}
        <p className={cn("text-2xl font-bold", metricColor || "text-foreground")}>
          {metricValue}
          {metricSuffix && (
            <span className="text-sm font-normal text-muted-foreground ml-1">
              {metricSuffix}
            </span>
          )}
        </p>

        {children}

        {/* Footer */}
        {footerStats && footerStats.length > 0 && (
          <div className="flex justify-between text-xs border-t border-border pt-3 mt-auto">
            {footerStats.map((stat, i) => (
              <div key={i} className={i > 0 ? "text-right" : ""}>
                <span className="text-muted-foreground">{stat.label}</span>
                <p className="font-semibold text-foreground">{stat.value}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export function MetricCardSkeleton() {
  return (
    <div className="flex flex-col rounded-lg overflow-hidden min-w-[220px] flex-1 bg-card border border-border shadow-sm">
      <div className="px-4 py-3 bg-muted animate-pulse">
        <Skeleton className="h-4 w-24 bg-muted-foreground/20" />
        <Skeleton className="h-3 w-32 bg-muted-foreground/20 mt-1" />
      </div>
      <div className="p-4">
        <Skeleton className="h-3 w-16 mb-2" />
        <Skeleton className="h-8 w-28 mb-4" />
        <div className="flex justify-between border-t border-border pt-3">
          <Skeleton className="h-4 w-16" />
          <Skeleton className="h-4 w-20" />
        </div>
      </div>
    </div>
  );
}
