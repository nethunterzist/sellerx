'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Check, HelpCircle, Zap } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { usePlans } from '@/hooks/queries/use-billing';
import { formatCurrency, PLAN_FEATURES } from '@/types/billing';
import type { BillingCycle } from '@/types/billing';
import { FadeIn, StaggerChildren } from '@/components/motion';

const FEATURE_LIST = [
  { code: 'max_stores', label: 'Mağaza Sayısı', type: 'limit' },
  { code: 'advanced_analytics', label: 'Gelişmiş Analitik', type: 'boolean' },
  { code: 'ai_qa_responses', label: 'AI Soru-Cevap', type: 'limit' },
  { code: 'webhook_support', label: 'Webhook Desteği', type: 'boolean' },
  { code: 'api_access', label: 'API Erişimi', type: 'boolean' },
  { code: 'priority_support', label: 'Öncelikli Destek', type: 'boolean' },
  { code: 'parasut_integration', label: 'Paraşüt Entegrasyonu', type: 'boolean' },
];

const FAQ_ITEMS = [
  {
    question: '14 günlük deneme süresi nasıl çalışır?',
    answer:
      'Starter, Pro veya Enterprise planlardan birini seçtiğinizde 14 gün boyunca tüm özelliklere ücretsiz erişebilirsiniz. Deneme süresi boyunca istediğiniz zaman iptal edebilirsiniz. Deneme süresi sonunda otomatik olarak seçtiğiniz plana geçiş yapılır.',
  },
  {
    question: 'Planımı ne zaman değiştirebilirim?',
    answer:
      'Planınızı istediğiniz zaman yükseltebilir veya düşürebilirsiniz. Plan yükseltmelerinde fark fiyatı alınır, düşürmelerde ise mevcut dönem sonuna kadar mevcut planınızı kullanmaya devam edersiniz.',
  },
  {
    question: 'Ödeme yöntemleri nelerdir?',
    answer:
      'Visa, Mastercard, American Express ve Troy kartlarını destekliyoruz. Tüm ödemeleriniz iyzico güvencesiyle gerçekleştirilmektedir.',
  },
  {
    question: 'Fatura kesiliyor mu?',
    answer:
      'Evet, tüm ödemeleriniz için e-fatura kesilmektedir. Faturalarınıza hesap ayarlarınızdan ulaşabilirsiniz.',
  },
  {
    question: 'Aboneliğimi iptal edersem ne olur?',
    answer:
      'Aboneliğinizi iptal ettiğinizde, mevcut faturalandırma döneminin sonuna kadar tüm özelliklere erişmeye devam edersiniz. Dönem sonunda hesabınız ücretsiz plana düşürülür.',
  },
];

export default function PricingPage() {
  const router = useRouter();
  const [billingCycle, setBillingCycle] = useState<BillingCycle>('MONTHLY');
  const { data: plans, isLoading } = usePlans();

  const isAnnual = billingCycle === 'SEMIANNUAL';

  const handleSelectPlan = (planCode: string) => {
    if (planCode === 'FREE') {
      router.push('/register');
    } else {
      router.push(`/register?plan=${planCode}&cycle=${billingCycle}`);
    }
  };

  const getFeatureValue = (planFeatures: Record<string, any>, featureCode: string, type: string) => {
    const value = planFeatures?.[featureCode];

    if (type === 'boolean') {
      return value ? (
        <Check className="h-5 w-5 text-green-500" />
      ) : (
        <span className="text-muted-foreground">-</span>
      );
    }

    if (type === 'limit') {
      if (value === -1 || value === null) {
        return <span className="font-medium">Sınırsız</span>;
      }
      return <span className="font-medium">{value}</span>;
    }

    return value || '-';
  };

  return (
    <TooltipProvider>
      <div className="container mx-auto px-4 py-16">
        {/* Hero Section */}
        <FadeIn direction="up">
          <div className="text-center mb-16">
            <Badge variant="secondary" className="mb-4">
              14 Gün Ücretsiz Deneme
            </Badge>
            <h1 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
              İşletmeniz için doğru planı seçin
            </h1>
            <p className="text-xl text-muted-foreground max-w-2xl mx-auto mb-8">
              Tüm planlar 14 günlük ücretsiz deneme ile başlar. Kredi kartı
              bilgileriniz deneme süresi boyunca ücretlendirilmez.
            </p>

            {/* Billing Toggle */}
            <div className="flex items-center justify-center gap-4">
              <Label
                htmlFor="billing-toggle"
                className={billingCycle === 'MONTHLY' ? 'font-medium' : 'text-muted-foreground'}
              >
                Aylık
              </Label>
              <Switch
                id="billing-toggle"
                checked={isAnnual}
                onCheckedChange={(checked) =>
                  setBillingCycle(checked ? 'SEMIANNUAL' : 'MONTHLY')
                }
              />
              <Label
                htmlFor="billing-toggle"
                className={isAnnual ? 'font-medium' : 'text-muted-foreground'}
              >
                6 Aylık
                <Badge variant="secondary" className="ml-2">
                  %20 İndirim
                </Badge>
              </Label>
            </div>
          </div>
        </FadeIn>

        {/* Pricing Cards */}
        {isLoading ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4 mb-16">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-[500px] animate-pulse rounded-lg bg-muted" />
            ))}
          </div>
        ) : (
          <StaggerChildren staggerDelay={0.1} className="grid gap-6 md:grid-cols-2 lg:grid-cols-4 mb-16">
            {plans?.map((plan) => {
              const price = plan.prices.find((p) => p.billingCycle === billingCycle);
              // Use price.price (backend field) with priceAmount as fallback
              const priceValue = price?.price ?? price?.priceAmount ?? 0;
              const monthlyPrice = price
                ? billingCycle === 'SEMIANNUAL'
                  ? priceValue / 6
                  : priceValue
                : 0;
              const isPopular = plan.code === 'PRO';

              return (
                <Card
                  key={plan.id}
                  className={`relative flex flex-col ${
                    isPopular ? 'border-primary shadow-lg scale-105' : ''
                  }`}
                >
                  {isPopular && (
                    <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                      <Badge className="bg-primary">
                        <Zap className="mr-1 h-3 w-3" />
                        En Popüler
                      </Badge>
                    </div>
                  )}

                  <CardHeader className="text-center pb-4">
                    <CardTitle className="text-xl">{plan.name}</CardTitle>
                    <CardDescription>{plan.description}</CardDescription>
                  </CardHeader>

                  <CardContent className="flex-1 flex flex-col">
                    <div className="text-center mb-6">
                      <div className="flex items-baseline justify-center gap-1">
                        <span className="text-4xl font-bold">
                          {formatCurrency(monthlyPrice, 'TRY')}
                        </span>
                        <span className="text-muted-foreground">/ay</span>
                      </div>
                      {billingCycle === 'SEMIANNUAL' && price && (
                        <p className="text-sm text-muted-foreground mt-1">
                          {formatCurrency(priceValue, 'TRY')} / 6 ay
                        </p>
                      )}
                    </div>

                    <ul className="space-y-3 mb-6 flex-1">
                      <li className="flex items-center gap-2">
                        <Check className="h-4 w-4 text-green-500 shrink-0" />
                        <span className="text-sm">
                          {plan.maxStores === null
                            ? 'Sınırsız mağaza'
                            : `${plan.maxStores} mağaza`}
                        </span>
                      </li>
                      {Object.entries(plan.features || {})
                        .filter(([_, value]) => value === true)
                        .slice(0, 5)
                        .map(([key]) => {
                          const feature = PLAN_FEATURES[key];
                          return (
                            <li key={key} className="flex items-center gap-2">
                              <Check className="h-4 w-4 text-green-500 shrink-0" />
                              <span className="text-sm">
                                {feature?.label || key}
                              </span>
                            </li>
                          );
                        })}
                    </ul>

                    <Button
                      className="w-full"
                      variant={isPopular ? 'default' : 'outline'}
                      onClick={() => handleSelectPlan(plan.code)}
                    >
                      {plan.code === 'FREE' ? 'Ücretsiz Başla' : 'Denemeyi Başlat'}
                    </Button>
                  </CardContent>
                </Card>
              );
            })}
          </StaggerChildren>
        )}

        {/* Feature Comparison Table */}
        <div className="mb-16">
          <h2 className="text-2xl font-bold text-center mb-8">
            Plan Karşılaştırması
          </h2>
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left p-4 font-medium">Özellik</th>
                    {plans?.map((plan) => (
                      <th key={plan.id} className="text-center p-4 font-medium">
                        {plan.name}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {FEATURE_LIST.map((feature) => (
                    <tr key={feature.code} className="border-b last:border-0">
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <span>{feature.label}</span>
                          {PLAN_FEATURES[feature.code]?.description && (
                            <Tooltip>
                              <TooltipTrigger>
                                <HelpCircle className="h-4 w-4 text-muted-foreground" />
                              </TooltipTrigger>
                              <TooltipContent>
                                <p className="max-w-xs">
                                  {PLAN_FEATURES[feature.code].description}
                                </p>
                              </TooltipContent>
                            </Tooltip>
                          )}
                        </div>
                      </td>
                      {plans?.map((plan) => (
                        <td key={plan.id} className="text-center p-4">
                          {getFeatureValue(plan.features, feature.code, feature.type)}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        </div>

        {/* FAQ Section */}
        <div className="max-w-3xl mx-auto">
          <h2 className="text-2xl font-bold text-center mb-8">
            Sıkça Sorulan Sorular
          </h2>
          <Accordion type="single" collapsible className="w-full">
            {FAQ_ITEMS.map((item, index) => (
              <AccordionItem key={index} value={`item-${index}`}>
                <AccordionTrigger className="text-left">
                  {item.question}
                </AccordionTrigger>
                <AccordionContent className="text-muted-foreground">
                  {item.answer}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </div>

        {/* CTA Section */}
        <div className="text-center mt-16 py-16 px-8 rounded-2xl bg-gradient-to-r from-primary/10 to-primary/5">
          <h2 className="text-3xl font-bold mb-4">Hemen başlamaya hazır mısınız?</h2>
          <p className="text-muted-foreground mb-8 max-w-xl mx-auto">
            14 gün boyunca tüm Pro özellikleri ücretsiz deneyin. Kredi kartı
            bilgileriniz deneme süresi boyunca ücretlendirilmez.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Button size="lg" onClick={() => router.push('/register?plan=PRO')}>
              Ücretsiz Denemeyi Başlat
            </Button>
            <Button size="lg" variant="outline" onClick={() => router.push('/sign-in')}>
              Giriş Yap
            </Button>
          </div>
        </div>
      </div>
    </TooltipProvider>
  );
}
