"use client";

import { useState, useCallback, useEffect, useMemo } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useProductsByStorePaginatedFull } from "@/hooks/queries/use-products";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Search, ChevronLeft, ChevronRight, Coins, Download, ExternalLink, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";
import type { TrendyolProduct, ProductFilters } from "@/types/product";
import { CostEditModal } from "@/components/products/cost-edit-modal";
import { ProductFiltersPopover } from "@/components/products/product-filters";
import { useCurrency } from "@/lib/contexts/currency-context";
import { FadeIn } from "@/components/motion";
import {
  FilterBarSkeleton,
  TableSkeleton,
  PaginationSkeleton,
} from "@/components/ui/skeleton-blocks";

// Get the latest (most recent) cost from costAndStockInfo array
function getLatestCost(product: TrendyolProduct): number | null {
  if (!product.costAndStockInfo || product.costAndStockInfo.length === 0) {
    return null;
  }
  // Sort by date descending and get the first one
  const sorted = [...product.costAndStockInfo].sort((a, b) =>
    new Date(b.stockDate).getTime() - new Date(a.stockDate).getTime()
  );
  return sorted[0].unitCost;
}

const PRODUCT_NAME_LIMIT = 40;

function truncateText(text: string, limit: number): string {
  if (text.length <= limit) return text;
  return text.slice(0, limit) + "...";
}

function ProductsPageSkeleton() {
  return (
    <div className="space-y-6">
      <FilterBarSkeleton showSearch={true} buttonCount={3} />
      <TableSkeleton columns={8} rows={10} showImage={true} />
      <PaginationSkeleton />
    </div>
  );
}

export default function ProductsPage() {
  const { formatCurrency } = useCurrency();
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [filters, setFilters] = useState<ProductFilters>({});
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [costModalOpen, setCostModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<TrendyolProduct | null>(null);
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchQuery);
      setPage(0); // Reset page when search changes
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Combine search with other filters - memoized for stable React Query key
  const activeFilters = useMemo<ProductFilters>(() => ({
    ...filters,
    search: debouncedSearch || undefined,
  }), [filters, debouncedSearch]);

  const { data, isLoading, error } = useProductsByStorePaginatedFull(storeId || undefined, {
    page,
    size: 50,
    sortBy: "onSale",
    sortDirection: "desc",
    filters: activeFilters,
  });

  // Handle filter changes - reset to page 0
  const handleFiltersChange = useCallback((newFilters: ProductFilters) => {
    setFilters(newFilters);
    setPage(0);
  }, []);

  const handleOpenCostModal = (product: TrendyolProduct) => {
    setSelectedProduct(product);
    setCostModalOpen(true);
  };

  // Excel Export - Download cost template with current data
  const handleExportExcel = async () => {
    if (!data?.products || data.products.length === 0) return;

    // Dynamic import for Excel libraries (reduces initial bundle size)
    const [ExcelJS, { saveAs }] = await Promise.all([
      import("exceljs").then((m) => m.default),
      import("file-saver"),
    ]);

    const todayStr = new Date().toISOString().split("T")[0];
    const exportData = data.products.map((product: TrendyolProduct) => {
      const latestCost = getLatestCost(product);
      return {
        "Barkod": product.barcode,
        "Ürün Adı": product.title,
        "Marka": product.brand,
        "Satış Fiyatı (TL)": product.salePrice,
        "Stok": product.trendyolQuantity,
        "Mevcut Maliyet (TL)": latestCost ?? "",
        "Yeni Maliyet (TL)": "",
        "Stok Tarihi (YYYY-MM-DD)": todayStr,
        "KDV Oranı (%)": latestCost ? (product.costAndStockInfo?.[0]?.costVatRate ?? 18) : 18,
        "Stok Adedi": "",
      };
    });

    const workbook = new ExcelJS.Workbook();
    const worksheet = workbook.addWorksheet("Ürün Maliyetleri");

    // Add header row
    const headers = Object.keys(exportData[0]);
    worksheet.addRow(headers);

    // Add data rows
    exportData.forEach((row) => {
      worksheet.addRow(Object.values(row));
    });

    // Set column widths
    const colWidths = [18, 50, 15, 15, 10, 18, 18, 22, 12, 12];
    worksheet.columns.forEach((col, i) => {
      col.width = colWidths[i] || 15;
    });

    // Write to buffer and save
    const buffer = await workbook.xlsx.writeBuffer();
    const blob = new Blob([buffer], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    saveAs(blob, `urun-maliyetleri-${todayStr}.xlsx`);
  };

  // Products are now filtered server-side
  const products = data?.products || [];

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  if (isLoading) return <ProductsPageSkeleton />;

  return (
    <FadeIn>
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            type="search"
            placeholder="Ürün ara..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-9 w-full pl-9 text-sm bg-gray-200 dark:bg-gray-800 border-0 rounded-md"
          />
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {/* Filter Popover */}
          <ProductFiltersPopover
            filters={filters}
            onFiltersChange={handleFiltersChange}
          />

          {/* Excel Export */}
          <Button
            onClick={handleExportExcel}
            disabled={!data?.products?.length}
            variant="outline"
            className="gap-2 border-gray-300 dark:border-gray-600 dark:bg-gray-800/50 dark:hover:bg-gray-700/50"
          >
            <Download className="h-4 w-4" />
            Excel İndir
          </Button>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <p className="text-red-800 dark:text-red-200 text-sm">Ürünler yüklenirken hata: {error.message}</p>
        </div>
      )}

      {/* Products Table */}
      <div className="bg-card rounded-lg border border-border">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="w-[80px]">Görsel</TableHead>
                <TableHead className="min-w-[300px]">Ürün</TableHead>
                <TableHead className="text-right">Fiyat</TableHead>
                <TableHead className="text-right">Maliyet</TableHead>
                <TableHead className="text-right">Stok</TableHead>
                <TableHead className="text-right">Komisyon</TableHead>
                <TableHead className="text-center">Durum</TableHead>
                <TableHead className="w-[100px]">İşlemler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {products.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="h-24 text-center text-muted-foreground">
                    Ürün bulunamadı
                  </TableCell>
                </TableRow>
              ) : (
                products.map((product: TrendyolProduct) => (
                  <TableRow key={product.id} className="hover:bg-muted/50">
                    <TableCell>
                      {/* Product Image - Trendyol Link */}
                      {product.productUrl ? (
                        <a
                          href={product.productUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex-shrink-0 group relative"
                        >
                          {product.image ? (
                            <img
                              src={product.image}
                              alt={product.title}
                              className="h-12 w-12 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                              onError={(e) => {
                                (e.target as HTMLImageElement).src = "https://via.placeholder.com/48?text=No+Image";
                              }}
                            />
                          ) : (
                            <div className="h-12 w-12 rounded flex items-center justify-center text-xs font-bold text-white group-hover:ring-2 ring-[#F27A1A] transition-all bg-[#F27A1A]">
                              T
                            </div>
                          )}
                          <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                            <ExternalLink className="h-2.5 w-2.5 text-white" />
                          </div>
                        </a>
                      ) : product.image ? (
                        <img
                          src={product.image}
                          alt={product.title}
                          className="h-12 w-12 rounded object-cover flex-shrink-0 border border-border"
                          onError={(e) => {
                            (e.target as HTMLImageElement).src = "https://via.placeholder.com/48?text=No+Image";
                          }}
                        />
                      ) : (
                        <div className="h-12 w-12 rounded flex items-center justify-center text-xs font-bold text-white flex-shrink-0 bg-[#F27A1A]">
                          T
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="min-w-0">
                        {product.title.length > PRODUCT_NAME_LIMIT ? (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              {product.productUrl ? (
                                <a
                                  href={product.productUrl}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="font-medium text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                                >
                                  {truncateText(product.title, PRODUCT_NAME_LIMIT)}
                                </a>
                              ) : (
                                <p className="font-medium text-sm text-foreground cursor-default">
                                  {truncateText(product.title, PRODUCT_NAME_LIMIT)}
                                </p>
                              )}
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[300px]">
                              <p>{product.title}</p>
                            </TooltipContent>
                          </Tooltip>
                        ) : product.productUrl ? (
                          <a
                            href={product.productUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="font-medium text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                          >
                            {product.title}
                          </a>
                        ) : (
                          <p className="font-medium text-sm text-foreground">
                            {product.title}
                          </p>
                        )}
                        <p className="text-xs text-muted-foreground mt-1">
                          Barkod: {product.barcode}
                        </p>
                        <p className="text-xs text-muted-foreground/70">
                          {product.brand} • {product.categoryName}
                        </p>
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <span className="font-medium text-foreground">
                        {formatCurrency(product.salePrice)}
                      </span>
                      <p className="text-xs text-muted-foreground">
                        KDV: %{product.vatRate}
                      </p>
                    </TableCell>
                    <TableCell className="text-right">
                      {(() => {
                        const cost = getLatestCost(product);
                        return cost !== null ? (
                          <span className="font-medium text-foreground">
                            {formatCurrency(cost)}
                          </span>
                        ) : (
                          <span className="text-muted-foreground text-sm">-</span>
                        );
                      })()}
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
                      <span className="font-medium text-foreground">
                        %{product.commissionRate}
                      </span>
                    </TableCell>
                    <TableCell className="text-center">
                      <div className="flex flex-wrap gap-1 justify-center">
                        {product.onSale && (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300 cursor-help">
                                Satışta
                              </span>
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[200px]">
                              Ürün şu anda Trendyol'da satışa açık
                            </TooltipContent>
                          </Tooltip>
                        )}
                        {product.approved && (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 cursor-help">
                                Onaylı
                              </span>
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[220px]">
                              Trendyol içerik ekibi tarafından incelenip onaylanmış
                            </TooltipContent>
                          </Tooltip>
                        )}
                        {product.archived && (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-muted text-muted-foreground cursor-help">
                                Arşivli
                              </span>
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[200px]">
                              Ürün arşive taşınmış, satışa kapalı
                            </TooltipContent>
                          </Tooltip>
                        )}
                        {product.rejected && (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 cursor-help">
                                Reddedildi
                              </span>
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[220px]">
                              Trendyol tarafından reddedildi (kural ihlali veya eksik bilgi)
                            </TooltipContent>
                          </Tooltip>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleOpenCostModal(product)}
                          className="h-8 text-foreground dark:bg-gray-700/50 dark:hover:bg-gray-600/50 border dark:border-gray-600"
                        >
                          <Coins className="h-4 w-4 mr-1" />
                          Maliyet
                        </Button>
                        {product.hasAutoDetectedCost && (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span
                                className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded text-[10px] font-medium bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 cursor-pointer"
                                onClick={() => handleOpenCostModal(product)}
                              >
                                <Sparkles className="h-3 w-3" />
                                Otomatik
                              </span>
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[250px]">
                              Stok artisi algilandi ve otomatik maliyet kaydi olusturuldu. Kontrol etmek icin tiklayin.
                            </TooltipContent>
                          </Tooltip>
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
          <div className="flex items-center justify-between px-4 py-3 border-t border-border">
            <p className="text-sm text-muted-foreground">
              Sayfa {data.currentPage + 1} / {data.totalPages} ({data.totalElements} ürün)
            </p>
            <div className="flex items-center gap-1">
              {/* Previous Button */}
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={data.isFirst}
                className="h-8 w-8 p-0"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>

              {/* Page Numbers */}
              {(() => {
                const totalPages = data.totalPages;
                const currentPage = data.currentPage;
                const pages: (number | string)[] = [];

                if (totalPages <= 7) {
                  // Show all pages if 7 or fewer
                  for (let i = 0; i < totalPages; i++) pages.push(i);
                } else {
                  // Always show first page
                  pages.push(0);

                  if (currentPage > 3) {
                    pages.push("...");
                  }

                  // Show pages around current
                  const start = Math.max(1, currentPage - 1);
                  const end = Math.min(totalPages - 2, currentPage + 1);

                  for (let i = start; i <= end; i++) {
                    if (!pages.includes(i)) pages.push(i);
                  }

                  if (currentPage < totalPages - 4) {
                    pages.push("...");
                  }

                  // Always show last page
                  if (!pages.includes(totalPages - 1)) {
                    pages.push(totalPages - 1);
                  }
                }

                return pages.map((p, idx) =>
                  p === "..." ? (
                    <span key={`ellipsis-${idx}`} className="px-2 text-muted-foreground">...</span>
                  ) : (
                    <Button
                      key={p}
                      variant={currentPage === p ? "default" : "outline"}
                      size="sm"
                      onClick={() => setPage(p as number)}
                      className={cn(
                        "h-8 w-8 p-0",
                        currentPage === p && "bg-primary text-primary-foreground"
                      )}
                    >
                      {(p as number) + 1}
                    </Button>
                  )
                );
              })()}

              {/* Next Button */}
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.isLast}
                className="h-8 w-8 p-0"
              >
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
    </FadeIn>
  );
}
