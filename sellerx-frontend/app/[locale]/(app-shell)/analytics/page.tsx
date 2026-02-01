"use client";

import { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useDashboardStats, useDashboardStatsByRange } from "@/hooks/useDashboardStats";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  TrendingUp,
  TrendingDown,
  BarChart3,
  ShoppingCart,
  DollarSign,
  AlertCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { format, subDays } from "date-fns";
import type { DateRange } from "react-day-picker";
import {
  RevenueTrendChart,
  ProfitBreakdownChart,
  DateRangePicker,
  OrderMetrics,
  ProductPerformanceTable,
  type DateRangePreset,
} from "@/components/analytics";
import { useCurrency } from "@/lib/contexts/currency-context";

function formatPercent(value: number): string {
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(1)}%`;
}

function StatCard({
  title,
  value,
  change,
  icon: Icon,
  isLoading,
  isCurrency = false,
  formatCurrency,
}: {
  title: string;
  value: number;
  change?: number;
  icon: React.ElementType;
  isLoading?: boolean;
  isCurrency?: boolean;
  formatCurrency: (value: number) => string;
}) {
  if (isLoading) {
    return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-4 w-4" />
        </CardHeader>
        <CardContent>
          <Skeleton className="h-8 w-32" />
          <Skeleton className="h-4 w-20 mt-2" />
        </CardContent>
      </Card>
    );
  }

  const isPositive = change && change > 0;
  const isNegative = change && change < 0;

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">
          {isCurrency ? formatCurrency(value) : value.toLocaleString("tr-TR")}
        </div>
        {change !== undefined && (
          <p
            className={cn(
              "text-xs flex items-center gap-1 mt-1",
              isPositive && "text-green-600 dark:text-green-400",
              isNegative && "text-red-600 dark:text-red-400",
              !isPositive && !isNegative && "text-muted-foreground"
            )}
          >
            {isPositive ? (
              <TrendingUp className="h-3 w-3" />
            ) : isNegative ? (
              <TrendingDown className="h-3 w-3" />
            ) : null}
            {formatPercent(change)} onceki doneme gore
          </p>
        )}
      </CardContent>
    </Card>
  );
}

export default function AnalyticsPage() {
  const { formatCurrency } = useCurrency();
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Date range state - default to last 30 days
  const [dateRange, setDateRange] = useState<DateRange | undefined>(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return { from: subDays(today, 29), to: today };
  });
  const [selectedPreset, setSelectedPreset] = useState<DateRangePreset>("last30days");

  // Format dates for API
  const startDate = dateRange?.from ? format(dateRange.from, "yyyy-MM-dd") : undefined;
  const endDate = dateRange?.to ? format(dateRange.to, "yyyy-MM-dd") : undefined;

  // Use range query for custom date selection
  const { data: rangeStats, isLoading: rangeLoading } = useDashboardStatsByRange(
    storeId,
    startDate,
    endDate,
    selectedPreset
  );

  // Also get default stats for comparison
  const { data: defaultStats, isLoading: defaultStatsLoading } = useDashboardStats(storeId || undefined);

  // Use range stats when available, otherwise fall back to today's stats
  const stats = rangeStats || defaultStats?.today;
  const isLoading = storeLoading || rangeLoading || defaultStatsLoading;

  // Handle date range change
  const handleDateRangeChange = (range: DateRange | undefined, preset: DateRangePreset) => {
    setDateRange(range);
    setSelectedPreset(preset);
  };

  // Use selected range stats or fallback to default
  const currentRevenue = stats?.totalRevenue || 0;
  const currentOrders = stats?.totalOrders || 0;
  const currentProfit = stats?.grossProfit || 0;

  // Calculate comparison with yesterday from default stats
  const yesterdayRevenue = defaultStats?.yesterday?.totalRevenue || 0;
  const yesterdayOrders = defaultStats?.yesterday?.totalOrders || 0;
  const yesterdayProfit = defaultStats?.yesterday?.grossProfit || 0;

  // Only show change for "today" preset
  const showComparison = selectedPreset === "today";
  const revenueChange = showComparison && yesterdayRevenue > 0
    ? ((currentRevenue - yesterdayRevenue) / yesterdayRevenue) * 100
    : undefined;
  const ordersChange = showComparison && yesterdayOrders > 0
    ? ((currentOrders - yesterdayOrders) / yesterdayOrders) * 100
    : undefined;
  const profitChange = showComparison && yesterdayProfit > 0
    ? ((currentProfit - yesterdayProfit) / yesterdayProfit) * 100
    : undefined;

  // Calculate average order value
  const currentAOV = currentOrders > 0 ? currentRevenue / currentOrders : 0;
  const yesterdayAOV = yesterdayOrders > 0 ? yesterdayRevenue / yesterdayOrders : 0;
  const aovChange = showComparison && yesterdayAOV > 0
    ? ((currentAOV - yesterdayAOV) / yesterdayAOV) * 100
    : undefined;

  // Profit margin
  const profitMargin = currentRevenue > 0 ? (currentProfit / currentRevenue) * 100 : 0;

  return (
    <div className="space-y-6">
      {/* Page Header with Date Range Picker */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">
            Satis performansinizi ve trendleri analiz edin
          </p>
        </div>
        <DateRangePicker
          dateRange={dateRange}
          onDateRangeChange={handleDateRangeChange}
          className="w-full sm:w-auto"
        />
      </div>

      {!storeId && !isLoading && (
        <Card className="border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-900/20">
          <CardContent className="flex items-center gap-3 py-4">
            <AlertCircle className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
            <p className="text-yellow-800 dark:text-yellow-200">
              Analitikleri goruntulemek icin bir magaza secin
            </p>
          </CardContent>
        </Card>
      )}

      {/* Main Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Toplam Gelir"
          value={currentRevenue}
          change={revenueChange}
          icon={DollarSign}
          isLoading={isLoading}
          isCurrency
          formatCurrency={formatCurrency}
        />
        <StatCard
          title="Siparis Sayisi"
          value={currentOrders}
          change={ordersChange}
          icon={ShoppingCart}
          isLoading={isLoading}
          formatCurrency={formatCurrency}
        />
        <StatCard
          title="Brut Kar"
          value={currentProfit}
          change={profitChange}
          icon={TrendingUp}
          isLoading={isLoading}
          isCurrency
          formatCurrency={formatCurrency}
        />
        <StatCard
          title="Ortalama Siparis"
          value={currentAOV}
          change={aovChange}
          icon={BarChart3}
          isLoading={isLoading}
          isCurrency
          formatCurrency={formatCurrency}
        />
      </div>

      {/* Charts Row */}
      <div className="grid gap-6 md:grid-cols-2">
        <RevenueTrendChart
          stats={stats}
          isLoading={isLoading}
          title="Gelir ve Kar Trendi"
        />
        <ProfitBreakdownChart
          stats={stats}
          isLoading={isLoading}
          title="Gelir Dagilimi"
        />
      </div>

      {/* Order Metrics */}
      <OrderMetrics stats={stats} isLoading={isLoading} />

      {/* Product Performance Table */}
      <ProductPerformanceTable
        products={stats?.products}
        isLoading={isLoading}
        title="Urun Performansi"
      />

      {/* Period Comparison - Only show when using default stats */}
      {defaultStats && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Donem Karsilastirmasi</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="grid gap-4 md:grid-cols-4">
                {[1, 2, 3, 4].map((i) => (
                  <Skeleton key={i} className="h-24" />
                ))}
              </div>
            ) : (
              <div className="grid gap-4 md:grid-cols-4">
                <div className="p-4 rounded-lg bg-blue-50 dark:bg-blue-900/30 text-center">
                  <p className="text-sm text-blue-600 dark:text-blue-400 font-medium">Bugun</p>
                  <p className="text-2xl font-bold text-blue-700 dark:text-blue-300 mt-1">
                    {formatCurrency(defaultStats.today?.totalRevenue || 0)}
                  </p>
                  <p className="text-xs text-blue-500 dark:text-blue-400 mt-1">
                    {defaultStats.today?.totalOrders || 0} siparis
                  </p>
                </div>
                <div className="p-4 rounded-lg bg-muted text-center">
                  <p className="text-sm text-muted-foreground font-medium">Dun</p>
                  <p className="text-2xl font-bold text-foreground mt-1">
                    {formatCurrency(defaultStats.yesterday?.totalRevenue || 0)}
                  </p>
                  <p className="text-xs text-muted-foreground mt-1">
                    {defaultStats.yesterday?.totalOrders || 0} siparis
                  </p>
                </div>
                <div className="p-4 rounded-lg bg-green-50 dark:bg-green-900/30 text-center">
                  <p className="text-sm text-green-600 dark:text-green-400 font-medium">Bu Ay</p>
                  <p className="text-2xl font-bold text-green-700 dark:text-green-300 mt-1">
                    {formatCurrency(defaultStats.thisMonth?.totalRevenue || 0)}
                  </p>
                  <p className="text-xs text-green-500 dark:text-green-400 mt-1">
                    {defaultStats.thisMonth?.totalOrders || 0} siparis
                  </p>
                </div>
                <div className="p-4 rounded-lg bg-purple-50 dark:bg-purple-900/30 text-center">
                  <p className="text-sm text-purple-600 dark:text-purple-400 font-medium">Gecen Ay</p>
                  <p className="text-2xl font-bold text-purple-700 dark:text-purple-300 mt-1">
                    {formatCurrency(defaultStats.lastMonth?.totalRevenue || 0)}
                  </p>
                  <p className="text-xs text-purple-500 dark:text-purple-400 mt-1">
                    {defaultStats.lastMonth?.totalOrders || 0} siparis
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
