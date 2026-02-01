"use client";

import { useState } from "react";
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
  useAdminOrderStats,
  useAdminRecentOrders,
} from "@/hooks/queries/use-admin-orders";
import {
  ShoppingCart,
  CalendarDays,
  CalendarRange,
  Calendar,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  Package,
} from "lucide-react";
import { format } from "date-fns";
import { tr } from "date-fns/locale";

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "TRY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);

const formatNumber = (value: number) =>
  new Intl.NumberFormat("tr-TR").format(value);

function formatDate(date: string | null) {
  if (!date) return "-";
  try {
    return format(new Date(date), "dd MMM yyyy, HH:mm", { locale: tr });
  } catch {
    return "-";
  }
}

const ORDER_STATUS_MAP: Record<string, { label: string; color: string; bg: string }> = {
  Created: {
    label: "Olusturuldu",
    color: "text-blue-700 dark:text-blue-400",
    bg: "bg-blue-100 dark:bg-blue-900/30",
  },
  Picking: {
    label: "Hazirlaniyor",
    color: "text-amber-700 dark:text-amber-400",
    bg: "bg-amber-100 dark:bg-amber-900/30",
  },
  Shipped: {
    label: "Kargoda",
    color: "text-purple-700 dark:text-purple-400",
    bg: "bg-purple-100 dark:bg-purple-900/30",
  },
  Delivered: {
    label: "Teslim Edildi",
    color: "text-emerald-700 dark:text-emerald-400",
    bg: "bg-emerald-100 dark:bg-emerald-900/30",
  },
  Cancelled: {
    label: "Iptal",
    color: "text-red-700 dark:text-red-400",
    bg: "bg-red-100 dark:bg-red-900/30",
  },
  UnDelivered: {
    label: "Teslim Edilemedi",
    color: "text-orange-700 dark:text-orange-400",
    bg: "bg-orange-100 dark:bg-orange-900/30",
  },
  Returned: {
    label: "Iade",
    color: "text-slate-700 dark:text-slate-400",
    bg: "bg-slate-100 dark:bg-slate-900/30",
  },
};

function getOrderStatusBadge(status: string) {
  const mapped = ORDER_STATUS_MAP[status];
  if (mapped) {
    return (
      <Badge className={`${mapped.bg} ${mapped.color} hover:${mapped.bg}`}>
        {mapped.label}
      </Badge>
    );
  }
  return <Badge variant="secondary">{status}</Badge>;
}

export default function AdminOrdersPage() {
  const [page, setPage] = useState(0);

  const {
    data: orderStats,
    isLoading: isLoadingStats,
    refetch,
    isFetching,
  } = useAdminOrderStats();
  const { data: recentOrders, isLoading: isLoadingOrders } =
    useAdminRecentOrders(page);

  const totalPages = recentOrders?.totalPages || 1;
  const orders = recentOrders?.content || [];

  const statCards = [
    {
      title: "Toplam Siparis",
      value: orderStats?.totalOrders,
      icon: ShoppingCart,
      color: "text-blue-500",
      bg: "bg-blue-500/10",
    },
    {
      title: "Bugun",
      value: orderStats?.todayOrders,
      icon: CalendarDays,
      color: "text-emerald-500",
      bg: "bg-emerald-500/10",
    },
    {
      title: "Bu Hafta",
      value: orderStats?.thisWeekOrders,
      icon: CalendarRange,
      color: "text-purple-500",
      bg: "bg-purple-500/10",
    },
    {
      title: "Bu Ay",
      value: orderStats?.thisMonthOrders,
      icon: Calendar,
      color: "text-amber-500",
      bg: "bg-amber-500/10",
    },
  ];

  // Sort status breakdown for consistent display
  const statusBreakdown = orderStats?.statusBreakdown
    ? Object.entries(orderStats.statusBreakdown).sort(
        ([, a], [, b]) => b - a
      )
    : [];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Siparisler
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Sistem genelinde siparis istatistikleri ve son siparisler
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
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
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

      {/* Status Breakdown */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Durum Dagilimi</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoadingStats ? (
            <div className="flex gap-3">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-8 w-28" />
              ))}
            </div>
          ) : statusBreakdown.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Durum verisi bulunamadi
            </p>
          ) : (
            <div className="flex flex-wrap gap-3">
              {statusBreakdown.map(([status, count]) => {
                const mapped = ORDER_STATUS_MAP[status];
                return (
                  <div
                    key={status}
                    className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium ${
                      mapped
                        ? `${mapped.bg} ${mapped.color}`
                        : "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300"
                    }`}
                  >
                    <span>{mapped?.label || status}</span>
                    <span className="font-bold">{formatNumber(count)}</span>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Recent Orders Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Package className="h-5 w-5" />
            Son Siparisler
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoadingOrders ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : orders.length === 0 ? (
            <div className="text-center py-12 text-slate-500 dark:text-slate-400">
              <ShoppingCart className="h-12 w-12 mx-auto mb-3 opacity-30" />
              <p>Siparis bulunamadi</p>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Siparis No</TableHead>
                    <TableHead>Magaza</TableHead>
                    <TableHead>Durum</TableHead>
                    <TableHead className="text-right">Toplam Tutar</TableHead>
                    <TableHead>Tarih</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {orders.map((order) => (
                    <TableRow key={order.id}>
                      <TableCell className="font-mono text-sm font-medium text-slate-900 dark:text-white">
                        {order.orderNumber}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <span className="text-sm text-slate-700 dark:text-slate-300">
                            {order.storeName}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>
                        {getOrderStatusBadge(order.status)}
                      </TableCell>
                      <TableCell className="text-right font-semibold text-slate-900 dark:text-white">
                        {formatCurrency(order.totalPrice)}
                      </TableCell>
                      <TableCell className="text-sm text-slate-500 dark:text-slate-400">
                        {formatDate(order.orderDate)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              <div className="flex items-center justify-between mt-4">
                <p className="text-sm text-slate-500">
                  Sayfa {page + 1} / {totalPages} (
                  {recentOrders?.totalElements || 0} kayit)
                </p>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setPage(Math.min(totalPages - 1, page + 1))
                    }
                    disabled={page >= totalPages - 1}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
