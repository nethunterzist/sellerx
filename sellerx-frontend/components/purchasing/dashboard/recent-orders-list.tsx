"use client";

import { useRouter } from "next/navigation";
import { usePurchaseOrders } from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { cn } from "@/lib/utils";
import { ChevronRight, Package, FileText, Truck, CheckCircle } from "lucide-react";
import type { PurchaseOrderStatus, PurchaseOrderSummary } from "@/types/purchasing";

interface RecentOrdersListProps {
  storeId: string | undefined;
  activeStatus?: PurchaseOrderStatus | null;
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

export function RecentOrdersList({ storeId, activeStatus }: RecentOrdersListProps) {
  const router = useRouter();
  const { formatCurrency } = useCurrency();

  const { data: orders, isLoading } = usePurchaseOrders(
    storeId,
    activeStatus || undefined
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
        <button
          onClick={() => router.push("/purchasing")}
          className="text-xs text-primary hover:underline flex items-center gap-1"
        >
          Tumunu Gor
          <ChevronRight className="h-3 w-3" />
        </button>
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
    </div>
  );
}
