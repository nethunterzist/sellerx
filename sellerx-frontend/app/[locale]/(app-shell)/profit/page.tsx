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
  Filter,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  ProfitFilters,
  ProductProfitTable,
  CollapsibleExpenseBreakdown,
  ProfitLossSideBySide,
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
  const [customMarginThreshold, setCustomMarginThreshold] = useState<number | null>(null);
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

  const { isLoading: expensesLoading } = useStoreExpenses(storeId || "");

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

  // Filter products based on search/filter criteria (same logic as ProductProfitTable)
  const filteredProducts = useMemo(() => {
    if (!currentStats?.products) return [];

    return currentStats.products.filter((p) => {
      // Search filter - using Turkish locale for proper İ/ı handling
      if (searchQuery) {
        const query = searchQuery.toLocaleLowerCase('tr-TR');
        const matchesSearch =
          p.productName.toLocaleLowerCase('tr-TR').includes(query) ||
          p.barcode.toLocaleLowerCase('tr-TR').includes(query) ||
          (p.brand?.toLocaleLowerCase('tr-TR').includes(query) ?? false);
        if (!matchesSearch) return false;
      }

      // Profit/Loss filter
      if (profitFilter === "profit" && p.grossProfit < 0) return false;
      if (profitFilter === "loss" && p.grossProfit >= 0) return false;

      // Custom margin threshold filter
      if (profitFilter === "custom" && customMarginThreshold !== null) {
        const margin = p.revenue > 0 ? (p.grossProfit / p.revenue) * 100 : 0;
        if (margin < customMarginThreshold) return false;
      }

      // Category filter
      if (selectedCategory && p.categoryName !== selectedCategory) return false;

      return true;
    });
  }, [currentStats?.products, searchQuery, profitFilter, customMarginThreshold, selectedCategory]);

  // Check if any filter is active
  const isFilterActive = searchQuery || profitFilter !== "all" || selectedCategory;

  // Calculate filtered stats from filtered products
  const filteredStats = useMemo(() => {
    if (!isFilterActive || filteredProducts.length === 0) return null;

    const totalRevenue = filteredProducts.reduce((sum, p) => sum + p.revenue, 0);
    const totalGrossProfit = filteredProducts.reduce((sum, p) => sum + p.grossProfit, 0);
    const totalProductCost = filteredProducts.reduce((sum, p) => sum + (p.productCost || 0), 0);
    const totalCommission = filteredProducts.reduce((sum, p) => sum + (p.estimatedCommission || 0), 0);
    const totalNetProfit = totalGrossProfit - totalCommission;
    const totalNetMargin = totalRevenue > 0 ? (totalNetProfit / totalRevenue) * 100 : 0;
    const totalGrossMargin = totalRevenue > 0 ? (totalGrossProfit / totalRevenue) * 100 : 0;

    return {
      revenue: totalRevenue,
      grossProfit: totalGrossProfit,
      productCost: totalProductCost,
      commission: totalCommission,
      netProfit: totalNetProfit,
      netMargin: totalNetMargin,
      grossMargin: totalGrossMargin,
      productCount: filteredProducts.length,
    };
  }, [filteredProducts, isFilterActive]);

  // Current period data
  const revenue = currentStats?.totalRevenue || 0;
  const grossProfit = currentStats?.grossProfit || 0;
  const commission = currentStats?.totalEstimatedCommission || 0;
  const vatDifference = currentStats?.vatDifference || 0;
  const stoppage = currentStats?.totalStoppage || 0;
  const productCost = currentStats?.totalProductCosts || 0;
  const shippingCost = currentStats?.totalShippingCost || 0;
  const returnCost = currentStats?.returnCost || 0;
  const totalExpenseAmount = currentStats?.totalExpenseAmount || 0;

  // Use backend's net profit (correctly calculated with all deductions)
  const netProfit = currentStats?.netProfit || 0;
  const netMargin = revenue > 0 ? (netProfit / revenue) * 100 : 0;
  const grossMargin = revenue > 0 ? (grossProfit / revenue) * 100 : 0;

  // Display values - use filtered stats when filter is active
  const displayRevenue = filteredStats?.revenue ?? revenue;
  const displayGrossProfit = filteredStats?.grossProfit ?? grossProfit;
  const displayNetProfit = filteredStats?.netProfit ?? netProfit;
  const displayNetMargin = filteredStats?.netMargin ?? netMargin;
  const displayGrossMargin = filteredStats?.grossMargin ?? grossMargin;

  // Income items
  const incomeItems = [
    { label: "Satis Geliri", value: revenue },
    { label: "KDV Farki (Alim-Satim)", value: vatDifference, color: vatDifference >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400" },
  ];

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
          customMarginThreshold={customMarginThreshold}
          onCustomMarginThresholdChange={setCustomMarginThreshold}
          selectedCategory={selectedCategory}
          categories={categories}
          onCategoryChange={setSelectedCategory}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />
      )}

      {/* Filter Active Indicator */}
      {isFilterActive && filteredStats && (
        <Card className="border-blue-200 dark:border-blue-800 bg-blue-50 dark:bg-blue-900/20">
          <CardContent className="flex items-center justify-between py-3">
            <div className="flex items-center gap-3">
              <Filter className="h-4 w-4 text-blue-600 dark:text-blue-400" />
              <p className="text-sm text-blue-800 dark:text-blue-200">
                <span className="font-medium">{filteredStats.productCount} urun</span> filtrelendi
                {searchQuery && (
                  <span className="ml-1">
                    (arama: &quot;{searchQuery}&quot;)
                  </span>
                )}
                {profitFilter === "profit" && " - Sadece kar edenler"}
                {profitFilter === "loss" && " - Sadece zarar edenler"}
                {profitFilter === "custom" && customMarginThreshold !== null && ` - >=${customMarginThreshold}% kar marji`}
                {selectedCategory && ` - ${selectedCategory}`}
              </p>
            </div>
            <button
              onClick={() => {
                setSearchQuery("");
                setProfitFilter("all");
                setSelectedCategory(null);
                setCustomMarginThreshold(null);
              }}
              className="flex items-center gap-1 text-sm text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-200"
            >
              <X className="h-4 w-4" />
              Temizle
            </button>
          </CardContent>
        </Card>
      )}

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card className={cn(displayNetProfit >= 0 ? "border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20" : "border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20")}>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className={cn("p-2 rounded-full", displayNetProfit >= 0 ? "bg-green-100 dark:bg-green-900/30" : "bg-red-100 dark:bg-red-900/30")}>
                {displayNetProfit >= 0 ? (
                  <TrendingUp className="h-6 w-6 text-green-600 dark:text-green-400" />
                ) : (
                  <TrendingDown className="h-6 w-6 text-red-600 dark:text-red-400" />
                )}
              </div>
              <div>
                <p className="text-sm text-muted-foreground">
                  Net Kar/Zarar
                  {isFilterActive && filteredStats && <span className="ml-1 text-blue-600">*</span>}
                </p>
                <p className={cn("text-2xl font-bold", displayNetProfit >= 0 ? "text-green-700 dark:text-green-300" : "text-red-700 dark:text-red-300")}>
                  {displayNetProfit >= 0 ? "+" : ""}{formatCurrency(displayNetProfit)}
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
                <p className="text-sm text-muted-foreground">
                  Toplam Gelir
                  {isFilterActive && filteredStats && <span className="ml-1 text-blue-600">*</span>}
                </p>
                <p className="text-2xl font-bold text-blue-700 dark:text-blue-300">
                  {formatCurrency(displayRevenue)}
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
                <p className="text-sm text-muted-foreground">
                  Brut Kar
                  {isFilterActive && filteredStats && <span className="ml-1 text-blue-600">*</span>}
                </p>
                <p className="text-2xl font-bold text-purple-700 dark:text-purple-300">
                  {formatCurrency(displayGrossProfit)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className={cn("p-2 rounded-full", displayNetMargin >= 15 ? "bg-green-100 dark:bg-green-900/30" : displayNetMargin >= 5 ? "bg-yellow-100 dark:bg-yellow-900/30" : "bg-red-100 dark:bg-red-900/30")}>
                <Percent className={cn("h-6 w-6", displayNetMargin >= 15 ? "text-green-600 dark:text-green-400" : displayNetMargin >= 5 ? "text-yellow-600 dark:text-yellow-400" : "text-red-600 dark:text-red-400")} />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">
                  Net Kar Marji
                  {isFilterActive && filteredStats && <span className="ml-1 text-blue-600">*</span>}
                </p>
                <p className={cn("text-2xl font-bold", displayNetMargin >= 15 ? "text-green-700 dark:text-green-300" : displayNetMargin >= 5 ? "text-yellow-700 dark:text-yellow-300" : "text-red-700 dark:text-red-300")}>
                  {displayNetMargin.toFixed(1)}%
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
        <CollapsibleExpenseBreakdown
          isLoading={isLoading || expensesLoading}
          formatCurrency={formatCurrency}
          productCost={productCost}
          commission={commission}
          stoppage={stoppage}
          shippingCost={shippingCost}
          returnCost={returnCost}
          platformFees={{
            internationalServiceFee: currentStats?.internationalServiceFee,
            overseasOperationFee: currentStats?.overseasOperationFee,
            terminDelayFee: currentStats?.terminDelayFee,
            platformServiceFee: currentStats?.platformServiceFee,
            invoiceCreditFee: currentStats?.invoiceCreditFee,
            unsuppliedFee: currentStats?.unsuppliedFee,
            azOverseasOperationFee: currentStats?.azOverseasOperationFee,
            azPlatformServiceFee: currentStats?.azPlatformServiceFee,
            packagingServiceFee: currentStats?.packagingServiceFee,
            warehouseServiceFee: currentStats?.warehouseServiceFee,
            callCenterFee: currentStats?.callCenterFee,
            photoShootingFee: currentStats?.photoShootingFee,
            integrationFee: currentStats?.integrationFee,
            storageServiceFee: currentStats?.storageServiceFee,
            otherPlatformFees: currentStats?.otherPlatformFees,
          }}
          invoicedDeductions={{
            advertisingFees: currentStats?.invoicedAdvertisingFees,
            penaltyFees: currentStats?.invoicedPenaltyFees,
            internationalFees: currentStats?.invoicedInternationalFees,
            otherFees: currentStats?.invoicedOtherFees,
            refunds: currentStats?.invoicedRefunds,
          }}
          expensesByCategory={currentStats?.expensesByCategory}
          totalExpenseAmount={totalExpenseAmount}
        />
      </div>

      {/* Margin Visualization */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Calculator className="h-5 w-5" />
            Kar Marji Dagilimi
            {isFilterActive && filteredStats && <span className="text-blue-600 text-sm font-normal ml-2">(Filtrelenmis)</span>}
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
                  <span className="font-medium">{displayGrossMargin.toFixed(1)}%</span>
                </div>
                <Progress
                  value={Math.min(Math.max(displayGrossMargin, 0), 100)}
                  className="h-3 [&>div]:bg-purple-500"
                />
              </div>
              <div>
                <div className="flex justify-between text-sm mb-1">
                  <span>Net Kar Marji</span>
                  <span className={cn("font-medium", displayNetMargin >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400")}>
                    {displayNetMargin.toFixed(1)}%
                  </span>
                </div>
                <Progress
                  value={Math.min(Math.max(displayNetMargin, 0), 100)}
                  className={cn("h-3", displayNetMargin >= 15 ? "[&>div]:bg-green-500" : displayNetMargin >= 5 ? "[&>div]:bg-yellow-500" : "[&>div]:bg-red-500")}
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

      {/* Profit/Loss Side by Side Comparison */}
      <ProfitLossSideBySide
        products={currentStats?.products}
        isLoading={isLoading}
        formatCurrency={formatCurrency}
      />

      {/* Enhanced Product Profitability Table */}
      <ProductProfitTable
        products={currentStats?.products}
        isLoading={isLoading}
        formatCurrency={formatCurrency}
        searchQuery={searchQuery}
        profitFilter={profitFilter}
        selectedCategory={selectedCategory}
        customMarginThreshold={customMarginThreshold}
      />

    </div>
  );
}
