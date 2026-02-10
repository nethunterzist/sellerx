"use client";

import { useState, useMemo, useEffect } from "react";
import { useRouter } from "next/navigation";
import { usePurchaseOrders } from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { cn } from "@/lib/utils";
import { ChevronRight, Package, FileText, Truck, CheckCircle, Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { PurchasingDateFilter, type PurchasingDatePreset } from "@/components/purchasing/purchasing-date-filter";
import type { PurchaseOrderStatus, PurchaseOrderSummary } from "@/types/purchasing";
import type { DateRange } from "react-day-picker";

// Custom hook for debounced value
function useDebouncedValue<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debouncedValue;
}

interface RecentOrdersListProps {
  storeId: string | undefined;
  activeStatus?: PurchaseOrderStatus | null;
  startDate?: string;
  endDate?: string;
  dateRange?: DateRange;
  onDateRangeChange?: (range: DateRange | undefined, preset: PurchasingDatePreset | undefined) => void;
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
  });
}

export function RecentOrdersList({ storeId, activeStatus, startDate, endDate, dateRange, onDateRangeChange }: RecentOrdersListProps) {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 300);

  const { data: orders, isLoading } = usePurchaseOrders(
    storeId,
    activeStatus || undefined,
    debouncedSearch || undefined,
    undefined, // supplierId
    startDate,
    endDate
  );

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border border-border">
        <div className="p-4 border-b border-border">
          <div className="h-5 w-32 bg-muted rounded animate-pulse" />
        </div>
        <div className="p-4 space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-14 bg-muted rounded animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  const recentOrders = (orders || []).slice(0, 5);

  return (
    <div className="bg-card rounded-xl border border-border">
      <div className="p-4 border-b border-border flex items-center justify-between">
        <h3 className="font-semibold text-foreground">
          {activeStatus ? `${statusConfig[activeStatus].label} Siparisler` : "Son Siparisler"}
        </h3>
        {onDateRangeChange && (
          <PurchasingDateFilter
            dateRange={dateRange}
            onDateRangeChange={onDateRangeChange}
          />
        )}
      </div>

      {/* Search Input */}
      <div className="p-4 border-b border-border">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Siparis veya tedarikci ara..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 h-9"
          />
        </div>
      </div>

      {recentOrders.length === 0 ? (
        <div className="p-8 text-center">
          <Package className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
          <p className="text-sm text-muted-foreground">Henuz siparis yok</p>
        </div>
      ) : (
        <div className="divide-y divide-border">
          {recentOrders.map((order) => {
            const config = statusConfig[order.status];
            const StatusIcon = config.icon;

            return (
              <button
                key={order.id}
                onClick={() => router.push(`/purchasing/${order.id}`)}
                className="w-full p-4 text-left hover:bg-muted/50 transition-colors flex items-center gap-4"
              >
                <div className={cn("p-2 rounded-lg", config.bgColor)}>
                  <StatusIcon className={cn("h-4 w-4", config.color)} />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-foreground truncate">
                      {order.poNumber}
                    </span>
                    <span
                      className={cn(
                        "text-xs px-2 py-0.5 rounded-full",
                        config.bgColor,
                        config.color
                      )}
                    >
                      {config.label}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-xs text-muted-foreground">
                      {order.supplierName || "Tedarikci belirtilmemis"}
                    </span>
                    <span className="text-muted-foreground">·</span>
                    <span className="text-xs text-muted-foreground">
                      {order.itemCount} urun
                    </span>
                  </div>
                </div>

                <div className="text-right">
                  <p className="font-medium text-foreground">
                    {formatCurrency(order.totalCost)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatDate(order.poDate)}
                  </p>
                </div>

                <ChevronRight className="h-4 w-4 text-muted-foreground" />
              </button>
            );
          })}
        </div>
      )}

      {/* Footer with "Tümünü Gör" */}
      {(orders || []).length > 0 && (
        <div className="p-4 border-t border-border flex justify-end">
          <button
            onClick={() => router.push("/purchasing/orders")}
            className="text-xs text-primary hover:underline flex items-center gap-1"
          >
            Tumunu Gor
            <ChevronRight className="h-3 w-3" />
          </button>
        </div>
      )}
    </div>
  );
}
