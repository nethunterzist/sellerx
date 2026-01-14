"use client";

import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useDashboardStats } from "@/hooks/useDashboardStats";
import { useSyncFinancial } from "@/hooks/queries/use-financial";
import { PeriodCards, ProductsTable } from "@/components/dashboard";
import { Button } from "@/components/ui/button";
import { RefreshCw, TrendingUp } from "lucide-react";
import { cn } from "@/lib/utils";
import type { ProductDetail } from "@/types/dashboard";

export default function DashboardPage() {
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;
  const { data: stats, isLoading: statsLoading, error } = useDashboardStats(storeId || undefined);
  const financialSyncMutation = useSyncFinancial();

  const handleFinancialSync = () => {
    if (storeId) {
      financialSyncMutation.mutate(storeId);
    }
  };

  // Transform dashboard products to table format
  const transformProducts = (products: ProductDetail[] | undefined) => {
    if (!products) return undefined;

    return products.slice(0, 10).map((p, index) => ({
      id: p.barcode || String(index),
      name: p.productName,
      sku: p.barcode,
      image: p.image, // Ürün görseli URL
      cogs: 0, // Backend'de cost verisi yok, ileride eklenebilir
      stock: p.stock || 0, // Trendyol stok adedi
      marketplace: "trendyol" as const,
      unitsSold: p.totalSoldQuantity,
      refunds: p.returnQuantity,
      sales: p.revenue,
      ads: 0, // Reklam verisi yok
      sellableReturns: "-",
      grossProfit: p.grossProfit,
      netProfit: p.grossProfit - p.estimatedCommission,
      margin: p.revenue > 0 ? Math.round((p.grossProfit / p.revenue) * 100) : 0,
      roi: 0, // ROI hesaplanamıyor
    }));
  };

  // Use today's products from dashboard stats
  const tableProducts = transformProducts(stats?.today?.products);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Kontrol Paneli</h1>
          {storeId && (
            <p className="text-sm text-gray-500 mt-1">
              {stats ? "Mağazanızdan canlı veriler" : "Mağaza verileri yükleniyor..."}
            </p>
          )}
          {error && (
            <p className="text-sm text-red-500 mt-1">
              İstatistikler yüklenirken hata: {error.message}
            </p>
          )}
        </div>

        <Button
          onClick={handleFinancialSync}
          disabled={financialSyncMutation.isPending || !storeId}
          variant="outline"
          className="gap-2"
        >
          <RefreshCw
            className={cn("h-4 w-4", financialSyncMutation.isPending && "animate-spin")}
          />
          {financialSyncMutation.isPending
            ? "Güncelleniyor..."
            : "Finansal Verileri Güncelle"}
        </Button>
      </div>

      {/* Financial Sync Result */}
      {financialSyncMutation.isSuccess && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <p className="text-green-800 text-sm flex items-center gap-2">
            <TrendingUp className="h-4 w-4" />
            Finansal veriler güncellendi!
            {financialSyncMutation.data.recordsProcessed > 0 && (
              <span>
                {" "}({financialSyncMutation.data.recordsProcessed} kayıt işlendi,{" "}
                {financialSyncMutation.data.recordsUpdated} kayıt güncellendi)
              </span>
            )}
          </p>
        </div>
      )}

      {financialSyncMutation.isError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 text-sm">
            Finansal güncelleme başarısız: {financialSyncMutation.error.message}
          </p>
        </div>
      )}

      {/* Period Cards */}
      <section>
        <PeriodCards stats={stats} isLoading={statsLoading} />
      </section>

      {/* Products Table */}
      <section>
        <ProductsTable
          products={tableProducts}
          isLoading={statsLoading || storeLoading}
        />
      </section>
    </div>
  );
}
