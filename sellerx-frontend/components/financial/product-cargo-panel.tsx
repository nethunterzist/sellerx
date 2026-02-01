"use client";

import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import {
  Truck,
  Package,
  ExternalLink,
  Calculator,
  Box,
  Hash,
  Scale,
  ChevronDown,
} from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetTitle,
} from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { useCurrency } from "@/lib/contexts/currency-context";
import { useProductCargoBreakdown } from "@/hooks/queries/use-invoices";
import type { AggregatedProduct, CargoShipmentDetail } from "@/types/invoice";

interface ProductCargoPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  product: AggregatedProduct | null;
  storeId: string;
  startDate: string;
  endDate: string;
}

// Pagination constants
const SHIPMENTS_PER_PAGE = 20;

// Row component for consistent styling
function DetailRow({
  label,
  value,
  isNegative,
  isBold,
}: {
  label: string;
  value: string | React.ReactNode;
  isNegative?: boolean;
  isBold?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-2.5 px-4 border-b border-border">
      <span className={cn("text-sm text-muted-foreground", isBold && "font-semibold text-foreground")}>
        {label}
      </span>
      <span
        className={cn(
          "text-sm",
          isBold ? "font-semibold" : "font-medium",
          isNegative ? "text-red-600" : "text-foreground"
        )}
      >
        {value}
      </span>
    </div>
  );
}

// Shipment row component
function ShipmentRow({
  shipment,
  formatCurrency,
}: {
  shipment: CargoShipmentDetail;
  formatCurrency: (n: number) => string;
}) {
  const formattedDate = shipment.invoiceDate
    ? new Date(shipment.invoiceDate).toLocaleDateString("tr-TR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      })
    : "-";

  return (
    <div className="flex items-center justify-between py-3 px-4 border-b border-border hover:bg-amber-50/50 dark:hover:bg-amber-900/10 transition-colors">
      <div className="flex items-center gap-3">
        <div className="p-2 rounded-lg bg-amber-100 dark:bg-amber-900/30 text-amber-600">
          <Box className="h-4 w-4" />
        </div>
        <div>
          <p className="text-sm font-medium text-foreground">
            #{shipment.orderNumber}
          </p>
          <p className="text-xs text-muted-foreground">
            {formattedDate}
            {shipment.desi && ` · ${shipment.desi} desi`}
          </p>
        </div>
      </div>
      <div className="text-right">
        <p className="font-semibold text-red-600">
          {formatCurrency(shipment.amount)}
        </p>
        {shipment.vatAmount && shipment.vatAmount > 0 && (
          <p className="text-xs text-muted-foreground">
            KDV: {formatCurrency(shipment.vatAmount)}
          </p>
        )}
      </div>
    </div>
  );
}

// Loading skeleton
function LoadingSkeleton() {
  return (
    <div className="divide-y divide-border">
      <div className="p-4">
        <div className="flex items-start gap-3">
          <Skeleton className="h-16 w-16 rounded" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-5 w-full" />
          </div>
        </div>
      </div>
      <div className="h-2 bg-muted" />
      {[...Array(4)].map((_, i) => (
        <div key={i} className="flex items-center justify-between p-4">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-5 w-24" />
        </div>
      ))}
      <div className="h-2 bg-muted" />
      {[...Array(3)].map((_, i) => (
        <div key={i} className="flex items-center justify-between p-4">
          <div className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-lg" />
            <div className="space-y-1">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-3 w-16" />
            </div>
          </div>
          <Skeleton className="h-5 w-24" />
        </div>
      ))}
    </div>
  );
}

export function ProductCargoPanel({
  open,
  onOpenChange,
  product,
  storeId,
  startDate,
  endDate,
}: ProductCargoPanelProps) {
  const { formatCurrency } = useCurrency();

  // Pagination state for shipments
  const [visibleShipmentCount, setVisibleShipmentCount] = useState(SHIPMENTS_PER_PAGE);

  // Reset pagination when panel closes or product changes
  const resetPagination = () => setVisibleShipmentCount(SHIPMENTS_PER_PAGE);

  // Fetch breakdown data when panel opens
  const {
    data: breakdownData,
    isLoading,
    isError,
  } = useProductCargoBreakdown(
    storeId,
    product?.barcode,
    startDate,
    endDate,
    open && !!product?.barcode
  );

  // Calculate visible shipments
  const allShipments = breakdownData?.shipments || [];
  const visibleShipments = useMemo(() => {
    return allShipments.slice(0, visibleShipmentCount);
  }, [allShipments, visibleShipmentCount]);

  const hasMoreShipments = allShipments.length > visibleShipmentCount;
  const remainingShipments = allShipments.length - visibleShipmentCount;

  const handleLoadMore = () => {
    setVisibleShipmentCount((prev) => prev + SHIPMENTS_PER_PAGE);
  };

  // Handle panel close - reset pagination
  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      resetPagination();
    }
    onOpenChange(newOpen);
  };

  if (!product) return null;

  return (
    <Sheet open={open} onOpenChange={handleOpenChange}>
      <SheetContent side="right" className="w-[420px] sm:max-w-[420px] p-0 overflow-y-auto">
        <SheetTitle className="sr-only">Kargo Detayları - {product.productName}</SheetTitle>

        {/* Header with amber/orange theme for KARGO */}
        <div className="sticky top-0 bg-amber-500 z-10">
          <div className="p-4">
            <div className="flex items-center gap-3 text-white">
              <Truck className="h-5 w-5" />
              <div>
                <p className="text-sm font-medium opacity-90">Kargo Detayı</p>
                <p className="text-lg font-bold">Gönderi Dağılımı</p>
              </div>
            </div>
          </div>
        </div>

        {isLoading ? (
          <LoadingSkeleton />
        ) : isError ? (
          <div className="p-8 text-center">
            <p className="text-sm text-muted-foreground">Veri yüklenirken hata oluştu.</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-4"
              onClick={() => onOpenChange(false)}
            >
              Kapat
            </Button>
          </div>
        ) : (
          <div className="divide-y divide-border">
            {/* Product Info */}
            <div className="p-4">
              <div className="flex items-start gap-3">
                {(breakdownData?.productImageUrl || product.productImageUrl) ? (
                  <a
                    href={breakdownData?.productUrl || product.productUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex-shrink-0 group relative"
                  >
                    <img
                      src={breakdownData?.productImageUrl || product.productImageUrl}
                      alt={product.productName || "Ürün"}
                      className="h-16 w-16 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = "https://via.placeholder.com/64?text=X";
                      }}
                    />
                    <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                      <ExternalLink className="h-2.5 w-2.5 text-white" />
                    </div>
                  </a>
                ) : (
                  <div className="h-16 w-16 rounded flex items-center justify-center text-sm font-bold text-white flex-shrink-0 bg-[#F27A1A]">
                    T
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-muted-foreground font-mono">
                    {product.barcode}
                  </p>
                  <p className="text-sm font-medium text-foreground mt-0.5 line-clamp-2">
                    {breakdownData?.productName || product.productName || "Bilinmeyen Ürün"}
                  </p>
                </div>
              </div>
            </div>

            {/* Divider */}
            <div className="h-2 bg-muted" />

            {/* Summary Stats - Kargo Maliyet Özeti */}
            <div className="py-3 px-4 border-b border-border bg-amber-50 dark:bg-amber-900/20">
              <div className="flex items-center gap-2 mb-2">
                <Calculator className="h-4 w-4 text-amber-600" />
                <span className="text-sm font-semibold text-foreground">
                  Kargo Maliyet Özeti
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                Seçili tarih aralığındaki kargo maliyetleri
              </p>
            </div>

            {/* Cargo Cost Details */}
            <DetailRow
              label="Toplam Kargo Maliyeti"
              value={formatCurrency(breakdownData?.totalAmount || product.totalAmount)}
              isBold
              isNegative
            />

            <DetailRow
              label="Toplam KDV (%20)"
              value={formatCurrency(breakdownData?.totalVatAmount || product.totalVatAmount || 0)}
            />

            <DetailRow
              label="Toplam Desi"
              value={`${(breakdownData?.totalDesi || product.totalDesi || 0).toFixed(2)}`}
            />

            <DetailRow
              label="Gönderi Sayısı"
              value={`${breakdownData?.totalShipmentCount || product.totalQuantity} adet`}
            />

            <DetailRow
              label="Sipariş Sayısı"
              value={`${breakdownData?.orderCount || product.invoiceCount} sipariş`}
            />

            {/* Divider */}
            <div className="h-2 bg-muted" />

            {/* Averages Section */}
            <div className="py-3 px-4 border-b border-border bg-muted/30">
              <div className="flex items-center gap-2">
                <Scale className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm font-semibold text-foreground">
                  Ortalamalar
                </span>
              </div>
            </div>

            <DetailRow
              label="Ortalama Desi/Gönderi"
              value={(breakdownData?.averageDesi || 0).toFixed(2)}
            />

            <DetailRow
              label="Ortalama Maliyet/Gönderi"
              value={formatCurrency(breakdownData?.averageCostPerShipment || 0)}
            />

            {/* Divider */}
            <div className="h-2 bg-muted" />

            {/* Shipments List */}
            <div className="py-3 px-4 border-b border-border bg-muted/30">
              <div className="flex items-center gap-2">
                <Hash className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm font-semibold text-foreground">
                  Son Gönderiler
                </span>
                {allShipments.length > 0 && (
                  <Badge variant="secondary" className="ml-auto text-xs">
                    {visibleShipments.length} / {allShipments.length} gönderi
                  </Badge>
                )}
              </div>
            </div>

            {visibleShipments.length > 0 ? (
              <>
                {visibleShipments.map((shipment, index) => (
                  <ShipmentRow
                    key={`${shipment.orderNumber}-${shipment.shipmentPackageId || index}`}
                    shipment={shipment}
                    formatCurrency={formatCurrency}
                  />
                ))}

                {/* Load More Button */}
                {hasMoreShipments && (
                  <div className="p-4 border-b border-border">
                    <Button
                      variant="outline"
                      className="w-full gap-2 text-amber-600 border-amber-300 hover:bg-amber-50 hover:border-amber-400"
                      onClick={handleLoadMore}
                    >
                      <ChevronDown className="h-4 w-4" />
                      Daha fazla göster ({remainingShipments} kalan)
                    </Button>
                  </div>
                )}
              </>
            ) : (
              <div className="p-6 text-center">
                <Package className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">
                  Gönderi detayı bulunamadı
                </p>
              </div>
            )}

            {/* View on Trendyol Button */}
            {(breakdownData?.productUrl || product.productUrl) && (
              <div className="p-4 border-t border-border">
                <Button
                  variant="outline"
                  className="w-full gap-2"
                  onClick={() => window.open(breakdownData?.productUrl || product.productUrl, "_blank")}
                >
                  <ExternalLink className="h-4 w-4" />
                  Trendyol'da Görüntüle
                </Button>
              </div>
            )}
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
