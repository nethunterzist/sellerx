"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  usePurchaseOrderStats,
  useCreatePurchaseOrder,
  useStockValuation,
} from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { Button } from "@/components/ui/button";
import { POStatusCards } from "@/components/purchasing/po-status-cards";
import {
  MonthlySummaryCard,
  RecentOrdersList,
} from "@/components/purchasing/dashboard";
import type { PurchasingDatePreset } from "@/components/purchasing/purchasing-date-filter";
import { Plus, Loader2, Package, Warehouse } from "lucide-react";
import type { PurchaseOrderStatus } from "@/types/purchasing";
import type { DateRange } from "react-day-picker";
import { format } from "date-fns";
import { FadeIn } from "@/components/motion";
import {
  StatCardSkeleton,
  FilterBarSkeleton,
  TableSkeleton,
  PaginationSkeleton,
} from "@/components/ui/skeleton-blocks";

function PurchasingPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <StatCardSkeleton key={i} />
        ))}
      </div>
      <FilterBarSkeleton showSearch={true} buttonCount={2} />
      <TableSkeleton columns={7} rows={8} />
      <PaginationSkeleton />
    </div>
  );
}

export default function PurchasingPage() {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
  const [activeStatus, setActiveStatus] = useState<PurchaseOrderStatus | null>(null);
  const [dateRange, setDateRange] = useState<DateRange | undefined>(undefined);
  const [, setDatePreset] = useState<PurchasingDatePreset | undefined>(undefined);

  // Format dates for API
  const startDate = dateRange?.from ? format(dateRange.from, "yyyy-MM-dd") : undefined;
  const endDate = dateRange?.to ? format(dateRange.to, "yyyy-MM-dd") : undefined;

  const handleDateRangeChange = (range: DateRange | undefined, preset: PurchasingDatePreset | undefined) => {
    setDateRange(range);
    setDatePreset(preset);
  };

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: stats, isLoading: statsLoading } = usePurchaseOrderStats(storeId || undefined);
  const { data: stockValuation } = useStockValuation(storeId || undefined);
  const createMutation = useCreatePurchaseOrder();

  const handleCreatePO = async () => {
    if (!storeId) return;

    try {
      const newPO = await createMutation.mutateAsync({
        storeId,
        data: {
          poDate: new Date().toISOString().split("T")[0],
        },
      });
      router.push(`/purchasing/${newPO.id}`);
    } catch (error) {
      console.error("Failed to create PO:", error);
    }
  };

  if (!storeId && !storeLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <Package className="h-16 w-16 text-muted-foreground/30 mb-4" />
        <h2 className="text-lg font-medium text-foreground mb-2">Magaza Secilmedi</h2>
        <p className="text-sm text-muted-foreground max-w-md">
          Satin alma islemlerini goruntulemek icin lutfen bir magaza secin.
        </p>
      </div>
    );
  }

  if (statsLoading) return <PurchasingPageSkeleton />;

  return (
    <FadeIn>
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Mal alimlarinizi takip edin ve karlilik analizlerinizi goruntuley
        </p>
        <div className="flex items-center gap-3">
          <Button
            onClick={handleCreatePO}
            disabled={createMutation.isPending}
            size="lg"
            className="gap-2"
          >
            {createMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Plus className="h-4 w-4" />
            )}
            Yeni Siparis
          </Button>
        </div>
      </div>

      {/* Status Cards */}
      <POStatusCards
        stats={stats}
        isLoading={statsLoading}
        activeStatus={activeStatus}
        onStatusClick={setActiveStatus}
      />

      {/* Orders Table - directly below status cards */}
      <RecentOrdersList
        storeId={storeId || undefined}
        activeStatus={activeStatus}
        startDate={startDate}
        endDate={endDate}
        dateRange={dateRange}
        onDateRangeChange={handleDateRangeChange}
      />

      {/* Summary Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Summary Card - shows date range if selected, otherwise current month */}
        <MonthlySummaryCard storeId={storeId || undefined} startDate={startDate} endDate={endDate} />

        {/* Stock Valuation Summary */}
        <div className="bg-card rounded-xl border border-border p-6">
          <div className="flex items-center justify-between mb-6">
            <h3 className="font-semibold text-foreground">Stok Degeri</h3>
            <button
              onClick={() => router.push("/purchasing/reports/stock-valuation")}
              className="text-xs text-primary hover:underline"
            >
              Detayli Rapor
            </button>
          </div>

          <div className="flex items-center gap-4">
            <div className="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-900/20">
              <Warehouse className="h-8 w-8 text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-3xl font-bold text-foreground">
                {formatCurrency(stockValuation?.totalValue || 0)}
              </p>
              <p className="text-sm text-muted-foreground">
                {(stockValuation?.totalQuantity || 0).toLocaleString()} adet urun
              </p>
            </div>
          </div>

          {stockValuation?.aging && (
            <div className="mt-4 pt-4 border-t border-border">
              <p className="text-xs text-muted-foreground mb-2">Stok Yasi Dagilimi</p>
              <div className="flex gap-2">
                <div className="flex-1 text-center p-2 rounded-lg bg-green-50 dark:bg-green-900/20">
                  <p className="text-xs font-medium text-green-600 dark:text-green-400">0-30 gun</p>
                  <p className="text-sm font-semibold text-foreground">
                    {formatCurrency(stockValuation.aging.days0to30)}
                  </p>
                </div>
                <div className="flex-1 text-center p-2 rounded-lg bg-yellow-50 dark:bg-yellow-900/20">
                  <p className="text-xs font-medium text-yellow-600 dark:text-yellow-400">30-60 gun</p>
                  <p className="text-sm font-semibold text-foreground">
                    {formatCurrency(stockValuation.aging.days30to60)}
                  </p>
                </div>
                <div className="flex-1 text-center p-2 rounded-lg bg-orange-50 dark:bg-orange-900/20">
                  <p className="text-xs font-medium text-orange-600 dark:text-orange-400">60-90 gun</p>
                  <p className="text-sm font-semibold text-foreground">
                    {formatCurrency(stockValuation.aging.days60to90)}
                  </p>
                </div>
                <div className="flex-1 text-center p-2 rounded-lg bg-red-50 dark:bg-red-900/20">
                  <p className="text-xs font-medium text-red-600 dark:text-red-400">90+ gun</p>
                  <p className="text-sm font-semibold text-foreground">
                    {formatCurrency(stockValuation.aging.days90plus)}
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

    </div>
    </FadeIn>
  );
}
