import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type {
  CrossSellRule,
  CrossSellSettings,
  CrossSellAnalytics,
  CreateCrossSellRuleRequest,
  UpdateCrossSellRuleRequest,
  UpdateCrossSellSettingsRequest,
  ReorderCrossSellRulesRequest,
  ProductSearchResult,
} from "@/types/cross-sell";

// Cross-Sell Query Keys
export const crossSellKeys = {
  all: ["cross-sell"] as const,
  rules: () => [...crossSellKeys.all, "rules"] as const,
  rulesByStore: (storeId: string) =>
    [...crossSellKeys.rules(), "store", storeId] as const,
  rule: (ruleId: string) =>
    [...crossSellKeys.rules(), "detail", ruleId] as const,
  settings: () => [...crossSellKeys.all, "settings"] as const,
  settingsByStore: (storeId: string) =>
    [...crossSellKeys.settings(), storeId] as const,
  analytics: () => [...crossSellKeys.all, "analytics"] as const,
  analyticsByStore: (storeId: string) =>
    [...crossSellKeys.analytics(), storeId] as const,
  productSearch: (storeId: string, query: string) =>
    [...crossSellKeys.all, "product-search", storeId, query] as const,
};

// =====================================================
// API Functions
// =====================================================

async function fetchRules(storeId: string): Promise<CrossSellRule[]> {
  const response = await fetch(`/api/cross-sell/stores/${storeId}/rules`);
  if (!response.ok) {
    throw new Error("Failed to fetch cross-sell rules");
  }
  return response.json();
}

async function fetchSettings(storeId: string): Promise<CrossSellSettings> {
  const response = await fetch(`/api/cross-sell/stores/${storeId}/settings`);
  if (!response.ok) {
    throw new Error("Failed to fetch cross-sell settings");
  }
  return response.json();
}

async function fetchAnalytics(storeId: string): Promise<CrossSellAnalytics> {
  const response = await fetch(
    `/api/cross-sell/stores/${storeId}/analytics`
  );
  if (!response.ok) {
    throw new Error("Failed to fetch cross-sell analytics");
  }
  return response.json();
}

async function createRule(
  storeId: string,
  data: CreateCrossSellRuleRequest
): Promise<CrossSellRule> {
  const response = await fetch(`/api/cross-sell/stores/${storeId}/rules`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to create cross-sell rule");
  }
  return response.json();
}

async function updateRule(
  ruleId: string,
  data: Partial<CreateCrossSellRuleRequest>
): Promise<CrossSellRule> {
  const response = await fetch(`/api/cross-sell/rules/${ruleId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to update cross-sell rule");
  }
  return response.json();
}

async function deleteRule(ruleId: string): Promise<void> {
  const response = await fetch(`/api/cross-sell/rules/${ruleId}`, {
    method: "DELETE",
  });
  if (!response.ok) {
    throw new Error("Failed to delete cross-sell rule");
  }
}

async function reorderRules(
  storeId: string,
  data: ReorderCrossSellRulesRequest
): Promise<CrossSellRule[]> {
  const response = await fetch(
    `/api/cross-sell/stores/${storeId}/rules/reorder`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    }
  );
  if (!response.ok) {
    throw new Error("Failed to reorder cross-sell rules");
  }
  return response.json();
}

async function updateSettings(
  storeId: string,
  data: UpdateCrossSellSettingsRequest
): Promise<CrossSellSettings> {
  const response = await fetch(
    `/api/cross-sell/stores/${storeId}/settings`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    }
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to update cross-sell settings");
  }
  return response.json();
}

async function searchProducts(
  storeId: string,
  query: string
): Promise<ProductSearchResult[]> {
  const params = new URLSearchParams({ q: query });
  const response = await fetch(
    `/api/cross-sell/stores/${storeId}/products/search?${params.toString()}`
  );
  if (!response.ok) {
    throw new Error("Failed to search products");
  }
  return response.json();
}

// =====================================================
// Query Hooks
// =====================================================

/**
 * Get all cross-sell rules for a store
 */
export function useCrossSellRules(storeId: string | undefined) {
  return useQuery({
    queryKey: crossSellKeys.rulesByStore(storeId!),
    queryFn: () => fetchRules(storeId!),
    enabled: !!storeId,
    staleTime: 2 * 60 * 1000,
  });
}

/**
 * Get cross-sell settings for a store
 */
export function useCrossSellSettings(storeId: string | undefined) {
  return useQuery({
    queryKey: crossSellKeys.settingsByStore(storeId!),
    queryFn: () => fetchSettings(storeId!),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Get cross-sell analytics for a store
 */
export function useCrossSellAnalytics(storeId: string | undefined) {
  return useQuery({
    queryKey: crossSellKeys.analyticsByStore(storeId!),
    queryFn: () => fetchAnalytics(storeId!),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Search products for cross-sell rule builder
 */
export function useProductSearch(
  storeId: string | undefined,
  query: string
) {
  return useQuery({
    queryKey: crossSellKeys.productSearch(storeId!, query),
    queryFn: () => searchProducts(storeId!, query),
    enabled: !!storeId && query.length >= 2,
    staleTime: 30 * 1000,
  });
}

// =====================================================
// Mutation Hooks
// =====================================================

/**
 * Create a new cross-sell rule
 */
export function useCreateCrossSellRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      data,
    }: {
      storeId: string;
      data: CreateCrossSellRuleRequest;
    }) => createRule(storeId, data),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: crossSellKeys.rulesByStore(storeId),
      });
    },
  });
}

/**
 * Update an existing cross-sell rule
 */
export function useUpdateCrossSellRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      ruleId,
      data,
    }: {
      ruleId: string;
      storeId: string;
      data: Partial<CreateCrossSellRuleRequest>;
    }) => updateRule(ruleId, data),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: crossSellKeys.rulesByStore(storeId),
      });
    },
  });
}

/**
 * Delete a cross-sell rule
 */
export function useDeleteCrossSellRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ ruleId }: { ruleId: string; storeId: string }) =>
      deleteRule(ruleId),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: crossSellKeys.rulesByStore(storeId),
      });
    },
  });
}

/**
 * Reorder cross-sell rules
 */
export function useReorderCrossSellRules() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      data,
    }: {
      storeId: string;
      data: ReorderCrossSellRulesRequest;
    }) => reorderRules(storeId, data),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: crossSellKeys.rulesByStore(storeId),
      });
    },
  });
}

/**
 * Update cross-sell settings
 */
export function useUpdateCrossSellSettings() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      data,
    }: {
      storeId: string;
      data: UpdateCrossSellSettingsRequest;
    }) => updateSettings(storeId, data),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: crossSellKeys.settingsByStore(storeId),
      });
    },
  });
}
