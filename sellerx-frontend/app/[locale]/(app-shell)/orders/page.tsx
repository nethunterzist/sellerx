"use client";

import React, { useState, useMemo } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useOrdersByDateRange, useOrdersByStatus } from "@/hooks/queries/use-orders";
import type { DateRange } from "react-day-picker";
import { format } from "date-fns";
import {
  OrderDateFilter,
  getOrderPresetRange,
  type OrderDatePreset,
} from "@/components/orders/order-date-filter";
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
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  StatCardSkeleton,
  FilterBarSkeleton,
  TableSkeleton,
  PaginationSkeleton,
} from "@/components/ui/skeleton-blocks";

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
      return "bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300";
    case "shipped":
      return "bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300";
    case "created":
    case "picking":
      return "bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300";
    case "cancelled":
    case "returned":
      return "bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300";
    default:
      return "bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-300";
  }
}

function OrderItemsRow({ items }: { items: OrderItem[] }) {
  const { formatCurrency } = useCurrency();
  return (
    <div className="bg-muted p-4 rounded-lg mt-2 space-y-2">
      <p className="text-xs font-medium text-muted-foreground uppercase">
        Siparis Kalemleri
      </p>
      {items.map((item, idx) => (
        <div
          key={idx}
          className="flex items-center justify-between text-sm border-b border-border pb-2 last:border-0"
        >
          <div className="flex-1">
            <p className="font-medium text-foreground">{item.productName}</p>
            <p className="text-xs text-muted-foreground">Barkod: {item.barcode}</p>
          </div>
          <div className="text-right">
            <p className="font-medium">
              {item.quantity} x {formatCurrency(item.price)}
            </p>
            <p className="text-xs text-muted-foreground">
              Maliyet: {formatCurrency(item.cost)} | Komisyon:{" "}
              {formatCurrency(item.unitEstimatedCommission)}
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

function OrdersPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <StatCardSkeleton key={i} />
        ))}
      </div>
      <FilterBarSkeleton showSearch={true} buttonCount={3} />
      <TableSkeleton columns={10} rows={10} />
      <PaginationSkeleton />
    </div>
  );
}

export default function OrdersPage() {
  const { formatCurrency } = useCurrency();
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [expandedOrders, setExpandedOrders] = useState<Set<string>>(new Set());
  const [statusFilter, setStatusFilter] = useState<OrderStatus | undefined>(
    undefined
  );

  // Date filter state - default to last 7 days
  const [datePreset, setDatePreset] = useState<OrderDatePreset>("last7days");
  const [dateRange, setDateRange] = useState<DateRange | undefined>(() =>
    getOrderPresetRange("last7days")
  );

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Format dates for API (ISO datetime format)
  const { startDate, endDate } = useMemo(() => {
    if (!dateRange?.from || !dateRange?.to) {
      return { startDate: undefined, endDate: undefined };
    }
    // Set start to beginning of day
    const start = new Date(dateRange.from);
    start.setHours(0, 0, 0, 0);
    // Set end to end of day
    const end = new Date(dateRange.to);
    end.setHours(23, 59, 59, 999);
    return {
      startDate: format(start, "yyyy-MM-dd'T'HH:mm:ss"),
      endDate: format(end, "yyyy-MM-dd'T'HH:mm:ss"),
    };
  }, [dateRange]);

  // Fetch orders by date range (when no status filter)
  const dateRangeQuery = useOrdersByDateRange(
    storeId,
    startDate,
    endDate,
    page,
    20
  );

  // Fetch orders by status (when status filter is active)
  const statusQuery = useOrdersByStatus(
    storeId,
    statusFilter,
    page,
    20
  );

  // Use status query when filter is active, otherwise use date range query
  const { data, isLoading, error } = statusFilter ? statusQuery : dateRangeQuery;

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

  const handleDateRangeChange = (
    range: DateRange | undefined,
    preset: OrderDatePreset
  ) => {
    setDateRange(range);
    setDatePreset(preset);
    setPage(0);
  };

  const clearFilters = () => {
    setStatusFilter(undefined);
    setSearchQuery("");
    setDatePreset("last7days");
    setDateRange(getOrderPresetRange("last7days"));
    setPage(0);
  };

  // Filter orders by search query only (status is handled server-side)
  const filteredOrders =
    data?.content?.filter((o) => {
      // Search filter only (status filtering is now server-side)
      if (searchQuery) {
        const query = searchQuery.toLowerCase();
        const matchesOrderNumber = o.tyOrderNumber
          .toLowerCase()
          .includes(query);
        const matchesProduct = o.orderItems.some((item) =>
          item.productName.toLowerCase().includes(query)
        );
        if (!matchesOrderNumber && !matchesProduct) {
          return false;
        }
      }
      return true;
    }) || [];

  const hasActiveFilters =
    statusFilter || searchQuery || datePreset !== "last7days";

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  if (isLoading) return <OrdersPageSkeleton />;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">
            {data?.totalElements
              ? `${statusFilter ? orderStatusLabels[statusFilter] + " - " : ""}${data.totalElements} sipariş`
              : "Siparişler yükleniyor..."}
          </p>
        </div>

        <OrderDateFilter
          dateRange={dateRange}
          onDateRangeChange={handleDateRangeChange}
        />
      </div>

      {/* Statistics Cards */}
      <OrderStatsCards storeId={storeId} />

      {/* Error State */}
      {error && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <p className="text-red-800 dark:text-red-200 text-sm">
            Siparişler yüklenirken hata: {error.message}
          </p>
        </div>
      )}

      {/* Filters */}
      <div className="bg-card rounded-lg border border-border p-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Filter className="h-4 w-4" />
            <span>Filtreler</span>
          </div>

          <div className="flex flex-wrap items-center gap-3 flex-1">
            {/* Search Input */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
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
                className="h-9 px-3 text-muted-foreground hover:text-foreground"
              >
                <X className="h-4 w-4 mr-1" />
                Filtreleri Temizle
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-card rounded-lg border border-border">
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
                <TableHead className="text-right">Kargo</TableHead>
                <TableHead className="text-right">Kar Marjı</TableHead>
                <TableHead className="text-center">Durum</TableHead>
                <TableHead className="text-center">Kalem</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredOrders.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={10}
                    className="h-24 text-center text-muted-foreground"
                  >
                    <Package className="h-8 w-8 mx-auto mb-2 opacity-50" />
                    {hasActiveFilters
                      ? "Filtrelere uygun sipariş bulunamadı"
                      : "Sipariş bulunamadı"}
                  </TableCell>
                </TableRow>
              ) : (
                filteredOrders.map((order: TrendyolOrder) => (
                  <React.Fragment key={order.id}>
                    <TableRow
                      className="hover:bg-muted cursor-pointer"
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
                          <p className="text-xs text-muted-foreground">
                            Paket: {order.packageNo}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell className="text-sm">
                        {formatDate(order.orderDate)}
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="font-medium">
                          {formatCurrency(order.totalPrice)}
                        </span>
                        <p className="text-xs text-muted-foreground">
                          Brüt: {formatCurrency(order.grossAmount)}
                        </p>
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="text-sm text-orange-600">
                          {formatCurrency(order.estimatedCommission)}
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="text-sm text-red-600">
                          {formatCurrency(order.stoppage)}
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="text-sm text-blue-600">
                          {formatCurrency(order.estimatedShippingCost || 0)}
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        {(() => {
                          // Calculate profit margin
                          const totalCost = order.orderItems.reduce(
                            (sum, item) => sum + (item.cost || 0) * item.quantity,
                            0
                          );
                          const expenses =
                            (order.estimatedCommission || 0) +
                            (order.stoppage || 0) +
                            (order.estimatedShippingCost || 0);
                          const netProfit = order.totalPrice - totalCost - expenses;
                          const margin =
                            order.totalPrice > 0
                              ? (netProfit / order.totalPrice) * 100
                              : 0;
                          return (
                            <span
                              className={`text-sm font-medium ${margin >= 0 ? "text-green-600" : "text-red-600"}`}
                            >
                              %{margin.toFixed(1)}
                            </span>
                          );
                        })()}
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
                        <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-muted text-xs font-medium">
                          {order.orderItems.length}
                        </span>
                      </TableCell>
                    </TableRow>
                    {expandedOrders.has(order.id) && (
                      <TableRow>
                        <TableCell colSpan={10} className="p-0">
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
          <div className="flex items-center justify-between px-4 py-3 border-t border-border">
            <p className="text-sm text-muted-foreground">
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
