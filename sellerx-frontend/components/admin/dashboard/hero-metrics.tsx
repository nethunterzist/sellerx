"use client";

import { Users, Store, ShoppingCart, TrendingUp, TrendingDown, Banknote } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import type { AdminDashboardStats } from "@/types/admin";

interface HeroMetricsProps {
  stats: AdminDashboardStats | undefined;
  isLoading: boolean;
}

interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: {
    value: number;
    label: string;
    isPositive: boolean;
  };
  color: "blue" | "purple" | "amber" | "green";
}

const colorClasses = {
  blue: {
    gradient: "from-blue-500/10 to-blue-600/5 dark:from-blue-500/20 dark:to-blue-600/10",
    iconBg: "bg-blue-100 dark:bg-blue-900/50",
    icon: "text-blue-600 dark:text-blue-400",
    border: "border-blue-200/50 dark:border-blue-800/50",
  },
  purple: {
    gradient: "from-purple-500/10 to-purple-600/5 dark:from-purple-500/20 dark:to-purple-600/10",
    iconBg: "bg-purple-100 dark:bg-purple-900/50",
    icon: "text-purple-600 dark:text-purple-400",
    border: "border-purple-200/50 dark:border-purple-800/50",
  },
  amber: {
    gradient: "from-amber-500/10 to-amber-600/5 dark:from-amber-500/20 dark:to-amber-600/10",
    iconBg: "bg-amber-100 dark:bg-amber-900/50",
    icon: "text-amber-600 dark:text-amber-400",
    border: "border-amber-200/50 dark:border-amber-800/50",
  },
  green: {
    gradient: "from-green-500/10 to-green-600/5 dark:from-green-500/20 dark:to-green-600/10",
    iconBg: "bg-green-100 dark:bg-green-900/50",
    icon: "text-green-600 dark:text-green-400",
    border: "border-green-200/50 dark:border-green-800/50",
  },
};

function MetricCard({ title, value, subtitle, icon, trend, color }: MetricCardProps) {
  const colors = colorClasses[color];

  return (
    <div
      className={`relative overflow-hidden rounded-xl border ${colors.border} bg-gradient-to-br ${colors.gradient} p-5 transition-all hover:shadow-lg`}
    >
      <div className="flex items-start justify-between">
        <div className="space-y-2">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <p className="text-3xl font-bold text-foreground">{value}</p>
          {subtitle && (
            <p className="text-xs text-muted-foreground">{subtitle}</p>
          )}
          {trend && (
            <div className="flex items-center gap-1 mt-1">
              {trend.isPositive ? (
                <TrendingUp className="h-3 w-3 text-green-500" />
              ) : (
                <TrendingDown className="h-3 w-3 text-red-500" />
              )}
              <span
                className={`text-xs font-medium ${
                  trend.isPositive ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400"
                }`}
              >
                {trend.isPositive ? "+" : ""}{trend.value}%
              </span>
              <span className="text-xs text-muted-foreground">{trend.label}</span>
            </div>
          )}
        </div>
        <div className={`p-3 rounded-xl ${colors.iconBg}`}>
          {icon}
        </div>
      </div>
    </div>
  );
}

function MetricCardSkeleton() {
  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start justify-between">
        <div className="space-y-2">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-9 w-20" />
          <Skeleton className="h-3 w-32" />
        </div>
        <Skeleton className="h-12 w-12 rounded-xl" />
      </div>
    </div>
  );
}

export function HeroMetrics({ stats, isLoading }: HeroMetricsProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCardSkeleton />
        <MetricCardSkeleton />
        <MetricCardSkeleton />
        <MetricCardSkeleton />
      </div>
    );
  }

  if (!stats) {
    return null;
  }

  // Calculate trends
  const weeklyUserGrowth = stats.newUsersThisWeek > 0 && stats.newUsersThisMonth > 0
    ? Math.round(((stats.newUsersThisWeek / (stats.newUsersThisMonth / 4)) - 1) * 100)
    : 0;

  const storeHealthPercent = stats.totalStores > 0
    ? Math.round((stats.activeStores / stats.totalStores) * 100)
    : 0;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <MetricCard
        title="Toplam Kullanıcı"
        value={stats.totalUsers.toLocaleString("tr-TR")}
        subtitle={`${stats.activeUsersLast30Days.toLocaleString("tr-TR")} aktif (30 gün)`}
        icon={<Users className="h-6 w-6 text-blue-600 dark:text-blue-400" />}
        trend={
          weeklyUserGrowth !== 0
            ? {
                value: Math.abs(weeklyUserGrowth),
                label: "bu hafta",
                isPositive: weeklyUserGrowth > 0,
              }
            : undefined
        }
        color="blue"
      />

      <MetricCard
        title="Aktif Mağaza"
        value={`${stats.activeStores}/${stats.totalStores}`}
        subtitle={`%${storeHealthPercent} sağlıklı`}
        icon={<Store className="h-6 w-6 text-purple-600 dark:text-purple-400" />}
        color="purple"
      />

      <MetricCard
        title="Bugünkü Siparişler"
        value={stats.ordersToday.toLocaleString("tr-TR")}
        subtitle={`Bu hafta: ${stats.ordersThisWeek.toLocaleString("tr-TR")}`}
        icon={<ShoppingCart className="h-6 w-6 text-amber-600 dark:text-amber-400" />}
        color="amber"
      />

      <MetricCard
        title="MRR"
        value={
          stats.mrr !== null
            ? `${stats.mrr.toLocaleString("tr-TR", { minimumFractionDigits: 0, maximumFractionDigits: 0 })} TL`
            : "—"
        }
        subtitle={`${stats.activeSubscriptions} aktif abonelik`}
        icon={<Banknote className="h-6 w-6 text-green-600 dark:text-green-400" />}
        color="green"
      />
    </div>
  );
}
