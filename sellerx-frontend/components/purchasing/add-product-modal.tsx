"use client";

import { useState, useMemo } from "react";
import { useProductsByStorePaginatedFull } from "@/hooks/queries/use-products";
import type { AddPurchaseOrderItemRequest, PurchaseOrderItem } from "@/types/purchasing";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Search, Loader2, ImageIcon } from "lucide-react";
import Image from "next/image";
import { cn } from "@/lib/utils";

interface AddProductModalProps {
  storeId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAdd: (data: AddPurchaseOrderItemRequest) => void;
  isAdding: boolean;
  editingItem?: PurchaseOrderItem | null;
  supplierCurrency?: string;
}

export function AddProductModal({
  storeId,
  open,
  onOpenChange,
  onAdd,
  isAdding,
  editingItem,
  supplierCurrency,
}: AddProductModalProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedProductId, setSelectedProductId] = useState<string | null>(
    editingItem?.productId || null
  );
  const [unitsOrdered, setUnitsOrdered] = useState(editingItem?.unitsOrdered?.toString() || "");
  const [manufacturingCost, setManufacturingCost] = useState(
    editingItem?.manufacturingCostPerUnit?.toString() || ""
  );
  const [manufacturingCostSupplierCurrency, setManufacturingCostSupplierCurrency] = useState(
    editingItem?.manufacturingCostSupplierCurrency?.toString() || ""
  );
  const [transportationCost, setTransportationCost] = useState(
    editingItem?.transportationCostPerUnit?.toString() || "0"
  );
  const [hsCode, setHsCode] = useState(editingItem?.hsCode || "");
  const [labels, setLabels] = useState(editingItem?.labels || "");

  const showSupplierCurrency = supplierCurrency && supplierCurrency !== "TRY";

  const { data: productsData, isLoading: productsLoading } = useProductsByStorePaginatedFull(
    storeId,
    { page: 0, size: 100 }
  );

  const products = productsData?.products || [];

  const filteredProducts = useMemo(() => {
    if (!searchQuery) return products;
    const query = searchQuery.toLowerCase();
    return products.filter(
      (p) =>
        p.title?.toLowerCase().includes(query) ||
        p.barcode?.toLowerCase().includes(query) ||
        p.brand?.toLowerCase().includes(query)
    );
  }, [products, searchQuery]);

  const selectedProduct = products.find((p) => p.id === selectedProductId);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProductId || !unitsOrdered || !manufacturingCost) return;

    onAdd({
      productId: selectedProductId,
      unitsOrdered: parseInt(unitsOrdered),
      manufacturingCostPerUnit: parseFloat(manufacturingCost),
      manufacturingCostSupplierCurrency: manufacturingCostSupplierCurrency
        ? parseFloat(manufacturingCostSupplierCurrency)
        : undefined,
      transportationCostPerUnit: parseFloat(transportationCost) || 0,
      hsCode: hsCode || undefined,
      labels: labels || undefined,
    });
  };

  const resetForm = () => {
    setSearchQuery("");
    setSelectedProductId(null);
    setUnitsOrdered("");
    setManufacturingCost("");
    setManufacturingCostSupplierCurrency("");
    setTransportationCost("0");
    setHsCode("");
    setLabels("");
  };

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      resetForm();
    }
    onOpenChange(isOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>
            {editingItem ? "Ürün Düzenle" : "Ürün Ekle"}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex-1 overflow-hidden flex flex-col gap-4">
          {/* Product Search & Selection */}
          {!editingItem && (
            <div className="space-y-3">
              <Label>Ürün Seç</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Ürün adı veya barkod ile ara..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9"
                />
              </div>

              <div className="border border-border rounded-lg max-h-[200px] overflow-y-auto">
                {productsLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                  </div>
                ) : filteredProducts.length === 0 ? (
                  <div className="text-center py-8 text-muted-foreground text-sm">
                    Ürün bulunamadı
                  </div>
                ) : (
                  <div className="divide-y divide-border">
                    {filteredProducts.slice(0, 20).map((product) => (
                      <button
                        key={product.id}
                        type="button"
                        onClick={() => setSelectedProductId(product.id)}
                        className={cn(
                          "w-full flex items-center gap-3 p-3 text-left hover:bg-muted/50 transition-colors",
                          selectedProductId === product.id && "bg-primary/10"
                        )}
                      >
                        {product.image ? (
                          <Image
                            src={product.image}
                            alt={product.title}
                            width={40}
                            height={40}
                            className="rounded object-cover"
                          />
                        ) : (
                          <div className="w-10 h-10 bg-muted rounded flex items-center justify-center">
                            <ImageIcon className="h-4 w-4 text-muted-foreground" />
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium line-clamp-1">{product.title}</p>
                          <p className="text-xs text-muted-foreground">{product.barcode}</p>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Selected Product Info */}
          {selectedProduct && (
            <div className="bg-muted/50 rounded-lg p-3 flex items-center gap-3">
              {selectedProduct.image ? (
                <Image
                  src={selectedProduct.image}
                  alt={selectedProduct.title}
                  width={48}
                  height={48}
                  className="rounded object-cover"
                />
              ) : (
                <div className="w-12 h-12 bg-muted rounded flex items-center justify-center">
                  <ImageIcon className="h-5 w-5 text-muted-foreground" />
                </div>
              )}
              <div>
                <p className="font-medium text-sm">{selectedProduct.title}</p>
                <p className="text-xs text-muted-foreground">{selectedProduct.barcode}</p>
              </div>
            </div>
          )}

          {/* Form Fields */}
          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="unitsOrdered">Adet *</Label>
              <Input
                id="unitsOrdered"
                type="number"
                min="1"
                placeholder="0"
                value={unitsOrdered}
                onChange={(e) => setUnitsOrdered(e.target.value)}
                required
              />
            </div>

            {showSupplierCurrency && (
              <div className="space-y-2">
                <Label htmlFor="manufacturingCostSC">Üretim Maliyeti ({supplierCurrency})</Label>
                <Input
                  id="manufacturingCostSC"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={manufacturingCostSupplierCurrency}
                  onChange={(e) => setManufacturingCostSupplierCurrency(e.target.value)}
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="manufacturingCost">Üretim Maliyeti (₺) *</Label>
              <Input
                id="manufacturingCost"
                type="number"
                min="0"
                step="0.01"
                placeholder="0.00"
                value={manufacturingCost}
                onChange={(e) => setManufacturingCost(e.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="transportationCost">Nakliye Maliyeti (₺)</Label>
              <Input
                id="transportationCost"
                type="number"
                min="0"
                step="0.01"
                placeholder="0.00"
                value={transportationCost}
                onChange={(e) => setTransportationCost(e.target.value)}
              />
            </div>
          </div>

          {/* HS Code and Labels */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="hsCode">HS Kodu (Gümrük Tarife)</Label>
              <Input
                id="hsCode"
                placeholder="örn. 6204.62"
                value={hsCode}
                onChange={(e) => setHsCode(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="labels">Etiketler (virgülle ayırın)</Label>
              <Input
                id="labels"
                placeholder="örn. acil, toptan, özel"
                value={labels}
                onChange={(e) => setLabels(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              İptal
            </Button>
            <Button
              type="submit"
              disabled={!selectedProductId || !unitsOrdered || !manufacturingCost || isAdding}
            >
              {isAdding ? (
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
              ) : null}
              {editingItem ? "Güncelle" : "Ekle"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
