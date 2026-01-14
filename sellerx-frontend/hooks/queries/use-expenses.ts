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
  byStore: (storeId: string) => [...expenseKeys.all, "store", storeId] as const,
};

// Get expense categories
export function useExpenseCategories() {
  return useQuery({
    queryKey: expenseKeys.categories(),
    queryFn: () => apiRequest<ExpenseCategory[]>("/expenses/categories"),
  });
}

// Get store expenses
export function useStoreExpenses(storeId: string | undefined) {
  return useQuery({
    queryKey: expenseKeys.byStore(storeId || ""),
    queryFn: () =>
      apiRequest<StoreExpensesResponse>(`/expenses/store/${storeId}`),
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
      expenseId: number;
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
      expenseId: number;
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
