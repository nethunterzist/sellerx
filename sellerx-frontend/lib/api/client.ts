// Type imports
import type { DashboardStatsResponse } from "@/types/dashboard";
import type {
  ProductListResponse,
  SyncProductsResponse,
  TrendyolProduct,
} from "@/types/product";
import type { TrendyolOrder, SyncOrdersResponse } from "@/types/order";
import type { PagedResponse } from "@/types/api";

// Auth token refresh function with better error handling
let isRefreshing = false;
let refreshSubscribers: Array<(success: boolean) => void> = [];

function subscribeTokenRefresh(cb: (success: boolean) => void) {
  refreshSubscribers.push(cb);
}

function onRefreshed(success: boolean) {
  refreshSubscribers.forEach((cb) => cb(success));
  refreshSubscribers = [];
}

async function refreshAuthToken(): Promise<boolean> {
  if (isRefreshing) {
    return new Promise((resolve) => {
      subscribeTokenRefresh(resolve);
    });
  }

  isRefreshing = true;

  try {
    const response = await fetch("/api/auth/refresh", {
      method: "POST",
      credentials: "include",
      cache: "no-store", // Prevent caching refresh requests
    });

    const success = response.ok;
    onRefreshed(success);
    return success;
  } catch {
    onRefreshed(false);
    return false;
  } finally {
    isRefreshing = false;
  }
}

// Generic API client for Next.js API routes
export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {},
): Promise<T> {
  // Use Next.js API routes instead of direct backend calls
  const url = `/api${endpoint}`;

  const makeRequest = async (retryCount = 0): Promise<T> => {
    // console.log(`[API Request] ${endpoint}, retryCount: ${retryCount}`);

    const config: RequestInit = {
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
      credentials: "include", // Cookie'leri dahil et
      ...options,
    };

    const response = await fetch(url, config);
    // console.log(`[API Response] ${endpoint}, status: ${response.status}`);

    // 401 Unauthorized - Token expired
    if (response.status === 401 && retryCount === 0) {
      // console.log(`[API] 401 detected, attempting refresh for ${endpoint}`);
      if (!isRefreshing) {
        isRefreshing = true;

        const refreshSuccess = await refreshAuthToken();
        isRefreshing = false;

        if (refreshSuccess) {
          // console.log(`[API] Refresh successful, retrying ${endpoint}`);
          onRefreshed(true);
          // Retry the original request with new token
          return makeRequest(1);
        } else {
          // Refresh failed, redirect to login
          window.location.href = "/sign-in";
          throw new Error("Kimlik doğrulama başarısız");
        }
      } else {
        // Already refreshing, wait for it
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((success) => {
            if (success) {
              makeRequest(1).then(resolve).catch(reject);
            } else {
              window.location.href = "/sign-in";
              reject(new Error("Kimlik doğrulama başarısız"));
            }
          });
        });
      }
    }

    if (!response.ok) {
      const error = await response
        .json()
        .catch(() => ({ message: "Ağ hatası" }));
      throw new Error(error.message || `HTTP ${response.status}`);
    }

    // 204 No Content
    if (response.status === 204) {
      return null as T;
    }

    // Content-Type control
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
      return response.json();
    }

    // If not JSON, return as text
    return null as T;
  };

  return makeRequest();
}

// Store API'leri için - Next.js API routes üzerinden
export const storeApi = {
  getAll: () => apiRequest<any[]>("/stores"),
  getById: (id: string) => apiRequest<any>(`/stores/${id}`),
  getMy: () => apiRequest<any[]>("/stores/my"),
  create: (data: any) =>
    apiRequest<any>("/stores", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  update: (id: string, data: any) =>
    apiRequest<any>(`/stores/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (id: string) =>
    apiRequest<any>(`/stores/${id}`, {
      method: "DELETE",
    }),
  // Selected store operations
  getSelectedStore: () =>
    apiRequest<{ selectedStoreId: string | null }>("/users/selected-store"),
  setSelectedStore: (storeId: string) =>
    apiRequest<any>("/users/selected-store", {
      method: "POST",
      body: JSON.stringify({ storeId }),
    }),
  // Test store connection (for existing store)
  testConnection: () =>
    apiRequest<{
      sellerId: string;
      marketplace: string;
      statusCode: number;
      message: string;
      storeId: string;
      connected: boolean;
      storeName: string;
    }>("/stores/test-connection"),
  // Test credentials before creating store
  testCredentials: (credentials: { sellerId: string; apiKey: string; apiSecret: string }) =>
    apiRequest<{
      connected: boolean;
      sellerId: string;
      storeName: string;
      message: string;
      error?: string;
    }>("/stores/test-credentials", {
      method: "POST",
      body: JSON.stringify(credentials),
    }),
};

// User API'leri için - Next.js API routes üzerinden
export const userApi = {
  getAll: () => apiRequest<any[]>("/users"),
  getById: (id: string) => apiRequest<any>(`/users/${id}`),
  update: (id: string, data: any) =>
    apiRequest<any>(`/users/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (id: string) =>
    apiRequest<any>(`/users/${id}`, {
      method: "DELETE",
    }),
  // Profile operations
  getProfile: () => apiRequest<any>("/users/profile"),
  updateProfile: (data: { name?: string; phone?: string; company?: string }) =>
    apiRequest<any>("/users/profile", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  // Password operations
  changePassword: (data: { currentPassword: string; newPassword: string; confirmPassword: string }) =>
    apiRequest<{ success: boolean; message: string }>("/users/password", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  // Preferences operations
  getPreferences: () => apiRequest<any>("/users/preferences"),
  updatePreferences: (data: any) =>
    apiRequest<any>("/users/preferences", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
};

// Products API'leri için - Next.js API routes üzerinden
export const productApi = {
  getAll: () => apiRequest<any[]>("/products"),
  getById: (id: string) => apiRequest<any>(`/products/${id}`),
  getByStore: (storeId: string) =>
    apiRequest<any[]>(`/products/store/${storeId}`),

  getByStorePaginated: (
    storeId: string,
    // options?: {
    //   page?: number;
    //   size?: number;
    //   sortBy?: string;
    //   sortDirection?: "asc" | "desc";
    // },
  ) => {
    // const {
    //   page = 0,
    //   size = 50,
    //   sortBy = "onSale",
    //   sortDirection = "desc",
    // } = options || {};
    // const query = `?page=${page}&size=${size}&sortBy=${sortBy}&sortDirection=${sortDirection}`;
    // return apiRequest<any[]>(`/products/store/${storeId}${query}`);
    return apiRequest<any[]>(`/products/store/${storeId}`);
  },

  create: (data: any) =>
    apiRequest<any>("/products", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  update: (id: string, data: any) =>
    apiRequest<any>(`/products/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (id: string) =>
    apiRequest<any>(`/products/${id}`, {
      method: "DELETE",
    }),
};

// Dashboard API
export const dashboardApi = {
  getStats: (storeId: string) =>
    apiRequest<DashboardStatsResponse>(`/dashboard/stats/${storeId}`),
};

// Products API - extended with sync
export const productApiExtended = {
  ...productApi,
  sync: (storeId: string) =>
    apiRequest<SyncProductsResponse>(`/products/sync/${storeId}`, {
      method: "POST",
    }),
  getByStorePaginated: (
    storeId: string,
    options?: {
      page?: number;
      size?: number;
      sortBy?: string;
      sortDirection?: "asc" | "desc";
    },
  ) => {
    const {
      page = 0,
      size = 50,
      sortBy = "onSale",
      sortDirection = "desc",
    } = options || {};
    const query = `?page=${page}&size=${size}&sortBy=${sortBy}&sortDirection=${sortDirection}`;
    return apiRequest<ProductListResponse>(`/products/store/${storeId}${query}`);
  },
  updateCostAndStock: (
    productId: string,
    data: { quantity: number; unitCost: number; costVatRate: number; stockDate: string },
  ) =>
    apiRequest<TrendyolProduct>(`/products/${productId}/cost-and-stock`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
};

// Orders API
export const ordersApi = {
  getByStore: (storeId: string, page = 0, size = 20) =>
    apiRequest<PagedResponse<TrendyolOrder>>(
      `/orders/stores/${storeId}?page=${page}&size=${size}`,
    ),
  getByDateRange: (
    storeId: string,
    startDate: string,
    endDate: string,
    page = 0,
    size = 20,
  ) =>
    apiRequest<PagedResponse<TrendyolOrder>>(
      `/orders/stores/${storeId}/by-date-range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`,
    ),
  sync: (storeId: string) =>
    apiRequest<SyncOrdersResponse>(`/orders/stores/${storeId}/sync`, {
      method: "POST",
    }),
};

// Legacy stat API for backwards compatibility
export const statApi = {
  getAll: () => apiRequest<any[]>("/dashboard/stats"),
};
