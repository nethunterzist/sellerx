"use client";

import { useMemo } from "react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { Users } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import type { AdminDashboardStats } from "@/types/admin";

interface UserGrowthChartProps {
  stats: AdminDashboardStats | undefined;
  isLoading: boolean;
}

export function UserGrowthChart({ stats, isLoading }: UserGrowthChartProps) {
  const chartData = useMemo(() => {
    if (!stats) return [];
    return [
      { period: "Bugün", users: stats.newUsersToday, fill: "#3b82f6" },
      { period: "Bu Hafta", users: stats.newUsersThisWeek, fill: "#60a5fa" },
      { period: "Bu Ay", users: stats.newUsersThisMonth, fill: "#93c5fd" },
      { period: "Aktif (30g)", users: stats.activeUsersLast30Days, fill: "#3b82f6" },
    ];
  }, [stats]);

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-popover border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground">{label}</p>
          <p className="text-sm text-blue-600 dark:text-blue-400">
            {payload[0].value.toLocaleString("tr-TR")} kullanıcı
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
      <div className="flex items-center gap-2 mb-4">
        <div className="p-2 rounded-lg bg-blue-100 dark:bg-blue-900/40">
          <Users className="h-4 w-4 text-blue-600 dark:text-blue-400" />
        </div>
        <h3 className="font-semibold text-foreground">Kullanıcı Büyümesi</h3>
      </div>
      <div className="h-[280px]">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="userGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
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
            <Tooltip content={<CustomTooltip />} />
            <Area
              type="monotone"
              dataKey="users"
              stroke="#3b82f6"
              strokeWidth={2}
              fill="url(#userGradient)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
