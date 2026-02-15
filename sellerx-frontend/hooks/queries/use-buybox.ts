import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { BuyboxSummary, BuyboxProductsResponse, BuyboxStatus } from "@/types/product";

export const buyboxKeys = {
  all: ["buybox"] as const,
  summary: (storeId: string) => [...buyboxKeys.all, "summary", storeId] as const,
  products: (storeId: string, filters: Record<string, any>) =>
    [...buyboxKeys.all, "products", storeId, filters] as const,
};

export function useBuyboxSummary(storeId?: string) {
  return useQuery<BuyboxSummary>({
    queryKey: buyboxKeys.summary(storeId!),
    queryFn: async () => {
      const response = await fetch(`/api/stores/${storeId}/buybox/summary`);
      if (!response.ok) throw new Error("Failed to fetch buybox summary");
      return response.json();
    },
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000,
  });
}

export function useBuyboxProducts(
  storeId?: string,
  options?: {
    page?: number;
    size?: number;
    status?: BuyboxStatus | "";
    search?: string;
    sortBy?: string;
    sortDir?: "asc" | "desc";
  }
) {
  const { page = 0, size = 20, status = "", search = "", sortBy = "buyboxOrder", sortDir = "asc" } = options || {};

  return useQuery<BuyboxProductsResponse>({
    queryKey: buyboxKeys.products(storeId!, { page, size, status, search, sortBy, sortDir }),
    queryFn: async () => {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sortBy,
        sortDir,
      });
      if (status) params.set("status", status);
      if (search) params.set("search", search);

      const response = await fetch(`/api/stores/${storeId}/buybox?${params.toString()}`);
      if (!response.ok) throw new Error("Failed to fetch buybox products");
      return response.json();
    },
    enabled: !!storeId,
    staleTime: 2 * 60 * 1000,
  });
}

export function useSyncBuybox() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (storeId: string) => {
      const response = await fetch(`/api/stores/${storeId}/buybox/sync`, {
        method: "POST",
      });
      if (!response.ok) throw new Error("Failed to sync buybox");
      return response;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: buyboxKeys.all });
    },
  });
}
