"use client";

import { useRouter } from "next/navigation";
import type { PurchaseOrderSummary, PurchaseOrderStatus } from "@/types/purchasing";
import type { Supplier } from "@/types/supplier";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  useDeletePurchaseOrder,
  useDuplicatePurchaseOrder,
} from "@/hooks/queries/use-purchasing";
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
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  MoreHorizontal,
  Eye,
  Trash2,
  Loader2,
  Copy,
  Search,
  X,
} from "lucide-react";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import { useState, useMemo } from "react";
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

const statusConfig: Record<
  PurchaseOrderStatus,
  { label: string; variant: "default" | "secondary" | "outline" | "destructive" }
> = {
  DRAFT: { label: "Taslak", variant: "secondary" },
  ORDERED: { label: "Sipariş Verildi", variant: "default" },
  SHIPPED: { label: "Gönderildi", variant: "outline" },
  CLOSED: { label: "Kapatıldı", variant: "default" },
};

type GroupBy = "none" | "supplier" | "parentPo";

interface POListTableProps {
  orders: PurchaseOrderSummary[];
  storeId: string;
  isLoading: boolean;
  suppliers?: Supplier[];
  // Controlled filter state from parent
  search: string;
  onSearchChange: (value: string) => void;
  supplierId: string;
  onSupplierIdChange: (value: string) => void;
}

export function POListTable({
  orders,
  storeId,
  isLoading,
  suppliers,
  search,
  onSearchChange,
  supplierId,
  onSupplierIdChange,
}: POListTableProps) {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
  const deleteMutation = useDeletePurchaseOrder();
  const duplicateMutation = useDuplicatePurchaseOrder();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [orderToDelete, setOrderToDelete] = useState<PurchaseOrderSummary | null>(null);
  const [groupBy, setGroupBy] = useState<GroupBy>("none");

  const handleView = (poId: number) => {
    router.push(`/purchasing/${poId}`);
  };

  const handleDeleteClick = (order: PurchaseOrderSummary) => {
    setOrderToDelete(order);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (orderToDelete) {
      await deleteMutation.mutateAsync({ storeId, poId: orderToDelete.id });
      setDeleteDialogOpen(false);
      setOrderToDelete(null);
    }
  };

  const handleDuplicate = async (order: PurchaseOrderSummary) => {
    try {
      const newPO = await duplicateMutation.mutateAsync({ storeId, poId: order.id });
      router.push(`/purchasing/${(newPO as { id: number }).id}`);
    } catch (error) {
      console.error("Failed to duplicate PO:", error);
    }
  };

  // Group orders
  const groupedOrders = useMemo(() => {
    if (groupBy === "none") return null;

    const groups: Record<string, PurchaseOrderSummary[]> = {};
    for (const order of orders) {
      let key: string;
      if (groupBy === "supplier") {
        key = order.supplierName || "Tedarikçi Belirtilmemiş";
      } else {
        key = order.parentPoId ? `Ana PO #${order.parentPoId}` : "Bağımsız Siparişler";
      }
      if (!groups[key]) groups[key] = [];
      groups[key].push(order);
    }
    return groups;
  }, [orders, groupBy]);

  const hasFilters = search || supplierId;

  const renderOrderRow = (order: PurchaseOrderSummary) => (
    <TableRow
      key={order.id}
      className="cursor-pointer hover:bg-muted/50"
      onClick={() => handleView(order.id)}
    >
      <TableCell className="font-medium">{order.poNumber}</TableCell>
      <TableCell>
        {format(new Date(order.poDate), "dd MMM yyyy", { locale: tr })}
      </TableCell>
      <TableCell>{order.supplierName || "-"}</TableCell>
      <TableCell className="text-center">{order.itemCount}</TableCell>
      <TableCell className="text-center">
        {order.totalUnits.toLocaleString()}
      </TableCell>
      <TableCell className="text-right font-medium">
        {formatCurrency(order.totalCost)}
      </TableCell>
      <TableCell>
        {order.estimatedArrival
          ? format(new Date(order.estimatedArrival), "dd MMM yyyy", { locale: tr })
          : "-"}
      </TableCell>
      <TableCell className="text-center">
        <Badge variant={statusConfig[order.status].variant}>
          {statusConfig[order.status].label}
        </Badge>
      </TableCell>
      <TableCell>
        <DropdownMenu>
          <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
            <Button variant="ghost" size="icon">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => handleView(order.id)}>
              <Eye className="h-4 w-4 mr-2" />
              Görüntüle
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={(e) => {
                e.stopPropagation();
                handleDuplicate(order);
              }}
              disabled={duplicateMutation.isPending}
            >
              <Copy className="h-4 w-4 mr-2" />
              Kopyala
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              className="text-destructive"
              onClick={(e) => {
                e.stopPropagation();
                handleDeleteClick(order);
              }}
            >
              <Trash2 className="h-4 w-4 mr-2" />
              Sil
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </TableCell>
    </TableRow>
  );

  const tableHeader = (
    <TableHeader>
      <TableRow>
        <TableHead>Sipariş No</TableHead>
        <TableHead>Tarih</TableHead>
        <TableHead>Tedarikçi</TableHead>
        <TableHead className="text-center">Ürün</TableHead>
        <TableHead className="text-center">Birim</TableHead>
        <TableHead className="text-right">Maliyet</TableHead>
        <TableHead>Tahmini Varış</TableHead>
        <TableHead className="text-center">Durum</TableHead>
        <TableHead className="w-[70px]"></TableHead>
      </TableRow>
    </TableHeader>
  );

  return (
    <>
      {/* Filters Row */}
      <div className="flex flex-col sm:flex-row gap-3 mb-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="PO numarası veya tedarikçi ara..."
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            className="pl-9"
          />
          {search && (
            <button
              onClick={() => onSearchChange("")}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        <Select value={supplierId || "all"} onValueChange={(v) => onSupplierIdChange(v === "all" ? "" : v)}>
          <SelectTrigger className="w-[200px]">
            <SelectValue placeholder="Tedarikçi" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Tüm Tedarikçiler</SelectItem>
            {suppliers?.map((s) => (
              <SelectItem key={s.id} value={String(s.id)}>
                {s.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={groupBy} onValueChange={(v) => setGroupBy(v as GroupBy)}>
          <SelectTrigger className="w-[180px]">
            <SelectValue placeholder="Gruplama" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="none">Grupsuz</SelectItem>
            <SelectItem value="supplier">Tedarikçiye Göre</SelectItem>
            <SelectItem value="parentPo">Ana Siparişe Göre</SelectItem>
          </SelectContent>
        </Select>

        {hasFilters && (
          <Button
            variant="ghost"
            size="sm"
            className="self-center"
            onClick={() => {
              onSearchChange("");
              onSupplierIdChange("");
            }}
          >
            <X className="h-4 w-4 mr-1" />
            Temizle
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      ) : orders.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-muted-foreground">
            {hasFilters
              ? "Arama kriterlerine uygun sipariş bulunamadı."
              : "Henüz satın alma siparişi bulunmuyor."}
          </p>
        </div>
      ) : groupedOrders ? (
        // Grouped view
        <div className="space-y-6">
          {Object.entries(groupedOrders).map(([groupName, groupOrders]) => (
            <div key={groupName}>
              <div className="flex items-center gap-2 mb-2">
                <h3 className="text-sm font-semibold text-foreground">{groupName}</h3>
                <Badge variant="outline" className="text-xs">
                  {groupOrders.length}
                </Badge>
              </div>
              <Table>
                {tableHeader}
                <TableBody>
                  {groupOrders.map(renderOrderRow)}
                </TableBody>
              </Table>
            </div>
          ))}
        </div>
      ) : (
        // Flat view
        <Table>
          {tableHeader}
          <TableBody>
            {orders.map(renderOrderRow)}
          </TableBody>
        </Table>
      )}

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Siparişi Sil</AlertDialogTitle>
            <AlertDialogDescription>
              {orderToDelete?.poNumber} numaralı siparişi silmek istediğinizden emin misiniz?
              Bu işlem geri alınamaz.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>İptal</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
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
    </>
  );
}
