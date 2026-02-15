'use client';

import { useMemo } from 'react';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
} from 'recharts';

import type { CostLineItem } from '@/types/calculator';
import { formatCurrency } from '@/lib/utils/calculator';

interface CalculatorCostChartProps {
  costs: CostLineItem[];
}

const COLOR_MAP: Record<string, string> = {
  'Alış Maliyeti': '#3b82f6',       // blue
  'Trendyol Komisyon': '#ef4444',    // red
  'Kargo Masrafı': '#f97316',        // orange
  'Reklam Maliyeti': '#f59e0b',      // amber
  'Nakliyat': '#a855f7',             // purple
  'Paketleme Maliyeti': '#06b6d4',   // cyan
  'Trendyol Platform Hizmet Bedeli': '#6b7280', // gray
  'Stopaj': '#ec4899',               // pink
  'ÖTV (Özel Tüketim Vergisi)': '#14b8a6', // teal
};

const FALLBACK_COLORS = ['#3b82f6', '#ef4444', '#f97316', '#f59e0b', '#a855f7', '#06b6d4', '#6b7280', '#ec4899'];

function getColor(name: string, index: number): string {
  return COLOR_MAP[name] || FALLBACK_COLORS[index % FALLBACK_COLORS.length];
}

interface CustomTooltipProps {
  active?: boolean;
  payload?: Array<{
    payload: { name: string; value: number; percentage: number; fill: string };
  }>;
}

function CustomTooltip({ active, payload }: CustomTooltipProps) {
  if (!active || !payload?.length) return null;
  const data = payload[0].payload;
  return (
    <div className="bg-popover border border-border rounded-lg shadow-lg p-3">
      <div className="flex items-center gap-2 mb-1">
        <div
          className="h-2.5 w-2.5 rounded-full"
          style={{ backgroundColor: data.fill }}
        />
        <span className="text-sm font-medium text-foreground">{data.name}</span>
      </div>
      <p className="text-sm text-foreground font-semibold">{formatCurrency(data.value)}</p>
      <p className="text-xs text-muted-foreground">%{data.percentage.toFixed(1)}</p>
    </div>
  );
}

export function CalculatorCostChart({ costs }: CalculatorCostChartProps) {
  const chartData = useMemo(() => {
    const filtered = costs.filter((c) => c.amountExVat > 0);
    const total = filtered.reduce((sum, c) => sum + c.amountExVat, 0);
    return filtered.map((c, i) => ({
      name: c.name,
      value: c.amountExVat,
      percentage: total > 0 ? (c.amountExVat / total) * 100 : 0,
      fill: getColor(c.name, i),
    }));
  }, [costs]);

  const totalCost = useMemo(
    () => chartData.reduce((sum, d) => sum + d.value, 0),
    [chartData]
  );

  if (chartData.length === 0) {
    return (
      <div className="flex items-center justify-center h-[200px] text-sm text-muted-foreground">
        Maliyet verisi giriniz
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="relative h-[180px]">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={50}
              outerRadius={80}
              paddingAngle={2}
              dataKey="value"
              stroke="none"
            >
              {chartData.map((entry, index) => (
                <Cell key={index} fill={entry.fill} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
          </PieChart>
        </ResponsiveContainer>
        {/* Center overlay */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <div className="text-center">
            <p className="text-xs text-muted-foreground">Toplam</p>
            <p className="text-sm font-bold text-foreground">{formatCurrency(totalCost)}</p>
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="grid grid-cols-2 gap-x-4 gap-y-1.5 pl-1 pr-3">
        {chartData.map((item, i) => (
          <div key={i} className="flex items-center gap-2 min-w-0">
            <div
              className="h-2.5 w-2.5 rounded-full shrink-0"
              style={{ backgroundColor: item.fill }}
            />
            <span className="text-xs text-muted-foreground truncate">{item.name}</span>
            <span className="text-xs font-medium text-foreground ml-auto shrink-0">
              %{item.percentage.toFixed(0)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
