"use client";

import { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useStoreExpenses } from "@/hooks/queries/use-expenses";
import { Button } from "@/components/ui/button";
import { ExpenseList } from "@/components/expenses/expense-list";
import { ExpenseFormModal } from "@/components/expenses/expense-form-modal";
import { Plus, Receipt, TrendingDown } from "lucide-react";
import type { StoreExpense } from "@/types/expense";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export default function ExpensesPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState<StoreExpense | null>(
    null
  );
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data, isLoading } = useStoreExpenses(storeId || undefined);

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
        <h1 className="text-2xl font-bold mb-4">Giderler</h1>
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Giderler</h1>
          <p className="text-sm text-gray-500 mt-1">
            Mağazanızın sabit ve değişken giderlerini yönetin
          </p>
        </div>

        <Button onClick={handleOpenNew} className="gap-2">
          <Plus className="h-4 w-4" />
          Yeni Gider Ekle
        </Button>
      </div>

      {/* Stats Card */}
      {data && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-red-100 flex items-center justify-center">
                <TrendingDown className="h-5 w-5 text-red-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Aylık Toplam Gider</p>
                <p className="text-xl font-bold text-gray-900">
                  {formatCurrency(data.totalMonthlyAmount)} TL
                </p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                <Receipt className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Toplam Gider Kalemi</p>
                <p className="text-xl font-bold text-gray-900">
                  {data.expenses.length}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Expense List */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="p-4 border-b border-gray-200">
          <h2 className="font-semibold text-gray-900">Gider Listesi</h2>
        </div>
        <div className="p-4">
          <ExpenseList
            expenses={data?.expenses || []}
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
    </div>
  );
}
