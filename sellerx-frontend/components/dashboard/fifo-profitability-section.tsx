"use client";

import { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useProfitabilityAnalysis } from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { cn } from "@/lib/utils";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Package,
  BarChart3,
  Calendar,
  ChevronDown,
  AlertTriangle,
} from "lucide-react";
import type { ProductProfitability } from "@/types/purchasing";

type Tab = "overview" | "top" | "least";

interface FifoProfitabilitySectionProps {
  className?: string;
  defaultOpen?: boolean;
}

export function FifoProfitabilitySection({
  className,
  defaultOpen = false,
}: FifoProfitabilitySectionProps) {
  const { formatCurrency } = useCurrency();
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const [activeTab, setActiveTab] = useState<Tab>("overview");

  // Date range - default to last 30 days
  const [dateRange, setDateRange] = useState(() => {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);
    return {
      startDate: startDate.toISOString().split("T")[0],
      endDate: endDate.toISOString().split("T")[0],
    };
  });

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: profitability, isLoading: profitabilityLoading } =
    useProfitabilityAnalysis(
      storeId || undefined,
      dateRange.startDate,
      dateRange.endDate
    );

  // Preset date ranges
  const setPresetRange = (days: number) => {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);
    setDateRange({
      startDate: startDate.toISOString().split("T")[0],
      endDate: endDate.toISOString().split("T")[0],
    });
  };

  // Set month range
  const setMonthRange = (monthsBack: number) => {
    const now = new Date();
    const startDate = new Date(now.getFullYear(), now.getMonth() - monthsBack, 1);
    const endDate = new Date(now.getFullYear(), now.getMonth() - monthsBack + 1, 0);
    setDateRange({
      startDate: startDate.toISOString().split("T")[0],
      endDate: endDate.toISOString().split("T")[0],
    });
  };

  // Format date for display
  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString("tr-TR", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  };

  // Get margin badge color
  const getMarginColor = (margin: number): string => {
    if (margin >= 40) return "text-green-600 dark:text-green-400";
    if (margin >= 20) return "text-yellow-600 dark:text-yellow-400";
    if (margin >= 0) return "text-orange-600 dark:text-orange-400";
    return "text-red-600 dark:text-red-400";
  };

  // Get margin background color
  const getMarginBgColor = (margin: number): string => {
    if (margin >= 40) return "bg-green-100 dark:bg-green-900/30";
    if (margin >= 20) return "bg-yellow-100 dark:bg-yellow-900/30";
    if (margin >= 0) return "bg-orange-100 dark:bg-orange-900/30";
    return "bg-red-100 dark:bg-red-900/30";
  };

  // Product row component
  const ProductRow = ({
    product,
    rank,
  }: {
    product: ProductProfitability;
    rank: number;
  }) => (
    <tr className="border-b border-border last:border-0 hover:bg-muted/20">
      <td className="p-3 text-center">
        <span className="text-sm font-medium text-muted-foreground">#{rank}</span>
      </td>
      <td className="p-3">
        <div className="flex items-center gap-3">
          {product.productImage ? (
            <img
              src={product.productImage}
              alt=""
              className="w-10 h-10 rounded object-cover"
            />
          ) : (
            <div className="w-10 h-10 rounded bg-muted flex items-center justify-center">
              <Package className="h-5 w-5 text-muted-foreground" />
            </div>
          )}
          <div className="min-w-0">
            <div className="flex items-center gap-1.5">
              <p className="text-sm font-medium text-foreground line-clamp-1">
                {product.productName}
              </p>
              {product.costEstimated && (
                <span
                  className="inline-flex items-center gap-0.5 shrink-0 px-1.5 py-0.5 rounded text-[10px] font-medium bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400"
                  title="Bu urunun FIFO stogu tukenmis, maliyet son bilinen maliyet uzerinden hesaplandi"
                >
                  <AlertTriangle className="h-2.5 w-2.5" />
                  Tahmini
                </span>
              )}
            </div>
            <p className="text-xs text-muted-foreground font-mono">
              {product.barcode}
            </p>
          </div>
        </div>
      </td>
      <td className="p-3 text-right text-sm text-muted-foreground">
        {product.quantitySold.toLocaleString()}
      </td>
      <td className="p-3 text-right text-sm text-foreground">
        {formatCurrency(product.revenue)}
      </td>
      <td className="p-3 text-right text-sm text-muted-foreground">
        {formatCurrency(product.cost)}
      </td>
      <td className="p-3 text-right text-sm font-medium">
        <span
          className={
            product.profit >= 0
              ? "text-green-600 dark:text-green-400"
              : "text-red-600 dark:text-red-400"
          }
        >
          {product.profit >= 0 ? "+" : ""}
          {formatCurrency(product.profit)}
        </span>
      </td>
      <td className="p-3 text-center">
        <span
          className={cn(
            "inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium",
            getMarginBgColor(product.margin),
            getMarginColor(product.margin)
          )}
        >
          {product.margin >= 0 ? (
            <TrendingUp className="h-3 w-3 mr-1" />
          ) : (
            <TrendingDown className="h-3 w-3 mr-1" />
          )}
          %{product.margin.toFixed(1)}
        </span>
      </td>
    </tr>
  );

  // No store selected
  if (!storeId && !storeLoading) {
    return null;
  }

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen} className={className}>
      <div className="bg-card rounded-lg border border-border">
        <CollapsibleTrigger asChild>
          <button className="w-full flex items-center justify-between p-4 hover:bg-muted/30 transition-colors">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-purple-50 dark:bg-purple-900/20">
                <BarChart3 className="h-5 w-5 text-purple-600 dark:text-purple-400" />
              </div>
              <div className="text-left">
                <h3 className="text-sm font-semibold text-foreground">
                  FIFO Karlilik Analizi
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {profitability
                    ? `${formatCurrency(profitability.grossProfit || 0)} brut kar | %${(profitability.grossMargin || 0).toFixed(1)} marj`
                    : "Detayli kar/zarar raporu"}
                </p>
              </div>
            </div>
            <ChevronDown
              className={cn(
                "h-5 w-5 text-muted-foreground transition-transform duration-200",
                isOpen && "rotate-180"
              )}
            />
          </button>
        </CollapsibleTrigger>

        <CollapsibleContent>
          <div className="border-t border-border">
            {/* Date Range Selector */}
            <div className="p-4 bg-muted/30 flex flex-wrap items-center gap-3">
              <div className="flex items-center gap-1 bg-background rounded-lg p-1">
                <button
                  onClick={() => setPresetRange(7)}
                  className="px-3 py-1.5 text-xs rounded-md hover:bg-muted transition-colors"
                >
                  7 Gun
                </button>
                <button
                  onClick={() => setPresetRange(30)}
                  className="px-3 py-1.5 text-xs rounded-md hover:bg-muted transition-colors"
                >
                  30 Gun
                </button>
                <button
                  onClick={() => setMonthRange(0)}
                  className="px-3 py-1.5 text-xs rounded-md hover:bg-muted transition-colors"
                >
                  Bu Ay
                </button>
                <button
                  onClick={() => setMonthRange(1)}
                  className="px-3 py-1.5 text-xs rounded-md hover:bg-muted transition-colors"
                >
                  Gecen Ay
                </button>
              </div>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Calendar className="h-4 w-4" />
                <input
                  type="date"
                  value={dateRange.startDate}
                  onChange={(e) =>
                    setDateRange((prev) => ({ ...prev, startDate: e.target.value }))
                  }
                  className="px-2 py-1 rounded border border-border bg-background"
                />
                <span>-</span>
                <input
                  type="date"
                  value={dateRange.endDate}
                  onChange={(e) =>
                    setDateRange((prev) => ({ ...prev, endDate: e.target.value }))
                  }
                  className="px-2 py-1 rounded border border-border bg-background"
                />
              </div>
            </div>

            {/* Loading state */}
            {profitabilityLoading && (
              <div className="p-6">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                  {[...Array(4)].map((_, i) => (
                    <div key={i} className="h-24 bg-muted rounded-lg animate-pulse" />
                  ))}
                </div>
                <div className="h-64 bg-muted rounded-lg animate-pulse" />
              </div>
            )}

            {!profitabilityLoading && profitability && (
              <>
                {/* Summary Cards */}
                <div className="p-4 grid grid-cols-2 md:grid-cols-4 gap-3">
                  {/* Revenue */}
                  <div className="bg-muted/30 rounded-lg p-3">
                    <div className="flex items-center gap-2 mb-1">
                      <DollarSign className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                      <span className="text-xs text-muted-foreground">Toplam Satis</span>
                    </div>
                    <p className="text-lg font-bold text-foreground">
                      {formatCurrency(profitability.totalRevenue || 0)}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {profitability.totalOrders || 0} siparis
                    </p>
                  </div>

                  {/* Cost */}
                  <div className="bg-muted/30 rounded-lg p-3">
                    <div className="flex items-center gap-2 mb-1">
                      <Package className="h-4 w-4 text-orange-600 dark:text-orange-400" />
                      <span className="text-xs text-muted-foreground">FIFO Maliyet</span>
                    </div>
                    <p className="text-lg font-bold text-foreground">
                      {formatCurrency(profitability.totalCost || 0)}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {(profitability.totalQuantitySold || 0).toLocaleString()} adet
                    </p>
                  </div>

                  {/* Gross Profit */}
                  <div className="bg-muted/30 rounded-lg p-3">
                    <div className="flex items-center gap-2 mb-1">
                      {(profitability.grossProfit || 0) >= 0 ? (
                        <TrendingUp className="h-4 w-4 text-green-600 dark:text-green-400" />
                      ) : (
                        <TrendingDown className="h-4 w-4 text-red-600 dark:text-red-400" />
                      )}
                      <span className="text-xs text-muted-foreground">Brut Kar</span>
                    </div>
                    <p
                      className={cn(
                        "text-lg font-bold",
                        (profitability.grossProfit || 0) >= 0
                          ? "text-green-600 dark:text-green-400"
                          : "text-red-600 dark:text-red-400"
                      )}
                    >
                      {(profitability.grossProfit || 0) >= 0 ? "+" : ""}
                      {formatCurrency(profitability.grossProfit || 0)}
                    </p>
                  </div>

                  {/* Margin */}
                  <div className="bg-muted/30 rounded-lg p-3">
                    <div className="flex items-center gap-2 mb-1">
                      <BarChart3 className="h-4 w-4 text-purple-600 dark:text-purple-400" />
                      <span className="text-xs text-muted-foreground">Brut Marj</span>
                    </div>
                    <p
                      className={cn(
                        "text-lg font-bold",
                        getMarginColor(profitability.grossMargin || 0)
                      )}
                    >
                      %{(profitability.grossMargin || 0).toFixed(1)}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {(profitability.grossMargin || 0) >= 40
                        ? "Mukemmel"
                        : (profitability.grossMargin || 0) >= 20
                          ? "Iyi"
                          : (profitability.grossMargin || 0) >= 0
                            ? "Dusuk"
                            : "Zarar"}
                    </p>
                  </div>
                </div>

                {/* Tabs */}
                <div className="px-4">
                  <div className="flex gap-1 bg-muted/50 rounded-lg p-1 w-fit">
                    <button
                      onClick={() => setActiveTab("overview")}
                      className={cn(
                        "px-3 py-1.5 text-xs rounded-md transition-colors",
                        activeTab === "overview"
                          ? "bg-background shadow-sm font-medium"
                          : "text-muted-foreground hover:text-foreground"
                      )}
                    >
                      Gunluk Trend
                    </button>
                    <button
                      onClick={() => setActiveTab("top")}
                      className={cn(
                        "px-3 py-1.5 text-xs rounded-md transition-colors flex items-center gap-1",
                        activeTab === "top"
                          ? "bg-background shadow-sm font-medium"
                          : "text-muted-foreground hover:text-foreground"
                      )}
                    >
                      <TrendingUp className="h-3 w-3 text-green-500" />
                      En Karli
                    </button>
                    <button
                      onClick={() => setActiveTab("least")}
                      className={cn(
                        "px-3 py-1.5 text-xs rounded-md transition-colors flex items-center gap-1",
                        activeTab === "least"
                          ? "bg-background shadow-sm font-medium"
                          : "text-muted-foreground hover:text-foreground"
                      )}
                    >
                      <TrendingDown className="h-3 w-3 text-red-500" />
                      En Az Karli
                    </button>
                  </div>
                </div>

                {/* Content */}
                <div className="p-4">
                  {/* Daily Trend */}
                  {activeTab === "overview" &&
                    profitability.dailyTrend &&
                    profitability.dailyTrend.length > 0 && (
                      <div className="overflow-x-auto rounded-lg border border-border">
                        <table className="w-full">
                          <thead>
                            <tr className="bg-muted/30">
                              <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                                Tarih
                              </th>
                              <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                                Siparis
                              </th>
                              <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                                Satis
                              </th>
                              <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                                Maliyet
                              </th>
                              <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                                Kar
                              </th>
                              <th className="text-center p-3 text-xs font-medium text-muted-foreground">
                                Marj
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {profitability.dailyTrend.map((day) => (
                              <tr
                                key={day.date}
                                className="border-t border-border hover:bg-muted/20"
                              >
                                <td className="p-3 text-sm text-foreground">
                                  {formatDate(day.date)}
                                </td>
                                <td className="p-3 text-sm text-muted-foreground text-right">
                                  {day.orderCount}
                                </td>
                                <td className="p-3 text-sm text-foreground text-right">
                                  {formatCurrency(day.revenue)}
                                </td>
                                <td className="p-3 text-sm text-muted-foreground text-right">
                                  {formatCurrency(day.cost)}
                                </td>
                                <td className="p-3 text-sm font-medium text-right">
                                  <span
                                    className={
                                      day.profit >= 0
                                        ? "text-green-600 dark:text-green-400"
                                        : "text-red-600 dark:text-red-400"
                                    }
                                  >
                                    {day.profit >= 0 ? "+" : ""}
                                    {formatCurrency(day.profit)}
                                  </span>
                                </td>
                                <td className="p-3 text-center">
                                  <span
                                    className={cn(
                                      "inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium",
                                      getMarginBgColor(day.margin),
                                      getMarginColor(day.margin)
                                    )}
                                  >
                                    %{day.margin.toFixed(1)}
                                  </span>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                  {/* Top Profitable Products */}
                  {activeTab === "top" && (
                    <div className="overflow-x-auto rounded-lg border border-border">
                      <table className="w-full">
                        <thead>
                          <tr className="bg-muted/30">
                            <th className="text-center p-3 text-xs font-medium text-muted-foreground w-12">
                              #
                            </th>
                            <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                              Urun
                            </th>
                            <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                              Adet
                            </th>
                            <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                              Satis
                            </th>
                            <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                              Maliyet
                            </th>
                            <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                              Kar
                            </th>
                            <th className="text-center p-3 text-xs font-medium text-muted-foreground">
                              Marj
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {profitability.topProfitable &&
                          profitability.topProfitable.length > 0 ? (
                            profitability.topProfitable.map((product, index) => (
                              <ProductRow
                                key={product.barcode}
                                product={product}
                                rank={index + 1}
                              />
                            ))
                          ) : (
                            <tr>
                              <td colSpan={7} className="p-8 text-center">
                                <TrendingUp className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
                                <p className="text-sm text-muted-foreground">
                                  Secilen donemde veri bulunamadi
                                </p>
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  )}

                  {/* Least Profitable Products */}
                  {activeTab === "least" && (
                    <div className="overflow-x-auto rounded-lg border border-border">
                      <table className="w-full">
                        <thead>
                          <tr className="bg-muted/30">
                            <th className="text-center p-3 text-xs font-medium text-muted-foreground w-12">
                              #
                            </th>
                            <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                              Urun
                            </th>
                            <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                              Adet
                            </th>
                            <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                              Satis
                            </th>
                            <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                              Maliyet
                            </th>
                            <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                              Kar
                            </th>
                            <th className="text-center p-3 text-xs font-medium text-muted-foreground">
                              Marj
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {profitability.leastProfitable &&
                          profitability.leastProfitable.length > 0 ? (
                            profitability.leastProfitable.map((product, index) => (
                              <ProductRow
                                key={product.barcode}
                                product={product}
                                rank={index + 1}
                              />
                            ))
                          ) : (
                            <tr>
                              <td colSpan={7} className="p-8 text-center">
                                <TrendingDown className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
                                <p className="text-sm text-muted-foreground">
                                  Secilen donemde veri bulunamadi
                                </p>
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                {/* Info Note */}
                <div className="px-4 pb-4">
                  <div className="bg-amber-50 dark:bg-amber-900/20 rounded-lg border border-amber-200 dark:border-amber-800 p-3">
                    <div className="flex items-start gap-2">
                      <BarChart3 className="h-4 w-4 text-amber-600 dark:text-amber-400 mt-0.5" />
                      <div>
                        <p className="text-xs font-medium text-amber-900 dark:text-amber-100">
                          FIFO Maliyet Hesabi
                        </p>
                        <p className="text-xs text-amber-700 dark:text-amber-300 mt-1">
                          Maliyetler FIFO yontemiyle hesaplanir. Komisyon, stopaj ve iade
                          maliyetleri dahil degildir.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>
        </CollapsibleContent>
      </div>
    </Collapsible>
  );
}
