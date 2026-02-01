"use client";

import { useMemo } from "react";
import { cn } from "@/lib/utils";
import { DashboardChart } from "./dashboard-chart";
import { Skeleton } from "@/components/ui/skeleton";
import type { DashboardStatsResponse } from "@/types/dashboard";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ChartViewProps {
  stats: DashboardStatsResponse | null | undefined;
  isLoading?: boolean;
  selectedProducts?: string[];
}

function formatPercentage(value: number): string {
  return `${value.toFixed(1)}%`;
}

interface MetricRowProps {
  label: string;
  value: string;
  isNegative?: boolean;
  isBold?: boolean;
}

function MetricRow({ label, value, isNegative, isBold }: MetricRowProps) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-border last:border-b-0">
      <span className={cn("text-sm text-muted-foreground", isBold && "font-semibold text-foreground")}>
        {label}
      </span>
      <span
        className={cn(
          "text-sm font-medium",
          isBold && "font-semibold",
          isNegative ? "text-red-600" : "text-foreground"
        )}
      >
        {value}
      </span>
    </div>
  );
}

function MetricRowSkeleton() {
  return (
    <div className="flex items-center justify-between py-2 border-b border-border">
      <Skeleton className="h-4 w-24" />
      <Skeleton className="h-4 w-16" />
    </div>
  );
}

export function ChartView({ stats, isLoading, selectedProducts = [] }: ChartViewProps) {
  const { formatCurrency } = useCurrency();

  // Orders'dan günlük veri çıkar (ürün filtrelemeli)
  const dailyData = useMemo(() => {
    if (!stats?.thisMonth?.orders) return [];

    const ordersByDate = new Map<string, {
      date: string;
      displayDate: string;
      units: number;
      revenue: number;
      netProfit: number;
      refunds: number;
    }>();

    stats.thisMonth.orders.forEach((order) => {
      // Ürün filtresi varsa sadece seçili ürünleri al
      let products = order.products || [];
      if (selectedProducts.length > 0) {
        products = products.filter((p) => selectedProducts.includes(p.barcode));
        // Bu siparişte seçili ürün yoksa atla
        if (products.length === 0) return;
      }

      const date = order.orderDate.split("T")[0];
      const displayDate = new Date(date).toLocaleDateString("tr-TR", {
        day: "2-digit",
        month: "short",
      });

      if (!ordersByDate.has(date)) {
        ordersByDate.set(date, {
          date,
          displayDate,
          units: 0,
          revenue: 0,
          netProfit: 0,
          refunds: 0,
        });
      }

      const dayData = ordersByDate.get(date)!;

      // Filtrelenmiş ürünlerden metrikleri hesapla
      if (selectedProducts.length > 0) {
        // Filtrelenmiş ürünler üzerinden hesapla
        products.forEach((p) => {
          dayData.units += p.quantity || 0;
          dayData.revenue += p.totalPrice || 0;
          dayData.netProfit += (p.totalPrice || 0) - (p.totalCost || 0) - (p.commission || 0);
        });
      } else {
        // Sipariş bazında hesapla (orijinal davranış)
        const orderUnits = products.reduce((sum, p) => sum + (p.quantity || 0), 0);
        dayData.units += orderUnits;
        dayData.revenue += order.revenue || order.totalPrice || 0;
        dayData.netProfit += order.grossProfit || 0;
        dayData.refunds += order.returnPrice || 0;
      }
    });

    // Tarihe göre sırala
    return Array.from(ordersByDate.values()).sort((a, b) =>
      a.date.localeCompare(b.date)
    );
  }, [stats, selectedProducts]);

  // Özet metrikler (ürün filtrelemeli)
  const summary = useMemo(() => {
    const data = stats?.thisMonth;
    if (!data) return null;

    // Ürün filtresi varsa siparişlerden hesapla
    if (selectedProducts.length > 0 && data.orders) {
      let totalRevenue = 0;
      let totalProductsSold = 0;
      let totalOrders = 0;
      const returnCount = 0;
      const returnCost = 0;
      let totalEstimatedCommission = 0;
      let totalProductCosts = 0;

      data.orders.forEach((order) => {
        const filteredProducts = (order.products || []).filter((p) =>
          selectedProducts.includes(p.barcode)
        );

        if (filteredProducts.length > 0) {
          totalOrders++;
          filteredProducts.forEach((p) => {
            totalProductsSold += p.quantity || 0;
            totalRevenue += p.totalPrice || 0;
            totalProductCosts += p.totalCost || 0;
            totalEstimatedCommission += p.commission || 0;
          });
        }
      });

      const grossProfit = totalRevenue - totalProductCosts;
      const netProfit = grossProfit - totalEstimatedCommission;
      const margin = totalRevenue > 0 ? (grossProfit / totalRevenue) * 100 : 0;
      const roi = totalProductCosts > 0 ? (netProfit / totalProductCosts) * 100 : 0;

      return {
        totalRevenue,
        totalProductsSold,
        totalOrders,
        returnCount,
        returnCost,
        totalEstimatedCommission,
        totalProductCosts,
        totalExpenseAmount: 0, // Giderler ürün bazında filtrelenmez
        grossProfit,
        netProfit,
        margin,
        roi,
      };
    }

    // Filtre yoksa orijinal hesaplama
    const netProfit = data.grossProfit - data.totalEstimatedCommission - data.totalExpenseAmount;
    const margin = data.totalRevenue > 0
      ? (data.grossProfit / data.totalRevenue) * 100
      : 0;
    const roi = data.totalProductCosts > 0
      ? (netProfit / data.totalProductCosts) * 100
      : 0;

    return {
      totalRevenue: data.totalRevenue,
      totalProductsSold: data.totalProductsSold,
      totalOrders: data.totalOrders,
      returnCount: data.returnCount,
      returnCost: data.returnCost,
      totalEstimatedCommission: data.totalEstimatedCommission,
      totalProductCosts: data.totalProductCosts,
      totalExpenseAmount: data.totalExpenseAmount,
      grossProfit: data.grossProfit,
      netProfit,
      margin,
      roi,
    };
  }, [stats, selectedProducts]);

  if (isLoading) {
    return (
      <div className="bg-card rounded-lg border border-border">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 p-6 items-start">
          {/* Sol: Grafik Alanı */}
          <div className="lg:col-span-2">
            <Skeleton className="h-[480px] w-full" />
          </div>

          {/* Sağ: Özet Panel */}
          <div className="space-y-1 min-h-[480px]">
            <h3 className="font-semibold text-foreground mb-3">Bu Ay Özet</h3>
            {[...Array(12)].map((_, i) => (
              <MetricRowSkeleton key={i} />
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-card rounded-lg border border-border">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 p-6 items-start">
        {/* Sol: Grafik Alanı */}
        <div className="lg:col-span-2">
          <h3 className="font-semibold text-foreground mb-4">Günlük Performans</h3>
          <DashboardChart data={dailyData} />
        </div>

        {/* Sağ: Özet Panel */}
        <div className="space-y-1 min-h-[480px]">
          <h3 className="font-semibold text-foreground mb-3">Bu Ay Özet</h3>

          {summary ? (
            <>
              <MetricRow
                label="Satışlar"
                value={formatCurrency(summary.totalRevenue)}
                isBold
              />
              <MetricRow
                label="Adet"
                value={summary.totalProductsSold.toLocaleString("tr-TR")}
              />
              <MetricRow
                label="Sipariş"
                value={summary.totalOrders.toLocaleString("tr-TR")}
              />
              <MetricRow
                label="İade Adet"
                value={summary.returnCount.toLocaleString("tr-TR")}
              />
              <MetricRow
                label="İade Maliyeti"
                value={formatCurrency(-summary.returnCost)}
                isNegative={summary.returnCost > 0}
              />
              <MetricRow
                label="Komisyon"
                value={formatCurrency(-summary.totalEstimatedCommission)}
                isNegative
              />
              <MetricRow
                label="Ürün Maliyeti"
                value={formatCurrency(-summary.totalProductCosts)}
                isNegative
              />
              <MetricRow
                label="Giderler"
                value={formatCurrency(-summary.totalExpenseAmount)}
                isNegative
              />
              <div className="h-2" />
              <MetricRow
                label="Brüt Kâr"
                value={formatCurrency(summary.grossProfit)}
                isNegative={summary.grossProfit < 0}
                isBold
              />
              <MetricRow
                label="Net Kâr"
                value={formatCurrency(summary.netProfit)}
                isNegative={summary.netProfit < 0}
                isBold
              />
              <div className="h-2" />
              <MetricRow
                label="Marj"
                value={formatPercentage(summary.margin)}
                isNegative={summary.margin < 0}
              />
              <MetricRow
                label="ROI"
                value={formatPercentage(summary.roi)}
                isNegative={summary.roi < 0}
              />
            </>
          ) : (
            <p className="text-muted-foreground text-sm">Veri bulunamadı</p>
          )}
        </div>
      </div>
    </div>
  );
}
