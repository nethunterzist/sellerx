"use client";

import { useAdminDashboardStats } from "@/hooks/queries/use-admin";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FadeIn } from "@/components/motion";
import {
  HeroMetrics,
  UserGrowthChart,
  StoreStatusChart,
  OrderVolumeChart,
  SystemHealthGrid,
  SupportTicketsCard,
} from "@/components/admin/dashboard";

export default function AdminDashboardPage() {
  const { data: stats, isLoading, error, refetch, isFetching } = useAdminDashboardStats();

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <AlertTriangle className="h-12 w-12 text-red-500 mx-auto mb-4" />
          <p className="text-lg text-foreground mb-4">
            Dashboard verileri yüklenemedi
          </p>
          <Button onClick={() => refetch()} variant="outline">
            <RefreshCw className="h-4 w-4 mr-2" />
            Tekrar Dene
          </Button>
        </div>
      </div>
    );
  }

  return (
    <FadeIn>
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Sistem genelinde özet bilgiler ve performans metrikleri
          </p>
        </div>
        <Button
          onClick={() => refetch()}
          variant="outline"
          size="sm"
          disabled={isFetching}
        >
          <RefreshCw className={`h-4 w-4 mr-2 ${isFetching ? "animate-spin" : ""}`} />
          Yenile
        </Button>
      </div>

      {/* Hero Metrics - 4 Large KPI Cards */}
      <HeroMetrics stats={stats} isLoading={isLoading} />

      {/* Charts Row 1 - User Growth & Store Status */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <div className="lg:col-span-3">
          <UserGrowthChart stats={stats} isLoading={isLoading} />
        </div>
        <div className="lg:col-span-2">
          <StoreStatusChart stats={stats} isLoading={isLoading} />
        </div>
      </div>

      {/* Charts Row 2 - Order Volume & Activity Timeline */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <div className="lg:col-span-3">
          <OrderVolumeChart stats={stats} isLoading={isLoading} />
        </div>
        <div className="lg:col-span-2">
          <SupportTicketsCard />
        </div>
      </div>

      {/* System Health Grid - 6 Mini Cards */}
      <SystemHealthGrid stats={stats} isLoading={isLoading} />
    </div>
    </FadeIn>
  );
}
