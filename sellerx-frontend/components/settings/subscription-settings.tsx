"use client";

import { useState } from "react";
import { CreditCard, Plus, AlertCircle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { SettingsSection, SettingsCard } from "./settings-section";
import { SubscriptionStatusCard } from "@/components/billing/subscription-status-card";
import { PaymentMethodCard } from "@/components/billing/payment-method-card";
import { PricingCard } from "@/components/billing/pricing-card";
import { AddCardModal } from "@/components/billing/add-card-modal";
import {
  useSubscription,
  usePlans,
  usePaymentMethods,
  useDeletePaymentMethod,
  useSetDefaultPaymentMethod,
  useCancelSubscription,
  useReactivateSubscription,
  useChangePlan,
} from "@/hooks/queries/use-billing";
import type { BillingCycle } from "@/types/billing";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { toast } from "sonner";

export function SubscriptionSettings() {
  const [showAddCard, setShowAddCard] = useState(false);
  const [showChangePlan, setShowChangePlan] = useState(false);
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);
  const [selectedCycle, setSelectedCycle] = useState<BillingCycle>("MONTHLY");
  const [selectedPlanCode, setSelectedPlanCode] = useState<string | null>(null);

  // Queries
  const { data: subscription, isLoading: subscriptionLoading } = useSubscription();
  const { data: plans, isLoading: plansLoading } = usePlans();
  const { data: paymentMethods, isLoading: paymentMethodsLoading } = usePaymentMethods();

  // Mutations
  const deletePaymentMethod = useDeletePaymentMethod();
  const setDefaultPaymentMethod = useSetDefaultPaymentMethod();
  const cancelSubscription = useCancelSubscription();
  const reactivateSubscription = useReactivateSubscription();
  const changePlan = useChangePlan();

  const handleDeleteCard = async (id: string) => {
    try {
      await deletePaymentMethod.mutateAsync(id);
      toast.success("Kart silindi");
    } catch (error: any) {
      toast.error(error.message || "Kart silinirken hata oluştu");
    }
  };

  const handleSetDefault = async (id: string) => {
    try {
      await setDefaultPaymentMethod.mutateAsync(id);
      toast.success("Varsayılan kart güncellendi");
    } catch (error: any) {
      toast.error(error.message || "Varsayılan kart güncellenirken hata oluştu");
    }
  };

  const handleCancelSubscription = async () => {
    try {
      await cancelSubscription.mutateAsync({ reason: "User requested", immediate: false });
      toast.success("Aboneliğiniz dönem sonunda iptal edilecek");
      setShowCancelConfirm(false);
    } catch (error: any) {
      toast.error(error.message || "Abonelik iptal edilirken hata oluştu");
    }
  };

  const handleReactivate = async () => {
    try {
      await reactivateSubscription.mutateAsync();
      toast.success("Aboneliğiniz yeniden aktif edildi");
    } catch (error: any) {
      toast.error(error.message || "Abonelik aktifleştirilirken hata oluştu");
    }
  };

  const handleChangePlan = async () => {
    if (!selectedPlanCode) return;

    try {
      await changePlan.mutateAsync({
        planCode: selectedPlanCode,
        billingCycle: selectedCycle,
      });
      toast.success("Plan değiştirildi");
      setShowChangePlan(false);
      setSelectedPlanCode(null);
    } catch (error: any) {
      toast.error(error.message || "Plan değiştirilirken hata oluştu");
    }
  };

  const handleSelectPlan = (planCode: string) => {
    if (subscription?.planCode === planCode) return;
    setSelectedPlanCode(planCode);
  };

  return (
    <div className="space-y-6">
      {/* Current Subscription Status */}
      <SettingsSection
        title="Mevcut Abonelik"
        description="Abonelik durumunuz ve plan detaylarınız"
      >
        <div className="-m-6">
          <SubscriptionStatusCard
            subscription={subscription}
            isLoading={subscriptionLoading}
            onUpgrade={() => setShowChangePlan(true)}
            onChangePlan={() => setShowChangePlan(true)}
            onCancel={() => setShowCancelConfirm(true)}
            onReactivate={handleReactivate}
          />
        </div>
      </SettingsSection>

      {/* Payment Methods */}
      <SettingsSection
        title="Ödeme Yöntemleri"
        description="Abonelik ödemeleri için kayıtlı kartlarınız"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowAddCard(true)}
          >
            <Plus className="h-4 w-4 mr-2" />
            Kart Ekle
          </Button>
        }
      >
        {paymentMethodsLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : !paymentMethods || paymentMethods.length === 0 ? (
          <div className="text-center py-8">
            <CreditCard className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">Kayıtlı kart yok</h3>
            <p className="text-sm text-muted-foreground mt-1">
              Abonelik ödemeleri için bir kart ekleyin.
            </p>
            <Button
              variant="outline"
              className="mt-4"
              onClick={() => setShowAddCard(true)}
            >
              <Plus className="h-4 w-4 mr-2" />
              Kart Ekle
            </Button>
          </div>
        ) : (
          <div className="space-y-3 -m-6 p-6">
            {paymentMethods.map((pm) => (
              <PaymentMethodCard
                key={pm.id}
                paymentMethod={pm}
                onSetDefault={handleSetDefault}
                onDelete={handleDeleteCard}
                isLoading={
                  deletePaymentMethod.isPending ||
                  setDefaultPaymentMethod.isPending
                }
              />
            ))}
          </div>
        )}
      </SettingsSection>

      {/* Plan Selection */}
      <SettingsSection
        title="Plan Değiştir"
        description="İhtiyaçlarınıza uygun planı seçin"
      >
        {/* Billing Cycle Toggle */}
        <div className="flex items-center justify-center gap-4 mb-6">
          <RadioGroup
            value={selectedCycle}
            onValueChange={(v) => setSelectedCycle(v as BillingCycle)}
            className="flex gap-4"
          >
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="MONTHLY" id="monthly" />
              <Label htmlFor="monthly">Aylık</Label>
            </div>
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="YEARLY" id="yearly" />
              <Label htmlFor="yearly">Yıllık (2 ay bedava)</Label>
            </div>
          </RadioGroup>
        </div>

        {plansLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : !plans || plans.length === 0 ? (
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              Planlar yüklenemedi. Lütfen daha sonra tekrar deneyin.
            </AlertDescription>
          </Alert>
        ) : (
          <div className="grid gap-4 md:grid-cols-3">
            {plans.map((plan) => (
              <PricingCard
                key={plan.code}
                plan={plan}
                selectedCycle={selectedCycle}
                isCurrentPlan={subscription?.planCode === plan.code}
                onSelect={handleSelectPlan}
                isLoading={changePlan.isPending}
              />
            ))}
          </div>
        )}

        {/* Confirm Plan Change */}
        {selectedPlanCode && (
          <div className="mt-4 p-4 bg-muted rounded-lg flex items-center justify-between">
            <div>
              <p className="font-medium">
                {plans?.find((p) => p.code === selectedPlanCode)?.name} planına
                geçmek istiyor musunuz?
              </p>
              <p className="text-sm text-muted-foreground">
                Değişiklik hemen uygulanacaktır.
              </p>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={() => setSelectedPlanCode(null)}
              >
                Vazgeç
              </Button>
              <Button
                onClick={handleChangePlan}
                disabled={changePlan.isPending}
              >
                {changePlan.isPending && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                Onayla
              </Button>
            </div>
          </div>
        )}
      </SettingsSection>

      {/* Add Card Modal */}
      <AddCardModal
        open={showAddCard}
        onClose={() => setShowAddCard(false)}
        onSuccess={() => toast.success("Kart başarıyla eklendi")}
      />

      {/* Cancel Subscription Confirm Dialog */}
      <Dialog open={showCancelConfirm} onOpenChange={setShowCancelConfirm}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Aboneliği İptal Et</DialogTitle>
            <DialogDescription>
              Aboneliğinizi iptal etmek istediğinizden emin misiniz? Mevcut
              dönem sonuna kadar erişiminiz devam edecektir.
            </DialogDescription>
          </DialogHeader>
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              İptal sonrası premium özelliklere erişiminiz kısıtlanacaktır.
            </AlertDescription>
          </Alert>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setShowCancelConfirm(false)}
            >
              Vazgeç
            </Button>
            <Button
              variant="destructive"
              onClick={handleCancelSubscription}
              disabled={cancelSubscription.isPending}
            >
              {cancelSubscription.isPending && (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              )}
              Evet, İptal Et
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
