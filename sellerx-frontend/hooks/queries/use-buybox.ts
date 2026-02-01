"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  BuyboxDashboard,
  BuyboxTrackedProduct,
  BuyboxProductDetail,
  BuyboxAlert,
  BuyboxSnapshot,
  AddProductRequest,
  UpdateAlertSettingsRequest,
} from "@/types/buybox";

/**
 * Buybox Query Keys Factory
 */
export const buyboxKeys = {
  all: ["buybox"] as const,
  dashboard: (storeId: string) =>
    [...buyboxKeys.all, "dashboard", storeId] as const,
  products: (storeId: string) =>
    [...buyboxKeys.all, "products", storeId] as const,
  productDetail: (trackedProductId: string) =>
    [...buyboxKeys.all, "detail", trackedProductId] as const,
  alerts: (storeId: string) => [...buyboxKeys.all, "alerts", storeId] as const,
};

/**
 * Dashboard verilerini çeker
 */
export function useBuyboxDashboard(storeId: string | undefined) {
  return useQuery<BuyboxDashboard>({
    queryKey: buyboxKeys.dashboard(storeId || ""),
    queryFn: async () => {
      const response = await fetch(`/api/buybox/stores/${storeId}/dashboard`);
      if (!response.ok) {
        throw new Error("Dashboard verileri alınamadı");
      }
      return response.json();
    },
    enabled: !!storeId,
    staleTime: 30000, // 30 saniye
    refetchInterval: 60000, // 1 dakika
  });
}

/**
 * Takip edilen ürünleri listeler
 */
export function useBuyboxProducts(storeId: string | undefined) {
  return useQuery<BuyboxTrackedProduct[]>({
    queryKey: buyboxKeys.products(storeId || ""),
    queryFn: async () => {
      const response = await fetch(`/api/buybox/stores/${storeId}/products`);
      if (!response.ok) {
        throw new Error("Ürün listesi alınamadı");
      }
      return response.json();
    },
    enabled: !!storeId,
    staleTime: 30000,
  });
}

/**
 * Takip edilen ürünün detaylarını çeker
 */
export function useBuyboxProductDetail(trackedProductId: string | undefined) {
  return useQuery<BuyboxProductDetail>({
    queryKey: buyboxKeys.productDetail(trackedProductId || ""),
    queryFn: async () => {
      const response = await fetch(
        `/api/buybox/products/${trackedProductId}`
      );
      if (!response.ok) {
        throw new Error("Ürün detayları alınamadı");
      }
      return response.json();
    },
    enabled: !!trackedProductId,
    staleTime: 30000,
  });
}

/**
 * Alertleri çeker
 */
export function useBuyboxAlerts(storeId: string | undefined) {
  return useQuery<BuyboxAlert[]>({
    queryKey: buyboxKeys.alerts(storeId || ""),
    queryFn: async () => {
      const response = await fetch(`/api/buybox/stores/${storeId}/alerts`);
      if (!response.ok) {
        throw new Error("Alertler alınamadı");
      }
      return response.json();
    },
    enabled: !!storeId,
    staleTime: 30000,
    refetchInterval: 60000,
  });
}

/**
 * Ürünü takibe ekler
 */
export function useAddProductToTrack(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: AddProductRequest) => {
      const response = await fetch(`/api/buybox/stores/${storeId}/products`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || "Ürün takibe eklenemedi");
      }

      return response.json() as Promise<BuyboxTrackedProduct>;
    },
    onSuccess: () => {
      if (storeId) {
        queryClient.invalidateQueries({ queryKey: buyboxKeys.products(storeId) });
        queryClient.invalidateQueries({ queryKey: buyboxKeys.dashboard(storeId) });
      }
    },
  });
}

/**
 * Ürünü takipten çıkarır
 */
export function useRemoveProductFromTrack(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (trackedProductId: string) => {
      const response = await fetch(
        `/api/buybox/stores/${storeId}/products/${trackedProductId}`,
        { method: "DELETE" }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || "Ürün takipten çıkarılamadı");
      }
    },
    onSuccess: () => {
      if (storeId) {
        queryClient.invalidateQueries({ queryKey: buyboxKeys.products(storeId) });
        queryClient.invalidateQueries({ queryKey: buyboxKeys.dashboard(storeId) });
      }
    },
  });
}

/**
 * Alert ayarlarını günceller
 */
export function useUpdateAlertSettings(trackedProductId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: UpdateAlertSettingsRequest) => {
      const response = await fetch(
        `/api/buybox/products/${trackedProductId}/settings`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(request),
        }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || "Ayarlar güncellenemedi");
      }

      return response.json() as Promise<BuyboxTrackedProduct>;
    },
    onSuccess: (data) => {
      if (trackedProductId) {
        queryClient.invalidateQueries({
          queryKey: buyboxKeys.productDetail(trackedProductId),
        });
        queryClient.invalidateQueries({
          queryKey: buyboxKeys.products(data.storeId),
        });
        queryClient.invalidateQueries({
          queryKey: buyboxKeys.dashboard(data.storeId),
        });
      }
    },
  });
}

/**
 * Manuel buybox kontrolü yapar
 */
export function useCheckBuyboxNow(trackedProductId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const response = await fetch(
        `/api/buybox/products/${trackedProductId}/check`,
        { method: "POST" }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || "Buybox kontrolü yapılamadı");
      }

      return response.json() as Promise<BuyboxSnapshot>;
    },
    onSuccess: () => {
      if (trackedProductId) {
        queryClient.invalidateQueries({
          queryKey: buyboxKeys.productDetail(trackedProductId),
        });
        // Dashboard ve products da güncellenmeli
        queryClient.invalidateQueries({ queryKey: buyboxKeys.all });
      }
    },
  });
}

/**
 * Alertleri okundu olarak işaretler
 */
export function useMarkAlertsRead(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const response = await fetch(
        `/api/buybox/stores/${storeId}/alerts/mark-read`,
        { method: "POST" }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || "Alertler işaretlenemedi");
      }
    },
    onSuccess: () => {
      if (storeId) {
        queryClient.invalidateQueries({ queryKey: buyboxKeys.alerts(storeId) });
        queryClient.invalidateQueries({ queryKey: buyboxKeys.dashboard(storeId) });
      }
    },
  });
}
