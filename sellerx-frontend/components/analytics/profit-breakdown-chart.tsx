"use client";

import { useMemo } from "react";
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  Legend,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { PieChartIcon } from "lucide-react";
import type { DashboardStats } from "@/types/dashboard";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ProfitBreakdownChartProps {
  stats?: DashboardStats;
  isLoading?: boolean;
  title?: string;
}

const COLORS = {
  profit: "#10b981", // green
  commission: "#ef4444", // red
  vat: "#f59e0b", // amber
  cost: "#6b7280", // gray
  expense: "#8b5cf6", // purple
};

export function ProfitBreakdownChart({
  stats,
  isLoading,
  title = "Gelir Dagilimi",
}: ProfitBreakdownChartProps) {
  const { formatCurrency } = useCurrency();
  const chartData = useMemo(() => {
    if (!stats) return [];

    const data = [
      {
        name: "Net Kar",
        value: Math.max(0, stats.grossProfit - stats.totalEstimatedCommission - stats.totalExpenseAmount),
        color: COLORS.profit,
      },
      {
        name: "Komisyon",
        value: stats.totalEstimatedCommission || 0,
        color: COLORS.commission,
      },
      {
        name: "Urun Maliyeti",
        value: stats.totalProductCosts || 0,
        color: COLORS.cost,
      },
      {
        name: "Giderler",
        value: stats.totalExpenseAmount || 0,
        color: COLORS.expense,
      },
    ].filter((item) => item.value > 0);

    return data;
  }, [stats]);

  const totalRevenue = stats?.totalRevenue || 0;

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <PieChartIcon className="h-5 w-5 text-purple-500" />
            <Skeleton className="h-5 w-32" />
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-[300px] w-full" />
        </CardContent>
      </Card>
    );
  }

  const hasData = chartData.length > 0 && totalRevenue > 0;

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-lg">
          <PieChartIcon className="h-5 w-5 text-purple-500" />
          {title}
        </CardTitle>
        {hasData && (
          <p className="text-sm text-muted-foreground">
            Toplam Gelir: {formatCurrency(totalRevenue)}
          </p>
        )}
      </CardHeader>
      <CardContent>
        {!hasData ? (
          <div className="h-[300px] flex items-center justify-center text-muted-foreground">
            <div className="text-center">
              <PieChartIcon className="h-12 w-12 mx-auto mb-3 opacity-30" />
              <p className="text-sm">Henuz veri yok</p>
            </div>
          </div>
        ) : (
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={100}
                  paddingAngle={2}
                  dataKey="value"
                >
                  {chartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(value: number, name: string) => [
                    formatCurrency(value),
                    name,
                  ]}
                  contentStyle={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: "8px",
                    boxShadow: "0 2px 8px rgba(0,0,0,0.1)",
                  }}
                />
                <Legend
                  verticalAlign="bottom"
                  height={36}
                  formatter={(value, entry) => {
                    const item = chartData.find((d) => d.name === value);
                    const percentage = item
                      ? ((item.value / totalRevenue) * 100).toFixed(1)
                      : 0;
                    return (
                      <span className="text-sm text-muted-foreground">
                        {value} ({percentage}%)
                      </span>
                    );
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
