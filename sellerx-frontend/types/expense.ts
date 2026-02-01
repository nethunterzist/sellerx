// Expense types matching backend DTOs

export type ExpenseFrequency = "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY" | "ONE_TIME";

// Expense category from backend
export interface ExpenseCategory {
  id: string;  // UUID
  name: string;
  createdAt?: string;
  updatedAt?: string;
}

// Backend: StoreExpenseDto
export interface StoreExpense {
  id: string;  // UUID
  storeId: string;
  expenseCategoryId: string;
  expenseCategoryName: string;
  productId?: string;
  productTitle: string;  // "Genel" if productId is null
  date: string;  // ISO datetime
  frequency: ExpenseFrequency;
  frequencyDisplayName: string;
  name: string;
  amount: number;
  vatRate?: number | null;       // null = faturasız işlem
  vatAmount?: number | null;
  isVatDeductible?: boolean;
  netAmount?: number | null;
  createdAt: string;
  updatedAt: string;
  // Legacy compatibility - map to expected structure
  category: ExpenseCategory;
  startDate: string;
  description?: string;
  endDate?: string;
}

// Legacy Expense type (for backward compatibility)
export interface Expense {
  id: string;
  storeId: string;
  name: string;
  amount: number;
  frequency: ExpenseFrequency;
  startDate: string;
  endDate?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

// Request types - matches backend CreateStoreExpenseRequest
export interface CreateExpenseRequest {
  expenseCategoryId: string;  // UUID
  name: string;
  amount: number;
  frequency: ExpenseFrequency;
  date?: string;  // ISO datetime, optional (defaults to now)
  productId?: string | null;  // UUID, null for general expense
  vatRate?: number | null;  // null = faturasız işlem
}

// Request types - matches backend UpdateStoreExpenseRequest
export interface UpdateExpenseRequest {
  expenseCategoryId: string;  // UUID
  name: string;
  amount: number;
  frequency: ExpenseFrequency;
  date: string;  // ISO datetime
  productId?: string | null;  // UUID, null for general expense
  vatRate?: number | null;  // null = faturasız işlem
}

// Response types - matches backend StoreExpensesResponse
export interface StoreExpensesResponse {
  expenses: StoreExpense[];
  totalExpense: number;  // Backend returns totalExpense (BigDecimal)
  totalMonthlyAmount?: number;  // Total monthly amount for dashboard calculations
}

// Frequency labels for UI
export const frequencyLabels: Record<ExpenseFrequency, string> = {
  DAILY: "Günlük",
  WEEKLY: "Haftalık",
  MONTHLY: "Aylık",
  YEARLY: "Yıllık",
  ONE_TIME: "Tek Seferlik",
};
