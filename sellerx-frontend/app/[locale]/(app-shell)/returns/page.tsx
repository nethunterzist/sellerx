"use client";

import React, { useState, useCallback } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useReturnAnalytics } from "@/hooks/queries/use-returns";
import {
  ReturnSummaryCards,
  TopReturnedProducts,
  ReturnReasonsChart,
  ReturnTrendChart,
  ReturnDecisionsTable,
} from "@/components/returns";
import { DateRangePicker } from "@/components/ui/date-range-picker";
import { SegmentedControl } from "@/components/ui/segmented-control";
import { FadeIn } from "@/components/motion";
import { AlertTriangle, BarChart3, ClipboardList } from "lucide-react";
import { format, startOfMonth } from "date-fns";
import {
  StatCardSkeleton,
  FilterBarSkeleton,
  ChartSkeleton,
  TableSkeleton,
} from "@/components/ui/skeleton-blocks";

function ReturnsPageSkeleton() {
  return (
    <div className="space-y-6">
      <FilterBarSkeleton showSearch={false} buttonCount={2} />
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
        {Array.from({ length: 5 }).map((_, i) => <StatCardSkeleton key={i} />)}
      </div>
      <ChartSkeleton height="h-48" />
      <TableSkeleton columns={6} rows={8} showImage={true} />
    </div>
  );
}

// Helper to get default date range (current month)
function getDefaultDateRange(): { from: Date; to: Date } {
  const today = new Date();
  return { from: startOfMonth(today), to: today };
}

// Custom presets with "Tumu" as first option
const RETURNS_PRESETS = [
  { label: "Bu Ay", value: "thisMonth" },
  { label: "Gecen Ay", value: "lastMonth" },
  { label: "Son 7 Gun", value: "7d" },
  { label: "Son 30 Gun", value: "30d" },
  { label: "Son 90 Gun", value: "90d" },
  { label: "Bu Yil", value: "thisYear" },
  { label: "Tumu", value: "all" },
];

type ReturnTab = "analytics" | "management";

const RETURN_TABS = [
  { id: "analytics" as const, label: "Analiz", icon: BarChart3 },
  { id: "management" as const, label: "Iade Yonetimi", icon: ClipboardList },
];

export default function ReturnsPage() {
  // Date range state - using DateRangePicker component
  const [dateRange, setDateRange] = useState<{ from: Date; to: Date }>(getDefaultDateRange);
  const [activeTab, setActiveTab] = useState<ReturnTab>("analytics");

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Format dates for API calls
  const startDate = format(dateRange.from, "yyyy-MM-dd");
  const endDate = format(dateRange.to, "yyyy-MM-dd");

  const {
    data: analyticsData,
    isLoading: analyticsLoading,
    error: analyticsError,
  } = useReturnAnalytics(storeId, startDate, endDate);

  // Handle date range change from DateRangePicker
  const handleDateRangeChange = useCallback((range: { from: Date; to: Date } | undefined) => {
    if (range) {
      setDateRange(range);
    }
  }, []);

  // Handle tab change
  const handleTabChange = useCallback((tab: ReturnTab) => {
    setActiveTab(tab);
  }, []);

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

  return (
    <div className="space-y-6">
      {/* Segmented Control + DateRangePicker on same row */}
      <div className="flex items-center justify-between gap-4">
        <SegmentedControl
          tabs={RETURN_TABS}
          activeTab={activeTab}
          onTabChange={handleTabChange}
          ariaLabel="Returns navigation"
          className="px-0"
        />
        <DateRangePicker
          value={dateRange}
          onChange={handleDateRangeChange}
          presets={RETURNS_PRESETS}
          defaultPreset="thisMonth"
          locale="tr"
        />
      </div>

      {/* Analytics Tab */}
      {activeTab === "analytics" && (
        <div className="space-y-6">
          {storeLoading || analyticsLoading ? (
            <ReturnsPageSkeleton />
          ) : (
            <>
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
              <FadeIn>
                <ReturnSummaryCards
                  totalReturns={analyticsData?.totalReturns || 0}
                  totalReturnedItems={analyticsData?.totalReturnedItems || 0}
                  returnRate={analyticsData?.returnRate || 0}
                  totalReturnLoss={analyticsData?.totalReturnLoss || 0}
                  avgLossPerReturn={analyticsData?.avgLossPerReturn || 0}
                  isLoading={analyticsLoading}
                />
              </FadeIn>

              {/* Reasons + Trend Charts - side by side */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {analyticsData?.returnReasonDistribution &&
                  Object.keys(analyticsData.returnReasonDistribution).some(k => k !== "Bilinmiyor") && (
                  <FadeIn delay={0.1}>
                    <ReturnReasonsChart
                      reasonDistribution={analyticsData.returnReasonDistribution}
                      isLoading={analyticsLoading}
                    />
                  </FadeIn>
                )}
                <FadeIn delay={0.15}>
                  <ReturnTrendChart
                    dailyTrend={analyticsData?.dailyTrend || []}
                    isLoading={analyticsLoading}
                  />
                </FadeIn>
              </div>

              {/* Returned Products */}
              <TopReturnedProducts
                products={analyticsData?.topReturnedProducts || []}
                isLoading={analyticsLoading}
              />
            </>
          )}
        </div>
      )}

      {/* Management Tab */}
      {activeTab === "management" && (
        <div className="mt-4">
          <ReturnDecisionsTable startDate={startDate} endDate={endDate} />
        </div>
      )}
    </div>
  );
}
