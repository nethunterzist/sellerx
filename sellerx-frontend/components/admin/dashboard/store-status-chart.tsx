"use client";

import { useMemo } from "react";
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from "recharts";
import { Store } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import type { AdminDashboardStats } from "@/types/admin";

interface StoreStatusChartProps {
  stats: AdminDashboardStats | undefined;
  isLoading: boolean;
}

const COLORS = {
  active: "#10b981", // green
  syncError: "#ef4444", // red
  webhookError: "#f59e0b", // amber
  other: "#6b7280", // gray
};

export function StoreStatusChart({ stats, isLoading }: StoreStatusChartProps) {
  const chartData = useMemo(() => {
    if (!stats || stats.totalStores === 0) return [];

    const other = Math.max(
      0,
      stats.totalStores -
        stats.activeStores -
        stats.storesWithSyncErrors -
        stats.storesWithWebhookErrors
    );

    const data = [];
    if (stats.activeStores > 0) {
      data.push({ name: "Aktif", value: stats.activeStores, color: COLORS.active });
    }
    if (stats.storesWithSyncErrors > 0) {
      data.push({ name: "Sync Hatası", value: stats.storesWithSyncErrors, color: COLORS.syncError });
    }
    if (stats.storesWithWebhookErrors > 0) {
      data.push({ name: "Webhook Hatası", value: stats.storesWithWebhookErrors, color: COLORS.webhookError });
    }
    if (other > 0) {
      data.push({ name: "Diğer", value: other, color: COLORS.other });
    }

    return data;
  }, [stats]);

  const total = stats?.totalStores || 0;

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      const percentage = total > 0 ? ((data.value / total) * 100).toFixed(1) : 0;
      return (
        <div className="bg-popover border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground">{data.name}</p>
          <p className="text-sm text-muted-foreground">
            {data.value.toLocaleString("tr-TR")} mağaza
          </p>
          <p className="text-sm text-muted-foreground">%{percentage}</p>
        </div>
      );
    }
    return null;
  };

  const CustomLegend = ({ payload }: any) => {
    return (
      <div className="flex flex-wrap gap-3 justify-center mt-2">
        {payload?.map((entry: any, index: number) => {
          const percentage = total > 0 ? ((entry.payload.value / total) * 100).toFixed(0) : 0;
          return (
            <div key={index} className="flex items-center gap-1.5 text-xs">
              <div
                className="w-2.5 h-2.5 rounded-full"
                style={{ backgroundColor: entry.color }}
              />
              <span className="text-muted-foreground">
                {entry.value} ({percentage}%)
              </span>
            </div>
          );
        })}
      </div>
    );
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

  if (chartData.length === 0) {
    return (
      <div className="bg-card rounded-xl border border-border p-6">
        <div className="flex items-center gap-2 mb-4">
          <div className="p-2 rounded-lg bg-purple-100 dark:bg-purple-900/40">
            <Store className="h-4 w-4 text-purple-600 dark:text-purple-400" />
          </div>
          <h3 className="font-semibold text-foreground">Mağaza Durumu</h3>
        </div>
        <div className="h-[280px] flex items-center justify-center text-muted-foreground">
          Mağaza verisi bulunamadı
        </div>
      </div>
    );
  }

  return (
    <div className="bg-card rounded-xl border border-border p-6">
      <div className="flex items-center gap-2 mb-4">
        <div className="p-2 rounded-lg bg-purple-100 dark:bg-purple-900/40">
          <Store className="h-4 w-4 text-purple-600 dark:text-purple-400" />
        </div>
        <h3 className="font-semibold text-foreground">Mağaza Durumu</h3>
      </div>
      <div className="h-[280px] relative">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="45%"
              innerRadius={55}
              outerRadius={85}
              paddingAngle={3}
              dataKey="value"
            >
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
            <Legend content={<CustomLegend />} />
          </PieChart>
        </ResponsiveContainer>
        {/* Center text */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none" style={{ marginTop: "-20px" }}>
          <div className="text-center">
            <p className="text-2xl font-bold text-foreground">{total}</p>
            <p className="text-xs text-muted-foreground">Toplam</p>
          </div>
        </div>
      </div>
    </div>
  );
}
