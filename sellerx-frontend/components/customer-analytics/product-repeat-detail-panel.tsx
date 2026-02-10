"use client";

import { X, Package, ExternalLink, Users, Repeat, ShoppingBag, Clock, Loader2, User, MapPin } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetTitle,
} from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import { useCurrency } from "@/lib/contexts/currency-context";
import { useProductBuyersInfinite } from "@/hooks/queries/use-customer-analytics";
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll";
import type { ProductRepeatData, ProductBuyer } from "@/types/customer-analytics";

interface ProductRepeatDetailPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  product: ProductRepeatData | null;
  storeId: string | undefined;
}

// Individual buyer row
function BuyerRow({
  buyer,
  formatCurrency,
}: {
  buyer: ProductBuyer;
  formatCurrency: (value: number) => string;
}) {
  return (
    <div className="flex items-center gap-3 px-4 py-3 border-b border-border last:border-b-0 hover:bg-muted/50">
      <div className="w-9 h-9 bg-muted rounded-full flex items-center justify-center flex-shrink-0">
        <User className="h-4 w-4 text-muted-foreground" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-foreground truncate">
          {buyer.customerName || "İsimsiz Müşteri"}
        </p>
        {buyer.city && (
          <p className="text-xs text-muted-foreground flex items-center gap-1">
            <MapPin className="h-3 w-3" />
            {buyer.city}
          </p>
        )}
      </div>
      <div className="text-right flex-shrink-0">
        <p className="text-sm font-semibold text-foreground">
          {buyer.purchaseCount}x
        </p>
        <p className="text-xs text-muted-foreground">
          {formatCurrency(buyer.totalSpend)}
        </p>
      </div>
    </div>
  );
}

export function ProductRepeatDetailPanel({
  open,
  onOpenChange,
  product,
  storeId,
}: ProductRepeatDetailPanelProps) {
  const { formatCurrency } = useCurrency();

  // Fetch paginated buyers with infinite scroll
  const {
    data: buyersData,
    isLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useProductBuyersInfinite(
    storeId,
    open && product ? product.barcode : undefined
  );

  // Infinite scroll sentinel
  const sentinelRef = useInfiniteScroll({
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  });

  if (!product) return null;

  // Flatten all pages into single array
  const allBuyers = buyersData?.pages.flatMap((page) => page.content) ?? [];
  const totalBuyers = buyersData?.pages[0]?.totalElements ?? product.totalBuyers;

  // Use product from list for header info
  const displayData = product;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[480px] sm:max-w-[480px] p-0 overflow-y-auto">
        {/* Accessibility: Visually hidden title for screen readers */}
        <SheetTitle className="sr-only">Ürün Detayları - {displayData.productName}</SheetTitle>

        {/* Header */}
        <div className="sticky top-0 z-10 border-b border-border bg-[#565d6a]">
          <div className="flex items-start gap-3 p-4 relative">
            {displayData.image ? (
              <a
                href={displayData.productUrl || "#"}
                target="_blank"
                rel="noopener noreferrer"
                className="relative group flex-shrink-0"
                onClick={(e) => { if (!displayData.productUrl) e.preventDefault(); }}
              >
                <img
                  src={displayData.image}
                  alt={displayData.productName}
                  className="w-14 h-14 rounded-lg object-cover border-2 border-white/20 group-hover:border-orange-400 transition-colors"
                />
                {displayData.productUrl && (
                  <div className="absolute inset-0 rounded-lg bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                    <ExternalLink className="h-4 w-4 text-white" />
                  </div>
                )}
              </a>
            ) : (
              <div className="w-14 h-14 bg-white/20 rounded-lg flex items-center justify-center flex-shrink-0">
                <Package className="h-7 w-7 text-white" />
              </div>
            )}
            <div className="flex-1 min-w-0 pr-8">
              {displayData.productUrl ? (
                <a
                  href={displayData.productUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm font-semibold text-white hover:underline line-clamp-2 block"
                >
                  {displayData.productName}
                </a>
              ) : (
                <p className="text-sm font-semibold text-white line-clamp-2">
                  {displayData.productName}
                </p>
              )}
              <p className="text-xs text-white/70 mt-1">
                {displayData.barcode}
              </p>
              {displayData.productUrl && (
                <a
                  href={displayData.productUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-xs text-white/70 hover:text-white mt-1"
                >
                  <ExternalLink className="h-3 w-3" />
                  Trendyol'da Görüntüle
                </a>
              )}
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
            <p className="text-xs text-muted-foreground flex items-center justify-center gap-1">
              <Users className="h-3 w-3" />
              Toplam Alıcı
            </p>
            <p className="text-lg font-semibold text-foreground mt-0.5">
              {displayData.totalBuyers}
            </p>
          </div>
          <div className="p-3 text-center">
            <p className="text-xs text-muted-foreground flex items-center justify-center gap-1">
              <Repeat className="h-3 w-3" />
              Tekrar Alıcı
            </p>
            <p className="text-lg font-semibold text-foreground mt-0.5">
              {displayData.repeatBuyers}
            </p>
          </div>
          <div className="p-3 text-center">
            <p className="text-xs text-muted-foreground flex items-center justify-center gap-1">
              <ShoppingBag className="h-3 w-3" />
              Satılan Adet
            </p>
            <p className="text-lg font-semibold text-foreground mt-0.5">
              {displayData.totalQuantitySold}
            </p>
          </div>
        </div>

        {/* Metrics */}
        <div className="flex items-center justify-between px-4 py-3 bg-muted/50 border-b border-border">
          <div className="flex items-center gap-2">
            <Badge variant="secondary" className="text-xs">
              Tekrar Oranı: %{displayData.repeatRate.toFixed(1)}
            </Badge>
          </div>
          {displayData.avgDaysBetweenRepurchase > 0 && (
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="h-3 w-3" />
              Ort. {Math.round(displayData.avgDaysBetweenRepurchase)} gün
            </div>
          )}
        </div>

        {/* Buyers Section Header */}
        <div className="px-4 py-3 border-b border-border bg-muted/30">
          <h3 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <Users className="h-4 w-4" />
            Bu Ürünü Alan Müşteriler
            <Badge variant="outline" className="text-xs">
              {totalBuyers}
            </Badge>
          </h3>
        </div>

        {/* Buyers List with Infinite Scroll */}
        {isLoading && allBuyers.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : allBuyers.length > 0 ? (
          <div>
            {allBuyers.map((buyer) => (
              <BuyerRow
                key={buyer.customerId}
                buyer={buyer}
                formatCurrency={formatCurrency}
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
            <Users className="h-8 w-8 mb-2" />
            <p className="text-sm">Müşteri bilgisi bulunamadı</p>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
