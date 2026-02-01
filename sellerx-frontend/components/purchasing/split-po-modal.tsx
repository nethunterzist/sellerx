"use client";

import { useState } from "react";
import type { PurchaseOrderItem } from "@/types/purchasing";
import { useSplitPurchaseOrder } from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Loader2, Scissors, ImageIcon } from "lucide-react";
import Image from "next/image";
import { useRouter } from "next/navigation";

interface SplitPOModalProps {
  storeId: string;
  poId: number;
  items: PurchaseOrderItem[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function SplitPOModal({
  storeId,
  poId,
  items,
  open,
  onOpenChange,
}: SplitPOModalProps) {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const splitMutation = useSplitPurchaseOrder();

  const toggleItem = (itemId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(itemId)) {
        next.delete(itemId);
      } else {
        next.add(itemId);
      }
      return next;
    });
  };

  const toggleAll = () => {
    if (selectedIds.size === items.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(items.map((i) => i.id)));
    }
  };

  const selectedItems = items.filter((i) => selectedIds.has(i.id));
  const selectedTotal = selectedItems.reduce((s, i) => s + i.totalCost, 0);
  const selectedUnits = selectedItems.reduce((s, i) => s + i.unitsOrdered, 0);

  const handleSplit = () => {
    if (selectedIds.size === 0 || selectedIds.size === items.length) return;

    splitMutation.mutate(
      { storeId, poId, itemIds: Array.from(selectedIds) },
      {
        onSuccess: (newPO) => {
          onOpenChange(false);
          setSelectedIds(new Set());
          if (newPO?.id) {
            router.push(`/purchasing/${newPO.id}`);
          }
        },
      }
    );
  };

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      setSelectedIds(new Set());
    }
    onOpenChange(isOpen);
  };

  const canSplit =
    selectedIds.size > 0 && selectedIds.size < items.length;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Scissors className="h-5 w-5" />
            Siparişi Böl
          </DialogTitle>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          Yeni siparişe taşımak istediğiniz ürünleri seçin. Seçilen ürünler
          mevcut siparişten kaldırılıp yeni bir siparişe aktarılacaktır.
        </p>

        {/* Select All */}
        <div className="flex items-center gap-2 border-b border-border pb-2">
          <Checkbox
            checked={
              selectedIds.size === items.length
                ? true
                : selectedIds.size > 0
                ? "indeterminate"
                : false
            }
            onCheckedChange={toggleAll}
          />
          <span className="text-sm font-medium">Tümünü Seç</span>
          <span className="text-xs text-muted-foreground ml-auto">
            {selectedIds.size} / {items.length} seçili
          </span>
        </div>

        {/* Item List */}
        <div className="flex-1 overflow-y-auto divide-y divide-border">
          {items.map((item) => (
            <label
              key={item.id}
              className="flex items-center gap-3 p-3 cursor-pointer hover:bg-muted/50 transition-colors"
            >
              <Checkbox
                checked={selectedIds.has(item.id)}
                onCheckedChange={() => toggleItem(item.id)}
              />
              {item.productImage ? (
                <Image
                  src={item.productImage}
                  alt={item.productName}
                  width={36}
                  height={36}
                  className="rounded object-cover"
                />
              ) : (
                <div className="w-9 h-9 bg-muted rounded flex items-center justify-center">
                  <ImageIcon className="h-4 w-4 text-muted-foreground" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium line-clamp-1">
                  {item.productName}
                </p>
                <p className="text-xs text-muted-foreground">
                  {item.unitsOrdered} adet &middot;{" "}
                  {formatCurrency(item.totalCost)}
                </p>
              </div>
            </label>
          ))}
        </div>

        {/* Summary */}
        {selectedIds.size > 0 && (
          <div className="bg-muted/50 rounded-lg p-3 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Seçili Ürünler</span>
              <span className="font-medium">{selectedIds.size} çeşit</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Toplam Adet</span>
              <span className="font-medium">
                {selectedUnits.toLocaleString()}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Toplam Maliyet</span>
              <span className="font-medium">{formatCurrency(selectedTotal)}</span>
            </div>
          </div>
        )}

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => handleOpenChange(false)}
          >
            İptal
          </Button>
          <Button
            onClick={handleSplit}
            disabled={!canSplit || splitMutation.isPending}
            className="gap-2"
          >
            {splitMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Scissors className="h-4 w-4" />
            )}
            Böl ve Yeni Sipariş Oluştur
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
