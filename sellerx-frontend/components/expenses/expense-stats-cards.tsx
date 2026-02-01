"use client";

import { TrendingDown, RefreshCw, Calendar, Tag } from "lucide-react";
import type { StoreExpense } from "@/types/expense";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ExpenseStatsCardsProps {
  expenses: StoreExpense[];
  totalExpense: number;
}

interface StatCardProps {
  icon: React.ReactNode;
  iconBgColor: string;
  title: string;
  value: string;
  subtitle?: string;
}

function StatCard({ icon, iconBgColor, title, value, subtitle }: StatCardProps) {
  return (
    <div className="bg-card rounded-lg border border-border p-4">
      <div className="flex items-center gap-3">
        <div className={`h-10 w-10 rounded-full ${iconBgColor} flex items-center justify-center`}>
          {icon}
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm text-muted-foreground truncate">{title}</p>
          <p className="text-xl font-bold text-foreground truncate">{value}</p>
          {subtitle && (
            <p className="text-xs text-muted-foreground truncate">{subtitle}</p>
          )}
        </div>
      </div>
    </div>
  );
}

export function ExpenseStatsCards({ expenses, totalExpense }: ExpenseStatsCardsProps) {
  const { formatCurrency } = useCurrency();

  // Calculate recurring expenses (DAILY, WEEKLY, MONTHLY, YEARLY)
  const recurringExpenses = expenses.filter(
    (e) => e.frequency !== "ONE_TIME"
  );
  const recurringTotal = recurringExpenses.reduce(
    (sum, e) => sum + e.amount,
    0
  );

  // Calculate one-time expenses
  const oneTimeExpenses = expenses.filter(
    (e) => e.frequency === "ONE_TIME"
  );
  const oneTimeTotal = oneTimeExpenses.reduce(
    (sum, e) => sum + e.amount,
    0
  );

  // Find top category by total amount
  const categoryTotals: Record<string, number> = {};
  expenses.forEach((e) => {
    const category = e.expenseCategoryName || "Diğer";
    categoryTotals[category] = (categoryTotals[category] || 0) + e.amount;
  });

  const topCategory = Object.entries(categoryTotals).sort(
    (a, b) => b[1] - a[1]
  )[0];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <StatCard
        icon={<TrendingDown className="h-5 w-5 text-red-600 dark:text-red-400" />}
        iconBgColor="bg-red-100 dark:bg-red-900/30"
        title="Aylık Toplam"
        value={formatCurrency(totalExpense)}
        subtitle={`${expenses.length} gider kalemi`}
      />

      <StatCard
        icon={<RefreshCw className="h-5 w-5 text-blue-600 dark:text-blue-400" />}
        iconBgColor="bg-blue-100 dark:bg-blue-900/30"
        title="Tekrarlayan"
        value={formatCurrency(recurringTotal)}
        subtitle={`${recurringExpenses.length} kalem`}
      />

      <StatCard
        icon={<Calendar className="h-5 w-5 text-purple-600 dark:text-purple-400" />}
        iconBgColor="bg-purple-100 dark:bg-purple-900/30"
        title="Tek Seferlik"
        value={formatCurrency(oneTimeTotal)}
        subtitle={`${oneTimeExpenses.length} kalem`}
      />

      <StatCard
        icon={<Tag className="h-5 w-5 text-orange-600 dark:text-orange-400" />}
        iconBgColor="bg-orange-100 dark:bg-orange-900/30"
        title="En Yüksek Kategori"
        value={topCategory ? topCategory[0] : "-"}
        subtitle={topCategory ? formatCurrency(topCategory[1]) : undefined}
      />
    </div>
  );
}
