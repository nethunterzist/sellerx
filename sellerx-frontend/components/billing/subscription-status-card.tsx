'use client';

import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import { AlertCircle, CheckCircle, Clock, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import type { Subscription } from '@/types/billing';
import {
  getSubscriptionStatusLabel,
  getSubscriptionStatusColor,
  formatCurrency,
  formatBillingCycle,
} from '@/types/billing';

interface SubscriptionStatusCardProps {
  subscription: Subscription | null | undefined;
  isLoading?: boolean;
  onUpgrade?: () => void;
  onChangePlan?: () => void;
  onCancel?: () => void;
  onReactivate?: () => void;
}

export function SubscriptionStatusCard({
  subscription,
  isLoading,
  onUpgrade,
  onChangePlan,
  onCancel,
  onReactivate,
}: SubscriptionStatusCardProps) {
  // Loading state
  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Abonelik Durumu</CardTitle>
          <CardDescription>Yükleniyor...</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="h-20 animate-pulse rounded-lg bg-muted" />
        </CardContent>
      </Card>
    );
  }

  // No subscription
  if (!subscription) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Abonelik Durumu</CardTitle>
          <CardDescription>Henüz bir aboneliğiniz bulunmuyor</CardDescription>
        </CardHeader>
        <CardContent>
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              Tüm özelliklere erişmek için bir plan seçin.
            </AlertDescription>
          </Alert>
          {onUpgrade && (
            <Button className="mt-4 w-full" onClick={onUpgrade}>
              Plan Seç
            </Button>
          )}
        </CardContent>
      </Card>
    );
  }

  const statusColor = getSubscriptionStatusColor(subscription.status);
  const statusLabel = getSubscriptionStatusLabel(subscription.status);

  const StatusIcon = () => {
    switch (subscription.status) {
      case 'ACTIVE':
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'TRIAL':
        return <Clock className="h-5 w-5 text-blue-500" />;
      case 'PAST_DUE':
      case 'SUSPENDED':
        return <AlertCircle className="h-5 w-5 text-orange-500" />;
      case 'CANCELLED':
      case 'EXPIRED':
        return <XCircle className="h-5 w-5 text-gray-500" />;
      default:
        return <Clock className="h-5 w-5 text-yellow-500" />;
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <StatusIcon />
              {subscription.planName}
            </CardTitle>
            <CardDescription>
              {formatBillingCycle(subscription.billingCycle)} abonelik
            </CardDescription>
          </div>
          <Badge
            variant={
              statusColor === 'green'
                ? 'default'
                : statusColor === 'red' || statusColor === 'orange'
                ? 'destructive'
                : 'secondary'
            }
          >
            {statusLabel}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Trial Info */}
        {subscription.isInTrial && subscription.trialEndDate && (
          <Alert>
            <Clock className="h-4 w-4" />
            <AlertDescription>
              Deneme süreniz{' '}
              <strong>
                {format(new Date(subscription.trialEndDate), 'dd MMMM yyyy', {
                  locale: tr,
                })}
              </strong>{' '}
              tarihinde sona erecek.
            </AlertDescription>
          </Alert>
        )}

        {/* Past Due Warning */}
        {subscription.status === 'PAST_DUE' && subscription.gracePeriodEnd && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              Ödemeniz gecikmiş durumda.{' '}
              <strong>
                {format(new Date(subscription.gracePeriodEnd), 'dd MMMM yyyy', {
                  locale: tr,
                })}
              </strong>{' '}
              tarihine kadar ödeme yapılmazsa aboneliğiniz askıya alınacak.
            </AlertDescription>
          </Alert>
        )}

        {/* Cancellation Notice */}
        {subscription.cancelAtPeriodEnd && subscription.currentPeriodEnd && (
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              Aboneliğiniz{' '}
              <strong>
                {format(new Date(subscription.currentPeriodEnd), 'dd MMMM yyyy', {
                  locale: tr,
                })}
              </strong>{' '}
              tarihinde iptal edilecek.
            </AlertDescription>
          </Alert>
        )}

        {/* Subscription Details */}
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="text-muted-foreground">Mağaza Limiti</p>
            <p className="font-medium">
              {subscription.maxStores === null
                ? 'Sınırsız'
                : subscription.maxStores}
            </p>
          </div>
          {subscription.currentPeriodEnd && (
            <div>
              <p className="text-muted-foreground">Dönem Sonu</p>
              <p className="font-medium">
                {format(new Date(subscription.currentPeriodEnd), 'dd MMM yyyy', {
                  locale: tr,
                })}
              </p>
            </div>
          )}
          <div>
            <p className="text-muted-foreground">Otomatik Yenileme</p>
            <p className="font-medium">
              {subscription.autoRenew ? 'Açık' : 'Kapalı'}
            </p>
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-2 pt-2">
          {subscription.hasAccess && !subscription.cancelAtPeriodEnd && (onUpgrade || onChangePlan) && (
            <Button variant="outline" onClick={onChangePlan || onUpgrade}>
              Plan Değiştir
            </Button>
          )}
          {subscription.hasAccess && !subscription.cancelAtPeriodEnd && onCancel && (
            <Button variant="ghost" onClick={onCancel}>
              İptal Et
            </Button>
          )}
          {subscription.cancelAtPeriodEnd && onReactivate && (
            <Button onClick={onReactivate}>Aboneliği Devam Ettir</Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
