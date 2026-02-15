// Type imports
import type { DashboardStats, DashboardStatsResponse, DeductionBreakdown, MultiPeriodStatsResponse, PLPeriodType } from "@/types/dashboard";
import type {
  ProductListResponse,
  SyncProductsResponse,
  TrendyolProduct,
} from "@/types/product";
import type { TrendyolOrder, SyncOrdersResponse } from "@/types/order";
import type { PagedResponse } from "@/types/api";
import type {
  ReturnAnalyticsResponse,
  ReturnedOrderDecision,
  TrendyolClaim,
  ClaimsPage,
  ClaimsSyncResponse,
  ClaimActionResponse,
  ClaimIssueReason,
  ClaimsStats,
  BulkActionResponse,
  ClaimItemAudit,
} from "@/types/returns";

import { logger } from "@/lib/logger";
import { getImpersonationToken } from "@/hooks/use-impersonation";

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
    logger.debug("Token refresh already in progress, waiting", { endpoint: "/api/auth/refresh" });
    return new Promise((resolve) => {
      subscribeTokenRefresh(resolve);
    });
  }

  isRefreshing = true;
  logger.info("Starting token refresh", { endpoint: "/api/auth/refresh" });

  try {
    const response = await fetch("/api/auth/refresh", {
      method: "POST",
      credentials: "include",
      cache: "no-store", // Prevent caching refresh requests
    });

    const success = response.ok;
    logger.info(`Token refresh ${success ? "successful" : "failed"}`, {
      endpoint: "/api/auth/refresh",
      status: response.status,
    });
    onRefreshed(success);
    return success;
  } catch (error) {
    logger.error("Token refresh error", { endpoint: "/api/auth/refresh", error });
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
    const method = options.method || "GET";
    logger.debug(`${method} ${endpoint}`, {
      endpoint,
      method,
      ...(retryCount > 0 ? { retry: retryCount } : {}),
    });

    // Build headers — inject impersonation token if present
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...(options.headers as Record<string, string>),
    };
    const impersonationToken = getImpersonationToken();
    if (impersonationToken) {
      headers["X-Impersonation-Token"] = impersonationToken;

      // Block write operations during impersonation (read-only mode)
      if (method !== "GET") {
        throw new Error("Salt okunur modda bu işlem kullanılamaz");
      }
    }

    const config: RequestInit = {
      headers,
      credentials: "include", // Cookie'leri dahil et
      ...options,
    };

    const response = await fetch(url, config);
    logger.debug(`${method} ${endpoint} responded`, {
      endpoint,
      method,
      status: response.status,
    });

    // 401 Unauthorized - Token expired
    if (response.status === 401 && retryCount === 0) {
      // During impersonation, don't attempt token refresh — just throw
      if (impersonationToken) {
        logger.warn("401 during impersonation session, token expired", { endpoint, method });
        throw new Error("Impersonation oturumu sona erdi");
      }

      logger.warn(`401 Unauthorized, attempting token refresh`, {
        endpoint,
        method,
        status: 401,
      });
      if (!isRefreshing) {
        isRefreshing = true;

        const refreshSuccess = await refreshAuthToken();
        isRefreshing = false;

        if (refreshSuccess) {
          logger.info(`Retrying after successful refresh`, { endpoint, method });
          onRefreshed(true);
          // Retry the original request with new token
          return makeRequest(1);
        } else {
          // Refresh failed, redirect to login
          window.location.href = "/sign-in";
          throw new Error("Kimlik dogrulama basarisiz");
        }
      } else {
        // Already refreshing, wait for it
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((success) => {
            if (success) {
              makeRequest(1).then(resolve).catch(reject);
            } else {
              window.location.href = "/sign-in";
              reject(new Error("Kimlik dogrulama basarisiz"));
            }
          });
        });
      }
    }

    if (!response.ok) {
      const error = await response
        .json()
        .catch(() => ({ message: "Ag hatasi" }));
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
  // Get sync progress for a store
  getSyncProgress: (id: string) =>
    apiRequest<{
      syncStatus: string;
      currentProcessingDate: string | null;
      completedChunks: number | null;
      totalChunks: number | null;
      percentage: number;
      checkpointDate: string | null;
      startDate: string | null;
    }>(`/stores/${id}/sync-progress`),
  // Cancel sync for a store
  cancelSync: (id: string) =>
    apiRequest<string>(`/stores/${id}/cancel-sync`, {
      method: "POST",
    }),
  // Retry sync for a store
  retrySync: (id: string) =>
    apiRequest<string>(`/stores/${id}/retry-sync`, {
      method: "POST",
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
  // Activity logs
  getActivityLogs: (limit = 20) =>
    apiRequest<ActivityLogEntry[]>(`/users/activity-logs?limit=${limit}`),
};

// Activity Log types
export interface ActivityLogEntry {
  id: number;
  action: string;
  device: string;
  browser: string;
  ipAddress: string;
  success: boolean;
  createdAt: string;
}

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

  getStatsByRange: (
    storeId: string,
    startDate: string,
    endDate: string,
    periodLabel?: string,
  ) => {
    const params = new URLSearchParams({
      startDate,
      endDate,
    });
    if (periodLabel) {
      params.append("periodLabel", periodLabel);
    }
    return apiRequest<DashboardStats>(
      `/dashboard/stats/${storeId}/range?${params.toString()}`,
    );
  },

  getMultiPeriodStats: (
    storeId: string,
    periodType: PLPeriodType = "monthly",
    periodCount: number = 12,
    productBarcode?: string,
  ) => {
    const params = new URLSearchParams({
      periodType,
      periodCount: periodCount.toString(),
    });
    if (productBarcode) {
      params.append("productBarcode", productBarcode);
    }
    return apiRequest<MultiPeriodStatsResponse>(
      `/dashboard/stats/${storeId}/multi-period?${params.toString()}`,
    );
  },

  /**
   * Get deduction invoice breakdown by transaction type for a date range.
   * Used for dashboard detail panel to show all invoice types individually.
   */
  getDeductionBreakdown: (
    storeId: string,
    startDate: string,
    endDate: string,
  ) => {
    const params = new URLSearchParams({ startDate, endDate });
    return apiRequest<DeductionBreakdown[]>(
      `/stores/${storeId}/deductions/breakdown?${params.toString()}`,
    );
  },
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
      filters?: {
        search?: string;
        minStock?: number;
        maxStock?: number;
        minPrice?: number;
        maxPrice?: number;
        minCommission?: number;
        maxCommission?: number;
        minCost?: number;
        maxCost?: number;
      };
    },
  ) => {
    const params = new URLSearchParams();
    params.set("page", String(options?.page ?? 0));
    params.set("size", String(options?.size ?? 50));
    params.set("sortBy", options?.sortBy ?? "onSale");
    params.set("sortDirection", options?.sortDirection ?? "desc");

    // Add filter parameters if provided
    const filters = options?.filters;
    if (filters) {
      if (filters.search) params.set("search", filters.search);
      if (filters.minStock !== undefined) params.set("minStock", String(filters.minStock));
      if (filters.maxStock !== undefined) params.set("maxStock", String(filters.maxStock));
      if (filters.minPrice !== undefined) params.set("minPrice", String(filters.minPrice));
      if (filters.maxPrice !== undefined) params.set("maxPrice", String(filters.maxPrice));
      if (filters.minCommission !== undefined) params.set("minCommission", String(filters.minCommission));
      if (filters.maxCommission !== undefined) params.set("maxCommission", String(filters.maxCommission));
      if (filters.minCost !== undefined) params.set("minCost", String(filters.minCost));
      if (filters.maxCost !== undefined) params.set("maxCost", String(filters.maxCost));
    }

    return apiRequest<ProductListResponse>(`/products/store/${storeId}?${params.toString()}`);
  },
  updateCostAndStock: (
    productId: string,
    data: { quantity: number; unitCost: number; costVatRate: number; stockDate: string },
  ) =>
    apiRequest<TrendyolProduct>(`/products/${productId}/cost-and-stock`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  bulkUpdateCosts: (
    storeId: string,
    items: Array<{ barcode: string; unitCost: number; costVatRate: number; quantity: number; stockDate: string }>,
  ) =>
    apiRequest<{
      totalProcessed: number;
      successCount: number;
      failureCount: number;
      failedItems: Array<{ barcode: string; reason: string }>;
    }>(`/products/store/${storeId}/bulk-cost-update`, {
      method: "POST",
      body: JSON.stringify({ items }),
    }),
  updateStockInfoByDate: (
    productId: string,
    stockDate: string,
    data: { quantity: number; unitCost: number; costVatRate: number },
  ) =>
    apiRequest<TrendyolProduct>(`/products/${productId}/stock-info/${stockDate}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  deleteStockInfoByDate: (productId: string, stockDate: string) =>
    apiRequest<TrendyolProduct>(`/products/${productId}/stock-info/${stockDate}`, {
      method: "DELETE",
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
      `/orders/stores/${storeId}/by-date-range?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}&page=${page}&size=${size}`,
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

// Financial API
export interface FinancialStats {
  totalOrders: number;
  settledOrders: number;
  notSettledOrders: number;
  settlementRate: number;
  transactionStats: {
    totalSaleTransactions: number;
    totalReturnTransactions: number;
    totalSaleRevenue: number;
    totalReturnAmount: number;
    netRevenue: number;
  };
}

export interface FinancialSyncResponse {
  message: string;
  storeId: string;
}

export const financialApi = {
  getStats: (storeId: string) =>
    apiRequest<FinancialStats>(`/financial/stores/${storeId}/stats`),

  sync: (storeId: string) =>
    apiRequest<FinancialSyncResponse>(`/financial/stores/${storeId}/sync`, {
      method: "POST",
    }),
};

// Webhook API
export interface WebhookStatus {
  storeId: string;
  webhookId: string | null;
  enabled: boolean;
  webhookUrl?: string;
  eventStats: Record<string, number>;
  totalEvents: number;
}

export interface WebhookEvent {
  id: string;
  eventId: string;
  storeId: string;
  sellerId: string;
  eventType: string;
  orderNumber: string | null;
  status: string | null;
  payload: string | null;
  processingStatus: "RECEIVED" | "PROCESSING" | "COMPLETED" | "FAILED" | "DUPLICATE";
  errorMessage: string | null;
  processingTimeMs: number | null;
  createdAt: string;
  processedAt: string | null;
}

export interface WebhookEventsResponse {
  content: WebhookEvent[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const webhookApi = {
  getStatus: (storeId: string) =>
    apiRequest<WebhookStatus>(`/stores/${storeId}/webhooks/status`),

  enable: (storeId: string) =>
    apiRequest<{ success: boolean; webhookId?: string; message: string }>(
      `/stores/${storeId}/webhooks/enable`,
      { method: "POST" }
    ),

  disable: (storeId: string) =>
    apiRequest<{ success: boolean; message: string }>(
      `/stores/${storeId}/webhooks/disable`,
      { method: "POST" }
    ),

  getEvents: (storeId: string, page = 0, size = 20, eventType?: string) => {
    let url = `/stores/${storeId}/webhooks/events?page=${page}&size=${size}`;
    if (eventType) {
      url += `&eventType=${encodeURIComponent(eventType)}`;
    }
    return apiRequest<WebhookEventsResponse>(url);
  },

  test: (storeId: string) =>
    apiRequest<{ success: boolean; message: string; eventId: string }>(
      `/stores/${storeId}/webhooks/test`,
      { method: "POST" }
    ),
};

// Returns API
export const returnsApi = {
  getAnalytics: (storeId: string, startDate: string, endDate: string) =>
    apiRequest<ReturnAnalyticsResponse>(
      `/returns/stores/${storeId}/analytics?startDate=${startDate}&endDate=${endDate}`
    ),

  getCurrentMonthAnalytics: (storeId: string) =>
    apiRequest<ReturnAnalyticsResponse>(
      `/returns/stores/${storeId}/analytics/current-month`
    ),

  getLast30DaysAnalytics: (storeId: string) =>
    apiRequest<ReturnAnalyticsResponse>(
      `/returns/stores/${storeId}/analytics/last-30-days`
    ),

  getReturnedOrders: (storeId: string, startDate: string, endDate: string) =>
    apiRequest<ReturnedOrderDecision[]>(
      `/returns/stores/${storeId}/returned-orders?startDate=${startDate}&endDate=${endDate}`
    ),

  updateResalable: (storeId: string, orderNumber: string, isResalable: boolean) =>
    apiRequest<void>(
      `/returns/stores/${storeId}/orders/${orderNumber}/resalable`,
      {
        method: "PUT",
        body: JSON.stringify({ isResalable }),
      }
    ),
};

// Claims API (Trendyol Returns Management)
export const claimsApi = {
  // Get claims with pagination and optional filter
  getClaims: (storeId: string, filter?: string, page = 0, size = 20) => {
    let url = `/returns/stores/${storeId}/claims?page=${page}&size=${size}`;
    if (filter) {
      url += `&status=${filter}`;
    }
    return apiRequest<ClaimsPage>(url);
  },

  // Get single claim detail
  getClaim: (storeId: string, claimId: string) =>
    apiRequest<TrendyolClaim>(`/returns/stores/${storeId}/claims/${claimId}`),

  // Sync claims from Trendyol
  syncClaims: (storeId: string) =>
    apiRequest<ClaimsSyncResponse>(`/returns/stores/${storeId}/claims/sync`, {
      method: "POST",
    }),

  // Approve claim
  approveClaim: (storeId: string, claimId: string, claimLineItemIds: string[]) =>
    apiRequest<ClaimActionResponse>(
      `/returns/stores/${storeId}/claims/${claimId}/approve`,
      {
        method: "PUT",
        body: JSON.stringify({ claimLineItemIds }),
      }
    ),

  // Reject claim (create issue)
  rejectClaim: (
    storeId: string,
    claimId: string,
    reasonId: number,
    claimItemIds: string[],
    description?: string
  ) =>
    apiRequest<ClaimActionResponse>(
      `/returns/stores/${storeId}/claims/${claimId}/reject`,
      {
        method: "POST",
        body: JSON.stringify({ reasonId, claimItemIds, description }),
      }
    ),

  // Bulk approve claims
  bulkApproveClaims: (
    storeId: string,
    claims: { claimId: string; claimLineItemIds: string[] }[]
  ) =>
    apiRequest<BulkActionResponse>(
      `/returns/stores/${storeId}/claims/bulk-approve`,
      {
        method: "POST",
        body: JSON.stringify({ claims }),
      }
    ),

  // Get claims statistics
  getStats: (storeId: string) =>
    apiRequest<ClaimsStats>(`/returns/stores/${storeId}/stats`),

  // Get claim issue reasons
  getIssueReasons: () =>
    apiRequest<ClaimIssueReason[]>(`/returns/claim-issue-reasons`),

  // Get claim item audit trail
  getClaimItemAudit: (storeId: string, itemId: string) =>
    apiRequest<ClaimItemAudit[]>(`/returns/stores/${storeId}/claims/items/${itemId}/audit`),
};

// AI Types
import type {
  AiSettings,
  KnowledgeBaseItem,
  CreateKnowledgeRequest,
  AiGenerateResponse,
  AiApproveRequest,
} from "@/types/ai";

// Q&A API
export interface Question {
  id: string;
  storeId: string;
  questionId: string;
  productId: string;
  barcode: string;
  productTitle: string;
  customerQuestion: string;
  questionDate: string;
  status: string;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
  answers?: Answer[];
}

export interface Answer {
  id: string;
  questionId: string;
  answerText: string;
  isSubmitted: boolean;
  submittedAt: string | null;
  trendyolAnswerId: string | null;
  createdAt: string;
}

export interface QaStats {
  totalQuestions: number;
  pendingQuestions: number;
  answeredQuestions: number;
  todayQuestions: number;
  averageResponseTimeHours: number;
}

export interface QaSyncResponse {
  message: string;
  synced: number;
  total: number;
}

export const qaApi = {
  getQuestions: (storeId: string, page = 0, size = 20, status?: string) => {
    let url = `/qa/stores/${storeId}/questions?page=${page}&size=${size}`;
    if (status) {
      url += `&status=${status}`;
    }
    return apiRequest<PagedResponse<Question>>(url);
  },

  getQuestion: (questionId: string) =>
    apiRequest<Question>(`/qa/questions/${questionId}`),

  syncQuestions: (storeId: string) =>
    apiRequest<QaSyncResponse>(`/qa/stores/${storeId}/questions/sync`, {
      method: "POST",
    }),

  getStats: (storeId: string) =>
    apiRequest<QaStats>(`/qa/stores/${storeId}/stats`),

  // AI endpoints
  generateAiAnswer: (questionId: string) =>
    apiRequest<AiGenerateResponse>(`/qa/questions/${questionId}/ai-generate`, {
      method: "POST",
    }),

  approveAiAnswer: (questionId: string, data: AiApproveRequest) =>
    apiRequest<Answer>(`/qa/questions/${questionId}/ai-approve`, {
      method: "POST",
      body: JSON.stringify(data),
    }),
};

// AI Settings API
export const aiSettingsApi = {
  get: (storeId: string) =>
    apiRequest<AiSettings>(`/ai-settings/stores/${storeId}`),

  update: (storeId: string, data: Partial<AiSettings>) =>
    apiRequest<AiSettings>(`/ai-settings/stores/${storeId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
};

// Knowledge Base API
export const knowledgeApi = {
  getAll: (storeId: string) =>
    apiRequest<KnowledgeBaseItem[]>(`/knowledge/stores/${storeId}`),

  create: (storeId: string, data: CreateKnowledgeRequest) =>
    apiRequest<KnowledgeBaseItem>(`/knowledge/stores/${storeId}`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  update: (id: string, data: CreateKnowledgeRequest) =>
    apiRequest<KnowledgeBaseItem>(`/knowledge/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  delete: (id: string) =>
    apiRequest<void>(`/knowledge/${id}`, {
      method: "DELETE",
    }),

  toggle: (id: string, active: boolean) =>
    apiRequest<void>(`/knowledge/${id}/toggle?active=${active}`, {
      method: "PATCH",
    }),
};

// Supplier API
import type {
  Supplier,
  CreateSupplierRequest,
  UpdateSupplierRequest,
} from "@/types/supplier";

export const supplierApi = {
  getAll: (storeId: string) =>
    apiRequest<Supplier[]>(`/suppliers/store/${storeId}`),

  getById: (storeId: string, supplierId: number) =>
    apiRequest<Supplier>(`/suppliers/store/${storeId}/${supplierId}`),

  create: (storeId: string, data: CreateSupplierRequest) =>
    apiRequest<Supplier>(`/suppliers/store/${storeId}`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  update: (storeId: string, supplierId: number, data: UpdateSupplierRequest) =>
    apiRequest<Supplier>(`/suppliers/store/${storeId}/${supplierId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  delete: (storeId: string, supplierId: number) =>
    apiRequest<void>(`/suppliers/store/${storeId}/${supplierId}`, {
      method: "DELETE",
    }),
};

// Purchasing API
import type {
  PurchaseOrder,
  PurchaseOrderSummary,
  PurchaseOrderStats,
  CreatePurchaseOrderRequest,
  UpdatePurchaseOrderRequest,
  AddPurchaseOrderItemRequest,
  PurchaseOrderStatus,
  Attachment,
  ProductCostHistoryResponse,
  FifoAnalysisResponse,
  StockValuationResponse,
  ProfitabilityResponse,
  PurchaseSummaryResponse,
  DepletedProduct,
} from "@/types/purchasing";

export const purchasingApi = {
  // List purchase orders (with optional filters including date range)
  getOrders: (
    storeId: string,
    status?: PurchaseOrderStatus,
    search?: string,
    supplierId?: number,
    startDate?: string,
    endDate?: string
  ) => {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    if (search) params.set("search", search);
    if (supplierId) params.set("supplierId", supplierId.toString());
    if (startDate) params.set("startDate", startDate);
    if (endDate) params.set("endDate", endDate);
    const query = params.toString();
    return apiRequest<PurchaseOrderSummary[]>(`/purchasing/orders/${storeId}${query ? `?${query}` : ""}`);
  },

  // Get purchase order stats (with optional date filter)
  getStats: (storeId: string, startDate?: string, endDate?: string) => {
    const params = new URLSearchParams();
    if (startDate) params.set("startDate", startDate);
    if (endDate) params.set("endDate", endDate);
    const query = params.toString();
    return apiRequest<PurchaseOrderStats>(`/purchasing/orders/${storeId}/stats${query ? `?${query}` : ""}`);
  },

  // Get single purchase order
  getOrder: (storeId: string, poId: number) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}/${poId}`),

  // Create purchase order
  createOrder: (storeId: string, data: CreatePurchaseOrderRequest) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update purchase order
  updateOrder: (storeId: string, poId: number, data: UpdatePurchaseOrderRequest) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}/${poId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Delete purchase order
  deleteOrder: (storeId: string, poId: number) =>
    apiRequest<void>(`/purchasing/orders/${storeId}/${poId}`, {
      method: "DELETE",
    }),

  // Update status
  updateStatus: (storeId: string, poId: number, status: PurchaseOrderStatus) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}/${poId}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    }),

  // Add item to purchase order
  addItem: (storeId: string, poId: number, data: AddPurchaseOrderItemRequest) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}/${poId}/items`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update item
  updateItem: (storeId: string, poId: number, itemId: number, data: AddPurchaseOrderItemRequest) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}/${poId}/items/${itemId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Remove item
  removeItem: (storeId: string, poId: number, itemId: number) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}/${poId}/items/${itemId}`, {
      method: "DELETE",
    }),

  // === Duplicate & Split ===

  duplicateOrder: (storeId: string, poId: number) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}/${poId}/duplicate`, {
      method: "POST",
    }),

  splitOrder: (storeId: string, poId: number, itemIds: number[]) =>
    apiRequest<PurchaseOrder>(`/purchasing/orders/${storeId}/${poId}/split`, {
      method: "POST",
      body: JSON.stringify({ itemIds }),
    }),

  // === Attachments ===

  getAttachments: (storeId: string, poId: number) =>
    apiRequest<Attachment[]>(`/purchasing/orders/${storeId}/${poId}/attachments`),

  deleteAttachment: (storeId: string, poId: number, attachmentId: number) =>
    apiRequest<void>(`/purchasing/orders/${storeId}/${poId}/attachments/${attachmentId}`, {
      method: "DELETE",
    }),

  // Upload attachment (uses FormData, not JSON)
  uploadAttachment: async (storeId: string, poId: number, file: File): Promise<Attachment> => {
    const formData = new FormData();
    formData.append("file", file);
    const response = await fetch(`/api/purchasing/orders/${storeId}/${poId}/attachments`, {
      method: "POST",
      credentials: "include",
      body: formData,
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: "Upload failed" }));
      throw new Error(error.message || `HTTP ${response.status}`);
    }
    return response.json();
  },

  // Download attachment URL
  getAttachmentDownloadUrl: (storeId: string, poId: number, attachmentId: number) =>
    `/api/purchasing/orders/${storeId}/${poId}/attachments/${attachmentId}/download`,

  // === Export/Import ===

  getExportUrl: (storeId: string, poId: number) =>
    `/api/purchasing/orders/${storeId}/${poId}/export`,

  importItems: async (storeId: string, poId: number, file: File): Promise<PurchaseOrder> => {
    const formData = new FormData();
    formData.append("file", file);
    const response = await fetch(`/api/purchasing/orders/${storeId}/${poId}/import`, {
      method: "POST",
      credentials: "include",
      body: formData,
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: "Import failed" }));
      throw new Error(error.message || `HTTP ${response.status}`);
    }
    return response.json();
  },

  // === Reports ===

  // Get product cost history
  getProductCostHistory: (storeId: string, productId: string) =>
    apiRequest<ProductCostHistoryResponse>(`/purchasing/orders/${storeId}/reports/product/${productId}/cost-history`),

  // Get FIFO analysis
  getFifoAnalysis: (storeId: string, barcode: string, startDate: string, endDate: string) =>
    apiRequest<FifoAnalysisResponse>(
      `/purchasing/orders/${storeId}/reports/fifo-analysis?barcode=${encodeURIComponent(barcode)}&startDate=${startDate}&endDate=${endDate}`
    ),

  // Get stock valuation
  getStockValuation: (storeId: string) =>
    apiRequest<StockValuationResponse>(`/purchasing/orders/${storeId}/reports/stock-valuation`),

  // Get profitability analysis
  getProfitabilityAnalysis: (storeId: string, startDate: string, endDate: string) =>
    apiRequest<ProfitabilityResponse>(
      `/purchasing/orders/${storeId}/reports/profitability?startDate=${startDate}&endDate=${endDate}`
    ),

  // Get purchase summary
  getPurchaseSummary: (storeId: string, startDate: string, endDate: string) =>
    apiRequest<PurchaseSummaryResponse>(
      `/purchasing/orders/${storeId}/reports/summary?startDate=${startDate}&endDate=${endDate}`
    ),

  // Get depleted products
  getDepletedProducts: (storeId: string) =>
    apiRequest<DepletedProduct[]>(`/purchasing/orders/${storeId}/reports/stock-depletion`),
};

// Billing API
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
} from "@/types/billing";

export const billingApi = {
  // Plans
  getPlans: () => apiRequest<PlanWithPrices[]>("/billing/plans"),

  // Subscription
  getSubscription: () => apiRequest<Subscription>("/billing/subscription"),

  startTrial: (data: StartTrialRequest) =>
    apiRequest<Subscription>("/billing/subscription/trial", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  activateSubscription: () =>
    apiRequest<Subscription>("/billing/subscription/activate", {
      method: "POST",
    }),

  changePlan: (data: ChangePlanRequest) =>
    apiRequest<Subscription>("/billing/subscription/plan", {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  cancelSubscription: (data: CancelSubscriptionRequest) =>
    apiRequest<Subscription>("/billing/subscription/cancel", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  reactivateSubscription: () =>
    apiRequest<Subscription>("/billing/subscription/reactivate", {
      method: "POST",
    }),

  // Payment Methods
  getPaymentMethods: () => apiRequest<PaymentMethod[]>("/billing/payment-methods"),

  addPaymentMethod: (data: AddPaymentMethodRequest) =>
    apiRequest<PaymentMethod>("/billing/payment-methods", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  deletePaymentMethod: (id: string) =>
    apiRequest<void>(`/billing/payment-methods/${id}`, {
      method: "DELETE",
    }),

  setDefaultPaymentMethod: (id: string) =>
    apiRequest<PaymentMethod>(`/billing/payment-methods/${id}/default`, {
      method: "PUT",
    }),

  // Invoices
  getInvoices: (page: number = 0, size: number = 10) =>
    apiRequest<PaginatedResponse<Invoice>>(`/billing/invoices?page=${page}&size=${size}`),

  getInvoice: (id: string) => apiRequest<Invoice>(`/billing/invoices/${id}`),

  downloadInvoice: (id: string) => `/api/billing/invoices/${id}/pdf`,

  // Checkout
  startCheckout: (data: CheckoutRequest) =>
    apiRequest<CheckoutResponse>("/billing/checkout/start", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  complete3DS: (transactionId: string, paymentId: string) =>
    apiRequest<CheckoutResponse>("/billing/checkout/complete-3ds", {
      method: "POST",
      body: JSON.stringify({ transactionId, paymentId }),
    }),

  // Features
  getFeatures: () => apiRequest<FeatureInfo[]>("/billing/features"),

  checkFeatureAccess: (code: string) =>
    apiRequest<FeatureAccessResult>(`/billing/features/${code}`),
};
