"use client";

import React, { useState } from "react";
import { motion } from "motion/react";
import { PeriodCard } from "./period-card";
import { Skeleton } from "@/components/ui/skeleton";
import { PeriodDetailModal, type PeriodDetailStats } from "./period-detail-modal";
import type { DashboardStatsResponse, DashboardStats } from "@/types/dashboard";

export type PeriodType = "today" | "yesterday" | "thisMonth" | "thisMonthForecast" | "lastMonth";

// Dynamic period data from multi-period hook
export interface DynamicPeriodData {
  stats: DashboardStats | undefined;
  label: string;
  shortLabel: string;
  dateRange: string;
  color: "blue" | "teal" | "green" | "orange" | "purple" | "pink";
  /** ISO date string for API calls (YYYY-MM-DD) */
  startDate?: string;
  /** ISO date string for API calls (YYYY-MM-DD) */
  endDate?: string;
}

interface PeriodCardsPropsMulti {
  /** Display mode - "multi" shows 4 cards, "single" shows 1 custom card, "dynamic" shows multiple preset cards */
  mode?: "multi";
  stats?: DashboardStatsResponse;
  isLoading?: boolean;
  selectedPeriod?: PeriodType;
  onPeriodSelect?: (period: PeriodType) => void;
  /** Store ID for fetching deduction breakdown in modal */
  storeId?: string;
}

interface PeriodCardsPropsSingle {
  /** Display mode - "multi" shows 4 cards, "single" shows 1 custom card */
  mode: "single";
  /** Stats for single card mode */
  customStats?: DashboardStats;
  /** Title for single card (e.g., "Son 7 Gün") */
  customTitle?: string;
  /** Date range for single card (e.g., "10 Oca - 16 Oca 2025") */
  customDateRange?: string;
  isLoading?: boolean;
  /** Store ID for fetching deduction breakdown in modal */
  storeId?: string;
  /** Start date in ISO format for API calls */
  startDate?: string;
  /** End date in ISO format for API calls */
  endDate?: string;
}

interface PeriodCardsPropsDynamic {
  /** Display mode - "dynamic" for multi-period presets like weeks, months, quarters */
  mode: "dynamic";
  /** Array of period data from useDashboardStatsByPreset hook */
  periodData?: DynamicPeriodData[];
  isLoading?: boolean;
  /** Selected period index for filtering products table */
  selectedIndex?: number;
  onPeriodSelect?: (index: number) => void;
  /** Store ID for fetching deduction breakdown in modal */
  storeId?: string;
}

type PeriodCardsProps = PeriodCardsPropsMulti | PeriodCardsPropsSingle | PeriodCardsPropsDynamic;

function getDateRange(type: "today" | "yesterday" | "mtd" | "mtdForecast" | "lastMonth"): string {
  const now = new Date();
  const options: Intl.DateTimeFormatOptions = { day: "numeric", month: "long", year: "numeric" };

  switch (type) {
    case "today":
      return now.toLocaleDateString("tr-TR", options);
    case "yesterday": {
      const yesterday = new Date(now);
      yesterday.setDate(yesterday.getDate() - 1);
      return yesterday.toLocaleDateString("tr-TR", options);
    }
    case "mtd": {
      const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
      return `${firstDay.toLocaleDateString("tr-TR", { day: "numeric" })}-${now.toLocaleDateString("tr-TR", options)}`;
    }
    case "mtdForecast": {
      const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
      const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
      return `${firstDay.toLocaleDateString("tr-TR", { day: "numeric" })}-${lastDay.toLocaleDateString("tr-TR", options)}`;
    }
    case "lastMonth": {
      const firstDay = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const lastDay = new Date(now.getFullYear(), now.getMonth(), 0);
      return `${firstDay.toLocaleDateString("tr-TR", { day: "numeric" })}-${lastDay.toLocaleDateString("tr-TR", options)}`;
    }
  }
}

// Transform backend DashboardStats to UI format
function mapToCardData(data: DashboardStats) {
  return {
    sales: data.totalRevenue,
    orders: data.totalOrders,
    units: data.totalProductsSold,
    refunds: data.returnCount,
    invoicedDeductions: data.invoicedDeductions ?? 0, // Kesilen Faturalar (REKLAM + CEZA + ULUSLARARASI + DIGER - IADE)
    grossProfit: data.grossProfit,
    netProfit: data.netProfit, // Use backend-calculated net profit (P0 fix)
    // Financial details from backend
    vatDifference: data.vatDifference,
    stoppage: data.totalStoppage,
    commission: data.totalEstimatedCommission,
    productCosts: data.totalProductCosts,
    shippingCost: data.totalShippingCost ?? 0, // Kargo maliyeti
    itemsWithoutCost: data.itemsWithoutCost,
  };
}

// Calculate forecast based on MTD (Month-to-Date) data using linear projection
function calculateForecast(mtdCardData: ReturnType<typeof mapToCardData>) {
  const now = new Date();
  const currentDay = now.getDate(); // Ayın kaçıncı günü (1-31)
  const totalDaysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();

  // Multiplier for linear projection
  const multiplier = totalDaysInMonth / currentDay;

  return {
    sales: mtdCardData.sales * multiplier,
    orders: Math.round(mtdCardData.orders * multiplier),
    units: Math.round(mtdCardData.units * multiplier),
    refunds: Math.round(mtdCardData.refunds * multiplier),
    invoicedDeductions: mtdCardData.invoicedDeductions * multiplier, // Kesilen faturalar tahmini
    grossProfit: mtdCardData.grossProfit * multiplier,
    netProfit: mtdCardData.netProfit * multiplier,
    vatDifference: mtdCardData.vatDifference * multiplier,
    stoppage: mtdCardData.stoppage * multiplier,
    commission: mtdCardData.commission * multiplier,
    productCosts: mtdCardData.productCosts * multiplier,
    shippingCost: mtdCardData.shippingCost * multiplier, // Kargo maliyeti tahmini
    itemsWithoutCost: mtdCardData.itemsWithoutCost, // Bu değişmez
  };
}

// Skeleton card for loading state
function PeriodCardSkeleton({ variant = "today" }: { variant?: "today" | "yesterday" | "thisMonth" | "thisMonthForecast" | "lastMonth" }) {
  const headerColors = {
    today: "bg-[#3B82F6]",
    yesterday: "bg-[#14B8A6]",
    thisMonth: "bg-[#0D9488]",
    thisMonthForecast: "bg-[#047857]",
    lastMonth: "bg-[#F59E0B]",
  };

  return (
    <div className="flex flex-col rounded-lg overflow-hidden min-w-[220px] flex-1 bg-card border border-border shadow-sm">
      {/* Colored Header */}
      <div className={`px-4 py-3 ${headerColors[variant]}`}>
        <Skeleton className="h-4 w-16 mb-1 bg-white/20" />
        <Skeleton className="h-3 w-24 bg-white/20" />
      </div>

      {/* Card Body */}
      <div className="p-4">
        {/* Sales */}
        <div className="mb-4">
          <Skeleton className="h-3 w-12 mb-1" />
          <Skeleton className="h-8 w-32" />
        </div>

        {/* Orders & Refunds */}
        <div className="flex justify-between mb-3">
          <div>
            <Skeleton className="h-3 w-16 mb-1" />
            <Skeleton className="h-4 w-10" />
          </div>
          <div className="text-right">
            <Skeleton className="h-3 w-12 mb-1" />
            <Skeleton className="h-4 w-6" />
          </div>
        </div>

        {/* Adv Cost & Est Payout */}
        <div className="flex justify-between mb-3">
          <div>
            <Skeleton className="h-3 w-20 mb-1" />
            <Skeleton className="h-4 w-16" />
          </div>
          <div className="text-right">
            <Skeleton className="h-3 w-16 mb-1" />
            <Skeleton className="h-4 w-20" />
          </div>
        </div>

        {/* Gross & Net Profit */}
        <div className="flex justify-between pt-3 border-t border-border">
          <div>
            <Skeleton className="h-3 w-14 mb-1" />
            <Skeleton className="h-4 w-20" />
          </div>
          <div className="text-right">
            <Skeleton className="h-3 w-12 mb-1" />
            <Skeleton className="h-4 w-20" />
          </div>
        </div>

        {/* More Link */}
        <Skeleton className="h-3 w-10 mt-3" />
      </div>
    </div>
  );
}

// Map color to variant for PeriodCard and PeriodCardSkeleton
type SkeletonVariant = "today" | "yesterday" | "thisMonth" | "thisMonthForecast" | "lastMonth";
function colorToVariant(color: string): SkeletonVariant {
  const colorMap: Record<string, SkeletonVariant> = {
    blue: "today",
    teal: "yesterday",
    green: "thisMonth",
    orange: "lastMonth",
    purple: "thisMonthForecast",
    pink: "lastMonth",
  };
  return colorMap[color] || "today";
}

// Map variant to header color for modal
function variantToHeaderColor(variant: SkeletonVariant): string {
  const colorMap: Record<SkeletonVariant, string> = {
    today: "bg-[#3B82F6]",
    yesterday: "bg-[#14B8A6]",
    thisMonth: "bg-[#0D9488]",
    thisMonthForecast: "bg-[#047857]",
    lastMonth: "bg-[#F59E0B]",
  };
  return colorMap[variant] || "bg-[#3B82F6]";
}

// Convert DashboardStats to PeriodDetailStats for modal
function mapToDetailStats(data: DashboardStats): PeriodDetailStats {
  return {
    // Temel metrikler
    sales: data.totalRevenue,
    units: data.totalProductsSold,
    orders: data.totalOrders,
    refunds: data.returnCount,
    refundCost: data.returnCost,
    commission: data.totalEstimatedCommission,
    productCosts: data.totalProductCosts,
    stoppage: data.totalStoppage,
    vatDifference: data.vatDifference,
    grossProfit: data.grossProfit,
    expenses: data.totalExpenseAmount,
    netProfit: data.netProfit,
    itemsWithoutCost: data.itemsWithoutCost,

    // ============== YENİ ALANLAR (32 Metrik) ==============

    // Kesilen Faturalar (Detay Modal için)
    invoicedDeductions: data.invoicedDeductions ?? 0,
    invoicedAdvertisingFees: data.invoicedAdvertisingFees ?? 0,
    invoicedPenaltyFees: data.invoicedPenaltyFees ?? 0,
    invoicedInternationalFees: data.invoicedInternationalFees ?? 0,
    invoicedOtherFees: data.invoicedOtherFees ?? 0,
    invoicedRefunds: data.invoicedRefunds ?? 0,

    // İndirimler & Kuponlar
    sellerDiscount: data.totalSellerDiscount ?? 0,
    platformDiscount: data.totalPlatformDiscount ?? 0,
    couponDiscount: data.totalCouponDiscount ?? 0,

    // Net Ciro
    netRevenue: data.netRevenue ?? (data.totalRevenue - (data.totalSellerDiscount ?? 0) - (data.totalPlatformDiscount ?? 0) - (data.totalCouponDiscount ?? 0)),

    // Kargo
    shippingCost: data.totalShippingCost ?? 0,

    // Platform Ücretleri (15 kategori)
    internationalServiceFee: data.internationalServiceFee ?? 0,
    overseasOperationFee: data.overseasOperationFee ?? 0,
    terminDelayFee: data.terminDelayFee ?? 0,
    platformServiceFee: data.platformServiceFee ?? 0,
    invoiceCreditFee: data.invoiceCreditFee ?? 0,
    unsuppliedFee: data.unsuppliedFee ?? 0,
    azOverseasOperationFee: data.azOverseasOperationFee ?? 0,
    azPlatformServiceFee: data.azPlatformServiceFee ?? 0,
    packagingServiceFee: data.packagingServiceFee ?? 0,
    warehouseServiceFee: data.warehouseServiceFee ?? 0,
    callCenterFee: data.callCenterFee ?? 0,
    photoShootingFee: data.photoShootingFee ?? 0,
    integrationFee: data.integrationFee ?? 0,
    storageServiceFee: data.storageServiceFee ?? 0,
    otherPlatformFees: data.otherPlatformFees ?? 0,

    // Erken Ödeme
    earlyPaymentFee: data.earlyPaymentFee ?? 0,

    // Gider Kategorileri (eski - geriye uyumluluk)
    officeExpenses: data.officeExpenses ?? 0,
    packagingExpenses: data.packagingExpenses ?? 0,
    accountingExpenses: data.accountingExpenses ?? 0,
    otherExpenses: data.otherExpenses ?? 0,

    // Dinamik gider kategorileri
    expensesByCategory: data.expensesByCategory,

    // İade Detayları
    refundRate: data.refundRate ?? 0,

    // ROI
    roi: data.roi ?? 0,
  };
}

// Detail modal state interface
interface DetailModalState {
  open: boolean;
  title: string;
  dateRange: string;
  stats: PeriodDetailStats | null;
  headerColor: string;
  storeId?: string;
  startDate?: string;
  endDate?: string;
}

export function PeriodCards(props: PeriodCardsProps) {
  // Modal state
  const [detailModal, setDetailModal] = useState<DetailModalState>({
    open: false,
    title: "",
    dateRange: "",
    stats: null,
    headerColor: "bg-[#3B82F6]",
    storeId: undefined,
    startDate: undefined,
    endDate: undefined,
  });

  // Helper to open detail modal
  const openDetailModal = (
    title: string,
    dateRange: string,
    stats: DashboardStats,
    variant: SkeletonVariant,
    storeId?: string,
    startDate?: string,
    endDate?: string
  ) => {
    setDetailModal({
      open: true,
      title,
      dateRange,
      stats: mapToDetailStats(stats),
      headerColor: variantToHeaderColor(variant),
      storeId,
      startDate,
      endDate,
    });
  };
  // Dynamic mode for multi-period presets (weeks, months, quarters, etc.)
  if (props.mode === "dynamic") {
    const { periodData, isLoading, selectedIndex = 0, onPeriodSelect, storeId } = props;

    if (isLoading) {
      return (
        <div className="flex gap-4 overflow-x-auto pb-2">
          <PeriodCardSkeleton variant="today" />
          <PeriodCardSkeleton variant="yesterday" />
          <PeriodCardSkeleton variant="thisMonth" />
          <PeriodCardSkeleton variant="lastMonth" />
        </div>
      );
    }

    if (!periodData || periodData.length === 0) {
      return (
        <div className="flex gap-4 overflow-x-auto pb-2">
          <div className="flex-1 min-w-[180px] p-8 bg-card border border-border rounded-lg text-center text-muted-foreground">
            Henüz veri yok
          </div>
        </div>
      );
    }

    return (
      <>
        <div className="flex gap-4 overflow-x-auto pb-2">
          {periodData.map((period, index) => {
            if (!period.stats) {
              return (
                <PeriodCardSkeleton key={index} variant={colorToVariant(period.color)} />
              );
            }

            const cardData = mapToCardData(period.stats);
            const variant = colorToVariant(period.color);

            return (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.25, delay: index * 0.05, ease: [0.25, 0.1, 0.25, 1] as const }}
                className="flex-1 min-w-0"
              >
              <PeriodCard
                title={period.shortLabel}
                dateRange={period.dateRange}
                sales={cardData.sales}
                ordersUnits={`${cardData.orders} / ${cardData.units}`}
                refunds={cardData.refunds}
                invoicedDeductions={cardData.invoicedDeductions}
                grossProfit={cardData.grossProfit}
                netProfit={cardData.netProfit}
                commission={cardData.commission}
                productCosts={cardData.productCosts}
                shippingCost={cardData.shippingCost}
                itemsWithoutCost={cardData.itemsWithoutCost}
                variant={variant}
                isSelected={selectedIndex === index}
                onClick={() => onPeriodSelect?.(index)}
                onDetailClick={() => openDetailModal(period.shortLabel, period.dateRange, period.stats!, variant, storeId, period.startDate, period.endDate)}
              />
              </motion.div>
            );
          })}
        </div>
        <PeriodDetailModal
          open={detailModal.open}
          onOpenChange={(open) => setDetailModal((prev) => ({ ...prev, open }))}
          title={detailModal.title}
          dateRange={detailModal.dateRange}
          stats={detailModal.stats}
          headerColor={detailModal.headerColor}
          storeId={detailModal.storeId}
          startDate={detailModal.startDate}
          endDate={detailModal.endDate}
        />
      </>
    );
  }

  // Single card mode for custom date ranges
  if (props.mode === "single") {
    const { customStats, customTitle, customDateRange, isLoading, storeId, startDate, endDate } = props;

    if (isLoading) {
      return (
        <div className="flex gap-4 overflow-x-auto pb-2">
          <PeriodCardSkeleton variant="today" />
        </div>
      );
    }

    if (!customStats) {
      return (
        <div className="flex gap-4 overflow-x-auto pb-2">
          <div className="flex-1 min-w-[180px] p-8 bg-card border border-border rounded-lg text-center text-muted-foreground">
            Henüz veri yok
          </div>
        </div>
      );
    }

    const cardData = mapToCardData(customStats);
    const title = customTitle || "Ozel Aralik";
    const dateRangeStr = customDateRange || "";

    return (
      <>
        <div className="flex gap-4 overflow-x-auto pb-2">
          <PeriodCard
            title={title}
            dateRange={dateRangeStr}
            sales={cardData.sales}
            ordersUnits={`${cardData.orders} / ${cardData.units}`}
            refunds={cardData.refunds}
            invoicedDeductions={cardData.invoicedDeductions}
            grossProfit={cardData.grossProfit}
            netProfit={cardData.netProfit}
            commission={cardData.commission}
            productCosts={cardData.productCosts}
            shippingCost={cardData.shippingCost}
            itemsWithoutCost={cardData.itemsWithoutCost}
            variant="custom"
            isSelected={true}
            onDetailClick={() => openDetailModal(title, dateRangeStr, customStats, "today", storeId, startDate, endDate)}
          />
        </div>
        <PeriodDetailModal
          open={detailModal.open}
          onOpenChange={(open) => setDetailModal((prev) => ({ ...prev, open }))}
          title={detailModal.title}
          dateRange={detailModal.dateRange}
          stats={detailModal.stats}
          headerColor={detailModal.headerColor}
          storeId={detailModal.storeId}
          startDate={detailModal.startDate}
          endDate={detailModal.endDate}
        />
      </>
    );
  }

  // Multi card mode (default) - original implementation
  const { stats, isLoading, selectedPeriod = "today", onPeriodSelect, storeId } = props;

  // Helper to get ISO date range for API calls
  const getIsoDateRange = (type: "today" | "yesterday" | "mtd" | "mtdForecast" | "lastMonth"): { startDate: string; endDate: string } => {
    const now = new Date();
    const formatDate = (d: Date) => d.toISOString().split("T")[0]; // YYYY-MM-DD

    switch (type) {
      case "today":
        return { startDate: formatDate(now), endDate: formatDate(now) };
      case "yesterday": {
        const yesterday = new Date(now);
        yesterday.setDate(yesterday.getDate() - 1);
        return { startDate: formatDate(yesterday), endDate: formatDate(yesterday) };
      }
      case "mtd":
      case "mtdForecast": {
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        return { startDate: formatDate(firstDay), endDate: formatDate(now) };
      }
      case "lastMonth": {
        const firstDay = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        const lastDay = new Date(now.getFullYear(), now.getMonth(), 0);
        return { startDate: formatDate(firstDay), endDate: formatDate(lastDay) };
      }
    }
  };

  // Show skeletons when loading
  if (isLoading) {
    return (
      <div className="flex gap-4 overflow-x-auto pb-2">
        <PeriodCardSkeleton variant="today" />
        <PeriodCardSkeleton variant="yesterday" />
        <PeriodCardSkeleton variant="thisMonth" />
        <PeriodCardSkeleton variant="thisMonthForecast" />
        <PeriodCardSkeleton variant="lastMonth" />
      </div>
    );
  }

  // Use backend data if available, show empty state if no data
  if (!stats) {
    return (
      <div className="flex gap-4 overflow-x-auto pb-2">
        <div className="flex-1 min-w-[180px] p-8 bg-white border border-[#DDDDDD] rounded-lg text-center text-gray-500">
          Henüz veri yok
        </div>
      </div>
    );
  }

  const today = stats.today ? mapToCardData(stats.today) : null;
  const yesterday = stats.yesterday ? mapToCardData(stats.yesterday) : null;
  const thisMonth = stats.thisMonth ? mapToCardData(stats.thisMonth) : null;
  const lastMonth = stats.lastMonth ? mapToCardData(stats.lastMonth) : null;

  // Calculate forecast based on MTD data
  const forecast = thisMonth ? calculateForecast(thisMonth) : null;

  // If no data at all, show empty state
  if (!today && !yesterday && !thisMonth && !lastMonth) {
    return (
      <div className="flex gap-4 overflow-x-auto pb-2">
        <div className="flex-1 min-w-[180px] p-8 bg-white border border-[#DDDDDD] rounded-lg text-center text-gray-500">
          Henüz veri yok
        </div>
      </div>
    );
  }

  // Collect multi-mode cards for stagger indexing
  const multiCards: { key: string; element: React.ReactElement }[] = [];

  if (today && stats.today) {
    multiCards.push({ key: "today", element: (
      <PeriodCard
        title="Bugun"
        dateRange={getDateRange("today")}
        sales={today.sales}
        ordersUnits={`${today.orders} / ${today.units}`}
        refunds={today.refunds}
        invoicedDeductions={today.invoicedDeductions}
        grossProfit={today.grossProfit}
        netProfit={today.netProfit}
        commission={today.commission}
        productCosts={today.productCosts}
        shippingCost={today.shippingCost}
        itemsWithoutCost={today.itemsWithoutCost}
        variant="today"
        isSelected={selectedPeriod === "today"}
        onClick={() => onPeriodSelect?.("today")}
        onDetailClick={() => {
          const dates = getIsoDateRange("today");
          openDetailModal("Bugun", getDateRange("today"), stats.today, "today", storeId, dates.startDate, dates.endDate);
        }}
      />
    )});
  }
  if (yesterday && stats.yesterday) {
    multiCards.push({ key: "yesterday", element: (
      <PeriodCard
        title="Dun"
        dateRange={getDateRange("yesterday")}
        sales={yesterday.sales}
        ordersUnits={`${yesterday.orders} / ${yesterday.units}`}
        refunds={yesterday.refunds}
        invoicedDeductions={yesterday.invoicedDeductions}
        grossProfit={yesterday.grossProfit}
        netProfit={yesterday.netProfit}
        commission={yesterday.commission}
        productCosts={yesterday.productCosts}
        shippingCost={yesterday.shippingCost}
        itemsWithoutCost={yesterday.itemsWithoutCost}
        variant="yesterday"
        isSelected={selectedPeriod === "yesterday"}
        onClick={() => onPeriodSelect?.("yesterday")}
        onDetailClick={() => {
          const dates = getIsoDateRange("yesterday");
          openDetailModal("Dun", getDateRange("yesterday"), stats.yesterday, "yesterday", storeId, dates.startDate, dates.endDate);
        }}
      />
    )});
  }
  if (thisMonth && stats.thisMonth) {
    multiCards.push({ key: "thisMonth", element: (
      <PeriodCard
        title="Bu Ay"
        dateRange={getDateRange("mtd")}
        sales={thisMonth.sales}
        ordersUnits={`${thisMonth.orders} / ${thisMonth.units}`}
        refunds={thisMonth.refunds}
        invoicedDeductions={thisMonth.invoicedDeductions}
        grossProfit={thisMonth.grossProfit}
        netProfit={thisMonth.netProfit}
        commission={thisMonth.commission}
        productCosts={thisMonth.productCosts}
        shippingCost={thisMonth.shippingCost}
        itemsWithoutCost={thisMonth.itemsWithoutCost}
        variant="thisMonth"
        isSelected={selectedPeriod === "thisMonth"}
        onClick={() => onPeriodSelect?.("thisMonth")}
        onDetailClick={() => {
          const dates = getIsoDateRange("mtd");
          openDetailModal("Bu Ay", getDateRange("mtd"), stats.thisMonth, "thisMonth", storeId, dates.startDate, dates.endDate);
        }}
      />
    )});
  }
  if (forecast && stats.thisMonth) {
    multiCards.push({ key: "forecast", element: (
      <PeriodCard
        title="Bu Ay (tahmin)"
        dateRange={getDateRange("mtdForecast")}
        sales={forecast.sales}
        ordersUnits={`${forecast.orders} / ${forecast.units}`}
        refunds={forecast.refunds}
        invoicedDeductions={forecast.invoicedDeductions}
        grossProfit={forecast.grossProfit}
        netProfit={forecast.netProfit}
        commission={forecast.commission}
        productCosts={forecast.productCosts}
        shippingCost={forecast.shippingCost}
        itemsWithoutCost={forecast.itemsWithoutCost}
        variant="thisMonthForecast"
        disabled={true}
        onDetailClick={() => {
          const dates = getIsoDateRange("mtdForecast");
          openDetailModal("Bu Ay (tahmin)", getDateRange("mtdForecast"), stats.thisMonth, "thisMonthForecast", storeId, dates.startDate, dates.endDate);
        }}
      />
    )});
  }
  if (lastMonth && stats.lastMonth) {
    multiCards.push({ key: "lastMonth", element: (
      <PeriodCard
        title="Gecen Ay"
        dateRange={getDateRange("lastMonth")}
        sales={lastMonth.sales}
        ordersUnits={`${lastMonth.orders} / ${lastMonth.units}`}
        refunds={lastMonth.refunds}
        invoicedDeductions={lastMonth.invoicedDeductions}
        grossProfit={lastMonth.grossProfit}
        netProfit={lastMonth.netProfit}
        commission={lastMonth.commission}
        productCosts={lastMonth.productCosts}
        shippingCost={lastMonth.shippingCost}
        itemsWithoutCost={lastMonth.itemsWithoutCost}
        variant="lastMonth"
        isSelected={selectedPeriod === "lastMonth"}
        onClick={() => onPeriodSelect?.("lastMonth")}
        onDetailClick={() => {
          const dates = getIsoDateRange("lastMonth");
          openDetailModal("Gecen Ay", getDateRange("lastMonth"), stats.lastMonth, "lastMonth", storeId, dates.startDate, dates.endDate);
        }}
      />
    )});
  }

  return (
    <>
      <div className="flex gap-4 overflow-x-auto pb-2">
        {multiCards.map((card, index) => (
          <motion.div
            key={card.key}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25, delay: index * 0.05, ease: [0.25, 0.1, 0.25, 1] as const }}
            className="flex-1 min-w-0"
          >
            {card.element}
          </motion.div>
        ))}
      </div>
      <PeriodDetailModal
        open={detailModal.open}
        onOpenChange={(open) => setDetailModal((prev) => ({ ...prev, open }))}
        title={detailModal.title}
        dateRange={detailModal.dateRange}
        stats={detailModal.stats}
        headerColor={detailModal.headerColor}
        storeId={detailModal.storeId}
        startDate={detailModal.startDate}
        endDate={detailModal.endDate}
      />
    </>
  );
}
