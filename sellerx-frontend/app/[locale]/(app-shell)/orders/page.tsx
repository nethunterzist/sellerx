"use client";

import React, { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  useOrdersByStore,
  useOrdersByStatus,
  useSyncOrders,
} from "@/hooks/queries/use-orders";
import { OrderStatsCards } from "@/components/orders/order-stats-cards";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  RefreshCw,
  Search,
  ChevronLeft,
  ChevronRight,
  Package,
  ChevronDown,
  ChevronUp,
  Filter,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { TrendyolOrder, OrderItem, OrderStatus } from "@/types/order";
import { orderStatusLabels } from "@/types/order";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("tr-TR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getStatusColor(status: string): string {
  switch (status?.toLowerCase()) {
    case "delivered":
      return "bg-green-100 text-green-800";
    case "shipped":
      return "bg-blue-100 text-blue-800";
    case "created":
    case "picking":
      return "bg-yellow-100 text-yellow-800";
    case "cancelled":
    case "returned":
      return "bg-red-100 text-red-800";
    default:
      return "bg-gray-100 text-gray-800";
  }
}

function OrderItemsRow({ items }: { items: OrderItem[] }) {
  return (
    <div className="bg-gray-50 p-4 rounded-lg mt-2 space-y-2">
      <p className="text-xs font-medium text-gray-500 uppercase">
        Sipariş Kalemleri
      </p>
      {items.map((item, idx) => (
        <div
          key={idx}
          className="flex items-center justify-between text-sm border-b border-gray-200 pb-2 last:border-0"
        >
          <div className="flex-1">
            <p className="font-medium text-gray-900">{item.productName}</p>
            <p className="text-xs text-gray-500">Barkod: {item.barcode}</p>
          </div>
          <div className="text-right">
            <p className="font-medium">
              {item.quantity} x {formatCurrency(item.price)} TL
            </p>
            <p className="text-xs text-gray-500">
              Maliyet: {formatCurrency(item.cost)} TL | Komisyon:{" "}
              {formatCurrency(item.unitEstimatedCommission)} TL
            </p>
          </div>
        </div>
      ))}
    </div>
  );
}

const ORDER_STATUSES: OrderStatus[] = [
  "Created",
  "Picking",
  "Invoiced",
  "Shipped",
  "Delivered",
  "Cancelled",
  "UnDelivered",
  "Returned",
];

export default function OrdersPage() {
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [expandedOrders, setExpandedOrders] = useState<Set<string>>(new Set());
  const [statusFilter, setStatusFilter] = useState<OrderStatus | undefined>(
    undefined
  );

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Use filtered query if status is selected, otherwise use all orders
  const allOrdersQuery = useOrdersByStore(
    statusFilter ? undefined : storeId,
    page,
    20
  );
  const filteredOrdersQuery = useOrdersByStatus(
    statusFilter ? storeId : undefined,
    statusFilter,
    page,
    20
  );

  const activeQuery = statusFilter ? filteredOrdersQuery : allOrdersQuery;
  const { data, isLoading, error } = activeQuery;

  const syncMutation = useSyncOrders();

  const handleSync = () => {
    if (storeId) {
      syncMutation.mutate(storeId);
    }
  };

  const toggleOrderExpand = (orderId: string) => {
    setExpandedOrders((prev) => {
      const next = new Set(prev);
      if (next.has(orderId)) {
        next.delete(orderId);
      } else {
        next.add(orderId);
      }
      return next;
    });
  };

  const handleStatusChange = (value: string) => {
    setPage(0);
    if (value === "all") {
      setStatusFilter(undefined);
    } else {
      setStatusFilter(value as OrderStatus);
    }
  };

  const clearFilters = () => {
    setStatusFilter(undefined);
    setSearchQuery("");
    setPage(0);
  };

  // Filter orders by search query (client-side)
  const filteredOrders =
    data?.content?.filter(
      (o) =>
        o.tyOrderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        o.orderItems.some((item) =>
          item.productName.toLowerCase().includes(searchQuery.toLowerCase())
        )
    ) || [];

  const hasActiveFilters = statusFilter || searchQuery;

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <h1 className="text-2xl font-bold mb-4">Siparişler</h1>
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Siparişler</h1>
          <p className="text-sm text-gray-500 mt-1">
            {data?.totalElements
              ? `${statusFilter ? orderStatusLabels[statusFilter] + " - " : ""}${data.totalElements} sipariş`
              : "Siparişler yükleniyor..."}
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
          {syncMutation.isPending
            ? "Senkronize ediliyor..."
            : "Trendyol'dan Senkronize Et"}
        </Button>
      </div>

      {/* Statistics Cards */}
      <OrderStatsCards storeId={storeId} />

      {/* Sync Result */}
      {syncMutation.isSuccess && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <p className="text-green-800 text-sm">
            Senkronizasyon tamamlandı! Çekilen: {syncMutation.data.totalFetched}
            , Kaydedilen: {syncMutation.data.totalSaved}, Güncellenen:{" "}
            {syncMutation.data.totalUpdated}
          </p>
        </div>
      )}

      {syncMutation.isError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 text-sm">
            Senkronizasyon başarısız: {syncMutation.error.message}
          </p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 text-sm">
            Siparişler yüklenirken hata: {error.message}
          </p>
        </div>
      )}

      {/* Filters */}
      <div className="bg-white rounded-lg border border-gray-200 p-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <Filter className="h-4 w-4" />
            <span>Filtreler</span>
          </div>

          <div className="flex flex-wrap items-center gap-3 flex-1">
            {/* Search Input */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <Input
                type="search"
                placeholder="Sipariş veya ürün ara..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="h-9 w-64 pl-9 text-sm"
              />
            </div>

            {/* Status Filter */}
            <Select
              value={statusFilter || "all"}
              onValueChange={handleStatusChange}
            >
              <SelectTrigger className="w-[180px] h-9">
                <SelectValue placeholder="Durum seçin" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tüm Durumlar</SelectItem>
                {ORDER_STATUSES.map((status) => (
                  <SelectItem key={status} value={status}>
                    {orderStatusLabels[status]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            {/* Clear Filters */}
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={clearFilters}
                className="h-9 px-3 text-gray-500 hover:text-gray-700"
              >
                <X className="h-4 w-4 mr-1" />
                Filtreleri Temizle
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="w-[50px]"></TableHead>
                <TableHead>Sipariş No</TableHead>
                <TableHead>Tarih</TableHead>
                <TableHead className="text-right">Toplam</TableHead>
                <TableHead className="text-right">Komisyon</TableHead>
                <TableHead className="text-right">Stopaj</TableHead>
                <TableHead className="text-center">Durum</TableHead>
                <TableHead className="text-center">Kalem</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={8} className="h-24 text-center">
                    <RefreshCw className="h-5 w-5 animate-spin mx-auto mb-2" />
                    Siparişler yükleniyor...
                  </TableCell>
                </TableRow>
              ) : filteredOrders.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={8}
                    className="h-24 text-center text-gray-500"
                  >
                    <Package className="h-8 w-8 mx-auto mb-2 text-gray-300" />
                    {hasActiveFilters
                      ? "Filtrelere uygun sipariş bulunamadı"
                      : "Sipariş bulunamadı"}
                  </TableCell>
                </TableRow>
              ) : (
                filteredOrders.map((order: TrendyolOrder) => (
                  <React.Fragment key={order.id}>
                    <TableRow
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => toggleOrderExpand(order.id)}
                    >
                      <TableCell>
                        <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                          {expandedOrders.has(order.id) ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          )}
                        </Button>
                      </TableCell>
                      <TableCell>
                        <div>
                          <p className="font-medium text-sm">
                            {order.tyOrderNumber}
                          </p>
                          <p className="text-xs text-gray-500">
                            Paket: {order.packageNo}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell className="text-sm">
                        {formatDate(order.orderDate)}
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="font-medium">
                          {formatCurrency(order.totalPrice)} TL
                        </span>
                        <p className="text-xs text-gray-500">
                          Brüt: {formatCurrency(order.grossAmount)} TL
                        </p>
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="text-sm text-orange-600">
                          {formatCurrency(order.estimatedCommission)} TL
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="text-sm text-red-600">
                          {formatCurrency(order.stoppage)} TL
                        </span>
                      </TableCell>
                      <TableCell className="text-center">
                        <span
                          className={cn(
                            "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium",
                            getStatusColor(order.status)
                          )}
                        >
                          {orderStatusLabels[order.status as OrderStatus] ||
                            order.status}
                        </span>
                      </TableCell>
                      <TableCell className="text-center">
                        <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-gray-100 text-xs font-medium">
                          {order.orderItems.length}
                        </span>
                      </TableCell>
                    </TableRow>
                    {expandedOrders.has(order.id) && (
                      <TableRow>
                        <TableCell colSpan={8} className="p-0">
                          <div className="px-4 pb-4">
                            <OrderItemsRow items={order.orderItems} />
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-200">
            <p className="text-sm text-gray-500">
              Sayfa {data.number + 1} / {data.totalPages} ({data.totalElements}{" "}
              sipariş)
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={data.first}
              >
                <ChevronLeft className="h-4 w-4" />
                Önceki
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.last}
              >
                Sonraki
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
