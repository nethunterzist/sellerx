"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { ChevronRight, ChevronDown, Store, ExternalLink } from "lucide-react";
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

interface CostHistoryItem {
  stockDate: string;
  quantity: number;
  unitCost: number;
  costVatRate?: number;
  usedQuantity?: number;
}

interface ProductStatus {
  onSale: boolean;
  approved: boolean;
  hasActiveCampaign: boolean;
  archived: boolean;
  blacklisted: boolean;
  rejected?: boolean;
}

interface ProductDetailData {
  id: string;
  name: string;
  sku: string;
  barcode?: string;
  image?: string;

  // ============== TEMEL METRİKLER ==============
  sales: number;              // Brüt Ciro
  units: number;              // Satış Adedi
  returnQuantity: number;     // İade Adedi

  // ============== İNDİRİMLER & KUPONLAR ==============
  sellerDiscount: number;     // Satıcı İndirimi
  platformDiscount: number;   // Platform İndirimi
  couponDiscount: number;     // Kupon İndirimi
  totalDiscount: number;      // Toplam İndirim

  // ============== NET CİRO ==============
  netRevenue: number;         // Net Ciro = Brüt - İndirimler

  // ============== MALİYETLER ==============
  productCost: number;        // Ürün Maliyeti (FIFO)
  shippingCost: number;       // Kargo Maliyeti (sipariş bazlı)
  refundCost: number;         // İade Maliyeti

  // ============== KOMİSYON ==============
  commission: number;         // Tahmini Komisyon

  // ============== KÂR METRİKLERİ ==============
  grossProfit: number;        // Brüt Kâr
  netProfit: number;          // Net Kâr

  // ============== ORANLAR ==============
  refundRate: number;         // İade Oranı (%)
  profitMargin: number;       // Kar Marjı (%)
  roi: number;                // ROI (%)

  // ============== ESKİ ALANLAR (Geriye Uyumluluk) ==============
  promo: number;              // Eski promosyon alanı
  costOfGoods: number;        // Eski maliyet alanı (productCost ile eşleşir)
  refundPercentage: number;   // Eski iade oranı (refundRate ile eşleşir)
  margin: number;             // Eski marj (profitMargin ile eşleşir)

  // Alt detaylar (expandable)
  salesBreakdown?: {
    organic: number;
    sponsored: number;
  };
  unitsBreakdown?: {
    organic: number;
    sponsored: number;
  };
  commissionBreakdown?: {
    marketplace: number;
    payment: number;
    shipping: number;
  };
  // TrendyolProduct'tan gelen veriler
  categoryName?: string;
  brand?: string;
  salePrice?: number;
  vatRate?: number;
  commissionRate?: number;
  trendyolQuantity?: number;
  productUrl?: string;
  status?: ProductStatus;
  costHistory?: CostHistoryItem[];
}

interface ProductDetailPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  product: ProductDetailData | null;
}

function formatPercentage(value: number): string {
  return `${value.toFixed(2)}%`;
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("tr-TR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

// Durum Badge bileşeni
function StatusBadges({ status }: { status?: ProductStatus }) {
  if (!status) return null;

  return (
    <div className="flex flex-wrap gap-1.5 mt-2">
      {status.onSale && (
        <Badge variant="default" className="bg-green-500 hover:bg-green-600 text-xs">
          Satışta
        </Badge>
      )}
      {status.hasActiveCampaign && (
        <Badge variant="default" className="bg-orange-500 hover:bg-orange-600 text-xs">
          Kampanyalı
        </Badge>
      )}
      {status.archived && (
        <Badge variant="secondary" className="text-xs">
          Arşivlenmiş
        </Badge>
      )}
      {status.blacklisted && (
        <Badge variant="destructive" className="text-xs">
          Kara Liste
        </Badge>
      )}
      {status.rejected && (
        <Badge variant="destructive" className="text-xs">
          Reddedildi
        </Badge>
      )}
    </div>
  );
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

export function ProductDetailPanel({
  open,
  onOpenChange,
  product,
}: ProductDetailPanelProps) {
  const { formatCurrency } = useCurrency();

  if (!product) return null;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[420px] sm:max-w-[420px] p-0 overflow-y-auto">
        {/* Accessibility: Visually hidden title for screen readers */}
        <SheetTitle className="sr-only">Ürün Detayları - {product.name}</SheetTitle>

        {/* Header */}
        <div className="sticky top-0 z-10 border-b border-border bg-[#565d6a]">
          <div className="flex items-start gap-3 p-4">
            {product.image ? (
              <a
                href={product.productUrl || "#"}
                target="_blank"
                rel="noopener noreferrer"
                className="relative group flex-shrink-0 cursor-pointer"
                onClick={(e) => { if (!product.productUrl) e.preventDefault(); }}
              >
                <img
                  src={product.image}
                  alt={product.name}
                  className="w-14 h-14 rounded-lg object-cover border-2 border-white/30 group-hover:border-orange-400 transition-colors"
                />
                <div className="absolute inset-0 rounded-lg bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                  <ExternalLink className="h-5 w-5 text-white" />
                </div>
              </a>
            ) : (
              <div className="w-14 h-14 bg-white/20 rounded-lg flex items-center justify-center flex-shrink-0">
                <Store className="h-7 w-7 text-white" />
              </div>
            )}
            <div className="flex-1 min-w-0 pr-8">
              <p className="text-xs text-white/70">
                {product.barcode || product.sku}
              </p>
              {product.productUrl ? (
                <a
                  href={product.productUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm font-semibold text-white mt-0.5 line-clamp-2 hover:underline block"
                >
                  {product.name}
                </a>
              ) : (
                <p className="text-sm font-semibold text-white mt-0.5 line-clamp-2">
                  {product.name}
                </p>
              )}
              {(product.categoryName || product.brand) && (
                <p className="text-xs text-white/60 mt-1">
                  {product.categoryName}
                  {product.categoryName && product.brand && " · "}
                  {product.brand}
                </p>
              )}
              <StatusBadges status={product.status} />
            </div>
          </div>
        </div>

        {/* Content - 32 Finansal Metrik */}
        <div className="divide-y divide-border">
          {/* ========== SATIŞ METRİKLERİ ========== */}

          {/* Satışlar (Brüt Ciro) */}
          <ExpandableRow
            label="Satışlar (Brüt Ciro)"
            value={formatCurrency(product.sales)}
            isBold
          />

          {/* Adet (Satış Adedi) */}
          <ExpandableRow
            label="Adet"
            value={String(product.units)}
            isBold
          />

          {/* ========== İNDİRİMLER & KUPONLAR ========== */}

          <ExpandableRow
            label="İndirimler & Kuponlar"
            value={formatCurrency(-(product.totalDiscount || (product.sellerDiscount + product.platformDiscount + product.couponDiscount)))}
            isNegative={(product.totalDiscount || (product.sellerDiscount + product.platformDiscount + product.couponDiscount)) > 0}
          >
            <SubRow
              label="Satıcı İndirimi"
              value={formatCurrency(-product.sellerDiscount)}
              isNegative={product.sellerDiscount > 0}
            />
            <SubRow
              label="Platform İndirimi"
              value={formatCurrency(-product.platformDiscount)}
              isNegative={product.platformDiscount > 0}
            />
            <SubRow
              label="Kupon İndirimi"
              value={formatCurrency(-product.couponDiscount)}
              isNegative={product.couponDiscount > 0}
            />
          </ExpandableRow>

          {/* Net Ciro */}
          <ExpandableRow
            label="Net Ciro"
            value={formatCurrency(product.netRevenue)}
            isBold
          />

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ========== MALİYETLER ========== */}

          {/* Ürün Maliyeti */}
          <ExpandableRow
            label="Ürün Maliyeti"
            value={formatCurrency(-(product.productCost || product.costOfGoods))}
            isNegative={(product.productCost || product.costOfGoods) > 0}
          />

          {/* Kargo Maliyeti */}
          <ExpandableRow
            label="Kargo Maliyeti"
            value={formatCurrency(-(product.shippingCost || 0))}
            isNegative={(product.shippingCost || 0) > 0}
          />

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ========== KOMİSYON ========== */}

          <ExpandableRow
            label="Komisyon"
            value={formatCurrency(-product.commission)}
            isNegative={product.commission > 0}
          />

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ========== KÂR METRİKLERİ ========== */}

          {/* Brüt Kâr */}
          <ExpandableRow
            label="Brüt Kâr"
            value={formatCurrency(product.grossProfit)}
            isNegative={product.grossProfit < 0}
          />

          {/* Divider - Strong border before Net Profit */}
          <div className="h-2 bg-primary/10" />

          {/* Net Kâr - Vurgulu */}
          <div className="bg-primary/5">
            <ExpandableRow
              label="Net Kâr"
              value={formatCurrency(product.netProfit)}
              isBold
              isNegative={product.netProfit < 0}
            />
          </div>

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ========== ORANLAR ========== */}

          {/* Kar Marjı */}
          <ExpandableRow
            label="Kar Marjı"
            value={formatPercentage(product.profitMargin || product.margin)}
            isBold
            isNegative={(product.profitMargin || product.margin) < 0}
          />

          {/* ROI */}
          <ExpandableRow
            label="ROI"
            value={formatPercentage(product.roi)}
            isBold
            isNegative={product.roi < 0}
          />

          {/* Fiyat Bilgileri - sadece varsa göster */}
          {(product.salePrice != null || product.vatRate != null || product.commissionRate != null) && (
            <>
              <div className="h-1 bg-muted" />

              {product.salePrice != null && (
                <ExpandableRow
                  label="Satış Fiyatı"
                  value={formatCurrency(product.salePrice)}
                />
              )}

              {product.vatRate != null && (
                <ExpandableRow
                  label="KDV Oranı"
                  value={`%${product.vatRate}`}
                />
              )}

              {product.commissionRate != null && (
                <ExpandableRow
                  label="Komisyon Oranı"
                  value={`%${product.commissionRate}`}
                />
              )}

              {product.trendyolQuantity != null && (
                <ExpandableRow
                  label="Trendyol Stok"
                  value={String(product.trendyolQuantity)}
                />
              )}
            </>
          )}

          {/* Maliyet Geçmişi */}
          {product.costHistory && product.costHistory.length > 0 && (
            <>
              <div className="h-1 bg-muted" />

              <ExpandableRow
                label="Maliyet Geçmişi"
                value={`${product.costHistory.length} kayıt`}
              >
                {product.costHistory.map((item, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between py-2 px-4 pl-10 border-b border-border text-xs"
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-muted-foreground w-20">
                        {formatDate(item.stockDate)}
                      </span>
                      <span className="text-foreground">
                        {item.quantity} adet
                      </span>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="font-medium text-foreground">
                        {formatCurrency(item.unitCost)}/ad
                      </span>
                      {item.costVatRate !== undefined && (
                        <span className="text-muted-foreground">
                          %{item.costVatRate} KDV
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </ExpandableRow>
            </>
          )}

        </div>
      </SheetContent>
    </Sheet>
  );
}

export type { ProductDetailData };
