"use client";

import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import { TrendsTable, type TrendMetricType } from "./trends-table";
import type { DashboardStatsResponse } from "@/types/dashboard";
import type { TrendyolProduct } from "@/types/product";
import {
  BarChart3,
  Package,
  ShoppingCart,
  RotateCcw,
  Percent,
  TrendingUp,
  Receipt,
  Wallet,
} from "lucide-react";

interface TrendsViewProps {
  stats: DashboardStatsResponse | null | undefined;
  isLoading?: boolean;
  trendyolProductMap?: Map<string, TrendyolProduct>;
  selectedProducts?: string[];
}

// Sellerboard-style metric tabs
const trendMetrics: { id: TrendMetricType; label: string; icon: React.ElementType }[] = [
  { id: "revenue", label: "Satış", icon: BarChart3 },
  { id: "units", label: "Adet", icon: Package },
  { id: "orders", label: "Sipariş", icon: ShoppingCart },
  { id: "refunds", label: "İade", icon: RotateCcw },
  { id: "commission", label: "Komisyon", icon: Percent },
  { id: "profit", label: "Brüt Kâr", icon: TrendingUp },
  { id: "netProfit", label: "Net Kâr", icon: Wallet },
  { id: "margin", label: "Marj", icon: Receipt },
];

type PeriodKey = "today" | "yesterday" | "thisMonth" | "lastMonth";

export interface ProductTrendData {
  barcode: string;
  name: string;
  image?: string;
  productUrl?: string;
  periods: {
    today?: ProductPeriodMetrics;
    yesterday?: ProductPeriodMetrics;
    thisMonth?: ProductPeriodMetrics;
    lastMonth?: ProductPeriodMetrics;
  };
}

export interface ProductPeriodMetrics {
  revenue: number;
  units: number;
  orders: number;
  refunds: number;
  commission: number;
  profit: number;
  netProfit: number;
  margin: number;
}

export function TrendsView({ stats, isLoading, trendyolProductMap, selectedProducts }: TrendsViewProps) {
  const [selectedMetric, setSelectedMetric] = useState<TrendMetricType>("revenue");

  // Tüm dönemlerdeki ürünleri birleştir
  const productTrends = useMemo<ProductTrendData[]>(() => {
    if (!stats) return [];

    const productMap = new Map<string, ProductTrendData>();
    const periodsToProcess: PeriodKey[] = ["today", "yesterday", "thisMonth", "lastMonth"];

    periodsToProcess.forEach((period) => {
      const periodData = stats[period];
      if (!periodData?.products) return;

      periodData.products.forEach((p) => {
        if (!p.barcode) return;

        // Find matching TrendyolProduct for productUrl
        const trendyolProduct = trendyolProductMap?.get(p.barcode);

        if (!productMap.has(p.barcode)) {
          productMap.set(p.barcode, {
            barcode: p.barcode,
            name: p.productName,
            image: p.image || trendyolProduct?.image,
            productUrl: trendyolProduct?.productUrl,
            periods: {},
          });
        }

        const product = productMap.get(p.barcode)!;

        // Update image if available
        if (p.image && !product.image) {
          product.image = p.image;
        }
        // Update productUrl if available
        if (trendyolProduct?.productUrl && !product.productUrl) {
          product.productUrl = trendyolProduct.productUrl;
        }

        const revenue = p.revenue || 0;
        const grossProfit = p.grossProfit || 0;
        const commission = p.estimatedCommission || 0;
        // Use backend-calculated values, fallback to estimation if not available
        const netProfit = p.netProfit ?? (grossProfit - commission);
        const margin = p.profitMargin ?? (revenue > 0 ? (grossProfit / revenue) * 100 : 0);

        product.periods[period] = {
          revenue,
          units: p.totalSoldQuantity || 0,
          orders: p.orderCount ?? 0, // Default 0 if null/undefined
          refunds: p.returnQuantity || 0,
          commission,
          profit: grossProfit,
          netProfit,
          margin,
        };
      });
    });

    // Filter by selected products, then sort by total revenue (descending)
    return Array.from(productMap.values())
      .filter((product) => {
        // If no products selected, show all
        if (!selectedProducts || selectedProducts.length === 0) return true;
        // Filter by selected product barcodes
        return selectedProducts.includes(product.barcode);
      })
      .sort((a, b) => {
        const aTotal = Object.values(a.periods).reduce((sum, p) => sum + (p?.revenue || 0), 0);
        const bTotal = Object.values(b.periods).reduce((sum, p) => sum + (p?.revenue || 0), 0);
        return bTotal - aTotal;
      });
  }, [stats, trendyolProductMap, selectedProducts]);

  return (
    <div className="bg-card rounded-lg border border-border overflow-hidden">
      {/* Sellerboard-style Metric Tabs */}
      <div className="flex items-center gap-1 p-3 border-b border-border bg-muted overflow-x-auto">
        {trendMetrics.map((m) => {
          const Icon = m.icon;
          const isActive = selectedMetric === m.id;
          return (
            <button
              key={m.id}
              onClick={() => setSelectedMetric(m.id)}
              className={cn(
                "flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-md transition-all whitespace-nowrap",
                isActive
                  ? "bg-[#1D70F1] text-white shadow-sm"
                  : "bg-card text-muted-foreground hover:bg-muted border border-border"
              )}
            >
              <Icon className="h-3.5 w-3.5" />
              {m.label}
            </button>
          );
        })}
      </div>

      {/* Trend Tablosu */}
      <TrendsTable
        products={productTrends}
        metric={selectedMetric}
        isLoading={isLoading}
      />
    </div>
  );
}
