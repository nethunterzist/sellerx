"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useAdminProductStats,
  useAdminTopProducts,
} from "@/hooks/queries/use-admin-orders";
import {
  Package,
  DollarSign,
  AlertCircle,
  RefreshCw,
  Store,
  Trophy,
} from "lucide-react";

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "TRY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);

const formatNumber = (value: number) =>
  new Intl.NumberFormat("tr-TR").format(value);

export default function AdminProductsPage() {
  const {
    data: productStats,
    isLoading: isLoadingStats,
    refetch,
    isFetching,
  } = useAdminProductStats();
  const { data: topProducts, isLoading: isLoadingTop } =
    useAdminTopProducts();

  const statCards = [
    {
      title: "Toplam Urun",
      value: productStats?.totalProducts,
      icon: Package,
      color: "text-blue-500",
      bg: "bg-blue-500/10",
    },
    {
      title: "Maliyeti Girilmis",
      value: productStats?.withCost,
      icon: DollarSign,
      color: "text-emerald-500",
      bg: "bg-emerald-500/10",
    },
    {
      title: "Maliyeti Girilmemis",
      value: productStats?.withoutCost,
      icon: AlertCircle,
      color: "text-amber-500",
      bg: "bg-amber-500/10",
    },
  ];

  // Products by store breakdown
  const productsByStore = productStats?.productsByStore
    ? Object.entries(productStats.productsByStore).sort(
        ([, a], [, b]) => b - a
      )
    : [];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Urunler
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Sistem genelinde urun istatistikleri ve en cok satan urunler
          </p>
        </div>
        <Button
          onClick={() => refetch()}
          variant="outline"
          size="sm"
          disabled={isFetching}
        >
          <RefreshCw
            className={`h-4 w-4 mr-2 ${isFetching ? "animate-spin" : ""}`}
          />
          Yenile
        </Button>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {statCards.map((card) => (
          <Card key={card.title}>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className={`p-3 rounded-lg ${card.bg}`}>
                  <card.icon className={`h-5 w-5 ${card.color}`} />
                </div>
                <div>
                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    {card.title}
                  </p>
                  {isLoadingStats ? (
                    <Skeleton className="h-7 w-16 mt-1" />
                  ) : (
                    <p className="text-2xl font-bold text-slate-900 dark:text-white">
                      {card.value !== undefined && card.value !== null
                        ? formatNumber(card.value)
                        : "0"}
                    </p>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Products by Store */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Store className="h-5 w-5" />
            Magazalara Gore Urunler
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoadingStats ? (
            <div className="flex gap-3">
              {[...Array(3)].map((_, i) => (
                <Skeleton key={i} className="h-10 w-40" />
              ))}
            </div>
          ) : productsByStore.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Magaza verisi bulunamadi
            </p>
          ) : (
            <div className="flex flex-wrap gap-3">
              {productsByStore.map(([storeName, count]) => (
                <div
                  key={storeName}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                >
                  <Store className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
                    {storeName}
                  </span>
                  <Badge variant="secondary" className="ml-1">
                    {formatNumber(count)}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Top Products Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Trophy className="h-5 w-5" />
            En Cok Satan Urunler
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoadingTop ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : !topProducts || topProducts.length === 0 ? (
            <div className="text-center py-12 text-slate-500 dark:text-slate-400">
              <Package className="h-12 w-12 mx-auto mb-3 opacity-30" />
              <p>Urun verisi bulunamadi</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12">#</TableHead>
                  <TableHead>Barkod</TableHead>
                  <TableHead>Urun Adi</TableHead>
                  <TableHead>Magaza</TableHead>
                  <TableHead className="text-right">Siparis Sayisi</TableHead>
                  <TableHead className="text-right">Ciro</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {topProducts.map((product, index) => (
                  <TableRow key={product.id}>
                    <TableCell>
                      <span
                        className={`inline-flex items-center justify-center h-6 w-6 rounded-full text-xs font-bold ${
                          index === 0
                            ? "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400"
                            : index === 1
                            ? "bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-300"
                            : index === 2
                            ? "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400"
                            : "bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400"
                        }`}
                      >
                        {index + 1}
                      </span>
                    </TableCell>
                    <TableCell className="font-mono text-sm text-slate-600 dark:text-slate-400">
                      {product.barcode}
                    </TableCell>
                    <TableCell>
                      <p className="font-medium text-slate-900 dark:text-white max-w-[300px] truncate">
                        {product.title}
                      </p>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{product.storeName}</Badge>
                    </TableCell>
                    <TableCell className="text-right font-semibold text-slate-900 dark:text-white">
                      {formatNumber(product.orderCount)}
                    </TableCell>
                    <TableCell className="text-right font-semibold text-emerald-600 dark:text-emerald-400">
                      {formatCurrency(product.revenue)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
