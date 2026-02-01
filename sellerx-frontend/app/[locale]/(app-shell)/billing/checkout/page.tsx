'use client';

import { useState, useMemo, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowLeft, Check, CreditCard, Loader2, Shield } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { PricingCard, AddCardModal, PaymentMethodCard } from '@/components/billing';
import {
  usePlans,
  useSubscription,
  usePaymentMethods,
  useStartTrial,
  useChangePlan,
  useCheckout,
} from '@/hooks/queries/use-billing';
import { formatCurrency, formatBillingCycle, BILLING_CYCLES } from '@/types/billing';
import type { BillingCycle, SubscriptionPlan } from '@/types/billing';

function CheckoutContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // URL params
  const preselectedPlan = searchParams.get('plan');
  const isChangePlan = searchParams.get('change') === 'true';

  // State
  const [step, setStep] = useState<'plan' | 'billing' | 'payment' | 'confirm'>(
    preselectedPlan ? 'billing' : 'plan'
  );
  const [selectedPlanCode, setSelectedPlanCode] = useState<string | null>(
    preselectedPlan
  );
  const [selectedCycle, setSelectedCycle] = useState<BillingCycle>('MONTHLY');
  const [selectedPaymentMethodId, setSelectedPaymentMethodId] = useState<string | null>(null);
  const [showAddCard, setShowAddCard] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Queries
  const { data: plans, isLoading: plansLoading } = usePlans();
  const { data: subscription } = useSubscription();
  const { data: paymentMethods } = usePaymentMethods();

  // Mutations
  const startTrial = useStartTrial();
  const changePlan = useChangePlan();
  const checkout = useCheckout();

  // Get selected plan
  const selectedPlan = useMemo(
    () => plans?.find((p) => p.code === selectedPlanCode),
    [plans, selectedPlanCode]
  );

  // Get price for selected cycle
  const selectedPrice = useMemo(() => {
    if (!selectedPlan) return null;
    return selectedPlan.prices.find((p) => p.billingCycle === selectedCycle);
  }, [selectedPlan, selectedCycle]);

  // Calculate totals
  const subtotal = selectedPrice?.priceAmount || 0;
  const taxRate = 0.20; // KDV %20
  const taxAmount = subtotal * taxRate;
  const total = subtotal + taxAmount;

  // Check if user can start trial
  const canStartTrial = !subscription || subscription.status === 'EXPIRED';

  // Get default payment method
  const defaultPaymentMethod = paymentMethods?.find((m) => m.isDefault);

  // Set default payment method when loaded
  useMemo(() => {
    if (defaultPaymentMethod && !selectedPaymentMethodId) {
      setSelectedPaymentMethodId(defaultPaymentMethod.id);
    }
  }, [defaultPaymentMethod, selectedPaymentMethodId]);

  const handleSelectPlan = (plan: SubscriptionPlan) => {
    if (plan.code === 'FREE') {
      // Free plan doesn't need checkout
      router.push('/billing');
      return;
    }
    setSelectedPlanCode(plan.code);
    setStep('billing');
  };

  const handleSelectCycle = (cycle: BillingCycle) => {
    setSelectedCycle(cycle);
  };

  const handleContinueToPayment = () => {
    if (!selectedPlan || !selectedPrice) return;
    setStep('payment');
  };

  const handleContinueToConfirm = () => {
    if (!selectedPaymentMethodId && !canStartTrial) {
      setError('Lütfen bir ödeme yöntemi seçin veya yeni kart ekleyin.');
      return;
    }
    setStep('confirm');
  };

  const handleCheckout = async () => {
    if (!selectedPlan || !selectedPrice) return;

    setIsProcessing(true);
    setError(null);

    try {
      if (isChangePlan && subscription) {
        // Change existing plan
        await changePlan.mutateAsync({
          planCode: selectedPlan.code,
          billingCycle: selectedCycle,
        });
      } else if (canStartTrial) {
        // Start trial
        await startTrial.mutateAsync({
          planCode: selectedPlan.code,
          billingCycle: selectedCycle,
        });
      } else {
        // Regular checkout
        const result = await checkout.mutateAsync({
          planCode: selectedPlan.code,
          billingCycle: selectedCycle,
          paymentMethodId: selectedPaymentMethodId || undefined,
        });

        // Handle 3DS redirect
        if (result.requires3DS && result.redirectUrl) {
          window.location.href = result.redirectUrl;
          return;
        }
      }

      // Success - redirect to billing page
      router.push('/billing?success=true');
    } catch (err: any) {
      setError(err.message || 'Ödeme işlemi sırasında bir hata oluştu.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleBack = () => {
    switch (step) {
      case 'billing':
        setStep('plan');
        break;
      case 'payment':
        setStep('billing');
        break;
      case 'confirm':
        setStep('payment');
        break;
      default:
        router.push('/billing');
    }
  };

  // Render step content
  const renderStepContent = () => {
    switch (step) {
      case 'plan':
        return (
          <div className="space-y-6">
            <div>
              <h2 className="text-xl font-semibold">Plan Seçin</h2>
              <p className="text-muted-foreground">
                İhtiyaçlarınıza en uygun planı seçin.
              </p>
            </div>

            {plansLoading ? (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {[...Array(4)].map((_, i) => (
                  <div
                    key={i}
                    className="h-96 animate-pulse rounded-lg bg-muted"
                  />
                ))}
              </div>
            ) : (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {plans?.map((plan) => (
                  <PricingCard
                    key={plan.id}
                    plan={plan}
                    isCurrentPlan={plan.code === subscription?.planCode}
                    onSelect={() => handleSelectPlan(plan)}
                  />
                ))}
              </div>
            )}
          </div>
        );

      case 'billing':
        return (
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2 space-y-6">
              <div>
                <h2 className="text-xl font-semibold">Faturalandırma Dönemi</h2>
                <p className="text-muted-foreground">
                  Ne sıklıkta ödeme yapmak istediğinizi seçin.
                </p>
              </div>

              <RadioGroup
                value={selectedCycle}
                onValueChange={(value) => handleSelectCycle(value as BillingCycle)}
                className="grid gap-4"
              >
                {selectedPlan?.prices.map((price) => {
                  const cycleInfo = BILLING_CYCLES[price.billingCycle];
                  const monthlyEquivalent =
                    price.priceAmount / cycleInfo.months;
                  const savings =
                    price.discountPercentage > 0
                      ? `${price.discountPercentage}% tasarruf`
                      : null;

                  return (
                    <Label
                      key={price.id}
                      htmlFor={price.billingCycle}
                      className={`flex items-center justify-between p-4 border rounded-lg cursor-pointer hover:bg-muted/50 transition-colors ${
                        selectedCycle === price.billingCycle
                          ? 'border-primary bg-primary/5'
                          : ''
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <RadioGroupItem
                          value={price.billingCycle}
                          id={price.billingCycle}
                        />
                        <div>
                          <p className="font-medium">
                            {formatBillingCycle(price.billingCycle)}
                          </p>
                          <p className="text-sm text-muted-foreground">
                            {formatCurrency(monthlyEquivalent, price.currency)}
                            /ay
                          </p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="font-semibold">
                          {formatCurrency(price.priceAmount, price.currency)}
                        </p>
                        {savings && (
                          <Badge variant="secondary" className="text-xs">
                            {savings}
                          </Badge>
                        )}
                      </div>
                    </Label>
                  );
                })}
              </RadioGroup>

              <Button onClick={handleContinueToPayment} className="w-full">
                Devam Et
              </Button>
            </div>

            {/* Order Summary */}
            <Card>
              <CardHeader>
                <CardTitle>Sipariş Özeti</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Plan</span>
                  <span className="font-medium">{selectedPlan?.name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Dönem</span>
                  <span>{formatBillingCycle(selectedCycle)}</span>
                </div>
                <Separator />
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Ara Toplam</span>
                  <span>{formatCurrency(subtotal, 'TRY')}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">KDV (%20)</span>
                  <span>{formatCurrency(taxAmount, 'TRY')}</span>
                </div>
                <Separator />
                <div className="flex justify-between text-lg font-semibold">
                  <span>Toplam</span>
                  <span>{formatCurrency(total, 'TRY')}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        );

      case 'payment':
        return (
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2 space-y-6">
              <div>
                <h2 className="text-xl font-semibold">Ödeme Yöntemi</h2>
                <p className="text-muted-foreground">
                  {canStartTrial
                    ? '14 günlük deneme süreniz başlayacak. Deneme sonunda kart bilgilerinizle ödeme alınacaktır.'
                    : 'Ödeme için kullanmak istediğiniz kartı seçin.'}
                </p>
              </div>

              {canStartTrial && (
                <Card className="border-green-200 bg-green-50">
                  <CardContent className="py-4">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-green-100">
                        <Check className="h-5 w-5 text-green-600" />
                      </div>
                      <div>
                        <p className="font-medium text-green-800">
                          14 Gün Ücretsiz Deneme
                        </p>
                        <p className="text-sm text-green-700">
                          Deneme süresi boyunca tüm özelliklere erişebilirsiniz.
                          İstediğiniz zaman iptal edebilirsiniz.
                        </p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )}

              {paymentMethods && paymentMethods.length > 0 ? (
                <RadioGroup
                  value={selectedPaymentMethodId || ''}
                  onValueChange={setSelectedPaymentMethodId}
                  className="space-y-3"
                >
                  {paymentMethods.map((method) => (
                    <Label
                      key={method.id}
                      htmlFor={method.id}
                      className={`block cursor-pointer ${
                        selectedPaymentMethodId === method.id
                          ? 'ring-2 ring-primary rounded-lg'
                          : ''
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <RadioGroupItem value={method.id} id={method.id} />
                        <div className="flex-1">
                          <PaymentMethodCard paymentMethod={method} />
                        </div>
                      </div>
                    </Label>
                  ))}
                </RadioGroup>
              ) : (
                <Card className="border-dashed">
                  <CardContent className="flex flex-col items-center justify-center py-8">
                    <CreditCard className="h-12 w-12 text-muted-foreground mb-4" />
                    <p className="text-muted-foreground mb-4">
                      Henüz kayıtlı kart bulunmuyor.
                    </p>
                  </CardContent>
                </Card>
              )}

              <Button
                variant="outline"
                className="w-full"
                onClick={() => setShowAddCard(true)}
              >
                <CreditCard className="mr-2 h-4 w-4" />
                Yeni Kart Ekle
              </Button>

              <Button
                onClick={handleContinueToConfirm}
                className="w-full"
                disabled={!selectedPaymentMethodId && !canStartTrial}
              >
                Devam Et
              </Button>
            </div>

            {/* Order Summary */}
            <Card>
              <CardHeader>
                <CardTitle>Sipariş Özeti</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Plan</span>
                  <span className="font-medium">{selectedPlan?.name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Dönem</span>
                  <span>{formatBillingCycle(selectedCycle)}</span>
                </div>
                <Separator />
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Ara Toplam</span>
                  <span>{formatCurrency(subtotal, 'TRY')}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">KDV (%20)</span>
                  <span>{formatCurrency(taxAmount, 'TRY')}</span>
                </div>
                <Separator />
                <div className="flex justify-between text-lg font-semibold">
                  <span>Toplam</span>
                  <span>{formatCurrency(total, 'TRY')}</span>
                </div>
                {canStartTrial && (
                  <p className="text-sm text-muted-foreground">
                    * İlk ödeme 14 gün sonra alınacaktır.
                  </p>
                )}
              </CardContent>
            </Card>
          </div>
        );

      case 'confirm':
        return (
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2 space-y-6">
              <div>
                <h2 className="text-xl font-semibold">Siparişi Onayla</h2>
                <p className="text-muted-foreground">
                  Lütfen sipariş bilgilerinizi kontrol edin.
                </p>
              </div>

              {/* Plan Details */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Plan Bilgileri</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Plan</span>
                    <span className="font-medium">{selectedPlan?.name}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">
                      Faturalandırma Dönemi
                    </span>
                    <span>{formatBillingCycle(selectedCycle)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Maksimum Mağaza</span>
                    <span>
                      {selectedPlan?.maxStores === null
                        ? 'Sınırsız'
                        : selectedPlan?.maxStores}
                    </span>
                  </div>
                </CardContent>
              </Card>

              {/* Payment Method */}
              {selectedPaymentMethodId && (
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base">Ödeme Yöntemi</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {paymentMethods?.find(
                      (m) => m.id === selectedPaymentMethodId
                    ) && (
                      <PaymentMethodCard
                        paymentMethod={
                          paymentMethods.find(
                            (m) => m.id === selectedPaymentMethodId
                          )!
                        }
                      />
                    )}
                  </CardContent>
                </Card>
              )}

              {/* Security Notice */}
              <Card className="border-blue-200 bg-blue-50">
                <CardContent className="py-4">
                  <div className="flex items-center gap-3">
                    <Shield className="h-5 w-5 text-blue-600" />
                    <p className="text-sm text-blue-700">
                      Tüm ödemeleriniz 256-bit SSL şifreleme ile korunmaktadır.
                      Ödeme işlemleriniz iyzico güvencesiyle
                      gerçekleştirilmektedir.
                    </p>
                  </div>
                </CardContent>
              </Card>

              {error && (
                <Card className="border-destructive bg-destructive/5">
                  <CardContent className="py-4">
                    <p className="text-sm text-destructive">{error}</p>
                  </CardContent>
                </Card>
              )}

              <Button
                onClick={handleCheckout}
                className="w-full"
                size="lg"
                disabled={isProcessing}
              >
                {isProcessing ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    İşleniyor...
                  </>
                ) : canStartTrial ? (
                  'Denemeyi Başlat'
                ) : (
                  `${formatCurrency(total, 'TRY')} Öde`
                )}
              </Button>
            </div>

            {/* Order Summary */}
            <Card>
              <CardHeader>
                <CardTitle>Sipariş Özeti</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Plan</span>
                  <span className="font-medium">{selectedPlan?.name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Dönem</span>
                  <span>{formatBillingCycle(selectedCycle)}</span>
                </div>
                <Separator />
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Ara Toplam</span>
                  <span>{formatCurrency(subtotal, 'TRY')}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">KDV (%20)</span>
                  <span>{formatCurrency(taxAmount, 'TRY')}</span>
                </div>
                <Separator />
                <div className="flex justify-between text-lg font-semibold">
                  <span>Toplam</span>
                  <span>{formatCurrency(total, 'TRY')}</span>
                </div>
                {canStartTrial && (
                  <p className="text-sm text-muted-foreground">
                    * İlk ödeme 14 gün sonra alınacaktır.
                  </p>
                )}
              </CardContent>
            </Card>
          </div>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={handleBack}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {isChangePlan ? 'Plan Değiştir' : 'Abonelik Satın Al'}
          </h1>
          <p className="text-muted-foreground">
            {step === 'plan' && 'İhtiyaçlarınıza uygun planı seçin.'}
            {step === 'billing' && 'Faturalandırma dönemini belirleyin.'}
            {step === 'payment' && 'Ödeme yönteminizi seçin.'}
            {step === 'confirm' && 'Siparişinizi onaylayın.'}
          </p>
        </div>
      </div>

      {/* Progress Steps */}
      <div className="flex items-center justify-center gap-2">
        {['plan', 'billing', 'payment', 'confirm'].map((s, i) => (
          <div key={s} className="flex items-center">
            <div
              className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium ${
                step === s
                  ? 'bg-primary text-primary-foreground'
                  : ['plan', 'billing', 'payment', 'confirm'].indexOf(step) > i
                  ? 'bg-primary/20 text-primary'
                  : 'bg-muted text-muted-foreground'
              }`}
            >
              {i + 1}
            </div>
            {i < 3 && (
              <div
                className={`h-0.5 w-12 ${
                  ['plan', 'billing', 'payment', 'confirm'].indexOf(step) > i
                    ? 'bg-primary/20'
                    : 'bg-muted'
                }`}
              />
            )}
          </div>
        ))}
      </div>

      {/* Step Content */}
      {renderStepContent()}

      {/* Add Card Modal */}
      <AddCardModal
        open={showAddCard}
        onClose={() => setShowAddCard(false)}
        onSuccess={() => {
          setShowAddCard(false);
          // Payment methods will be refetched automatically
        }}
      />
    </div>
  );
}

export default function CheckoutPage() {
  return (
    <Suspense
      fallback={
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      }
    >
      <CheckoutContent />
    </Suspense>
  );
}
