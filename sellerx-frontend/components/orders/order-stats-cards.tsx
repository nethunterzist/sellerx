"use client";

import { useOrderStatistics, useOrderStatisticsByDateRange } from "@/hooks/queries/use-orders";
import { cn } from "@/lib/utils";
import { ShoppingCart, TrendingUp, Receipt, XCircle } from "lucide-react";
import { useCurrency } from "@/lib/contexts/currency-context";

interface OrderStatsCardsProps {
  storeId: string | undefined;
  startDate?: string;
  endDate?: string;
}

type CardVariant = "orders" | "revenue" | "average" | "cancelled";

const headerColors: Record<CardVariant, string> = {
  orders: "bg-blue-500",
  revenue: "bg-green-600",
  average: "bg-purple-500",
  cancelled: "bg-red-500",
};

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ElementType;
  variant: CardVariant;
}

function StatCard({ title, value, icon: Icon, variant }: StatCardProps) {
  const headerColor = headerColors[variant];

  return (
    <div className="flex flex-col rounded-lg overflow-hidden min-w-[180px] flex-1 bg-card border border-border shadow-sm">
      {/* Colored Header */}
      <div className={cn("px-4 py-3", headerColor)}>
        <div className="flex items-center gap-2">
          <Icon className="h-4 w-4 text-white/80" />
          <h3 className="text-sm font-semibold text-white">{title}</h3>
        </div>
      </div>

      {/* Card Body */}
      <div className="p-4">
        <p className="text-2xl font-bold text-foreground">{value}</p>
      </div>
    </div>
  );
}

function StatCardSkeleton() {
  return (
    <div className="flex flex-col rounded-lg overflow-hidden min-w-[180px] flex-1 bg-card border border-border shadow-sm">
      <div className="px-4 py-3 bg-muted animate-pulse">
        <div className="h-4 w-24 bg-muted-foreground/20 rounded" />
      </div>
      <div className="p-4">
        <div className="h-8 w-20 bg-muted rounded animate-pulse" />
      </div>
    </div>
  );
}

export function OrderStatsCards({ storeId, startDate, endDate }: OrderStatsCardsProps) {
  const { formatCurrency } = useCurrency();

  // Use date-filtered statistics if dates are provided, otherwise use all-time stats
  const hasDateFilter = !!startDate && !!endDate;

  const dateRangeQuery = useOrderStatisticsByDateRange(
    hasDateFilter ? storeId : undefined,
    startDate,
    endDate
  );

  const allTimeQuery = useOrderStatistics(
    !hasDateFilter ? storeId : undefined
  );

  const { data, isLoading, error } = hasDateFilter ? dateRangeQuery : allTimeQuery;

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
        <p className="text-red-800 text-sm">
          İstatistikler yüklenirken hata: {error.message}
        </p>
      </div>
    );
  }

  if (isLoading || !data) {
    return (
      <div className="flex gap-4 overflow-x-auto pb-2 mb-6">
        {Array.from({ length: 4 }).map((_, i) => (
          <StatCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  return (
    <div className="flex gap-4 overflow-x-auto pb-2 mb-6">
      <StatCard
        title="Toplam Sipariş"
        value={data.totalOrders.toLocaleString("tr-TR")}
        icon={ShoppingCart}
        variant="orders"
      />
      <StatCard
        title="Toplam Ciro"
        value={formatCurrency(data.totalRevenue)}
        icon={TrendingUp}
        variant="revenue"
      />
      <StatCard
        title="Ortalama Sipariş"
        value={formatCurrency(data.averageOrderValue)}
        icon={Receipt}
        variant="average"
      />
      <StatCard
        title="İptal Edilen"
        value={data.cancelledOrders.toLocaleString("tr-TR")}
        icon={XCircle}
        variant="cancelled"
      />
    </div>
  );
}
