"use client";

import { useState } from "react";
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
import { Edit, Trash2 } from "lucide-react";
import { useDeleteExpense } from "@/hooks/queries/use-expenses";
import type { StoreExpense } from "@/types/expense";
import { frequencyLabels } from "@/types/expense";

interface ExpenseListProps {
  expenses: StoreExpense[];
  storeId: string;
  isLoading?: boolean;
  onEdit: (expense: StoreExpense) => void;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
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
  const [deleteExpenseId, setDeleteExpenseId] = useState<number | null>(null);
  const deleteMutation = useDeleteExpense();

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
      <div className="rounded-lg border border-gray-200 overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead>Kategori</TableHead>
              <TableHead className="text-right">Tutar</TableHead>
              <TableHead>Sıklık</TableHead>
              <TableHead>Başlangıç</TableHead>
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
            ) : expenses.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="h-24 text-center text-gray-500"
                >
                  Henüz gider eklenmemiş
                </TableCell>
              </TableRow>
            ) : (
              expenses.map((expense) => (
                <TableRow key={expense.id} className="hover:bg-gray-50">
                  <TableCell className="font-medium">
                    {expense.category.name}
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {formatCurrency(expense.amount)} TL
                  </TableCell>
                  <TableCell>
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                      {frequencyLabels[expense.frequency]}
                    </span>
                  </TableCell>
                  <TableCell className="text-gray-500">
                    {formatDate(expense.startDate)}
                    {expense.endDate && ` - ${formatDate(expense.endDate)}`}
                  </TableCell>
                  <TableCell className="text-gray-500 max-w-[200px] truncate">
                    {expense.description || "-"}
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
