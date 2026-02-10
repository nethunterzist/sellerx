"use client";

import { useState, useMemo, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { usePurchaseOrders } from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { cn } from "@/lib/utils";
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
import { PurchasingDateFilter, type PurchasingDatePreset } from "@/components/purchasing/purchasing-date-filter";
import {
  Search,
  Package,
  FileText,
  Truck,
  CheckCircle,
  ArrowLeft,
  X,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import type { PurchaseOrderStatus } from "@/types/purchasing";
import type { DateRange } from "react-day-picker";
import { format } from "date-fns";
import {
  FilterBarSkeleton,
  TableSkeleton,
  PaginationSkeleton,
} from "@/components/ui/skeleton-blocks";

const PAGE_SIZE = 20;

// Custom hook for debounced value
function useDebouncedValue<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debouncedValue;
}

const statusConfig: Record<
  PurchaseOrderStatus,
  { label: string; color: string; bgColor: string; icon: typeof FileText }
> = {
  DRAFT: {
    label: "Taslak",
    color: "text-gray-600 dark:text-gray-400",
    bgColor: "bg-gray-100 dark:bg-gray-800",
    icon: FileText,
  },
  ORDERED: {
    label: "Siparis Verildi",
    color: "text-blue-600 dark:text-blue-400",
    bgColor: "bg-blue-100 dark:bg-blue-900/30",
    icon: Package,
  },
  SHIPPED: {
    label: "Yolda",
    color: "text-amber-600 dark:text-amber-400",
    bgColor: "bg-amber-100 dark:bg-amber-900/30",
    icon: Truck,
  },
  CLOSED: {
    label: "Tamamlandi",
    color: "text-green-600 dark:text-green-400",
    bgColor: "bg-green-100 dark:bg-green-900/30",
    icon: CheckCircle,
  },
};

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("tr-TR", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function PurchasingOrdersPageSkeleton() {
  return (
    <div className="space-y-6">
      <FilterBarSkeleton showSearch={true} buttonCount={3} />
      <TableSkeleton columns={6} rows={10} />
      <PaginationSkeleton />
    </div>
  );
}

export default function PurchasingOrdersPage() {
  const router = useRouter();
  const { formatCurrency } = useCurrency();

  // State
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 300);
  const [currentPage, setCurrentPage] = useState(0);
  const [activeStatus, setActiveStatus] = useState<PurchaseOrderStatus | "all">("all");
  const [dateRange, setDateRange] = useState<DateRange | undefined>(undefined);

  // Format dates for API
  const startDate = dateRange?.from ? format(dateRange.from, "yyyy-MM-dd") : undefined;
  const endDate = dateRange?.to ? format(dateRange.to, "yyyy-MM-dd") : undefined;

  const handleDateRangeChange = (range: DateRange | undefined, preset: PurchasingDatePreset | undefined) => {
    setDateRange(range);
    setCurrentPage(0);
  };

  // Store
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Data
  const { data: orders, isLoading } = usePurchaseOrders(
    storeId || undefined,
    activeStatus === "all" ? undefined : activeStatus,
    debouncedSearch || undefined,
    undefined,
    startDate,
    endDate
  );

  // Reset page when search or status changes
  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearch, activeStatus]);

  // Pagination
  const totalItems = orders?.length || 0;
  const totalPages = Math.ceil(totalItems / PAGE_SIZE);

  const paginatedOrders = useMemo(() => {
    const start = currentPage * PAGE_SIZE;
    return (orders || []).slice(start, start + PAGE_SIZE);
  }, [orders, currentPage]);

  // Active filters count
  const activeFiltersCount = useMemo(() => {
    let count = 0;
    if (search) count++;
    if (activeStatus !== "all") count++;
    if (dateRange?.from) count++;
    return count;
  }, [search, activeStatus, dateRange]);

  const clearFilters = () => {
    setSearch("");
    setActiveStatus("all");
    setDateRange(undefined);
    setCurrentPage(0);
  };

  // Generate page numbers for pagination (same as products page)
  const getPageNumbers = (): (number | string)[] => {
    const pages: (number | string)[] = [];

    if (totalPages <= 7) {
      for (let i = 0; i < totalPages; i++) pages.push(i);
    } else {
      pages.push(0);

      if (currentPage > 3) {
        pages.push("...");
      }

      const start = Math.max(1, currentPage - 1);
      const end = Math.min(totalPages - 2, currentPage + 1);

      for (let i = start; i <= end; i++) {
        if (!pages.includes(i)) pages.push(i);
      }

      if (currentPage < totalPages - 4) {
        pages.push("...");
      }

      if (!pages.includes(totalPages - 1)) {
        pages.push(totalPages - 1);
      }
    }

    return pages;
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

  if (isLoading) return <PurchasingOrdersPageSkeleton />;

  return (
    <div className="space-y-6">
      {/* Header - Title with Back Button */}
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push("/purchasing")}
          className="h-9 w-9"
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-xl font-semibold text-foreground">Tum Siparisler</h1>
      </div>

      {/* Table Container with Filters Inside */}
      <div className="bg-card rounded-lg border border-border">
        {/* Filters Row - Inside Container */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-border">
          {/* Search */}
          <div className="relative flex-1 min-w-[200px] max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Siparis veya tedarikci ara..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>

          {/* Status Filter */}
          <Select
            value={activeStatus}
            onValueChange={(value) => setActiveStatus(value as PurchaseOrderStatus | "all")}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Durum secin" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tum Durumlar</SelectItem>
              <SelectItem value="DRAFT">Taslak</SelectItem>
              <SelectItem value="ORDERED">Siparis Verildi</SelectItem>
              <SelectItem value="SHIPPED">Yolda</SelectItem>
              <SelectItem value="CLOSED">Tamamlandi</SelectItem>
            </SelectContent>
          </Select>

          {/* Clear Filters */}
          {activeFiltersCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={clearFilters}
              className="gap-1 text-muted-foreground"
            >
              <X className="h-3 w-3" />
              Temizle ({activeFiltersCount})
            </Button>
          )}

          {/* Date Filter - Far Right */}
          <div className="ml-auto">
            <PurchasingDateFilter
              dateRange={dateRange}
              onDateRangeChange={handleDateRangeChange}
            />
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead>Siparis No</TableHead>
                <TableHead>Tedarikci</TableHead>
                <TableHead>Durum</TableHead>
                <TableHead className="text-right">Urun Sayisi</TableHead>
                <TableHead className="text-right">Tutar</TableHead>
                <TableHead className="text-right">Tarih</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {paginatedOrders.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-32 text-center">
                    <div className="flex flex-col items-center justify-center text-muted-foreground">
                      <Package className="h-10 w-10 mb-2 opacity-50" />
                      <p>Siparis bulunamadi</p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                paginatedOrders.map((order) => {
                  const config = statusConfig[order.status];
                  const StatusIcon = config.icon;

                  return (
                    <TableRow
                      key={order.id}
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => router.push(`/purchasing/${order.id}`)}
                    >
                      <TableCell className="font-medium">{order.poNumber}</TableCell>
                      <TableCell>{order.supplierName || "Belirtilmemis"}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <div className={cn("p-1.5 rounded-lg", config.bgColor)}>
                            <StatusIcon className={cn("h-3.5 w-3.5", config.color)} />
                          </div>
                          <span className={cn("text-sm", config.color)}>
                            {config.label}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="text-right">{order.itemCount}</TableCell>
                      <TableCell className="text-right font-medium">
                        {formatCurrency(order.totalCost)}
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">
                        {formatDate(order.poDate)}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </div>

        {/* Pagination - Same style as products page */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border">
            <p className="text-sm text-muted-foreground">
              Sayfa {currentPage + 1} / {totalPages} ({totalItems} siparis)
            </p>
            <div className="flex items-center gap-1">
              {/* Previous Button */}
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                disabled={currentPage === 0}
                className="h-8 w-8 p-0"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>

              {/* Page Numbers */}
              {getPageNumbers().map((p, idx) =>
                p === "..." ? (
                  <span key={`ellipsis-${idx}`} className="px-2 text-muted-foreground">...</span>
                ) : (
                  <Button
                    key={p}
                    variant={currentPage === p ? "default" : "outline"}
                    size="sm"
                    onClick={() => setCurrentPage(p as number)}
                    className={cn(
                      "h-8 w-8 p-0",
                      currentPage === p && "bg-primary text-primary-foreground"
                    )}
                  >
                    {(p as number) + 1}
                  </Button>
                )
              )}

              {/* Next Button */}
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={currentPage === totalPages - 1}
                className="h-8 w-8 p-0"
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
