"use client";

import { useMemo, useState } from "react";
import { cn } from "@/lib/utils";
import { ChevronRight, ChevronDown, Info } from "lucide-react";
import { DashboardChart } from "./dashboard-chart";
import { Skeleton } from "@/components/ui/skeleton";
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
import type { DashboardStatsResponse } from "@/types/dashboard";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ChartViewProps {
  stats: DashboardStatsResponse | null | undefined;
  isLoading?: boolean;
  selectedProducts?: string[];
}

function formatPercentage(value: number): string {
  return `${value.toFixed(1)}%`;
}

interface MetricRowProps {
  label: string;
  value: string;
  isNegative?: boolean;
  isBold?: boolean;
}

function MetricRow({ label, value, isNegative, isBold }: MetricRowProps) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-border last:border-b-0">
      <span className={cn("text-sm text-muted-foreground", isBold && "font-semibold text-foreground")}>
        {label}
      </span>
      <span
        className={cn(
          "text-sm font-medium",
          isBold && "font-semibold",
          isNegative ? "text-red-600" : "text-foreground"
        )}
      >
        {value}
      </span>
    </div>
  );
}

function MetricRowSkeleton() {
  return (
    <div className="flex items-center justify-between py-2 border-b border-border">
      <Skeleton className="h-4 w-24" />
      <Skeleton className="h-4 w-16" />
    </div>
  );
}

// Expandable metric row for expense breakdown
interface ExpandableMetricRowProps {
  label: string;
  value: string;
  isNegative?: boolean;
  isBold?: boolean;
  children?: React.ReactNode;
  defaultOpen?: boolean;
  infoText?: string;
}

function ExpandableMetricRow({
  label,
  value,
  isNegative,
  isBold,
  children,
  defaultOpen = false,
  infoText,
}: ExpandableMetricRowProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const hasChildren = !!children;

  if (!hasChildren) {
    return (
      <div className="flex items-center justify-between py-2 border-b border-border">
        <span className={cn("text-sm text-muted-foreground flex items-center gap-1.5", isBold && "font-semibold text-foreground")}>
          {label}
          {infoText && (
            <Tooltip>
              <TooltipTrigger asChild>
                <Info className="h-3.5 w-3.5 text-muted-foreground cursor-help" />
              </TooltipTrigger>
              <TooltipContent side="top" className="max-w-[250px]" showArrow={false}>
                {infoText}
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
        <div className="flex items-center justify-between py-2 border-b border-border hover:bg-muted/50 cursor-pointer">
          <div className="flex items-center gap-1.5">
            {isOpen ? (
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            ) : (
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            )}
            <span className={cn("text-sm text-muted-foreground", isBold && "font-semibold text-foreground")}>
              {label}
            </span>
            {infoText && (
              <Tooltip>
                <TooltipTrigger asChild>
                  <Info className="h-3.5 w-3.5 text-muted-foreground cursor-help" />
                </TooltipTrigger>
                <TooltipContent side="top" className="max-w-[250px]" showArrow={false}>
                  {infoText}
                </TooltipContent>
              </Tooltip>
            )}
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
        <div className="bg-muted/30 border-b border-border">{children}</div>
      </CollapsibleContent>
    </Collapsible>
  );
}

// Sub-row for expanded content
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
    <div className="flex items-center justify-between py-1.5 px-4 pl-7">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className={cn("text-xs font-medium", isNegative ? "text-red-600" : "text-foreground")}>
        {value}
      </span>
    </div>
  );
}

export function ChartView({ stats, isLoading, selectedProducts = [] }: ChartViewProps) {
  const { formatCurrency } = useCurrency();

  // Orders'dan günlük veri çıkar (ürün filtrelemeli)
  const dailyData = useMemo(() => {
    if (!stats?.thisMonth?.orders) return [];

    const ordersByDate = new Map<string, {
      date: string;
      displayDate: string;
      units: number;
      revenue: number;
      netProfit: number;
      refunds: number;
    }>();

    stats.thisMonth.orders.forEach((order) => {
      // Ürün filtresi varsa sadece seçili ürünleri al
      let products = order.products || [];
      if (selectedProducts.length > 0) {
        products = products.filter((p) => selectedProducts.includes(p.barcode));
        // Bu siparişte seçili ürün yoksa atla
        if (products.length === 0) return;
      }

      const date = order.orderDate.split("T")[0];
      const displayDate = new Date(date).toLocaleDateString("tr-TR", {
        day: "2-digit",
        month: "short",
      });

      if (!ordersByDate.has(date)) {
        ordersByDate.set(date, {
          date,
          displayDate,
          units: 0,
          revenue: 0,
          netProfit: 0,
          refunds: 0,
        });
      }

      const dayData = ordersByDate.get(date)!;

      // Filtrelenmiş ürünlerden metrikleri hesapla
      if (selectedProducts.length > 0) {
        // Filtrelenmiş ürünler üzerinden hesapla
        products.forEach((p) => {
          dayData.units += p.quantity || 0;
          dayData.revenue += p.totalPrice || 0;
          dayData.netProfit += (p.totalPrice || 0) - (p.totalCost || 0) - (p.commission || 0);
        });
      } else {
        // Sipariş bazında hesapla (orijinal davranış)
        const orderUnits = products.reduce((sum, p) => sum + (p.quantity || 0), 0);
        dayData.units += orderUnits;
        dayData.revenue += order.revenue || order.totalPrice || 0;
        dayData.netProfit += order.grossProfit || 0;
        dayData.refunds += order.returnPrice || 0;
      }
    });

    // Tarihe göre sırala
    return Array.from(ordersByDate.values()).sort((a, b) =>
      a.date.localeCompare(b.date)
    );
  }, [stats, selectedProducts]);

  // Özet metrikler (ürün filtrelemeli)
  const summary = useMemo(() => {
    const data = stats?.thisMonth;
    if (!data) return null;

    // Hesap bazlı giderler (ürün filtresinden bağımsız)
    const accountLevelExpenses = {
      shippingCost: data.totalShippingCost ?? 0,
      advertisingFees: data.invoicedAdvertisingFees ?? 0,
      platformServiceFee: (data.platformServiceFee ?? 0) + (data.azPlatformServiceFee ?? 0),
      penaltyFees: data.invoicedPenaltyFees ?? 0,
      internationalFees: data.invoicedInternationalFees ?? 0,
      otherFees: data.invoicedOtherFees ?? 0,
      refunds: data.invoicedRefunds ?? 0, // Pozitif değer (iade tazminatı)
      userExpenses: data.totalExpenseAmount ?? 0, // Kullanıcı tanımlı giderler
    };

    // Toplam gider kırılımı hesaplama (komisyon hariç)
    const totalExpenseBreakdown =
      accountLevelExpenses.shippingCost +
      accountLevelExpenses.advertisingFees +
      accountLevelExpenses.platformServiceFee +
      accountLevelExpenses.penaltyFees +
      accountLevelExpenses.internationalFees +
      accountLevelExpenses.otherFees -
      accountLevelExpenses.refunds + // İadeler çıkarılır (pozitif etki)
      accountLevelExpenses.userExpenses;

    // Ürün filtresi varsa products array'inden hesapla (shippingCost burada var)
    if (selectedProducts.length > 0 && data.products) {
      const filteredProducts = data.products.filter((p) =>
        selectedProducts.includes(p.barcode)
      );

      let totalRevenue = 0;
      let totalProductsSold = 0;
      let returnCount = 0;
      let returnCost = 0;
      let totalEstimatedCommission = 0;
      let totalProductCosts = 0;
      let productShippingCost = 0;

      filteredProducts.forEach((p) => {
        totalProductsSold += p.totalSoldQuantity || 0;
        totalRevenue += p.revenue || 0;
        totalProductCosts += p.productCost || 0;
        totalEstimatedCommission += p.estimatedCommission || 0;
        productShippingCost += p.shippingCost || 0;
        returnCount += p.returnQuantity || 0;
        returnCost += p.refundCost || 0;
      });

      // Sipariş sayısını orders'tan hesapla
      let totalOrders = 0;
      if (data.orders) {
        data.orders.forEach((order) => {
          const hasSelectedProduct = (order.products || []).some((p) =>
            selectedProducts.includes(p.barcode)
          );
          if (hasSelectedProduct) totalOrders++;
        });
      }

      const grossProfit = totalRevenue - totalProductCosts;
      const netProfit = grossProfit - totalEstimatedCommission - productShippingCost;
      const margin = totalRevenue > 0 ? (grossProfit / totalRevenue) * 100 : 0;
      const roi = totalProductCosts > 0 ? (netProfit / totalProductCosts) * 100 : 0;

      return {
        totalRevenue,
        totalProductsSold,
        totalOrders,
        returnCount,
        returnCost,
        totalEstimatedCommission,
        totalProductCosts,
        grossProfit,
        netProfit,
        margin,
        roi,
        // Ürün bazlı gider kırılımı
        isProductFiltered: true,
        productShippingCost,
        // Hesap bazlı giderler (ürün filtresinde gösterilmez ama bilgi için saklanır)
        accountLevelExpenses,
        totalExpenseBreakdown: productShippingCost, // Ürün filtresinde sadece kargo
      };
    }

    // Filtre yoksa orijinal hesaplama
    const netProfit = data.grossProfit - data.totalEstimatedCommission - totalExpenseBreakdown;
    const margin = data.totalRevenue > 0
      ? (data.grossProfit / data.totalRevenue) * 100
      : 0;
    const roi = data.totalProductCosts > 0
      ? (netProfit / data.totalProductCosts) * 100
      : 0;

    // İndirimler
    const sellerDiscount = data.totalSellerDiscount ?? 0;
    const platformDiscount = data.totalPlatformDiscount ?? 0;
    const couponDiscount = data.totalCouponDiscount ?? 0;
    const totalDiscount = sellerDiscount + platformDiscount + couponDiscount;
    const netRevenue = data.netRevenue ?? (data.totalRevenue - totalDiscount);

    // Stopaj
    const stoppage = data.totalStoppage ?? 0;

    // Kesilen Faturalar (invoiced kategorileri)
    const invoicedAdvertisingFees = data.invoicedAdvertisingFees ?? 0;
    const invoicedPenaltyFees = data.invoicedPenaltyFees ?? 0;
    const invoicedInternationalFees = data.invoicedInternationalFees ?? 0;
    const invoicedOtherFees = data.invoicedOtherFees ?? 0;
    const invoicedRefunds = data.invoicedRefunds ?? 0;
    const platformServiceFee = data.platformServiceFee ?? 0;
    const azPlatformServiceFee = data.azPlatformServiceFee ?? 0;

    // Toplam kesilen faturalar
    const totalInvoicedDeductions =
      platformServiceFee +
      azPlatformServiceFee +
      invoicedAdvertisingFees +
      invoicedPenaltyFees +
      invoicedInternationalFees +
      invoicedOtherFees -
      invoicedRefunds;

    // Ekstra giderler (kullanıcı tanımlı)
    const userExpenses = data.totalExpenseAmount ?? 0;
    const expensesByCategory = data.expensesByCategory ?? {};

    return {
      totalRevenue: data.totalRevenue,
      totalProductsSold: data.totalProductsSold,
      totalOrders: data.totalOrders,
      returnCount: data.returnCount,
      returnCost: data.returnCost,
      totalEstimatedCommission: data.totalEstimatedCommission,
      totalProductCosts: data.totalProductCosts,
      grossProfit: data.grossProfit,
      netProfit,
      margin,
      roi,
      // Gider kırılımı
      isProductFiltered: false,
      accountLevelExpenses,
      totalExpenseBreakdown,
      // İndirimler
      sellerDiscount,
      platformDiscount,
      couponDiscount,
      totalDiscount,
      netRevenue,
      // Stopaj
      stoppage,
      // Kargo
      shippingCost: accountLevelExpenses.shippingCost,
      // Kesilen Faturalar
      platformServiceFee,
      azPlatformServiceFee,
      invoicedAdvertisingFees,
      invoicedPenaltyFees,
      invoicedInternationalFees,
      invoicedOtherFees,
      invoicedRefunds,
      totalInvoicedDeductions,
      // Ekstra Giderler
      userExpenses,
      expensesByCategory,
    };
  }, [stats, selectedProducts]);

  if (isLoading) {
    return (
      <div className="bg-card rounded-lg border border-border">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 p-6 items-start">
          {/* Sol: Grafik Alanı */}
          <div className="lg:col-span-2">
            <Skeleton className="h-[480px] w-full" />
          </div>

          {/* Sağ: Özet Panel */}
          <div className="space-y-1 min-h-[480px]">
            <h3 className="font-semibold text-foreground mb-3">Bu Ay Özet</h3>
            {[...Array(12)].map((_, i) => (
              <MetricRowSkeleton key={i} />
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-card rounded-lg border border-border">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 p-6 items-start">
        {/* Sol: Grafik Alanı */}
        <div className="lg:col-span-2">
          <h3 className="font-semibold text-foreground mb-4">Günlük Performans</h3>
          <DashboardChart data={dailyData} />
        </div>

        {/* Sağ: Özet Panel */}
        <div className="space-y-1 min-h-[480px]">
          <h3 className="font-semibold text-foreground mb-3">Bu Ay Özet</h3>

          {summary ? (
            <>
              {/* ==================== SATIŞLAR ==================== */}
              <MetricRow
                label="Satışlar (Brüt Ciro)"
                value={formatCurrency(summary.totalRevenue)}
                isBold
              />
              <MetricRow
                label="Satış Adedi"
                value={summary.totalProductsSold.toLocaleString("tr-TR")}
              />
              <MetricRow
                label="Sipariş Sayısı"
                value={summary.totalOrders.toLocaleString("tr-TR")}
              />

              {/* ==================== İNDİRİMLER ==================== */}
              {!summary.isProductFiltered && (
                <ExpandableMetricRow
                  label="İndirimler & Kuponlar"
                  value={formatCurrency(-((summary.sellerDiscount ?? 0) + (summary.platformDiscount ?? 0) + (summary.couponDiscount ?? 0)))}
                  isNegative={(summary.sellerDiscount ?? 0) + (summary.platformDiscount ?? 0) + (summary.couponDiscount ?? 0) > 0}
                >
                  <SubRow
                    label="Satıcı İndirimi"
                    value={formatCurrency(-(summary.sellerDiscount ?? 0))}
                    isNegative={(summary.sellerDiscount ?? 0) > 0}
                  />
                  <SubRow
                    label="Platform İndirimi"
                    value={formatCurrency(-(summary.platformDiscount ?? 0))}
                    isNegative={(summary.platformDiscount ?? 0) > 0}
                  />
                  <SubRow
                    label="Kupon İndirimi"
                    value={formatCurrency(-(summary.couponDiscount ?? 0))}
                    isNegative={(summary.couponDiscount ?? 0) > 0}
                  />
                </ExpandableMetricRow>
              )}

              {/* Net Ciro */}
              {!summary.isProductFiltered && (
                <MetricRow
                  label="Net Ciro"
                  value={formatCurrency(summary.netRevenue ?? (summary.totalRevenue - (summary.sellerDiscount ?? 0) - (summary.platformDiscount ?? 0) - (summary.couponDiscount ?? 0)))}
                  isBold
                />
              )}

              {/* Divider */}
              <div className="h-1 bg-muted my-1" />

              {/* ==================== MALİYETLER ==================== */}
              {/* Komisyon */}
              <MetricRow
                label="Komisyon"
                value={formatCurrency(-summary.totalEstimatedCommission)}
                isNegative={summary.totalEstimatedCommission > 0}
              />

              {/* Kargo Maliyeti */}
              <MetricRow
                label="Kargo Maliyeti"
                value={formatCurrency(-(summary.isProductFiltered ? (summary.productShippingCost ?? 0) : (summary.shippingCost ?? 0)))}
                isNegative={(summary.isProductFiltered ? (summary.productShippingCost ?? 0) : (summary.shippingCost ?? 0)) > 0}
              />

              {/* Ürün Maliyeti */}
              <MetricRow
                label="Ürün Maliyeti"
                value={formatCurrency(-summary.totalProductCosts)}
                isNegative={summary.totalProductCosts > 0}
              />


              {/* Stopaj */}
              {!summary.isProductFiltered && (
                <MetricRow
                  label="Stopaj"
                  value={formatCurrency(-(summary.stoppage ?? 0))}
                  isNegative={(summary.stoppage ?? 0) > 0}
                />
              )}

              {/* Divider */}
              <div className="h-1 bg-muted my-1" />

              {/* ==================== KESİLEN FATURALAR ==================== */}
              {summary.isProductFiltered ? (
                // Ürün filtresinde: Bilgi notu ile göster
                <ExpandableMetricRow
                  label="Kesilen Faturalar"
                  value="—"
                  infoText="Reklam, platform hizmet bedeli vb. kesintiler ürün bazlı değildir"
                >
                  <div className="flex items-center gap-1.5 py-2 px-4 text-xs text-muted-foreground">
                    <Info className="h-3.5 w-3.5 flex-shrink-0" />
                    <span>
                      Bu kesintiler hesap bazlıdır, tek bir ürüne atfedilemez.
                      Dönem toplamı: {formatCurrency(
                        (summary.accountLevelExpenses?.advertisingFees ?? 0) +
                        (summary.accountLevelExpenses?.platformServiceFee ?? 0) +
                        (summary.accountLevelExpenses?.penaltyFees ?? 0) +
                        (summary.accountLevelExpenses?.internationalFees ?? 0) +
                        (summary.accountLevelExpenses?.otherFees ?? 0)
                      )}
                    </span>
                  </div>
                </ExpandableMetricRow>
              ) : (
                <ExpandableMetricRow
                  label="Kesilen Faturalar"
                  value={formatCurrency(-(summary.totalInvoicedDeductions ?? 0))}
                  isNegative
                >
                  {(summary.platformServiceFee ?? 0) > 0 && (
                    <SubRow
                      label="Platform Hizmet Bedeli"
                      value={formatCurrency(-(summary.platformServiceFee ?? 0))}
                      isNegative
                    />
                  )}
                  {(summary.azPlatformServiceFee ?? 0) > 0 && (
                    <SubRow
                      label="AZ-Platform Hizmet Bedeli"
                      value={formatCurrency(-(summary.azPlatformServiceFee ?? 0))}
                      isNegative
                    />
                  )}
                  {(summary.invoicedAdvertisingFees ?? 0) > 0 && (
                    <SubRow
                      label="Reklam Bedeli"
                      value={formatCurrency(-(summary.invoicedAdvertisingFees ?? 0))}
                      isNegative
                    />
                  )}
                  {(summary.invoicedPenaltyFees ?? 0) > 0 && (
                    <SubRow
                      label="Ceza / Kesintiler"
                      value={formatCurrency(-(summary.invoicedPenaltyFees ?? 0))}
                      isNegative
                    />
                  )}
                  {(summary.invoicedInternationalFees ?? 0) > 0 && (
                    <SubRow
                      label="Uluslararası Kesintiler"
                      value={formatCurrency(-(summary.invoicedInternationalFees ?? 0))}
                      isNegative
                    />
                  )}
                  {(summary.invoicedOtherFees ?? 0) > 0 && (
                    <SubRow
                      label="Diğer Kesintiler"
                      value={formatCurrency(-(summary.invoicedOtherFees ?? 0))}
                      isNegative
                    />
                  )}
                  {(summary.invoicedRefunds ?? 0) > 0 && (
                    <SubRow
                      label="İadeler (Tazmin)"
                      value={formatCurrency(summary.invoicedRefunds ?? 0)}
                    />
                  )}
                </ExpandableMetricRow>
              )}

              {/* Divider */}
              <div className="h-1 bg-muted my-1" />

              {/* ==================== KÂR ==================== */}
              {/* Brüt Kâr */}
              <MetricRow
                label="Brüt Kâr"
                value={formatCurrency(summary.grossProfit)}
                isNegative={summary.grossProfit < 0}
                isBold
              />

              {/* Ekstra Giderler */}
              {!summary.isProductFiltered && (summary.userExpenses ?? 0) > 0 && (
                <ExpandableMetricRow
                  label="Ekstra Giderler"
                  value={formatCurrency(-(summary.userExpenses ?? 0))}
                  isNegative
                >
                  {summary.expensesByCategory && Object.keys(summary.expensesByCategory).length > 0 ? (
                    Object.entries(summary.expensesByCategory)
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
                    <SubRow
                      label="Toplam"
                      value={formatCurrency(-(summary.userExpenses ?? 0))}
                      isNegative
                    />
                  )}
                </ExpandableMetricRow>
              )}

              {/* Divider - Strong border before Net Profit */}
              <div className="h-2 bg-primary/10 my-1" />

              {/* Net Kâr - Highlighted */}
              <div className="bg-primary/5 -mx-1 px-1">
                <MetricRow
                  label="Net Kâr"
                  value={formatCurrency(summary.netProfit)}
                  isNegative={summary.netProfit < 0}
                  isBold
                />
              </div>

              {/* Divider */}
              <div className="h-1 bg-muted my-1" />

              {/* ==================== METRİKLER ==================== */}
              <MetricRow
                label="Kâr Marjı"
                value={formatPercentage(summary.margin)}
                isNegative={summary.margin < 0}
                isBold
              />
              <MetricRow
                label="ROI"
                value={formatPercentage(summary.roi)}
                isNegative={summary.roi < 0}
                isBold
              />

            </>
          ) : (
            <p className="text-muted-foreground text-sm">Veri bulunamadı</p>
          )}
        </div>
      </div>
    </div>
  );
}
