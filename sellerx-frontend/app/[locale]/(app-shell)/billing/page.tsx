'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { CreditCard, Plus, Receipt, Settings } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  SubscriptionStatusCard,
  PaymentMethodCard,
  AddCardModal,
  InvoiceList,
  PricingCard,
} from '@/components/billing';
import {
  useSubscription,
  usePaymentMethods,
  useDeletePaymentMethod,
  useSetDefaultPaymentMethod,
  useInvoices,
  usePlans,
  useCancelSubscription,
  useReactivateSubscription,
} from '@/hooks/queries/use-billing';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';

export default function BillingPage() {
  const router = useRouter();
  const [showAddCard, setShowAddCard] = useState(false);
  const [deleteCardId, setDeleteCardId] = useState<string | null>(null);
  const [showCancelDialog, setShowCancelDialog] = useState(false);

  // Queries
  const { data: subscription, isLoading: subscriptionLoading } = useSubscription();
  const { data: paymentMethods, isLoading: paymentMethodsLoading } = usePaymentMethods();
  const { data: invoices, isLoading: invoicesLoading } = useInvoices();
  const { data: plans, isLoading: plansLoading } = usePlans();

  // Mutations
  const deletePaymentMethod = useDeletePaymentMethod();
  const setDefaultPaymentMethod = useSetDefaultPaymentMethod();
  const cancelSubscription = useCancelSubscription();
  const reactivateSubscription = useReactivateSubscription();

  const handleUpgrade = () => {
    router.push('/billing/checkout');
  };

  const handleChangePlan = () => {
    router.push('/billing/checkout?change=true');
  };

  const handleDeleteCard = async () => {
    if (!deleteCardId) return;
    try {
      await deletePaymentMethod.mutateAsync(deleteCardId);
      setDeleteCardId(null);
    } catch (error) {
      console.error('Kart silme hatası:', error);
    }
  };

  const handleSetDefaultCard = async (id: string) => {
    try {
      await setDefaultPaymentMethod.mutateAsync(id);
    } catch (error) {
      console.error('Varsayılan kart ayarlama hatası:', error);
    }
  };

  const handleCancelSubscription = async () => {
    try {
      await cancelSubscription.mutateAsync({ reason: "Kullanıcı tarafından iptal edildi", immediate: false });
      setShowCancelDialog(false);
    } catch (error) {
      console.error('Abonelik iptal hatası:', error);
    }
  };

  const handleReactivate = async () => {
    try {
      await reactivateSubscription.mutateAsync();
    } catch (error) {
      console.error('Abonelik yeniden aktifleştirme hatası:', error);
    }
  };

  const handleDownloadInvoice = (invoiceId: string) => {
    // TODO: Implement PDF download
    window.open(`/api/billing/invoices/${invoiceId}/pdf`, '_blank');
  };

  // Get current plan info
  const currentPlanCode = subscription?.planCode;
  const currentPlan = plans?.find((p) => p.code === currentPlanCode);
  const isFreePlan = currentPlanCode === 'FREE' || !subscription;
  const isCancelled = subscription?.status === 'CANCELLED';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Abonelik ve Faturalandırma</h1>
        <p className="text-muted-foreground">
          Abonelik planınızı, ödeme yöntemlerinizi ve faturalarınızı yönetin.
        </p>
      </div>

      <Tabs defaultValue="subscription" className="space-y-6">
        <TabsList>
          <TabsTrigger value="subscription" className="gap-2">
            <Settings className="h-4 w-4" />
            Abonelik
          </TabsTrigger>
          <TabsTrigger value="payment" className="gap-2">
            <CreditCard className="h-4 w-4" />
            Ödeme Yöntemleri
          </TabsTrigger>
          <TabsTrigger value="invoices" className="gap-2">
            <Receipt className="h-4 w-4" />
            Faturalar
          </TabsTrigger>
        </TabsList>

        {/* Subscription Tab */}
        <TabsContent value="subscription" className="space-y-6">
          {/* Current Subscription */}
          <SubscriptionStatusCard
            subscription={subscription}
            isLoading={subscriptionLoading}
            onUpgrade={handleUpgrade}
            onChangePlan={handleChangePlan}
            onCancel={() => setShowCancelDialog(true)}
            onReactivate={handleReactivate}
          />

          {/* Available Plans */}
          {(isFreePlan || isCancelled) && (
            <Card>
              <CardHeader>
                <CardTitle>Mevcut Planlar</CardTitle>
                <CardDescription>
                  İhtiyaçlarınıza uygun planı seçerek özelliklerinizi genişletin.
                </CardDescription>
              </CardHeader>
              <CardContent>
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
                        isCurrentPlan={plan.code === currentPlanCode}
                        onSelect={() => router.push(`/billing/checkout?plan=${plan.code}`)}
                      />
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </TabsContent>

        {/* Payment Methods Tab */}
        <TabsContent value="payment" className="space-y-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle>Kayıtlı Kartlar</CardTitle>
                <CardDescription>
                  Abonelik ödemeleri için kullanılacak kartlarınız.
                </CardDescription>
              </div>
              <Button onClick={() => setShowAddCard(true)} size="sm">
                <Plus className="mr-2 h-4 w-4" />
                Kart Ekle
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              {paymentMethodsLoading ? (
                <div className="space-y-4">
                  {[...Array(2)].map((_, i) => (
                    <div
                      key={i}
                      className="h-20 animate-pulse rounded-lg bg-muted"
                    />
                  ))}
                </div>
              ) : paymentMethods && paymentMethods.length > 0 ? (
                paymentMethods.map((method) => (
                  <PaymentMethodCard
                    key={method.id}
                    paymentMethod={method}
                    onSetDefault={handleSetDefaultCard}
                    onDelete={(id) => setDeleteCardId(id)}
                    isLoading={
                      deletePaymentMethod.isPending ||
                      setDefaultPaymentMethod.isPending
                    }
                  />
                ))
              ) : (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                  <CreditCard className="h-12 w-12 text-muted-foreground mb-4" />
                  <h3 className="text-lg font-medium">Kayıtlı kart bulunmuyor</h3>
                  <p className="text-sm text-muted-foreground mb-4">
                    Abonelik ödemeleri için bir kart ekleyin.
                  </p>
                  <Button onClick={() => setShowAddCard(true)}>
                    <Plus className="mr-2 h-4 w-4" />
                    Kart Ekle
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Invoices Tab */}
        <TabsContent value="invoices">
          <Card>
            <CardHeader>
              <CardTitle>Fatura Geçmişi</CardTitle>
              <CardDescription>
                Geçmiş abonelik ödemelerinizin faturaları.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <InvoiceList
                invoices={invoices?.content || []}
                isLoading={invoicesLoading}
                onDownload={handleDownloadInvoice}
              />
            </CardContent>
          </Card>
        </TabsContent>

      </Tabs>

      {/* Add Card Modal */}
      <AddCardModal
        open={showAddCard}
        onClose={() => setShowAddCard(false)}
      />

      {/* Delete Card Confirmation */}
      <AlertDialog open={!!deleteCardId} onOpenChange={() => setDeleteCardId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Kartı Sil</AlertDialogTitle>
            <AlertDialogDescription>
              Bu kartı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>İptal</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteCard}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Sil
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Cancel Subscription Confirmation */}
      <AlertDialog open={showCancelDialog} onOpenChange={setShowCancelDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Aboneliği İptal Et</AlertDialogTitle>
            <AlertDialogDescription>
              Aboneliğinizi iptal etmek istediğinizden emin misiniz? Mevcut dönem
              sonuna kadar tüm özelliklere erişmeye devam edeceksiniz. Dönem
              sonunda hesabınız ücretsiz plana geçecektir.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Vazgeç</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleCancelSubscription}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Aboneliği İptal Et
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
