"use client";

import { useState, useMemo } from "react";
import {
  Table,
  TableBody,
  TableHead,
  TableHeader,
  TableRow,
  TableCell,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ChevronRight, ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { useCurrency } from "@/lib/contexts/currency-context";
import { useMultiPeriodStats } from "@/hooks/useDashboardStats";
import type {
  PeriodStats,
  PLPeriodPreset,
  PLPeriodType,
} from "@/types/dashboard";
import { PL_PERIOD_PRESETS } from "@/types/dashboard";

interface PLViewProps {
  storeId?: string;
}

// Metric row definition
interface MetricRow {
  key: string;
  label: string;
  field: keyof PeriodStats | "calculated";
  isNegative?: boolean;
  isBold?: boolean;
  isExpandable?: boolean;
  isCurrency?: boolean;
  isPercentage?: boolean;
  children?: MetricRow[];
  getValue?: (period: PeriodStats) => number;
}

const METRIC_ROWS: MetricRow[] = [
  { key: "revenue", label: "Satışlar", field: "totalRevenue", isBold: true },
  { key: "units", label: "Adet", field: "totalProductsSold", isCurrency: false },
  { key: "orders", label: "Sipariş", field: "totalOrders", isCurrency: false },
  {
    key: "returns",
    label: "İadeler",
    field: "returnCount",
    isCurrency: false,
    isExpandable: true,
    children: [
      { key: "returnCost", label: "İade Maliyeti", field: "returnCost", isNegative: true },
    ],
  },
  { key: "commission", label: "Komisyon", field: "totalEstimatedCommission", isNegative: true },
  { key: "productCosts", label: "Ürün Maliyeti", field: "totalProductCosts", isNegative: true },
  { key: "shippingCost", label: "Kargo Maliyeti", field: "totalShippingCost", isNegative: true },
  { key: "stoppage", label: "Stopaj", field: "totalStoppage", isNegative: true },
  { key: "vatDifference", label: "KDV Farkı", field: "vatDifference" },
  { key: "expenses", label: "Giderler", field: "totalExpenseAmount", isNegative: true },
  { key: "divider1", label: "divider", field: "calculated" },
  { key: "grossProfit", label: "Brüt Kâr", field: "grossProfit", isBold: true },
  { key: "netProfit", label: "Net Kâr", field: "netProfit", isBold: true },
  { key: "divider2", label: "divider", field: "calculated" },
  { key: "margin", label: "Marj", field: "profitMargin", isBold: true, isCurrency: false, isPercentage: true },
  { key: "roi", label: "ROI", field: "roi", isBold: true, isCurrency: false, isPercentage: true },
];

function PLViewSkeleton({ columnCount = 12 }: { columnCount?: number }) {
  return (
    <div className="bg-card rounded-lg border border-border overflow-hidden">
      <div className="p-4 border-b border-border">
        <Skeleton className="h-9 w-48" />
      </div>
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted">
              <TableHead className="w-[180px] sticky left-0 z-10 bg-muted">Metrik</TableHead>
              {[...Array(columnCount)].map((_, i) => (
                <TableHead key={i} className="text-right min-w-[100px]">
                  <Skeleton className="h-4 w-16 ml-auto" />
                </TableHead>
              ))}
              <TableHead className="text-right min-w-[120px] sticky right-0 z-10 bg-muted">
                <Skeleton className="h-4 w-16 ml-auto" />
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {[...Array(14)].map((_, i) => (
              <TableRow key={i}>
                <TableCell className="sticky left-0 z-10 bg-card">
                  <Skeleton className="h-4 w-28" />
                </TableCell>
                {[...Array(columnCount)].map((_, j) => (
                  <TableCell key={j} className="text-right">
                    <Skeleton className="h-4 w-16 ml-auto" />
                  </TableCell>
                ))}
                <TableCell className="text-right sticky right-0 z-10 bg-card">
                  <Skeleton className="h-4 w-20 ml-auto" />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}

interface PLMetricRowProps {
  metric: MetricRow;
  periods: PeriodStats[];
  total: PeriodStats;
  expandedRows: Set<string>;
  onToggleExpand: (key: string) => void;
  formatValue: (value: number, isCurrency: boolean, isPercentage: boolean, showNegative?: boolean) => string;
}

function PLMetricRow({
  metric,
  periods,
  total,
  expandedRows,
  onToggleExpand,
  formatValue,
}: PLMetricRowProps) {
  // Divider row
  if (metric.label === "divider") {
    return (
      <TableRow>
        <TableCell colSpan={periods.length + 2} className="h-2 bg-muted p-0" />
      </TableRow>
    );
  }

  const isExpanded = expandedRows.has(metric.key);
  const isCurrency = metric.isCurrency !== false && !metric.isPercentage;
  const isPercentage = metric.isPercentage ?? false;

  const getValue = (period: PeriodStats): number => {
    if (metric.getValue) return metric.getValue(period);
    if (metric.field === "calculated") return 0;
    const value = period[metric.field];
    return typeof value === "number" ? value : 0;
  };

  return (
    <>
      <TableRow
        className={cn(
          "hover:bg-muted/50 transition-colors",
          metric.isExpandable && "cursor-pointer",
          metric.isBold && "bg-muted/30"
        )}
        onClick={() => metric.isExpandable && onToggleExpand(metric.key)}
      >
        {/* Sticky first column - Metric name */}
        <TableCell
          className={cn(
            "sticky left-0 z-10 bg-card font-medium text-foreground",
            metric.isBold && "bg-muted"
          )}
        >
          <div className="flex items-center gap-2">
            {metric.isExpandable ? (
              isExpanded ? (
                <ChevronDown className="h-4 w-4 text-muted-foreground" />
              ) : (
                <ChevronRight className="h-4 w-4 text-muted-foreground" />
              )
            ) : (
              <div className="w-4" />
            )}
            <span className={cn(metric.isBold && "font-semibold")}>{metric.label}</span>
          </div>
        </TableCell>

        {/* Period columns */}
        {periods.map((period, index) => {
          const value = getValue(period);
          return (
            <TableCell
              key={index}
              className={cn(
                "text-right min-w-[100px]",
                metric.isBold && "font-semibold",
                value < 0 && "text-red-600",
                metric.isNegative && value !== 0 && "text-red-600"
              )}
            >
              {formatValue(value, isCurrency, isPercentage, metric.isNegative && value > 0)}
            </TableCell>
          );
        })}

        {/* Sticky total column */}
        <TableCell
          className={cn(
            "text-right min-w-[120px] sticky right-0 z-10 bg-card border-l border-border",
            metric.isBold && "font-semibold bg-muted",
            getValue(total) < 0 && "text-red-600",
            metric.isNegative && getValue(total) !== 0 && "text-red-600"
          )}
        >
          {formatValue(getValue(total), isCurrency, isPercentage, metric.isNegative && getValue(total) > 0)}
        </TableCell>
      </TableRow>

      {/* Child rows (expandable) */}
      {metric.isExpandable && isExpanded && metric.children?.map((child) => (
        <TableRow key={child.key} className="bg-muted/20 hover:bg-muted/40 transition-colors">
          <TableCell className="sticky left-0 z-10 bg-card pl-10 text-muted-foreground text-sm">
            <div className="flex items-center gap-2">
              <span className="text-muted-foreground">└</span>
              {child.label}
            </div>
          </TableCell>
          {periods.map((period, index) => {
            const childValue = child.field !== "calculated" ? period[child.field] : 0;
            const value = typeof childValue === "number" ? childValue : 0;
            return (
              <TableCell
                key={index}
                className={cn(
                  "text-right text-sm min-w-[100px]",
                  value < 0 && "text-red-600",
                  child.isNegative && value !== 0 && "text-red-600"
                )}
              >
                {formatValue(value, child.isCurrency !== false, false, child.isNegative && value > 0)}
              </TableCell>
            );
          })}
          <TableCell
            className={cn(
              "text-right text-sm min-w-[120px] sticky right-0 z-10 bg-card border-l border-border",
              (child.field !== "calculated" && (total[child.field] as number) < 0) && "text-red-600",
              child.isNegative && child.field !== "calculated" && (total[child.field] as number) !== 0 && "text-red-600"
            )}
          >
            {formatValue(
              child.field !== "calculated" ? (total[child.field] as number || 0) : 0,
              child.isCurrency !== false,
              false,
              child.isNegative && child.field !== "calculated" && (total[child.field] as number) > 0
            )}
          </TableCell>
        </TableRow>
      ))}
    </>
  );
}

export function PLView({ storeId }: PLViewProps) {
  const [selectedPreset, setSelectedPreset] = useState<PLPeriodPreset>("last12months");
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const { formatCurrency } = useCurrency();

  // Get preset config
  const presetConfig = useMemo(() => {
    return PL_PERIOD_PRESETS.find((p) => p.id === selectedPreset) || PL_PERIOD_PRESETS[0];
  }, [selectedPreset]);

  // Fetch data
  const { data, isLoading, error } = useMultiPeriodStats(
    storeId,
    presetConfig.periodType,
    presetConfig.periodCount
  );

  const handleToggleExpand = (key: string) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const formatValue = (
    value: number,
    isCurrency: boolean,
    isPercentage: boolean,
    showNegative?: boolean
  ): string => {
    if (isPercentage) {
      const formatted = `%${value.toFixed(1).replace(".", ",")}`;
      return value < 0 ? formatted : formatted;
    }

    if (isCurrency) {
      const absValue = Math.abs(value);
      const formatted = formatCurrency(absValue);
      if (showNegative && value !== 0) {
        return `-${formatted}`;
      }
      return formatted;
    }

    return value.toLocaleString("tr-TR");
  };

  if (isLoading) {
    return <PLViewSkeleton columnCount={presetConfig.periodCount} />;
  }

  if (error || !data) {
    return (
      <div className="bg-card rounded-lg border border-border p-8 text-center text-muted-foreground">
        {error ? `Hata: ${error.message}` : "Veri bulunamadı"}
      </div>
    );
  }

  return (
    <div className="bg-card rounded-lg border border-border overflow-hidden">
      {/* Header with preset selector */}
      <div className="p-4 border-b border-border flex items-center justify-between">
        <h3 className="font-semibold text-foreground">Kar/Zarar Tablosu</h3>
        <Select value={selectedPreset} onValueChange={(v) => setSelectedPreset(v as PLPeriodPreset)}>
          <SelectTrigger className="w-[220px]">
            <SelectValue placeholder="Dönem seçin" />
          </SelectTrigger>
          <SelectContent>
            {PL_PERIOD_PRESETS.map((preset) => (
              <SelectItem key={preset.id} value={preset.id}>
                {preset.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Scrollable table */}
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted">
              {/* Sticky first column */}
              <TableHead className="w-[180px] sticky left-0 z-20 bg-muted font-semibold">
                Parametre / Tarih
              </TableHead>

              {/* Period columns */}
              {data.periods.map((period, index) => (
                <TableHead
                  key={index}
                  className="text-right min-w-[100px] font-semibold whitespace-nowrap"
                >
                  {period.periodLabel}
                </TableHead>
              ))}

              {/* Sticky total column */}
              <TableHead className="text-right min-w-[120px] sticky right-0 z-20 bg-muted font-semibold border-l border-border">
                Toplam
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {METRIC_ROWS.map((metric) => (
              <PLMetricRow
                key={metric.key}
                metric={metric}
                periods={data.periods}
                total={data.total}
                expandedRows={expandedRows}
                onToggleExpand={handleToggleExpand}
                formatValue={formatValue}
              />
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
