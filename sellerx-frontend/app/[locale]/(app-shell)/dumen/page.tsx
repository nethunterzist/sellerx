"use client";

import { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useDashboardStats, useDashboardStatsByRange } from "@/hooks/useDashboardStats";
import { useFinancialStats } from "@/hooks/queries/use-financial";
import { useStoreExpenses } from "@/hooks/queries/use-expenses";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Progress } from "@/components/ui/progress";
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Percent,
  MinusCircle,
  PlusCircle,
  Calculator,
  ShoppingCart,
  BarChart3,
  CheckCircle,
  Clock,
  Package,
  RotateCcw,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { FadeIn } from "@/components/motion";
import { format, subDays } from "date-fns";
import type { DateRange } from "react-day-picker";
import {
  RevenueTrendChart,
  ProfitBreakdownChart,
  DateRangePicker,
  ProductPerformanceTable,
  type DateRangePreset,
} from "@/components/analytics";
import { useCurrency } from "@/lib/contexts/currency-context";
import type { StoreExpense } from "@/types/expense";

// KPI Card Component
function KPICard({
  title,
  value,
  icon: Icon,
  trend,
  trendValue,
  variant = "default",
  isLoading,
}: {
  title: string;
  value: string;
  icon: React.ElementType;
  trend?: "up" | "down" | "neutral";
  trendValue?: string;
  variant?: "default" | "success" | "danger" | "warning" | "purple" | "blue";
  isLoading?: boolean;
}) {
  const variantStyles = {
    default: "text-foreground",
    success: "text-green-600 dark:text-green-400",
    danger: "text-red-600 dark:text-red-400",
    warning: "text-yellow-600 dark:text-yellow-400",
    purple: "text-purple-600 dark:text-purple-400",
    blue: "text-blue-600 dark:text-blue-400",
  };

  const bgStyles = {
    default: "",
    success: "border-green-200 dark:border-green-800 bg-green-50/50 dark:bg-green-900/20",
    danger: "border-red-200 dark:border-red-800 bg-red-50/50 dark:bg-red-900/20",
    warning: "border-yellow-200 dark:border-yellow-800 bg-yellow-50/50 dark:bg-yellow-900/20",
    purple: "border-purple-200 dark:border-purple-800 bg-purple-50/50 dark:bg-purple-900/20",
    blue: "border-blue-200 dark:border-blue-800 bg-blue-50/50 dark:bg-blue-900/20",
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="pt-4 pb-3">
          <div className="flex items-center justify-between mb-2">
            <Skeleton className="h-3 w-20" />
            <Skeleton className="h-4 w-4" />
          </div>
          <Skeleton className="h-7 w-28" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={cn(bgStyles[variant])}>
      <CardContent className="pt-4 pb-3">
        <div className="flex items-center justify-between mb-1">
          <span className="text-xs font-medium text-muted-foreground">{title}</span>
          <Icon className={cn("h-4 w-4", variantStyles[variant])} />
        </div>
        <div className={cn("text-xl font-bold", variantStyles[variant])}>
          {value}
        </div>
        {trend && trendValue && (
          <p className="text-[10px] text-muted-foreground flex items-center gap-1 mt-1">
            {trend === "up" ? (
              <TrendingUp className="h-3 w-3 text-green-500" />
            ) : trend === "down" ? (
              <TrendingDown className="h-3 w-3 text-red-500" />
            ) : null}
            {trendValue}
          </p>
        )}
      </CardContent>
    </Card>
  );
}

// Breakdown Card Component
function BreakdownCard({
  title,
  items,
  total,
  isPositive,
  isLoading,
  formatCurrency,
}: {
  title: string;
  items: Array<{ label: string; value: number; color?: string }>;
  total: number;
  isPositive?: boolean;
  isLoading?: boolean;
  formatCurrency: (value: number) => string;
}) {
  if (isLoading) {
    return (
      <Card>
        <CardHeader className="pb-2">
          <Skeleton className="h-5 w-24" />
        </CardHeader>
        <CardContent className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex justify-between">
              <Skeleton className="h-4 w-28" />
              <Skeleton className="h-4 w-20" />
            </div>
          ))}
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base flex items-center gap-2">
          {isPositive ? (
            <PlusCircle className="h-4 w-4 text-green-600 dark:text-green-400" />
          ) : (
            <MinusCircle className="h-4 w-4 text-red-600 dark:text-red-400" />
          )}
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {items.map((item, index) => (
          <div
            key={index}
            className="flex justify-between items-center py-1 border-b border-border last:border-0"
          >
            <span className="text-muted-foreground text-sm">{item.label}</span>
            <span
              className={cn(
                "font-medium text-sm",
                item.color || (isPositive ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400")
              )}
            >
              {isPositive ? "+" : "-"}{formatCurrency(Math.abs(item.value))}
            </span>
          </div>
        ))}
        <div
          className={cn(
            "flex justify-between items-center pt-2 border-t-2",
            isPositive ? "border-green-200 dark:border-green-800" : "border-red-200 dark:border-red-800"
          )}
        >
          <span className="font-semibold text-sm">Toplam</span>
          <span
            className={cn(
              "font-bold",
              isPositive ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400"
            )}
          >
            {isPositive ? "+" : "-"}{formatCurrency(Math.abs(total))}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}

export default function DumenPage() {
  const { formatCurrency } = useCurrency();
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Date range state
  const [dateRange, setDateRange] = useState<DateRange | undefined>(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return { from: subDays(today, 29), to: today };
  });
  const [selectedPreset, setSelectedPreset] = useState<DateRangePreset>("last30days");

  // Format dates for API
  const startDate = dateRange?.from ? format(dateRange.from, "yyyy-MM-dd") : undefined;
  const endDate = dateRange?.to ? format(dateRange.to, "yyyy-MM-dd") : undefined;

  // Dashboard stats
  const { data: rangeStats, isLoading: rangeLoading } = useDashboardStatsByRange(
    storeId,
    startDate,
    endDate,
    selectedPreset
  );
  const { data: defaultStats, isLoading: defaultStatsLoading } = useDashboardStats(storeId || undefined);

  // Financial stats
  const { data: financialStats, isLoading: financialLoading } = useFinancialStats(storeId || undefined);

  // Expenses
  const { data: expensesData, isLoading: expensesLoading } = useStoreExpenses(storeId || "");
  const expenses = expensesData?.expenses || [];

  const handleDateRangeChange = (range: DateRange | undefined, preset: DateRangePreset) => {
    setDateRange(range);
    setSelectedPreset(preset);
  };

  // Use range stats when available
  const stats = rangeStats || defaultStats?.today;
  const isLoading = storeLoading || rangeLoading || defaultStatsLoading;

  // Today's data for profit calculations
  const todayData = defaultStats?.today;
  const revenue = todayData?.totalRevenue || 0;
  const grossProfit = todayData?.grossProfit || 0;
  const commission = todayData?.totalEstimatedCommission || 0;
  const vatDifference = todayData?.vatDifference || 0;
  const stoppage = todayData?.totalStoppage || 0;
  const productCost = todayData?.totalProductCosts || 0;

  // Expenses calculation
  const monthlyExpenses = expensesData?.totalMonthlyAmount || expenses.reduce(
    (sum: number, e: StoreExpense) => sum + e.amount,
    0
  );
  const dailyExpenses = monthlyExpenses / 30;

  // Net profit calculation
  const totalDeductions = commission + dailyExpenses + stoppage;
  const netProfit = grossProfit - totalDeductions;
  const netMargin = revenue > 0 ? (netProfit / revenue) * 100 : 0;
  const grossMargin = revenue > 0 ? (grossProfit / revenue) * 100 : 0;

  // Current stats for KPIs
  const currentRevenue = stats?.totalRevenue || 0;
  const currentOrders = stats?.totalOrders || 0;
  const currentProfit = stats?.grossProfit || 0;
  const returnCount = stats?.returnCount || 0;
  const returnRate = currentOrders > 0 ? (returnCount / currentOrders) * 100 : 0;
  const currentAOV = currentOrders > 0 ? currentRevenue / currentOrders : 0;

  // Income items
  const incomeItems = [
    { label: "Satis Geliri", value: revenue },
    { label: "KDV Farki", value: vatDifference, color: vatDifference >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400" },
  ];

  // Expense items
  const expenseItems = [
    { label: "Urun Maliyeti", value: productCost },
    { label: "Komisyon", value: commission },
    { label: "Gunluk Giderler", value: dailyExpenses },
    { label: "Stopaj (%5)", value: stoppage },
  ];

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <p className="text-muted-foreground">Lutfen once bir magaza secin.</p>
      </div>
    );
  }

  return (
    <FadeIn>
    <div className="space-y-6">
      {/* Header */}
      <div>
        <DateRangePicker
          dateRange={dateRange}
          onDateRangeChange={handleDateRangeChange}
          className="w-full sm:w-auto"
        />
      </div>

      {/* KPI Cards - Row 1: Main Metrics */}
      <div className="grid gap-3 grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
        <KPICard
          title="Toplam Gelir"
          value={formatCurrency(currentRevenue)}
          icon={DollarSign}
          variant="blue"
          isLoading={isLoading}
        />
        <KPICard
          title="Brut Kar"
          value={formatCurrency(currentProfit)}
          icon={TrendingUp}
          variant="purple"
          isLoading={isLoading}
        />
        <KPICard
          title="Net Kar"
          value={formatCurrency(netProfit)}
          icon={netProfit >= 0 ? TrendingUp : TrendingDown}
          variant={netProfit >= 0 ? "success" : "danger"}
          isLoading={isLoading}
        />
        <KPICard
          title="Siparis Sayisi"
          value={currentOrders.toString()}
          icon={ShoppingCart}
          variant="default"
          isLoading={isLoading}
        />
        <KPICard
          title="Ortalama Siparis"
          value={formatCurrency(currentAOV)}
          icon={BarChart3}
          variant="default"
          isLoading={isLoading}
        />
      </div>

      {/* KPI Cards - Row 2: Margins & Financial */}
      <div className="grid gap-3 grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
        <KPICard
          title="Net Kar Marji"
          value={`${netMargin.toFixed(1)}%`}
          icon={Percent}
          variant={netMargin >= 15 ? "success" : netMargin >= 5 ? "warning" : "danger"}
          isLoading={isLoading}
        />
        <KPICard
          title="Brut Kar Marji"
          value={`${grossMargin.toFixed(1)}%`}
          icon={Calculator}
          variant="purple"
          isLoading={isLoading}
        />
        <KPICard
          title="Iade Orani"
          value={`${returnRate.toFixed(1)}%`}
          icon={RotateCcw}
          variant={returnRate <= 5 ? "success" : returnRate <= 10 ? "warning" : "danger"}
          isLoading={isLoading}
        />
        <KPICard
          title="Uzlasma Orani"
          value={`${(financialStats?.settlementRate || 0).toFixed(1)}%`}
          icon={CheckCircle}
          variant={(financialStats?.settlementRate || 0) >= 80 ? "success" : "warning"}
          isLoading={financialLoading}
        />
        <KPICard
          title="Uzlasmis Siparis"
          value={(financialStats?.settledOrders || 0).toString()}
          icon={Clock}
          variant="default"
          isLoading={financialLoading}
        />
      </div>

      {/* Income/Expense Breakdown */}
      <div className="grid gap-4 md:grid-cols-2">
        <BreakdownCard
          title="Gelirler"
          items={incomeItems}
          total={revenue + vatDifference}
          isPositive={true}
          isLoading={isLoading}
          formatCurrency={formatCurrency}
        />
        <BreakdownCard
          title="Giderler"
          items={expenseItems}
          total={totalDeductions + productCost}
          isPositive={false}
          isLoading={isLoading || expensesLoading}
          formatCurrency={formatCurrency}
        />
      </div>

      {/* Margin Visualization */}
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base flex items-center gap-2">
            <Calculator className="h-4 w-4" />
            Kar Marji Dagilimi
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-24" />
          ) : (
            <div className="space-y-3">
              <div>
                <div className="flex justify-between text-sm mb-1">
                  <span>Brut Kar Marji</span>
                  <span className="font-medium">{grossMargin.toFixed(1)}%</span>
                </div>
                <Progress
                  value={Math.min(Math.max(grossMargin, 0), 100)}
                  className="h-2 [&>div]:bg-purple-500"
                />
              </div>
              <div>
                <div className="flex justify-between text-sm mb-1">
                  <span>Net Kar Marji</span>
                  <span className={cn("font-medium", netMargin >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400")}>
                    {netMargin.toFixed(1)}%
                  </span>
                </div>
                <Progress
                  value={Math.min(Math.max(netMargin, 0), 100)}
                  className={cn("h-2", netMargin >= 15 ? "[&>div]:bg-green-500" : netMargin >= 5 ? "[&>div]:bg-yellow-500" : "[&>div]:bg-red-500")}
                />
              </div>
              <div className="flex items-center gap-4 text-xs text-muted-foreground pt-2 border-t">
                <div className="flex items-center gap-1">
                  <div className="w-2 h-2 rounded bg-green-500" />
                  <span>Iyi (15%+)</span>
                </div>
                <div className="flex items-center gap-1">
                  <div className="w-2 h-2 rounded bg-yellow-500" />
                  <span>Orta (5-15%)</span>
                </div>
                <div className="flex items-center gap-1">
                  <div className="w-2 h-2 rounded bg-red-500" />
                  <span>Dusuk (&lt;5%)</span>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Charts Row */}
      <div className="grid gap-4 md:grid-cols-2">
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

      {/* Product Performance Table */}
      <ProductPerformanceTable
        products={stats?.products}
        isLoading={isLoading}
        title="Urun Performansi"
      />

      {/* Period Comparison */}
      {defaultStats && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Donem Karsilastirmasi</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="grid gap-3 md:grid-cols-4">
                {[1, 2, 3, 4].map((i) => (
                  <Skeleton key={i} className="h-20" />
                ))}
              </div>
            ) : (
              <div className="grid gap-3 md:grid-cols-4">
                <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/30 text-center">
                  <p className="text-xs text-blue-600 dark:text-blue-400 font-medium">Bugun</p>
                  <p className="text-lg font-bold text-blue-700 dark:text-blue-300 mt-1">
                    {formatCurrency(defaultStats.today?.totalRevenue || 0)}
                  </p>
                  <p className="text-[10px] text-blue-500 dark:text-blue-400">
                    {defaultStats.today?.totalOrders || 0} siparis
                  </p>
                </div>
                <div className="p-3 rounded-lg bg-muted text-center">
                  <p className="text-xs text-muted-foreground font-medium">Dun</p>
                  <p className="text-lg font-bold text-foreground mt-1">
                    {formatCurrency(defaultStats.yesterday?.totalRevenue || 0)}
                  </p>
                  <p className="text-[10px] text-muted-foreground">
                    {defaultStats.yesterday?.totalOrders || 0} siparis
                  </p>
                </div>
                <div className="p-3 rounded-lg bg-green-50 dark:bg-green-900/30 text-center">
                  <p className="text-xs text-green-600 dark:text-green-400 font-medium">Bu Ay</p>
                  <p className="text-lg font-bold text-green-700 dark:text-green-300 mt-1">
                    {formatCurrency(defaultStats.thisMonth?.totalRevenue || 0)}
                  </p>
                  <p className="text-[10px] text-green-500 dark:text-green-400">
                    {defaultStats.thisMonth?.totalOrders || 0} siparis
                  </p>
                </div>
                <div className="p-3 rounded-lg bg-purple-50 dark:bg-purple-900/30 text-center">
                  <p className="text-xs text-purple-600 dark:text-purple-400 font-medium">Gecen Ay</p>
                  <p className="text-lg font-bold text-purple-700 dark:text-purple-300 mt-1">
                    {formatCurrency(defaultStats.lastMonth?.totalRevenue || 0)}
                  </p>
                  <p className="text-[10px] text-purple-500 dark:text-purple-400">
                    {defaultStats.lastMonth?.totalOrders || 0} siparis
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
    </FadeIn>
  );
}
