import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { productApi, productApiExtended } from "@/lib/api/client";
import type {
  TrendyolProduct,
  ProductListResponse,
  SyncProductsResponse,
} from "@/types/product";
import { dashboardKeys } from "@/hooks/useDashboardStats";

// Product Query Keys
export const productKeys = {
  all: ["products"] as const,
  lists: () => [...productKeys.all, "list"] as const,
  list: (filters: Record<string, any>) =>
    [...productKeys.lists(), { filters }] as const,
  details: () => [...productKeys.all, "detail"] as const,
  detail: (id: string) => [...productKeys.details(), id] as const,
  byStore: (storeId: string) => [...productKeys.all, "store", storeId] as const,
  byStorePaginated: (storeId: string, page: number, size: number) =>
    [...productKeys.all, "store", storeId, "paginated", { page, size }] as const,
};

// Get all products
export function useProducts() {
  return useQuery({
    queryKey: productKeys.lists(),
    queryFn: productApi.getAll,
  });
}

// Get products by store
export function useProductsByStore(storeId: string) {
  return useQuery({
    queryKey: productKeys.byStore(storeId),
    queryFn: () => productApi.getByStore(storeId),
    enabled: !!storeId,
  });
}

// Get product by ID
export function useProduct(id: string) {
  return useQuery({
    queryKey: productKeys.detail(id),
    queryFn: () => productApi.getById(id),
    enabled: !!id,
  });
}

// Get products by store with pagination
export function useProductsByStorePaginated(
  storeId: string,
  // options?: {
  //   page?: number;
  //   size?: number;
  //   sortBy?: string;
  //   sortDirection?: "asc" | "desc";
  // },
) {
  return useQuery({
    queryKey: productKeys.byStore(storeId),
    queryFn: () => productApi.getByStorePaginated(storeId),
    enabled: !!storeId,
  });
}

// Create product mutation
export function useCreateProduct() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: productApi.create,
    onSuccess: () => {
      // Invalidate and refetch products
      queryClient.invalidateQueries({ queryKey: productKeys.all });
    },
  });
}

// Update product mutation
export function useUpdateProduct() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) =>
      productApi.update(id, data),
    onSuccess: (data, variables) => {
      // Update the specific product in cache
      queryClient.setQueryData(productKeys.detail(variables.id), data);
      // Invalidate lists
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
}

// Delete product mutation
export function useDeleteProduct() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: productApi.delete,
    onSuccess: (data, id) => {
      // Remove from cache
      queryClient.removeQueries({ queryKey: productKeys.detail(id) });
      // Invalidate lists
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
}

// Sync products from Trendyol
export function useSyncProducts() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (storeId: string) => productApiExtended.sync(storeId),
    onSuccess: (data, storeId) => {
      // Invalidate products for this store
      queryClient.invalidateQueries({ queryKey: productKeys.byStore(storeId) });
      // Invalidate dashboard stats as product data may have changed
      queryClient.invalidateQueries({ queryKey: dashboardKeys.stats(storeId) });
    },
  });
}

// Update product cost and stock
export function useUpdateProductCostAndStock() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      productId,
      data,
    }: {
      productId: string;
      data: { quantity: number; unitCost: number; costVatRate: number; stockDate: string };
    }) => productApiExtended.updateCostAndStock(productId, data),
    onSuccess: (data, variables) => {
      // Update the specific product in cache
      queryClient.setQueryData(productKeys.detail(variables.productId), data);
      // Invalidate lists
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
}

// Get products by store with full pagination support
export function useProductsByStorePaginatedFull(
  storeId: string | undefined,
  options?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: "asc" | "desc";
  },
) {
  const { page = 0, size = 50 } = options || {};
  return useQuery<ProductListResponse>({
    queryKey: productKeys.byStorePaginated(storeId!, page, size),
    queryFn: () => productApiExtended.getByStorePaginated(storeId!, options),
    enabled: !!storeId,
    staleTime: 5 * 60 * 1000, // 5 dakika - cache'i daha uzun tut
    gcTime: 10 * 60 * 1000, // 10 dakika garbage collection
  });
}

// Bulk update product costs
export function useBulkUpdateCosts() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      items,
    }: {
      storeId: string;
      items: Array<{ barcode: string; unitCost: number; costVatRate: number; quantity: number; stockDate: string }>;
    }) => productApiExtended.bulkUpdateCosts(storeId, items),
    onSuccess: (data, variables) => {
      // Invalidate products for this store
      queryClient.invalidateQueries({ queryKey: productKeys.byStore(variables.storeId) });
      // Invalidate dashboard stats as product data may have changed
      queryClient.invalidateQueries({ queryKey: dashboardKeys.stats(variables.storeId) });
    },
  });
}
