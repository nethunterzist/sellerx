"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useSuppliers } from "@/hooks/queries/use-suppliers";
import {
  usePurchaseOrder,
  useUpdatePurchaseOrder,
  useUpdatePurchaseOrderStatus,
  useAddPurchaseOrderItem,
  useUpdatePurchaseOrderItem,
  useRemovePurchaseOrderItem,
  useDeletePurchaseOrder,
  useDuplicatePurchaseOrder,
} from "@/hooks/queries/use-purchasing";
import { POStatusFlow } from "@/components/purchasing/po-status-flow";
import { POForm } from "@/components/purchasing/po-form";
import { POItemsTable } from "@/components/purchasing/po-items-table";
import { AddProductModal } from "@/components/purchasing/add-product-modal";
import { POAttachmentsTab } from "@/components/purchasing/po-attachments-tab";
import { SplitPOModal } from "@/components/purchasing/split-po-modal";
import { ImportItemsModal } from "@/components/purchasing/import-items-modal";
import { Button } from "@/components/ui/button";
import {
  ArrowLeft,
  Plus,
  Loader2,
  Trash2,
  Copy,
  Scissors,
  Download,
  Upload,
  Paperclip,
} from "lucide-react";
import { useCurrency } from "@/lib/contexts/currency-context";
import type {
  PurchaseOrderStatus,
  UpdatePurchaseOrderRequest,
  AddPurchaseOrderItemRequest,
  PurchaseOrderItem,
} from "@/types/purchasing";
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

interface PageProps {
  params: Promise<{ poId: string }>;
}

export default function PurchaseOrderDetailPage({ params }: PageProps) {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
  const [poId, setPoId] = useState<string | null>(null);
  const [addProductOpen, setAddProductOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<PurchaseOrderItem | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [splitModalOpen, setSplitModalOpen] = useState(false);
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [showAttachments, setShowAttachments] = useState(false);

  // Resolve params
  useEffect(() => {
    params.then((p) => setPoId(p.poId));
  }, [params]);

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: purchaseOrder, isLoading: poLoading } = usePurchaseOrder(
    storeId || undefined,
    poId ? parseInt(poId) : undefined
  );

  const { data: suppliers } = useSuppliers(storeId || undefined);

  const updateMutation = useUpdatePurchaseOrder();
  const statusMutation = useUpdatePurchaseOrderStatus();
  const addItemMutation = useAddPurchaseOrderItem();
  const updateItemMutation = useUpdatePurchaseOrderItem();
  const removeItemMutation = useRemovePurchaseOrderItem();
  const deleteMutation = useDeletePurchaseOrder();
  const duplicateMutation = useDuplicatePurchaseOrder();

  const handleSave = (data: UpdatePurchaseOrderRequest) => {
    if (!storeId || !poId) return;
    updateMutation.mutate({
      storeId,
      poId: parseInt(poId),
      data,
    });
  };

  const handleStatusChange = (newStatus: PurchaseOrderStatus) => {
    if (!storeId || !poId) return;
    statusMutation.mutate({
      storeId,
      poId: parseInt(poId),
      status: newStatus,
    });
  };

  const handleAddItem = (data: AddPurchaseOrderItemRequest) => {
    if (!storeId || !poId) return;

    if (editingItem) {
      updateItemMutation.mutate(
        {
          storeId,
          poId: parseInt(poId),
          itemId: editingItem.id,
          data,
        },
        {
          onSuccess: () => {
            setAddProductOpen(false);
            setEditingItem(null);
          },
        }
      );
    } else {
      addItemMutation.mutate(
        {
          storeId,
          poId: parseInt(poId),
          data,
        },
        {
          onSuccess: () => {
            setAddProductOpen(false);
          },
        }
      );
    }
  };

  const handleEditItem = (item: PurchaseOrderItem) => {
    setEditingItem(item);
    setAddProductOpen(true);
  };

  const handleRemoveItem = (itemId: number) => {
    if (!storeId || !poId) return;
    removeItemMutation.mutate({
      storeId,
      poId: parseInt(poId),
      itemId,
    });
  };

  const handleDelete = () => {
    if (!storeId || !poId) return;
    deleteMutation.mutate(
      { storeId, poId: parseInt(poId) },
      {
        onSuccess: () => {
          router.push("/purchasing");
        },
      }
    );
  };

  const handleDuplicate = () => {
    if (!storeId || !poId) return;
    duplicateMutation.mutate(
      { storeId, poId: parseInt(poId) },
      {
        onSuccess: (newPO) => {
          if (newPO?.id) {
            router.push(`/purchasing/${newPO.id}`);
          }
        },
      }
    );
  };

  const handleExport = () => {
    if (!storeId || !poId) return;
    window.open(
      `/api/purchasing/orders/${storeId}/${poId}/export`,
      "_blank"
    );
  };

  const handleModalClose = (open: boolean) => {
    if (!open) {
      setEditingItem(null);
    }
    setAddProductOpen(open);
  };

  if (!poId || storeLoading || poLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!storeId) {
    return (
      <div className="p-8">
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  if (!purchaseOrder) {
    return (
      <div className="p-8">
        <p className="text-muted-foreground">Sipariş bulunamadı.</p>
      </div>
    );
  }

  const isEditable = purchaseOrder.status !== "CLOSED";

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.push("/purchasing")}
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-foreground">
              {purchaseOrder.poNumber}
            </h1>
            <p className="text-sm text-muted-foreground">
              {purchaseOrder.supplierName || "Tedarikçi belirtilmemiş"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Export */}
          <Button
            variant="outline"
            size="sm"
            onClick={handleExport}
            className="gap-2"
          >
            <Download className="h-4 w-4" />
            Dışa Aktar
          </Button>

          {/* Import */}
          {isEditable && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setImportModalOpen(true)}
              className="gap-2"
            >
              <Upload className="h-4 w-4" />
              İçe Aktar
            </Button>
          )}

          {/* Duplicate */}
          <Button
            variant="outline"
            size="sm"
            onClick={handleDuplicate}
            disabled={duplicateMutation.isPending}
            className="gap-2"
          >
            {duplicateMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Copy className="h-4 w-4" />
            )}
            Kopyala
          </Button>

          {/* Split */}
          {isEditable && purchaseOrder.items.length > 1 && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setSplitModalOpen(true)}
              className="gap-2"
            >
              <Scissors className="h-4 w-4" />
              Böl
            </Button>
          )}

          {/* Delete */}
          {purchaseOrder.status === "DRAFT" && (
            <Button
              variant="destructive"
              size="sm"
              onClick={() => setDeleteDialogOpen(true)}
              className="gap-2"
            >
              <Trash2 className="h-4 w-4" />
              Sil
            </Button>
          )}
        </div>
      </div>

      {/* Status Flow */}
      <POStatusFlow
        currentStatus={purchaseOrder.status}
        onStatusChange={handleStatusChange}
        isUpdating={statusMutation.isPending}
      />

      {/* Form */}
      <POForm
        purchaseOrder={purchaseOrder}
        onSave={handleSave}
        isSaving={updateMutation.isPending}
        suppliers={suppliers}
      />

      {/* Summary Card */}
      <div className="bg-card rounded-lg border border-border p-4">
        <div className="grid grid-cols-3 gap-4 text-center">
          <div>
            <p className="text-2xl font-bold text-foreground">
              {purchaseOrder.totalUnits.toLocaleString()}
            </p>
            <p className="text-sm text-muted-foreground">Toplam Adet</p>
          </div>
          <div>
            <p className="text-2xl font-bold text-foreground">
              {purchaseOrder.itemCount}
            </p>
            <p className="text-sm text-muted-foreground">Ürün Çeşidi</p>
          </div>
          <div>
            <p className="text-2xl font-bold text-primary">
              {formatCurrency(purchaseOrder.totalCost)}
            </p>
            <p className="text-sm text-muted-foreground">Toplam Maliyet</p>
          </div>
        </div>
      </div>

      {/* Items Section */}
      <div className="bg-card rounded-lg border border-border">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <h2 className="font-semibold text-foreground">Ürünler</h2>
          {isEditable && (
            <Button
              onClick={() => setAddProductOpen(true)}
              size="sm"
              className="gap-2"
            >
              <Plus className="h-4 w-4" />
              Ürün Ekle
            </Button>
          )}
        </div>
        <div className="p-4">
          <POItemsTable
            items={purchaseOrder.items}
            onEdit={handleEditItem}
            onRemove={handleRemoveItem}
            isRemoving={removeItemMutation.isPending}
            disabled={!isEditable}
            supplierCurrency={purchaseOrder.supplierCurrency}
          />
        </div>
      </div>

      {/* Attachments Section */}
      <div className="bg-card rounded-lg border border-border">
        <button
          className="w-full p-4 border-b border-border flex items-center justify-between text-left"
          onClick={() => setShowAttachments(!showAttachments)}
        >
          <h2 className="font-semibold text-foreground flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Ekler
          </h2>
          <span className="text-sm text-muted-foreground">
            {showAttachments ? "Gizle" : "Göster"}
          </span>
        </button>
        {showAttachments && (
          <div className="p-4">
            <POAttachmentsTab
              storeId={storeId}
              poId={parseInt(poId)}
              disabled={!isEditable}
            />
          </div>
        )}
      </div>

      {/* Add Product Modal */}
      <AddProductModal
        storeId={storeId}
        open={addProductOpen}
        onOpenChange={handleModalClose}
        onAdd={handleAddItem}
        isAdding={addItemMutation.isPending || updateItemMutation.isPending}
        editingItem={editingItem}
        supplierCurrency={purchaseOrder.supplierCurrency}
      />

      {/* Split PO Modal */}
      <SplitPOModal
        storeId={storeId}
        poId={parseInt(poId)}
        items={purchaseOrder.items}
        open={splitModalOpen}
        onOpenChange={setSplitModalOpen}
      />

      {/* Import Items Modal */}
      <ImportItemsModal
        storeId={storeId}
        poId={parseInt(poId)}
        open={importModalOpen}
        onOpenChange={setImportModalOpen}
      />

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Siparişi Sil</AlertDialogTitle>
            <AlertDialogDescription>
              {purchaseOrder.poNumber} numaralı siparişi silmek istediğinizden emin misiniz?
              Bu işlem geri alınamaz.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>İptal</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                "Sil"
              )}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
