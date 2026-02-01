"use client";

import { useMemo } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TrendingUp } from "lucide-react";
import type { DashboardStats } from "@/types/dashboard";
import { format, parseISO, subDays } from "date-fns";
import { tr } from "date-fns/locale";
import { useCurrency } from "@/lib/contexts/currency-context";

interface RevenueTrendChartProps {
  stats?: DashboardStats;
  isLoading?: boolean;
  title?: string;
}

// Compact format for Y-axis (K, M suffix)
function formatCompact(value: number, symbol: string): string {
  if (value >= 1000000) {
    return `${symbol}${(value / 1000000).toFixed(1)}M`;
  }
  if (value >= 1000) {
    return `${symbol}${(value / 1000).toFixed(0)}K`;
  }
  return `${symbol}${value.toFixed(0)}`;
}

export function RevenueTrendChart({
  stats,
  isLoading,
  title = "Gelir Trendi",
}: RevenueTrendChartProps) {
  const { formatCurrency, getCurrencySymbol } = useCurrency();
  // Group orders by date and calculate daily revenue
  const chartData = useMemo(() => {
    if (!stats?.orders || stats.orders.length === 0) {
      // Generate last 7 days with zero values for empty state
      const days = [];
      for (let i = 6; i >= 0; i--) {
        const date = subDays(new Date(), i);
        days.push({
          date: format(date, "dd MMM", { locale: tr }),
          fullDate: format(date, "yyyy-MM-dd"),
          revenue: 0,
          orders: 0,
          profit: 0,
        });
      }
      return days;
    }

    // Group orders by date
    const dailyData = new Map<
      string,
      { revenue: number; orders: number; profit: number }
    >();

    stats.orders.forEach((order) => {
      const dateKey = order.orderDate.split("T")[0];
      const existing = dailyData.get(dateKey) || {
        revenue: 0,
        orders: 0,
        profit: 0,
      };
      dailyData.set(dateKey, {
        revenue: existing.revenue + order.revenue,
        orders: existing.orders + 1,
        profit: existing.profit + order.grossProfit,
      });
    });

    // Convert to array and sort by date
    const sorted = Array.from(dailyData.entries())
      .map(([date, data]) => ({
        date: format(parseISO(date), "dd MMM", { locale: tr }),
        fullDate: date,
        ...data,
      }))
      .sort((a, b) => a.fullDate.localeCompare(b.fullDate));

    return sorted;
  }, [stats?.orders]);

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <TrendingUp className="h-5 w-5 text-blue-500" />
            <Skeleton className="h-5 w-32" />
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-[300px] w-full" />
        </CardContent>
      </Card>
    );
  }

  const hasData = chartData.some((d) => d.revenue > 0);

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-lg">
          <TrendingUp className="h-5 w-5 text-blue-500" />
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {!hasData ? (
          <div className="h-[300px] flex items-center justify-center text-muted-foreground">
            <div className="text-center">
              <TrendingUp className="h-12 w-12 mx-auto mb-3 opacity-30" />
              <p className="text-sm">Henuz veri yok</p>
            </div>
          </div>
        ) : (
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart
                data={chartData}
                margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 12, fill: "#6b7280" }}
                  tickLine={{ stroke: "#e5e7eb" }}
                  axisLine={{ stroke: "#e5e7eb" }}
                />
                <YAxis
                  tickFormatter={(value) => formatCompact(value, getCurrencySymbol())}
                  tick={{ fontSize: 12, fill: "#6b7280" }}
                  tickLine={{ stroke: "#e5e7eb" }}
                  axisLine={{ stroke: "#e5e7eb" }}
                  width={60}
                />
                <Tooltip
                  formatter={(value: number, name: string) => {
                    if (name === "Gelir" || name === "Kar") {
                      return [formatCurrency(value), name];
                    }
                    return [value, name];
                  }}
                  labelStyle={{ color: "#374151" }}
                  contentStyle={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: "8px",
                    boxShadow: "0 2px 8px rgba(0,0,0,0.1)",
                  }}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="revenue"
                  name="Gelir"
                  stroke="#3b82f6"
                  strokeWidth={2}
                  dot={{ fill: "#3b82f6", strokeWidth: 2, r: 4 }}
                  activeDot={{ r: 6, fill: "#3b82f6" }}
                />
                <Line
                  type="monotone"
                  dataKey="profit"
                  name="Kar"
                  stroke="#10b981"
                  strokeWidth={2}
                  dot={{ fill: "#10b981", strokeWidth: 2, r: 4 }}
                  activeDot={{ r: 6, fill: "#10b981" }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
