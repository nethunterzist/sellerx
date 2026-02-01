"use client";

import {
  Clock,
  RefreshCw,
  CheckCircle,
  XCircle,
  CreditCard,
  UserPlus,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { StatCardMini } from "./stat-card-mini";
import type { AdminDashboardStats } from "@/types/admin";

interface SystemHealthGridProps {
  stats: AdminDashboardStats | undefined;
  isLoading: boolean;
}

export function SystemHealthGrid({ stats, isLoading }: SystemHealthGridProps) {
  if (isLoading) {
    return (
      <div className="space-y-4">
        <h3 className="font-semibold text-foreground">Sistem Durumu</h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-[76px] rounded-lg" />
          ))}
        </div>
      </div>
    );
  }

  if (!stats) {
    return null;
  }

  const healthMetrics = [
    {
      label: "Bekleyen Sync",
      value: stats.pendingSyncs,
      icon: Clock,
      color: stats.pendingSyncs > 0 ? "yellow" : "gray",
      alert: stats.pendingSyncs > 5,
    },
    {
      label: "Aktif Sync",
      value: stats.activeSyncs,
      icon: RefreshCw,
      color: stats.activeSyncs > 0 ? "blue" : "gray",
      alert: false,
    },
    {
      label: "Bugün Tamamlanan",
      value: stats.completedSyncsToday,
      icon: CheckCircle,
      color: "green",
      alert: false,
    },
    {
      label: "Bugün Başarısız",
      value: stats.failedSyncsToday,
      icon: XCircle,
      color: stats.failedSyncsToday > 0 ? "red" : "gray",
      alert: stats.failedSyncsToday > 0,
    },
    {
      label: "Aktif Abonelik",
      value: stats.activeSubscriptions,
      icon: CreditCard,
      color: "green",
      alert: false,
    },
    {
      label: "Deneme Kullanıcı",
      value: stats.trialUsers,
      icon: UserPlus,
      color: "blue",
      alert: false,
    },
  ] as const;

  return (
    <div className="space-y-4">
      <h3 className="font-semibold text-foreground">Sistem Durumu</h3>
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        {healthMetrics.map((metric) => (
          <StatCardMini
            key={metric.label}
            label={metric.label}
            value={metric.value}
            icon={metric.icon}
            color={metric.color}
            alert={metric.alert}
          />
        ))}
      </div>
    </div>
  );
}
