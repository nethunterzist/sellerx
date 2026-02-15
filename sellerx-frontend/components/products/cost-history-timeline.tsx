"use client";

import { CalendarDays, Package, Sparkles, ShoppingCart, TrendingDown, TrendingUp, Pencil, Trash2, AlertTriangle, Info } from "lucide-react";
import { cn } from "@/lib/utils";
import type { CostAndStockInfo } from "@/types/product";
import { Progress } from "@/components/ui/progress";
import { Button } from "@/components/ui/button";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

interface CostHistoryTimelineProps {
  costHistory: CostAndStockInfo[];
  salePrice: number;
  vatRate: number;
  commissionRate: number;
  lastShippingCostPerUnit?: number | null;
  onEdit?: (entry: CostAndStockInfo) => void;
  onDelete?: (entry: CostAndStockInfo) => void;
  editingDate?: string;
  deletingDate?: string;
  onConfirmDelete?: () => void;
  onCancelDelete?: () => void;
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

export function CostHistoryTimeline({
  costHistory,
  salePrice,
  vatRate,
  commissionRate,
  lastShippingCostPerUnit,
  onEdit,
  onDelete,
  editingDate,
  deletingDate,
  onConfirmDelete,
  onCancelDelete,
}: CostHistoryTimelineProps) {
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

  // Calculate estimated profit with commission and shipping
  const salePriceExVat = salePrice / (1 + vatRate / 100);
  const commissionAmount = salePriceExVat * (commissionRate / 100);
  const shippingCost = lastShippingCostPerUnit ?? 0;
  const estimatedProfit = salePriceExVat - weightedAvgCost - commissionAmount - shippingCost;
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
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <div className={cn(
                "rounded-lg p-3 text-center cursor-help relative",
                profitMargin >= 20 ? "bg-emerald-50 dark:bg-emerald-900/20" : profitMargin >= 10 ? "bg-yellow-50 dark:bg-yellow-900/20" : "bg-red-50 dark:bg-red-900/20"
              )}>
                <div className="flex items-center justify-center gap-1">
                  <p className={cn(
                    "text-xs font-medium",
                    profitMargin >= 20 ? "text-emerald-600 dark:text-emerald-400" : profitMargin >= 10 ? "text-yellow-600 dark:text-yellow-400" : "text-red-600 dark:text-red-400"
                  )}>Tah. Kâr Marjı</p>
                  <Info className={cn(
                    "h-3 w-3",
                    profitMargin >= 20 ? "text-emerald-500 dark:text-emerald-400" : profitMargin >= 10 ? "text-yellow-500 dark:text-yellow-400" : "text-red-500 dark:text-red-400"
                  )} />
                </div>
                <p className={cn(
                  "text-lg font-bold flex items-center justify-center gap-1",
                  profitMargin >= 20 ? "text-emerald-700 dark:text-emerald-300" : profitMargin >= 10 ? "text-yellow-700 dark:text-yellow-300" : "text-red-700 dark:text-red-300"
                )}>
                  {profitMargin >= 0 ? <TrendingUp className="h-4 w-4" /> : <TrendingDown className="h-4 w-4" />}
                  %{profitMargin.toFixed(1)}
                </p>
              </div>
            </TooltipTrigger>
            <TooltipContent side="bottom" className="max-w-xs p-3">
              <div className="space-y-2 text-xs">
                <p className="font-semibold text-sm border-b pb-1">Kâr Marjı Hesaplama</p>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span>Satış (KDV Hariç):</span>
                    <span className="font-medium">{formatCurrency(salePriceExVat)}</span>
                  </div>
                  <div className="flex justify-between text-rose-200">
                    <span>- Maliyet:</span>
                    <span className="font-medium">{formatCurrency(weightedAvgCost)}</span>
                  </div>
                  <div className="flex justify-between text-rose-200">
                    <span>- Komisyon (%{commissionRate}):</span>
                    <span className="font-medium">{formatCurrency(commissionAmount)}</span>
                  </div>
                  <div className="flex justify-between text-rose-200">
                    <span>- Kargo:</span>
                    <span className="font-medium">
                      {shippingCost > 0 ? formatCurrency(shippingCost) : "Veri yok"}
                    </span>
                  </div>
                  <div className="flex justify-between font-semibold border-t border-white/20 pt-1 mt-1">
                    <span>= Net Kâr:</span>
                    <span className={estimatedProfit >= 0 ? "text-emerald-200" : "text-rose-200"}>
                      {formatCurrency(estimatedProfit)}
                    </span>
                  </div>
                </div>
                {shippingCost === 0 && (
                  <p className="text-amber-200 text-[10px] mt-2">
                    ⚠️ Kargo verisi henüz yok. Kargo faturası geldikten sonra otomatik güncellenecek.
                  </p>
                )}
              </div>
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>
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
            const isEditing = editingDate === entry.stockDate;
            const isDeleting = deletingDate === entry.stockDate;

            return (
              <div key={index} className="relative flex gap-3">
                {/* Timeline dot */}
                <div className={cn(
                  "relative z-10 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2",
                  isDeleting
                    ? "bg-red-500 border-red-500 ring-2 ring-red-200 dark:ring-red-800"
                    : isEditing
                      ? "bg-blue-500 border-blue-500 ring-2 ring-blue-200 dark:ring-blue-800"
                      : isFullyUsed
                        ? "bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600"
                        : index === 0
                          ? "bg-blue-500 border-blue-500"
                          : "bg-card border-blue-300 dark:border-blue-600"
                )}>
                  {!isFullyUsed && index === 0 && !isEditing && !isDeleting && (
                    <div className="h-2 w-2 rounded-full bg-white" />
                  )}
                </div>

                {/* Content */}
                {isDeleting ? (
                  <div className="flex-1 rounded-lg border p-3 bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800">
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-2">
                        <AlertTriangle className="h-4 w-4 text-red-500 dark:text-red-400" />
                        <span className="text-sm text-red-700 dark:text-red-300">
                          Bu kaydı silmek istediğinize emin misiniz?
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          className="h-7 text-xs"
                          onClick={onCancelDelete}
                        >
                          İptal
                        </Button>
                        <Button
                          type="button"
                          variant="destructive"
                          size="sm"
                          className="h-7 text-xs"
                          onClick={onConfirmDelete}
                        >
                          Evet, Sil
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : (
                <div className={cn(
                  "flex-1 rounded-lg border p-3 transition-colors",
                  isEditing
                    ? "bg-blue-100 dark:bg-blue-900/40 border-blue-300 dark:border-blue-700 ring-2 ring-blue-200 dark:ring-blue-800"
                    : isFullyUsed
                      ? "bg-gray-50 dark:bg-gray-800/50 border-gray-200 dark:border-gray-700 opacity-60"
                      : index === 0
                        ? "bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800"
                        : "bg-card border-border hover:border-gray-300 dark:hover:border-gray-600"
                )}>
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center gap-2 flex-wrap">
                      <CalendarDays className="h-3.5 w-3.5 text-muted-foreground" />
                      <span className="text-sm font-medium text-foreground">{formatDate(entry.stockDate)}</span>
                      {isEditing && (
                        <span className="px-1.5 py-0.5 text-[10px] bg-blue-500 text-white rounded font-medium">
                          Düzenleniyor
                        </span>
                      )}
                      {index === 0 && !isFullyUsed && !isEditing && (
                        <span className="px-1.5 py-0.5 text-[10px] bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded font-medium">
                          En Güncel
                        </span>
                      )}
                      {isFullyUsed && (
                        <span className="px-1.5 py-0.5 text-[10px] bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 rounded font-medium">
                          Tükendi
                        </span>
                      )}
                      {entry.costSource === "AUTO_DETECTED" && (
                        <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 rounded font-medium">
                          <Sparkles className="h-2.5 w-2.5" />
                          Otomatik
                        </span>
                      )}
                      {entry.costSource === "PURCHASE_ORDER" && (
                        <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded font-medium">
                          <ShoppingCart className="h-2.5 w-2.5" />
                          Satin Alma
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-foreground">
                        {formatCurrency(entry.unitCost)}
                      </span>
                      {/* Edit/Delete buttons */}
                      {(onEdit || onDelete) && (
                        <div className="flex items-center gap-1 ml-2">
                          {onEdit && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              className="h-6 w-6 text-muted-foreground hover:text-blue-600 dark:hover:text-blue-400"
                              onClick={() => onEdit(entry)}
                              disabled={isEditing}
                              title="Düzenle"
                            >
                              <Pencil className="h-3.5 w-3.5" />
                            </Button>
                          )}
                          {onDelete && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              className="h-6 w-6 text-muted-foreground hover:text-red-600 dark:hover:text-red-400"
                              onClick={() => onDelete(entry)}
                              disabled={isEditing}
                              title="Sil"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          )}
                        </div>
                      )}
                    </div>
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
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
