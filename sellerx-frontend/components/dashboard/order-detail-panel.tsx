"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { ChevronRight, ChevronDown, Package, ShoppingCart, Calendar } from "lucide-react";
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

export function OrderDetailPanel({
  open,
  onOpenChange,
  order,
}: OrderDetailPanelProps) {
  const { formatCurrency } = useCurrency();

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
          <div className="flex items-start gap-3 p-4">
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
          </div>
        </div>

        {/* Content */}
        <div className="divide-y divide-border">
          {/* ========== ÜRÜNLER ========== */}
          <ExpandableRow
            label="Ürünler"
            value={`${order.products.length} kalem`}
            isBold
            defaultOpen={true}
          >
            {order.products.map((product, index) => (
              <ProductRow key={index} product={product} formatCurrency={formatCurrency} />
            ))}
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
          <ExpandableRow
            label="Maliyetler"
            value={formatCurrency(-(order.totalProductCost + order.estimatedCommission + order.estimatedShippingCost + order.stoppage))}
            isNegative
          >
            <SubRow
              label="Ürün Maliyeti"
              value={formatCurrency(-order.totalProductCost)}
              isNegative={order.totalProductCost > 0}
            />
            <SubRow
              label="Tahmini Komisyon"
              value={formatCurrency(-order.estimatedCommission)}
              isNegative={order.estimatedCommission > 0}
            />
            <SubRow
              label="Kargo Maliyeti"
              value={formatCurrency(-order.estimatedShippingCost)}
              isNegative={order.estimatedShippingCost > 0}
            />
            <SubRow
              label="Stopaj"
              value={formatCurrency(-order.stoppage)}
              isNegative={order.stoppage > 0}
            />
          </ExpandableRow>

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
            value={`${order.products.length} ürün`}
          >
            {order.products.map((product, index) => (
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
      </SheetContent>
    </Sheet>
  );
}
