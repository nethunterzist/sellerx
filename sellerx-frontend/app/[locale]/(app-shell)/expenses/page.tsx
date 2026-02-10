"use client";

import { useState, useMemo } from "react";
import { format } from "date-fns";
import type { DateRange } from "react-day-picker";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useStoreExpenses } from "@/hooks/queries/use-expenses";
import { Button } from "@/components/ui/button";
import { ExpenseList } from "@/components/expenses/expense-list";
import { ExpenseFormModal } from "@/components/expenses/expense-form-modal";
import { ExpenseCategoryModal } from "@/components/expenses/expense-category-modal";
import { ExpenseStatsCards } from "@/components/expenses/expense-stats-cards";
import { ExpenseCategoryChart } from "@/components/expenses/expense-category-chart";
import { ExpenseTrendChart } from "@/components/expenses/expense-trend-chart";
import { ExpenseFiltersComponent, type ExpenseFilters } from "@/components/expenses/expense-filters";
import {
  ExpenseDateFilter,
  getExpensePresetRange,
  type ExpenseDatePreset,
} from "@/components/expenses/expense-date-filter";
import { Plus, Settings } from "lucide-react";
import type { StoreExpense } from "@/types/expense";
import {
  StatCardSkeleton,
  ChartSkeleton,
  FilterBarSkeleton,
  TableSkeleton,
  PaginationSkeleton,
} from "@/components/ui/skeleton-blocks";

const DEFAULT_FILTERS: ExpenseFilters = {
  category: "all",
  frequency: "all",
  search: "",
};

function ExpensesPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <StatCardSkeleton key={i} />
        ))}
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <ChartSkeleton />
        <ChartSkeleton />
      </div>
      <FilterBarSkeleton showSearch={true} buttonCount={2} />
      <TableSkeleton columns={6} rows={8} />
      <PaginationSkeleton />
    </div>
  );
}

export default function ExpensesPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [categoryModalOpen, setCategoryModalOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState<StoreExpense | null>(null);
  const [filters, setFilters] = useState<ExpenseFilters>(DEFAULT_FILTERS);

  // Date filter state - no default (show all expenses)
  const [dateRange, setDateRange] = useState<DateRange | undefined>(undefined);
  const [datePreset, setDatePreset] = useState<ExpenseDatePreset | undefined>(undefined);

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Convert date range to ISO format for API
  const { startDate, endDate } = useMemo(() => {
    if (!dateRange?.from || !dateRange?.to) return {};
    const start = new Date(dateRange.from);
    start.setHours(0, 0, 0, 0);
    const end = new Date(dateRange.to);
    end.setHours(23, 59, 59, 999);
    return {
      startDate: format(start, "yyyy-MM-dd'T'HH:mm:ss"),
      endDate: format(end, "yyyy-MM-dd'T'HH:mm:ss"),
    };
  }, [dateRange]);

  const { data, isLoading } = useStoreExpenses(storeId || undefined, startDate, endDate);

  const handleDateRangeChange = (
    range: DateRange | undefined,
    preset: ExpenseDatePreset | undefined
  ) => {
    setDateRange(range);
    setDatePreset(preset);
  };

  // Get unique categories from expenses
  const categories = useMemo(() => {
    if (!data?.expenses) return [];
    const uniqueCategories = new Set(
      data.expenses.map((e) => e.expenseCategoryName).filter(Boolean)
    );
    return Array.from(uniqueCategories).sort();
  }, [data?.expenses]);

  // Filter expenses based on filters
  const filteredExpenses = useMemo(() => {
    if (!data?.expenses) return [];

    return data.expenses.filter((expense) => {
      // Category filter
      if (filters.category !== "all" && expense.expenseCategoryName !== filters.category) {
        return false;
      }

      // Frequency filter
      if (filters.frequency !== "all" && expense.frequency !== filters.frequency) {
        return false;
      }

      // Search filter
      if (filters.search) {
        const searchLower = filters.search.toLowerCase();
        const nameMatch = expense.name?.toLowerCase().includes(searchLower);
        const categoryMatch = expense.expenseCategoryName?.toLowerCase().includes(searchLower);
        if (!nameMatch && !categoryMatch) {
          return false;
        }
      }

      return true;
    });
  }, [data?.expenses, filters]);

  const handleEdit = (expense: StoreExpense) => {
    setEditingExpense(expense);
    setModalOpen(true);
  };

  const handleOpenNew = () => {
    setEditingExpense(null);
    setModalOpen(true);
  };

  const handleCloseModal = (open: boolean) => {
    setModalOpen(open);
    if (!open) {
      setEditingExpense(null);
    }
  };

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  if (isLoading) return <ExpensesPageSkeleton />;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">
            Mağazanızın sabit ve değişken giderlerini yönetin
          </p>
        </div>

        <div className="flex items-center gap-3">
          <ExpenseDateFilter
            dateRange={dateRange}
            onDateRangeChange={handleDateRangeChange}
          />
          <Button onClick={handleOpenNew} className="gap-2">
            <Plus className="h-4 w-4" />
            Yeni Gider Ekle
          </Button>
          <Button
            variant="outline"
            size="icon"
            onClick={() => setCategoryModalOpen(true)}
            title="Gider Kategorileri"
          >
            <Settings className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      {data && (
        <ExpenseStatsCards
          expenses={data.expenses}
          totalExpense={data.totalExpense || 0}
        />
      )}

      {/* Charts Row - Two charts side by side */}
      {data && data.expenses.length > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Pie Chart - Category Distribution */}
          <ExpenseCategoryChart expenses={data.expenses} />

          {/* Bar Chart - Frequency Distribution */}
          <ExpenseTrendChart expenses={data.expenses} />
        </div>
      )}

      {/* Expense Table - Full Width */}
      <div className="bg-card rounded-lg border border-border">
        <div className="p-4 border-b border-border">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-4">
            <h2 className="font-semibold text-foreground">Gider Listesi</h2>
            {data && (
              <span className="text-sm text-muted-foreground">
                {filteredExpenses.length} / {data.expenses.length} gider
              </span>
            )}
          </div>
          {/* Filters inside table header */}
          {data && data.expenses.length > 0 && (
            <ExpenseFiltersComponent
              filters={filters}
              onFiltersChange={setFilters}
              categories={categories}
            />
          )}
        </div>
        <div className="p-4">
          <ExpenseList
            expenses={filteredExpenses}
            storeId={storeId || ""}
            isLoading={isLoading}
            onEdit={handleEdit}
          />
        </div>
      </div>

      {/* Form Modal */}
      {storeId && (
        <ExpenseFormModal
          storeId={storeId}
          expense={editingExpense}
          open={modalOpen}
          onOpenChange={handleCloseModal}
        />
      )}

      {/* Category Management Modal */}
      {storeId && (
        <ExpenseCategoryModal
          storeId={storeId}
          open={categoryModalOpen}
          onOpenChange={setCategoryModalOpen}
        />
      )}
    </div>
  );
}
