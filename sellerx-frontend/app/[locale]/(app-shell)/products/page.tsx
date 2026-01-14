"use client";

import { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  useProductsByStorePaginatedFull,
  useSyncProducts,
} from "@/hooks/queries/use-products";
import { useSyncStockOrders } from "@/hooks/queries/use-stock-sync";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { RefreshCw, Search, ChevronLeft, ChevronRight, ExternalLink, Coins, Package } from "lucide-react";
import { cn } from "@/lib/utils";
import type { TrendyolProduct } from "@/types/product";
import { CostEditModal } from "@/components/products/cost-edit-modal";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export default function ProductsPage() {
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [costModalOpen, setCostModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<TrendyolProduct | null>(null);
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data, isLoading, error } = useProductsByStorePaginatedFull(storeId || undefined, {
    page,
    size: 50,
    sortBy: "onSale",
    sortDirection: "desc",
  });

  const syncMutation = useSyncProducts();
  const stockSyncMutation = useSyncStockOrders();

  const handleSync = () => {
    if (storeId) {
      syncMutation.mutate(storeId);
    }
  };

  const handleStockSync = () => {
    if (storeId) {
      stockSyncMutation.mutate(storeId);
    }
  };

  const handleOpenCostModal = (product: TrendyolProduct) => {
    setSelectedProduct(product);
    setCostModalOpen(true);
  };

  // Filter products by search query
  const filteredProducts = data?.products?.filter(
    (p) =>
      p.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.barcode.toLowerCase().includes(searchQuery.toLowerCase())
  ) || [];

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <h1 className="text-2xl font-bold mb-4">Ürünler</h1>
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Ürünler</h1>
          <p className="text-sm text-gray-500 mt-1">
            {data?.totalElements
              ? `Mağazanızda ${data.totalElements} ürün var`
              : "Ürünler yükleniyor..."}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <Input
              type="search"
              placeholder="Ürün ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="h-9 w-64 pl-9 text-sm"
            />
          </div>

          <Button
            onClick={handleStockSync}
            disabled={stockSyncMutation.isPending || !storeId}
            variant="outline"
            className="gap-2"
          >
            <Package
              className={cn("h-4 w-4", stockSyncMutation.isPending && "animate-spin")}
            />
            {stockSyncMutation.isPending ? "Stok güncelleniyor..." : "Stok Senkronize Et"}
          </Button>

          <Button
            onClick={handleSync}
            disabled={syncMutation.isPending || !storeId}
            variant="outline"
            className="gap-2"
          >
            <RefreshCw
              className={cn("h-4 w-4", syncMutation.isPending && "animate-spin")}
            />
            {syncMutation.isPending ? "Senkronize ediliyor..." : "Trendyol'dan Senkronize Et"}
          </Button>
        </div>
      </div>

      {/* Sync Result */}
      {syncMutation.isSuccess && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <p className="text-green-800 text-sm">
            Senkronizasyon tamamlandı! Çekilen: {syncMutation.data.totalFetched}, Kaydedilen:{" "}
            {syncMutation.data.totalSaved}, Güncellenen: {syncMutation.data.totalUpdated}
          </p>
        </div>
      )}

      {syncMutation.isError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 text-sm">
            Senkronizasyon başarısız: {syncMutation.error.message}
          </p>
        </div>
      )}

      {/* Stock Sync Result */}
      {stockSyncMutation.isSuccess && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <p className="text-green-800 text-sm flex items-center gap-2">
            <Package className="h-4 w-4" />
            Stok senkronizasyonu tamamlandı!{" "}
            {stockSyncMutation.data.productsProcessed > 0 && (
              <span>
                ({stockSyncMutation.data.productsProcessed} ürün işlendi,{" "}
                {stockSyncMutation.data.productsUpdated} ürün güncellendi)
              </span>
            )}
          </p>
        </div>
      )}

      {stockSyncMutation.isError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 text-sm">
            Stok senkronizasyonu başarısız: {stockSyncMutation.error.message}
          </p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 text-sm">Ürünler yüklenirken hata: {error.message}</p>
        </div>
      )}

      {/* Products Table */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="w-[80px]">Görsel</TableHead>
                <TableHead className="min-w-[300px]">Ürün</TableHead>
                <TableHead className="text-right">Fiyat</TableHead>
                <TableHead className="text-right">Stok</TableHead>
                <TableHead className="text-right">Komisyon</TableHead>
                <TableHead className="text-center">Durum</TableHead>
                <TableHead className="w-[100px]">İşlemler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={7} className="h-24 text-center">
                    <RefreshCw className="h-5 w-5 animate-spin mx-auto mb-2" />
                    Ürünler yükleniyor...
                  </TableCell>
                </TableRow>
              ) : filteredProducts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="h-24 text-center text-gray-500">
                    Ürün bulunamadı
                  </TableCell>
                </TableRow>
              ) : (
                filteredProducts.map((product: TrendyolProduct) => (
                  <TableRow key={product.id} className="hover:bg-gray-50">
                    <TableCell>
                      {product.image ? (
                        <img
                          src={product.image}
                          alt={product.title}
                          className="w-12 h-12 object-cover rounded"
                        />
                      ) : (
                        <div className="w-12 h-12 bg-gray-100 rounded flex items-center justify-center text-gray-400 text-xs">
                          Yok
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="min-w-0">
                        <p className="font-medium text-sm text-gray-900 line-clamp-2">
                          {product.title}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                          Barkod: {product.barcode}
                        </p>
                        <p className="text-xs text-gray-400">
                          {product.brand} • {product.categoryName}
                        </p>
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <span className="font-medium">
                        {formatCurrency(product.salePrice)} TL
                      </span>
                      <p className="text-xs text-gray-500">
                        KDV: %{product.vatRate}
                      </p>
                    </TableCell>
                    <TableCell className="text-right">
                      <span
                        className={cn(
                          "font-medium",
                          product.trendyolQuantity === 0
                            ? "text-red-600"
                            : product.trendyolQuantity < 10
                              ? "text-yellow-600"
                              : "text-green-600"
                        )}
                      >
                        {product.trendyolQuantity}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <span className="text-sm">%{product.commissionRate}</span>
                    </TableCell>
                    <TableCell className="text-center">
                      <div className="flex flex-wrap gap-1 justify-center">
                        {product.onSale && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
                            Satışta
                          </span>
                        )}
                        {product.approved && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                            Onaylı
                          </span>
                        )}
                        {product.archived && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                            Arşivli
                          </span>
                        )}
                        {product.rejected && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-800">
                            Reddedildi
                          </span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleOpenCostModal(product)}
                          className="h-8 text-orange-600 hover:text-orange-800"
                        >
                          <Coins className="h-4 w-4 mr-1" />
                          Maliyet
                        </Button>
                        {product.productUrl && (
                          <Button
                            variant="ghost"
                            size="sm"
                            asChild
                            className="h-8 text-blue-600 hover:text-blue-800"
                          >
                            <a
                              href={product.productUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              <ExternalLink className="h-4 w-4 mr-1" />
                              Görüntüle
                            </a>
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-200">
            <p className="text-sm text-gray-500">
              Sayfa {data.currentPage + 1} / {data.totalPages} ({data.totalElements} ürün)
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={data.isFirst}
              >
                <ChevronLeft className="h-4 w-4" />
                Önceki
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.isLast}
              >
                Sonraki
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Cost Edit Modal */}
      {selectedProduct && (
        <CostEditModal
          product={selectedProduct}
          open={costModalOpen}
          onOpenChange={setCostModalOpen}
        />
      )}
    </div>
  );
}
