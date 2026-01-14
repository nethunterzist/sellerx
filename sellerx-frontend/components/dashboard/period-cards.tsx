"use client";

import { PeriodCard } from "./period-card";
import { Skeleton } from "@/components/ui/skeleton";
import type { DashboardStatsResponse, DashboardStats } from "@/types/dashboard";

interface PeriodCardsProps {
  stats?: DashboardStatsResponse;
  isLoading?: boolean;
}

function getDateRange(type: "today" | "yesterday" | "mtd" | "lastMonth"): string {
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
    advCost: 0, // Backend'de yok - ileride eklenebilir
    estPayout: data.totalRevenue - data.totalEstimatedCommission - data.totalStoppage,
    grossProfit: data.grossProfit,
    netProfit: data.grossProfit - data.totalExpenseAmount,
  };
}

// Skeleton card for loading state
function PeriodCardSkeleton({ isToday = false }: { isToday?: boolean }) {
  return (
    <div
      className={`flex flex-col rounded-lg p-4 min-w-[180px] flex-1 ${
        isToday ? "bg-[#1D70F1]" : "bg-white border border-[#DDDDDD]"
      }`}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-2">
        <div>
          <Skeleton className={`h-4 w-16 mb-1 ${isToday ? "bg-white/20" : ""}`} />
          <Skeleton className={`h-3 w-24 ${isToday ? "bg-white/20" : ""}`} />
        </div>
      </div>

      {/* Sales */}
      <div className="mb-3">
        <Skeleton className={`h-3 w-12 mb-1 ${isToday ? "bg-white/20" : ""}`} />
        <Skeleton className={`h-8 w-32 ${isToday ? "bg-white/20" : ""}`} />
      </div>

      {/* Orders & Refunds */}
      <div className="flex justify-between mb-2">
        <div>
          <Skeleton className={`h-3 w-16 mb-1 ${isToday ? "bg-white/20" : ""}`} />
          <Skeleton className={`h-4 w-10 ${isToday ? "bg-white/20" : ""}`} />
        </div>
        <div className="text-right">
          <Skeleton className={`h-3 w-12 mb-1 ${isToday ? "bg-white/20" : ""}`} />
          <Skeleton className={`h-4 w-6 ${isToday ? "bg-white/20" : ""}`} />
        </div>
      </div>

      {/* Adv Cost & Est Payout */}
      <div className="flex justify-between mb-2">
        <div>
          <Skeleton className={`h-3 w-20 mb-1 ${isToday ? "bg-white/20" : ""}`} />
          <Skeleton className={`h-4 w-16 ${isToday ? "bg-white/20" : ""}`} />
        </div>
        <div className="text-right">
          <Skeleton className={`h-3 w-16 mb-1 ${isToday ? "bg-white/20" : ""}`} />
          <Skeleton className={`h-4 w-20 ${isToday ? "bg-white/20" : ""}`} />
        </div>
      </div>

      {/* Gross & Net Profit */}
      <div
        className="flex justify-between pt-2 mt-auto border-t"
        style={{ borderColor: isToday ? "rgba(255,255,255,0.2)" : "#DDDDDD" }}
      >
        <div>
          <Skeleton className={`h-3 w-14 mb-1 ${isToday ? "bg-white/20" : ""}`} />
          <Skeleton className={`h-4 w-20 ${isToday ? "bg-white/20" : ""}`} />
        </div>
        <div className="text-right">
          <Skeleton className={`h-3 w-12 mb-1 ${isToday ? "bg-white/20" : ""}`} />
          <Skeleton className={`h-4 w-20 ${isToday ? "bg-white/20" : ""}`} />
        </div>
      </div>

      {/* More Link */}
      <Skeleton className={`h-3 w-10 mt-3 ${isToday ? "bg-white/20" : ""}`} />
    </div>
  );
}

export function PeriodCards({ stats, isLoading }: PeriodCardsProps) {
  // Show skeletons when loading
  if (isLoading) {
    return (
      <div className="flex gap-4 overflow-x-auto pb-2">
        <PeriodCardSkeleton isToday />
        <PeriodCardSkeleton />
        <PeriodCardSkeleton />
        <PeriodCardSkeleton />
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

  // Calculate percentage changes if we have real data
  const monthlyChange = thisMonth && lastMonth && lastMonth.netProfit !== 0
    ? ((thisMonth.netProfit - lastMonth.netProfit) / lastMonth.netProfit) * 100
    : undefined;

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

  return (
    <div className="flex gap-4 overflow-x-auto pb-2">
      {today && (
        <PeriodCard
          title="Bugün"
          dateRange={getDateRange("today")}
          sales={today.sales}
          ordersUnits={`${today.orders} / ${today.units}`}
          refunds={today.refunds}
          advCost={today.advCost}
          estPayout={today.estPayout}
          grossProfit={today.grossProfit}
          netProfit={today.netProfit}
          isToday={true}
        />
      )}
      {yesterday && (
        <PeriodCard
          title="Dün"
          dateRange={getDateRange("yesterday")}
          sales={yesterday.sales}
          ordersUnits={`${yesterday.orders} / ${yesterday.units}`}
          refunds={yesterday.refunds}
          advCost={yesterday.advCost}
          estPayout={yesterday.estPayout}
          grossProfit={yesterday.grossProfit}
          netProfit={yesterday.netProfit}
        />
      )}
      {thisMonth && (
        <PeriodCard
          title="Bu Ay"
          dateRange={getDateRange("mtd")}
          sales={thisMonth.sales}
          ordersUnits={`${thisMonth.orders} / ${thisMonth.units}`}
          refunds={thisMonth.refunds}
          advCost={thisMonth.advCost}
          estPayout={thisMonth.estPayout}
          grossProfit={thisMonth.grossProfit}
          netProfit={thisMonth.netProfit}
          percentageChange={monthlyChange}
        />
      )}
      {lastMonth && (
        <PeriodCard
          title="Geçen Ay"
          dateRange={getDateRange("lastMonth")}
          sales={lastMonth.sales}
          ordersUnits={`${lastMonth.orders} / ${lastMonth.units}`}
          refunds={lastMonth.refunds}
          advCost={lastMonth.advCost}
          estPayout={lastMonth.estPayout}
          grossProfit={lastMonth.grossProfit}
          netProfit={lastMonth.netProfit}
        />
      )}
    </div>
  );
}
