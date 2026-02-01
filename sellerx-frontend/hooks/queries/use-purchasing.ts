import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { purchasingApi } from "@/lib/api/client";
import type {
  PurchaseOrder,
  PurchaseOrderSummary,
  PurchaseOrderStats,
  PurchaseOrderStatus,
  CreatePurchaseOrderRequest,
  UpdatePurchaseOrderRequest,
  AddPurchaseOrderItemRequest,
  Attachment,
  ProductCostHistoryResponse,
  FifoAnalysisResponse,
  StockValuationResponse,
  ProfitabilityResponse,
  PurchaseSummaryResponse,
} from "@/types/purchasing";

// Query Keys
export const purchasingKeys = {
  all: ["purchasing"] as const,
  lists: () => [...purchasingKeys.all, "list"] as const,
  list: (storeId: string, filters?: { status?: PurchaseOrderStatus; search?: string; supplierId?: number }) =>
    [...purchasingKeys.lists(), storeId, filters] as const,
  stats: (storeId: string) => [...purchasingKeys.all, "stats", storeId] as const,
  details: () => [...purchasingKeys.all, "detail"] as const,
  detail: (storeId: string, poId: number) =>
    [...purchasingKeys.details(), storeId, poId] as const,
  // Attachments
  attachments: (storeId: string, poId: number) =>
    [...purchasingKeys.all, "attachments", storeId, poId] as const,
  // Reports
  reports: () => [...purchasingKeys.all, "reports"] as const,
  productCostHistory: (storeId: string, productId: string) =>
    [...purchasingKeys.reports(), "cost-history", storeId, productId] as const,
  fifoAnalysis: (storeId: string, barcode: string, startDate: string, endDate: string) =>
    [...purchasingKeys.reports(), "fifo-analysis", storeId, barcode, startDate, endDate] as const,
  stockValuation: (storeId: string) =>
    [...purchasingKeys.reports(), "stock-valuation", storeId] as const,
  profitability: (storeId: string, startDate: string, endDate: string) =>
    [...purchasingKeys.reports(), "profitability", storeId, startDate, endDate] as const,
  purchaseSummary: (storeId: string, startDate: string, endDate: string) =>
    [...purchasingKeys.reports(), "summary", storeId, startDate, endDate] as const,
};

// === Queries ===

// Get purchase orders list
export function usePurchaseOrders(
  storeId: string | undefined,
  status?: PurchaseOrderStatus,
  search?: string,
  supplierId?: number
) {
  return useQuery({
    queryKey: purchasingKeys.list(storeId!, { status, search, supplierId }),
    queryFn: () => purchasingApi.getOrders(storeId!, status, search, supplierId),
    enabled: !!storeId,
  });
}

// Get purchase order stats
export function usePurchaseOrderStats(storeId: string | undefined) {
  return useQuery({
    queryKey: purchasingKeys.stats(storeId!),
    queryFn: () => purchasingApi.getStats(storeId!),
    enabled: !!storeId,
  });
}

// Get single purchase order
export function usePurchaseOrder(storeId: string | undefined, poId: number | undefined) {
  return useQuery({
    queryKey: purchasingKeys.detail(storeId!, poId!),
    queryFn: () => purchasingApi.getOrder(storeId!, poId!),
    enabled: !!storeId && !!poId,
  });
}

// === Mutations ===

// Create purchase order
export function useCreatePurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, data }: { storeId: string; data: CreatePurchaseOrderRequest }) =>
      purchasingApi.createOrder(storeId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
    },
  });
}

// Update purchase order
export function useUpdatePurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      data,
    }: {
      storeId: string;
      poId: number;
      data: UpdatePurchaseOrderRequest;
    }) => purchasingApi.updateOrder(storeId, poId, data),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(
        purchasingKeys.detail(variables.storeId, variables.poId),
        data
      );
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
    },
  });
}

// Delete purchase order
export function useDeletePurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, poId }: { storeId: string; poId: number }) =>
      purchasingApi.deleteOrder(storeId, poId),
    onSuccess: (_, variables) => {
      queryClient.removeQueries({
        queryKey: purchasingKeys.detail(variables.storeId, variables.poId),
      });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
    },
  });
}

// Update purchase order status
export function useUpdatePurchaseOrderStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      status,
    }: {
      storeId: string;
      poId: number;
      status: PurchaseOrderStatus;
    }) => purchasingApi.updateStatus(storeId, poId, status),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(
        purchasingKeys.detail(variables.storeId, variables.poId),
        data
      );
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
    },
  });
}

// Add item to purchase order
export function useAddPurchaseOrderItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      data,
    }: {
      storeId: string;
      poId: number;
      data: AddPurchaseOrderItemRequest;
    }) => purchasingApi.addItem(storeId, poId, data),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(
        purchasingKeys.detail(variables.storeId, variables.poId),
        data
      );
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
    },
  });
}

// Update item in purchase order
export function useUpdatePurchaseOrderItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      itemId,
      data,
    }: {
      storeId: string;
      poId: number;
      itemId: number;
      data: AddPurchaseOrderItemRequest;
    }) => purchasingApi.updateItem(storeId, poId, itemId, data),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(
        purchasingKeys.detail(variables.storeId, variables.poId),
        data
      );
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
    },
  });
}

// Remove item from purchase order
export function useRemovePurchaseOrderItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      itemId,
    }: {
      storeId: string;
      poId: number;
      itemId: number;
    }) => purchasingApi.removeItem(storeId, poId, itemId),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(
        purchasingKeys.detail(variables.storeId, variables.poId),
        data
      );
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
    },
  });
}

// === Duplicate & Split ===

// Duplicate purchase order
export function useDuplicatePurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, poId }: { storeId: string; poId: number }) =>
      purchasingApi.duplicateOrder(storeId, poId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
    },
  });
}

// Split purchase order
export function useSplitPurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      itemIds,
    }: {
      storeId: string;
      poId: number;
      itemIds: number[];
    }) => purchasingApi.splitOrder(storeId, poId, itemIds),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
      queryClient.invalidateQueries({
        queryKey: purchasingKeys.detail(variables.storeId, variables.poId),
      });
    },
  });
}

// === Attachments ===

// Get attachments
export function usePOAttachments(storeId: string | undefined, poId: number | undefined) {
  return useQuery({
    queryKey: purchasingKeys.attachments(storeId!, poId!),
    queryFn: () => purchasingApi.getAttachments(storeId!, poId!),
    enabled: !!storeId && !!poId,
  });
}

// Upload attachment
export function useUploadAttachment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      file,
    }: {
      storeId: string;
      poId: number;
      file: File;
    }) => purchasingApi.uploadAttachment(storeId, poId, file),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: purchasingKeys.attachments(variables.storeId, variables.poId),
      });
    },
  });
}

// Delete attachment
export function useDeleteAttachment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      attachmentId,
    }: {
      storeId: string;
      poId: number;
      attachmentId: number;
    }) => purchasingApi.deleteAttachment(storeId, poId, attachmentId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: purchasingKeys.attachments(variables.storeId, variables.poId),
      });
    },
  });
}

// === Import/Export ===

// Import items from Excel
export function useImportPurchaseOrderItems() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      poId,
      file,
    }: {
      storeId: string;
      poId: number;
      file: File;
    }) => purchasingApi.importItems(storeId, poId, file),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(
        purchasingKeys.detail(variables.storeId, variables.poId),
        data
      );
      queryClient.invalidateQueries({ queryKey: purchasingKeys.lists() });
      queryClient.invalidateQueries({ queryKey: purchasingKeys.stats(variables.storeId) });
    },
  });
}

// === Report Queries ===

// Get product cost history
export function useProductCostHistory(storeId: string | undefined, productId: string | undefined) {
  return useQuery({
    queryKey: purchasingKeys.productCostHistory(storeId!, productId!),
    queryFn: () => purchasingApi.getProductCostHistory(storeId!, productId!),
    enabled: !!storeId && !!productId,
  });
}

// Get FIFO analysis
export function useFifoAnalysis(
  storeId: string | undefined,
  barcode: string | undefined,
  startDate: string | undefined,
  endDate: string | undefined
) {
  return useQuery({
    queryKey: purchasingKeys.fifoAnalysis(storeId!, barcode!, startDate!, endDate!),
    queryFn: () => purchasingApi.getFifoAnalysis(storeId!, barcode!, startDate!, endDate!),
    enabled: !!storeId && !!barcode && !!startDate && !!endDate,
  });
}

// Get stock valuation
export function useStockValuation(storeId: string | undefined) {
  return useQuery({
    queryKey: purchasingKeys.stockValuation(storeId!),
    queryFn: () => purchasingApi.getStockValuation(storeId!),
    enabled: !!storeId,
  });
}

// Get profitability analysis
export function useProfitabilityAnalysis(
  storeId: string | undefined,
  startDate: string | undefined,
  endDate: string | undefined
) {
  return useQuery({
    queryKey: purchasingKeys.profitability(storeId!, startDate!, endDate!),
    queryFn: () => purchasingApi.getProfitabilityAnalysis(storeId!, startDate!, endDate!),
    enabled: !!storeId && !!startDate && !!endDate,
  });
}

// Get purchase summary
export function usePurchaseSummary(
  storeId: string | undefined,
  startDate: string | undefined,
  endDate: string | undefined
) {
  return useQuery({
    queryKey: purchasingKeys.purchaseSummary(storeId!, startDate!, endDate!),
    queryFn: () => purchasingApi.getPurchaseSummary(storeId!, startDate!, endDate!),
    enabled: !!storeId && !!startDate && !!endDate,
  });
}
