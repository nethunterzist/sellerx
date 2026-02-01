"use client";

import { useMemo } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import { ShoppingCart } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import type { AdminDashboardStats } from "@/types/admin";

interface OrderVolumeChartProps {
  stats: AdminDashboardStats | undefined;
  isLoading: boolean;
}

const COLORS = ["#f59e0b", "#fbbf24", "#fcd34d"];

export function OrderVolumeChart({ stats, isLoading }: OrderVolumeChartProps) {
  const chartData = useMemo(() => {
    if (!stats) return [];
    return [
      { period: "Bugün", orders: stats.ordersToday },
      { period: "Bu Hafta", orders: stats.ordersThisWeek },
      { period: "Bu Ay", orders: stats.ordersThisMonth },
    ];
  }, [stats]);

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-popover border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground">{label}</p>
          <p className="text-sm text-amber-600 dark:text-amber-400">
            {payload[0].value.toLocaleString("tr-TR")} sipariş
          </p>
        </div>
      );
    }
    return null;
  };

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border border-border p-6">
        <div className="flex items-center gap-2 mb-4">
          <Skeleton className="h-5 w-5" />
          <Skeleton className="h-5 w-32" />
        </div>
        <Skeleton className="h-[280px] w-full" />
      </div>
    );
  }

  return (
    <div className="bg-card rounded-xl border border-border p-6">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-lg bg-amber-100 dark:bg-amber-900/40">
            <ShoppingCart className="h-4 w-4 text-amber-600 dark:text-amber-400" />
          </div>
          <h3 className="font-semibold text-foreground">Sipariş Hacmi</h3>
        </div>
        {stats && (
          <div className="text-right">
            <p className="text-2xl font-bold text-foreground">
              {stats.totalOrders.toLocaleString("tr-TR")}
            </p>
            <p className="text-xs text-muted-foreground">Toplam sipariş</p>
          </div>
        )}
      </div>
      <div className="h-[250px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 20, right: 10, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-border" vertical={false} />
            <XAxis
              dataKey="period"
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 12, fill: "hsl(var(--muted-foreground))" }}
            />
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 12, fill: "hsl(var(--muted-foreground))" }}
              tickFormatter={(value) => value.toLocaleString("tr-TR")}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: "hsl(var(--muted))", opacity: 0.3 }} />
            <Bar dataKey="orders" radius={[6, 6, 0, 0]} maxBarSize={60}>
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
