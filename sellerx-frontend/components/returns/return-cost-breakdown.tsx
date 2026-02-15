"use client";

import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from "recharts";
import { Skeleton } from "@/components/ui/skeleton";
import type { ReturnCostBreakdown as CostBreakdownType } from "@/types/returns";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ReturnCostBreakdownProps {
  costBreakdown: CostBreakdownType;
  isLoading?: boolean;
}

const COLORS = ["#EF4444", "#F97316", "#EAB308", "#22C55E", "#8B5CF6"];

export function ReturnCostBreakdown({
  costBreakdown,
  isLoading = false,
}: ReturnCostBreakdownProps) {
  const { formatCurrency } = useCurrency();

  const data = [
    { name: "Ürün Maliyeti", value: costBreakdown.productCost, color: COLORS[0] },
    { name: "Gidiş Kargo", value: costBreakdown.shippingCostOut, color: COLORS[1] },
    { name: "Dönüş Kargo", value: costBreakdown.shippingCostReturn, color: COLORS[2] },
    { name: "Ambalaj", value: costBreakdown.packagingCost, color: COLORS[3] },
  ].filter((item) => item.value > 0);

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      const percentage = ((data.value / costBreakdown.totalLoss) * 100).toFixed(1);
      return (
        <div className="bg-card p-3 rounded-lg shadow-lg border border-border">
          <p className="font-medium text-foreground">{data.name}</p>
          <p className="text-muted-foreground">{formatCurrency(data.value)}</p>
          <p className="text-sm text-muted-foreground">%{percentage}</p>
        </div>
      );
    }
    return null;
  };

  const renderLegend = (props: any) => {
    const { payload } = props;
    return (
      <ul className="flex flex-wrap justify-center gap-4 mt-4">
        {payload.map((entry: any, index: number) => (
          <li key={`legend-${index}`} className="flex items-center gap-2 text-sm">
            <span
              className="w-3 h-3 rounded-full"
              style={{ backgroundColor: entry.color }}
            />
            <span className="text-muted-foreground">{entry.value}</span>
          </li>
        ))}
      </ul>
    );
  };

  if (isLoading) {
    return (
      <div className="bg-card rounded-lg border border-border p-6">
        <div className="flex items-center justify-between mb-4">
          <Skeleton className="h-6 w-32" />
          <div className="text-right space-y-1">
            <Skeleton className="h-4 w-20 ml-auto" />
            <Skeleton className="h-6 w-24 ml-auto" />
          </div>
        </div>
        <div className="h-64 flex items-center justify-center">
          <Skeleton className="h-40 w-40 rounded-full" />
        </div>
        <div className="flex justify-center gap-4 mt-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="flex items-center gap-2">
              <Skeleton className="h-3 w-3 rounded-full" />
              <Skeleton className="h-4 w-16" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="bg-card rounded-lg border border-border p-6">
        <h3 className="text-lg font-semibold text-foreground mb-4">Zarar Kırılımı</h3>
        <div className="h-64 flex items-center justify-center">
          <p className="text-muted-foreground">Henüz zarar verisi bulunmuyor</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-card rounded-lg border border-border p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-foreground">Zarar Kırılımı</h3>
        <div className="text-right">
          <p className="text-sm text-muted-foreground">Toplam Zarar</p>
          <p className="text-lg font-bold text-red-600">
            {formatCurrency(costBreakdown.totalLoss)}
          </p>
        </div>
      </div>
      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={80}
              paddingAngle={2}
              dataKey="value"
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
            <Legend content={renderLegend} />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
