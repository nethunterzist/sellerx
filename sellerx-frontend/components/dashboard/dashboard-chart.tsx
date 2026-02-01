"use client";

import {
  ComposedChart,
  Bar,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ChartDataPoint {
  date: string;
  displayDate: string;
  units: number;
  revenue: number;
  netProfit: number;
  refunds: number;
}

interface DashboardChartProps {
  data: ChartDataPoint[];
}

export function DashboardChart({ data }: DashboardChartProps) {
  const { formatCurrency, getCurrencySymbol } = useCurrency();

  function CustomTooltip({ active, payload, label }: any) {
    if (!active || !payload || !payload.length) return null;

    return (
      <div className="bg-card border border-border rounded-lg shadow-lg p-3">
        <p className="font-medium text-foreground mb-2">{label}</p>
        {payload.map((entry: any, index: number) => (
          <div key={index} className="flex items-center gap-2 text-sm">
            <div
              className="w-3 h-3 rounded"
              style={{ backgroundColor: entry.color }}
            />
            <span className="text-muted-foreground">{entry.name}:</span>
            <span className="font-medium text-foreground">
              {entry.name === "Adet" || entry.name === "İade"
                ? entry.value
                : formatCurrency(entry.value)}
            </span>
          </div>
        ))}
      </div>
    );
  }
  if (!data || data.length === 0) {
    return (
      <div className="h-[480px] flex items-center justify-center text-muted-foreground">
        Grafik verisi bulunamadı
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={480}>
      <ComposedChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
        <XAxis
          dataKey="displayDate"
          tick={{ fontSize: 12, fill: "#6B7280" }}
          tickLine={false}
          axisLine={{ stroke: "#E5E7EB" }}
        />
        <YAxis
          yAxisId="left"
          tick={{ fontSize: 12, fill: "#6B7280" }}
          tickLine={false}
          axisLine={{ stroke: "#E5E7EB" }}
          tickFormatter={(value) => value.toLocaleString("tr-TR")}
        />
        <YAxis
          yAxisId="right"
          orientation="right"
          tick={{ fontSize: 12, fill: "#6B7280" }}
          tickLine={false}
          axisLine={{ stroke: "#E5E7EB" }}
          tickFormatter={(value) => `${getCurrencySymbol()}${(value / 1000).toFixed(0)}K`}
        />
        <Tooltip content={<CustomTooltip />} />
        <Legend
          iconType="square"
          wrapperStyle={{ paddingTop: "10px" }}
        />
        <Bar
          yAxisId="left"
          dataKey="units"
          name="Adet"
          fill="#14B8A6"
          radius={[4, 4, 0, 0]}
          barSize={20}
        />
        <Line
          yAxisId="right"
          type="monotone"
          dataKey="revenue"
          name="Satış"
          stroke="#3B82F6"
          strokeWidth={2}
          dot={{ fill: "#3B82F6", strokeWidth: 2, r: 4 }}
        />
        <Line
          yAxisId="right"
          type="monotone"
          dataKey="netProfit"
          name="Net Kâr"
          stroke="#10B981"
          strokeWidth={2}
          dot={{ fill: "#10B981", strokeWidth: 2, r: 4 }}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
