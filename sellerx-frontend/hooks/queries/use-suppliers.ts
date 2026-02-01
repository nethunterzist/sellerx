import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { supplierApi } from "@/lib/api/client";
import type {
  Supplier,
  CreateSupplierRequest,
  UpdateSupplierRequest,
} from "@/types/supplier";

// Query Keys
export const supplierKeys = {
  all: ["suppliers"] as const,
  lists: () => [...supplierKeys.all, "list"] as const,
  list: (storeId: string) => [...supplierKeys.lists(), storeId] as const,
  details: () => [...supplierKeys.all, "detail"] as const,
  detail: (storeId: string, supplierId: number) =>
    [...supplierKeys.details(), storeId, supplierId] as const,
};

// === Queries ===

export function useSuppliers(storeId: string | undefined) {
  return useQuery({
    queryKey: supplierKeys.list(storeId!),
    queryFn: () => supplierApi.getAll(storeId!),
    enabled: !!storeId,
  });
}

export function useSupplier(storeId: string | undefined, supplierId: number | undefined) {
  return useQuery({
    queryKey: supplierKeys.detail(storeId!, supplierId!),
    queryFn: () => supplierApi.getById(storeId!, supplierId!),
    enabled: !!storeId && !!supplierId,
  });
}

// === Mutations ===

export function useCreateSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, data }: { storeId: string; data: CreateSupplierRequest }) =>
      supplierApi.create(storeId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: supplierKeys.list(variables.storeId) });
    },
  });
}

export function useUpdateSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      supplierId,
      data,
    }: {
      storeId: string;
      supplierId: number;
      data: UpdateSupplierRequest;
    }) => supplierApi.update(storeId, supplierId, data),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(
        supplierKeys.detail(variables.storeId, variables.supplierId),
        data
      );
      queryClient.invalidateQueries({ queryKey: supplierKeys.list(variables.storeId) });
    },
  });
}

export function useDeleteSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, supplierId }: { storeId: string; supplierId: number }) =>
      supplierApi.delete(storeId, supplierId),
    onSuccess: (_, variables) => {
      queryClient.removeQueries({
        queryKey: supplierKeys.detail(variables.storeId, variables.supplierId),
      });
      queryClient.invalidateQueries({ queryKey: supplierKeys.list(variables.storeId) });
    },
  });
}
