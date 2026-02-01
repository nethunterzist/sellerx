'use client';

import { ReactNode } from 'react';
import { Lock } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useSubscription, useFeatures } from '@/hooks/queries/use-billing';
import { PLAN_FEATURES } from '@/types/billing';

interface FeatureGateProps {
  feature: string;
  children: ReactNode;
  fallback?: ReactNode;
  onUpgrade?: () => void;
}

/**
 * Component that gates features based on subscription plan
 */
export function FeatureGate({
  feature,
  children,
  fallback,
  onUpgrade,
}: FeatureGateProps) {
  const { isLoading: subscriptionLoading } = useSubscription();
  const { data: features, isLoading: featuresLoading } = useFeatures();

  if (subscriptionLoading || featuresLoading) {
    return <div className="animate-pulse bg-muted h-32 rounded-lg" />;
  }

  // Check if feature is available from features API
  const featureData = features?.find(f => f.featureCode === feature);
  const hasFeature = featureData?.enabled ?? false;

  if (hasFeature) {
    return <>{children}</>;
  }

  // Show fallback or default upgrade prompt
  if (fallback) {
    return <>{fallback}</>;
  }

  const featureInfo = PLAN_FEATURES[feature];

  return (
    <Card className="border-dashed">
      <CardHeader className="text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-muted">
          <Lock className="h-6 w-6 text-muted-foreground" />
        </div>
        <CardTitle className="text-lg">
          {featureInfo?.label || 'Premium Özellik'}
        </CardTitle>
        <CardDescription>
          {featureInfo?.description ||
            'Bu özelliğe erişmek için planınızı yükseltmeniz gerekiyor.'}
        </CardDescription>
      </CardHeader>
      <CardContent className="text-center">
        {onUpgrade && (
          <Button onClick={onUpgrade}>Planı Yükselt</Button>
        )}
      </CardContent>
    </Card>
  );
}

interface FeatureLimitGateProps {
  feature: string;
  currentUsage: number;
  children: ReactNode;
  onLimitReached?: () => void;
}

/**
 * Component that gates features based on usage limits
 */
export function FeatureLimitGate({
  feature,
  currentUsage,
  children,
  onLimitReached,
}: FeatureLimitGateProps) {
  const { data: features, isLoading } = useFeatures();

  if (isLoading) {
    return <div className="animate-pulse bg-muted h-32 rounded-lg" />;
  }

  // Get feature limit from features API
  const featureData = features?.find(f => f.featureCode === feature);
  const limit =
    featureData?.featureType === 'UNLIMITED'
      ? Infinity
      : featureData?.featureType === 'BOOLEAN' && featureData?.enabled
      ? Infinity
      : featureData?.limit ?? 0;

  if (currentUsage < limit) {
    return <>{children}</>;
  }

  const featureInfo = PLAN_FEATURES[feature];

  return (
    <Card className="border-dashed border-orange-300 bg-orange-50">
      <CardContent className="py-6 text-center">
        <p className="text-sm text-orange-700">
          {featureInfo?.label || 'Özellik'} limitinize ulaştınız ({currentUsage}/{limit === Infinity ? '∞' : limit})
        </p>
        {onLimitReached && (
          <Button
            variant="outline"
            size="sm"
            className="mt-2"
            onClick={onLimitReached}
          >
            Limiti Artır
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

interface StoreGateProps {
  currentStoreCount: number;
  children: ReactNode;
  onUpgrade?: () => void;
}

/**
 * Component that gates store creation based on plan limits
 */
export function StoreGate({
  currentStoreCount,
  children,
  onUpgrade,
}: StoreGateProps) {
  const { data: subscription, isLoading } = useSubscription();

  if (isLoading) {
    return <div className="animate-pulse bg-muted h-32 rounded-lg" />;
  }

  // Use flat maxStores from subscription (not nested plan.maxStores)
  const maxStores = subscription?.maxStores ?? 1;
  const canAddStore = maxStores === null || currentStoreCount < maxStores;

  if (canAddStore) {
    return <>{children}</>;
  }

  return (
    <Card className="border-dashed">
      <CardHeader className="text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-muted">
          <Lock className="h-6 w-6 text-muted-foreground" />
        </div>
        <CardTitle className="text-lg">Mağaza Limitine Ulaşıldı</CardTitle>
        <CardDescription>
          Mevcut planınızda {maxStores === null ? 'sınırsız' : maxStores} mağaza yönetebilirsiniz.
          Daha fazla mağaza eklemek için planınızı yükseltin.
        </CardDescription>
      </CardHeader>
      <CardContent className="text-center">
        {onUpgrade && (
          <Button onClick={onUpgrade}>Planı Yükselt</Button>
        )}
      </CardContent>
    </Card>
  );
}
