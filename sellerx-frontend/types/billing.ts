// Billing Types for SellerX

// Enums
export type BillingCycle = 'MONTHLY' | 'QUARTERLY' | 'SEMIANNUAL' | 'YEARLY';
export type SubscriptionStatus = 'PENDING_PAYMENT' | 'TRIAL' | 'ACTIVE' | 'PAST_DUE' | 'SUSPENDED' | 'CANCELLED' | 'EXPIRED';
export type InvoiceStatus = 'DRAFT' | 'PENDING' | 'PAID' | 'FAILED' | 'VOID' | 'REFUNDED';
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED' | 'PARTIALLY_REFUNDED';
export type FeatureType = 'BOOLEAN' | 'LIMIT' | 'UNLIMITED';

// Plan Types
export interface SubscriptionPlan {
  id: string;
  code: string;
  name: string;
  description: string | null;
  monthlyPrice: number | null;
  yearlyPrice: number | null;
  currency: string;
  maxStores: number | null;
  features: Record<string, any>;  // Array of feature objects from backend
  featuresMap?: Record<string, any>;  // Map format for easy lookup
  sortOrder: number;
  isFree?: boolean;
  isPopular?: boolean;
  hasUnlimitedStores?: boolean;
}

export interface SubscriptionPrice {
  id: string;
  billingCycle: BillingCycle;
  billingCycleDisplay?: string;
  price: number;          // Backend sends 'price', not 'priceAmount'
  priceAmount?: number;   // Alias for compatibility
  discountPercentage: number | null;
  monthlyEquivalent?: number;
  currency: string;
}

export interface PlanWithPrices extends SubscriptionPlan {
  prices: SubscriptionPrice[];
}

// Subscription Types (matches backend SubscriptionDto)
export interface Subscription {
  id: string;
  userId: number;
  status: SubscriptionStatus;
  // Flat plan fields (from backend)
  planCode: string;
  planName: string;
  maxStores: number | null;
  // Billing
  billingCycle: BillingCycle;
  price: number | null;
  currency: string;
  // Dates
  trialEndDate: string | null;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  // Grace period
  gracePeriodEnd?: string | null;
  // Flags
  cancelAtPeriodEnd: boolean;
  autoRenew: boolean;
  // Downgrade info
  hasDowngradeScheduled: boolean;
  downgradeToPlanCode: string | null;
  // Computed (from backend)
  isInTrial: boolean;
  hasAccess: boolean;
  daysRemaining: number;
  trialDaysRemaining: number;
}

// Payment Method Types
export interface PaymentMethod {
  id: string;
  type: 'CREDIT_CARD' | 'DEBIT_CARD';
  cardAlias: string | null;
  cardLastFour: string;
  cardBrand: string;
  cardFamily: string | null;
  cardBankName: string | null;
  cardExpMonth: number;
  cardExpYear: number;
  isDefault: boolean;
  createdAt: string;
}

export interface AddPaymentMethodRequest {
  cardAlias?: string;
  cardHolderName: string;
  cardNumber: string;
  expireMonth: string;
  expireYear: string;
}

// Invoice Types
export interface Invoice {
  id: string;
  invoiceNumber: string;
  status: InvoiceStatus;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
  currency: string;
  billingPeriodStart: string;
  billingPeriodEnd: string;
  dueDate: string;
  paidAt: string | null;
  lineItems: InvoiceLineItem[];
  notes: string | null;
  createdAt: string;
}

export interface InvoiceLineItem {
  description: string;
  quantity?: number;
  unitPrice?: string;
  amount: string;
  type?: 'CHARGE' | 'CREDIT';
  planCode?: string;
  periodStart?: string;
  periodEnd?: string;
  remainingDays?: number;
}

// Checkout Types
export interface CheckoutRequest {
  planCode: string;
  billingCycle: BillingCycle;
  paymentMethodId?: string;
  cardDetails?: {
    cardAlias?: string;
    cardHolderName: string;
    cardNumber: string;
    expireMonth: string;
    expireYear: string;
    cvc: string;
  };
}

export interface CheckoutResponse {
  success: boolean;
  status: 'SUCCESS' | 'TRIAL_STARTED' | 'REQUIRES_3DS' | 'ERROR';
  subscriptionId?: string;
  paymentId?: string;
  trialEndDate?: string;
  requires3DS: boolean;
  threeDSHtmlContent?: string;
  redirectUrl?: string;
  errorCode?: string;
  errorMessage?: string;
}

// Feature Types
export interface FeatureInfo {
  featureCode: string;
  enabled: boolean;
  featureType: FeatureType;
  limit: number | null;
  currentUsage: number | null;
}

export interface FeatureAccessResult {
  allowed: boolean;
  limitReached: boolean;
  message: string | null;
  limit: number | null;
  currentUsage: number | null;
}

// API Request/Response Types
export interface StartTrialRequest {
  planCode: string;
  billingCycle: BillingCycle;
}

export interface ChangePlanRequest {
  planCode: string;
  billingCycle: BillingCycle;
}

export interface CancelSubscriptionRequest {
  reason: string;
  immediate: boolean;
}

// Paginated Response
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// Utility Types
export interface BillingCycleInfo {
  code: BillingCycle;
  label: string;
  months: number;
  discountLabel?: string;
}

export const BILLING_CYCLES: BillingCycleInfo[] = [
  { code: 'MONTHLY', label: 'Aylık', months: 1 },
  { code: 'QUARTERLY', label: '3 Aylık', months: 3, discountLabel: '%10 İndirim' },
  { code: 'SEMIANNUAL', label: '6 Aylık', months: 6, discountLabel: '%20 İndirim' },
  { code: 'YEARLY', label: 'Yıllık', months: 12, discountLabel: '%30 İndirim' },
];

export const PLAN_FEATURES: Record<string, { label: string; description: string }> = {
  max_stores: { label: 'Mağaza Sayısı', description: 'Yönetebileceğiniz maksimum mağaza sayısı' },
  advanced_analytics: { label: 'Gelişmiş Analitik', description: 'Detaylı satış ve performans analizleri' },
  ai_qa_responses: { label: 'AI Yanıtları', description: 'Aylık AI destekli müşteri yanıtları' },
  webhook_support: { label: 'Webhook Desteği', description: 'Gerçek zamanlı sipariş bildirimleri' },
  api_access: { label: 'API Erişimi', description: 'Programatik API erişimi' },
  priority_support: { label: 'Öncelikli Destek', description: '7/24 öncelikli müşteri desteği' },
  parasut_integration: { label: 'Paraşüt Entegrasyonu', description: 'Otomatik e-fatura oluşturma' },
};

// Helper Functions
export function getSubscriptionStatusLabel(status: SubscriptionStatus): string {
  const labels: Record<SubscriptionStatus, string> = {
    PENDING_PAYMENT: 'Ödeme Bekleniyor',
    TRIAL: 'Deneme Süresi',
    ACTIVE: 'Aktif',
    PAST_DUE: 'Ödeme Gecikmiş',
    SUSPENDED: 'Askıya Alındı',
    CANCELLED: 'İptal Edildi',
    EXPIRED: 'Süresi Doldu',
  };
  return labels[status] || status;
}

export function getSubscriptionStatusColor(status: SubscriptionStatus): string {
  const colors: Record<SubscriptionStatus, string> = {
    PENDING_PAYMENT: 'yellow',
    TRIAL: 'blue',
    ACTIVE: 'green',
    PAST_DUE: 'orange',
    SUSPENDED: 'red',
    CANCELLED: 'gray',
    EXPIRED: 'gray',
  };
  return colors[status] || 'gray';
}

export function getInvoiceStatusLabel(status: InvoiceStatus): string {
  const labels: Record<InvoiceStatus, string> = {
    DRAFT: 'Taslak',
    PENDING: 'Beklemede',
    PAID: 'Ödendi',
    FAILED: 'Başarısız',
    VOID: 'İptal Edildi',
    REFUNDED: 'İade Edildi',
  };
  return labels[status] || status;
}

export function formatCurrency(amount: number, currency: string = 'TRY'): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
  }).format(amount);
}

export function formatBillingCycle(cycle: BillingCycle): string {
  const info = BILLING_CYCLES.find(c => c.code === cycle);
  return info?.label || cycle;
}
