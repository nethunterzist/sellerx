"use client";

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from "recharts";
import type { StoreExpense } from "@/types/expense";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ExpenseCategoryChartProps {
  expenses: StoreExpense[];
}

// Category colors
const CATEGORY_COLORS: Record<string, string> = {
  "Ambalaj": "#3b82f6",      // blue
  "Kargo": "#f97316",        // orange
  "Reklam": "#ef4444",       // red
  "Muhasebe": "#8b5cf6",     // purple
  "Ofis": "#22c55e",         // green
  "Diğer": "#6b7280",        // gray
};

const DEFAULT_COLOR = "#94a3b8";

export function ExpenseCategoryChart({ expenses }: ExpenseCategoryChartProps) {
  const { formatCurrency } = useCurrency();

  // Aggregate expenses by category
  const categoryTotals: Record<string, number> = {};
  expenses.forEach((e) => {
    const category = e.expenseCategoryName || "Diğer";
    categoryTotals[category] = (categoryTotals[category] || 0) + e.amount;
  });

  // Convert to chart data
  const chartData = Object.entries(categoryTotals)
    .map(([name, value]) => ({
      name,
      value,
      color: CATEGORY_COLORS[name] || DEFAULT_COLOR,
    }))
    .sort((a, b) => b.value - a.value);

  const total = chartData.reduce((sum, item) => sum + item.value, 0);

  if (chartData.length === 0) {
    return (
      <div className="bg-card rounded-lg border border-border p-6">
        <h3 className="font-semibold text-foreground mb-4">Kategori Dağılımı</h3>
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
      const percentage = ((data.value / total) * 100).toFixed(1);
      return (
        <div className="bg-popover border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground">{data.name}</p>
          <p className="text-sm text-muted-foreground">
            {formatCurrency(data.value)}
          </p>
          <p className="text-sm text-muted-foreground">
            %{percentage}
          </p>
        </div>
      );
    }
    return null;
  };

  // Custom legend
  const CustomLegend = ({ payload }: any) => {
    return (
      <div className="flex flex-wrap gap-3 justify-center mt-4">
        {payload.map((entry: any, index: number) => {
          const percentage = ((entry.payload.value / total) * 100).toFixed(0);
          return (
            <div key={index} className="flex items-center gap-1.5 text-sm">
              <div
                className="w-3 h-3 rounded-full"
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

  return (
    <div className="bg-card rounded-lg border border-border p-6">
      <h3 className="font-semibold text-foreground mb-4">Kategori Dağılımı</h3>
      <div className="h-[280px]">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="45%"
              innerRadius={50}
              outerRadius={90}
              paddingAngle={2}
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
      </div>
    </div>
  );
}
