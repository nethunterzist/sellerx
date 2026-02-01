"use client";

import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useFinancialStats, useSyncFinancial } from "@/hooks/queries/use-financial";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { RefreshCw, TrendingUp, TrendingDown, DollarSign, Percent, Package, RotateCcw, CheckCircle, Clock } from "lucide-react";
import { cn } from "@/lib/utils";
import { useCurrency } from "@/lib/contexts/currency-context";

function formatPercent(value: number): string {
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value);
}

// Summary Card Component
function SummaryCard({
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
  variant?: "default" | "success" | "danger" | "warning";
  isLoading?: boolean;
}) {
  const variantStyles = {
    default: "text-muted-foreground",
    success: "text-green-600 dark:text-green-400",
    danger: "text-red-600 dark:text-red-400",
    warning: "text-yellow-600 dark:text-yellow-400",
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-4 w-4" />
        </CardHeader>
        <CardContent>
          <Skeleton className="h-8 w-32 mb-2" />
          <Skeleton className="h-3 w-20" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        <Icon className={cn("h-4 w-4", variantStyles[variant])} />
      </CardHeader>
      <CardContent>
        <div className={cn("text-2xl font-bold", variantStyles[variant])}>
          {value}
        </div>
        {trend && trendValue && (
          <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
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

// Stats Grid Skeleton
function StatsGridSkeleton() {
  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      {[...Array(8)].map((_, i) => (
        <SummaryCard
          key={i}
          title=""
          value=""
          icon={DollarSign}
          isLoading={true}
        />
      ))}
    </div>
  );
}

export default function FinancialPage() {
  const { formatCurrency } = useCurrency();
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: stats, isLoading: statsLoading, error } = useFinancialStats(storeId || undefined);
  const syncMutation = useSyncFinancial();

  const handleSync = () => {
    if (storeId) {
      syncMutation.mutate(storeId);
    }
  };

  const isLoading = storeLoading || statsLoading;

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">
            Trendyol'dan gelen uzlaşma ve komisyon verileri
          </p>
        </div>

        <Button
          onClick={handleSync}
          disabled={syncMutation.isPending || !storeId}
          variant="outline"
          className="gap-2"
        >
          <RefreshCw
            className={cn("h-4 w-4", syncMutation.isPending && "animate-spin")}
          />
          {syncMutation.isPending ? "Senkronize ediliyor..." : "Finansal Verileri Senkronize Et"}
        </Button>
      </div>

      {/* Sync Result */}
      {syncMutation.isSuccess && (
        <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
          <p className="text-green-800 dark:text-green-200 text-sm flex items-center gap-2">
            <CheckCircle className="h-4 w-4" />
            Finansal veriler başarıyla senkronize edildi!
          </p>
        </div>
      )}

      {syncMutation.isError && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <p className="text-red-800 dark:text-red-200 text-sm">
            Senkronizasyon başarısız: {syncMutation.error.message}
          </p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <p className="text-red-800 dark:text-red-200 text-sm">
            Finansal veriler yüklenirken hata: {error.message}
          </p>
        </div>
      )}

      {/* Stats Grid */}
      {isLoading ? (
        <StatsGridSkeleton />
      ) : stats ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {/* Order Stats */}
          <SummaryCard
            title="Toplam Sipariş"
            value={stats.totalOrders.toString()}
            icon={Package}
            variant="default"
          />
          <SummaryCard
            title="Uzlaşmış Sipariş"
            value={stats.settledOrders.toString()}
            icon={CheckCircle}
            variant="success"
          />
          <SummaryCard
            title="Bekleyen Sipariş"
            value={stats.notSettledOrders.toString()}
            icon={Clock}
            variant="warning"
          />
          <SummaryCard
            title="Uzlaşma Oranı"
            value={`%${formatPercent(stats.settlementRate)}`}
            icon={Percent}
            variant={stats.settlementRate >= 80 ? "success" : stats.settlementRate >= 50 ? "warning" : "danger"}
          />

          {/* Transaction Stats */}
          <SummaryCard
            title="Toplam Satış Geliri"
            value={formatCurrency(stats.transactionStats?.totalSaleRevenue || 0)}
            icon={TrendingUp}
            variant="success"
          />
          <SummaryCard
            title="Toplam İade Tutarı"
            value={formatCurrency(stats.transactionStats?.totalReturnAmount || 0)}
            icon={RotateCcw}
            variant="danger"
          />
          <SummaryCard
            title="Net Gelir"
            value={formatCurrency(stats.transactionStats?.netRevenue || 0)}
            icon={DollarSign}
            variant={(stats.transactionStats?.netRevenue || 0) >= 0 ? "success" : "danger"}
          />
          <SummaryCard
            title="Satış/İade Oranı"
            value={stats.transactionStats?.totalSaleTransactions
              ? `${stats.transactionStats.totalSaleTransactions}/${stats.transactionStats.totalReturnTransactions}`
              : "0/0"
            }
            icon={TrendingDown}
            variant="default"
            trendValue={stats.transactionStats?.totalSaleTransactions
              ? `İade: %${formatPercent((stats.transactionStats.totalReturnTransactions / stats.transactionStats.totalSaleTransactions) * 100)}`
              : undefined
            }
          />
        </div>
      ) : (
        <div className="bg-muted border border-border rounded-lg p-8 text-center">
          <DollarSign className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
          <h3 className="text-lg font-medium text-foreground mb-2">Henüz finansal veri yok</h3>
          <p className="text-muted-foreground mb-4">
            Trendyol'dan finansal verileri senkronize etmek için yukarıdaki butonu kullanın.
          </p>
          <Button onClick={handleSync} disabled={syncMutation.isPending || !storeId}>
            <RefreshCw className={cn("h-4 w-4 mr-2", syncMutation.isPending && "animate-spin")} />
            Finansal Verileri Senkronize Et
          </Button>
        </div>
      )}

      {/* Info Section */}
      <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
        <h3 className="text-sm font-medium text-blue-800 dark:text-blue-200 mb-2">Finansal Veriler Hakkında</h3>
        <ul className="text-sm text-blue-700 dark:text-blue-300 space-y-1">
          <li>• <strong>Uzlaşmış Sipariş:</strong> Trendyol tarafından ödemesi yapılmış siparişler</li>
          <li>• <strong>Bekleyen Sipariş:</strong> Henüz uzlaşılmamış siparişler</li>
          <li>• <strong>Net Gelir:</strong> Toplam satış geliri - iade tutarları</li>
          <li>• Veriler, Trendyol Finance API'sinden son 3 aylık dönem için çekilir</li>
        </ul>
      </div>
    </div>
  );
}
