"use client";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  RotateCcw,
  TrendingDown,
  Package,
  AlertTriangle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { DashboardStats } from "@/types/dashboard";
import { useCurrency } from "@/lib/contexts/currency-context";

interface OrderMetricsProps {
  stats?: DashboardStats;
  isLoading?: boolean;
}

function MetricCard({
  title,
  value,
  subtitle,
  icon: Icon,
  iconColor,
  valueColor,
  isLoading,
}: {
  title: string;
  value: string;
  subtitle?: string;
  icon: React.ElementType;
  iconColor: string;
  valueColor?: string;
  isLoading?: boolean;
}) {
  if (isLoading) {
    return (
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-start justify-between">
            <div className="space-y-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-8 w-16" />
              <Skeleton className="h-3 w-20" />
            </div>
            <Skeleton className="h-10 w-10 rounded-full" />
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-sm font-medium text-muted-foreground">{title}</p>
            <p className={cn("text-2xl font-bold mt-1", valueColor)}>{value}</p>
            {subtitle && (
              <p className="text-xs text-muted-foreground mt-1">{subtitle}</p>
            )}
          </div>
          <div className={cn("p-3 rounded-full", iconColor)}>
            <Icon className="h-5 w-5 text-white" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export function OrderMetrics({ stats, isLoading }: OrderMetricsProps) {
  const { formatCurrency } = useCurrency();

  // Calculate metrics
  const totalOrders = stats?.totalOrders || 0;
  const returnCount = stats?.returnCount || 0;
  const totalProductsSold = stats?.totalProductsSold || 0;
  const returnCost = stats?.returnCost || 0;
  const itemsWithoutCost = stats?.itemsWithoutCost || 0;

  // Return rate calculation
  const returnRate =
    totalOrders > 0 ? ((returnCount / totalOrders) * 100).toFixed(1) : "0.0";

  // Average products per order
  const avgProductsPerOrder =
    totalOrders > 0 ? (totalProductsSold / totalOrders).toFixed(1) : "0.0";

  // Determine return rate severity
  const returnRateNum = parseFloat(returnRate);
  const returnRateColor =
    returnRateNum > 10
      ? "text-red-600 dark:text-red-400"
      : returnRateNum > 5
      ? "text-yellow-600 dark:text-yellow-400"
      : "text-green-600 dark:text-green-400";

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <MetricCard
        title="Iade Orani"
        value={`%${returnRate}`}
        subtitle={`${returnCount} iade / ${totalOrders} siparis`}
        icon={RotateCcw}
        iconColor="bg-orange-500"
        valueColor={returnRateColor}
        isLoading={isLoading}
      />

      <MetricCard
        title="Iade Maliyeti"
        value={formatCurrency(returnCost)}
        subtitle={`${returnCount} urun`}
        icon={TrendingDown}
        iconColor="bg-red-500"
        valueColor="text-red-600"
        isLoading={isLoading}
      />

      <MetricCard
        title="Siparis Basina Urun"
        value={avgProductsPerOrder}
        subtitle="ortalama"
        icon={Package}
        iconColor="bg-blue-500"
        isLoading={isLoading}
      />

      <MetricCard
        title="Maliyet Eksik"
        value={itemsWithoutCost.toString()}
        subtitle="urun maliyeti girilmemis"
        icon={AlertTriangle}
        iconColor={itemsWithoutCost > 0 ? "bg-yellow-500" : "bg-green-500"}
        valueColor={itemsWithoutCost > 0 ? "text-yellow-600" : "text-green-600"}
        isLoading={isLoading}
      />
    </div>
  );
}
