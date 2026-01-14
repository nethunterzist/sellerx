// Expense types matching backend DTOs

export type ExpenseFrequency = "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY" | "ONE_TIME";

// Expense category from backend
export interface ExpenseCategory {
  id: number;
  name: string;
}

// Backend: StoreExpense entity
export interface StoreExpense {
  id: number;
  storeId: string;
  category: ExpenseCategory;
  amount: number;
  frequency: ExpenseFrequency;
  description?: string;
  startDate: string; // ISO date string
  endDate?: string; // ISO date string, optional
  createdAt: string; // ISO datetime
  updatedAt: string; // ISO datetime
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

// Request types
export interface CreateExpenseRequest {
  categoryId: number;
  amount: number;
  frequency: ExpenseFrequency;
  description?: string;
  startDate: string;
  endDate?: string;
}

export interface UpdateExpenseRequest {
  categoryId: number;
  amount: number;
  frequency: ExpenseFrequency;
  description?: string;
  startDate: string;
  endDate?: string;
}

// Response types
export interface StoreExpensesResponse {
  expenses: StoreExpense[];
  totalMonthlyAmount: number;
}

// Frequency labels for UI
export const frequencyLabels: Record<ExpenseFrequency, string> = {
  DAILY: "Günlük",
  WEEKLY: "Haftalık",
  MONTHLY: "Aylık",
  YEARLY: "Yıllık",
  ONE_TIME: "Tek Seferlik",
};
