"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  XCircle,
  Trophy,
  Users,
  AlertTriangle,
  CheckCheck,
  Bell,
} from "lucide-react";
import type { BuyboxAlert, BuyboxAlertType } from "@/types/buybox";
import { useTranslations } from "next-intl";
import { useLocale } from "next-intl";
import { formatDistanceToNow } from "date-fns";
import { tr, enUS } from "date-fns/locale";

interface BuyboxAlertsListProps {
  alerts: BuyboxAlert[] | undefined;
  storeId: string | undefined;
  isLoading: boolean;
  showMarkAllRead?: boolean;
  maxHeight?: string;
  mockMode?: boolean;
  onMarkAsRead?: (alertId: string) => void;
  onMarkAllAsRead?: () => void;
}

const alertTypeIcons: Record<BuyboxAlertType, typeof XCircle> = {
  BUYBOX_LOST: XCircle,
  BUYBOX_WON: Trophy,
  NEW_COMPETITOR: Users,
  PRICE_RISK: AlertTriangle,
};

const alertTypeColors: Record<BuyboxAlertType, string> = {
  BUYBOX_LOST: "bg-red-100 text-red-800",
  BUYBOX_WON: "bg-green-100 text-green-800",
  NEW_COMPETITOR: "bg-orange-100 text-orange-800",
  PRICE_RISK: "bg-yellow-100 text-yellow-800",
};

export function BuyboxAlertsList({
  alerts,
  isLoading,
  showMarkAllRead = true,
  maxHeight = "400px",
  mockMode = false,
  onMarkAsRead,
  onMarkAllAsRead,
}: BuyboxAlertsListProps) {
  const t = useTranslations("Buybox");
  const locale = useLocale();

  const handleMarkAsRead = (alertId: string) => {
    if (mockMode) return;
    onMarkAsRead?.(alertId);
  };

  const handleMarkAllAsRead = () => {
    if (mockMode) return;
    onMarkAllAsRead?.();
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
        {showMarkAllRead && unreadCount > 0 && !mockMode && (
          <Button
            variant="outline"
            size="sm"
            onClick={handleMarkAllAsRead}
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
                const Icon = alertTypeIcons[alert.alertType] || Bell;
                const colorClass = alertTypeColors[alert.alertType] || "bg-gray-100 text-gray-800";

                return (
                  <div
                    key={alert.id}
                    className={`flex gap-3 p-3 rounded-lg transition-colors ${
                      alert.isRead ? "bg-muted/30" : "bg-muted/70 border-l-4 border-primary"
                    }`}
                  >
                    <div className={`p-2 rounded-full shrink-0 ${colorClass}`}>
                      <Icon className="h-5 w-5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="font-medium text-sm line-clamp-1">{alert.title}</p>
                          {alert.productTitle && (
                            <p className="text-xs text-muted-foreground line-clamp-1">
                              {alert.productTitle}
                            </p>
                          )}
                        </div>
                        <Badge
                          variant="outline"
                          className={`text-xs shrink-0 ${colorClass}`}
                        >
                          {t(`alerts.types.${alert.alertType}`)}
                        </Badge>
                      </div>
                      <p className="text-sm mt-1 text-muted-foreground line-clamp-2">
                        {alert.message}
                      </p>
                      <div className="flex items-center justify-between mt-2">
                        <span className="text-xs text-muted-foreground">
                          {formatDistanceToNow(new Date(alert.createdAt), {
                            addSuffix: true,
                            locale: locale === "tr" ? tr : enUS,
                          })}
                        </span>
                        {!alert.isRead && !mockMode && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-6 text-xs"
                            onClick={() => handleMarkAsRead(alert.id)}
                          >
                            {t("alerts.markAsRead")}
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
