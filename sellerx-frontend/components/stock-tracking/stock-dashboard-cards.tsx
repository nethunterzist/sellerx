"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Package, PackageX, AlertTriangle, PackageCheck, Bell } from "lucide-react";
import type { StockTrackingDashboard } from "@/types/stock-tracking";
import { useTranslations } from "next-intl";

interface StockDashboardCardsProps {
  dashboard: StockTrackingDashboard | undefined;
  isLoading: boolean;
}

export function StockDashboardCards({ dashboard, isLoading }: StockDashboardCardsProps) {
  const t = useTranslations("stockTracking");

  const cards = [
    {
      title: t("dashboard.trackedProducts"),
      value: dashboard?.activeTrackedProducts ?? 0,
      total: dashboard?.totalTrackedProducts ?? 0,
      icon: Package,
      color: "text-blue-600",
      bgColor: "bg-blue-100",
    },
    {
      title: t("dashboard.outOfStock"),
      value: dashboard?.outOfStockProducts ?? 0,
      icon: PackageX,
      color: "text-red-600",
      bgColor: "bg-red-100",
      alert: true,
    },
    {
      title: t("dashboard.lowStock"),
      value: dashboard?.lowStockProducts ?? 0,
      icon: AlertTriangle,
      color: "text-orange-600",
      bgColor: "bg-orange-100",
    },
    {
      title: t("dashboard.unreadAlerts"),
      value: dashboard?.totalUnreadAlerts ?? 0,
      icon: Bell,
      color: "text-purple-600",
      bgColor: "bg-purple-100",
    },
  ];

  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <Card key={i} className="animate-pulse">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <div className="h-4 w-24 bg-gray-200 rounded" />
              <div className="h-8 w-8 bg-gray-200 rounded" />
            </CardHeader>
            <CardContent>
              <div className="h-8 w-16 bg-gray-200 rounded" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      {cards.map((card, index) => (
        <Card key={index} className={card.alert && card.value > 0 ? "border-red-200" : ""}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {card.title}
            </CardTitle>
            <div className={`p-2 rounded-lg ${card.bgColor}`}>
              <card.icon className={`h-4 w-4 ${card.color}`} />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {card.value}
              {card.total !== undefined && (
                <span className="text-sm font-normal text-muted-foreground">
                  {" "}/ {card.total}
                </span>
              )}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
