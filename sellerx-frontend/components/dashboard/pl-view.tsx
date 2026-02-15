"use client";

import { useState, useMemo } from "react";
import { FadeIn } from "@/components/motion";
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
} from "@/types/dashboard";
import { PL_PERIOD_PRESETS, calculateDynamicPeriodCount } from "@/types/dashboard";

interface PLViewProps {
  storeId?: string;
  selectedProducts?: string[]; // Array of barcodes to filter
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
  isHighlighted?: boolean;
  showBadge?: boolean; // For returnCount badge
  children?: MetricRow[];
  getValue?: (period: PeriodStats) => number;
  getExpenseCategories?: boolean; // For dynamic expense categories
}

const METRIC_ROWS: MetricRow[] = [
  // ==================== SATIŞLAR ====================
  { key: "revenue", label: "Satışlar (Brüt Ciro)", field: "totalRevenue", isBold: true },
  { key: "units", label: "Satış Adedi", field: "totalProductsSold", isCurrency: false },
  { key: "orders", label: "Sipariş Sayısı", field: "totalOrders", isCurrency: false },

  // ==================== İNDİRİMLER ====================
  {
    key: "discounts",
    label: "İndirimler & Kuponlar",
    field: "calculated",
    isNegative: true,
    isExpandable: true,
    getValue: (p) => (p.totalSellerDiscount ?? 0) + (p.totalPlatformDiscount ?? 0) + (p.totalCouponDiscount ?? 0),
    children: [
      { key: "sellerDiscount", label: "Satıcı İndirimi", field: "totalSellerDiscount", isNegative: true },
      { key: "platformDiscount", label: "Platform İndirimi", field: "totalPlatformDiscount", isNegative: true },
      { key: "couponDiscount", label: "Kupon İndirimi", field: "totalCouponDiscount", isNegative: true },
    ],
  },

  // NET CİRO
  { key: "netRevenue", label: "Net Ciro", field: "netRevenue", isBold: true },

  { key: "divider1", label: "divider", field: "calculated" },

  // ==================== MALİYETLER ====================
  { key: "commission", label: "Komisyon", field: "totalEstimatedCommission", isNegative: true },
  { key: "shippingCost", label: "Kargo Maliyeti", field: "totalShippingCost", isNegative: true },
  { key: "productCosts", label: "Ürün Maliyeti", field: "totalProductCosts", isNegative: true },
  { key: "stoppage", label: "Stopaj", field: "totalStoppage", isNegative: true },

  { key: "divider2", label: "divider", field: "calculated" },

  // ==================== KESİLEN FATURALAR ====================
  {
    key: "invoicedFees",
    label: "Kesilen Faturalar",
    field: "calculated",
    isNegative: true,
    isExpandable: true,
    getValue: (p) =>
      (p.platformServiceFee ?? 0) +
      (p.azPlatformServiceFee ?? 0) +
      (p.invoicedAdvertisingFees ?? 0) +
      (p.invoicedPenaltyFees ?? 0) +
      (p.invoicedInternationalFees ?? 0) +
      (p.invoicedOtherFees ?? 0) -
      (p.invoicedRefunds ?? 0),
    children: [
      { key: "platformService", label: "Platform Hizmet Bedeli", field: "platformServiceFee", isNegative: true },
      { key: "azPlatformService", label: "AZ-Platform Hizmet Bedeli", field: "azPlatformServiceFee", isNegative: true },
      { key: "advertising", label: "Reklam Bedeli", field: "invoicedAdvertisingFees", isNegative: true },
      { key: "penalty", label: "Ceza", field: "invoicedPenaltyFees", isNegative: true },
      { key: "international", label: "Uluslararası Hizmet Bedeli", field: "invoicedInternationalFees", isNegative: true },
      { key: "other", label: "Diğer Kesintiler", field: "invoicedOtherFees", isNegative: true },
      { key: "refunds", label: "İadeler", field: "invoicedRefunds", isNegative: false },
    ],
  },

  { key: "divider2b", label: "divider", field: "calculated" },

  // ==================== KÂR ====================
  { key: "grossProfit", label: "Brüt Kâr", field: "grossProfit", isBold: true },
  {
    key: "expenses",
    label: "Ekstra Giderler",
    field: "totalExpenseAmount",
    isNegative: true,
    isExpandable: true,
    getExpenseCategories: true,
  },

  { key: "divider3", label: "divider", field: "calculated" },

  // NET KÂR (highlighted)
  { key: "netProfit", label: "Net Kâr", field: "netProfit", isBold: true, isHighlighted: true },

  { key: "divider4", label: "divider", field: "calculated" },

  // ==================== METRİKLER ====================
  { key: "margin", label: "Kâr Marjı", field: "profitMargin", isBold: true, isCurrency: false, isPercentage: true },
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
  // Divider row - special highlight for Net Kâr divider
  if (metric.label === "divider") {
    // Check if next row is highlighted (Net Kâr)
    const isHighlightDivider = metric.key === "divider3";
    return (
      <TableRow>
        <TableCell
          colSpan={periods.length + 2}
          className={cn("p-0", isHighlightDivider ? "h-2 bg-primary/10" : "h-2 bg-muted")}
        />
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

  // Get return count for badge
  const getReturnCount = (period: PeriodStats): number => {
    return period.returnCount ?? 0;
  };

  return (
    <>
      <TableRow
        className={cn(
          "hover:bg-muted/50 transition-colors",
          metric.isExpandable && "cursor-pointer",
          metric.isBold && "bg-muted/30",
          metric.isHighlighted && "bg-primary/5"
        )}
        onClick={() => metric.isExpandable && onToggleExpand(metric.key)}
      >
        {/* Sticky first column - Metric name */}
        <TableCell
          className={cn(
            "sticky left-0 z-10 bg-card font-medium text-foreground",
            metric.isBold && "bg-muted",
            metric.isHighlighted && "bg-primary/5"
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
            {/* Badge for return count in total column header */}
            {metric.showBadge && total.returnCount > 0 && (
              <span className="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700">
                {total.returnCount} ürün
              </span>
            )}
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
            metric.isHighlighted && "bg-primary/5",
            getValue(total) < 0 && "text-red-600",
            metric.isNegative && getValue(total) !== 0 && "text-red-600"
          )}
        >
          {formatValue(getValue(total), isCurrency, isPercentage, metric.isNegative && getValue(total) > 0)}
        </TableCell>
      </TableRow>

      {/* Child rows (expandable) - Regular children */}
      {metric.isExpandable && isExpanded && !metric.getExpenseCategories && metric.children?.map((child) => (
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

      {/* Dynamic expense categories from expensesByCategory */}
      {metric.isExpandable && isExpanded && metric.getExpenseCategories && (() => {
        const categories = total.expensesByCategory;
        const hasCategories = categories && Object.keys(categories).length > 0;

        if (!hasCategories) {
          return (
            <TableRow className="bg-muted/20">
              <TableCell className="sticky left-0 z-10 bg-card pl-10 text-muted-foreground text-sm italic" colSpan={periods.length + 2}>
                Kategori detayı bulunamadı
              </TableCell>
            </TableRow>
          );
        }

        return (
          <>
            {Object.entries(categories).map(([categoryName, totalValue]) => (
              <TableRow key={categoryName} className="bg-muted/20 hover:bg-muted/40 transition-colors">
                <TableCell className="sticky left-0 z-10 bg-card pl-10 text-muted-foreground text-sm">
                  <div className="flex items-center gap-2">
                    <span className="text-muted-foreground">└</span>
                    {categoryName}
                  </div>
                </TableCell>
                {periods.map((period, index) => {
                  const value = period.expensesByCategory?.[categoryName] ?? 0;
                  return (
                    <TableCell key={index} className="text-right text-sm min-w-[100px] text-red-600">
                      {formatValue(value, true, false, value > 0)}
                    </TableCell>
                  );
                })}
                <TableCell className="text-right text-sm min-w-[120px] sticky right-0 z-10 bg-card border-l border-border text-red-600">
                  {formatValue(totalValue ?? 0, true, false, true)}
                </TableCell>
              </TableRow>
            ))}
          </>
        );
      })()}
    </>
  );
}

export function PLView({ storeId, selectedProducts = [] }: PLViewProps) {
  const [selectedPreset, setSelectedPreset] = useState<PLPeriodPreset>("last12months");
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const { formatCurrency } = useCurrency();

  // Get preset config
  const presetConfig = useMemo(() => {
    return PL_PERIOD_PRESETS.find((p) => p.id === selectedPreset) || PL_PERIOD_PRESETS[0];
  }, [selectedPreset]);

  // Calculate dynamic period count for quarterly/yearly presets
  const effectivePeriodCount = useMemo(() => {
    return calculateDynamicPeriodCount(presetConfig);
  }, [presetConfig]);

  // Use first selected product barcode for filtering (API supports single product)
  const productBarcode = selectedProducts.length === 1 ? selectedProducts[0] : undefined;

  // Fetch data
  const { data, isLoading, error } = useMultiPeriodStats(
    storeId,
    presetConfig.periodType,
    effectivePeriodCount,
    productBarcode
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
    return <PLViewSkeleton columnCount={effectivePeriodCount} />;
  }

  if (error || !data) {
    return (
      <div className="bg-card rounded-lg border border-border p-8 text-center text-muted-foreground">
        {error ? `Hata: ${error.message}` : "Veri bulunamadı"}
      </div>
    );
  }

  return (
    <FadeIn>
    <div className="bg-card rounded-lg border border-border overflow-hidden">
      {/* Header with preset selector */}
      <div className="p-4 border-b border-border flex items-center justify-between">
        <h3 className="font-semibold text-foreground">Kar/Zarar Tablosu</h3>
        <Select value={selectedPreset} onValueChange={(v) => setSelectedPreset(v as PLPeriodPreset)}>
          <SelectTrigger className="w-[200px]">
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
              {data.periods.map((period, index) => {
                // Format date range for weekly views (e.g., "02.02 - 07.02.2025")
                const formatDateRange = () => {
                  if (presetConfig.periodType !== "weekly") return null;

                  const start = new Date(period.startDate);
                  const end = new Date(period.endDate);

                  const formatDay = (d: Date) =>
                    `${d.getDate().toString().padStart(2, "0")}.${(d.getMonth() + 1).toString().padStart(2, "0")}`;

                  const formatDayWithYear = (d: Date) =>
                    `${formatDay(d)}.${d.getFullYear()}`;

                  // Same year: "02.02 - 07.02.2025"
                  // Different year: "28.12.2024 - 03.01.2025"
                  if (start.getFullYear() === end.getFullYear()) {
                    return `${formatDay(start)} - ${formatDayWithYear(end)}`;
                  }
                  return `${formatDayWithYear(start)} - ${formatDayWithYear(end)}`;
                };

                const dateRange = formatDateRange();

                return (
                  <TableHead
                    key={index}
                    className={cn(
                      "text-right font-semibold whitespace-nowrap",
                      dateRange ? "min-w-[120px]" : "min-w-[100px]"
                    )}
                  >
                    <div className="flex flex-col items-end gap-0.5">
                      <span>{period.periodLabel}</span>
                      {dateRange && (
                        <span className="text-[10px] font-normal text-muted-foreground">
                          {dateRange}
                        </span>
                      )}
                    </div>
                  </TableHead>
                );
              })}

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
    </FadeIn>
  );
}
