"use client";

import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import {
  ChevronRight,
  ChevronDown,
  FileText,
  Receipt,
  Truck,
  Globe,
  AlertTriangle,
  Megaphone,
  RefreshCcw,
  Package,
  Calendar,
  Hash,
  MapPin,
  BoxesIcon,
  ListIcon,
  Copy,
  Printer,
  X,
  Loader2,
} from "lucide-react";
import { toast } from "sonner";
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
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrency } from "@/lib/contexts/currency-context";
import type { InvoiceDetail, CargoInvoiceItem, CommissionInvoiceItem, InvoiceItem, AggregatedInvoiceProduct } from "@/types/invoice";
import { getCategoryDisplayName, isKargoType, isKomisyonType } from "@/types/invoice";
import {
  useCargoInvoiceItemsInfinite,
  useInvoiceItemsInfinite,
  useCommissionInvoiceItemsInfinite,
} from "@/hooks/queries/use-invoices";
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll";

// Category icons
const categoryIcons: Record<string, React.ReactNode> = {
  KOMISYON: <Receipt className="h-5 w-5" />,
  KARGO: <Truck className="h-5 w-5" />,
  ULUSLARARASI: <Globe className="h-5 w-5" />,
  CEZA: <AlertTriangle className="h-5 w-5" />,
  REKLAM: <Megaphone className="h-5 w-5" />,
  IADE: <RefreshCcw className="h-5 w-5" />,
  DIGER: <FileText className="h-5 w-5" />,
};

// Category colors
const categoryColors: Record<string, { bg: string; text: string; border: string }> = {
  KOMISYON: { bg: "bg-blue-500", text: "text-blue-600", border: "border-blue-500" },
  KARGO: { bg: "bg-amber-500", text: "text-amber-600", border: "border-amber-500" },
  ULUSLARARASI: { bg: "bg-purple-500", text: "text-purple-600", border: "border-purple-500" },
  CEZA: { bg: "bg-red-500", text: "text-red-600", border: "border-red-500" },
  REKLAM: { bg: "bg-pink-500", text: "text-pink-600", border: "border-pink-500" },
  IADE: { bg: "bg-green-500", text: "text-green-600", border: "border-green-500" },
  DIGER: { bg: "bg-gray-500", text: "text-gray-600", border: "border-gray-500" },
};

interface InvoiceDetailPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  invoice: InvoiceDetail | null;
  storeId: string | undefined;
}

function formatDate(dateStr: string) {
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString("tr-TR", {
      day: "2-digit",
      month: "long",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
}

// Row component for consistent styling
function DetailRow({
  label,
  value,
  icon,
  isNegative,
  isBold,
}: {
  label: string;
  value: string | React.ReactNode;
  icon?: React.ReactNode;
  isNegative?: boolean;
  isBold?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-2.5 px-4 border-b border-border">
      <div className="flex items-center gap-2">
        {icon && <span className="text-muted-foreground">{icon}</span>}
        <span className={cn("text-sm text-muted-foreground", isBold && "font-semibold text-foreground")}>
          {label}
        </span>
      </div>
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

// Expandable row for items
function ExpandableItemsSection({
  title,
  count,
  children,
  defaultOpen = false,
}: {
  title: string;
  count: number;
  children: React.ReactNode;
  defaultOpen?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger className="w-full">
        <div className="flex items-center justify-between py-3 px-4 border-b border-border hover:bg-muted/50 cursor-pointer">
          <div className="flex items-center gap-2">
            {isOpen ? (
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            ) : (
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            )}
            <span className="text-sm font-medium text-foreground">{title}</span>
          </div>
          <Badge variant="secondary" className="text-xs">
            {count} kayıt
          </Badge>
        </div>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="bg-muted/30">{children}</div>
      </CollapsibleContent>
    </Collapsible>
  );
}

// Cargo item row
function CargoItemRow({ item, formatCurrency }: { item: CargoInvoiceItem; formatCurrency: (n: number) => string }) {
  return (
    <div className="flex items-center justify-between py-2.5 px-4 pl-10 border-b border-border text-xs">
      <div className="flex-1 min-w-0">
        <p className="font-medium text-foreground truncate">
          {item.productName || `Sipariş: ${item.orderNumber}`}
        </p>
        <p className="text-muted-foreground">
          {item.barcode && `${item.barcode} · `}
          {item.desi && `${item.desi} desi`}
        </p>
      </div>
      <div className="text-right">
        <p className="font-medium text-red-600">{formatCurrency(-item.amount)}</p>
        {item.vatAmount && (
          <p className="text-muted-foreground">KDV: {formatCurrency(item.vatAmount)}</p>
        )}
      </div>
    </div>
  );
}

// Commission item row - extended with all Trendyol Excel fields
function CommissionItemRow({ item, formatCurrency }: { item: CommissionInvoiceItem; formatCurrency: (n: number) => string }) {
  // Format date helper
  const formatItemDate = (dateStr?: string) => {
    if (!dateStr) return "-";
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString("tr-TR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="py-3 px-4 pl-8 border-b border-border text-xs hover:bg-muted/30">
      {/* Row 1: Product/Order info */}
      <div className="flex items-center justify-between mb-2">
        <div className="flex-1 min-w-0">
          <p className="font-medium text-foreground truncate">
            {item.productName || `Sipariş: ${item.orderNumber}`}
          </p>
          <p className="text-muted-foreground">
            {item.barcode && <span>{item.barcode}</span>}
            {item.recordId && <span> · Kayıt No: {item.recordId}</span>}
          </p>
        </div>
        <div className="text-right flex-shrink-0 ml-2">
          <p className="font-semibold text-red-600">{formatCurrency(-(item.commissionAmount || 0))}</p>
          {item.totalAmount && (
            <p className="text-muted-foreground">Toplam: {formatCurrency(item.totalAmount)}</p>
          )}
        </div>
      </div>

      {/* Row 2: Commission details */}
      <div className="flex items-center gap-4 text-muted-foreground">
        {item.commissionRate && (
          <span className="inline-flex items-center gap-1">
            <span className="text-foreground font-medium">%{item.commissionRate}</span> komisyon
          </span>
        )}
        {item.transactionType && (
          <span className="inline-flex items-center gap-1">
            <span className="px-1.5 py-0.5 bg-muted rounded text-xs">{item.transactionType}</span>
          </span>
        )}
      </div>

      {/* Row 3: Date and payment info */}
      <div className="flex items-center gap-4 mt-1.5 text-muted-foreground">
        {item.orderDate && (
          <span className="inline-flex items-center gap-1">
            <Calendar className="h-3 w-3" /> Sipariş: {formatItemDate(item.orderDate)}
          </span>
        )}
        {item.transactionDate && (
          <span className="inline-flex items-center gap-1">
            İşlem: {formatItemDate(item.transactionDate)}
          </span>
        )}
        {item.paymentPeriod && (
          <span className="inline-flex items-center gap-1">
            Vade: {item.paymentPeriod} gün
          </span>
        )}
        {item.paymentDate && (
          <span className="inline-flex items-center gap-1">
            Ödeme: {formatItemDate(item.paymentDate)}
          </span>
        )}
      </div>

      {/* Row 4: Revenue breakdown (if available) */}
      {(item.sellerRevenue || item.trendyolRevenue) && (
        <div className="flex items-center gap-4 mt-1.5 text-muted-foreground">
          {item.sellerRevenue && (
            <span className="inline-flex items-center gap-1">
              Satıcı Hakediş: <span className="text-green-600 font-medium">{formatCurrency(item.sellerRevenue)}</span>
            </span>
          )}
          {item.trendyolRevenue && (
            <span className="inline-flex items-center gap-1">
              Trendyol Hakediş: <span className="text-foreground font-medium">{formatCurrency(item.trendyolRevenue)}</span>
            </span>
          )}
        </div>
      )}
    </div>
  );
}

// Generic invoice item row
function InvoiceItemRow({ item, formatCurrency }: { item: InvoiceItem; formatCurrency: (n: number) => string }) {
  return (
    <div className="flex items-center justify-between py-2.5 px-4 pl-10 border-b border-border text-xs">
      <div className="flex-1 min-w-0">
        <p className="font-medium text-foreground">
          {item.transactionType || item.description || "-"}
        </p>
      </div>
      <div className="text-right">
        <p className="font-medium text-red-600">{formatCurrency(-item.amount)}</p>
        {item.vatAmount != null && item.vatAmount > 0 && (
          <p className="text-muted-foreground">KDV: {formatCurrency(item.vatAmount)}</p>
        )}
      </div>
    </div>
  );
}

// Aggregated product row (for "Ürünler" tab)
function AggregatedProductRow({
  product,
  formatCurrency,
  isKargo,
}: {
  product: AggregatedInvoiceProduct;
  formatCurrency: (n: number) => string;
  isKargo: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-3 px-4 border-b border-border text-xs hover:bg-muted/50">
      <div className="flex items-center gap-3 flex-1 min-w-0">
        {/* Product Image */}
        <div className="w-10 h-10 rounded bg-muted flex items-center justify-center flex-shrink-0 overflow-hidden">
          {product.productImage ? (
            <img
              src={product.productImage}
              alt={product.productName || "Ürün"}
              className="w-full h-full object-cover"
            />
          ) : (
            <Package className="h-5 w-5 text-muted-foreground" />
          )}
        </div>
        <div className="min-w-0">
          <p className="font-medium text-foreground truncate">
            {product.productName || "Bilinmeyen Ürün"}
          </p>
          <p className="text-muted-foreground">
            {product.barcode}
            {isKargo && product.totalDesi && ` · ${product.totalDesi.toFixed(1)} desi`}
          </p>
        </div>
      </div>
      <div className="text-right flex-shrink-0">
        <p className="font-semibold text-red-600">{formatCurrency(-product.totalAmount)}</p>
        <p className="text-muted-foreground">{product.itemCount} kalem</p>
      </div>
    </div>
  );
}

// Items loader skeleton
function ItemsLoadingSkeleton() {
  return (
    <div className="space-y-2 p-4 pl-10">
      {[...Array(3)].map((_, i) => (
        <div key={i} className="flex items-center justify-between">
          <div className="space-y-1">
            <Skeleton className="h-4 w-40" />
            <Skeleton className="h-3 w-24" />
          </div>
          <Skeleton className="h-4 w-16" />
        </div>
      ))}
    </div>
  );
}

export function InvoiceDetailPanel({
  open,
  onOpenChange,
  invoice,
  storeId,
}: InvoiceDetailPanelProps) {
  const { formatCurrency } = useCurrency();
  const [activeTab, setActiveTab] = useState<"items" | "products">("items");

  // Fetch invoice items based on type
  const isKargo = invoice ? isKargoType(invoice.invoiceTypeCode) : false;
  const isKomisyon = invoice ? isKomisyonType(invoice.invoiceTypeCode) : false;

  // Infinite query hooks for lazy loading
  const {
    data: cargoData,
    isLoading: cargoLoading,
    hasNextPage: cargoHasNextPage,
    isFetchingNextPage: cargoFetchingNext,
    fetchNextPage: cargoFetchNextPage,
  } = useCargoInvoiceItemsInfinite(
    storeId,
    invoice?.invoiceNumber,
    20,
    open && isKargo
  );

  const {
    data: commissionData,
    isLoading: commissionLoading,
    hasNextPage: commissionHasNextPage,
    isFetchingNextPage: commissionFetchingNext,
    fetchNextPage: commissionFetchNextPage,
  } = useCommissionInvoiceItemsInfinite(
    storeId,
    invoice?.invoiceNumber,
    20,
    open && isKomisyon
  );

  const {
    data: genericData,
    isLoading: genericLoading,
    hasNextPage: genericHasNextPage,
    isFetchingNextPage: genericFetchingNext,
    fetchNextPage: genericFetchNextPage,
  } = useInvoiceItemsInfinite(
    storeId,
    invoice?.invoiceNumber,
    20,
    open && !isKargo && !isKomisyon
  );

  // Infinite scroll refs
  const cargoSentinelRef = useInfiniteScroll({
    hasNextPage: cargoHasNextPage,
    isFetchingNextPage: cargoFetchingNext,
    fetchNextPage: cargoFetchNextPage,
  });

  const commissionSentinelRef = useInfiniteScroll({
    hasNextPage: commissionHasNextPage,
    isFetchingNextPage: commissionFetchingNext,
    fetchNextPage: commissionFetchNextPage,
  });

  const genericSentinelRef = useInfiniteScroll({
    hasNextPage: genericHasNextPage,
    isFetchingNextPage: genericFetchingNext,
    fetchNextPage: genericFetchNextPage,
  });

  // Flatten pages into single arrays
  const cargoItems = useMemo(
    () => cargoData?.pages.flatMap((page) => page.content) ?? [],
    [cargoData]
  );

  const commissionItems = useMemo(
    () => commissionData?.pages.flatMap((page) => page.content) ?? [],
    [commissionData]
  );

  const genericItems = useMemo(
    () => genericData?.pages.flatMap((page) => page.content) ?? [],
    [genericData]
  );

  // Get total counts from first page
  const cargoTotalCount = cargoData?.pages[0]?.totalElements ?? 0;
  const commissionTotalCount = commissionData?.pages[0]?.totalElements ?? 0;
  const genericTotalCount = genericData?.pages[0]?.totalElements ?? 0;

  // Aggregate cargo items by barcode (SKU)
  const aggregatedCargoProducts = useMemo<AggregatedInvoiceProduct[]>(() => {
    if (!cargoItems?.length) return [];

    const map = new Map<string, AggregatedInvoiceProduct>();

    cargoItems.forEach((item) => {
      const key = item.barcode || "unknown";
      const existing = map.get(key);

      if (existing) {
        existing.itemCount++;
        existing.totalAmount += item.amount;
        existing.totalDesi = (existing.totalDesi || 0) + (item.desi || 0);
        // Keep first non-null image
        if (!existing.productImage && item.productImageUrl) {
          existing.productImage = item.productImageUrl;
        }
      } else {
        map.set(key, {
          barcode: item.barcode || "Barkod Yok",
          productName: item.productName || "Bilinmeyen Ürün",
          productImage: item.productImageUrl,
          itemCount: 1,
          totalAmount: item.amount,
          totalDesi: item.desi || 0,
        });
      }
    });

    return Array.from(map.values()).sort(
      (a, b) => Math.abs(b.totalAmount) - Math.abs(a.totalAmount)
    );
  }, [cargoItems]);

  // Aggregate commission items by barcode (SKU)
  const aggregatedCommissionProducts = useMemo<AggregatedInvoiceProduct[]>(() => {
    if (!commissionItems?.length) return [];

    const map = new Map<string, AggregatedInvoiceProduct>();

    commissionItems.forEach((item) => {
      const key = item.barcode || "unknown";
      const existing = map.get(key);

      if (existing) {
        existing.itemCount++;
        existing.totalAmount += item.commissionAmount || 0;
        existing.totalCommission = (existing.totalCommission || 0) + (item.commissionAmount || 0);
      } else {
        map.set(key, {
          barcode: item.barcode || "Barkod Yok",
          productName: item.productName || "Bilinmeyen Ürün",
          itemCount: 1,
          totalAmount: item.commissionAmount || 0,
          totalCommission: item.commissionAmount || 0,
        });
      }
    });

    return Array.from(map.values()).sort(
      (a, b) => Math.abs(b.totalAmount) - Math.abs(a.totalAmount)
    );
  }, [commissionItems]);

  // Determine if tabs should be shown (only for KARGO and KOMISYON with items)
  const showTabs = (isKargo && cargoItems.length > 0) || (isKomisyon && commissionItems.length > 0);
  const aggregatedProducts = isKargo ? aggregatedCargoProducts : aggregatedCommissionProducts;

  if (!invoice) return null;

  const colors = categoryColors[invoice.invoiceCategory] || categoryColors.DIGER;
  const icon = categoryIcons[invoice.invoiceCategory] || categoryIcons.DIGER;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[420px] sm:max-w-[420px] p-0 overflow-y-auto">
        <SheetTitle className="sr-only">Fatura Detayları - {invoice.invoiceNumber}</SheetTitle>

        {/* Header with category color */}
        <div className={cn("sticky top-0 z-10", colors.bg)}>
          <div className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3 text-white">
                {icon}
                <div>
                  <p className="text-sm font-medium opacity-90">
                    {getCategoryDisplayName(invoice.invoiceCategory)}
                  </p>
                  <p className="text-lg font-bold">{invoice.invoiceType}</p>
                </div>
              </div>
              {/* Action Buttons */}
              <div className="flex items-center gap-1">
                <button
                  onClick={() => {
                    const text = invoice.invoiceNumber || invoice.invoiceTypeCode || "";
                    navigator.clipboard.writeText(text);
                    toast.success("Fatura numarasi kopyalandi");
                  }}
                  className="p-2 rounded-md hover:bg-white/20 transition-colors text-white"
                  title="Fatura numarasini kopyala"
                >
                  <Copy className="h-4 w-4" />
                </button>
                <button
                  onClick={() => window.print()}
                  className="p-2 rounded-md hover:bg-white/20 transition-colors text-white"
                  title="Yazdir"
                >
                  <Printer className="h-4 w-4" />
                </button>
                <button
                  onClick={() => onOpenChange(false)}
                  className="p-2 rounded-md hover:bg-white/20 transition-colors text-white"
                  title="Kapat"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Invoice Details */}
        <div className="divide-y divide-border">
          {/* Fatura No */}
          <DetailRow
            label="Fatura No"
            value={invoice.invoiceNumber}
            icon={<Hash className="h-4 w-4" />}
            isBold
          />

          {/* Tarih */}
          <DetailRow
            label="Fatura Tarihi"
            value={formatDate(invoice.invoiceDate)}
            icon={<Calendar className="h-4 w-4" />}
          />

          {/* Sipariş No */}
          {invoice.orderNumber && (
            <DetailRow
              label="Sipariş No"
              value={invoice.orderNumber}
              icon={<Package className="h-4 w-4" />}
            />
          )}

          {/* Divider */}
          <div className="h-2 bg-muted" />

          {/* Tutar Bilgileri */}
          <DetailRow
            label="KDV Matrahı"
            value={formatCurrency(invoice.baseAmount)}
          />

          <DetailRow
            label="KDV Tutarı"
            value={formatCurrency(invoice.vatAmount)}
          />

          <DetailRow
            label="KDV Oranı"
            value={`%${invoice.vatRate}`}
          />

          {/* Ana Tutar - Vurgulu */}
          <div className={cn("bg-primary/5 border-y-2", colors.border, "border-opacity-30")}>
            <DetailRow
              label="Toplam Tutar"
              value={formatCurrency(invoice.amount)}
              isBold
              isNegative={invoice.isDeduction}
            />
          </div>

          {/* Description */}
          {invoice.description && (
            <>
              <div className="h-2 bg-muted" />
              <div className="p-4">
                <p className="text-xs text-muted-foreground mb-1">Açıklama</p>
                <p className="text-sm text-foreground">{invoice.description}</p>
              </div>
            </>
          )}

          {/* Items Section */}
          {(isKargo || isKomisyon || (!isKargo && !isKomisyon)) && (
            <>
              <div className="h-2 bg-muted" />

              {/* Tab Navigation for KARGO and KOMISYON */}
              {showTabs && (
                <div className="flex border-b border-border">
                  <button
                    onClick={() => setActiveTab("items")}
                    className={cn(
                      "flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors",
                      activeTab === "items"
                        ? "border-b-2 border-primary text-primary"
                        : "text-muted-foreground hover:text-foreground"
                    )}
                  >
                    <ListIcon className="h-4 w-4" />
                    Fatura Kalemleri
                    <Badge variant="secondary" className="ml-1 text-xs">
                      {isKargo ? cargoTotalCount : commissionTotalCount}
                    </Badge>
                  </button>
                  <button
                    onClick={() => setActiveTab("products")}
                    className={cn(
                      "flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors",
                      activeTab === "products"
                        ? "border-b-2 border-primary text-primary"
                        : "text-muted-foreground hover:text-foreground"
                    )}
                  >
                    <BoxesIcon className="h-4 w-4" />
                    Ürünler
                    <Badge variant="secondary" className="ml-1 text-xs">
                      {aggregatedProducts.length}
                    </Badge>
                  </button>
                </div>
              )}

              {/* Tab Content for KARGO */}
              {isKargo && (
                <>
                  {activeTab === "items" && (
                    <div className="divide-y divide-border">
                      {cargoLoading ? (
                        <ItemsLoadingSkeleton />
                      ) : cargoItems.length > 0 ? (
                        <>
                          {cargoItems.map((item, idx) => (
                            <CargoItemRow key={item.id || idx} item={item} formatCurrency={formatCurrency} />
                          ))}
                          {/* Infinite scroll sentinel */}
                          <div ref={cargoSentinelRef} />
                          {cargoFetchingNext && (
                            <div className="flex items-center justify-center py-3">
                              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                            </div>
                          )}
                        </>
                      ) : (
                        <div className="p-4 text-center text-sm text-muted-foreground">
                          Kalem bulunamadı
                        </div>
                      )}
                    </div>
                  )}
                  {activeTab === "products" && (
                    <div className="divide-y divide-border">
                      {cargoLoading ? (
                        <ItemsLoadingSkeleton />
                      ) : aggregatedCargoProducts.length ? (
                        aggregatedCargoProducts.map((product) => (
                          <AggregatedProductRow
                            key={product.barcode}
                            product={product}
                            formatCurrency={formatCurrency}
                            isKargo={true}
                          />
                        ))
                      ) : (
                        <div className="p-4 text-center text-sm text-muted-foreground">
                          Ürün bulunamadı
                        </div>
                      )}
                    </div>
                  )}
                </>
              )}

              {/* Tab Content for KOMISYON */}
              {isKomisyon && (
                <>
                  {activeTab === "items" && (
                    <div className="divide-y divide-border">
                      {commissionLoading ? (
                        <ItemsLoadingSkeleton />
                      ) : commissionItems.length > 0 ? (
                        <>
                          {commissionItems.map((item, idx) => (
                            <CommissionItemRow key={idx} item={item} formatCurrency={formatCurrency} />
                          ))}
                          {/* Infinite scroll sentinel */}
                          <div ref={commissionSentinelRef} />
                          {commissionFetchingNext && (
                            <div className="flex items-center justify-center py-3">
                              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                            </div>
                          )}
                        </>
                      ) : (
                        <div className="p-4 text-center text-sm text-muted-foreground">
                          Kalem bulunamadı
                        </div>
                      )}
                    </div>
                  )}
                  {activeTab === "products" && (
                    <div className="divide-y divide-border">
                      {commissionLoading ? (
                        <ItemsLoadingSkeleton />
                      ) : aggregatedCommissionProducts.length ? (
                        aggregatedCommissionProducts.map((product) => (
                          <AggregatedProductRow
                            key={product.barcode}
                            product={product}
                            formatCurrency={formatCurrency}
                            isKargo={false}
                          />
                        ))
                      ) : (
                        <div className="p-4 text-center text-sm text-muted-foreground">
                          Ürün bulunamadı
                        </div>
                      )}
                    </div>
                  )}
                </>
              )}

              {/* Generic Items (for non-kargo, non-komisyon types) */}
              {!isKargo && !isKomisyon && (genericLoading || genericItems.length > 0) && (
                <ExpandableItemsSection
                  title="Fatura Kalemleri"
                  count={genericTotalCount}
                  defaultOpen
                >
                  {genericLoading ? (
                    <ItemsLoadingSkeleton />
                  ) : (
                    <>
                      {genericItems.map((item, idx) => (
                        <InvoiceItemRow key={item.id || idx} item={item} formatCurrency={formatCurrency} />
                      ))}
                      {/* Infinite scroll sentinel */}
                      <div ref={genericSentinelRef} />
                      {genericFetchingNext && (
                        <div className="flex items-center justify-center py-3">
                          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                        </div>
                      )}
                    </>
                  )}
                </ExpandableItemsSection>
              )}
            </>
          )}

          {/* Product Info (if available) */}
          {(invoice.barcode || invoice.productName) && (
            <>
              <div className="h-2 bg-muted" />
              <div className="p-4">
                <p className="text-xs text-muted-foreground mb-2">Ürün Bilgisi</p>
                {invoice.productName && (
                  <p className="text-sm font-medium text-foreground">{invoice.productName}</p>
                )}
                {invoice.barcode && (
                  <p className="text-xs text-muted-foreground mt-1">Barkod: {invoice.barcode}</p>
                )}
                {invoice.desi && (
                  <p className="text-xs text-muted-foreground">Desi: {invoice.desi}</p>
                )}
              </div>
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
