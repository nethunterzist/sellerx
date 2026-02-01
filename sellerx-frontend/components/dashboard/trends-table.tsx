"use client";

import { cn } from "@/lib/utils";
import { Store, ExternalLink } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Sparkline } from "./sparkline";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  getHeatmapCellStyle,
  calculateChange,
  formatChangeText,
  formatCellValue,
} from "@/lib/utils/heatmap";
import { useCurrency } from "@/lib/contexts/currency-context";
import type { ProductTrendData, ProductPeriodMetrics } from "./trends-view";

export type TrendMetricType =
  | "revenue"
  | "units"
  | "orders"
  | "refunds"
  | "commission"
  | "profit"
  | "netProfit"
  | "margin";

interface TrendsTableProps {
  products: ProductTrendData[];
  metric: TrendMetricType;
  isLoading?: boolean;
}

type PeriodKey = "today" | "yesterday" | "thisMonth" | "lastMonth";

const periodLabels: Record<PeriodKey, string> = {
  today: "Bugün",
  yesterday: "Dün",
  thisMonth: "Bu Ay",
  lastMonth: "Geçen Ay",
};

const periodOrder: PeriodKey[] = ["today", "yesterday", "thisMonth", "lastMonth"];

// Check if metric is currency-based
function isCurrencyMetric(metric: TrendMetricType): boolean {
  return ["revenue", "commission", "profit", "netProfit"].includes(metric);
}

// Check if metric is percentage-based
function isPercentageMetric(metric: TrendMetricType): boolean {
  return metric === "margin";
}

function formatValueWithCurrency(
  value: number,
  metric: TrendMetricType,
  formatCurrency?: (amount: number) => string
): string {
  if (isPercentageMetric(metric)) {
    return `${value.toFixed(1)}%`;
  }
  return formatCellValue(value, isCurrencyMetric(metric), formatCurrency);
}

function TrendsTableSkeleton() {
  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="bg-muted border-b border-border">
            <th className="sticky left-0 z-10 bg-muted px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider min-w-[200px]">
              Ürün
            </th>
            <th className="px-3 py-3 text-center text-xs font-semibold text-muted-foreground uppercase tracking-wider w-[80px]">
              Trend
            </th>
            {periodOrder.map((p) => (
              <th key={p} className="px-3 py-3 text-center text-xs font-semibold text-muted-foreground uppercase tracking-wider min-w-[120px]">
                {periodLabels[p]}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {[...Array(8)].map((_, i) => (
            <tr key={i}>
              <td className="sticky left-0 z-10 bg-card px-4 py-3">
                <div className="flex items-center gap-3">
                  <Skeleton className="h-10 w-10 rounded" />
                  <div className="space-y-1">
                    <Skeleton className="h-4 w-32" />
                    <Skeleton className="h-3 w-20" />
                  </div>
                </div>
              </td>
              <td className="px-3 py-3">
                <Skeleton className="h-6 w-16 mx-auto" />
              </td>
              {periodOrder.map((p) => (
                <td key={p} className="px-3 py-3">
                  <Skeleton className="h-12 w-full rounded" />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// Sellerboard-style heatmap cell component
function HeatmapCell({
  currentValue,
  previousValue,
  metric,
  showChange = true,
  formatCurrency,
}: {
  currentValue: number;
  previousValue: number;
  metric: TrendMetricType;
  showChange?: boolean;
  formatCurrency?: (amount: number) => string;
}) {
  const change = calculateChange(currentValue, previousValue);
  const cellStyle = getHeatmapCellStyle(change);
  const formattedValue = formatValueWithCurrency(currentValue, metric, formatCurrency);

  return (
    <div
      className={cn(
        "px-3 py-2 rounded-md text-center transition-all",
        cellStyle
      )}
    >
      <div className="font-semibold text-sm">{formattedValue}</div>
      {showChange && (
        <div className="text-xs opacity-90 mt-0.5">
          {formatChangeText(change)}
        </div>
      )}
    </div>
  );
}

export function TrendsTable({ products, metric, isLoading }: TrendsTableProps) {
  const { formatCurrency } = useCurrency();

  if (isLoading) {
    return <TrendsTableSkeleton />;
  }

  if (!products || products.length === 0) {
    return (
      <div className="p-12 text-center text-muted-foreground">
        <Store className="h-12 w-12 mx-auto mb-3 text-muted-foreground/50" />
        <p className="text-sm">Trend verisi bulunamadı</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="bg-muted border-b border-border">
            <th className="sticky left-0 z-10 bg-muted px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider min-w-[220px] shadow-[2px_0_5px_-2px_hsl(var(--border))]">
              Ürün
            </th>
            <th className="px-3 py-3 text-center text-xs font-semibold text-muted-foreground uppercase tracking-wider w-[90px]">
              Trend
            </th>
            {periodOrder.map((p) => (
              <th key={p} className="px-3 py-3 text-center text-xs font-semibold text-muted-foreground uppercase tracking-wider min-w-[130px]">
                {periodLabels[p]}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {products.map((product) => {
            // Sparkline için veri hazırla (tersine çevir - son dönem sağda)
            const sparklineData = [...periodOrder]
              .reverse()
              .map((p) => ({
                value: product.periods[p]?.[metric] || 0,
              }));

            // Check if sparkline trend is positive
            const firstValue = sparklineData[0]?.value || 0;
            const lastValue = sparklineData[sparklineData.length - 1]?.value || 0;
            const isPositiveTrend = lastValue >= firstValue;

            return (
              <tr key={product.barcode} className="hover:bg-muted/50 transition-colors">
                {/* Sticky Product Column */}
                <td className="sticky left-0 z-10 bg-card px-4 py-3 shadow-[2px_0_5px_-2px_hsl(var(--border))]">
                  <div className="flex items-center gap-3">
                    {/* Product Image - Trendyol Link */}
                    {product.productUrl ? (
                      <a
                        href={product.productUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex-shrink-0 group relative"
                      >
                        {product.image ? (
                          <img
                            src={product.image}
                            alt={product.name}
                            className="h-10 w-10 rounded-md object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                            onError={(e) => {
                              (e.target as HTMLImageElement).style.display = "none";
                            }}
                          />
                        ) : (
                          <div className="h-10 w-10 bg-[#F27A1A] rounded-md flex items-center justify-center text-xs font-bold text-white group-hover:ring-2 ring-[#F27A1A] transition-all">
                            T
                          </div>
                        )}
                        <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                          <ExternalLink className="h-2.5 w-2.5 text-white" />
                        </div>
                      </a>
                    ) : product.image ? (
                      <img
                        src={product.image}
                        alt={product.name}
                        className="h-10 w-10 rounded-md object-cover border border-border flex-shrink-0"
                        onError={(e) => {
                          (e.target as HTMLImageElement).style.display = "none";
                        }}
                      />
                    ) : (
                      <div className="h-10 w-10 bg-muted rounded-md flex items-center justify-center flex-shrink-0">
                        <Store className="h-5 w-5 text-muted-foreground" />
                      </div>
                    )}
                    <div className="min-w-0">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          {product.productUrl ? (
                            <a
                              href={product.productUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-sm font-medium text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer truncate max-w-[140px] block"
                            >
                              {product.name}
                            </a>
                          ) : (
                            <p className="text-sm font-medium text-foreground truncate max-w-[140px] cursor-default">
                              {product.name}
                            </p>
                          )}
                        </TooltipTrigger>
                        <TooltipContent>
                          <p>{product.name}</p>
                        </TooltipContent>
                      </Tooltip>
                      <p className="text-xs text-muted-foreground font-mono">{product.barcode}</p>
                    </div>
                  </div>
                </td>

                {/* Sparkline Column */}
                <td className="px-3 py-3">
                  <div className="flex justify-center">
                    <Sparkline
                      data={sparklineData}
                      color={isPositiveTrend ? "#10B981" : "#F59E0B"}
                      width={70}
                      height={28}
                    />
                  </div>
                </td>

                {/* Period Heatmap Cells */}
                {periodOrder.map((period, index) => {
                  const currentValue = product.periods[period]?.[metric] || 0;
                  const previousPeriod = periodOrder[index + 1];
                  const previousValue = previousPeriod
                    ? product.periods[previousPeriod]?.[metric] || 0
                    : currentValue; // If no previous period, use current (no change)

                  const showChange = !!previousPeriod;

                  return (
                    <td key={period} className="px-2 py-2">
                      <HeatmapCell
                        currentValue={currentValue}
                        previousValue={previousValue}
                        metric={metric}
                        showChange={showChange}
                        formatCurrency={formatCurrency}
                      />
                    </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
