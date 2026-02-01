import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type {
  TrackedProduct,
  TrackedProductDetail,
  StockTrackingDashboard,
  StockAlert,
  AddTrackedProductRequest,
  UpdateAlertSettingsRequest,
  ProductPreview,
} from "@/types/stock-tracking";

// Query Keys
export const stockTrackingKeys = {
  all: ["stock-tracking"] as const,
  dashboard: (storeId: string) =>
    [...stockTrackingKeys.all, "dashboard", storeId] as const,
  products: (storeId: string) =>
    [...stockTrackingKeys.all, "products", storeId] as const,
  product: (productId: string, storeId: string) =>
    [...stockTrackingKeys.all, "product", productId, storeId] as const,
  alerts: (storeId: string, unreadOnly?: boolean) =>
    [...stockTrackingKeys.all, "alerts", storeId, unreadOnly] as const,
  preview: (url: string) =>
    [...stockTrackingKeys.all, "preview", url] as const,
};

// Fetch functions
async function fetchDashboard(storeId: string): Promise<StockTrackingDashboard> {
  const response = await fetch(`/api/stock-tracking/stores/${storeId}/dashboard`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to fetch dashboard");
  }
  return response.json();
}

async function fetchTrackedProducts(storeId: string): Promise<TrackedProduct[]> {
  const response = await fetch(`/api/stock-tracking/stores/${storeId}/products`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to fetch tracked products");
  }
  return response.json();
}

async function fetchProductDetail(
  productId: string,
  storeId: string
): Promise<TrackedProductDetail> {
  const response = await fetch(
    `/api/stock-tracking/products/${productId}?storeId=${storeId}`
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to fetch product detail");
  }
  return response.json();
}

async function fetchAlerts(
  storeId: string,
  unreadOnly: boolean = false
): Promise<StockAlert[]> {
  const response = await fetch(
    `/api/stock-tracking/stores/${storeId}/alerts?unreadOnly=${unreadOnly}`
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to fetch alerts");
  }
  return response.json();
}

async function fetchProductPreview(url: string): Promise<ProductPreview> {
  const response = await fetch(
    `/api/stock-tracking/preview?url=${encodeURIComponent(url)}`
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    return {
      productId: null,
      productName: null,
      brandName: null,
      imageUrl: null,
      price: null,
      quantity: null,
      inStock: null,
      isValid: false,
      errorMessage: error.error || "Failed to fetch product preview",
    };
  }
  return response.json();
}

// Queries
export function useStockTrackingDashboard(storeId: string | undefined) {
  return useQuery({
    queryKey: stockTrackingKeys.dashboard(storeId || ""),
    queryFn: () => fetchDashboard(storeId!),
    enabled: !!storeId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

export function useTrackedProducts(storeId: string | undefined) {
  return useQuery({
    queryKey: stockTrackingKeys.products(storeId || ""),
    queryFn: () => fetchTrackedProducts(storeId!),
    enabled: !!storeId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

export function useTrackedProductDetail(
  productId: string | undefined,
  storeId: string | undefined
) {
  return useQuery({
    queryKey: stockTrackingKeys.product(productId || "", storeId || ""),
    queryFn: () => fetchProductDetail(productId!, storeId!),
    enabled: !!productId && !!storeId,
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
}

export function useStockAlerts(
  storeId: string | undefined,
  unreadOnly: boolean = false
) {
  return useQuery({
    queryKey: stockTrackingKeys.alerts(storeId || "", unreadOnly),
    queryFn: () => fetchAlerts(storeId!, unreadOnly),
    enabled: !!storeId,
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
}

/**
 * Hook for previewing a product URL before adding to tracking.
 * Only fetches when URL is a valid Trendyol product URL.
 */
export function useProductPreview(url: string) {
  // Validate URL format before fetching
  const isValidUrl = isValidTrendyolProductUrl(url);

  return useQuery({
    queryKey: stockTrackingKeys.preview(url),
    queryFn: () => fetchProductPreview(url),
    enabled: isValidUrl,
    staleTime: 1000 * 60 * 5, // 5 minutes cache
    retry: false, // Don't retry on failure
  });
}

/**
 * Validate if URL is a valid Trendyol product page URL.
 * Must have trendyol.com domain and -p-{id} pattern.
 */
export function isValidTrendyolProductUrl(url: string): boolean {
  if (!url || url.trim().length === 0) return false;

  try {
    const parsed = new URL(url);
    // Check domain
    if (!parsed.hostname.endsWith("trendyol.com")) return false;
    // Check product ID pattern
    const productIdMatch = url.match(/-p-(\d+)/);
    if (!productIdMatch) return false;
    return true;
  } catch {
    return false;
  }
}

// Mutations
export function useAddTrackedProduct(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: AddTrackedProductRequest) => {
      const response = await fetch(
        `/api/stock-tracking/stores/${storeId}/products`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(data),
        }
      );

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.error || "Failed to add product");
      }

      return response.json();
    },
    onSuccess: () => {
      if (storeId) {
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.products(storeId),
        });
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.dashboard(storeId),
        });
      }
    },
  });
}

export function useRemoveTrackedProduct(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (productId: string) => {
      const response = await fetch(
        `/api/stock-tracking/products/${productId}?storeId=${storeId}`,
        { method: "DELETE" }
      );

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.error || "Failed to remove product");
      }

      return response.json();
    },
    onSuccess: () => {
      if (storeId) {
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.products(storeId),
        });
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.dashboard(storeId),
        });
      }
    },
  });
}

export function useUpdateAlertSettings(
  productId: string | undefined,
  storeId: string | undefined
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: UpdateAlertSettingsRequest) => {
      const response = await fetch(
        `/api/stock-tracking/products/${productId}/settings?storeId=${storeId}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(data),
        }
      );

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.error || "Failed to update settings");
      }

      return response.json();
    },
    onSuccess: () => {
      if (productId && storeId) {
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.product(productId, storeId),
        });
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.products(storeId),
        });
      }
    },
  });
}

export function useCheckStockNow(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (productId: string) => {
      const response = await fetch(
        `/api/stock-tracking/products/${productId}/check?storeId=${storeId}`,
        { method: "POST" }
      );

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.error || "Failed to check stock");
      }

      return response.json();
    },
    onSuccess: (_, productId) => {
      if (storeId) {
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.product(productId, storeId),
        });
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.products(storeId),
        });
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.dashboard(storeId),
        });
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.alerts(storeId),
        });
      }
    },
  });
}

export function useMarkAlertAsRead(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (alertId: string) => {
      const response = await fetch(
        `/api/stock-tracking/alerts/${alertId}/read?storeId=${storeId}`,
        { method: "PUT" }
      );

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.error || "Failed to mark alert as read");
      }

      return response.json();
    },
    onSuccess: () => {
      if (storeId) {
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.alerts(storeId),
        });
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.dashboard(storeId),
        });
      }
    },
  });
}

export function useMarkAllAlertsAsRead(storeId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const response = await fetch(
        `/api/stock-tracking/stores/${storeId}/alerts/mark-all-read`,
        { method: "PUT" }
      );

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.error || "Failed to mark all alerts as read");
      }

      return response.json();
    },
    onSuccess: () => {
      if (storeId) {
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.alerts(storeId),
        });
        queryClient.invalidateQueries({
          queryKey: stockTrackingKeys.dashboard(storeId),
        });
      }
    },
  });
}
