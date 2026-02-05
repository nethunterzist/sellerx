"use client";

import { Package, PackageX, AlertTriangle, Bell } from "lucide-react";
import { MetricCard, MetricCardSkeleton } from "@/components/ui/metric-card";
import type { StockTrackingDashboard } from "@/types/stock-tracking";
import { useTranslations } from "next-intl";

interface StockDashboardCardsProps {
  dashboard: StockTrackingDashboard | undefined;
  isLoading: boolean;
}

export function StockDashboardCards({ dashboard, isLoading }: StockDashboardCardsProps) {
  const t = useTranslations("stockTracking");

  if (isLoading) {
    return (
      <div className="flex gap-4 overflow-x-auto pb-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <MetricCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  return (
    <div className="flex gap-4 overflow-x-auto pb-2">
      <MetricCard
        title={t("dashboard.trackedProducts")}
        subtitle="Aktif ürünler"
        icon={Package}
        headerColor="bg-blue-500"
        metricValue={dashboard?.activeTrackedProducts ?? 0}
        metricSuffix={dashboard?.totalTrackedProducts ? `/ ${dashboard.totalTrackedProducts}` : undefined}
        footerStats={[
          { label: "Toplam", value: dashboard?.totalTrackedProducts ?? 0 },
        ]}
      />

      <MetricCard
        title={t("dashboard.outOfStock")}
        subtitle="Kritik ürünler"
        icon={PackageX}
        headerColor="bg-red-500"
        metricValue={dashboard?.outOfStockProducts ?? 0}
        metricColor={(dashboard?.outOfStockProducts ?? 0) > 0 ? "text-red-600" : undefined}
      />

      <MetricCard
        title={t("dashboard.lowStock")}
        subtitle="Eşik altında"
        icon={AlertTriangle}
        headerColor="bg-orange-500"
        metricValue={dashboard?.lowStockProducts ?? 0}
        metricColor={(dashboard?.lowStockProducts ?? 0) > 0 ? "text-orange-600" : undefined}
      />

      <MetricCard
        title={t("dashboard.unreadAlerts")}
        subtitle="Bekleyen uyarılar"
        icon={Bell}
        headerColor="bg-purple-500"
        metricValue={dashboard?.totalUnreadAlerts ?? 0}
        metricColor={(dashboard?.totalUnreadAlerts ?? 0) > 0 ? "text-purple-600" : undefined}
      />
    </div>
  );
}
