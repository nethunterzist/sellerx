"use client";

import { useState } from "react";
import type { PurchaseOrderItem } from "@/types/purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Trash2, Edit2, Loader2, ImageIcon } from "lucide-react";
import Image from "next/image";
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

interface POItemsTableProps {
  items: PurchaseOrderItem[];
  onEdit: (item: PurchaseOrderItem) => void;
  onRemove: (itemId: number) => void;
  isRemoving: boolean;
  disabled?: boolean;
  supplierCurrency?: string;
}

export function POItemsTable({
  items,
  onEdit,
  onRemove,
  isRemoving,
  disabled,
  supplierCurrency,
}: POItemsTableProps) {
  const { formatCurrency } = useCurrency();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<PurchaseOrderItem | null>(null);

  const showSupplierCurrency = supplierCurrency && supplierCurrency !== "TRY";

  const handleDeleteClick = (item: PurchaseOrderItem) => {
    setItemToDelete(item);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = () => {
    if (itemToDelete) {
      onRemove(itemToDelete.id);
      setDeleteDialogOpen(false);
      setItemToDelete(null);
    }
  };

  if (items.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        Henüz ürün eklenmemiş. Yukarıdaki butonu kullanarak ürün ekleyebilirsiniz.
      </div>
    );
  }

  return (
    <>
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[50px]"></TableHead>
              <TableHead>Ürün</TableHead>
              <TableHead className="text-center">Adet</TableHead>
              {showSupplierCurrency && (
                <TableHead className="text-right">Maliyet ({supplierCurrency})</TableHead>
              )}
              <TableHead className="text-right">Üretim Maliyeti</TableHead>
              <TableHead className="text-right">Nakliye Maliyeti</TableHead>
              <TableHead className="text-right">Birim Maliyet</TableHead>
              <TableHead className="text-right">Toplam</TableHead>
              <TableHead>HS Kodu</TableHead>
              <TableHead>Etiketler</TableHead>
              {!disabled && <TableHead className="w-[100px]"></TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {items.map((item) => (
              <TableRow key={item.id}>
                <TableCell>
                  {item.productImage ? (
                    <Image
                      src={item.productImage}
                      alt={item.productName}
                      width={40}
                      height={40}
                      className="rounded object-cover"
                    />
                  ) : (
                    <div className="w-10 h-10 bg-muted rounded flex items-center justify-center">
                      <ImageIcon className="h-4 w-4 text-muted-foreground" />
                    </div>
                  )}
                </TableCell>
                <TableCell>
                  <div>
                    <p className="font-medium text-sm line-clamp-1">{item.productName}</p>
                    <p className="text-xs text-muted-foreground">{item.productBarcode}</p>
                  </div>
                </TableCell>
                <TableCell className="text-center">
                  {item.unitsOrdered.toLocaleString()}
                </TableCell>
                {showSupplierCurrency && (
                  <TableCell className="text-right text-muted-foreground">
                    {item.manufacturingCostSupplierCurrency != null
                      ? item.manufacturingCostSupplierCurrency.toFixed(2)
                      : "-"}
                  </TableCell>
                )}
                <TableCell className="text-right">
                  {formatCurrency(item.manufacturingCostPerUnit)}
                </TableCell>
                <TableCell className="text-right">
                  {formatCurrency(item.transportationCostPerUnit)}
                </TableCell>
                <TableCell className="text-right font-medium">
                  {formatCurrency(item.totalCostPerUnit)}
                </TableCell>
                <TableCell className="text-right font-medium">
                  {formatCurrency(item.totalCost)}
                </TableCell>
                <TableCell>
                  <span className="text-xs text-muted-foreground font-mono">
                    {item.hsCode || "-"}
                  </span>
                </TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-1">
                    {item.labels
                      ? item.labels.split(",").map((label, i) => (
                          <Badge key={i} variant="outline" className="text-xs px-1.5 py-0">
                            {label.trim()}
                          </Badge>
                        ))
                      : <span className="text-xs text-muted-foreground">-</span>}
                  </div>
                </TableCell>
                {!disabled && (
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => onEdit(item)}
                      >
                        <Edit2 className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleDeleteClick(item)}
                        className="text-destructive hover:text-destructive"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Ürünü Kaldır</AlertDialogTitle>
            <AlertDialogDescription>
              {itemToDelete?.productName} ürününü siparişten kaldırmak istediğinizden emin misiniz?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>İptal</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {isRemoving ? <Loader2 className="h-4 w-4 animate-spin" /> : "Kaldır"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
