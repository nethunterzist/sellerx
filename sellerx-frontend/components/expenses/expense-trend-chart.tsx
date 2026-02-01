"use client";

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from "recharts";
import type { StoreExpense } from "@/types/expense";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ExpenseTrendChartProps {
  expenses: StoreExpense[];
}

// Frequency colors matching expense-list
const FREQUENCY_COLORS: Record<string, string> = {
  DAILY: "#ef4444",     // red
  WEEKLY: "#f97316",    // orange
  MONTHLY: "#3b82f6",   // blue
  YEARLY: "#8b5cf6",    // purple
  ONE_TIME: "#6b7280",  // gray
};

const FREQUENCY_LABELS: Record<string, string> = {
  DAILY: "Günlük",
  WEEKLY: "Haftalık",
  MONTHLY: "Aylık",
  YEARLY: "Yıllık",
  ONE_TIME: "Tek Seferlik",
};

export function ExpenseTrendChart({ expenses }: ExpenseTrendChartProps) {
  const { formatCurrency } = useCurrency();

  // Aggregate expenses by frequency
  const frequencyTotals: Record<string, number> = {};
  expenses.forEach((e) => {
    frequencyTotals[e.frequency] = (frequencyTotals[e.frequency] || 0) + e.amount;
  });

  // Convert to chart data and sort by amount
  const chartData = Object.entries(frequencyTotals)
    .map(([frequency, total]) => ({
      frequency,
      label: FREQUENCY_LABELS[frequency] || frequency,
      total,
      color: FREQUENCY_COLORS[frequency] || "#94a3b8",
    }))
    .sort((a, b) => b.total - a.total);

  if (chartData.length === 0) {
    return (
      <div className="bg-card rounded-lg border border-border p-6 h-full">
        <h3 className="font-semibold text-foreground mb-4">Sıklık Bazlı Dağılım</h3>
        <div className="h-[250px] flex items-center justify-center text-muted-foreground">
          Gider verisi bulunamadı
        </div>
      </div>
    );
  }

  // Custom tooltip
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="bg-popover border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground">{data.label}</p>
          <p className="text-sm text-muted-foreground">
            {formatCurrency(data.total)}
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="bg-card rounded-lg border border-border p-6 h-full">
      <h3 className="font-semibold text-foreground mb-4">Sıklık Bazlı Dağılım</h3>
      <div className="h-[280px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={chartData}
            layout="vertical"
            margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" horizontal={true} vertical={false} stroke="hsl(var(--border))" />
            <XAxis
              type="number"
              tickFormatter={(value) => `₺${(value / 1000).toFixed(0)}K`}
              stroke="hsl(var(--muted-foreground))"
              fontSize={12}
            />
            <YAxis
              type="category"
              dataKey="label"
              width={80}
              stroke="hsl(var(--muted-foreground))"
              fontSize={12}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(0, 0, 0, 0.05)' }} />
            <Bar dataKey="total" radius={[0, 4, 4, 0]}>
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
