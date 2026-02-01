"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useProductsByStorePaginatedFull } from "@/hooks/queries/use-products";
import { useProductCostHistory } from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  ArrowLeft,
  Search,
  Clock,
  Package,
  TrendingUp,
  TrendingDown,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import type { CostEntry } from "@/types/purchasing";

export default function CostHistoryPage() {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
  const [selectedProductId, setSelectedProductId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [showProductList, setShowProductList] = useState(false);

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: productListResponse, isLoading: productsLoading } = useProductsByStorePaginatedFull(storeId, { size: 500 });
  const products = productListResponse?.products || [];
  const { data: costHistory, isLoading: historyLoading } = useProductCostHistory(
    storeId || undefined,
    selectedProductId || undefined
  );

  // Filter products by search term
  const filteredProducts = useMemo(() => {
    if (!products.length) return [];
    if (!searchTerm) return products;
    const term = searchTerm.toLowerCase();
    return products.filter(
      (p: any) =>
        p.title?.toLowerCase().includes(term) ||
        p.barcode?.toLowerCase().includes(term) ||
        p.productId?.toLowerCase().includes(term)
    );
  }, [products, searchTerm]);

  // Selected product details
  const selectedProduct = useMemo(() => {
    if (!selectedProductId || !products) return null;
    return products.find((p) => p.id === selectedProductId);
  }, [selectedProductId, products]);

  // Format date for display
  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString("tr-TR", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  };

  // Calculate usage bar color
  const getUsageColor = (percentage: number): string => {
    if (percentage >= 100) return "bg-green-500 dark:bg-green-400";
    if (percentage >= 50) return "bg-blue-500 dark:bg-blue-400";
    if (percentage > 0) return "bg-amber-500 dark:bg-amber-400";
    return "bg-gray-300 dark:bg-gray-600";
  };

  // No store selected
  if (!storeId && !storeLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <Clock className="h-16 w-16 text-muted-foreground/30 mb-4" />
        <h2 className="text-lg font-medium text-foreground mb-2">Magaza Secilmedi</h2>
        <p className="text-sm text-muted-foreground max-w-md">
          Maliyet gecmisi raporunu goruntulemek icin lutfen bir magaza secin.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => router.push("/purchasing")}
          className="gap-2"
        >
          <ArrowLeft className="h-4 w-4" />
          Geri
        </Button>
        <div>
          <h1 className="text-2xl font-bold text-foreground">Urun Maliyet Gecmisi</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Urun bazli maliyet degisimlerini ve FIFO lotlarini inceleyin
          </p>
        </div>
      </div>

      {/* Product Selector */}
      <div className="bg-card rounded-xl border border-border p-6">
        <div className="relative">
          <label className="block text-sm font-medium text-foreground mb-2">
            Urun Sec
          </label>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="Urun adi, barkod veya stok kodu ile ara..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setShowProductList(true);
              }}
              onFocus={() => setShowProductList(true)}
              className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-border bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Product Dropdown */}
          {showProductList && (
            <div className="absolute top-full left-0 right-0 mt-1 max-h-64 overflow-y-auto bg-card border border-border rounded-lg shadow-lg z-50">
              {productsLoading ? (
                <div className="p-4 text-center text-muted-foreground">
                  Urunler yukleniyor...
                </div>
              ) : filteredProducts.length === 0 ? (
                <div className="p-4 text-center text-muted-foreground">
                  Urun bulunamadi
                </div>
              ) : (
                filteredProducts.slice(0, 20).map((product) => (
                  <button
                    key={product.id}
                    onClick={() => {
                      setSelectedProductId(product.id);
                      setSearchTerm(product.title || product.barcode || "");
                      setShowProductList(false);
                    }}
                    className={cn(
                      "w-full p-3 text-left hover:bg-muted/50 transition-colors flex items-center gap-3 border-b border-border last:border-0",
                      selectedProductId === product.id && "bg-primary/10"
                    )}
                  >
                    {product.image ? (
                      <img
                        src={product.image}
                        alt=""
                        className="w-10 h-10 rounded object-cover"
                      />
                    ) : (
                      <div className="w-10 h-10 rounded bg-muted flex items-center justify-center">
                        <Package className="h-5 w-5 text-muted-foreground" />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-foreground truncate">
                        {product.title}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {product.barcode} · {product.productId}
                      </p>
                    </div>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        {/* Selected Product Info */}
        {selectedProduct && (
          <div className="mt-4 p-4 rounded-lg bg-muted/30 flex items-center gap-4">
            {selectedProduct.image ? (
              <img
                src={selectedProduct.image}
                alt=""
                className="w-16 h-16 rounded object-cover"
              />
            ) : (
              <div className="w-16 h-16 rounded bg-muted flex items-center justify-center">
                <Package className="h-8 w-8 text-muted-foreground" />
              </div>
            )}
            <div className="flex-1 min-w-0">
              <p className="font-medium text-foreground">{selectedProduct.title}</p>
              <p className="text-sm text-muted-foreground">
                Barkod: {selectedProduct.barcode}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Cost History Results */}
      {selectedProductId && (
        <>
          {historyLoading ? (
            <div className="bg-card rounded-xl border border-border p-6">
              <div className="animate-pulse space-y-4">
                <div className="h-6 w-48 bg-muted rounded" />
                <div className="space-y-3">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="h-16 bg-muted rounded" />
                  ))}
                </div>
              </div>
            </div>
          ) : costHistory ? (
            <>
              {/* Summary Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-card rounded-xl border border-border p-4">
                  <p className="text-xs text-muted-foreground">Toplam Alim</p>
                  <p className="text-xl font-bold text-foreground mt-1">
                    {costHistory.totalQuantity.toLocaleString()} adet
                  </p>
                </div>
                <div className="bg-card rounded-xl border border-border p-4">
                  <p className="text-xs text-muted-foreground">Kalan Stok</p>
                  <p className="text-xl font-bold text-foreground mt-1">
                    {costHistory.remainingQuantity.toLocaleString()} adet
                  </p>
                </div>
                <div className="bg-card rounded-xl border border-border p-4">
                  <p className="text-xs text-muted-foreground">Ortalama Maliyet</p>
                  <p className="text-xl font-bold text-foreground mt-1">
                    {formatCurrency(costHistory.averageCost)}
                  </p>
                </div>
                <div className="bg-card rounded-xl border border-border p-4">
                  <p className="text-xs text-muted-foreground">Toplam Deger (FIFO)</p>
                  <p className="text-xl font-bold text-foreground mt-1">
                    {formatCurrency(costHistory.totalValue)}
                  </p>
                </div>
              </div>

              {/* Cost Entries (FIFO Lots) */}
              <div className="bg-card rounded-xl border border-border">
                <div className="p-4 border-b border-border">
                  <h3 className="font-semibold text-foreground">
                    Maliyet Lotlari (FIFO Sirali)
                  </h3>
                  <p className="text-xs text-muted-foreground mt-1">
                    En eski stok oncelikli olarak tuketilir
                  </p>
                </div>

                {costHistory.entries.length === 0 ? (
                  <div className="p-8 text-center">
                    <Clock className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
                    <p className="text-sm text-muted-foreground">
                      Bu urun icin maliyet kaydı bulunamadi
                    </p>
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-border bg-muted/30">
                          <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                            Tarih
                          </th>
                          <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                            Miktar
                          </th>
                          <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                            Kalan
                          </th>
                          <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                            Birim Maliyet
                          </th>
                          <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                            Toplam
                          </th>
                          <th className="text-center p-3 text-xs font-medium text-muted-foreground">
                            Kullanim
                          </th>
                          <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                            Siparis
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {costHistory.entries.map((entry, index) => (
                          <tr
                            key={index}
                            className="border-b border-border last:border-0 hover:bg-muted/20"
                          >
                            <td className="p-3 text-sm text-foreground">
                              {formatDate(entry.stockDate)}
                            </td>
                            <td className="p-3 text-sm text-foreground text-right">
                              {entry.quantity.toLocaleString()}
                            </td>
                            <td className="p-3 text-sm text-right">
                              <span
                                className={cn(
                                  "font-medium",
                                  entry.remainingQuantity === 0
                                    ? "text-muted-foreground"
                                    : "text-foreground"
                                )}
                              >
                                {entry.remainingQuantity.toLocaleString()}
                              </span>
                            </td>
                            <td className="p-3 text-sm text-foreground text-right font-medium">
                              {formatCurrency(entry.unitCost)}
                            </td>
                            <td className="p-3 text-sm text-foreground text-right">
                              {formatCurrency(entry.totalValue)}
                            </td>
                            <td className="p-3">
                              <div className="flex items-center gap-2">
                                <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                                  <div
                                    className={cn(
                                      "h-full rounded-full transition-all",
                                      getUsageColor(entry.usagePercentage)
                                    )}
                                    style={{
                                      width: `${Math.min(entry.usagePercentage, 100)}%`,
                                    }}
                                  />
                                </div>
                                <span className="text-xs text-muted-foreground w-10 text-right">
                                  {Math.round(entry.usagePercentage)}%
                                </span>
                              </div>
                            </td>
                            <td className="p-3 text-sm">
                              {entry.purchaseOrderNumber ? (
                                <button
                                  onClick={() =>
                                    router.push(`/purchasing/${entry.purchaseOrderId}`)
                                  }
                                  className="text-primary hover:underline"
                                >
                                  {entry.purchaseOrderNumber}
                                </button>
                              ) : (
                                <span className="text-muted-foreground">-</span>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Weighted Average Info */}
              {costHistory.weightedAverageCost > 0 && (
                <div className="bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-800 p-4">
                  <div className="flex items-start gap-3">
                    <TrendingUp className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5" />
                    <div>
                      <p className="font-medium text-blue-900 dark:text-blue-100">
                        Agirlikli Ortalama Maliyet
                      </p>
                      <p className="text-sm text-blue-700 dark:text-blue-300 mt-1">
                        Kalan stogun agirlikli ortalama maliyeti:{" "}
                        <strong>{formatCurrency(costHistory.weightedAverageCost)}</strong> / adet
                      </p>
                      <p className="text-xs text-blue-600 dark:text-blue-400 mt-1">
                        Bu deger, FIFO yontemiyle henuz satilmamis stoklarin agirlikli ortalamasini
                        gosterir.
                      </p>
                    </div>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="bg-card rounded-xl border border-border p-8 text-center">
              <Clock className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
              <p className="text-sm text-muted-foreground">
                Maliyet gecmisi yuklenemedi
              </p>
            </div>
          )}
        </>
      )}

      {/* Initial State - No Product Selected */}
      {!selectedProductId && (
        <div className="bg-card rounded-xl border border-border p-12 text-center">
          <Search className="h-16 w-16 mx-auto text-muted-foreground/30 mb-4" />
          <h3 className="text-lg font-medium text-foreground mb-2">
            Urun Secin
          </h3>
          <p className="text-sm text-muted-foreground max-w-md mx-auto">
            Maliyet gecmisini goruntulemek icin yukaridaki alandan bir urun secin.
            Her alim icin FIFO bazli maliyet lotlarini gorebilirsiniz.
          </p>
        </div>
      )}
    </div>
  );
}
