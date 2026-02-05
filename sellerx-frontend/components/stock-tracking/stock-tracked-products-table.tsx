"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
import {
  MoreHorizontal,
  RefreshCw,
  Trash2,
  Eye,
  ExternalLink,
  Loader2,
} from "lucide-react";
import type { TrackedProduct } from "@/types/stock-tracking";
import { useRemoveTrackedProduct, useCheckStockNow } from "@/hooks/queries/use-stock-tracking";
import { useTranslations } from "next-intl";
import { useLocale } from "next-intl";
import { toast } from "sonner";
import { formatDistanceToNow } from "date-fns";
import { tr, enUS } from "date-fns/locale";

interface StockTrackedProductsTableProps {
  products: TrackedProduct[] | undefined;
  storeId: string | undefined;
  isLoading: boolean;
  mockMode?: boolean;
}

export function StockTrackedProductsTable({
  products,
  storeId,
  isLoading,
  mockMode = false,
}: StockTrackedProductsTableProps) {
  const t = useTranslations("stockTracking");
  const locale = useLocale();
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [checkingId, setCheckingId] = useState<string | null>(null);

  const removeProduct = useRemoveTrackedProduct(storeId);
  const checkStock = useCheckStockNow(storeId);

  const handleCheckStock = async (productId: string) => {
    if (mockMode) {
      toast.success(t("table.checkSuccess"));
      return;
    }
    setCheckingId(productId);
    try {
      await checkStock.mutateAsync(productId);
      toast.success(t("table.checkSuccess"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("table.checkError"));
    } finally {
      setCheckingId(null);
    }
  };

  const handleDelete = async () => {
    if (!deleteId) return;

    if (mockMode) {
      toast.success(t("table.deleteSuccess"));
      setDeleteId(null);
      return;
    }

    try {
      await removeProduct.mutateAsync(deleteId);
      toast.success(t("table.deleteSuccess"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("table.deleteError"));
    } finally {
      setDeleteId(null);
    }
  };

  const getStockBadge = (product: TrackedProduct) => {
    if (product.lastStockQuantity === null) {
      return <Badge variant="outline">{t("table.noData")}</Badge>;
    }
    if (product.lastStockQuantity === 0) {
      return <Badge variant="destructive">{t("table.outOfStock")}</Badge>;
    }
    if (product.lastStockQuantity <= product.lowStockThreshold) {
      return (
        <Badge variant="outline" className="border-orange-500 text-orange-600">
          {t("table.lowStock")}
        </Badge>
      );
    }
    return <Badge variant="outline" className="border-green-500 text-green-600">{t("table.inStock")}</Badge>;
  };

  if (isLoading) {
    return (
      <div className="bg-card rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[80px]">{t("table.image")}</TableHead>
              <TableHead>{t("table.product")}</TableHead>
              <TableHead>{t("table.stock")}</TableHead>
              <TableHead>{t("table.lastChecked")}</TableHead>
              <TableHead className="w-[100px]">{t("table.actions")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {[1, 2, 3].map((i) => (
              <TableRow key={i} className="animate-pulse">
                <TableCell><div className="h-12 w-12 bg-gray-200 rounded" /></TableCell>
                <TableCell><div className="h-4 w-48 bg-gray-200 rounded" /></TableCell>
                <TableCell><div className="h-4 w-16 bg-gray-200 rounded" /></TableCell>
                <TableCell><div className="h-4 w-24 bg-gray-200 rounded" /></TableCell>
                <TableCell><div className="h-4 w-8 bg-gray-200 rounded" /></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    );
  }

  if (!products || products.length === 0) {
    return (
      <div className="bg-card rounded-lg border border-border p-8 text-center">
        <p className="text-muted-foreground">{t("table.empty")}</p>
      </div>
    );
  }

  return (
    <>
      <div className="bg-card rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[80px]">{t("table.image")}</TableHead>
              <TableHead>{t("table.product")}</TableHead>
              <TableHead>{t("table.stock")}</TableHead>
              <TableHead>{t("table.price")}</TableHead>
              <TableHead>{t("table.lastChecked")}</TableHead>
              <TableHead className="w-[100px]">{t("table.actions")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {products.map((product) => (
              <TableRow key={product.id} className={!product.isActive ? "opacity-50" : ""}>
                <TableCell>
                  {product.imageUrl ? (
                    <Image
                      src={product.imageUrl}
                      alt={product.productName}
                      width={48}
                      height={48}
                      className="rounded object-cover"
                    />
                  ) : (
                    <div className="h-12 w-12 bg-gray-100 rounded flex items-center justify-center">
                      <span className="text-xs text-gray-400">N/A</span>
                    </div>
                  )}
                </TableCell>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-medium line-clamp-1">{product.productName}</span>
                    <span className="text-xs text-muted-foreground">{product.brandName}</span>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col gap-1">
                    <span className="font-semibold">
                      {product.lastStockQuantity ?? "-"} {t("table.unit")}
                    </span>
                    {getStockBadge(product)}
                  </div>
                </TableCell>
                <TableCell>
                  {product.lastPrice ? (
                    <span>{product.lastPrice.toLocaleString(locale === "tr" ? "tr-TR" : "en-US")} TL</span>
                  ) : (
                    <span className="text-muted-foreground">-</span>
                  )}
                </TableCell>
                <TableCell>
                  {product.lastCheckedAt ? (
                    <span className="text-sm text-muted-foreground">
                      {formatDistanceToNow(new Date(product.lastCheckedAt), {
                        addSuffix: true,
                        locale: locale === "tr" ? tr : enUS,
                      })}
                    </span>
                  ) : (
                    <span className="text-muted-foreground">-</span>
                  )}
                </TableCell>
                <TableCell>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem asChild>
                        <Link href={`/${locale}/stock-tracking/${product.id}`}>
                          <Eye className="mr-2 h-4 w-4" />
                          {t("table.viewDetail")}
                        </Link>
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        onClick={() => handleCheckStock(product.id)}
                        disabled={checkingId === product.id}
                      >
                        {checkingId === product.id ? (
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        ) : (
                          <RefreshCw className="mr-2 h-4 w-4" />
                        )}
                        {t("table.checkNow")}
                      </DropdownMenuItem>
                      <DropdownMenuItem asChild>
                        <a href={product.productUrl} target="_blank" rel="noopener noreferrer">
                          <ExternalLink className="mr-2 h-4 w-4" />
                          {t("table.openTrendyol")}
                        </a>
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        onClick={() => setDeleteId(product.id)}
                        className="text-red-600"
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        {t("table.delete")}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <AlertDialog open={!!deleteId} onOpenChange={(open) => !open && setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t("table.deleteConfirmTitle")}</AlertDialogTitle>
            <AlertDialogDescription>
              {t("table.deleteConfirmDescription")}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t("common.cancel")}</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-red-600 hover:bg-red-700"
            >
              {removeProduct.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              {t("table.confirmDelete")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
