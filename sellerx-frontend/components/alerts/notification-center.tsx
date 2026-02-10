'use client';

import { useState } from 'react';
import { Bell, Check, CheckCheck, X, Package, DollarSign, Tag, ShoppingCart, Settings } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { useUnreadAlertCount, useUnreadAlerts, useMarkAlertAsRead, useMarkAllAlertsAsRead, useApproveStockAlert, useDismissStockAlert } from '@/hooks/queries/use-alerts';
import type { AlertHistory, AlertType, AlertSeverity } from '@/types/alert';
import { cn } from '@/lib/utils';
import { useTranslations } from 'next-intl';
import Link from 'next/link';

const alertTypeIcons: Record<AlertType, React.ElementType> = {
  STOCK: Package,
  PROFIT: DollarSign,
  PRICE: Tag,
  ORDER: ShoppingCart,
  SYSTEM: Settings,
};

const severityColors: Record<AlertSeverity, string> = {
  LOW: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400',
  MEDIUM: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  HIGH: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
  CRITICAL: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
};

const severityDots: Record<AlertSeverity, string> = {
  LOW: 'bg-gray-400',
  MEDIUM: 'bg-yellow-500',
  HIGH: 'bg-orange-500',
  CRITICAL: 'bg-red-500',
};

function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'Az önce';
  if (diffMins < 60) return `${diffMins} dk önce`;
  if (diffHours < 24) return `${diffHours} saat önce`;
  if (diffDays < 7) return `${diffDays} gün önce`;
  return date.toLocaleDateString('tr-TR');
}

interface NotificationItemProps {
  alert: AlertHistory;
  onMarkAsRead: (id: string) => void;
  onApprove?: (id: string) => void;
  onDismiss?: (id: string) => void;
  isApproving?: boolean;
  isDismissing?: boolean;
}

function NotificationItem({ alert, onMarkAsRead, onApprove, onDismiss, isApproving, isDismissing }: NotificationItemProps) {
  const Icon = alertTypeIcons[alert.alertType];
  const isPendingApproval = alert.status === 'PENDING_APPROVAL';
  const hasCostInfo = alert.data?.hasCostInfo === true;

  return (
    <div
      className={cn(
        'flex items-start gap-3 p-3 border-b border-gray-100 dark:border-gray-800 transition-colors',
        !alert.read && 'bg-blue-50/50 dark:bg-blue-950/20',
        isPendingApproval && 'bg-amber-50/50 dark:bg-amber-950/20 border-l-2 border-l-amber-400'
      )}
    >
      <div className={cn('p-2 rounded-lg', severityColors[alert.severity])}>
        <Icon className="h-4 w-4" />
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span
            className={cn(
              'h-2 w-2 rounded-full flex-shrink-0',
              severityDots[alert.severity]
            )}
          />
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
            {alert.title}
          </p>
        </div>
        {alert.message && (
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">
            {alert.message}
          </p>
        )}

        {/* Pending approval action buttons */}
        {isPendingApproval && (
          <div className="flex items-center gap-2 mt-2">
            {hasCostInfo ? (
              <Button
                variant="default"
                size="sm"
                className="h-7 text-xs bg-green-600 hover:bg-green-700"
                onClick={(e) => {
                  e.stopPropagation();
                  onApprove?.(alert.id);
                }}
                disabled={isApproving || isDismissing}
              >
                <Check className="h-3 w-3 mr-1" />
                Onayla
              </Button>
            ) : (
              <Link
                href="/products"
                className="inline-flex items-center h-7 px-3 text-xs font-medium rounded-md bg-blue-600 text-white hover:bg-blue-700"
                onClick={(e) => e.stopPropagation()}
              >
                Maliyet Gir
              </Link>
            )}
            <Button
              variant="outline"
              size="sm"
              className="h-7 text-xs text-red-600 border-red-200 hover:bg-red-50 dark:text-red-400 dark:border-red-800 dark:hover:bg-red-950"
              onClick={(e) => {
                e.stopPropagation();
                onDismiss?.(alert.id);
              }}
              disabled={isApproving || isDismissing}
            >
              <X className="h-3 w-3 mr-1" />
              Reddet
            </Button>
          </div>
        )}

        {/* Regular stock alert link (non-pending) */}
        {!isPendingApproval && alert.alertType === 'STOCK' && alert.data?.productId && (
          <Link
            href="/products"
            className="text-xs text-blue-600 hover:text-blue-700 dark:text-blue-400 mt-1 inline-block"
            onClick={(e) => e.stopPropagation()}
          >
            Urunleri Incele &rarr;
          </Link>
        )}

        <div className="flex items-center gap-2 mt-1">
          {alert.storeName && (
            <span className="text-xs text-gray-400 dark:text-gray-500">
              {alert.storeName}
            </span>
          )}
          <span className="text-xs text-gray-400 dark:text-gray-500">
            {formatTimeAgo(alert.createdAt)}
          </span>
        </div>
      </div>

      {!alert.read && !isPendingApproval && (
        <Button
          variant="ghost"
          size="sm"
          className="h-8 w-8 p-0 flex-shrink-0"
          onClick={(e) => {
            e.stopPropagation();
            onMarkAsRead(alert.id);
          }}
        >
          <Check className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}

export function NotificationCenter() {
  const [open, setOpen] = useState(false);
  const t = useTranslations('notifications');

  const { data: countData } = useUnreadAlertCount();
  const { data: unreadAlerts, isLoading } = useUnreadAlerts();
  const markAsRead = useMarkAlertAsRead();
  const markAllAsRead = useMarkAllAlertsAsRead();
  const approveAlert = useApproveStockAlert();
  const dismissAlert = useDismissStockAlert();

  const unreadCount = countData?.count || 0;

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

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          className="relative h-9 w-9 p-0"
          aria-label={t('title') || 'Notifications'}
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-red-500 text-white text-xs flex items-center justify-center font-medium">
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
        </Button>
      </PopoverTrigger>

      <PopoverContent
        className="w-96 p-0 mr-2"
        align="end"
        sideOffset={8}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-gray-800">
          <h3 className="font-semibold text-gray-900 dark:text-gray-100">
            {t('title') || 'Bildirimler'}
          </h3>
          <div className="flex items-center gap-2">
            {unreadCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                className="h-8 text-xs"
                onClick={handleMarkAllAsRead}
                disabled={markAllAsRead.isPending}
              >
                <CheckCheck className="h-4 w-4 mr-1" />
                {t('markAllRead') || 'Tümünü Oku'}
              </Button>
            )}
          </div>
        </div>

        {/* Notifications list */}
        <div className="max-h-[400px] overflow-y-auto">
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500" />
            </div>
          ) : unreadAlerts && unreadAlerts.length > 0 ? (
            unreadAlerts.slice(0, 10).map((alert) => (
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
          ) : (
            <div className="flex flex-col items-center justify-center py-8 px-4">
              <Bell className="h-12 w-12 text-gray-300 dark:text-gray-600 mb-3" />
              <p className="text-sm text-gray-500 dark:text-gray-400 text-center">
                {t('noNotifications') || 'Yeni bildirim yok'}
              </p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-4 py-3 border-t border-gray-100 dark:border-gray-800">
          <Link
            href="/notifications"
            className="text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400"
            onClick={() => setOpen(false)}
          >
            {t('viewAll') || 'Tüm Bildirimleri Gör'} &rarr;
          </Link>
        </div>
      </PopoverContent>
    </Popover>
  );
}
