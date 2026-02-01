import { useQuery, useQueries } from "@tanstack/react-query";
import { dashboardApi } from "@/lib/api/client";
import {
  subDays,
  subWeeks,
  subMonths,
  subQuarters,
  startOfDay,
  endOfDay,
  startOfWeek,
  endOfWeek,
  startOfMonth,
  endOfMonth,
  startOfQuarter,
  endOfQuarter,
  format,
} from "date-fns";
import { tr } from "date-fns/locale";
import type {
  DashboardStats,
  DashboardStatsResponse,
  OrderDetail,
  ProductDetail,
  PeriodExpense,
  DateRangePreset,
  PeriodRange,
  MultiPeriodStatsResponse,
  PLPeriodType,
} from "@/types/dashboard";

// Dashboard Query Keys
export const dashboardKeys = {
  all: ["dashboard"] as const,
  stats: (storeId: string) => [...dashboardKeys.all, "stats", storeId] as const,
  statsByRange: (storeId: string, startDate: string, endDate: string) =>
    [...dashboardKeys.all, "stats", "range", storeId, startDate, endDate] as const,
  multiPeriod: (storeId: string, periodType: PLPeriodType, periodCount: number, productBarcode?: string) =>
    [...dashboardKeys.all, "stats", "multi-period", storeId, periodType, periodCount, productBarcode] as const,
};

export const useDashboardStats = (storeId?: string) => {
  return useQuery<DashboardStatsResponse>({
    queryKey: dashboardKeys.stats(storeId!),
    queryFn: () => dashboardApi.getStats(storeId!),
    enabled: !!storeId,
    // PERFORMANCE: Removed refetchInterval - use manual refresh instead
    staleTime: 5 * 60 * 1000, // 5 dakika boyunca fresh kabul et
    gcTime: 15 * 60 * 1000, // 15 dakika cache'te tut
    retry: 2, // 2 kez tekrar dene
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000), // Exponential backoff
    refetchOnWindowFocus: false, // Pencere odaklandığında yenileme
    refetchOnReconnect: true, // İnternet bağlantısı yenilendiğinde çalıştır
  });
};

/**
 * Hook for fetching dashboard stats for a custom date range.
 * Used when user selects presets like "Son 7 Gün" or custom date range.
 */
export const useDashboardStatsByRange = (
  storeId: string | undefined,
  startDate: string | undefined,
  endDate: string | undefined,
  periodLabel?: string,
) => {
  return useQuery<DashboardStats>({
    queryKey: dashboardKeys.statsByRange(storeId!, startDate!, endDate!),
    queryFn: () => dashboardApi.getStatsByRange(storeId!, startDate!, endDate!, periodLabel),
    enabled: !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 dakika boyunca fresh kabul et
    gcTime: 15 * 60 * 1000, // 15 dakika cache'te tut
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000),
    refetchOnWindowFocus: false,
  });
};

// Re-export types for backwards compatibility
export type {
  DashboardStats,
  DashboardStatsResponse,
  OrderDetail,
  ProductDetail,
  PeriodExpense as ExpenseDetail,
};

// Period colors for consistent styling
const PERIOD_COLORS: Array<"blue" | "teal" | "green" | "orange"> = ["blue", "teal", "green", "orange"];

/**
 * Calculate multiple period ranges for a given preset.
 * Returns an array of PeriodRange objects for multi-card display.
 */
export function calculateMultiPeriodRanges(preset: DateRangePreset): PeriodRange[] | null {
  const today = new Date();
  const formatDate = (date: Date) => format(date, "yyyy-MM-dd");
  const formatDisplayDate = (start: Date, end: Date) => {
    const startStr = format(start, "d MMM", { locale: tr });
    const endStr = format(end, "d MMM", { locale: tr });
    return startStr === endStr ? startStr : `${startStr} - ${endStr}`;
  };

  switch (preset) {
    case "default":
      // Default uses the standard 4-period API, not multi-period
      return null;

    case "basic": {
      // Bugün / Dün / Bu Ay / Geçen Ay (4 cards, no forecast)
      const todayStart = startOfDay(today);
      const todayEnd = endOfDay(today);
      const yesterdayStart = startOfDay(subDays(today, 1));
      const yesterdayEnd = endOfDay(subDays(today, 1));
      const thisMonthStart = startOfMonth(today);
      const thisMonthEnd = endOfDay(today); // Use today to avoid future dates
      const lastMonthStart = startOfMonth(subMonths(today, 1));
      const lastMonthEnd = endOfMonth(subMonths(today, 1));

      return [
        {
          startDate: formatDate(todayStart),
          endDate: formatDate(todayEnd),
          label: "Bugün",
          shortLabel: "Bugün",
          color: PERIOD_COLORS[0],
        },
        {
          startDate: formatDate(yesterdayStart),
          endDate: formatDate(yesterdayEnd),
          label: "Dün",
          shortLabel: "Dün",
          color: PERIOD_COLORS[1],
        },
        {
          startDate: formatDate(thisMonthStart),
          endDate: formatDate(thisMonthEnd),
          label: "Bu Ay",
          shortLabel: "Bu Ay",
          color: PERIOD_COLORS[2],
        },
        {
          startDate: formatDate(lastMonthStart),
          endDate: formatDate(lastMonthEnd),
          label: "Geçen Ay",
          shortLabel: "Geçen Ay",
          color: PERIOD_COLORS[3],
        },
      ];
    }

    case "days": {
      // Bugün / Dün / 7 gün / 14 gün / 30 gün - show as 5 cards
      const todayStart = startOfDay(today);
      const todayEnd = endOfDay(today);
      const yesterdayStart = startOfDay(subDays(today, 1));
      const yesterdayEnd = endOfDay(subDays(today, 1));
      const last7Start = startOfDay(subDays(today, 6));
      const last14Start = startOfDay(subDays(today, 13));
      const last30Start = startOfDay(subDays(today, 29));

      return [
        {
          startDate: formatDate(todayStart),
          endDate: formatDate(todayEnd),
          label: "Bugün",
          shortLabel: "Bugün",
          color: PERIOD_COLORS[0],
        },
        {
          startDate: formatDate(yesterdayStart),
          endDate: formatDate(yesterdayEnd),
          label: "Dün",
          shortLabel: "Dün",
          color: PERIOD_COLORS[1],
        },
        {
          startDate: formatDate(last7Start),
          endDate: formatDate(todayEnd),
          label: "Son 7 Gün",
          shortLabel: "7 Gün",
          color: PERIOD_COLORS[2],
        },
        {
          startDate: formatDate(last14Start),
          endDate: formatDate(todayEnd),
          label: "Son 14 Gün",
          shortLabel: "14 Gün",
          color: PERIOD_COLORS[3],
        },
        {
          startDate: formatDate(last30Start),
          endDate: formatDate(todayEnd),
          label: "Son 30 Gün",
          shortLabel: "30 Gün",
          color: PERIOD_COLORS[4],
        },
      ];
    }

    case "weeks": {
      // Bu Hafta / Geçen Hafta / 2 Hafta Önce / 3 Hafta Önce
      const thisWeekStart = startOfWeek(today, { weekStartsOn: 1 });
      const thisWeekEnd = endOfDay(today); // Use today instead of end of week to avoid future dates
      const lastWeekStart = startOfWeek(subWeeks(today, 1), { weekStartsOn: 1 });
      const lastWeekEnd = endOfWeek(subWeeks(today, 1), { weekStartsOn: 1 });
      const twoWeeksAgoStart = startOfWeek(subWeeks(today, 2), { weekStartsOn: 1 });
      const twoWeeksAgoEnd = endOfWeek(subWeeks(today, 2), { weekStartsOn: 1 });
      const threeWeeksAgoStart = startOfWeek(subWeeks(today, 3), { weekStartsOn: 1 });
      const threeWeeksAgoEnd = endOfWeek(subWeeks(today, 3), { weekStartsOn: 1 });

      return [
        {
          startDate: formatDate(thisWeekStart),
          endDate: formatDate(thisWeekEnd),
          label: "Bu Hafta (bugüne kadar)",
          shortLabel: "Bu Hafta",
          color: PERIOD_COLORS[0],
        },
        {
          startDate: formatDate(lastWeekStart),
          endDate: formatDate(lastWeekEnd),
          label: "Geçen Hafta",
          shortLabel: "Geçen Hafta",
          color: PERIOD_COLORS[1],
        },
        {
          startDate: formatDate(twoWeeksAgoStart),
          endDate: formatDate(twoWeeksAgoEnd),
          label: "2 Hafta Önce",
          shortLabel: "2 Hafta Önce",
          color: PERIOD_COLORS[2],
        },
        {
          startDate: formatDate(threeWeeksAgoStart),
          endDate: formatDate(threeWeeksAgoEnd),
          label: "3 Hafta Önce",
          shortLabel: "3 Hafta Önce",
          color: PERIOD_COLORS[3],
        },
      ];
    }

    case "months": {
      // Bu Ay / Geçen Ay / 2 Ay Önce / 3 Ay Önce
      const thisMonthStart = startOfMonth(today);
      const thisMonthEnd = endOfDay(today); // Use today instead of end of month to avoid future dates
      const lastMonthStart = startOfMonth(subMonths(today, 1));
      const lastMonthEnd = endOfMonth(subMonths(today, 1));
      const twoMonthsAgoStart = startOfMonth(subMonths(today, 2));
      const twoMonthsAgoEnd = endOfMonth(subMonths(today, 2));
      const threeMonthsAgoStart = startOfMonth(subMonths(today, 3));
      const threeMonthsAgoEnd = endOfMonth(subMonths(today, 3));

      return [
        {
          startDate: formatDate(thisMonthStart),
          endDate: formatDate(thisMonthEnd),
          label: "Bu Ay (bugüne kadar)",
          shortLabel: "Bu Ay",
          color: PERIOD_COLORS[0],
        },
        {
          startDate: formatDate(lastMonthStart),
          endDate: formatDate(lastMonthEnd),
          label: "Geçen Ay",
          shortLabel: "Geçen Ay",
          color: PERIOD_COLORS[1],
        },
        {
          startDate: formatDate(twoMonthsAgoStart),
          endDate: formatDate(twoMonthsAgoEnd),
          label: "2 Ay Önce",
          shortLabel: "2 Ay Önce",
          color: PERIOD_COLORS[2],
        },
        {
          startDate: formatDate(threeMonthsAgoStart),
          endDate: formatDate(threeMonthsAgoEnd),
          label: "3 Ay Önce",
          shortLabel: "3 Ay Önce",
          color: PERIOD_COLORS[3],
        },
      ];
    }

    case "daysAgo": {
      // Bugün / Dün / 2 Gün Önce / 3 Gün Önce
      return [0, 1, 2, 3].map((daysBack, index) => {
        const date = subDays(today, daysBack);
        const dayStart = startOfDay(date);
        const dayEnd = endOfDay(date);
        const labels = ["Bugün", "Dün", "2 Gün Önce", "3 Gün Önce"];
        return {
          startDate: formatDate(dayStart),
          endDate: formatDate(dayEnd),
          label: labels[daysBack],
          shortLabel: labels[daysBack],
          color: PERIOD_COLORS[index],
        };
      });
    }

    case "weekDays": {
      // Bugün / Dün / 7 Gün Önce / 8 Gün Önce
      const daysBack = [0, 1, 7, 8];
      const labels = ["Bugün", "Dün", "7 Gün Önce", "8 Gün Önce"];
      return daysBack.map((days, index) => {
        const date = subDays(today, days);
        const dayStart = startOfDay(date);
        const dayEnd = endOfDay(date);
        return {
          startDate: formatDate(dayStart),
          endDate: formatDate(dayEnd),
          label: labels[index],
          shortLabel: labels[index],
          color: PERIOD_COLORS[index],
        };
      });
    }

    case "quarters": {
      // Bu Çeyrek / Geçen Çeyrek / 2 Çeyrek Önce / 3 Çeyrek Önce
      const thisQuarterStart = startOfQuarter(today);
      const thisQuarterEnd = endOfDay(today); // Use today instead of end of quarter to avoid future dates
      const lastQuarterStart = startOfQuarter(subQuarters(today, 1));
      const lastQuarterEnd = endOfQuarter(subQuarters(today, 1));
      const twoQuartersAgoStart = startOfQuarter(subQuarters(today, 2));
      const twoQuartersAgoEnd = endOfQuarter(subQuarters(today, 2));
      const threeQuartersAgoStart = startOfQuarter(subQuarters(today, 3));
      const threeQuartersAgoEnd = endOfQuarter(subQuarters(today, 3));

      return [
        {
          startDate: formatDate(thisQuarterStart),
          endDate: formatDate(thisQuarterEnd),
          label: "Bu Çeyrek (bugüne kadar)",
          shortLabel: "Bu Çeyrek",
          color: PERIOD_COLORS[0],
        },
        {
          startDate: formatDate(lastQuarterStart),
          endDate: formatDate(lastQuarterEnd),
          label: "Geçen Çeyrek",
          shortLabel: "Geçen Çeyrek",
          color: PERIOD_COLORS[1],
        },
        {
          startDate: formatDate(twoQuartersAgoStart),
          endDate: formatDate(twoQuartersAgoEnd),
          label: "2 Çeyrek Önce",
          shortLabel: "2 Çeyrek Önce",
          color: PERIOD_COLORS[2],
        },
        {
          startDate: formatDate(threeQuartersAgoStart),
          endDate: formatDate(threeQuartersAgoEnd),
          label: "3 Çeyrek Önce",
          shortLabel: "3 Çeyrek Önce",
          color: PERIOD_COLORS[3],
        },
      ];
    }

    // Single-card presets - return single period range
    case "last7days": {
      const start = startOfDay(subDays(today, 6));
      const end = endOfDay(today);
      return [
        {
          startDate: formatDate(start),
          endDate: formatDate(end),
          label: "Son 7 Gün",
          shortLabel: "Son 7 Gün",
          color: PERIOD_COLORS[0],
        },
      ];
    }

    case "last14days": {
      const start = startOfDay(subDays(today, 13));
      const end = endOfDay(today);
      return [
        {
          startDate: formatDate(start),
          endDate: formatDate(end),
          label: "Son 14 Gün",
          shortLabel: "Son 14 Gün",
          color: PERIOD_COLORS[0],
        },
      ];
    }

    case "last30days": {
      const start = startOfDay(subDays(today, 29));
      const end = endOfDay(today);
      return [
        {
          startDate: formatDate(start),
          endDate: formatDate(end),
          label: "Son 30 Gün",
          shortLabel: "Son 30 Gün",
          color: PERIOD_COLORS[0],
        },
      ];
    }

    case "thisQuarter": {
      const start = startOfQuarter(today);
      const end = endOfDay(today); // Use today instead of end of quarter to avoid future dates
      return [
        {
          startDate: formatDate(start),
          endDate: formatDate(end),
          label: "Bu Çeyrek (bugüne kadar)",
          shortLabel: "Bu Çeyrek",
          color: PERIOD_COLORS[0],
        },
      ];
    }

    case "lastQuarter": {
      const lastQ = subQuarters(today, 1);
      const start = startOfQuarter(lastQ);
      const end = endOfQuarter(lastQ);
      return [
        {
          startDate: formatDate(start),
          endDate: formatDate(end),
          label: "Geçen Çeyrek",
          shortLabel: "Geçen Çeyrek",
          color: PERIOD_COLORS[0],
        },
      ];
    }

    case "custom":
      // Custom is handled separately by date picker
      return null;

    default:
      return null;
  }
}

/**
 * Hook for fetching dashboard stats for multiple periods in parallel.
 * Used for presets like "weeks", "months", "quarters" that show 4 cards.
 */
export const useDashboardStatsByPreset = (
  storeId: string | undefined,
  preset: DateRangePreset
) => {
  const periodRanges = calculateMultiPeriodRanges(preset);

  const queries = useQueries({
    queries:
      periodRanges?.map((range) => ({
        queryKey: [
          ...dashboardKeys.all,
          "stats",
          "preset",
          storeId,
          range.startDate,
          range.endDate,
        ],
        queryFn: () =>
          dashboardApi.getStatsByRange(
            storeId!,
            range.startDate,
            range.endDate,
            range.label
          ),
        enabled: !!storeId && !!periodRanges,
        staleTime: 5 * 60 * 1000,
        gcTime: 15 * 60 * 1000,
        retry: 2,
        retryDelay: (attemptIndex: number) =>
          Math.min(1000 * 2 ** attemptIndex, 10000),
        refetchOnWindowFocus: false,
      })) ?? [],
  });

  // Combine results
  const isLoading = queries.some((q) => q.isLoading);
  const error = queries.find((q) => q.error)?.error as Error | null;
  const data = periodRanges
    ? queries.map((q, index) => ({
        stats: q.data as DashboardStats | undefined,
        label: periodRanges[index].label,
        shortLabel: periodRanges[index].shortLabel,
        dateRange: `${periodRanges[index].startDate} - ${periodRanges[index].endDate}`,
        color: periodRanges[index].color,
      }))
    : null;

  return {
    data,
    periodRanges,
    isLoading,
    error,
    isMultiPeriod: periodRanges !== null && periodRanges.length > 1,
  };
};

/**
 * Hook for fetching multi-period stats for P&L breakdown table.
 * Used for horizontal scroll table with monthly/weekly/daily breakdown.
 */
export const useMultiPeriodStats = (
  storeId: string | undefined,
  periodType: PLPeriodType = "monthly",
  periodCount: number = 12,
  productBarcode?: string,
) => {
  return useQuery<MultiPeriodStatsResponse>({
    queryKey: dashboardKeys.multiPeriod(storeId!, periodType, periodCount, productBarcode),
    queryFn: () => dashboardApi.getMultiPeriodStats(storeId!, periodType, periodCount, productBarcode),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000, // 5 dakika boyunca fresh kabul et
    gcTime: 15 * 60 * 1000, // 15 dakika cache'te tut
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000),
    refetchOnWindowFocus: false,
  });
};
