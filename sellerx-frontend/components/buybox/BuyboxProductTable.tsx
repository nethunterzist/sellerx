"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2, RefreshCw, Eye, MoreVertical } from "lucide-react";
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
  BuyboxTrackedProduct,
  getBuyboxStatusLabel,
  getBuyboxStatusColor,
} from "@/types/buybox";
import { useRemoveProductFromTrack } from "@/hooks/queries/use-buybox";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface BuyboxProductTableProps {
  products: BuyboxTrackedProduct[];
  storeId: string;
  isLoading: boolean;
}

export function BuyboxProductTable({
  products,
  storeId,
  isLoading,
}: BuyboxProductTableProps) {
  const router = useRouter();
  const removeProduct = useRemoveProductFromTrack(storeId);
  const [removingId, setRemovingId] = useState<string | null>(null);

  const handleRemove = async (id: string) => {
    if (!confirm("Bu ürünü takipten çıkarmak istediğinize emin misiniz?")) {
      return;
    }
    setRemovingId(id);
    try {
      await removeProduct.mutateAsync(id);
    } finally {
      setRemovingId(null);
    }
  };

  const handleViewDetail = (id: string) => {
    router.push(`/buybox/${id}`);
  };

  const formatPrice = (price: number | undefined) => {
    if (price === undefined || price === null) return "-";
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: "TRY",
    }).format(price);
  };

  const getStatusBadgeVariant = (status: string | undefined) => {
    if (!status) return "secondary";
    const colorMap: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
      success: "default",
      destructive: "destructive",
      warning: "secondary",
      secondary: "outline",
    };
    return colorMap[getBuyboxStatusColor(status as any)] || "secondary";
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (products.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        Henüz takip edilen ürün yok. Envanterinizden ürün ekleyin.
      </div>
    );
  }

  return (
    <div className="border rounded-lg">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Ürün</TableHead>
            <TableHead>Durum</TableHead>
            <TableHead className="text-right">Fiyatınız</TableHead>
            <TableHead className="text-right">Kazanan Fiyat</TableHead>
            <TableHead className="text-right">Fark</TableHead>
            <TableHead className="text-center">Sıra</TableHead>
            <TableHead>Son Kontrol</TableHead>
            <TableHead className="w-[50px]"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {products.map((product) => (
            <TableRow
              key={product.id}
              className="cursor-pointer hover:bg-muted/50"
              onClick={() => handleViewDetail(product.id)}
            >
              <TableCell>
                <div className="flex items-center gap-3">
                  {product.productImageUrl ? (
                    <img
                      src={product.productImageUrl}
                      alt={product.productTitle}
                      className="w-10 h-10 object-cover rounded"
                    />
                  ) : (
                    <div className="w-10 h-10 bg-muted rounded flex items-center justify-center">
                      <span className="text-xs text-muted-foreground">N/A</span>
                    </div>
                  )}
                  <div>
                    <p className="font-medium line-clamp-1">
                      {product.productTitle}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {product.productBarcode}
                    </p>
                  </div>
                </div>
              </TableCell>
              <TableCell>
                {product.lastStatus ? (
                  <Badge variant={getStatusBadgeVariant(product.lastStatus)}>
                    {getBuyboxStatusLabel(product.lastStatus)}
                  </Badge>
                ) : (
                  <span className="text-muted-foreground">-</span>
                )}
              </TableCell>
              <TableCell className="text-right">
                {formatPrice(product.myPrice)}
              </TableCell>
              <TableCell className="text-right">
                {formatPrice(product.lastWinnerPrice)}
              </TableCell>
              <TableCell className="text-right">
                {product.priceDifference !== undefined &&
                product.priceDifference !== null ? (
                  <span
                    className={
                      product.priceDifference > 0
                        ? "text-red-600"
                        : product.priceDifference < 0
                        ? "text-green-600"
                        : ""
                    }
                  >
                    {product.priceDifference > 0 ? "+" : ""}
                    {formatPrice(product.priceDifference)}
                  </span>
                ) : (
                  "-"
                )}
              </TableCell>
              <TableCell className="text-center">
                {product.myPosition ? (
                  <span className="font-medium">
                    {product.myPosition}/{product.totalSellers || "?"}
                  </span>
                ) : (
                  "-"
                )}
              </TableCell>
              <TableCell>
                {product.lastCheckedAt ? (
                  <span className="text-sm text-muted-foreground">
                    {formatDistanceToNow(new Date(product.lastCheckedAt), {
                      addSuffix: true,
                      locale: tr,
                    })}
                  </span>
                ) : (
                  "-"
                )}
              </TableCell>
              <TableCell>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                    <Button variant="ghost" size="icon">
                      <MoreVertical className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => handleViewDetail(product.id)}>
                      <Eye className="h-4 w-4 mr-2" />
                      Detay
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      className="text-red-600"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleRemove(product.id);
                      }}
                      disabled={removingId === product.id}
                    >
                      <Trash2 className="h-4 w-4 mr-2" />
                      Takipten Çıkar
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
