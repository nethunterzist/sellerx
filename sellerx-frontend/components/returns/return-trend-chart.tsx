"use client";

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
import { format, parseISO } from "date-fns";
import { tr } from "date-fns/locale";
import { TrendingDown } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import type { DailyReturnStats } from "@/types/returns";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ReturnTrendChartProps {
  dailyTrend: DailyReturnStats[];
  isLoading?: boolean;
}

export function ReturnTrendChart({
  dailyTrend,
  isLoading = false,
}: ReturnTrendChartProps) {
  const { formatCurrency, getCurrencySymbol } = useCurrency();

  // Transform data for chart
  const chartData = dailyTrend.map((item) => ({
    ...item,
    dateLabel: format(parseISO(item.date), "d MMM", { locale: tr }),
  }));

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="bg-card p-3 rounded-lg shadow-lg border border-border">
          <p className="font-medium text-foreground mb-2">
            {format(parseISO(data.date), "d MMMM yyyy", { locale: tr })}
          </p>
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-red-500" />
              <span className="text-muted-foreground">İade Sayısı:</span>
              <span className="font-medium">{data.returnCount}</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-orange-500" />
              <span className="text-muted-foreground">Zarar:</span>
              <span className="font-medium text-red-600">
                {formatCurrency(data.totalLoss)}
              </span>
            </div>
          </div>
        </div>
      );
    }
    return null;
  };

  if (isLoading) {
    return (
      <div className="bg-card rounded-lg border border-border p-6">
        <div className="flex items-center justify-between mb-4">
          <Skeleton className="h-6 w-24" />
          <div className="flex items-center gap-4">
            <Skeleton className="h-4 w-28" />
            <Skeleton className="h-4 w-28" />
          </div>
        </div>
        <div className="h-64 relative">
          {/* Y-axis skeleton */}
          <div className="absolute left-0 top-0 h-full flex flex-col justify-between py-4">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-3 w-8" />
            ))}
          </div>
          {/* Chart area skeleton */}
          <div className="ml-12 h-full flex items-end gap-2 pb-8">
            {[...Array(7)].map((_, i) => (
              <div key={i} className="flex-1 flex flex-col items-center gap-2">
                <Skeleton
                  className="w-full rounded-t"
                  style={{ height: `${Math.random() * 60 + 20}%` }}
                />
              </div>
            ))}
          </div>
          {/* X-axis skeleton */}
          <div className="absolute bottom-0 left-12 right-0 flex justify-between">
            {[...Array(7)].map((_, i) => (
              <Skeleton key={i} className="h-3 w-10" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (chartData.length === 0) {
    return (
      <div className="bg-card rounded-lg border border-border p-6">
        <h3 className="text-lg font-semibold text-foreground mb-4">İade Trendi</h3>
        <div className="h-64 flex items-center justify-center flex-col gap-2">
          <TrendingDown className="h-12 w-12 text-muted-foreground" />
          <p className="text-muted-foreground">Henüz trend verisi bulunmuyor</p>
        </div>
      </div>
    );
  }

  // Calculate totals
  const totalReturns = chartData.reduce((sum, item) => sum + item.returnCount, 0);
  const totalLoss = chartData.reduce((sum, item) => sum + item.totalLoss, 0);

  return (
    <div className="bg-card rounded-lg border border-border p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-foreground">İade Trendi</h3>
        <div className="flex items-center gap-4 text-sm">
          <span className="text-muted-foreground">
            Toplam: <span className="font-medium text-foreground">{totalReturns} iade</span>
          </span>
          <span className="text-muted-foreground">
            Zarar:{" "}
            <span className="font-medium text-red-600">
              {formatCurrency(totalLoss)}
            </span>
          </span>
        </div>
      </div>
      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={chartData}
            margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis
              dataKey="dateLabel"
              tick={{ fontSize: 12 }}
              tickLine={false}
            />
            <YAxis
              yAxisId="left"
              tick={{ fontSize: 12 }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              yAxisId="right"
              orientation="right"
              tick={{ fontSize: 12 }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => `${getCurrencySymbol()}${value.toLocaleString()}`}
            />
            <Tooltip content={<CustomTooltip />} />
            <Legend
              formatter={(value) =>
                value === "returnCount" ? "İade Sayısı" : "Toplam Zarar"
              }
            />
            <Line
              yAxisId="left"
              type="monotone"
              dataKey="returnCount"
              stroke="#EF4444"
              strokeWidth={2}
              dot={{ fill: "#EF4444", strokeWidth: 2 }}
              activeDot={{ r: 6 }}
            />
            <Line
              yAxisId="right"
              type="monotone"
              dataKey="totalLoss"
              stroke="#F97316"
              strokeWidth={2}
              dot={{ fill: "#F97316", strokeWidth: 2 }}
              activeDot={{ r: 6 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
