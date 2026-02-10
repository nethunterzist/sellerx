"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { ChevronRight, ChevronDown, User, ExternalLink, X, Package, Calendar, MapPin, ShoppingBag, Loader2 } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Badge } from "@/components/ui/badge";
import { useCurrency } from "@/lib/contexts/currency-context";
import { useCustomerOrdersInfinite } from "@/hooks/queries/use-customer-analytics";
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll";
import type { CustomerListItem, CustomerOrderDto, CustomerOrderItemDto } from "@/types/customer-analytics";

interface CustomerDetailPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  customer: CustomerListItem | null;
  storeId: string | undefined;
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("tr-TR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function formatDateTime(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("tr-TR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getStatusBadge(status: string) {
  const statusMap: Record<string, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
    Delivered: { label: "Teslim Edildi", variant: "default" },
    Shipped: { label: "Kargoda", variant: "secondary" },
    Created: { label: "Oluşturuldu", variant: "outline" },
    Picking: { label: "Hazırlanıyor", variant: "outline" },
    Invoiced: { label: "Faturalandı", variant: "secondary" },
    Cancelled: { label: "İptal Edildi", variant: "destructive" },
    Returned: { label: "İade Edildi", variant: "destructive" },
    UnDelivered: { label: "Teslim Edilemedi", variant: "destructive" },
  };

  const config = statusMap[status] || { label: status, variant: "outline" as const };
  return <Badge variant={config.variant} className="text-xs">{config.label}</Badge>;
}

// Order card with expandable product details
function OrderCard({
  order,
  formatCurrency,
  defaultOpen = false,
}: {
  order: CustomerOrderDto;
  formatCurrency: (value: number) => string;
  defaultOpen?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger className="w-full">
        <div className="flex items-start gap-3 p-4 border-b border-border hover:bg-muted/50 cursor-pointer">
          <div className="flex-shrink-0 mt-0.5">
            {isOpen ? (
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            ) : (
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            )}
          </div>
          <div className="flex-1 min-w-0 text-left">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-foreground">
                #{order.tyOrderNumber}
              </span>
              {getStatusBadge(order.status)}
            </div>
            <div className="flex items-center gap-3 mt-1 text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {formatDateTime(order.orderDate)}
              </span>
              {order.shipmentCity && (
                <span className="flex items-center gap-1">
                  <MapPin className="h-3 w-3" />
                  {order.shipmentCity}
                </span>
              )}
            </div>
          </div>
          <div className="text-right flex-shrink-0">
            <p className="text-sm font-semibold text-foreground">
              {formatCurrency(order.totalPrice)}
            </p>
            <p className="text-xs text-muted-foreground">
              {order.items.length} ürün
            </p>
          </div>
        </div>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="bg-muted/30 border-b border-border">
          {order.items.map((item, index) => (
            <ProductItem key={index} item={item} formatCurrency={formatCurrency} />
          ))}
          {order.totalDiscount > 0 && (
            <div className="flex items-center justify-between px-4 py-2 border-t border-border/50 text-xs">
              <span className="text-muted-foreground">Toplam İndirim</span>
              <span className="text-red-600 font-medium">
                -{formatCurrency(order.totalDiscount)}
              </span>
            </div>
          )}
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}

// Individual product item within an order
function ProductItem({
  item,
  formatCurrency,
}: {
  item: CustomerOrderItemDto;
  formatCurrency: (value: number) => string;
}) {
  return (
    <div className="flex items-start gap-3 px-4 py-3 pl-10 border-b border-border/50 last:border-b-0">
      {item.image ? (
        <a
          href={item.productUrl || "#"}
          target="_blank"
          rel="noopener noreferrer"
          className="relative group flex-shrink-0 cursor-pointer"
          onClick={(e) => { if (!item.productUrl) e.preventDefault(); }}
        >
          <img
            src={item.image}
            alt={item.productName}
            className="w-10 h-10 rounded object-cover border border-border group-hover:border-orange-400 transition-colors"
          />
          {item.productUrl && (
            <div className="absolute inset-0 rounded bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <ExternalLink className="h-3 w-3 text-white" />
            </div>
          )}
        </a>
      ) : (
        <div className="w-10 h-10 bg-muted rounded flex items-center justify-center flex-shrink-0">
          <Package className="h-5 w-5 text-muted-foreground" />
        </div>
      )}
      <div className="flex-1 min-w-0">
        {item.productUrl ? (
          <a
            href={item.productUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs font-medium text-foreground line-clamp-2 hover:underline block"
          >
            {item.productName}
          </a>
        ) : (
          <p className="text-xs font-medium text-foreground line-clamp-2">
            {item.productName}
          </p>
        )}
        <p className="text-xs text-muted-foreground mt-0.5">
          {item.barcode}
        </p>
      </div>
      <div className="text-right flex-shrink-0">
        <p className="text-xs font-medium text-foreground">
          {formatCurrency(item.price)}
        </p>
        <p className="text-xs text-muted-foreground">
          {item.quantity} adet × {formatCurrency(item.unitPrice)}
        </p>
        {item.discount > 0 && (
          <p className="text-xs text-red-600">
            -{formatCurrency(item.discount)}
          </p>
        )}
      </div>
    </div>
  );
}

export function CustomerDetailPanel({
  open,
  onOpenChange,
  customer,
  storeId,
}: CustomerDetailPanelProps) {
  const { formatCurrency } = useCurrency();

  // Fetch paginated customer orders with infinite scroll
  const {
    data: ordersData,
    isLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useCustomerOrdersInfinite(
    storeId,
    open && customer ? customer.customerKey : undefined
  );

  // Infinite scroll sentinel
  const sentinelRef = useInfiniteScroll({
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  });

  if (!customer) return null;

  // Flatten all pages into single array
  const allOrders = ordersData?.pages.flatMap((page) => page.content) ?? [];
  const totalOrders = ordersData?.pages[0]?.totalElements ?? customer.orderCount;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[480px] sm:max-w-[480px] p-0 overflow-y-auto">
        {/* Accessibility: Visually hidden title for screen readers */}
        <SheetTitle className="sr-only">Müşteri Detayları - {customer.displayName}</SheetTitle>

        {/* Header */}
        <div className="sticky top-0 z-10 border-b border-border bg-[#565d6a]">
          <div className="flex items-start gap-3 p-4 relative">
            <div className="w-14 h-14 bg-white/20 rounded-full flex items-center justify-center flex-shrink-0">
              <User className="h-7 w-7 text-white" />
            </div>
            <div className="flex-1 min-w-0 pr-8">
              <p className="text-sm font-semibold text-white mt-0.5 line-clamp-2">
                {customer.displayName}
              </p>
              {customer.city && (
                <p className="text-xs text-white/70 mt-1 flex items-center gap-1">
                  <MapPin className="h-3 w-3" />
                  {customer.city}
                </p>
              )}
              <div className="flex items-center gap-3 mt-2">
                <Badge variant="secondary" className="bg-white/20 text-white text-xs">
                  {customer.orderCount} sipariş
                </Badge>
                <Badge variant="secondary" className="bg-white/20 text-white text-xs">
                  {customer.itemCount} ürün
                </Badge>
              </div>
            </div>
            {/* Close Button */}
            <button
              onClick={() => onOpenChange(false)}
              className="absolute right-4 top-4 p-1.5 rounded-md hover:bg-white/20 transition-colors text-white"
              title="Kapat"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Summary Stats */}
        <div className="grid grid-cols-3 divide-x divide-border border-b border-border">
          <div className="p-3 text-center">
            <p className="text-xs text-muted-foreground">Toplam Harcama</p>
            <p className="text-sm font-semibold text-foreground mt-0.5">
              {formatCurrency(customer.totalSpend)}
            </p>
          </div>
          <div className="p-3 text-center">
            <p className="text-xs text-muted-foreground">Ort. Sipariş</p>
            <p className="text-sm font-semibold text-foreground mt-0.5">
              {formatCurrency(customer.avgOrderValue)}
            </p>
          </div>
          <div className="p-3 text-center">
            <p className="text-xs text-muted-foreground">Tekrar Sıklığı</p>
            <p className="text-sm font-semibold text-foreground mt-0.5">
              {customer.avgRepeatIntervalDays != null
                ? `${Math.round(customer.avgRepeatIntervalDays)} gün`
                : "-"}
            </p>
          </div>
        </div>

        {/* Date Range */}
        <div className="flex items-center justify-between px-4 py-2 bg-muted/50 border-b border-border text-xs">
          <span className="text-muted-foreground">
            İlk sipariş: {formatDate(customer.firstOrderDate)}
          </span>
          <span className="text-muted-foreground">
            Son sipariş: {formatDate(customer.lastOrderDate)}
          </span>
        </div>

        {/* Orders Section Header */}
        <div className="px-4 py-3 border-b border-border bg-muted/30">
          <h3 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <ShoppingBag className="h-4 w-4" />
            Sipariş Geçmişi
            <Badge variant="outline" className="text-xs">
              {totalOrders}
            </Badge>
          </h3>
        </div>

        {/* Orders List with Infinite Scroll */}
        {isLoading && allOrders.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : allOrders.length > 0 ? (
          <div>
            {allOrders.map((order, index) => (
              <OrderCard
                key={order.orderId}
                order={order}
                formatCurrency={formatCurrency}
                defaultOpen={index === 0} // First order expanded by default
              />
            ))}
            {/* Sentinel element for infinite scroll */}
            <div ref={sentinelRef} className="h-1" />
            {/* Loading indicator for next page */}
            {isFetchingNextPage && (
              <div className="flex items-center justify-center py-4">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            )}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <ShoppingBag className="h-8 w-8 mb-2" />
            <p className="text-sm">Sipariş bulunamadı</p>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
