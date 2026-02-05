"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useStockValuation } from "@/hooks/queries/use-purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  ArrowLeft,
  Warehouse,
  Package,
  Clock,
  AlertTriangle,
  Search,
  ArrowUpDown,
  ChevronUp,
  ChevronDown,
} from "lucide-react";
import type { ProductValuation } from "@/types/purchasing";

type SortField = "fifoValue" | "quantity" | "daysInStock" | "averageCost";
type SortDirection = "asc" | "desc";

export default function StockValuationPage() {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
  const [searchTerm, setSearchTerm] = useState("");
  const [sortField, setSortField] = useState<SortField>("fifoValue");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");
  const [agingFilter, setAgingFilter] = useState<string | null>(null);

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: valuation, isLoading: valuationLoading } = useStockValuation(
    storeId || undefined
  );

  // Filter and sort products
  const filteredProducts = useMemo(() => {
    if (!valuation?.products) return [];

    let filtered = [...valuation.products];

    // Search filter
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.productName?.toLowerCase().includes(term) ||
          p.barcode?.toLowerCase().includes(term)
      );
    }

    // Aging filter
    if (agingFilter) {
      filtered = filtered.filter((p) => {
        switch (agingFilter) {
          case "0-30":
            return p.daysInStock <= 30;
          case "30-60":
            return p.daysInStock > 30 && p.daysInStock <= 60;
          case "60+":
            return p.daysInStock > 60;
          default:
            return true;
        }
      });
    }

    // Sort
    filtered.sort((a, b) => {
      const aVal = a[sortField];
      const bVal = b[sortField];
      if (sortDirection === "asc") {
        return (aVal || 0) > (bVal || 0) ? 1 : -1;
      }
      return (aVal || 0) < (bVal || 0) ? 1 : -1;
    });

    return filtered;
  }, [valuation?.products, searchTerm, sortField, sortDirection, agingFilter]);

  // Calculate totals for filtered products
  const filteredTotals = useMemo(() => {
    return {
      value: filteredProducts.reduce((sum, p) => sum + p.fifoValue, 0),
      quantity: filteredProducts.reduce((sum, p) => sum + p.quantity, 0),
    };
  }, [filteredProducts]);

  // Sort handler
  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDirection("desc");
    }
  };

  // Sort icon
  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) return <ArrowUpDown className="h-3 w-3" />;
    return sortDirection === "asc" ? (
      <ChevronUp className="h-3 w-3" />
    ) : (
      <ChevronDown className="h-3 w-3" />
    );
  };

  // Format date
  const formatDate = (dateString: string | undefined): string => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleDateString("tr-TR", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  };

  // Get age badge color
  const getAgeBadgeColor = (days: number): string => {
    if (days <= 30) return "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400";
    if (days <= 60) return "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400";
    if (days <= 90) return "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400";
    return "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400";
  };

  // No store selected
  if (!storeId && !storeLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <Warehouse className="h-16 w-16 text-muted-foreground/30 mb-4" />
        <h2 className="text-lg font-medium text-foreground mb-2">Magaza Secilmedi</h2>
        <p className="text-sm text-muted-foreground max-w-md">
          Stok degerleme raporunu goruntulemek icin lutfen bir magaza secin.
        </p>
      </div>
    );
  }

  // Loading state
  if (valuationLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <div className="h-9 w-24 bg-muted rounded animate-pulse" />
          <div className="h-8 w-64 bg-muted rounded animate-pulse" />
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-24 bg-muted rounded-xl animate-pulse" />
          ))}
        </div>
        <div className="h-96 bg-muted rounded-xl animate-pulse" />
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
          <h1 className="text-2xl font-bold text-foreground">Stok Degerleme Raporu</h1>
          <p className="text-sm text-muted-foreground mt-1">
            FIFO bazli stok degeri ve yaslama analizi
          </p>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {/* Total Value */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="p-2 rounded-lg bg-emerald-50 dark:bg-emerald-900/20">
              <Warehouse className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
            </div>
            <span className="text-xs text-muted-foreground">Toplam Deger</span>
          </div>
          <p className="text-2xl font-bold text-foreground">
            {formatCurrency(valuation?.totalValue || 0)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            {(valuation?.totalQuantity || 0).toLocaleString()} adet
          </p>
        </div>

        {/* Aging Breakdown */}
        <div
          className={cn(
            "rounded-xl border p-4 cursor-pointer transition-all",
            agingFilter === "0-30"
              ? "bg-green-50 dark:bg-green-900/20 border-green-300 dark:border-green-700"
              : "bg-card border-border hover:border-green-300 dark:hover:border-green-700"
          )}
          onClick={() => setAgingFilter(agingFilter === "0-30" ? null : "0-30")}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-medium text-green-600 dark:text-green-400">
              0-30 gun
            </span>
            {agingFilter === "0-30" && (
              <span className="text-xs text-green-600">Aktif</span>
            )}
          </div>
          <p className="text-xl font-bold text-foreground">
            {formatCurrency(valuation?.aging?.days0to30 || 0)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">Taze stok</p>
        </div>

        <div
          className={cn(
            "rounded-xl border p-4 cursor-pointer transition-all",
            agingFilter === "30-60"
              ? "bg-yellow-50 dark:bg-yellow-900/20 border-yellow-300 dark:border-yellow-700"
              : "bg-card border-border hover:border-yellow-300 dark:hover:border-yellow-700"
          )}
          onClick={() => setAgingFilter(agingFilter === "30-60" ? null : "30-60")}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-medium text-yellow-600 dark:text-yellow-400">
              30-60 gun
            </span>
            {agingFilter === "30-60" && (
              <span className="text-xs text-yellow-600">Aktif</span>
            )}
          </div>
          <p className="text-xl font-bold text-foreground">
            {formatCurrency(valuation?.aging?.days30to60 || 0)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">Normal</p>
        </div>

        <div
          className={cn(
            "rounded-xl border p-4 cursor-pointer transition-all",
            agingFilter === "60+"
              ? "bg-red-50 dark:bg-red-900/20 border-red-300 dark:border-red-700"
              : "bg-card border-border hover:border-red-300 dark:hover:border-red-700"
          )}
          onClick={() =>
            setAgingFilter(agingFilter === "60+" ? null : "60+")
          }
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-medium text-red-600 dark:text-red-400">
              60+ gun
            </span>
            {agingFilter === "60+" && (
              <span className="text-xs text-red-600">Aktif</span>
            )}
          </div>
          <p className="text-xl font-bold text-foreground">
            {formatCurrency(
              (valuation?.aging?.days60to90 || 0) + (valuation?.aging?.days90plus || 0)
            )}
          </p>
          <p className="text-xs text-muted-foreground mt-1">Dikkat gerektiren</p>
        </div>
      </div>

      {/* Search and Filter */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Urun adi veya barkod ile ara..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-lg border border-border bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
        {agingFilter && (
          <Button variant="outline" onClick={() => setAgingFilter(null)}>
            Filtreyi Temizle
          </Button>
        )}
      </div>

      {/* Filtered Totals */}
      {(searchTerm || agingFilter) && (
        <div className="bg-muted/30 rounded-lg p-3 flex items-center justify-between">
          <span className="text-sm text-muted-foreground">
            {filteredProducts.length} urun filtrelendi
          </span>
          <span className="text-sm font-medium text-foreground">
            Toplam: {formatCurrency(filteredTotals.value)} ({filteredTotals.quantity.toLocaleString()} adet)
          </span>
        </div>
      )}

      {/* Products Table */}
      <div className="bg-card rounded-xl border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                  Urun
                </th>
                <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                  Barkod
                </th>
                <th
                  className="text-right p-3 text-xs font-medium text-muted-foreground cursor-pointer hover:text-foreground"
                  onClick={() => handleSort("quantity")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Miktar
                    <SortIcon field="quantity" />
                  </div>
                </th>
                <th
                  className="text-right p-3 text-xs font-medium text-muted-foreground cursor-pointer hover:text-foreground"
                  onClick={() => handleSort("averageCost")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Ort. Maliyet
                    <SortIcon field="averageCost" />
                  </div>
                </th>
                <th
                  className="text-right p-3 text-xs font-medium text-muted-foreground cursor-pointer hover:text-foreground"
                  onClick={() => handleSort("fifoValue")}
                >
                  <div className="flex items-center justify-end gap-1">
                    FIFO Deger
                    <SortIcon field="fifoValue" />
                  </div>
                </th>
                <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                  En Eski Stok
                </th>
                <th
                  className="text-center p-3 text-xs font-medium text-muted-foreground cursor-pointer hover:text-foreground"
                  onClick={() => handleSort("daysInStock")}
                >
                  <div className="flex items-center justify-center gap-1">
                    Yas
                    <SortIcon field="daysInStock" />
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center">
                    <Package className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
                    <p className="text-sm text-muted-foreground">
                      {searchTerm || agingFilter
                        ? "Filtrelere uygun urun bulunamadi"
                        : "Stokta urun bulunamadi"}
                    </p>
                  </td>
                </tr>
              ) : (
                filteredProducts.map((product) => (
                  <tr
                    key={product.productId}
                    className="border-b border-border last:border-0 hover:bg-muted/20"
                  >
                    <td className="p-3">
                      <div className="flex items-center gap-3">
                        {product.productImage ? (
                          <img
                            src={product.productImage}
                            alt=""
                            className="w-10 h-10 rounded object-cover"
                          />
                        ) : (
                          <div className="w-10 h-10 rounded bg-muted flex items-center justify-center">
                            <Package className="h-5 w-5 text-muted-foreground" />
                          </div>
                        )}
                        <div className="max-w-[200px]">
                          <span className="text-sm font-medium text-foreground line-clamp-2">
                            {product.productName}
                          </span>
                          {product.stockDepleted && (
                            <span className="inline-flex items-center gap-0.5 mt-0.5 px-1.5 py-0.5 rounded text-[10px] font-medium bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">
                              <AlertTriangle className="h-2.5 w-2.5" />
                              Stok Tukendi
                            </span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="p-3 text-sm text-muted-foreground font-mono">
                      {product.barcode}
                    </td>
                    <td className="p-3 text-sm text-foreground text-right">
                      {product.quantity.toLocaleString()}
                    </td>
                    <td className="p-3 text-sm text-foreground text-right">
                      {formatCurrency(product.averageCost)}
                    </td>
                    <td className="p-3 text-sm font-medium text-foreground text-right">
                      {formatCurrency(product.fifoValue)}
                    </td>
                    <td className="p-3 text-sm text-muted-foreground">
                      {formatDate(product.oldestStockDate)}
                    </td>
                    <td className="p-3 text-center">
                      <span
                        className={cn(
                          "inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium",
                          getAgeBadgeColor(product.daysInStock)
                        )}
                      >
                        {product.daysInStock > 90 && (
                          <AlertTriangle className="h-3 w-3" />
                        )}
                        {product.daysInStock} gun
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Info Box */}
      <div className="bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-800 p-4">
        <div className="flex items-start gap-3">
          <Clock className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5" />
          <div>
            <p className="font-medium text-blue-900 dark:text-blue-100">
              Stok Yaslandirma Nedir?
            </p>
            <p className="text-sm text-blue-700 dark:text-blue-300 mt-1">
              Stok yaslandirma, urunlerin ne kadar suredir depoda bekledigini gosterir.
              Uzun sureli stoklar nakit akisini olumsuz etkiler ve bozulma/eskime riski tasir.
              60 gun uzerindeki stoklar icin indirim veya promosyon dusunulebilir.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
