"use client";

import React, { useState, useCallback } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useReturnAnalytics } from "@/hooks/queries/use-returns";
import {
  ReturnSummaryCards,
  ReturnCostBreakdown,
  TopReturnedProducts,
  ReturnReasonsChart,
  ReturnTrendChart,
} from "@/components/returns";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { RefreshCw, AlertTriangle, CalendarIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { format, subDays, startOfMonth } from "date-fns";
import { tr } from "date-fns/locale";
import {
  StatCardSkeleton,
  FilterBarSkeleton,
  ChartSkeleton,
  TableSkeleton,
} from "@/components/ui/skeleton-blocks";

function ReturnsPageSkeleton() {
  return (
    <div className="space-y-6">
      <FilterBarSkeleton showSearch={false} buttonCount={4} />
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
        {Array.from({ length: 5 }).map((_, i) => <StatCardSkeleton key={i} />)}
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <ChartSkeleton />
        <ChartSkeleton />
      </div>
      <ChartSkeleton height="h-48" />
      <TableSkeleton columns={6} rows={8} showImage={true} />
    </div>
  );
}

type DatePreset = "last7" | "last30" | "thisMonth" | "custom";

const datePresets: { id: DatePreset; label: string }[] = [
  { id: "last7", label: "Son 7 Gun" },
  { id: "last30", label: "Son 30 Gun" },
  { id: "thisMonth", label: "Bu Ay" },
  { id: "custom", label: "Ozel Tarih" },
];

export default function ReturnsPage() {
  const [selectedPreset, setSelectedPreset] = useState<DatePreset>("last30");
  const [customStartDate, setCustomStartDate] = useState<Date>(subDays(new Date(), 30));
  const [customEndDate, setCustomEndDate] = useState<Date>(new Date());

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const getDateRange = useCallback(() => {
    const today = new Date();
    switch (selectedPreset) {
      case "last7":
        return {
          startDate: format(subDays(today, 7), "yyyy-MM-dd"),
          endDate: format(today, "yyyy-MM-dd"),
        };
      case "last30":
        return {
          startDate: format(subDays(today, 30), "yyyy-MM-dd"),
          endDate: format(today, "yyyy-MM-dd"),
        };
      case "thisMonth":
        return {
          startDate: format(startOfMonth(today), "yyyy-MM-dd"),
          endDate: format(today, "yyyy-MM-dd"),
        };
      case "custom":
        return {
          startDate: format(customStartDate, "yyyy-MM-dd"),
          endDate: format(customEndDate, "yyyy-MM-dd"),
        };
      default:
        return {
          startDate: format(subDays(today, 30), "yyyy-MM-dd"),
          endDate: format(today, "yyyy-MM-dd"),
        };
    }
  }, [selectedPreset, customStartDate, customEndDate]);

  const { startDate, endDate } = getDateRange();

  const {
    data: analyticsData,
    isLoading: analyticsLoading,
    error: analyticsError,
    refetch: refetchAnalytics,
  } = useReturnAnalytics(storeId, startDate, endDate);

  const formatDateRange = () => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    return `${format(start, "d MMM", { locale: tr })} - ${format(end, "d MMM yyyy", { locale: tr })}`;
  };

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <div className="flex items-center gap-2 text-amber-600">
          <AlertTriangle className="h-5 w-5" />
          <p>Lutfen once bir magaza secin.</p>
        </div>
      </div>
    );
  }

  if (storeLoading || analyticsLoading) {
    return <ReturnsPageSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <p className="text-muted-foreground">
            Iade performansinizi analiz edin ve zarar kaynaklarini tespit edin
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => refetchAnalytics()}
          disabled={analyticsLoading}
          className="gap-2"
        >
          <RefreshCw className={cn("h-4 w-4", analyticsLoading && "animate-spin")} />
          Yenile
        </Button>
      </div>

      {/* Date Filters */}
      <div className="flex flex-wrap items-center gap-2">
        {datePresets.map((preset) => (
          <Button
            key={preset.id}
            variant={selectedPreset === preset.id ? "default" : "outline"}
            size="sm"
            onClick={() => setSelectedPreset(preset.id)}
          >
            {preset.label}
          </Button>
        ))}

        {selectedPreset === "custom" && (
          <div className="flex items-center gap-2 ml-2">
            <Popover>
              <PopoverTrigger asChild>
                <Button variant="outline" size="sm" className="gap-2">
                  <CalendarIcon className="h-4 w-4" />
                  {format(customStartDate, "d MMM yyyy", { locale: tr })}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <Calendar
                  mode="single"
                  selected={customStartDate}
                  onSelect={(date) => date && setCustomStartDate(date)}
                  initialFocus
                />
              </PopoverContent>
            </Popover>
            <span className="text-muted-foreground">-</span>
            <Popover>
              <PopoverTrigger asChild>
                <Button variant="outline" size="sm" className="gap-2">
                  <CalendarIcon className="h-4 w-4" />
                  {format(customEndDate, "d MMM yyyy", { locale: tr })}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <Calendar
                  mode="single"
                  selected={customEndDate}
                  onSelect={(date) => date && setCustomEndDate(date)}
                  initialFocus
                />
              </PopoverContent>
            </Popover>
          </div>
        )}

        <span className="text-sm text-muted-foreground ml-auto">{formatDateRange()}</span>
      </div>

      {/* Error State */}
      {analyticsError && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <div className="flex items-center gap-2 text-red-600 dark:text-red-400">
            <AlertTriangle className="h-5 w-5" />
            <p>Iade verileri yuklenirken hata olustu: {analyticsError.message}</p>
          </div>
        </div>
      )}

      {/* Summary Cards */}
      <ReturnSummaryCards
        totalReturns={analyticsData?.totalReturns || 0}
        totalReturnedItems={analyticsData?.totalReturnedItems || 0}
        returnRate={analyticsData?.returnRate || 0}
        totalReturnLoss={analyticsData?.totalReturnLoss || 0}
        avgLossPerReturn={analyticsData?.avgLossPerReturn || 0}
        isLoading={analyticsLoading}
      />

      {/* Charts: Cost Breakdown + Reasons */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ReturnCostBreakdown
          costBreakdown={
            analyticsData?.costBreakdown || {
              productCost: 0,
              shippingCostOut: 0,
              shippingCostReturn: 0,
              commissionLoss: 0,
              packagingCost: 0,
              totalLoss: 0,
            }
          }
          isLoading={analyticsLoading}
        />
        <ReturnReasonsChart
          reasonDistribution={analyticsData?.returnReasonDistribution || {}}
          isLoading={analyticsLoading}
        />
      </div>

      {/* Trend Chart */}
      <ReturnTrendChart
        dailyTrend={analyticsData?.dailyTrend || []}
        isLoading={analyticsLoading}
      />

      {/* Top Returned Products */}
      <TopReturnedProducts
        products={analyticsData?.topReturnedProducts || []}
        isLoading={analyticsLoading}
      />
    </div>
  );
}
