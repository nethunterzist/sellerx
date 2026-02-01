"use client";

import { CalendarDays, Package, TrendingDown, TrendingUp } from "lucide-react";
import { cn } from "@/lib/utils";
import type { CostAndStockInfo } from "@/types/product";
import { Progress } from "@/components/ui/progress";
import { useCurrency } from "@/lib/contexts/currency-context";

interface CostHistoryTimelineProps {
  costHistory: CostAndStockInfo[];
  salePrice: number;
  vatRate: number;
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("tr-TR", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

// Calculate weighted average cost
function calculateWeightedAverageCost(entries: CostAndStockInfo[]): number {
  const remainingEntries = entries.filter(e => (e.quantity - (e.usedQuantity || 0)) > 0);
  if (remainingEntries.length === 0) return 0;

  let totalValue = 0;
  let totalQuantity = 0;

  remainingEntries.forEach(entry => {
    const remaining = entry.quantity - (entry.usedQuantity || 0);
    totalValue += remaining * entry.unitCost;
    totalQuantity += remaining;
  });

  return totalQuantity > 0 ? totalValue / totalQuantity : 0;
}

export function CostHistoryTimeline({ costHistory, salePrice, vatRate }: CostHistoryTimelineProps) {
  const { formatCurrency } = useCurrency();

  if (!costHistory || costHistory.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        <Package className="h-8 w-8 mx-auto mb-2 opacity-50" />
        <p className="text-sm">Henüz maliyet kaydı yok</p>
        <p className="text-xs mt-1">Yeni maliyet ekleyerek başlayın</p>
      </div>
    );
  }

  // Sort by date descending
  const sortedHistory = [...costHistory].sort(
    (a, b) => new Date(b.stockDate).getTime() - new Date(a.stockDate).getTime()
  );

  // Calculate totals
  const totalQuantity = costHistory.reduce((sum, item) => sum + item.quantity, 0);
  const totalUsed = costHistory.reduce((sum, item) => sum + (item.usedQuantity || 0), 0);
  const remainingStock = totalQuantity - totalUsed;
  const usagePercentage = totalQuantity > 0 ? (totalUsed / totalQuantity) * 100 : 0;

  // Calculate weighted average cost
  const weightedAvgCost = calculateWeightedAverageCost(costHistory);

  // Calculate estimated profit
  const salePriceExVat = salePrice / (1 + vatRate / 100);
  const costWithVat = weightedAvgCost * (1 + (sortedHistory[0]?.costVatRate || 20) / 100);
  const estimatedProfit = salePriceExVat - weightedAvgCost;
  const profitMargin = salePriceExVat > 0 ? (estimatedProfit / salePriceExVat) * 100 : 0;

  return (
    <div className="space-y-4">
      {/* Summary Cards */}
      <div className="grid grid-cols-3 gap-3">
        <div className="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-3 text-center">
          <p className="text-xs text-blue-600 dark:text-blue-400 font-medium">Ort. Maliyet</p>
          <p className="text-lg font-bold text-blue-700 dark:text-blue-300">{formatCurrency(weightedAvgCost)}</p>
        </div>
        <div className="bg-green-50 dark:bg-green-900/20 rounded-lg p-3 text-center">
          <p className="text-xs text-green-600 dark:text-green-400 font-medium">Kalan Stok</p>
          <p className="text-lg font-bold text-green-700 dark:text-green-300">{remainingStock} adet</p>
        </div>
        <div className={cn(
          "rounded-lg p-3 text-center",
          profitMargin >= 20 ? "bg-emerald-50 dark:bg-emerald-900/20" : profitMargin >= 10 ? "bg-yellow-50 dark:bg-yellow-900/20" : "bg-red-50 dark:bg-red-900/20"
        )}>
          <p className={cn(
            "text-xs font-medium",
            profitMargin >= 20 ? "text-emerald-600 dark:text-emerald-400" : profitMargin >= 10 ? "text-yellow-600 dark:text-yellow-400" : "text-red-600 dark:text-red-400"
          )}>Tah. Kâr Marjı</p>
          <p className={cn(
            "text-lg font-bold flex items-center justify-center gap-1",
            profitMargin >= 20 ? "text-emerald-700 dark:text-emerald-300" : profitMargin >= 10 ? "text-yellow-700 dark:text-yellow-300" : "text-red-700 dark:text-red-300"
          )}>
            {profitMargin >= 0 ? <TrendingUp className="h-4 w-4" /> : <TrendingDown className="h-4 w-4" />}
            %{profitMargin.toFixed(1)}
          </p>
        </div>
      </div>

      {/* Stock Usage Progress */}
      <div className="space-y-2">
        <div className="flex justify-between text-xs text-muted-foreground">
          <span>Stok Kullanımı</span>
          <span>{totalUsed} / {totalQuantity} adet (%{usagePercentage.toFixed(0)})</span>
        </div>
        <Progress value={usagePercentage} className="h-2" />
      </div>

      {/* Timeline */}
      <div className="relative">
        <div className="absolute left-[9px] top-0 bottom-0 w-0.5 bg-gray-200 dark:bg-gray-700" />

        <div className="space-y-4">
          {sortedHistory.map((entry, index) => {
            const remaining = entry.quantity - (entry.usedQuantity || 0);
            const isFullyUsed = remaining <= 0;
            const entryUsagePercentage = entry.quantity > 0
              ? ((entry.usedQuantity || 0) / entry.quantity) * 100
              : 0;

            return (
              <div key={index} className="relative flex gap-3">
                {/* Timeline dot */}
                <div className={cn(
                  "relative z-10 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2",
                  isFullyUsed
                    ? "bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600"
                    : index === 0
                      ? "bg-blue-500 border-blue-500"
                      : "bg-card border-blue-300 dark:border-blue-600"
                )}>
                  {!isFullyUsed && index === 0 && (
                    <div className="h-2 w-2 rounded-full bg-white" />
                  )}
                </div>

                {/* Content */}
                <div className={cn(
                  "flex-1 rounded-lg border p-3 transition-colors",
                  isFullyUsed
                    ? "bg-gray-50 dark:bg-gray-800/50 border-gray-200 dark:border-gray-700 opacity-60"
                    : index === 0
                      ? "bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800"
                      : "bg-card border-border hover:border-gray-300 dark:hover:border-gray-600"
                )}>
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <CalendarDays className="h-3.5 w-3.5 text-muted-foreground" />
                      <span className="text-sm font-medium text-foreground">{formatDate(entry.stockDate)}</span>
                      {index === 0 && !isFullyUsed && (
                        <span className="px-1.5 py-0.5 text-[10px] bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded font-medium">
                          En Güncel
                        </span>
                      )}
                      {isFullyUsed && (
                        <span className="px-1.5 py-0.5 text-[10px] bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 rounded font-medium">
                          Tükendi
                        </span>
                      )}
                    </div>
                    <span className="text-sm font-semibold text-foreground">
                      {formatCurrency(entry.unitCost)}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <div className="flex items-center gap-4">
                      <span>Alım: {entry.quantity} adet</span>
                      <span>Kalan: {remaining} adet</span>
                      <span>KDV: %{entry.costVatRate}</span>
                    </div>
                    <span className="text-muted-foreground/70">
                      Toplam: {formatCurrency(entry.quantity * entry.unitCost)}
                    </span>
                  </div>

                  {/* Entry progress bar */}
                  {entry.quantity > 0 && (
                    <div className="mt-2">
                      <Progress
                        value={entryUsagePercentage}
                        className={cn("h-1", isFullyUsed && "opacity-50")}
                      />
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
