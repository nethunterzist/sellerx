"use client";

import { useState, useMemo } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Edit, Trash2, RefreshCw, Calendar, ArrowUpDown, ArrowUp, ArrowDown, Repeat } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { useDeleteExpense } from "@/hooks/queries/use-expenses";
import type { StoreExpense, ExpenseFrequency } from "@/types/expense";
import { frequencyLabels } from "@/types/expense";
import { cn } from "@/lib/utils";
import { useCurrency } from "@/lib/contexts/currency-context";

type SortField = "amount" | "date" | "category" | null;
type SortDirection = "asc" | "desc";

// Frequency badge colors
const frequencyColors: Record<ExpenseFrequency, { bg: string; text: string }> = {
  DAILY: { bg: "bg-red-100 dark:bg-red-900/30", text: "text-red-700 dark:text-red-300" },
  WEEKLY: { bg: "bg-orange-100 dark:bg-orange-900/30", text: "text-orange-700 dark:text-orange-300" },
  MONTHLY: { bg: "bg-blue-100 dark:bg-blue-900/30", text: "text-blue-700 dark:text-blue-300" },
  YEARLY: { bg: "bg-purple-100 dark:bg-purple-900/30", text: "text-purple-700 dark:text-purple-300" },
  ONE_TIME: { bg: "bg-gray-100 dark:bg-gray-800", text: "text-gray-700 dark:text-gray-300" },
};

interface ExpenseListProps {
  expenses: StoreExpense[];
  storeId: string;
  isLoading?: boolean;
  onEdit: (expense: StoreExpense) => void;
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("tr-TR");
}

function ExpenseRowSkeleton() {
  return (
    <TableRow>
      <TableCell>
        <Skeleton className="h-4 w-24" />
      </TableCell>
      <TableCell className="text-right">
        <Skeleton className="h-4 w-20 ml-auto" />
      </TableCell>
      <TableCell>
        <Skeleton className="h-4 w-16" />
      </TableCell>
      <TableCell>
        <Skeleton className="h-4 w-24" />
      </TableCell>
      <TableCell>
        <Skeleton className="h-4 w-32" />
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-2 justify-end">
          <Skeleton className="h-8 w-8" />
          <Skeleton className="h-8 w-8" />
        </div>
      </TableCell>
    </TableRow>
  );
}

export function ExpenseList({
  expenses,
  storeId,
  isLoading,
  onEdit,
}: ExpenseListProps) {
  const { formatCurrency } = useCurrency();
  const [deleteExpenseId, setDeleteExpenseId] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");
  const deleteMutation = useDeleteExpense();

  // Sorting logic
  const sortedExpenses = useMemo(() => {
    if (!sortField) return expenses;

    return [...expenses].sort((a, b) => {
      let comparison = 0;

      switch (sortField) {
        case "amount":
          comparison = a.amount - b.amount;
          break;
        case "date":
          comparison = new Date(a.date).getTime() - new Date(b.date).getTime();
          break;
        case "category":
          comparison = (a.expenseCategoryName || "").localeCompare(
            b.expenseCategoryName || ""
          );
          break;
      }

      return sortDirection === "asc" ? comparison : -comparison;
    });
  }, [expenses, sortField, sortDirection]);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDirection("desc");
    }
  };

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) {
      return <ArrowUpDown className="h-3 w-3 ml-1 opacity-50" />;
    }
    return sortDirection === "asc" ? (
      <ArrowUp className="h-3 w-3 ml-1" />
    ) : (
      <ArrowDown className="h-3 w-3 ml-1" />
    );
  };

  const handleDelete = () => {
    if (deleteExpenseId) {
      deleteMutation.mutate(
        { storeId, expenseId: deleteExpenseId },
        {
          onSuccess: () => {
            setDeleteExpenseId(null);
          },
        }
      );
    }
  };

  return (
    <>
      <div className="rounded-lg border border-border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead>
                <button
                  onClick={() => handleSort("category")}
                  className="flex items-center hover:text-foreground transition-colors"
                >
                  Kategori
                  <SortIcon field="category" />
                </button>
              </TableHead>
              <TableHead className="text-right">
                <button
                  onClick={() => handleSort("amount")}
                  className="flex items-center ml-auto hover:text-foreground transition-colors"
                >
                  Tutar (KDV Hariç)
                  <SortIcon field="amount" />
                </button>
              </TableHead>
              <TableHead>Sıklık</TableHead>
              <TableHead>
                <button
                  onClick={() => handleSort("date")}
                  className="flex items-center hover:text-foreground transition-colors"
                >
                  Başlangıç
                  <SortIcon field="date" />
                </button>
              </TableHead>
              <TableHead>Açıklama</TableHead>
              <TableHead className="w-[100px] text-right">İşlemler</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <>
                <ExpenseRowSkeleton />
                <ExpenseRowSkeleton />
                <ExpenseRowSkeleton />
              </>
            ) : sortedExpenses.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="h-24 text-center text-muted-foreground"
                >
                  Henüz gider eklenmemiş
                </TableCell>
              </TableRow>
            ) : (
              sortedExpenses.map((expense) => (
                <TableRow key={expense.id} className="hover:bg-muted">
                  <TableCell className="font-medium">
                    <div className="flex flex-col gap-1">
                      <div className="flex items-center gap-2">
                        {expense.expenseCategoryName || expense.name}
                        {expense.isRecurringTemplate && (
                          <Badge variant="outline" className="text-xs px-1.5 py-0 h-5 gap-1 border-blue-300 text-blue-600 dark:border-blue-600 dark:text-blue-400">
                            <Repeat className="h-3 w-3" />
                            Şablon
                          </Badge>
                        )}
                      </div>
                      {expense.parentExpenseId && (
                        <span className="text-xs text-muted-foreground italic">
                          (Otomatik oluşturuldu)
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="font-medium">{formatCurrency(expense.amount)}</div>
                    {expense.vatRate != null ? (
                      <div className="text-xs text-muted-foreground">
                        KDV %{expense.vatRate}: {formatCurrency(expense.vatAmount ?? 0)}
                      </div>
                    ) : (
                      <div className="text-xs text-muted-foreground">Faturasız</div>
                    )}
                  </TableCell>
                  <TableCell>
                    <span className={cn(
                      "inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium",
                      frequencyColors[expense.frequency].bg,
                      frequencyColors[expense.frequency].text
                    )}>
                      {expense.frequency === "ONE_TIME" ? (
                        <Calendar className="h-3 w-3" />
                      ) : (
                        <RefreshCw className="h-3 w-3" />
                      )}
                      {expense.frequencyDisplayName || frequencyLabels[expense.frequency]}
                    </span>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    <div className="flex flex-col gap-0.5">
                      <span>{formatDate(expense.date?.split("T")[0] || "")}</span>
                      {expense.endDate && (
                        <span className="text-xs">
                          → {formatDate(expense.endDate.split("T")[0])}
                        </span>
                      )}
                      {expense.isRecurringTemplate && !expense.endDate && (
                        <span className="text-xs text-blue-500">Süresiz</span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground max-w-[200px] truncate">
                    {expense.name || "-"}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1 justify-end">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-blue-600 hover:text-blue-800"
                        onClick={() => onEdit(expense)}
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-red-600 hover:text-red-800"
                        onClick={() => setDeleteExpenseId(expense.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <AlertDialog
        open={!!deleteExpenseId}
        onOpenChange={(open) => !open && setDeleteExpenseId(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Gideri Sil</AlertDialogTitle>
            <AlertDialogDescription>
              Bu gideri silmek istediğinizden emin misiniz? Bu işlem geri
              alınamaz.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>
              İptal
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              className="bg-red-600 hover:bg-red-700"
            >
              {deleteMutation.isPending ? "Siliniyor..." : "Sil"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
