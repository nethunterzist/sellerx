"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { ChevronRight, ChevronDown, AlertTriangle, Calendar, X, Loader2 } from "lucide-react";
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
import {
  Tooltip,
  TooltipTrigger,
  TooltipContent,
} from "@/components/ui/tooltip";
import { useCurrency } from "@/lib/contexts/currency-context";
import { useDeductionBreakdown } from "@/hooks/useDashboardStats";

export interface PeriodDetailStats {
  // Temel metrikler
  sales: number;           // Brüt Ciro
  units: number;           // Satış adedi
  orders: number;          // Sipariş sayısı
  refunds: number;         // İade sayısı
  refundCost: number;      // İade maliyeti
  commission: number;      // Komisyon
  productCosts: number;    // Ürün maliyeti
  stoppage: number;        // Stopaj (vergi kesintisi)
  vatDifference: number;   // KDV farkı
  grossProfit: number;     // Brüt kar
  expenses: number;        // Toplam giderler
  netProfit: number;       // Net kar
  itemsWithoutCost: number;

  // ============== YENİ ALANLAR (32 Metrik) ==============

  // İndirimler & Kuponlar
  sellerDiscount?: number;     // Satıcı indirimi
  platformDiscount?: number;   // Platform indirimi
  couponDiscount?: number;     // Kupon indirimi

  // Net Ciro
  netRevenue?: number;         // Net ciro = brüt ciro - indirimler

  // Kargo
  shippingCost?: number;       // Kargo maliyeti

  // Platform Ücretleri (15 kategori)
  internationalServiceFee?: number;  // Uluslararası hizmet bedeli
  overseasOperationFee?: number;     // Yurt Dışı Operasyon Bedeli
  terminDelayFee?: number;           // Termin gecikme bedeli
  platformServiceFee?: number;       // Platform Hizmet Bedeli
  invoiceCreditFee?: number;         // Fatura Kontör Satış Bedeli
  unsuppliedFee?: number;            // Tedarik Edememe
  azOverseasOperationFee?: number;   // AZ-Yurtdışı Operasyon Bedeli
  azPlatformServiceFee?: number;     // AZ-Platform Hizmet Bedeli
  packagingServiceFee?: number;      // Paketleme hizmet bedeli
  warehouseServiceFee?: number;      // Depo hizmet bedeli
  callCenterFee?: number;            // Çağrı merkezi bedeli
  photoShootingFee?: number;         // Fotoğraf çekim bedeli
  integrationFee?: number;           // Entegrasyon bedeli
  storageServiceFee?: number;        // Depolama hizmet bedeli
  otherPlatformFees?: number;        // Diğer platform ücretleri

  // Erken Ödeme
  earlyPaymentFee?: number;          // Erken ödeme maliyeti

  // Gider Kategorileri (eski - geriye uyumluluk)
  officeExpenses?: number;           // Ofis giderleri
  packagingExpenses?: number;        // Ambalaj giderleri
  accountingExpenses?: number;       // Muhasebe giderleri
  otherExpenses?: number;            // Diğer giderler

  // Dinamik gider kategorileri - yeni kategoriler otomatik desteklenir
  expensesByCategory?: Record<string, number>;

  // İade Detayları
  refundRate?: number;               // İade oranı (%)

  // ROI
  roi?: number;

  // ============== KESİLEN FATURALAR (Fatura Kategorileri) ==============
  invoicedDeductions?: number;        // Toplam kesilen faturalar
  invoicedAdvertisingFees?: number;   // REKLAM kategorisi
  invoicedPenaltyFees?: number;       // CEZA kategorisi
  invoicedInternationalFees?: number; // ULUSLARARASI kategorisi
  invoicedOtherFees?: number;         // DIGER kategorisi
  invoicedRefunds?: number;           // IADE kategorisi (pozitif değer)
}

interface PeriodDetailModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  dateRange: string;
  stats: PeriodDetailStats | null;
  headerColor?: string;
  /** Store ID for fetching deduction breakdown - when provided, shows individual invoice types */
  storeId?: string;
  /** Start date in ISO format (YYYY-MM-DD) for deduction breakdown API */
  startDate?: string;
  /** End date in ISO format (YYYY-MM-DD) for deduction breakdown API */
  endDate?: string;
}

function formatPercentage(value: number): string {
  if (!isFinite(value) || isNaN(value)) return "0.00%";
  return `${value.toFixed(2)}%`;
}

// Expandable row component
function ExpandableRow({
  label,
  value,
  isNegative,
  isBold,
  children,
  defaultOpen = false,
  showWarning = false,
  warningText,
}: {
  label: string;
  value: string;
  isNegative?: boolean;
  isBold?: boolean;
  children?: React.ReactNode;
  defaultOpen?: boolean;
  showWarning?: boolean;
  warningText?: string;
}) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const hasChildren = !!children;

  if (!hasChildren) {
    return (
      <div className="flex items-center justify-between py-2.5 px-4 border-b border-border">
        <span className={cn("text-sm text-muted-foreground flex items-center gap-1.5", isBold && "font-semibold text-foreground")}>
          {label}
          {showWarning && warningText && (
            <Tooltip>
              <TooltipTrigger asChild>
                <AlertTriangle className="h-3.5 w-3.5 text-amber-500 cursor-help" />
              </TooltipTrigger>
              <TooltipContent side="top" className="bg-amber-500 text-white" showArrow={false}>
                {warningText}
              </TooltipContent>
            </Tooltip>
          )}
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

// Sub-row for expanded content
function SubRow({
  label,
  value,
  isNegative,
  isPositive,
}: {
  label: string;
  value: string;
  isNegative?: boolean;
  isPositive?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-2 px-4 pl-10 border-b border-border">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className={cn(
        "text-sm font-medium",
        isNegative && "text-red-600",
        isPositive && "text-green-600"
      )}>
        {isPositive ? `+${value}` : value}
      </span>
    </div>
  );
}

export function PeriodDetailModal({
  open,
  onOpenChange,
  title,
  dateRange,
  stats,
  headerColor = "bg-[#3B82F6]",
  storeId,
  startDate,
  endDate,
}: PeriodDetailModalProps) {
  const { formatCurrency } = useCurrency();

  // Fetch individual deduction breakdown when storeId and dates are available
  const { data: deductionBreakdown, isLoading: isLoadingBreakdown } = useDeductionBreakdown(
    storeId,
    startDate,
    endDate
  );

  if (!stats) return null;

  // Calculate derived values
  const margin = stats.sales > 0 ? (stats.netProfit / stats.sales) * 100 : 0;
  const roi = stats.productCosts > 0 ? (stats.netProfit / stats.productCosts) * 100 : 0;

  // Calculate total from breakdown for display
  const breakdownTotal = deductionBreakdown?.reduce((acc, item) => {
    return acc + (item.totalDebt || 0) - (item.totalCredit || 0);
  }, 0) || 0;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[420px] sm:max-w-[420px] p-0 overflow-y-auto">
        {/* Accessibility: Visually hidden title for screen readers */}
        <SheetTitle className="sr-only">Detay - {title}</SheetTitle>

        {/* Header */}
        <div className={cn("sticky top-0 z-10 border-b border-border", headerColor)}>
          <div className="flex items-center gap-3 p-4 relative">
            <div className="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center flex-shrink-0">
              <Calendar className="h-5 w-5 text-white" />
            </div>
            <div className="flex-1 min-w-0 pr-8">
              <p className="text-base font-semibold text-white">
                {title}
              </p>
              <p className="text-sm text-white/80 mt-0.5">
                {dateRange}
              </p>
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

        {/* Content - Metrics */}
        <div className="divide-y divide-border">
          {/* ==================== SATIŞLAR ==================== */}
          <ExpandableRow
            label="Satışlar (Brüt Ciro)"
            value={formatCurrency(stats.sales)}
            isBold
          />

          {/* Adet */}
          <ExpandableRow
            label="Satış Adedi"
            value={String(stats.units)}
          />

          {/* Siparis */}
          <ExpandableRow
            label="Sipariş Sayısı"
            value={String(stats.orders)}
          />

          {/* ==================== İNDİRİMLER ==================== */}
          <ExpandableRow
            label="İndirimler & Kuponlar"
            value={formatCurrency(-((stats.sellerDiscount ?? 0) + (stats.platformDiscount ?? 0) + (stats.couponDiscount ?? 0)))}
            isNegative={(stats.sellerDiscount ?? 0) + (stats.platformDiscount ?? 0) + (stats.couponDiscount ?? 0) > 0}
          >
            <SubRow label="Satıcı İndirimi" value={formatCurrency(-(stats.sellerDiscount ?? 0))} isNegative={(stats.sellerDiscount ?? 0) > 0} />
            <SubRow label="Platform İndirimi" value={formatCurrency(-(stats.platformDiscount ?? 0))} isNegative={(stats.platformDiscount ?? 0) > 0} />
            <SubRow label="Kupon İndirimi" value={formatCurrency(-(stats.couponDiscount ?? 0))} isNegative={(stats.couponDiscount ?? 0) > 0} />
          </ExpandableRow>

          {/* Net Ciro */}
          <ExpandableRow
            label="Net Ciro"
            value={formatCurrency(stats.netRevenue ?? (stats.sales - (stats.sellerDiscount ?? 0) - (stats.platformDiscount ?? 0) - (stats.couponDiscount ?? 0)))}
            isBold
          />

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ==================== MALİYETLER ==================== */}
          {/* Komisyon */}
          <ExpandableRow
            label="Komisyon"
            value={formatCurrency(-stats.commission)}
            isNegative={stats.commission > 0}
          />

          {/* Kargo Maliyeti */}
          <ExpandableRow
            label="Kargo Maliyeti"
            value={formatCurrency(-(stats.shippingCost ?? 0))}
            isNegative={(stats.shippingCost ?? 0) > 0}
          />

          {/* Urun Maliyeti */}
          <ExpandableRow
            label="Ürün Maliyeti"
            value={formatCurrency(-stats.productCosts)}
            isNegative={stats.productCosts > 0}
            showWarning={stats.itemsWithoutCost > 0}
            warningText={stats.itemsWithoutCost > 0 ? `${stats.itemsWithoutCost} ürünün maliyeti eksik` : undefined}
          />

          {/* Iade Maliyeti - sadece 0'dan büyükse göster */}
          {(stats.refundCost > 0 || stats.refunds > 0) && (
            <div className="flex items-center justify-between py-2.5 px-4 border-b border-border">
              <span className="text-sm text-muted-foreground flex items-center gap-2">
                İade Maliyeti
                {stats.refunds > 0 && (
                  <span className="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700">
                    {stats.refunds} ürün
                  </span>
                )}
              </span>
              <span className={cn("text-sm font-medium", stats.refundCost > 0 ? "text-red-600" : "text-foreground")}>
                {formatCurrency(-stats.refundCost)}
              </span>
            </div>
          )}

          {/* Stopaj (Vergi Kesintisi) */}
          <ExpandableRow
            label="Stopaj"
            value={formatCurrency(-stats.stoppage)}
            isNegative={stats.stoppage > 0}
          />

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ==================== KESİLEN FATURALAR (Birleşik) ==================== */}
          {/* Fatura tiplerini tek tek göster (API'den) veya grouped fallback */}
          <ExpandableRow
            label="Kesilen Faturalar"
            value={formatCurrency(deductionBreakdown && deductionBreakdown.length > 0
              ? -breakdownTotal
              : -(
                  // Fallback: Platform Ücretleri + Kategori Bazlı Kesintiler
                  (stats.platformServiceFee ?? 0) +
                  (stats.azPlatformServiceFee ?? 0) +
                  (stats.invoicedAdvertisingFees ?? 0) +
                  (stats.invoicedPenaltyFees ?? 0) +
                  (stats.invoicedInternationalFees ?? 0) +
                  (stats.invoicedOtherFees ?? 0) -
                  (stats.invoicedRefunds ?? 0)
                )
            )}
            isNegative={true}
          >
            {/* Loading state */}
            {isLoadingBreakdown && (
              <div className="flex items-center justify-center py-4 px-4">
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground mr-2" />
                <span className="text-sm text-muted-foreground">Yükleniyor...</span>
              </div>
            )}

            {/* Individual invoice types from API */}
            {!isLoadingBreakdown && deductionBreakdown && deductionBreakdown.length > 0 ? (
              deductionBreakdown.map((item) => {
                const isRefund = (item.totalCredit ?? 0) > 0;
                const amount = isRefund ? (item.totalCredit ?? 0) : (item.totalDebt ?? 0);

                // Skip items with zero amount
                if (amount === 0) return null;

                return (
                  <SubRow
                    key={item.transactionType}
                    label={item.transactionType}
                    value={formatCurrency(isRefund ? amount : -amount)}
                    isNegative={!isRefund}
                    isPositive={isRefund}
                  />
                );
              })
            ) : (
              /* Fallback: Grouped categories (backwards compatibility) */
              !isLoadingBreakdown && (
                <>
                  {(stats.platformServiceFee ?? 0) > 0 && (
                    <SubRow label="Platform Hizmet Bedeli" value={formatCurrency(-(stats.platformServiceFee ?? 0))} isNegative />
                  )}
                  {(stats.azPlatformServiceFee ?? 0) > 0 && (
                    <SubRow label="AZ-Platform Hizmet Bedeli" value={formatCurrency(-(stats.azPlatformServiceFee ?? 0))} isNegative />
                  )}
                  {(stats.invoicedAdvertisingFees ?? 0) > 0 && (
                    <SubRow label="Reklam Bedeli" value={formatCurrency(-(stats.invoicedAdvertisingFees ?? 0))} isNegative />
                  )}
                  {(stats.invoicedPenaltyFees ?? 0) > 0 && (
                    <SubRow label="Ceza / Kesintiler" value={formatCurrency(-(stats.invoicedPenaltyFees ?? 0))} isNegative />
                  )}
                  {(stats.invoicedInternationalFees ?? 0) > 0 && (
                    <SubRow label="Uluslararası Kesintiler" value={formatCurrency(-(stats.invoicedInternationalFees ?? 0))} isNegative />
                  )}
                  {(stats.invoicedOtherFees ?? 0) > 0 && (
                    <SubRow label="Diğer Kesintiler" value={formatCurrency(-(stats.invoicedOtherFees ?? 0))} isNegative />
                  )}
                  {(stats.invoicedRefunds ?? 0) > 0 && (
                    <SubRow label="İadeler (Tazmin)" value={formatCurrency(stats.invoicedRefunds ?? 0)} />
                  )}
                </>
              )
            )}
          </ExpandableRow>

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ==================== KÂR ==================== */}
          {/* Brut Kar */}
          <ExpandableRow
            label="Brüt Kâr"
            value={formatCurrency(stats.grossProfit)}
            isBold
            isNegative={stats.grossProfit < 0}
          />

          {/* Ekstra Giderler - sadece 0'dan büyükse göster */}
          {/* Dinamik kategori desteği: expensesByCategory varsa onu kullan, yoksa eski hardcoded alanlara fallback */}
          {(stats.expenses ?? 0) > 0 && (
            <ExpandableRow
              label="Ekstra Giderler"
              value={formatCurrency(-stats.expenses)}
              isNegative={(stats.expenses ?? 0) > 0}
            >
              {/* Dinamik kategoriler - expensesByCategory Map'inden render */}
              {stats.expensesByCategory && Object.keys(stats.expensesByCategory).length > 0 ? (
                Object.entries(stats.expensesByCategory)
                  .filter(([, amount]) => (amount ?? 0) > 0)
                  .map(([categoryName, amount]) => (
                    <SubRow
                      key={categoryName}
                      label={categoryName}
                      value={formatCurrency(-(amount ?? 0))}
                      isNegative
                    />
                  ))
              ) : (
                // Fallback: Eski hardcoded alanlar (geriye uyumluluk)
                <>
                  {(stats.officeExpenses ?? 0) > 0 && (
                    <SubRow label="Ofis Giderleri" value={formatCurrency(-(stats.officeExpenses ?? 0))} isNegative />
                  )}
                  {(stats.packagingExpenses ?? 0) > 0 && (
                    <SubRow label="Ambalaj Giderleri" value={formatCurrency(-(stats.packagingExpenses ?? 0))} isNegative />
                  )}
                  {(stats.accountingExpenses ?? 0) > 0 && (
                    <SubRow label="Muhasebe Giderleri" value={formatCurrency(-(stats.accountingExpenses ?? 0))} isNegative />
                  )}
                  {(stats.otherExpenses ?? 0) > 0 && (
                    <SubRow label="Diğer Giderler" value={formatCurrency(-(stats.otherExpenses ?? 0))} isNegative />
                  )}
                </>
              )}
            </ExpandableRow>
          )}

          {/* Divider - Strong border before Net Profit */}
          <div className="h-2 bg-primary/10" />

          {/* Net Kar - Highlighted */}
          <div className="bg-primary/5">
            <ExpandableRow
              label="Net Kâr"
              value={formatCurrency(stats.netProfit)}
              isBold
              isNegative={stats.netProfit < 0}
            />
          </div>

          {/* Divider */}
          <div className="h-1 bg-muted" />

          {/* ==================== METRİKLER ==================== */}
          {/* Marj */}
          <ExpandableRow
            label="Kâr Marjı"
            value={formatPercentage(margin)}
            isBold
            isNegative={margin < 0}
          />

          {/* ROI */}
          <ExpandableRow
            label="ROI"
            value={formatPercentage(stats.roi ?? roi)}
            isBold
            isNegative={(stats.roi ?? roi) < 0}
          />
        </div>
      </SheetContent>
    </Sheet>
  );
}
