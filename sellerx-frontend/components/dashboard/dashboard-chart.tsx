"use client";

import { useMemo } from "react";
import {
  ComposedChart,
  Bar,
  Line,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  CHART_METRICS,
  DEFAULT_SELECTED_METRICS,
  type ChartMetricConfig,
  type ExtendedChartDataPoint,
} from "@/types/chart-metrics";

interface DashboardChartProps {
  data: ExtendedChartDataPoint[];
  selectedMetrics?: string[];
}

export function DashboardChart({
  data,
  selectedMetrics = DEFAULT_SELECTED_METRICS,
}: DashboardChartProps) {
  const { formatCurrency, getCurrencySymbol } = useCurrency();

  // Get metric configs for selected metrics
  const activeMetrics = useMemo(() => {
    return selectedMetrics
      .map((id) => CHART_METRICS.find((m) => m.id === id))
      .filter((m): m is ChartMetricConfig => m !== undefined);
  }, [selectedMetrics]);

  // Check if we need both Y-axes
  const hasLeftAxis = activeMetrics.some((m) => m.yAxisId === "left");
  const hasRightAxis = activeMetrics.some((m) => m.yAxisId === "right");

  // Format value based on metric formatter type
  const formatValue = (value: number, formatter: ChartMetricConfig["formatter"]) => {
    switch (formatter) {
      case "currency":
        return formatCurrency(value);
      case "percentage":
        return `%${value.toFixed(1)}`;
      case "number":
      default:
        return value.toLocaleString("tr-TR");
    }
  };

  function CustomTooltip({ active, payload, label }: any) {
    if (!active || !payload || !payload.length) return null;

    return (
      <div className="bg-card border border-border rounded-lg shadow-lg p-3">
        <p className="font-medium text-foreground mb-2">{label}</p>
        {payload.map((entry: any, index: number) => {
          // Find metric config for this entry
          const metric = activeMetrics.find((m) => m.dataKey === entry.dataKey);
          const formattedValue = metric
            ? formatValue(entry.value, metric.formatter)
            : entry.value;

          return (
            <div key={index} className="flex items-center gap-2 text-sm">
              <div
                className="w-3 h-3 rounded"
                style={{ backgroundColor: entry.color }}
              />
              <span className="text-muted-foreground">{entry.name}:</span>
              <span className="font-medium text-foreground">{formattedValue}</span>
            </div>
          );
        })}
      </div>
    );
  }

  // Custom legend formatter
  function renderLegend(props: any) {
    const { payload } = props;
    return (
      <div className="flex flex-wrap justify-center gap-4 pt-2">
        {payload.map((entry: any, index: number) => (
          <div key={index} className="flex items-center gap-1.5 text-sm">
            <div
              className="w-3 h-3 rounded-sm"
              style={{ backgroundColor: entry.color }}
            />
            <span className="text-muted-foreground">{entry.value}</span>
          </div>
        ))}
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="h-[480px] flex items-center justify-center text-muted-foreground">
        Grafik verisi bulunamadi
      </div>
    );
  }

  if (activeMetrics.length === 0) {
    return (
      <div className="h-[480px] flex items-center justify-center text-muted-foreground">
        Lutfen en az bir metrik seciniz
      </div>
    );
  }

  // Render appropriate chart component based on metric type
  const renderMetricComponent = (metric: ChartMetricConfig) => {
    const commonProps = {
      key: metric.id,
      yAxisId: metric.yAxisId,
      dataKey: metric.dataKey,
      name: metric.shortLabel,
    };

    switch (metric.type) {
      case "bar":
        return (
          <Bar
            {...commonProps}
            fill={metric.color}
            radius={[4, 4, 0, 0]}
            barSize={activeMetrics.filter((m) => m.type === "bar").length > 1 ? 15 : 20}
          />
        );
      case "area":
        return (
          <Area
            {...commonProps}
            type="monotone"
            stroke={metric.color}
            fill={metric.color}
            fillOpacity={0.2}
            strokeWidth={2}
          />
        );
      case "line":
      default:
        return (
          <Line
            {...commonProps}
            type="monotone"
            stroke={metric.color}
            strokeWidth={2}
            dot={{ fill: metric.color, strokeWidth: 2, r: 4 }}
          />
        );
    }
  };

  return (
    <ResponsiveContainer width="100%" height={480}>
      <ComposedChart
        data={data}
        margin={{ top: 5, right: hasRightAxis ? 30 : 5, left: 20, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
        <XAxis
          dataKey="displayDate"
          tick={{ fontSize: 12, fill: "#6B7280" }}
          tickLine={false}
          axisLine={{ stroke: "#E5E7EB" }}
        />
        {hasLeftAxis && (
          <YAxis
            yAxisId="left"
            tick={{ fontSize: 12, fill: "#6B7280" }}
            tickLine={false}
            axisLine={{ stroke: "#E5E7EB" }}
            tickFormatter={(value) => {
              // Check if any left-axis metric is percentage
              const hasPercentage = activeMetrics.some(
                (m) => m.yAxisId === "left" && m.formatter === "percentage"
              );
              if (hasPercentage) {
                return `%${value}`;
              }
              return value.toLocaleString("tr-TR");
            }}
          />
        )}
        {hasRightAxis && (
          <YAxis
            yAxisId="right"
            orientation="right"
            tick={{ fontSize: 12, fill: "#6B7280" }}
            tickLine={false}
            axisLine={{ stroke: "#E5E7EB" }}
            tickFormatter={(value) =>
              `${getCurrencySymbol()}${(value / 1000).toFixed(0)}K`
            }
          />
        )}
        <Tooltip content={<CustomTooltip />} />
        <Legend content={renderLegend} />
        {activeMetrics.map(renderMetricComponent)}
      </ComposedChart>
    </ResponsiveContainer>
  );
}
