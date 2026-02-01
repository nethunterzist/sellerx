import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { billingApi } from '@/lib/api/client';
import type {
  PlanWithPrices,
  Subscription,
  PaymentMethod,
  Invoice,
  FeatureInfo,
  FeatureAccessResult,
  AddPaymentMethodRequest,
  CheckoutRequest,
  CheckoutResponse,
  StartTrialRequest,
  ChangePlanRequest,
  CancelSubscriptionRequest,
  PaginatedResponse,
  BillingCycle,
} from '@/types/billing';

// Query Keys
export const billingKeys = {
  all: ['billing'] as const,
  plans: () => [...billingKeys.all, 'plans'] as const,
  subscription: () => [...billingKeys.all, 'subscription'] as const,
  paymentMethods: () => [...billingKeys.all, 'payment-methods'] as const,
  invoices: (page?: number) => [...billingKeys.all, 'invoices', { page }] as const,
  features: () => [...billingKeys.all, 'features'] as const,
  featureAccess: (code: string) => [...billingKeys.all, 'features', code] as const,
};

// ========== Plans ==========

export function usePlans() {
  return useQuery({
    queryKey: billingKeys.plans(),
    queryFn: billingApi.getPlans,
    staleTime: 1000 * 60 * 60, // 1 hour - plans don't change often
  });
}

// ========== Subscription ==========

export function useSubscription() {
  return useQuery({
    queryKey: billingKeys.subscription(),
    queryFn: async () => {
      try {
        return await billingApi.getSubscription();
      } catch (error: any) {
        if (error.status === 404) {
          return null;
        }
        throw error;
      }
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

export function useStartTrial() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.startTrial,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
    },
  });
}

export function useActivateSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.activateSubscription,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
    },
  });
}

export function useChangePlan() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.changePlan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
      queryClient.invalidateQueries({ queryKey: billingKeys.invoices() });
    },
  });
}

export function useCancelSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.cancelSubscription,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
    },
  });
}

export function useReactivateSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.reactivateSubscription,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
    },
  });
}

// ========== Payment Methods ==========

export function usePaymentMethods() {
  return useQuery({
    queryKey: billingKeys.paymentMethods(),
    queryFn: billingApi.getPaymentMethods,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

export function useAddPaymentMethod() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.addPaymentMethod,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.paymentMethods() });
    },
  });
}

export function useDeletePaymentMethod() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.deletePaymentMethod,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.paymentMethods() });
    },
  });
}

export function useSetDefaultPaymentMethod() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.setDefaultPaymentMethod,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.paymentMethods() });
    },
  });
}

// ========== Invoices ==========

export function useInvoices(page: number = 0, size: number = 10) {
  return useQuery({
    queryKey: billingKeys.invoices(page),
    queryFn: () => billingApi.getInvoices(page, size),
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

export function useInvoice(invoiceId: string | null) {
  return useQuery({
    queryKey: [...billingKeys.invoices(), invoiceId],
    queryFn: async () => {
      if (!invoiceId) return null;
      return billingApi.getInvoice(invoiceId);
    },
    enabled: !!invoiceId,
  });
}

// ========== Checkout ==========

export function useCheckout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: billingApi.startCheckout,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
      queryClient.invalidateQueries({ queryKey: billingKeys.paymentMethods() });
    },
  });
}

export function useComplete3DS() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: { transactionId: string; paymentId: string }) =>
      billingApi.complete3DS(request.transactionId, request.paymentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: billingKeys.subscription() });
      queryClient.invalidateQueries({ queryKey: billingKeys.invoices() });
    },
  });
}

// ========== Features ==========

export function useFeatures() {
  return useQuery({
    queryKey: billingKeys.features(),
    queryFn: billingApi.getFeatures,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

export function useFeatureAccess(featureCode: string) {
  return useQuery({
    queryKey: billingKeys.featureAccess(featureCode),
    queryFn: () => billingApi.checkFeatureAccess(featureCode),
    staleTime: 1000 * 60 * 1, // 1 minute
  });
}

// ========== Utility Hooks ==========

/**
 * Check if user has access to the application
 */
export function useHasAccess() {
  const { data: subscription, isLoading } = useSubscription();

  return {
    hasAccess: subscription?.hasAccess ?? false,
    isInTrial: subscription?.isInTrial ?? false,
    subscription,
    isLoading,
  };
}

/**
 * Check if user can add more stores
 */
export function useCanAddStore(currentStoreCount: number) {
  const { data: subscription } = useSubscription();

  if (!subscription) {
    return { canAdd: currentStoreCount < 1, maxStores: 1 }; // Free plan limit
  }

  const maxStores = subscription.maxStores;
  if (maxStores === null) {
    return { canAdd: true, maxStores: Infinity };
  }

  return { canAdd: currentStoreCount < maxStores, maxStores };
}

/**
 * Get the appropriate plan recommendation based on store count
 */
export function useRecommendedPlan(desiredStoreCount: number) {
  const { data: plans } = usePlans();

  if (!plans) return null;

  // Find the cheapest plan that supports the desired store count
  const suitablePlans = plans
    .filter(plan => plan.maxStores === null || plan.maxStores >= desiredStoreCount)
    .sort((a, b) => {
      const priceA = a.prices.find(p => p.billingCycle === 'MONTHLY')?.priceAmount ?? 0;
      const priceB = b.prices.find(p => p.billingCycle === 'MONTHLY')?.priceAmount ?? 0;
      return priceA - priceB;
    });

  return suitablePlans[0] || null;
}
