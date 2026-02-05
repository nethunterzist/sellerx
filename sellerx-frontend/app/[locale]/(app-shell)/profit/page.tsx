"use client";

import { useState, useMemo } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useDashboardStats, useDashboardStatsByRange } from "@/hooks/useDashboardStats";
import { useStoreExpenses } from "@/hooks/queries/use-expenses";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Progress } from "@/components/ui/progress";
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Percent,
  AlertCircle,
  MinusCircle,
  PlusCircle,
  Calculator,
  Wallet,
  Receipt,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { StoreExpense } from "@/types/expense";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  ProfitFilters,
  ProductProfitTable,
  type ProfitPeriod,
  type ProfitFilter,
} from "@/components/profit";
import {
  format,
  startOfDay,
  endOfDay,
  startOfMonth,
  endOfMonth,
  subDays,
  subMonths,
} from "date-fns";

function ProfitBreakdownCard({
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
        <CardHeader>
          <CardTitle className="text-lg">
            <Skeleton className="h-5 w-32" />
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
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
        <CardTitle className="text-lg flex items-center gap-2">
          {isPositive ? (
            <PlusCircle className="h-5 w-5 text-green-600 dark:text-green-400" />
          ) : (
            <MinusCircle className="h-5 w-5 text-red-600 dark:text-red-400" />
          )}
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {items.map((item, index) => (
          <div
            key={index}
            className="flex justify-between items-center py-1 border-b border-border last:border-0"
          >
            <span className="text-muted-foreground text-sm">{item.label}</span>
            <span
              className={cn(
                "font-medium",
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
          <span className="font-semibold">Toplam</span>
          <span
            className={cn(
              "font-bold text-lg",
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

// Helper function to calculate date range based on period
function getDateRangeForPeriod(
  period: ProfitPeriod,
  customRange?: { startDate: string; endDate: string }
): { startDate: string; endDate: string } | null {
  const today = new Date();
  const formatDate = (date: Date) => format(date, "yyyy-MM-dd");

  switch (period) {
    case "today":
      return {
        startDate: formatDate(startOfDay(today)),
        endDate: formatDate(endOfDay(today)),
      };
    case "yesterday": {
      const yesterday = subDays(today, 1);
      return {
        startDate: formatDate(startOfDay(yesterday)),
        endDate: formatDate(endOfDay(yesterday)),
      };
    }
    case "thisMonth":
      return {
        startDate: formatDate(startOfMonth(today)),
        endDate: formatDate(endOfDay(today)),
      };
    case "lastMonth": {
      const lastMonth = subMonths(today, 1);
      return {
        startDate: formatDate(startOfMonth(lastMonth)),
        endDate: formatDate(endOfMonth(lastMonth)),
      };
    }
    case "custom":
      return customRange || null;
    default:
      return null;
  }
}

// Period label mapping
const periodLabels: Record<ProfitPeriod, string> = {
  today: "Bugun",
  yesterday: "Dun",
  thisMonth: "Bu Ay",
  lastMonth: "Gecen Ay",
  custom: "Ozel Tarih",
};

export default function ProfitPage() {
  const { formatCurrency } = useCurrency();
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Filter state
  const [selectedPeriod, setSelectedPeriod] = useState<ProfitPeriod>("today");
  const [customDateRange, setCustomDateRange] = useState<{ startDate: string; endDate: string } | undefined>();
  const [profitFilter, setProfitFilter] = useState<ProfitFilter>("all");
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  // Calculate date range based on selected period
  const dateRange = useMemo(
    () => getDateRangeForPeriod(selectedPeriod, customDateRange),
    [selectedPeriod, customDateRange]
  );

  // Use range-based query when period is selected
  const { data: rangeStats, isLoading: rangeStatsLoading } = useDashboardStatsByRange(
    storeId || undefined,
    dateRange?.startDate,
    dateRange?.endDate,
    periodLabels[selectedPeriod]
  );

  // Fallback to regular stats for "today"
  const { data: stats, isLoading: statsLoading } = useDashboardStats(storeId || undefined);

  // Use range stats when available, otherwise use today's data from regular stats
  const currentStats = selectedPeriod === "today" && !rangeStats ? stats?.today : rangeStats;
  const isStatsLoading = selectedPeriod === "today" && !rangeStats ? statsLoading : rangeStatsLoading;

  const { data: expensesData, isLoading: expensesLoading } = useStoreExpenses(storeId || "");
  const expenses = expensesData?.expenses || [];

  const isLoading = storeLoading || isStatsLoading;

  // Extract unique categories from products
  const categories = useMemo(() => {
    if (!currentStats?.products) return [];
    const uniqueCategories = new Set<string>();
    currentStats.products.forEach((p) => {
      if (p.categoryName) {
        uniqueCategories.add(p.categoryName);
      }
    });
    return Array.from(uniqueCategories).sort();
  }, [currentStats?.products]);

  // Current period data
  const revenue = currentStats?.totalRevenue || 0;
  const grossProfit = currentStats?.grossProfit || 0;
  const commission = currentStats?.totalEstimatedCommission || 0;
  const vatDifference = currentStats?.vatDifference || 0;
  const stoppage = currentStats?.totalStoppage || 0;
  const productCost = currentStats?.totalProductCosts || 0;
  const shippingCost = currentStats?.totalShippingCost || 0;
  const returnCost = currentStats?.returnCost || 0;
  const invoicedDeductions = currentStats?.invoicedDeductions || 0;
  const totalExpenseAmount = currentStats?.totalExpenseAmount || 0;

  // Platform ucretleri (stopaj haric - stopaj ayri gosteriliyor)
  const otherPlatformFees =
    (currentStats?.internationalServiceFee || 0) +
    (currentStats?.overseasOperationFee || 0) +
    (currentStats?.terminDelayFee || 0) +
    (currentStats?.platformServiceFee || 0) +
    (currentStats?.invoiceCreditFee || 0) +
    (currentStats?.unsuppliedFee || 0) +
    (currentStats?.azOverseasOperationFee || 0) +
    (currentStats?.azPlatformServiceFee || 0) +
    (currentStats?.packagingServiceFee || 0) +
    (currentStats?.warehouseServiceFee || 0) +
    (currentStats?.callCenterFee || 0) +
    (currentStats?.photoShootingFee || 0) +
    (currentStats?.integrationFee || 0) +
    (currentStats?.storageServiceFee || 0) +
    (currentStats?.otherPlatformFees || 0);

  // Use totalMonthlyAmount from API or calculate from expenses
  const monthlyExpenses = expensesData?.totalMonthlyAmount || expenses.reduce(
    (sum: number, e: StoreExpense) => sum + e.amount,
    0
  );

  // Use backend's net profit (correctly calculated with all deductions)
  const netProfit = currentStats?.netProfit || 0;
  const totalDeductions = commission + stoppage + otherPlatformFees + shippingCost + returnCost + invoicedDeductions + totalExpenseAmount;
  const netMargin = revenue > 0 ? (netProfit / revenue) * 100 : 0;
  const grossMargin = revenue > 0 ? (grossProfit / revenue) * 100 : 0;

  // Income items
  const incomeItems = [
    { label: "Satis Geliri", value: revenue },
    { label: "KDV Farki (Alim-Satim)", value: vatDifference, color: vatDifference >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400" },
  ];

  // Expense items (sifir olanlari gizle)
  const allExpenseItems = [
    { label: "Urun Maliyeti", value: productCost },
    { label: "Tahmini Komisyon", value: commission },
    { label: "Stopaj", value: stoppage },
    { label: "Platform Ucretleri", value: otherPlatformFees },
    { label: "Kargo Maliyeti", value: shippingCost },
    { label: "Iade Maliyeti", value: returnCost },
    { label: "Kesilen Faturalar", value: invoicedDeductions },
    { label: "Giderler", value: totalExpenseAmount },
  ];
  const expenseItems = allExpenseItems.filter((item) => item.value > 0);

  // Handle custom date range change
  const handleCustomDateRangeChange = (range: { startDate: string; endDate: string } | null) => {
    setCustomDateRange(range || undefined);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <p className="text-sm text-muted-foreground">
          Gelir ve giderlerinizi detayli analiz edin
        </p>
      </div>

      {!storeId && !isLoading && (
        <Card className="border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-900/20">
          <CardContent className="flex items-center gap-3 py-4">
            <AlertCircle className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
            <p className="text-yellow-800 dark:text-yellow-200">
              Karlilik analizini goruntulemek icin bir magaza secin
            </p>
          </CardContent>
        </Card>
      )}

      {/* Filters */}
      {storeId && (
        <ProfitFilters
          selectedPeriod={selectedPeriod}
          onPeriodChange={setSelectedPeriod}
          customDateRange={customDateRange}
          onCustomDateRangeChange={handleCustomDateRangeChange}
          profitFilter={profitFilter}
          onProfitFilterChange={setProfitFilter}
          selectedCategory={selectedCategory}
          categories={categories}
          onCategoryChange={setSelectedCategory}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />
      )}

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card className={cn(netProfit >= 0 ? "border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20" : "border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20")}>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className={cn("p-2 rounded-full", netProfit >= 0 ? "bg-green-100 dark:bg-green-900/30" : "bg-red-100 dark:bg-red-900/30")}>
                {netProfit >= 0 ? (
                  <TrendingUp className="h-6 w-6 text-green-600 dark:text-green-400" />
                ) : (
                  <TrendingDown className="h-6 w-6 text-red-600 dark:text-red-400" />
                )}
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Net Kar/Zarar</p>
                <p className={cn("text-2xl font-bold", netProfit >= 0 ? "text-green-700 dark:text-green-300" : "text-red-700 dark:text-red-300")}>
                  {netProfit >= 0 ? "+" : ""}{formatCurrency(netProfit)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-full bg-blue-100 dark:bg-blue-900/30">
                <DollarSign className="h-6 w-6 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Toplam Gelir</p>
                <p className="text-2xl font-bold text-blue-700 dark:text-blue-300">
                  {formatCurrency(revenue)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-full bg-purple-100 dark:bg-purple-900/30">
                <Wallet className="h-6 w-6 text-purple-600 dark:text-purple-400" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Brut Kar</p>
                <p className="text-2xl font-bold text-purple-700 dark:text-purple-300">
                  {formatCurrency(grossProfit)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className={cn("p-2 rounded-full", netMargin >= 15 ? "bg-green-100 dark:bg-green-900/30" : netMargin >= 5 ? "bg-yellow-100 dark:bg-yellow-900/30" : "bg-red-100 dark:bg-red-900/30")}>
                <Percent className={cn("h-6 w-6", netMargin >= 15 ? "text-green-600 dark:text-green-400" : netMargin >= 5 ? "text-yellow-600 dark:text-yellow-400" : "text-red-600 dark:text-red-400")} />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Net Kar Marji</p>
                <p className={cn("text-2xl font-bold", netMargin >= 15 ? "text-green-700 dark:text-green-300" : netMargin >= 5 ? "text-yellow-700 dark:text-yellow-300" : "text-red-700 dark:text-red-300")}>
                  {netMargin.toFixed(1)}%
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Profit Breakdown */}
      <div className="grid gap-6 md:grid-cols-2">
        <ProfitBreakdownCard
          title="Gelirler"
          items={incomeItems}
          total={revenue + vatDifference}
          isPositive={true}
          isLoading={isLoading}
          formatCurrency={formatCurrency}
        />
        <ProfitBreakdownCard
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
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Calculator className="h-5 w-5" />
            Kar Marji Dagilimi
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-32" />
          ) : (
            <div className="space-y-4">
              <div>
                <div className="flex justify-between text-sm mb-1">
                  <span>Brut Kar Marji</span>
                  <span className="font-medium">{grossMargin.toFixed(1)}%</span>
                </div>
                <Progress
                  value={Math.min(Math.max(grossMargin, 0), 100)}
                  className="h-3 [&>div]:bg-purple-500"
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
                  className={cn("h-3", netMargin >= 15 ? "[&>div]:bg-green-500" : netMargin >= 5 ? "[&>div]:bg-yellow-500" : "[&>div]:bg-red-500")}
                />
              </div>
              <div className="flex items-center justify-between pt-4 border-t border-border">
                <div className="flex items-center gap-4 text-sm">
                  <div className="flex items-center gap-1">
                    <div className="w-3 h-3 rounded bg-green-500" />
                    <span>Iyi (15%+)</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <div className="w-3 h-3 rounded bg-yellow-500" />
                    <span>Orta (5-15%)</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <div className="w-3 h-3 rounded bg-red-500" />
                    <span>Dusuk (&lt;5%)</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Enhanced Product Profitability Table */}
      <ProductProfitTable
        products={currentStats?.products}
        isLoading={isLoading}
        formatCurrency={formatCurrency}
        searchQuery={searchQuery}
        profitFilter={profitFilter}
        selectedCategory={selectedCategory}
      />

      {/* Monthly Expenses Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Receipt className="h-5 w-5" />
            Aylik Sabit Giderler
          </CardTitle>
        </CardHeader>
        <CardContent>
          {expensesLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="flex justify-between">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-4 w-24" />
                </div>
              ))}
            </div>
          ) : expenses && expenses.length > 0 ? (
            <div className="space-y-3">
              {expenses.map((expense: StoreExpense) => (
                <div key={expense.id} className="flex justify-between items-center py-2 border-b border-border last:border-0">
                  <span className="text-muted-foreground">{expense.expenseCategoryName || expense.name}</span>
                  <span className="font-medium">{formatCurrency(expense.amount)}</span>
                </div>
              ))}
              <div className="flex justify-between items-center pt-3 border-t-2 border-border">
                <span className="font-semibold">Toplam Aylik Gider</span>
                <span className="font-bold text-lg text-red-600 dark:text-red-400">
                  {formatCurrency(monthlyExpenses)}
                </span>
              </div>
              <div className="flex justify-between items-center text-sm text-muted-foreground">
                <span>Gunluk ortalama</span>
                <span>{formatCurrency(monthlyExpenses / 30)}/gun</span>
              </div>
            </div>
          ) : (
            <div className="text-center py-6 text-muted-foreground">
              <Receipt className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p className="text-sm">Henuz gider tanimlanmamis</p>
              <p className="text-xs mt-1">Ayarlar sayfasindan gider ekleyebilirsiniz</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
