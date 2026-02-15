"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useProfitabilityAnalysis } from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  ArrowLeft,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Package,
  ShoppingCart,
  BarChart3,
  Calendar,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import type { ProductProfitability } from "@/types/purchasing";

type Tab = "overview" | "top" | "least";

export default function ProfitabilityPage() {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
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

  const { data: profitability, isLoading: profitabilityLoading } = useProfitabilityAnalysis(
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

  // No store selected
  if (!storeId && !storeLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <TrendingUp className="h-16 w-16 text-muted-foreground/30 mb-4" />
        <h2 className="text-lg font-medium text-foreground mb-2">Magaza Secilmedi</h2>
        <p className="text-sm text-muted-foreground max-w-md">
          Karlilik analizini goruntulemek icin lutfen bir magaza secin.
        </p>
      </div>
    );
  }

  // Loading state
  if (profitabilityLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <div className="h-9 w-24 bg-muted rounded animate-pulse" />
          <div className="h-8 w-64 bg-muted rounded animate-pulse" />
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-28 bg-muted rounded-xl animate-pulse" />
          ))}
        </div>
        <div className="h-96 bg-muted rounded-xl animate-pulse" />
      </div>
    );
  }

  // Product profitability row component
  const ProductRow = ({ product, rank }: { product: ProductProfitability; rank: number }) => (
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
            <p className="text-sm font-medium text-foreground line-clamp-1">
              {product.productName}
            </p>
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
        <span className={product.profit >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400"}>
          {product.profit >= 0 ? "+" : ""}{formatCurrency(product.profit)}
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.push("/purchasing")}
            className="gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Geri
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Karlilik Analizi</h1>
            <p className="text-sm text-muted-foreground mt-1">
              FIFO bazli kar/zarar raporu
            </p>
          </div>
        </div>

        {/* Date Range Selector */}
        <div className="flex items-center gap-2 flex-wrap">
          <div className="flex items-center gap-1 bg-muted/50 rounded-lg p-1">
            <button
              onClick={() => setPresetRange(7)}
              className="px-3 py-1.5 text-xs rounded-md hover:bg-background transition-colors"
            >
              7 Gun
            </button>
            <button
              onClick={() => setPresetRange(30)}
              className="px-3 py-1.5 text-xs rounded-md hover:bg-background transition-colors"
            >
              30 Gun
            </button>
            <button
              onClick={() => setMonthRange(0)}
              className="px-3 py-1.5 text-xs rounded-md hover:bg-background transition-colors"
            >
              Bu Ay
            </button>
            <button
              onClick={() => setMonthRange(1)}
              className="px-3 py-1.5 text-xs rounded-md hover:bg-background transition-colors"
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
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {/* Revenue */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-900/20">
              <DollarSign className="h-4 w-4 text-blue-600 dark:text-blue-400" />
            </div>
            <span className="text-xs text-muted-foreground">Toplam Satis</span>
          </div>
          <p className="text-2xl font-bold text-foreground">
            {formatCurrency(profitability?.totalRevenue || 0)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            {profitability?.totalOrders || 0} siparis
          </p>
        </div>

        {/* Cost */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="p-2 rounded-lg bg-orange-50 dark:bg-orange-900/20">
              <Package className="h-4 w-4 text-orange-600 dark:text-orange-400" />
            </div>
            <span className="text-xs text-muted-foreground">FIFO Maliyet</span>
          </div>
          <p className="text-2xl font-bold text-foreground">
            {formatCurrency(profitability?.totalCost || 0)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            {(profitability?.totalQuantitySold || 0).toLocaleString()} adet
          </p>
        </div>

        {/* Gross Profit */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className={cn(
              "p-2 rounded-lg",
              (profitability?.grossProfit || 0) >= 0
                ? "bg-green-50 dark:bg-green-900/20"
                : "bg-red-50 dark:bg-red-900/20"
            )}>
              {(profitability?.grossProfit || 0) >= 0 ? (
                <TrendingUp className="h-4 w-4 text-green-600 dark:text-green-400" />
              ) : (
                <TrendingDown className="h-4 w-4 text-red-600 dark:text-red-400" />
              )}
            </div>
            <span className="text-xs text-muted-foreground">Brut Kar</span>
          </div>
          <p className={cn(
            "text-2xl font-bold",
            (profitability?.grossProfit || 0) >= 0
              ? "text-green-600 dark:text-green-400"
              : "text-red-600 dark:text-red-400"
          )}>
            {(profitability?.grossProfit || 0) >= 0 ? "+" : ""}
            {formatCurrency(profitability?.grossProfit || 0)}
          </p>
        </div>

        {/* Margin */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="p-2 rounded-lg bg-purple-50 dark:bg-purple-900/20">
              <BarChart3 className="h-4 w-4 text-purple-600 dark:text-purple-400" />
            </div>
            <span className="text-xs text-muted-foreground">Brut Marj</span>
          </div>
          <p className={cn(
            "text-2xl font-bold",
            getMarginColor(profitability?.grossMargin || 0)
          )}>
            %{(profitability?.grossMargin || 0).toFixed(1)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            {(profitability?.grossMargin || 0) >= 40
              ? "Mukemmel"
              : (profitability?.grossMargin || 0) >= 20
              ? "Iyi"
              : (profitability?.grossMargin || 0) >= 0
              ? "Dusuk"
              : "Zarar"}
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-muted/50 rounded-lg p-1 w-fit">
        <button
          onClick={() => setActiveTab("overview")}
          className={cn(
            "px-4 py-2 text-sm rounded-md transition-colors",
            activeTab === "overview"
              ? "bg-background shadow-sm font-medium"
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          Genel Bakis
        </button>
        <button
          onClick={() => setActiveTab("top")}
          className={cn(
            "px-4 py-2 text-sm rounded-md transition-colors flex items-center gap-1",
            activeTab === "top"
              ? "bg-background shadow-sm font-medium"
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          <TrendingUp className="h-4 w-4 text-green-500" />
          En Karli
        </button>
        <button
          onClick={() => setActiveTab("least")}
          className={cn(
            "px-4 py-2 text-sm rounded-md transition-colors flex items-center gap-1",
            activeTab === "least"
              ? "bg-background shadow-sm font-medium"
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          <TrendingDown className="h-4 w-4 text-red-500" />
          En Az Karli
        </button>
      </div>

      {/* Content based on active tab */}
      {activeTab === "overview" && profitability?.dailyTrend && profitability.dailyTrend.length > 0 && (
        <div className="bg-card rounded-xl border border-border">
          <div className="p-4 border-b border-border">
            <h3 className="font-semibold text-foreground">Gunluk Karlilik Trendi</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  <th className="text-left p-3 text-xs font-medium text-muted-foreground">Tarih</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Siparis</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Satis</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Maliyet</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Kar</th>
                  <th className="text-center p-3 text-xs font-medium text-muted-foreground">Marj</th>
                </tr>
              </thead>
              <tbody>
                {profitability.dailyTrend.map((day) => (
                  <tr key={day.date} className="border-b border-border last:border-0 hover:bg-muted/20">
                    <td className="p-3 text-sm text-foreground">{formatDate(day.date)}</td>
                    <td className="p-3 text-sm text-muted-foreground text-right">{day.orderCount}</td>
                    <td className="p-3 text-sm text-foreground text-right">{formatCurrency(day.revenue)}</td>
                    <td className="p-3 text-sm text-muted-foreground text-right">{formatCurrency(day.cost)}</td>
                    <td className="p-3 text-sm font-medium text-right">
                      <span className={day.profit >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400"}>
                        {day.profit >= 0 ? "+" : ""}{formatCurrency(day.profit)}
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
        </div>
      )}

      {activeTab === "top" && (
        <div className="bg-card rounded-xl border border-border">
          <div className="p-4 border-b border-border">
            <h3 className="font-semibold text-foreground">En Karli Urunler</h3>
            <p className="text-xs text-muted-foreground mt-1">
              Secilen donemde en yuksek kar getiren urunler
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  <th className="text-center p-3 text-xs font-medium text-muted-foreground w-12">#</th>
                  <th className="text-left p-3 text-xs font-medium text-muted-foreground">Urun</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Adet</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Satis</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Maliyet</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Kar</th>
                  <th className="text-center p-3 text-xs font-medium text-muted-foreground">Marj</th>
                </tr>
              </thead>
              <tbody>
                {profitability?.topProfitable && profitability.topProfitable.length > 0 ? (
                  profitability.topProfitable.map((product, index) => (
                    <ProductRow key={product.barcode} product={product} rank={index + 1} />
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
        </div>
      )}

      {activeTab === "least" && (
        <div className="bg-card rounded-xl border border-border">
          <div className="p-4 border-b border-border">
            <h3 className="font-semibold text-foreground">En Az Karli Urunler</h3>
            <p className="text-xs text-muted-foreground mt-1">
              Dikkat gerektiren dusuk marjli veya zararli urunler
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  <th className="text-center p-3 text-xs font-medium text-muted-foreground w-12">#</th>
                  <th className="text-left p-3 text-xs font-medium text-muted-foreground">Urun</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Adet</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Satis</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Maliyet</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Kar</th>
                  <th className="text-center p-3 text-xs font-medium text-muted-foreground">Marj</th>
                </tr>
              </thead>
              <tbody>
                {profitability?.leastProfitable && profitability.leastProfitable.length > 0 ? (
                  profitability.leastProfitable.map((product, index) => (
                    <ProductRow key={product.barcode} product={product} rank={index + 1} />
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
        </div>
      )}

      {/* Info Box */}
      <div className="bg-amber-50 dark:bg-amber-900/20 rounded-xl border border-amber-200 dark:border-amber-800 p-4">
        <div className="flex items-start gap-3">
          <BarChart3 className="h-5 w-5 text-amber-600 dark:text-amber-400 mt-0.5" />
          <div>
            <p className="font-medium text-amber-900 dark:text-amber-100">
              FIFO Maliyet Hesabı Hakkinda
            </p>
            <p className="text-sm text-amber-700 dark:text-amber-300 mt-1">
              Bu rapor, satis yapilan urunlerin maliyetini FIFO (Ilk Giren Ilk Cikar)
              yontemiyle hesaplar. Yani en eski alinan stok, en once satilmis olarak
              kabul edilir. Bu sayede gercekci kar/zarar analizi yapabilirsiniz.
            </p>
            <p className="text-xs text-amber-600 dark:text-amber-400 mt-2">
              Not: Komisyon, stopaj ve iade maliyetleri bu rapora dahil degildir.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
