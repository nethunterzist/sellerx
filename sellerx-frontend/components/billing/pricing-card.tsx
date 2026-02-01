'use client';

import { Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { PlanWithPrices, BillingCycle } from '@/types/billing';
import { formatCurrency, PLAN_FEATURES, formatBillingCycle } from '@/types/billing';

interface PricingCardProps {
  plan: PlanWithPrices;
  selectedCycle?: BillingCycle;
  isCurrentPlan?: boolean;
  onSelect?: (planCode: string) => void;
  isLoading?: boolean;
}

export function PricingCard({
  plan,
  selectedCycle = 'MONTHLY',
  isCurrentPlan = false,
  onSelect,
  isLoading = false,
}: PricingCardProps) {
  // Guard against undefined plan
  if (!plan) {
    console.error('PricingCard: plan is undefined');
    return null;
  }

  // Handle both array and null/undefined prices
  const prices = plan.prices || [];
  const price = prices.find((p) => p.billingCycle === selectedCycle);

  // Use price field (backend sends 'price', not 'priceAmount')
  const displayPrice = price?.price ?? price?.priceAmount ?? plan.monthlyPrice ?? 0;
  const monthlyEquivalent = price?.monthlyEquivalent ?? displayPrice;

  const isPopular = plan.isPopular || plan.code === 'PRO';
  const isFree = plan.isFree || plan.code === 'FREE';

  // Use featuresMap if available, otherwise try to parse features array
  const featuresMap = plan.featuresMap || {};
  const features = Object.entries(featuresMap).filter(
    ([_, value]) => value === true || (typeof value === 'number' && value !== 0)
  );

  return (
    <Card
      className={`relative flex flex-col ${
        isPopular ? 'border-primary shadow-lg scale-105' : ''
      } ${isCurrentPlan ? 'border-green-500' : ''}`}
    >
      {isPopular && (
        <Badge className="absolute -top-3 left-1/2 -translate-x-1/2 bg-primary">
          En Popüler
        </Badge>
      )}
      {isCurrentPlan && (
        <Badge className="absolute -top-3 left-1/2 -translate-x-1/2 bg-green-500">
          Mevcut Plan
        </Badge>
      )}

      <CardHeader className="text-center pb-2">
        <CardTitle className="text-xl">{plan.name}</CardTitle>
        {plan.description && (
          <CardDescription>{plan.description}</CardDescription>
        )}
      </CardHeader>

      <CardContent className="flex-1">
        <div className="text-center mb-6">
          <div className="flex items-baseline justify-center gap-1">
            <span className="text-4xl font-bold">
              {formatCurrency(displayPrice)}
            </span>
            {!isFree && (
              <span className="text-muted-foreground">
                /{formatBillingCycle(selectedCycle)}
              </span>
            )}
          </div>
          {selectedCycle !== 'MONTHLY' && !isFree && price?.discountPercentage && (
            <div className="mt-1 text-sm text-green-600">
              %{price.discountPercentage} indirimli
            </div>
          )}
          {selectedCycle !== 'MONTHLY' && !isFree && (
            <div className="mt-1 text-sm text-muted-foreground">
              Aylık {formatCurrency(monthlyEquivalent)}
            </div>
          )}
        </div>

        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <Check className="h-4 w-4 text-green-500 flex-shrink-0" />
            <span className="text-sm">
              {plan.maxStores === null
                ? 'Sınırsız mağaza'
                : `${plan.maxStores} mağaza`}
            </span>
          </div>

          {features.map(([key, value]) => {
            const featureInfo = PLAN_FEATURES[key];
            if (!featureInfo) return null;

            return (
              <div key={key} className="flex items-center gap-2">
                <Check className="h-4 w-4 text-green-500 flex-shrink-0" />
                <span className="text-sm">
                  {typeof value === 'number'
                    ? `${value === -1 ? 'Sınırsız' : value} ${featureInfo.label}`
                    : featureInfo.label}
                </span>
              </div>
            );
          })}
        </div>
      </CardContent>

      <CardFooter>
        <Button
          className="w-full"
          variant={isCurrentPlan ? 'outline' : isPopular ? 'default' : 'outline'}
          disabled={isCurrentPlan || isLoading || isFree}
          onClick={() => onSelect?.(plan.code)}
        >
          {isCurrentPlan
            ? 'Mevcut Plan'
            : isFree
            ? 'Ücretsiz'
            : isLoading
            ? 'Yükleniyor...'
            : 'Seç'}
        </Button>
      </CardFooter>
    </Card>
  );
}
