import { useQuery } from "@tanstack/react-query";

export interface AdminRevenueStats {
  mrr: number;
  arr: number;
  churnRate: number;
  billingEnabled: boolean;
  totalRevenue: number;
  activeSubscriptions: number;
  trialSubscriptions: number;
  pastDueSubscriptions: number;
  cancelledSubscriptions: number;
}

export interface AdminRevenueHistoryItem {
  month: string;
  revenue: number;
  subscriptions: number;
}

export interface AdminSubscription {
  id: string;
  userEmail: string;
  userName: string;
  planName: string;
  status: string;
  billingCycle: string;
  currentPeriodEnd: string;
  createdAt: string;
}

export interface AdminSubscriptionsPage {
  content: AdminSubscription[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface AdminPayment {
  id: string;
  userEmail: string;
  amount: number;
  currency: string;
  status: string;
  description: string;
  createdAt: string;
}

export interface AdminPaymentsPage {
  content: AdminPayment[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export function useAdminRevenueStats() {
  return useQuery<AdminRevenueStats>({
    queryKey: ["admin", "revenue", "stats"],
    queryFn: async () => {
      const response = await fetch("/api/admin/billing/revenue/stats");
      if (!response.ok) {
        throw new Error("Gelir istatistikleri alinamadi");
      }
      return response.json();
    },
    staleTime: 1000 * 60 * 5,
  });
}

export function useAdminRevenueHistory() {
  return useQuery<AdminRevenueHistoryItem[]>({
    queryKey: ["admin", "revenue", "history"],
    queryFn: async () => {
      const response = await fetch("/api/admin/billing/revenue/history");
      if (!response.ok) {
        throw new Error("Gelir gecmisi alinamadi");
      }
      return response.json();
    },
    staleTime: 1000 * 60 * 5,
  });
}

export function useAdminSubscriptions(params?: {
  status?: string;
  page?: number;
  size?: number;
}) {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set("status", params.status);
  if (params?.page !== undefined) searchParams.set("page", String(params.page));
  if (params?.size) searchParams.set("size", String(params.size));

  return useQuery<AdminSubscriptionsPage>({
    queryKey: ["admin", "subscriptions", params],
    queryFn: async () => {
      const response = await fetch(
        `/api/admin/billing/subscriptions?${searchParams}`
      );
      if (!response.ok) {
        throw new Error("Abonelik listesi alinamadi");
      }
      return response.json();
    },
  });
}

export function useAdminPayments(page = 0) {
  return useQuery<AdminPaymentsPage>({
    queryKey: ["admin", "payments", page],
    queryFn: async () => {
      const response = await fetch(
        `/api/admin/billing/payments?page=${page}&size=20`
      );
      if (!response.ok) {
        throw new Error("Odeme listesi alinamadi");
      }
      return response.json();
    },
  });
}
