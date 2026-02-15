"use client";

import { useState, useMemo, useEffect } from "react";
import { FadeIn } from "@/components/motion";
import { cn } from "@/lib/utils";
import { ChevronRight, ChevronDown, Package, ShoppingCart, Calendar, Loader2, FileText, AlertCircle, X } from "lucide-react";
import { Button } from "@/components/ui/button";
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
import { useCurrency } from "@/lib/contexts/currency-context";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useOrderInvoiceItems } from "@/hooks/queries/use-invoices";
import type { OrderDetailPanelData, OrderProductDetail } from "@/types/dashboard";

interface OrderDetailPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  order: OrderDetailPanelData | null;
}

function formatPercentage(value: number): string {
  return `${value.toFixed(2)}%`;
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("tr-TR", {
    day: "2-digit",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatShortDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("tr-TR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

// Açılabilir satır bileşeni
function ExpandableRow({
  label,
  value,
  isNegative,
  isBold,
  children,
  defaultOpen = false,
}: {
  label: string;
  value: string;
  isNegative?: boolean;
  isBold?: boolean;
  children?: React.ReactNode;
  defaultOpen?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const hasChildren = !!children;

  if (!hasChildren) {
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

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger className="w-full">
        <div className="flex items-center justify-between py-2.5 px-4 border-b border-border hover:bg-muted/50 cursor-pointer">
          <div className="flex items-center gap-2">
            {isOpen ? (
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            ) : (
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            )}
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
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="bg-muted/50">{children}</div>
      </CollapsibleContent>
    </Collapsible>
  );
}

// Alt satır (expanded içerik)
function SubRow({
  label,
  value,
  isNegative,
}: {
  label: string;
  value: string;
  isNegative?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-2 px-4 pl-10 border-b border-border">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className={cn("text-sm font-medium", isNegative ? "text-red-600" : "text-foreground")}>
        {value}
      </span>
    </div>
  );
}

// Ürün satırı bileşeni
function ProductRow({ product, formatCurrency }: { product: OrderProductDetail; formatCurrency: (value: number) => string }) {
  return (
    <div className="flex items-center justify-between py-2 px-4 pl-10 border-b border-border">
      <div className="flex-1 min-w-0 pr-4">
        <p className="text-sm text-foreground truncate">{product.productName}</p>
        <p className="text-xs text-muted-foreground">{product.barcode}</p>
      </div>
      <div className="flex items-center gap-4 text-sm">
        <span className="text-muted-foreground">{product.quantity} ad</span>
        <span className="font-medium">{formatCurrency(product.totalPrice)}</span>
      </div>
    </div>
  );
}

const PRODUCTS_PAGE_SIZE = 20;

export function OrderDetailPanel({
  open,
  onOpenChange,
  order,
}: OrderDetailPanelProps) {
  const { formatCurrency } = useCurrency();
  const { data: selectedStoreData } = useSelectedStore();
  const storeId = selectedStoreData?.selectedStoreId;
  const [productsLimit, setProductsLimit] = useState(PRODUCTS_PAGE_SIZE);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  // Fetch invoice items for this order
  const {
    data: invoiceItems,
    isLoading: isLoadingInvoice,
    error: invoiceError,
  } = useOrderInvoiceItems(
    storeId ?? undefined,
    order?.orderNumber,
    open && !!order?.orderNumber && !!storeId // Only fetch when panel is open
  );

  // Frontend slice pagination for products
  const productsData = useMemo(() => {
    if (!order?.products) return { visible: [], total: 0, hasMore: false };
    const total = order.products.length;
    const visible = order.products.slice(0, productsLimit);
    return {
      visible,
      total,
      hasMore: productsLimit < total,
    };
  }, [order?.products, productsLimit]);

  const handleLoadMoreProducts = () => {
    setIsLoadingMore(true);
    setTimeout(() => {
      setProductsLimit((prev) => prev + PRODUCTS_PAGE_SIZE);
      setIsLoadingMore(false);
    }, 150);
  };

  // Reset pagination when order changes
  useEffect(() => {
    setProductsLimit(PRODUCTS_PAGE_SIZE);
  }, [order?.orderNumber]);

  if (!order) return null;

  // Toplam ürün adedi
  const totalQuantity = order.products.reduce((sum, p) => sum + p.quantity, 0);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[420px] sm:max-w-[420px] p-0 overflow-y-auto">
        {/* Accessibility: Visually hidden title for screen readers */}
        <SheetTitle className="sr-only">Sipariş Detayları - {order.orderNumber}</SheetTitle>

        {/* Header */}
        <div className="sticky top-0 bg-card z-10 border-b border-border">
          <div className="flex items-start gap-3 p-4 relative">
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center flex-shrink-0">
              <ShoppingCart className="h-6 w-6 text-primary" />
            </div>
            <div className="flex-1 min-w-0 pr-8">
              <p className="text-xs text-muted-foreground flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {formatDate(order.orderDate)}
              </p>
              <p className="text-base font-semibold text-foreground mt-0.5">
                #{order.orderNumber}
              </p>
              <div className="flex items-center gap-2 mt-1.5">
                <span className="inline-flex items-center gap-1 text-xs bg-muted px-2 py-0.5 rounded">
                  <Package className="h-3 w-3" />
                  {totalQuantity} ürün
                </span>
                <span className="text-xs font-medium text-primary">
                  {formatCurrency(order.totalPrice)}
                </span>
              </div>
            </div>
            {/* Kapatma Butonu */}
            <button
              onClick={() => onOpenChange(false)}
              className="absolute right-4 top-4 p-1.5 rounded-md hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
              title="Kapat"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Content */}
        <FadeIn delay={0.1}>
        <div className="divide-y divide-border">
          {/* ========== ÜRÜNLER ========== */}
          <ExpandableRow
            label="Ürünler"
            value={productsData.hasMore
              ? `${productsData.visible.length} / ${productsData.total} kalem`
              : `${productsData.total} kalem`}
            isBold
            defaultOpen={true}
          >
            {productsData.visible.map((product, index) => (
              <ProductRow key={index} product={product} formatCurrency={formatCurrency} />
            ))}
            {/* Load More Button */}
            {productsData.hasMore && (
              <div className="flex justify-center py-3 px-4 pl-10 border-b border-border">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleLoadMoreProducts}
                  disabled={isLoadingMore}
                  className="text-xs h-7"
                >
                  {isLoadingMore ? (
                    <>
                      <Loader2 className="h-3 w-3 animate-spin mr-1" />
                      Yükleniyor...
                    </>
                  ) : (
                    `Daha Fazla Yükle (${productsData.total - productsData.visible.length} kalem)`
                  )}
                </Button>
              </div>
            )}
          </ExpandableRow>

          {/* Divider */}
          <div className="h-2 bg-muted" />

          {/* ========== FİYATLANDIRMA ========== */}
          <ExpandableRow
            label="Brüt Satış"
            value={formatCurrency(order.totalPrice)}
            isBold
          />

          {order.returnPrice > 0 && (
            <ExpandableRow
              label="İade Tutarı"
              value={formatCurrency(-order.returnPrice)}
              isNegative
            />
          )}

          <ExpandableRow
            label="Net Ciro"
            value={formatCurrency(order.revenue)}
            isBold
          />

          {/* Divider */}
          <div className="h-2 bg-muted" />

          {/* ========== MALİYETLER ========== */}
          {(() => {
            // Fatura komisyon verisi var mı kontrol et
            const hasActualCommission = invoiceItems?.hasInvoiceData &&
              invoiceItems?.commissionItems &&
              invoiceItems.commissionItems.length > 0;

            // Fatura kargo verisi var mı kontrol et
            const hasActualCargo = invoiceItems?.hasInvoiceData &&
              invoiceItems?.cargoItems &&
              invoiceItems.cargoItems.length > 0;

            // Gösterilecek değerler
            const commissionValue = hasActualCommission
              ? invoiceItems.totalCommissionAmount
              : order.estimatedCommission;

            const cargoValue = hasActualCargo
              ? invoiceItems.totalCargoAmount
              : order.estimatedShippingCost;

            return (
              <ExpandableRow
                label="Maliyetler"
                value={formatCurrency(-(order.totalProductCost + commissionValue + cargoValue + order.stoppage))}
                isNegative
              >
                <SubRow
                  label="Ürün Maliyeti"
                  value={formatCurrency(-order.totalProductCost)}
                  isNegative={order.totalProductCost > 0}
                />
                {/* Komisyon - Dinamik label */}
                <div className="flex items-center justify-between py-2 px-4 pl-10 border-b border-border">
                  <span className="text-sm text-muted-foreground flex items-center gap-1.5">
                    {hasActualCommission ? (
                      <>
                        <span className="w-2 h-2 rounded-full bg-green-500" />
                        Komisyon
                      </>
                    ) : (
                      <>
                        <span className="px-1.5 py-0.5 text-xs bg-amber-100 text-amber-700 rounded">
                          Tahmini
                        </span>
                        Komisyon
                      </>
                    )}
                  </span>
                  <span className={cn("text-sm font-medium", commissionValue > 0 ? "text-red-600" : "text-foreground")}>
                    {formatCurrency(-commissionValue)}
                  </span>
                </div>
                {/* Kargo - Dinamik label */}
                <div className="flex items-center justify-between py-2 px-4 pl-10 border-b border-border">
                  <span className="text-sm text-muted-foreground flex items-center gap-1.5">
                    {hasActualCargo ? (
                      <>
                        <span className="w-2 h-2 rounded-full bg-green-500" />
                        Kargo Maliyeti
                      </>
                    ) : (
                      <>
                        <span className="px-1.5 py-0.5 text-xs bg-amber-100 text-amber-700 rounded">
                          Tahmini
                        </span>
                        Kargo Maliyeti
                      </>
                    )}
                  </span>
                  <span className={cn("text-sm font-medium", cargoValue > 0 ? "text-red-600" : "text-foreground")}>
                    {formatCurrency(-cargoValue)}
                  </span>
                </div>
                <SubRow
                  label="Stopaj"
                  value={formatCurrency(-order.stoppage)}
                  isNegative={order.stoppage > 0}
                />
              </ExpandableRow>
            );
          })()}

          {/* Divider */}
          <div className="h-2 bg-muted" />

          {/* ========== FATURA GİDERLERİ ========== */}
          {(() => {
            // Platform hizmet bedeli ve diğer kesintileri ayır
            const platformServiceItems = invoiceItems?.deductionItems?.filter(item => {
              const desc = (item.description || '').toLowerCase();
              const type = (item.transactionType || '').toLowerCase();
              return desc.includes('platform') || desc.includes('hizmet') ||
                     type.includes('platform') || type.includes('service');
            }) || [];

            const otherDeductionItems = invoiceItems?.deductionItems?.filter(item => {
              const desc = (item.description || '').toLowerCase();
              const type = (item.transactionType || '').toLowerCase();
              return !(desc.includes('platform') || desc.includes('hizmet') ||
                       type.includes('platform') || type.includes('service'));
            }) || [];

            const platformServiceTotal = platformServiceItems.reduce((sum, item) => sum + (item.amount || 0), 0);
            const otherDeductionTotal = otherDeductionItems.reduce((sum, item) => sum + (item.amount || 0), 0);

            // Toplam KDV hesapla
            const totalVat = (invoiceItems?.totalCargoVatAmount || 0) +
                            (invoiceItems?.totalCommissionVatAmount || 0) +
                            (invoiceItems?.totalDeductionVatAmount || 0);

            return (
              <ExpandableRow
                label="Fatura Giderleri"
                value={
                  isLoadingInvoice
                    ? "Yükleniyor..."
                    : invoiceItems?.hasInvoiceData
                      ? formatCurrency(-invoiceItems.grandTotal)
                      : "-"
                }
                isNegative={invoiceItems?.hasInvoiceData && invoiceItems.grandTotal > 0}
              >
                {isLoadingInvoice ? (
                  <div className="flex items-center justify-center py-4 px-4 pl-10 gap-2 text-muted-foreground">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span className="text-sm">Fatura verileri yükleniyor...</span>
                  </div>
                ) : invoiceError ? (
                  <div className="flex items-center gap-2 py-3 px-4 pl-10 text-red-600">
                    <AlertCircle className="h-4 w-4" />
                    <span className="text-sm">Fatura verileri yüklenemedi</span>
                  </div>
                ) : !invoiceItems?.hasInvoiceData ? (
                  <div className="py-3 px-4 pl-10">
                    <div className="flex items-start gap-2 text-muted-foreground">
                      <FileText className="h-4 w-4 mt-0.5 flex-shrink-0" />
                      <div>
                        <p className="text-sm">Fatura verisi bekleniyor...</p>
                        <p className="text-xs mt-0.5 text-muted-foreground/70">
                          Faturalar genellikle siparişten 1-2 ay sonra kesilir
                        </p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <>
                    {/* Kargo Faturaları */}
                    {invoiceItems.cargoItems && invoiceItems.cargoItems.length > 0 && (
                      <>
                        <SubRow
                          label={`Kargo Faturaları (${invoiceItems.cargoItems.length})`}
                          value={formatCurrency(-invoiceItems.totalCargoAmount)}
                          isNegative
                        />
                        {invoiceItems.cargoItems.map((item, index) => (
                          <div
                            key={`cargo-${index}`}
                            className="flex items-center justify-between py-1.5 px-4 pl-14 text-xs text-muted-foreground"
                          >
                            <span className="truncate pr-2">
                              {item.invoiceSerialNumber || `Gönderi #${index + 1}`}
                            </span>
                            <span className="text-red-600">
                              {formatCurrency(-item.amount)}
                            </span>
                          </div>
                        ))}
                        {/* Kargo KDV */}
                        {invoiceItems.totalCargoVatAmount > 0 && (
                          <div className="flex items-center justify-between py-1 px-4 pl-14 text-xs text-muted-foreground/70">
                            <span>KDV</span>
                            <span className="text-red-600/70">
                              {formatCurrency(-invoiceItems.totalCargoVatAmount)}
                            </span>
                          </div>
                        )}
                      </>
                    )}

                    {/* Komisyon İşlemleri */}
                    {invoiceItems.commissionItems && invoiceItems.commissionItems.length > 0 && (
                      <>
                        <SubRow
                          label={`Komisyon İşlemleri (${invoiceItems.commissionItems.length})`}
                          value={formatCurrency(-invoiceItems.totalCommissionAmount)}
                          isNegative
                        />
                        {invoiceItems.commissionItems.map((item, index) => {
                          const amount = item.commissionAmount ?? item.totalAmount ?? 0;
                          return (
                            <div
                              key={`comm-${index}`}
                              className="flex items-center justify-between py-1.5 px-4 pl-14 text-xs text-muted-foreground"
                            >
                              <span className="truncate pr-2">{item.transactionType || "Komisyon"}</span>
                              <span className={amount < 0 ? "text-green-600" : "text-red-600"}>
                                {formatCurrency(-amount)}
                              </span>
                            </div>
                          );
                        })}
                        {/* Komisyon KDV */}
                        {invoiceItems.totalCommissionVatAmount > 0 && (
                          <div className="flex items-center justify-between py-1 px-4 pl-14 text-xs text-muted-foreground/70">
                            <span>KDV</span>
                            <span className="text-red-600/70">
                              {formatCurrency(-invoiceItems.totalCommissionVatAmount)}
                            </span>
                          </div>
                        )}
                      </>
                    )}

                    {/* Platform Hizmet Bedeli */}
                    {platformServiceItems.length > 0 && (
                      <>
                        <SubRow
                          label={`Platform Hizmet Bedeli (${platformServiceItems.length})`}
                          value={formatCurrency(-platformServiceTotal)}
                          isNegative
                        />
                        {platformServiceItems.map((item, index) => (
                          <div
                            key={`plat-${index}`}
                            className="flex items-center justify-between py-1.5 px-4 pl-14 text-xs text-muted-foreground"
                          >
                            <span className="truncate pr-2">{item.description || item.transactionType || "Platform Hizmet"}</span>
                            <span className="text-red-600">
                              {formatCurrency(-item.amount)}
                            </span>
                          </div>
                        ))}
                      </>
                    )}

                    {/* Diğer Kesintiler */}
                    {otherDeductionItems.length > 0 && (
                      <>
                        <SubRow
                          label={`Diğer Kesintiler (${otherDeductionItems.length})`}
                          value={formatCurrency(-otherDeductionTotal)}
                          isNegative
                        />
                        {otherDeductionItems.map((item, index) => (
                          <div
                            key={`ded-${index}`}
                            className="flex items-center justify-between py-1.5 px-4 pl-14 text-xs text-muted-foreground"
                          >
                            <span className="truncate pr-2">{item.description || item.transactionType || "Kesinti"}</span>
                            <span className="text-red-600">
                              {formatCurrency(-item.amount)}
                            </span>
                          </div>
                        ))}
                      </>
                    )}

                    {/* Toplam KDV */}
                    {totalVat > 0 && (
                      <div className="flex items-center justify-between py-2 px-4 pl-10 border-t border-border/50 text-xs">
                        <span className="text-muted-foreground">Toplam KDV (Dahil)</span>
                        <span className="text-red-600/80 font-medium">
                          {formatCurrency(-totalVat)}
                        </span>
                      </div>
                    )}

                    {/* Toplam Fatura Gideri */}
                    {((invoiceItems.cargoItems?.length || 0) > 0 ? 1 : 0) +
                      ((invoiceItems.commissionItems?.length || 0) > 0 ? 1 : 0) +
                      (platformServiceItems.length > 0 ? 1 : 0) +
                      (otherDeductionItems.length > 0 ? 1 : 0) > 1 && (
                      <div className="flex items-center justify-between py-2 px-4 pl-10 border-t border-border bg-muted/30">
                        <span className="text-sm font-medium text-foreground">Toplam Fatura Gideri</span>
                        <span className="text-sm font-semibold text-red-600">
                          {formatCurrency(-invoiceItems.grandTotal)}
                        </span>
                      </div>
                    )}
                  </>
                )}
              </ExpandableRow>
            );
          })()}

          {/* Divider */}
          <div className="h-2 bg-muted" />

          {/* ========== KÂR METRİKLERİ ========== */}
          <ExpandableRow
            label="Brüt Kâr"
            value={formatCurrency(order.grossProfit)}
            isNegative={order.grossProfit < 0}
          />

          {/* Net Kâr - Vurgulu */}
          <div className="bg-primary/5 border-y-2 border-primary/20">
            <ExpandableRow
              label="Net Kâr"
              value={formatCurrency(order.netProfit)}
              isBold
              isNegative={order.netProfit < 0}
            />
          </div>

          {/* Divider */}
          <div className="h-2 bg-muted" />

          {/* ========== ORANLAR ========== */}
          <ExpandableRow
            label="Kar Marjı"
            value={formatPercentage(order.profitMargin)}
            isBold
            isNegative={order.profitMargin < 0}
          />

          <ExpandableRow
            label="ROI"
            value={formatPercentage(order.roi)}
            isBold
            isNegative={order.roi < 0}
          />

          {/* ========== ÜRÜN BAZLI DETAY ========== */}
          <div className="h-2 bg-muted" />

          <ExpandableRow
            label="Ürün Bazlı Kârlılık"
            value={productsData.hasMore
              ? `${productsData.visible.length} / ${productsData.total} ürün`
              : `${productsData.total} ürün`}
          >
            {productsData.visible.map((product, index) => (
              <div
                key={index}
                className="flex items-center justify-between py-2 px-4 pl-10 border-b border-border text-xs"
              >
                <div className="flex-1 min-w-0 pr-2">
                  <p className="text-foreground truncate">{product.productName}</p>
                </div>
                <div className="flex items-center gap-3 text-right">
                  <div>
                    <p className="text-muted-foreground">{product.quantity} ad × {formatCurrency(product.unitPrice)}</p>
                    <p className={cn("font-medium", product.profit < 0 ? "text-red-600" : "text-green-600")}>
                      Kâr: {formatCurrency(product.profit)}
                    </p>
                  </div>
                </div>
              </div>
            ))}
            {/* Load More Button */}
            {productsData.hasMore && (
              <div className="flex justify-center py-3 px-4 pl-10 border-b border-border">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleLoadMoreProducts}
                  disabled={isLoadingMore}
                  className="text-xs h-7"
                >
                  {isLoadingMore ? (
                    <>
                      <Loader2 className="h-3 w-3 animate-spin mr-1" />
                      Yükleniyor...
                    </>
                  ) : (
                    `Daha Fazla Yükle (${productsData.total - productsData.visible.length} ürün)`
                  )}
                </Button>
              </div>
            )}
          </ExpandableRow>

          {/* Kargo Bilgileri (varsa) */}
          {order.shipmentInfo && (order.shipmentInfo.cargoCompany || order.shipmentInfo.trackingNumber) && (
            <>
              <div className="h-2 bg-muted" />
              <ExpandableRow
                label="Kargo Bilgileri"
                value={order.shipmentInfo.cargoCompany || "-"}
              >
                {order.shipmentInfo.trackingNumber && (
                  <SubRow
                    label="Takip No"
                    value={order.shipmentInfo.trackingNumber}
                  />
                )}
              </ExpandableRow>
            </>
          )}
        </div>
        </FadeIn>
      </SheetContent>
    </Sheet>
  );
}
