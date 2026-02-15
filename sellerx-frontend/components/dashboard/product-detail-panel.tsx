"use client";

import { useState, useMemo, useEffect } from "react";
import { FadeIn } from "@/components/motion";
import { cn } from "@/lib/utils";
import { ChevronRight, ChevronDown, Store, ExternalLink, X, Loader2, FileText } from "lucide-react";
import { useProductCommissionBreakdown, useProductCargoBreakdown, useProductExpenseBreakdown } from "@/hooks/queries/use-invoices";
import { useSelectedStore } from "@/hooks/queries/use-stores";
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

  // ============== REKLAM METRİKLERİ ==============
  cpc?: number;                    // Cost Per Click (TL)
  cvr?: number;                    // Conversion Rate (örn: 0.018 = %1.8)
  advertisingCostPerSale?: number; // Reklam Maliyeti = CPC / CVR
  acos?: number;                   // ACOS = (advertisingCostPerSale / salePrice) * 100
  totalAdvertisingCost?: number;   // Toplam reklam maliyeti
}

interface ProductDetailPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  product: ProductDetailData | null;
  startDate?: string; // ISO date for invoice queries
  endDate?: string;   // ISO date for invoice queries
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

const COST_HISTORY_PAGE_SIZE = 20;

export function ProductDetailPanel({
  open,
  onOpenChange,
  product,
  startDate,
  endDate,
}: ProductDetailPanelProps) {
  const { formatCurrency } = useCurrency();
  const [costHistoryLimit, setCostHistoryLimit] = useState(COST_HISTORY_PAGE_SIZE);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  // Get store ID for invoice queries
  const { data: selectedStoreData } = useSelectedStore();
  const storeId = selectedStoreData?.selectedStoreId;

  // Fetch invoice data for the product (cargo and commission breakdowns)
  const { data: commissionData, isLoading: isLoadingCommission } = useProductCommissionBreakdown(
    storeId,
    product?.barcode,
    startDate,
    endDate
  );

  const { data: cargoData, isLoading: isLoadingCargo } = useProductCargoBreakdown(
    storeId,
    product?.barcode,
    startDate,
    endDate
  );

  // Fetch expense breakdown (platform fees, penalties, international, other)
  const { data: expenseData, isLoading: isLoadingExpense } = useProductExpenseBreakdown(
    storeId,
    product?.barcode,
    startDate,
    endDate
  );

  // Frontend slice pagination for cost history
  const costHistoryData = useMemo(() => {
    if (!product?.costHistory) return { visible: [], total: 0, hasMore: false };
    const total = product.costHistory.length;
    const visible = product.costHistory.slice(0, costHistoryLimit);
    return {
      visible,
      total,
      hasMore: costHistoryLimit < total,
    };
  }, [product?.costHistory, costHistoryLimit]);

  const handleLoadMoreCostHistory = () => {
    setIsLoadingMore(true);
    // Simulate async for UX (instant feels jarring)
    setTimeout(() => {
      setCostHistoryLimit((prev) => prev + COST_HISTORY_PAGE_SIZE);
      setIsLoadingMore(false);
    }, 150);
  };

  // Reset pagination when product changes
  useEffect(() => {
    setCostHistoryLimit(COST_HISTORY_PAGE_SIZE);
  }, [product?.id]);

  if (!product) return null;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[420px] sm:max-w-[420px] p-0 overflow-y-auto">
        {/* Accessibility: Visually hidden title for screen readers */}
        <SheetTitle className="sr-only">Ürün Detayları - {product.name}</SheetTitle>

        {/* Header */}
        <div className="sticky top-0 z-10 border-b border-border bg-[#565d6a]">
          <div className="flex items-start gap-3 p-4 relative">
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

        {/* Content - 32 Finansal Metrik */}
        <FadeIn delay={0.1}>
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

          {/* ========== İADE BİLGİLERİ ========== */}
          {(product.returnQuantity > 0 || product.refundCost > 0) && (
            <ExpandableRow
              label="İadeler"
              value={`${product.returnQuantity || 0} adet`}
              isNegative
            >
              {product.returnQuantity > 0 && (
                <SubRow
                  label="İade Adedi"
                  value={`${product.returnQuantity} adet`}
                />
              )}
              {product.refundCost > 0 && (
                <SubRow
                  label="İade Tutarı"
                  value={formatCurrency(-product.refundCost)}
                  isNegative
                />
              )}
              {product.refundRate > 0 && (
                <SubRow
                  label="İade Oranı"
                  value={formatPercentage(product.refundRate)}
                  isNegative
                />
              )}
            </ExpandableRow>
          )}

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
          >
            {/* Fatura Kargo Verisi */}
            {isLoadingCargo ? (
              <div className="flex items-center gap-2 py-2 px-4 pl-10 text-sm text-muted-foreground">
                <Loader2 className="h-3 w-3 animate-spin" />
                Fatura verisi yükleniyor...
              </div>
            ) : cargoData && cargoData.totalShipmentCount > 0 ? (
              <SubRow
                label={`📄 Fatura Kargo (${cargoData.totalShipmentCount} gönderi)`}
                value={formatCurrency(-cargoData.totalAmount)}
                isNegative={cargoData.totalAmount > 0}
              />
            ) : startDate && endDate ? (
              <div className="py-2 px-4 pl-10 text-xs text-muted-foreground">
                Fatura verisi bulunamadı
              </div>
            ) : null}
          </ExpandableRow>

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ========== KOMİSYON ========== */}

          <ExpandableRow
            label="Komisyon"
            value={formatCurrency(-product.commission)}
            isNegative={product.commission > 0}
          >
            {/* Fatura Komisyon Kırılımı */}
            {isLoadingCommission ? (
              <div className="flex items-center gap-2 py-2 px-4 pl-10 text-sm text-muted-foreground">
                <Loader2 className="h-3 w-3 animate-spin" />
                Fatura verisi yükleniyor...
              </div>
            ) : commissionData && commissionData.totalItemCount > 0 ? (
              <>
                {/* Satış Komisyonu */}
                {commissionData.saleCommission !== undefined && commissionData.saleCommission !== 0 && (
                  <SubRow
                    label="📄 Satış Komisyonu"
                    value={formatCurrency(-commissionData.saleCommission)}
                    isNegative={commissionData.saleCommission > 0}
                  />
                )}
                {/* İndirim Komisyonu (genellikle pozitif - satıcıya geri ödeme) */}
                {commissionData.discountCommission !== undefined && commissionData.discountCommission !== 0 && (
                  <SubRow
                    label="📄 İndirim Komisyonu"
                    value={formatCurrency(commissionData.discountCommission)}
                    isNegative={commissionData.discountCommission < 0}
                  />
                )}
                {/* Kupon Komisyonu (genellikle pozitif - satıcıya geri ödeme) */}
                {commissionData.couponCommission !== undefined && commissionData.couponCommission !== 0 && (
                  <SubRow
                    label="📄 Kupon Komisyonu"
                    value={formatCurrency(commissionData.couponCommission)}
                    isNegative={commissionData.couponCommission < 0}
                  />
                )}
                {/* İade Komisyonu */}
                {commissionData.returnCommission !== undefined && commissionData.returnCommission !== 0 && (
                  <SubRow
                    label="📄 İade Komisyonu"
                    value={formatCurrency(commissionData.returnCommission)}
                    isNegative={commissionData.returnCommission < 0}
                  />
                )}
              </>
            ) : startDate && endDate ? (
              <div className="py-2 px-4 pl-10 text-xs text-muted-foreground">
                Fatura verisi bulunamadı
              </div>
            ) : null}
          </ExpandableRow>

          {/* ========== DİĞER GİDERLER (Expense Breakdown) ========== */}
          {isLoadingExpense ? (
            <div className="flex items-center gap-2 py-3 px-4 text-sm text-muted-foreground border-b border-border">
              <Loader2 className="h-3 w-3 animate-spin" />
              Gider verisi yükleniyor...
            </div>
          ) : expenseData && expenseData.hasExpenseData ? (
            <ExpandableRow
              label="Diğer Giderler"
              value={formatCurrency(-expenseData.totalExpenses)}
              isNegative={expenseData.totalExpenses > 0}
            >
              {/* Platform Hizmet Bedeli */}
              {expenseData.platformServiceFee > 0 && (
                <SubRow
                  label={`📄 Platform Hizmet (${expenseData.platformServiceFeeCount})`}
                  value={formatCurrency(-expenseData.platformServiceFee)}
                  isNegative
                />
              )}
              {/* Uluslararası Kargo */}
              {expenseData.internationalShippingFee > 0 && (
                <SubRow
                  label={`🌍 Uluslararası Kargo (${expenseData.internationalShippingCount})`}
                  value={formatCurrency(-expenseData.internationalShippingFee)}
                  isNegative
                />
              )}
              {/* Cezalar */}
              {expenseData.penaltyFee > 0 && (
                <SubRow
                  label={`⚠️ Cezalar (${expenseData.penaltyCount})`}
                  value={formatCurrency(-expenseData.penaltyFee)}
                  isNegative
                />
              )}
              {/* Diğer */}
              {expenseData.otherExpenses > 0 && (
                <SubRow
                  label={`📋 Diğer (${expenseData.otherExpenseCount})`}
                  value={formatCurrency(-expenseData.otherExpenses)}
                  isNegative
                />
              )}
              {/* Toplam KDV */}
              {expenseData.totalVatAmount > 0 && (
                <div className="flex items-center justify-between py-2 px-4 pl-10 border-b border-border bg-muted/30">
                  <span className="text-xs text-muted-foreground">KDV Toplamı</span>
                  <span className="text-xs font-medium text-muted-foreground">
                    {formatCurrency(expenseData.totalVatAmount)}
                  </span>
                </div>
              )}
            </ExpandableRow>
          ) : null}

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

          {/* ========== REKLAM METRİKLERİ ========== */}
          {(product.acos != null || product.advertisingCostPerSale != null || product.cpc != null) && (
            <>
              <div className="h-1 bg-muted" />

              {/* ACOS - Renkli gösterim */}
              {product.acos != null && (
                <div className="flex items-center justify-between py-2.5 px-4 border-b border-border">
                  <span className="text-sm text-muted-foreground font-semibold">ACOS</span>
                  <span
                    className={cn(
                      "text-sm font-semibold",
                      product.acos <= 15
                        ? "text-green-600"
                        : product.acos <= 30
                        ? "text-yellow-600"
                        : "text-red-600"
                    )}
                  >
                    {formatPercentage(product.acos)}
                  </span>
                </div>
              )}

              {/* Reklam Maliyeti (birim başı) */}
              {product.advertisingCostPerSale != null && (
                <ExpandableRow
                  label="Reklam Maliyeti"
                  value={`${formatCurrency(product.advertisingCostPerSale)}/satış`}
                  isNegative
                >
                  {/* CPC ve CVR alt satırları */}
                  {product.cpc != null && (
                    <SubRow
                      label="CPC (Tıklama Başı Maliyet)"
                      value={formatCurrency(product.cpc)}
                    />
                  )}
                  {product.cvr != null && (
                    <SubRow
                      label="CVR (Dönüşüm Oranı)"
                      value={`%${(product.cvr * 100).toFixed(2)}`}
                    />
                  )}
                </ExpandableRow>
              )}

              {/* Toplam Reklam Maliyeti */}
              {product.totalAdvertisingCost != null && product.totalAdvertisingCost > 0 && (
                <ExpandableRow
                  label="Toplam Reklam Maliyeti"
                  value={formatCurrency(-product.totalAdvertisingCost)}
                  isNegative
                />
              )}
            </>
          )}

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
          {costHistoryData.total > 0 && (
            <>
              <div className="h-1 bg-muted" />

              <ExpandableRow
                label="Maliyet Geçmişi"
                value={costHistoryData.hasMore
                  ? `${costHistoryData.visible.length} / ${costHistoryData.total} kayıt`
                  : `${costHistoryData.total} kayıt`}
              >
                {costHistoryData.visible.map((item, index) => (
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
                {/* Load More Button */}
                {costHistoryData.hasMore && (
                  <div className="flex justify-center py-3 px-4 pl-10 border-b border-border">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={handleLoadMoreCostHistory}
                      disabled={isLoadingMore}
                      className="text-xs h-7"
                    >
                      {isLoadingMore ? (
                        <>
                          <Loader2 className="h-3 w-3 animate-spin mr-1" />
                          Yükleniyor...
                        </>
                      ) : (
                        `Daha Fazla Yükle (${costHistoryData.total - costHistoryData.visible.length} kayıt)`
                      )}
                    </Button>
                  </div>
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

export type { ProductDetailData };
