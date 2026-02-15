"use client";

import { useState } from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";
import {
  useAlerts,
  useMarkAlertAsRead,
  useMarkAllAlertsAsRead,
  useApproveStockAlert,
  useDismissStockAlert,
} from "@/hooks/queries/use-alerts";
import type { AlertHistory, AlertType, AlertSeverity } from "@/types/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ShoppingCart,
  Package,
  Tag,
  DollarSign,
  Settings,
  Bell,
  CheckCheck,
  Check,
  X,
  ChevronLeft,
  ChevronRight,
  Filter,
  Undo2,
} from "lucide-react";

const alertTypeIcons: Record<AlertType, { icon: React.ElementType; color: string; bg: string }> = {
  STOCK: { icon: Package, color: "text-orange-600 dark:text-orange-400", bg: "bg-orange-100 dark:bg-orange-900/30" },
  PROFIT: { icon: DollarSign, color: "text-green-600 dark:text-green-400", bg: "bg-green-100 dark:bg-green-900/30" },
  PRICE: { icon: Tag, color: "text-blue-600 dark:text-blue-400", bg: "bg-blue-100 dark:bg-blue-900/30" },
  ORDER: { icon: ShoppingCart, color: "text-purple-600 dark:text-purple-400", bg: "bg-purple-100 dark:bg-purple-900/30" },
  RETURN: { icon: Undo2, color: "text-red-600 dark:text-red-400", bg: "bg-red-100 dark:bg-red-900/30" },
  SYSTEM: { icon: Settings, color: "text-gray-600 dark:text-gray-400", bg: "bg-gray-100 dark:bg-gray-800" },
};

const severityColors: Record<AlertSeverity, string> = {
  LOW: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300",
  MEDIUM: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
  HIGH: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
  CRITICAL: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
};

const alertTypeLabels: Record<AlertType, string> = {
  STOCK: "Stok",
  PROFIT: "Kar",
  PRICE: "Fiyat",
  ORDER: "Siparis",
  RETURN: "Iade",
  SYSTEM: "Sistem",
};

const severityLabels: Record<AlertSeverity, string> = {
  LOW: "Dusuk",
  MEDIUM: "Orta",
  HIGH: "Yuksek",
  CRITICAL: "Kritik",
};

function NotificationsPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Skeleton className="h-8 w-32" />
        <Skeleton className="h-9 w-36 rounded-md" />
      </div>
      <div className="flex gap-3">
        <Skeleton className="h-9 w-32" />
        <Skeleton className="h-9 w-32" />
      </div>
      <div className="space-y-3">
        {[...Array(5)].map((_, i) => (
          <Skeleton key={i} className="h-24 w-full rounded-lg" />
        ))}
      </div>
    </div>
  );
}

interface NotificationItemProps {
  alert: AlertHistory;
  onMarkAsRead: (id: string) => void;
  onApprove?: (id: string) => void;
  onDismiss?: (id: string) => void;
  isApproving?: boolean;
  isDismissing?: boolean;
}

function NotificationItem({
  alert,
  onMarkAsRead,
  onApprove,
  onDismiss,
  isApproving,
  isDismissing,
}: NotificationItemProps) {
  const { icon: AlertIcon, color, bg } = alertTypeIcons[alert.alertType];
  const isPendingApproval = alert.status === "PENDING_APPROVAL";
  const hasCostInfo = alert.data?.hasCostInfo === true;

  return (
    <div
      className={cn(
        "flex items-start gap-4 p-4 rounded-lg border transition-colors",
        !alert.read
          ? "bg-blue-50/50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800"
          : "bg-card border-border hover:bg-muted/50",
        isPendingApproval && "border-l-4 border-l-amber-400"
      )}
    >
      <div className={cn("p-2 rounded-lg shrink-0", bg)}>
        <AlertIcon className={cn("h-5 w-5", color)} />
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <h3
                className={cn(
                  "text-sm truncate",
                  !alert.read ? "font-semibold" : "font-medium"
                )}
              >
                {alert.title}
              </h3>
              <Badge variant="secondary" className={cn("text-xs", severityColors[alert.severity])}>
                {severityLabels[alert.severity]}
              </Badge>
              {!alert.read && (
                <span className="h-2 w-2 rounded-full bg-blue-500 shrink-0" />
              )}
            </div>

            {alert.message && (
              <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
                {alert.message}
              </p>
            )}

            <div className="flex items-center gap-2 mt-2 text-xs text-muted-foreground">
              {alert.storeName && (
                <>
                  <span>{alert.storeName}</span>
                  <span>•</span>
                </>
              )}
              <span>{new Date(alert.createdAt).toLocaleString("tr-TR")}</span>
            </div>

            {/* Pending approval actions */}
            {isPendingApproval && (
              <div className="flex items-center gap-2 mt-3">
                {hasCostInfo ? (
                  <Button
                    variant="default"
                    size="sm"
                    className="h-7 text-xs bg-green-600 hover:bg-green-700"
                    onClick={() => onApprove?.(alert.id)}
                    disabled={isApproving || isDismissing}
                  >
                    <Check className="h-3 w-3 mr-1" />
                    Onayla
                  </Button>
                ) : (
                  <Button
                    variant="default"
                    size="sm"
                    className="h-7 text-xs"
                    asChild
                  >
                    <Link href="/products">Maliyet Gir</Link>
                  </Button>
                )}
                <Button
                  variant="outline"
                  size="sm"
                  className="h-7 text-xs text-red-600 border-red-200 hover:bg-red-50 dark:text-red-400 dark:border-red-800 dark:hover:bg-red-950"
                  onClick={() => onDismiss?.(alert.id)}
                  disabled={isApproving || isDismissing}
                >
                  <X className="h-3 w-3 mr-1" />
                  Reddet
                </Button>
              </div>
            )}
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {!alert.read && !isPendingApproval && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => onMarkAsRead(alert.id)}
                className="text-xs h-7"
              >
                <Check className="h-3 w-3 mr-1" />
                Okundu
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState<AlertType | "all">("all");
  const [severityFilter, setSeverityFilter] = useState<AlertSeverity | "all">("all");

  const { data, isLoading, error } = useAlerts({
    page,
    size: 20,
    type: typeFilter === "all" ? undefined : typeFilter,
    severity: severityFilter === "all" ? undefined : severityFilter,
  });

  const markAsRead = useMarkAlertAsRead();
  const markAllAsRead = useMarkAllAlertsAsRead();
  const approveAlert = useApproveStockAlert();
  const dismissAlert = useDismissStockAlert();

  const handleMarkAsRead = (id: string) => {
    markAsRead.mutate(id);
  };

  const handleMarkAllAsRead = () => {
    markAllAsRead.mutate();
  };

  const handleApprove = (id: string) => {
    approveAlert.mutate(id);
  };

  const handleDismiss = (id: string) => {
    dismissAlert.mutate(id);
  };

  const clearFilters = () => {
    setTypeFilter("all");
    setSeverityFilter("all");
    setPage(0);
  };

  const hasActiveFilters = typeFilter !== "all" || severityFilter !== "all";

  if (isLoading) {
    return (
      <div className="p-6 max-w-4xl mx-auto">
        <NotificationsPageSkeleton />
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 max-w-4xl mx-auto">
        <div className="text-center py-12">
          <Bell className="h-12 w-12 text-red-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-foreground">Bir hata olustu</h3>
          <p className="text-sm text-muted-foreground mt-1">
            Bildirimler yuklenirken bir sorun olustu
          </p>
        </div>
      </div>
    );
  }

  const alerts = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;
  const currentPage = data?.number || 0;
  const isFirstPage = currentPage === 0;
  const isLastPage = currentPage >= totalPages - 1;

  const unreadCount = alerts.filter((a) => !a.read).length;

  return (
    <div className="p-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Bildirimler</h1>
          {totalElements > 0 && (
            <p className="text-sm text-muted-foreground mt-1">
              {totalElements} bildirim{unreadCount > 0 && `, ${unreadCount} okunmamis`}
            </p>
          )}
        </div>
        {unreadCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={handleMarkAllAsRead}
            disabled={markAllAsRead.isPending}
            className="gap-2"
          >
            <CheckCheck className="h-4 w-4" />
            Tumunu Okundu Isaretle
          </Button>
        )}
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Filter className="h-4 w-4" />
          <span>Filtreler:</span>
        </div>

        <Select
          value={typeFilter}
          onValueChange={(value) => {
            setTypeFilter(value as AlertType | "all");
            setPage(0);
          }}
        >
          <SelectTrigger className="w-[140px] h-9">
            <SelectValue placeholder="Tur" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Tum Turler</SelectItem>
            <SelectItem value="STOCK">Stok</SelectItem>
            <SelectItem value="PROFIT">Kar</SelectItem>
            <SelectItem value="PRICE">Fiyat</SelectItem>
            <SelectItem value="ORDER">Siparis</SelectItem>
            <SelectItem value="SYSTEM">Sistem</SelectItem>
          </SelectContent>
        </Select>

        <Select
          value={severityFilter}
          onValueChange={(value) => {
            setSeverityFilter(value as AlertSeverity | "all");
            setPage(0);
          }}
        >
          <SelectTrigger className="w-[140px] h-9">
            <SelectValue placeholder="Onem" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Tum Onemler</SelectItem>
            <SelectItem value="LOW">Dusuk</SelectItem>
            <SelectItem value="MEDIUM">Orta</SelectItem>
            <SelectItem value="HIGH">Yuksek</SelectItem>
            <SelectItem value="CRITICAL">Kritik</SelectItem>
          </SelectContent>
        </Select>

        {hasActiveFilters && (
          <Button
            variant="ghost"
            size="sm"
            onClick={clearFilters}
            className="h-9 px-3 text-muted-foreground hover:text-foreground"
          >
            <X className="h-4 w-4 mr-1" />
            Temizle
          </Button>
        )}
      </div>

      {/* Notifications List */}
      <div className="space-y-3">
        {alerts.length === 0 ? (
          <div className="text-center py-12">
            <Bell className="h-12 w-12 text-muted-foreground opacity-50 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-foreground">Bildirim yok</h3>
            <p className="text-sm text-muted-foreground mt-1">
              {hasActiveFilters
                ? "Bu filtrelere uygun bildirim bulunamadi"
                : "Yeni bildirimler burada gorunecek"}
            </p>
            {hasActiveFilters && (
              <Button
                variant="outline"
                size="sm"
                onClick={clearFilters}
                className="mt-4"
              >
                Filtreleri Temizle
              </Button>
            )}
          </div>
        ) : (
          alerts.map((alert) => (
            <NotificationItem
              key={alert.id}
              alert={alert}
              onMarkAsRead={handleMarkAsRead}
              onApprove={handleApprove}
              onDismiss={handleDismiss}
              isApproving={approveAlert.isPending}
              isDismissing={dismissAlert.isPending}
            />
          ))
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-6 pt-4 border-t border-border">
          <p className="text-sm text-muted-foreground">
            Sayfa {currentPage + 1} / {totalPages} ({totalElements} bildirim)
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={isFirstPage}
            >
              <ChevronLeft className="h-4 w-4 mr-1" />
              Onceki
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => p + 1)}
              disabled={isLastPage}
            >
              Sonraki
              <ChevronRight className="h-4 w-4 ml-1" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
