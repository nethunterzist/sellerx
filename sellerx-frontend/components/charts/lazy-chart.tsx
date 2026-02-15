"use client";

/**
 * Lazy Chart Components
 *
 * Dynamic imports for recharts to reduce initial bundle size.
 * Uses Next.js dynamic() for client-side only loading with Suspense.
 *
 * Usage:
 * import { LazyComposedChart, LazyPieChart, LazyBarChart, LazyLineChart } from '@/components/charts/lazy-chart';
 */

import dynamic from "next/dynamic";
import { Skeleton } from "@/components/ui/skeleton";

// Chart loading skeleton
export function ChartSkeleton({ height = 300 }: { height?: number }) {
  return (
    <div
      className="flex flex-col items-center justify-center w-full animate-pulse"
      style={{ height }}
    >
      <Skeleton className="w-full h-full rounded-lg" />
    </div>
  );
}

// Small sparkline skeleton
export function SparklineSkeleton() {
  return <Skeleton className="w-full h-12 rounded" />;
}

// Pie chart skeleton
export function PieChartSkeleton({ height = 300 }: { height?: number }) {
  return (
    <div
      className="flex items-center justify-center w-full animate-pulse"
      style={{ height }}
    >
      <Skeleton className="w-48 h-48 rounded-full" />
    </div>
  );
}

// ============ Lazy-loaded Chart Components ============

/**
 * Lazy DashboardChart - Main dashboard chart with multiple metrics
 */
export const LazyDashboardChart = dynamic(
  () => import("@/components/dashboard/dashboard-chart").then((mod) => mod.DashboardChart),
  {
    loading: () => <ChartSkeleton height={480} />,
    ssr: false,
  }
);

/**
 * Lazy Sparkline - Small inline charts
 */
export const LazySparkline = dynamic(
  () => import("@/components/dashboard/sparkline").then((mod) => mod.Sparkline),
  {
    loading: () => <SparklineSkeleton />,
    ssr: false,
  }
);

/**
 * Lazy ExpenseCategoryChart - Pie chart for expense categories
 */
export const LazyExpenseCategoryChart = dynamic(
  () =>
    import("@/components/expenses/expense-category-chart").then(
      (mod) => mod.ExpenseCategoryChart
    ),
  {
    loading: () => <PieChartSkeleton height={300} />,
    ssr: false,
  }
);

/**
 * Lazy ExpenseTrendChart - Bar chart for expense trends
 */
export const LazyExpenseTrendChart = dynamic(
  () =>
    import("@/components/expenses/expense-trend-chart").then(
      (mod) => mod.ExpenseTrendChart
    ),
  {
    loading: () => <ChartSkeleton height={300} />,
    ssr: false,
  }
);

/**
 * Lazy ReturnCostBreakdown - Pie chart for return costs
 */
export const LazyReturnCostBreakdown = dynamic(
  () =>
    import("@/components/returns/return-cost-breakdown").then(
      (mod) => mod.ReturnCostBreakdown
    ),
  {
    loading: () => <PieChartSkeleton height={300} />,
    ssr: false,
  }
);

/**
 * Lazy ReturnTrendChart - Line/Bar chart for return trends
 */
export const LazyReturnTrendChart = dynamic(
  () =>
    import("@/components/returns/return-trend-chart").then(
      (mod) => mod.ReturnTrendChart
    ),
  {
    loading: () => <ChartSkeleton height={300} />,
    ssr: false,
  }
);

/**
 * Lazy ReturnReasonsChart - Pie chart for return reasons
 */
export const LazyReturnReasonsChart = dynamic(
  () =>
    import("@/components/returns/return-reasons-chart").then(
      (mod) => mod.ReturnReasonsChart
    ),
  {
    loading: () => <PieChartSkeleton height={300} />,
    ssr: false,
  }
);

/**
 * Lazy ProfitBreakdownChart - Pie chart for profit breakdown
 */
export const LazyProfitBreakdownChart = dynamic(
  () =>
    import("@/components/analytics/profit-breakdown-chart").then(
      (mod) => mod.ProfitBreakdownChart
    ),
  {
    loading: () => <PieChartSkeleton height={300} />,
    ssr: false,
  }
);

/**
 * Lazy RevenueTrendChart - Line chart for revenue trends
 */
export const LazyRevenueTrendChart = dynamic(
  () =>
    import("@/components/analytics/revenue-trend-chart").then(
      (mod) => mod.RevenueTrendChart
    ),
  {
    loading: () => <ChartSkeleton height={300} />,
    ssr: false,
  }
);

// ============ Admin Charts ============

/**
 * Lazy StoreStatusChart - Pie chart for store status (admin)
 */
export const LazyStoreStatusChart = dynamic(
  () =>
    import("@/components/admin/dashboard/store-status-chart").then(
      (mod) => mod.StoreStatusChart
    ),
  {
    loading: () => <PieChartSkeleton height={300} />,
    ssr: false,
  }
);

/**
 * Lazy UserGrowthChart - Line chart for user growth (admin)
 */
export const LazyUserGrowthChart = dynamic(
  () =>
    import("@/components/admin/dashboard/user-growth-chart").then(
      (mod) => mod.UserGrowthChart
    ),
  {
    loading: () => <ChartSkeleton height={300} />,
    ssr: false,
  }
);

/**
 * Lazy OrderVolumeChart - Bar chart for order volume (admin)
 */
export const LazyOrderVolumeChart = dynamic(
  () =>
    import("@/components/admin/dashboard/order-volume-chart").then(
      (mod) => mod.OrderVolumeChart
    ),
  {
    loading: () => <ChartSkeleton height={300} />,
    ssr: false,
  }
);
