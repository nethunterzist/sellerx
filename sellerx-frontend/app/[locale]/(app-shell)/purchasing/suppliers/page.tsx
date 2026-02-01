"use client";

import { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  useSuppliers,
  useCreateSupplier,
  useUpdateSupplier,
  useDeleteSupplier,
} from "@/hooks/queries/use-suppliers";
import { SupplierListTable } from "@/components/purchasing/supplier-list-table";
import { SupplierFormModal } from "@/components/purchasing/supplier-form-modal";
import { Button } from "@/components/ui/button";
import { Plus, Users, Loader2 } from "lucide-react";
import type { Supplier, CreateSupplierRequest, UpdateSupplierRequest } from "@/types/supplier";

export default function SuppliersPage() {
  const [formOpen, setFormOpen] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: suppliers, isLoading: suppliersLoading } = useSuppliers(
    storeId || undefined
  );
  const createMutation = useCreateSupplier();
  const updateMutation = useUpdateSupplier();
  const deleteMutation = useDeleteSupplier();

  const handleSubmit = (data: CreateSupplierRequest | UpdateSupplierRequest) => {
    if (!storeId) return;

    if (editingSupplier) {
      updateMutation.mutate(
        {
          storeId,
          supplierId: editingSupplier.id,
          data: data as UpdateSupplierRequest,
        },
        {
          onSuccess: () => {
            setFormOpen(false);
            setEditingSupplier(null);
          },
        }
      );
    } else {
      createMutation.mutate(
        { storeId, data: data as CreateSupplierRequest },
        {
          onSuccess: () => {
            setFormOpen(false);
          },
        }
      );
    }
  };

  const handleEdit = (supplier: Supplier) => {
    setEditingSupplier(supplier);
    setFormOpen(true);
  };

  const handleDelete = (supplierId: number) => {
    if (!storeId) return;
    deleteMutation.mutate({ storeId, supplierId });
  };

  const handleModalClose = (open: boolean) => {
    if (!open) {
      setEditingSupplier(null);
    }
    setFormOpen(open);
  };

  if (storeLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!storeId) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <Users className="h-16 w-16 text-muted-foreground/30 mb-4" />
        <h2 className="text-lg font-medium text-foreground mb-2">
          Mağaza Seçilmedi
        </h2>
        <p className="text-sm text-muted-foreground max-w-md">
          Tedarikçileri görüntülemek için lütfen bir mağaza seçin.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Tedarikçiler</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Tedarikçilerinizi yönetin ve satın alma siparişlerinizle ilişkilendirin.
          </p>
        </div>
        <Button onClick={() => setFormOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          Yeni Tedarikçi
        </Button>
      </div>

      {/* Table */}
      <div className="bg-card rounded-xl border border-border">
        <div className="p-4">
          <SupplierListTable
            suppliers={suppliers || []}
            isLoading={suppliersLoading}
            onEdit={handleEdit}
            onDelete={handleDelete}
            isDeleting={deleteMutation.isPending}
          />
        </div>
      </div>

      {/* Form Modal */}
      <SupplierFormModal
        open={formOpen}
        onOpenChange={handleModalClose}
        onSubmit={handleSubmit}
        isSubmitting={createMutation.isPending || updateMutation.isPending}
        supplier={editingSupplier}
      />
    </div>
  );
}
