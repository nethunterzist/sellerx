"use client";

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
import { AlertCircle } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";

interface ReturnReasonsChartProps {
  reasonDistribution: Record<string, number>;
  isLoading?: boolean;
}

const COLORS = [
  "#EF4444",
  "#F97316",
  "#EAB308",
  "#84CC16",
  "#22C55E",
  "#14B8A6",
  "#06B6D4",
  "#3B82F6",
  "#8B5CF6",
  "#EC4899",
];

// Mapping from Trendyol reason codes to Turkish labels
const reasonLabels: Record<string, string> = {
  SIZE_MISMATCH: "Beden Uyumsuzluğu",
  WRONG_PRODUCT: "Yanlış Ürün",
  DAMAGED: "Hasarlı Ürün",
  QUALITY_ISSUE: "Kalite Sorunu",
  CUSTOMER_CHANGED_MIND: "Müşteri Vazgeçti",
  NOT_AS_DESCRIBED: "Açıklamaya Uymuyor",
  LATE_DELIVERY: "Geç Teslimat",
  DEFECTIVE: "Arızalı Ürün",
  COLOR_MISMATCH: "Renk Uyumsuzluğu",
  OTHER: "Diğer",
};

export function ReturnReasonsChart({
  reasonDistribution,
  isLoading = false,
}: ReturnReasonsChartProps) {
  // Convert the distribution to chart data
  const data = Object.entries(reasonDistribution)
    .map(([reason, count]) => ({
      reason: reasonLabels[reason] || reason,
      count,
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 10); // Top 10 reasons

  const total = data.reduce((sum, item) => sum + item.count, 0);

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      const percentage = total > 0 ? ((data.count / total) * 100).toFixed(1) : 0;
      return (
        <div className="bg-card p-3 rounded-lg shadow-lg border border-border">
          <p className="font-medium text-foreground">{data.reason}</p>
          <p className="text-muted-foreground">{data.count} iade</p>
          <p className="text-sm text-muted-foreground">%{percentage}</p>
        </div>
      );
    }
    return null;
  };

  if (isLoading) {
    return (
      <div className="bg-card rounded-lg border border-border p-6">
        <div className="flex items-center justify-between mb-4">
          <Skeleton className="h-6 w-28" />
          <Skeleton className="h-4 w-24" />
        </div>
        <div className="h-64 space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex items-center gap-4">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-6 flex-1" style={{ maxWidth: `${100 - i * 15}%` }} />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="bg-card rounded-lg border border-border p-6">
        <h3 className="text-lg font-semibold text-foreground mb-4">İade Nedenleri</h3>
        <div className="h-64 flex items-center justify-center flex-col gap-2">
          <AlertCircle className="h-12 w-12 text-muted-foreground" />
          <p className="text-muted-foreground">Henüz iade nedeni verisi bulunmuyor</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-card rounded-lg border border-border p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-foreground">İade Nedenleri</h3>
        <span className="text-sm text-muted-foreground">Toplam: {total} iade</span>
      </div>
      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={data}
            layout="vertical"
            margin={{ top: 5, right: 30, left: 100, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" horizontal={true} vertical={false} />
            <XAxis type="number" />
            <YAxis
              type="category"
              dataKey="reason"
              tick={{ fontSize: 12 }}
              width={90}
            />
            <Tooltip content={<CustomTooltip />} />
            <Bar dataKey="count" radius={[0, 4, 4, 0]}>
              {data.map((_, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
