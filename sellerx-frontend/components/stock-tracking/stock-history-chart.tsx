"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from "recharts";
import { TrendingUp } from "lucide-react";
import type { StockSnapshot } from "@/types/stock-tracking";
import { useTranslations } from "next-intl";
import { useLocale } from "next-intl";
import { format } from "date-fns";
import { tr, enUS } from "date-fns/locale";

interface StockHistoryChartProps {
  snapshots: StockSnapshot[] | undefined;
  lowStockThreshold: number;
  isLoading: boolean;
}

export function StockHistoryChart({
  snapshots,
  lowStockThreshold,
  isLoading,
}: StockHistoryChartProps) {
  const t = useTranslations("stockTracking");
  const locale = useLocale();

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5" />
            {t("chart.title")}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] animate-pulse bg-gray-100 rounded" />
        </CardContent>
      </Card>
    );
  }

  if (!snapshots || snapshots.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5" />
            {t("chart.title")}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] flex items-center justify-center text-muted-foreground">
            {t("chart.noData")}
          </div>
        </CardContent>
      </Card>
    );
  }

  // Sort snapshots by date and format for chart
  const chartData = [...snapshots]
    .sort((a, b) => new Date(a.checkedAt).getTime() - new Date(b.checkedAt).getTime())
    .map((snapshot) => ({
      date: format(new Date(snapshot.checkedAt), "dd MMM HH:mm", {
        locale: locale === "tr" ? tr : enUS,
      }),
      fullDate: format(new Date(snapshot.checkedAt), "PPpp", {
        locale: locale === "tr" ? tr : enUS,
      }),
      quantity: snapshot.quantity,
      price: snapshot.price,
      change: snapshot.quantityChange,
    }));

  const maxQuantity = Math.max(...chartData.map((d) => d.quantity), lowStockThreshold * 2);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <TrendingUp className="h-5 w-5" />
          {t("chart.title")}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 11 }}
                tickLine={false}
                axisLine={false}
                className="text-muted-foreground"
              />
              <YAxis
                domain={[0, maxQuantity]}
                tick={{ fontSize: 11 }}
                tickLine={false}
                axisLine={false}
                className="text-muted-foreground"
              />
              <Tooltip
                content={({ active, payload }) => {
                  if (!active || !payload?.length) return null;
                  const data = payload[0].payload;
                  return (
                    <div className="bg-background border rounded-lg shadow-lg p-3">
                      <p className="text-xs text-muted-foreground mb-1">{data.fullDate}</p>
                      <p className="font-semibold">
                        {t("chart.stock")}: {data.quantity} {t("table.unit")}
                      </p>
                      {data.change !== null && (
                        <p
                          className={`text-sm ${
                            data.change > 0
                              ? "text-green-600"
                              : data.change < 0
                              ? "text-red-600"
                              : "text-muted-foreground"
                          }`}
                        >
                          {t("chart.change")}: {data.change > 0 ? "+" : ""}
                          {data.change}
                        </p>
                      )}
                      {data.price && (
                        <p className="text-sm text-muted-foreground">
                          {t("chart.price")}: {data.price.toLocaleString()} TL
                        </p>
                      )}
                    </div>
                  );
                }}
              />
              <ReferenceLine
                y={lowStockThreshold}
                stroke="#f97316"
                strokeDasharray="5 5"
                label={{
                  value: t("chart.lowStockLine"),
                  position: "right",
                  fill: "#f97316",
                  fontSize: 11,
                }}
              />
              <ReferenceLine
                y={0}
                stroke="#ef4444"
                strokeDasharray="3 3"
              />
              <Line
                type="stepAfter"
                dataKey="quantity"
                stroke="hsl(var(--primary))"
                strokeWidth={2}
                dot={{ fill: "hsl(var(--primary))", r: 3 }}
                activeDot={{ r: 5, fill: "hsl(var(--primary))" }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
        <div className="flex items-center justify-center gap-6 mt-4 text-sm">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full bg-primary" />
            <span className="text-muted-foreground">{t("chart.stockLevel")}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-6 h-0.5 bg-orange-500" style={{ borderStyle: "dashed" }} />
            <span className="text-muted-foreground">{t("chart.lowStockThreshold")}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
