"use client";

import { useState } from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";
import { mockNotifications } from "@/lib/mock/notifications";
import { NotificationType } from "@/types/notification";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";
import { ListItemSkeleton } from "@/components/ui/skeleton-blocks";
import {
  ShoppingCart,
  Package,
  Tag,
  Info,
  CheckCircle,
  AlertTriangle,
  Bell,
  CheckCheck,
} from "lucide-react";

const notificationIcons: Record<NotificationType, { icon: React.ElementType; color: string; bg: string }> = {
  VIDEO_ADDED: { icon: Info, color: "text-purple-600 dark:text-purple-400", bg: "bg-purple-100 dark:bg-purple-900/30" },
  ORDER_UPDATE: { icon: ShoppingCart, color: "text-blue-600 dark:text-blue-400", bg: "bg-blue-100 dark:bg-blue-900/30" },
  STOCK_ALERT: { icon: Package, color: "text-orange-600 dark:text-orange-400", bg: "bg-orange-100 dark:bg-orange-900/30" },
  SYSTEM: { icon: Info, color: "text-muted-foreground", bg: "bg-muted" },
  SUCCESS: { icon: CheckCircle, color: "text-green-600 dark:text-green-400", bg: "bg-green-100 dark:bg-green-900/30" },
  WARNING: { icon: AlertTriangle, color: "text-yellow-600 dark:text-yellow-400", bg: "bg-yellow-100 dark:bg-yellow-900/30" },
};

function NotificationsPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Skeleton className="h-8 w-32" />
        <Skeleton className="h-9 w-36 rounded-md" />
      </div>
      <Card>
        <CardContent className="p-0">
          <ListItemSkeleton count={8} />
        </CardContent>
      </Card>
    </div>
  );
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState(mockNotifications);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllAsRead = () => {
    setNotifications((prev) =>
      prev.map((n) => ({ ...n, read: true }))
    );
  };

  const markAsRead = (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  return (
    <div className="p-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          {unreadCount > 0 && (
            <p className="text-sm text-muted-foreground">
              {unreadCount} okunmamış bildirim
            </p>
          )}
        </div>
        {unreadCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={markAllAsRead}
            className="gap-2"
          >
            <CheckCheck className="h-4 w-4" />
            Tümünü Okundu İşaretle
          </Button>
        )}
      </div>

      {/* Notifications List */}
      <div className="space-y-3">
        {notifications.length === 0 ? (
          <div className="text-center py-12">
            <Bell className="h-12 w-12 text-muted-foreground opacity-50 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-foreground">
              Bildirim yok
            </h3>
            <p className="text-sm text-muted-foreground mt-1">
              Yeni bildirimler burada görünecek
            </p>
          </div>
        ) : (
          notifications.map((notification) => {
            const { icon: NotifIcon, color, bg } = notificationIcons[notification.type];
            return (
              <div
                key={notification.id}
                className={cn(
                  "flex items-start gap-4 p-4 rounded-lg border transition-colors",
                  !notification.read
                    ? "bg-blue-50/50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800"
                    : "bg-card border-border hover:bg-muted"
                )}
              >
                <div className={cn("p-2 rounded-lg", bg)}>
                  <NotifIcon className={cn("h-5 w-5", color)} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <h3
                        className={cn(
                          "text-sm",
                          !notification.read ? "font-semibold" : "font-medium"
                        )}
                      >
                        {notification.title}
                      </h3>
                      <p className="text-sm text-muted-foreground mt-1">
                        {notification.message}
                      </p>
                      <p className="text-xs text-muted-foreground mt-2">
                        {new Date(notification.createdAt).toLocaleString("tr-TR")}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      {!notification.read && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => markAsRead(notification.id)}
                          className="text-xs h-7"
                        >
                          Okundu
                        </Button>
                      )}
                      {notification.link && (
                        <Button
                          variant="outline"
                          size="sm"
                          asChild
                          className="text-xs h-7"
                        >
                          <Link href={notification.link}>Görüntüle</Link>
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
