import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/api/client";
import type {
  ExpenseCategory,
  StoreExpense,
  StoreExpensesResponse,
  CreateExpenseRequest,
  UpdateExpenseRequest,
} from "@/types/expense";

// Expense Query Keys
export const expenseKeys = {
  all: ["expenses"] as const,
  categories: () => [...expenseKeys.all, "categories"] as const,
  storeCategories: (storeId: string) => [...expenseKeys.all, "categories", "store", storeId] as const,
  byStore: (storeId: string) => [...expenseKeys.all, "store", storeId] as const,
  byStoreAndDate: (storeId: string, startDate?: string, endDate?: string) =>
    [...expenseKeys.all, "store", storeId, { startDate, endDate }] as const,
};

// Get expense categories
export function useExpenseCategories() {
  return useQuery({
    queryKey: expenseKeys.categories(),
    queryFn: () => apiRequest<ExpenseCategory[]>("/expenses/categories"),
  });
}

// Get store expenses with optional date filtering
export function useStoreExpenses(
  storeId: string | undefined,
  startDate?: string,
  endDate?: string
) {
  return useQuery({
    queryKey: expenseKeys.byStoreAndDate(storeId || "", startDate, endDate),
    queryFn: () => {
      let url = `/expenses/store/${storeId}`;
      if (startDate && endDate) {
        url += `?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`;
      }
      return apiRequest<StoreExpensesResponse>(url);
    },
    enabled: !!storeId,
  });
}

// Create expense mutation
export function useCreateExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      data,
    }: {
      storeId: string;
      data: CreateExpenseRequest;
    }) =>
      apiRequest<StoreExpense>(`/expenses/store/${storeId}`, {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: expenseKeys.byStore(storeId) });
    },
  });
}

// Update expense mutation
export function useUpdateExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      expenseId,
      data,
    }: {
      storeId: string;
      expenseId: string;
      data: UpdateExpenseRequest;
    }) =>
      apiRequest<StoreExpense>(`/expenses/store/${storeId}/${expenseId}`, {
        method: "PUT",
        body: JSON.stringify(data),
      }),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: expenseKeys.byStore(storeId) });
    },
  });
}

// Delete expense mutation
export function useDeleteExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      expenseId,
    }: {
      storeId: string;
      expenseId: string;
    }) =>
      apiRequest<{ success: boolean }>(
        `/expenses/store/${storeId}/${expenseId}`,
        {
          method: "DELETE",
        }
      ),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: expenseKeys.byStore(storeId) });
    },
  });
}

// ==================== Store-Specific Category Hooks ====================

// Get expense categories for a specific store
export function useStoreExpenseCategories(storeId: string | undefined) {
  return useQuery({
    queryKey: expenseKeys.storeCategories(storeId || ""),
    queryFn: () => apiRequest<ExpenseCategory[]>(`/expenses/store/${storeId}/categories`),
    enabled: !!storeId,
  });
}

// Create expense category mutation
export function useCreateExpenseCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      name,
    }: {
      storeId: string;
      name: string;
    }) =>
      apiRequest<ExpenseCategory>(`/expenses/store/${storeId}/categories`, {
        method: "POST",
        body: JSON.stringify({ name }),
      }),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: expenseKeys.storeCategories(storeId) });
      queryClient.invalidateQueries({ queryKey: expenseKeys.categories() });
    },
  });
}

// Update expense category mutation
export function useUpdateExpenseCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      categoryId,
      name,
    }: {
      storeId: string;
      categoryId: string;
      name: string;
    }) =>
      apiRequest<ExpenseCategory>(`/expenses/store/${storeId}/categories/${categoryId}`, {
        method: "PUT",
        body: JSON.stringify({ name }),
      }),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: expenseKeys.storeCategories(storeId) });
      queryClient.invalidateQueries({ queryKey: expenseKeys.categories() });
      queryClient.invalidateQueries({ queryKey: expenseKeys.byStore(storeId) });
    },
  });
}

// Delete expense category mutation
export function useDeleteExpenseCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      categoryId,
    }: {
      storeId: string;
      categoryId: string;
    }) =>
      apiRequest<void>(`/expenses/store/${storeId}/categories/${categoryId}`, {
        method: "DELETE",
      }),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: expenseKeys.storeCategories(storeId) });
      queryClient.invalidateQueries({ queryKey: expenseKeys.categories() });
    },
  });
}
