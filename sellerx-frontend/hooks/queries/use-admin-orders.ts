import { useQuery } from "@tanstack/react-query";

export interface AdminOrderStats {
  totalOrders: number;
  todayOrders: number;
  thisWeekOrders: number;
  thisMonthOrders: number;
  statusBreakdown: Record<string, number>;
}

export interface AdminRecentOrder {
  id: string;
  orderNumber: string;
  storeName: string;
  storeId: string;
  status: string;
  totalPrice: number;
  orderDate: string;
  itemCount: number;
}

export interface AdminRecentOrdersPage {
  content: AdminRecentOrder[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface AdminProductStats {
  totalProducts: number;
  withCost: number;
  withoutCost: number;
  productsByStore: Record<string, number>;
}

export interface AdminTopProduct {
  id: string;
  barcode: string;
  title: string;
  storeName: string;
  orderCount: number;
  revenue: number;
}

export function useAdminOrderStats() {
  return useQuery<AdminOrderStats>({
    queryKey: ["admin", "orders", "stats"],
    queryFn: async () => {
      const response = await fetch("/api/admin/orders/stats");
      if (!response.ok) {
        throw new Error("Siparis istatistikleri alinamadi");
      }
      return response.json();
    },
    staleTime: 1000 * 60 * 5,
  });
}

export function useAdminRecentOrders(page = 0) {
  return useQuery<AdminRecentOrdersPage>({
    queryKey: ["admin", "orders", "recent", page],
    queryFn: async () => {
      const response = await fetch(
        `/api/admin/orders/recent?page=${page}&size=20`
      );
      if (!response.ok) {
        throw new Error("Son siparisler alinamadi");
      }
      return response.json();
    },
  });
}

export function useAdminProductStats() {
  return useQuery<AdminProductStats>({
    queryKey: ["admin", "products", "stats"],
    queryFn: async () => {
      const response = await fetch("/api/admin/products/stats");
      if (!response.ok) {
        throw new Error("Urun istatistikleri alinamadi");
      }
      return response.json();
    },
    staleTime: 1000 * 60 * 5,
  });
}

export function useAdminTopProducts() {
  return useQuery<AdminTopProduct[]>({
    queryKey: ["admin", "products", "top"],
    queryFn: async () => {
      const response = await fetch("/api/admin/products/top");
      if (!response.ok) {
        throw new Error("En cok satan urunler alinamadi");
      }
      return response.json();
    },
    staleTime: 1000 * 60 * 5,
  });
}
