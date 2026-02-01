"use client";

import { useState } from "react";
import { Search, Plus, X } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useProductsByStore } from "@/hooks/queries/use-products";
import { useAddProductToTrack } from "@/hooks/queries/use-buybox";
import { BuyboxTrackedProduct } from "@/types/buybox";

interface BuyboxAddProductModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  storeId: string;
  trackedProducts: BuyboxTrackedProduct[];
}

export function BuyboxAddProductModal({
  open,
  onOpenChange,
  storeId,
  trackedProducts,
}: BuyboxAddProductModalProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const { data: products, isLoading } = useProductsByStore(storeId);
  const addProduct = useAddProductToTrack(storeId);

  // Takipteki ürün ID'lerini al
  const trackedProductIds = new Set(trackedProducts.map((p) => p.productId));

  // Arama ve filtreleme - handle both array and paginated response formats
  // API returns { products: [...], totalElements, ... } for paginated response
  const productList = Array.isArray(products) ? products : ((products as any)?.products || []);
  const filteredProducts = productList.filter((product: any) => {
    // Zaten takipte olanları çıkar
    if (trackedProductIds.has(product.id)) {
      return false;
    }

    // Arama filtresi
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      return (
        product.title?.toLowerCase().includes(search) ||
        product.barcode?.toLowerCase().includes(search) ||
        product.productId?.toLowerCase().includes(search)
      );
    }

    return true;
  });

  const handleAddProduct = async (productId: string) => {
    try {
      await addProduct.mutateAsync({ productId });
      onOpenChange(false);
    } catch (error) {
      // Error handled by mutation
    }
  };

  const formatPrice = (price: number | undefined) => {
    if (price === undefined || price === null) return "-";
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: "TRY",
    }).format(price);
  };

  const maxReached = trackedProducts.length >= 10;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Ürün Ekle</DialogTitle>
          <DialogDescription>
            Envanterinizden buybox takibi yapmak istediğiniz ürünü seçin.
            {maxReached && (
              <span className="text-red-500 block mt-1">
                Maksimum 10 ürün takip edilebilir.
              </span>
            )}
          </DialogDescription>
        </DialogHeader>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Ürün ara (isim, barkod, ID)"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10"
          />
          {searchTerm && (
            <Button
              variant="ghost"
              size="sm"
              className="absolute right-1 top-1/2 -translate-y-1/2"
              onClick={() => setSearchTerm("")}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>

        <ScrollArea className="h-[400px] border rounded-lg">
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary" />
            </div>
          ) : filteredProducts.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              {searchTerm
                ? "Arama sonucu bulunamadı"
                : "Eklenebilecek ürün bulunamadı"}
            </div>
          ) : (
            <div className="divide-y">
              {filteredProducts.map((product) => (
                <div
                  key={product.id}
                  className="flex items-center justify-between p-3 hover:bg-muted/50"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    {product.image ? (
                      <img
                        src={product.image}
                        alt={product.title}
                        className="w-12 h-12 object-cover rounded"
                      />
                    ) : (
                      <div className="w-12 h-12 bg-muted rounded flex items-center justify-center">
                        <span className="text-xs text-muted-foreground">N/A</span>
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <p className="font-medium line-clamp-1">{product.title}</p>
                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <span>{product.barcode}</span>
                        <span>•</span>
                        <span>{formatPrice(product.salePrice)}</span>
                      </div>
                    </div>
                  </div>
                  <Button
                    size="sm"
                    onClick={() => handleAddProduct(product.id)}
                    disabled={addProduct.isPending || maxReached}
                  >
                    <Plus className="h-4 w-4 mr-1" />
                    Ekle
                  </Button>
                </div>
              ))}
            </div>
          )}
        </ScrollArea>

        <div className="text-sm text-muted-foreground">
          {filteredProducts.length} ürün listeleniyor
          {trackedProducts.length > 0 && (
            <span> • {trackedProducts.length}/10 ürün takipte</span>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
