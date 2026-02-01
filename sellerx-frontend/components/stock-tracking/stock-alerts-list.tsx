"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  PackageX,
  AlertTriangle,
  PackageCheck,
  TrendingUp,
  CheckCheck,
  Bell,
} from "lucide-react";
import type { StockAlert, StockAlertType, StockAlertSeverity } from "@/types/stock-tracking";
import { useMarkAlertAsRead, useMarkAllAlertsAsRead } from "@/hooks/queries/use-stock-tracking";
import { useTranslations } from "next-intl";
import { useLocale } from "next-intl";
import { toast } from "sonner";
import { formatDistanceToNow } from "date-fns";
import { tr, enUS } from "date-fns/locale";

interface StockAlertsListProps {
  alerts: StockAlert[] | undefined;
  storeId: string | undefined;
  isLoading: boolean;
  showMarkAllRead?: boolean;
  maxHeight?: string;
}

const alertTypeIcons: Record<StockAlertType, typeof PackageX> = {
  OUT_OF_STOCK: PackageX,
  LOW_STOCK: AlertTriangle,
  BACK_IN_STOCK: PackageCheck,
  STOCK_INCREASED: TrendingUp,
};

const severityColors: Record<StockAlertSeverity, string> = {
  LOW: "bg-blue-100 text-blue-800",
  MEDIUM: "bg-yellow-100 text-yellow-800",
  HIGH: "bg-orange-100 text-orange-800",
  CRITICAL: "bg-red-100 text-red-800",
};

export function StockAlertsList({
  alerts,
  storeId,
  isLoading,
  showMarkAllRead = true,
  maxHeight = "400px",
}: StockAlertsListProps) {
  const t = useTranslations("stockTracking");
  const locale = useLocale();

  const markAsRead = useMarkAlertAsRead(storeId);
  const markAllAsRead = useMarkAllAlertsAsRead(storeId);

  const handleMarkAsRead = async (alertId: string) => {
    try {
      await markAsRead.mutateAsync(alertId);
    } catch (error) {
      toast.error(t("alerts.markReadError"));
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await markAllAsRead.mutateAsync();
      toast.success(t("alerts.markAllReadSuccess"));
    } catch (error) {
      toast.error(t("alerts.markAllReadError"));
    }
  };

  const unreadCount = alerts?.filter((a) => !a.isRead).length ?? 0;

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bell className="h-5 w-5" />
            {t("alerts.title")}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="animate-pulse flex gap-3 p-3 rounded-lg bg-muted/50">
                <div className="h-10 w-10 bg-gray-200 rounded-full" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-3/4 bg-gray-200 rounded" />
                  <div className="h-3 w-1/2 bg-gray-200 rounded" />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="flex items-center gap-2">
          <Bell className="h-5 w-5" />
          {t("alerts.title")}
          {unreadCount > 0 && (
            <Badge variant="secondary">{unreadCount}</Badge>
          )}
        </CardTitle>
        {showMarkAllRead && unreadCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={handleMarkAllAsRead}
            disabled={markAllAsRead.isPending}
          >
            <CheckCheck className="mr-2 h-4 w-4" />
            {t("alerts.markAllRead")}
          </Button>
        )}
      </CardHeader>
      <CardContent>
        {!alerts || alerts.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            <Bell className="h-12 w-12 mx-auto mb-2 opacity-50" />
            <p>{t("alerts.empty")}</p>
          </div>
        ) : (
          <ScrollArea style={{ maxHeight }}>
            <div className="space-y-3">
              {alerts.map((alert) => {
                const Icon = alertTypeIcons[alert.alertType as StockAlertType] || Bell;
                return (
                  <div
                    key={alert.id}
                    className={`flex gap-3 p-3 rounded-lg transition-colors ${
                      alert.isRead ? "bg-muted/30" : "bg-muted/70 border-l-4 border-primary"
                    }`}
                  >
                    <div className={`p-2 rounded-full ${severityColors[alert.severity as StockAlertSeverity]}`}>
                      <Icon className="h-5 w-5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="font-medium text-sm line-clamp-1">{alert.title}</p>
                          <p className="text-xs text-muted-foreground line-clamp-1">
                            {alert.productName}
                          </p>
                        </div>
                        <Badge
                          variant="outline"
                          className={`text-xs ${severityColors[alert.severity as StockAlertSeverity]}`}
                        >
                          {alert.severity}
                        </Badge>
                      </div>
                      <p className="text-sm mt-1 text-muted-foreground">{alert.message}</p>
                      <div className="flex items-center justify-between mt-2">
                        <span className="text-xs text-muted-foreground">
                          {formatDistanceToNow(new Date(alert.createdAt), {
                            addSuffix: true,
                            locale: locale === "tr" ? tr : enUS,
                          })}
                        </span>
                        {!alert.isRead && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-6 text-xs"
                            onClick={() => handleMarkAsRead(alert.id)}
                            disabled={markAsRead.isPending}
                          >
                            {t("alerts.markRead")}
                          </Button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </ScrollArea>
        )}
      </CardContent>
    </Card>
  );
}
