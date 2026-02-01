"use client";

import { LineChart, Line, ResponsiveContainer } from "recharts";

interface SparklineDataPoint {
  value: number;
}

interface SparklineProps {
  data: SparklineDataPoint[];
  color?: string;
  width?: number;
  height?: number;
}

export function Sparkline({
  data,
  color = "#14B8A6",
  width = 80,
  height = 30,
}: SparklineProps) {
  if (!data || data.length === 0) {
    return <div style={{ width, height }} className="bg-gray-100 rounded" />;
  }

  // Determine color based on trend
  const firstValue = data[0]?.value || 0;
  const lastValue = data[data.length - 1]?.value || 0;
  const trendColor = lastValue >= firstValue ? "#10B981" : "#EF4444";

  return (
    <div style={{ width, height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <Line
            type="monotone"
            dataKey="value"
            stroke={color === "auto" ? trendColor : color}
            dot={false}
            strokeWidth={1.5}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
