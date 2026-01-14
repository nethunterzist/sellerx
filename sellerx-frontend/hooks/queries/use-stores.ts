import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { storeApi } from "@/lib/api/client";

// Store Query Keys
export const storeKeys = {
  all: ["stores"] as const,
  lists: () => [...storeKeys.all, "list"] as const,
  list: (filters: Record<string, any>) =>
    [...storeKeys.lists(), { filters }] as const,
  details: () => [...storeKeys.all, "detail"] as const,
  detail: (id: string) => [...storeKeys.details(), id] as const,
  my: () => [...storeKeys.all, "my"] as const,
  selected: () => [...storeKeys.all, "selected"] as const,
};

// Get all stores
export function useStores() {
  return useQuery({
    queryKey: storeKeys.lists(),
    queryFn: storeApi.getAll,
  });
}

// Get my stores
export function useMyStores() {
  return useQuery({
    queryKey: storeKeys.my(),
    queryFn: storeApi.getMy,
  });
}

// Get store by ID
export function useStore(id: string) {
  return useQuery({
    queryKey: storeKeys.detail(id),
    queryFn: () => storeApi.getById(id),
    enabled: !!id,
  });
}

// Create store mutation
export function useCreateStore() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: storeApi.create,
    onSuccess: () => {
      // Invalidate and refetch stores
      queryClient.invalidateQueries({ queryKey: storeKeys.all });
    },
  });
}

// Update store mutation
export function useUpdateStore() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) =>
      storeApi.update(id, data),
    onSuccess: (data, variables) => {
      // Update the specific store in cache
      queryClient.setQueryData(storeKeys.detail(variables.id), data);
      // Invalidate lists
      queryClient.invalidateQueries({ queryKey: storeKeys.lists() });
      queryClient.invalidateQueries({ queryKey: storeKeys.my() });
    },
  });
}

// Delete store mutation
export function useDeleteStore() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: storeApi.delete,
    onSuccess: (data, id) => {
      // Remove from cache
      queryClient.removeQueries({ queryKey: storeKeys.detail(id) });
      // Invalidate lists
      queryClient.invalidateQueries({ queryKey: storeKeys.lists() });
      queryClient.invalidateQueries({ queryKey: storeKeys.my() });
      queryClient.invalidateQueries({ queryKey: storeKeys.selected() });
    },
  });
}

// Get selected store
export function useSelectedStore() {
  return useQuery({
    queryKey: storeKeys.selected(),
    queryFn: storeApi.getSelectedStore,
  });
}

// Set selected store mutation
export function useSetSelectedStore() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: storeApi.setSelectedStore,
    onSuccess: () => {
      // Invalidate selected store query
      queryClient.invalidateQueries({ queryKey: storeKeys.selected() });
    },
  });
}
