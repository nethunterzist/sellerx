"use client";

import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  ChevronDown,
  ChevronUp,
  ArrowUpDown,
  Package,
  ExternalLink,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { ProfitFilter } from "./profit-filters";

// Product data structure from dashboard stats
export interface ProductProfitData {
  productName: string;
  barcode: string;
  brand?: string;
  image?: string;
  categoryName?: string;
  revenue: number;
  grossProfit: number;
  totalSoldQuantity: number;
  returnQuantity?: number;
  estimatedCommission?: number;
  productCost?: number;
  productUrl?: string;
  // ============== REKLAM METRİKLERİ ==============
  cpc?: number;                    // Cost Per Click (TL)
  cvr?: number;                    // Conversion Rate (örn: 0.018 = %1.8)
  advertisingCostPerSale?: number; // Reklam Maliyeti = CPC / CVR
  acos?: number;                   // ACOS = (advertisingCostPerSale / salePrice) * 100
  totalAdvertisingCost?: number;   // Toplam reklam maliyeti
}

interface ProductProfitTableProps {
  products?: ProductProfitData[];
  isLoading?: boolean;
  formatCurrency: (value: number) => string;
  // Filters
  searchQuery?: string;
  profitFilter?: ProfitFilter;
  selectedCategory?: string | null;
  customMarginThreshold?: number | null; // Custom margin filter (e.g., 20 for >= 20%)
}

// Skeleton row for loading state
function ProductRowSkeleton() {
  return (
    <TableRow>
      <TableCell>
        <div className="flex items-start gap-3">
          <Skeleton className="h-10 w-10 rounded flex-shrink-0" />
          <div className="min-w-0 space-y-2">
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-3 w-32" />
          </div>
        </div>
      </TableCell>
      <TableCell><Skeleton className="h-4 w-24 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-8 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell>
        <div className="flex items-center gap-2">
          <Skeleton className="h-2 flex-1" />
          <Skeleton className="h-4 w-12" />
        </div>
      </TableCell>
      <TableCell><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
    </TableRow>
  );
}

const PRODUCTS_PER_PAGE = 50;
const PRODUCT_NAME_LIMIT = 50;

function truncateText(text: string, limit: number): string {
  if (text.length <= limit) return text;
  return text.slice(0, limit) + "...";
}

// Sort fields
type SortField = "productName" | "categoryName" | "totalSoldQuantity" | "revenue" | "productCost" | "estimatedCommission" | "grossProfit" | "netProfit" | "margin" | "acos";
type SortDirection = "asc" | "desc";

export function ProductProfitTable({
  products,
  isLoading,
  formatCurrency,
  searchQuery = "",
  profitFilter = "all",
  selectedCategory = null,
  customMarginThreshold = null,
}: ProductProfitTableProps) {
  const [visibleCount, setVisibleCount] = useState(PRODUCTS_PER_PAGE);
  const [sortField, setSortField] = useState<SortField>("margin");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");

  // Handle column sort
  const handleColumnSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDirection("desc");
    }
  };

  // Process and filter products
  const processedProducts = useMemo(() => {
    if (!products) return [];

    return products
      .map((p) => {
        const margin = p.revenue > 0 ? (p.grossProfit / p.revenue) * 100 : 0;
        const netProfit = p.grossProfit - (p.estimatedCommission || 0);
        return {
          ...p,
          margin,
          netProfit,
        };
      })
      .filter((p) => {
        // Search filter - searches in productName, barcode, and brand
        // Using Turkish locale for proper İ/ı character handling
        if (searchQuery) {
          const query = searchQuery.toLocaleLowerCase('tr-TR');
          const matchesSearch =
            p.productName.toLocaleLowerCase('tr-TR').includes(query) ||
            p.barcode.toLocaleLowerCase('tr-TR').includes(query) ||
            (p.brand?.toLocaleLowerCase('tr-TR').includes(query) ?? false);
          if (!matchesSearch) return false;
        }

        // Profit/Loss filter
        if (profitFilter === "profit" && p.grossProfit < 0) return false;
        if (profitFilter === "loss" && p.grossProfit >= 0) return false;

        // Custom margin threshold filter
        if (profitFilter === "custom" && customMarginThreshold !== null) {
          if (p.margin < customMarginThreshold) return false;
        }

        // Category filter
        if (selectedCategory && p.categoryName !== selectedCategory) return false;

        return true;
      })
      .sort((a, b) => {
        let comparison = 0;

        // String fields - alphabetical sorting
        if (sortField === "productName" || sortField === "categoryName") {
          const aVal = (a[sortField] || "") as string;
          const bVal = (b[sortField] || "") as string;
          comparison = aVal.localeCompare(bVal, "tr");
        }
        // Numeric fields
        else {
          const aVal = (a[sortField] ?? 0) as number;
          const bVal = (b[sortField] ?? 0) as number;
          comparison = aVal - bVal;
        }

        return sortDirection === "asc" ? comparison : -comparison;
      });
  }, [products, searchQuery, profitFilter, selectedCategory, customMarginThreshold, sortField, sortDirection]);

  // Lazy loading
  const visibleProducts = processedProducts.slice(0, visibleCount);
  const hasMoreProducts = processedProducts.length > visibleCount;
  const remainingCount = processedProducts.length - visibleCount;

  // Load more products
  const handleLoadMore = () => {
    setVisibleCount((prev) => prev + PRODUCTS_PER_PAGE);
  };

  // Render sort icon
  const renderSortIcon = (field: SortField) => {
    if (sortField === field) {
      return sortDirection === "asc" ? (
        <ChevronUp className="h-3 w-3" />
      ) : (
        <ChevronDown className="h-3 w-3" />
      );
    }
    return <ArrowUpDown className="h-3 w-3 opacity-30" />;
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Package className="h-5 w-5" />
            Urun Bazli Karlilik
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="w-[300px]">Urun</TableHead>
                <TableHead className="text-right">Kategori</TableHead>
                <TableHead className="text-right">Satilan</TableHead>
                <TableHead className="text-right">Gelir</TableHead>
                <TableHead className="text-right">Maliyet</TableHead>
                <TableHead className="text-right">Komisyon</TableHead>
                <TableHead className="text-right">Brut Kar</TableHead>
                <TableHead className="text-right">Net Kar</TableHead>
                <TableHead className="w-[150px]">Kar Marji</TableHead>
                <TableHead className="text-right">ACOS</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {[1, 2, 3, 4, 5].map((i) => (
                <ProductRowSkeleton key={i} />
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    );
  }

  if (!products || products.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Package className="h-5 w-5" />
            Urun Bazli Karlilik
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-12 text-muted-foreground">
            <Package className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p className="text-sm">Henuz urun verisi yok</p>
            <p className="text-xs mt-1">Satis yapmaya basladiginizda urunler burada gorunecek</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (processedProducts.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Package className="h-5 w-5" />
            Urun Bazli Karlilik
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-12 text-muted-foreground">
            <Package className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p className="text-sm">Filtrelere uyan urun bulunamadi</p>
            <p className="text-xs mt-1">Farkli filtreler deneyin</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <Package className="h-5 w-5" />
            Urun Bazli Karlilik
          </CardTitle>
          <span className="text-sm text-muted-foreground">
            {visibleProducts.length} / {processedProducts.length} urun
          </span>
        </div>
      </CardHeader>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead
                  className="w-[300px] cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("productName")}
                >
                  <div className="flex items-center gap-1">
                    Urun
                    {renderSortIcon("productName")}
                  </div>
                </TableHead>
                <TableHead
                  className="text-right cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("categoryName")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Kategori
                    {renderSortIcon("categoryName")}
                  </div>
                </TableHead>
                <TableHead
                  className="text-right cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("totalSoldQuantity")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Satilan
                    {renderSortIcon("totalSoldQuantity")}
                  </div>
                </TableHead>
                <TableHead
                  className="text-right cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("revenue")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Gelir
                    {renderSortIcon("revenue")}
                  </div>
                </TableHead>
                <TableHead
                  className="text-right cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("productCost")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Maliyet
                    {renderSortIcon("productCost")}
                  </div>
                </TableHead>
                <TableHead
                  className="text-right cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("estimatedCommission")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Komisyon
                    {renderSortIcon("estimatedCommission")}
                  </div>
                </TableHead>
                <TableHead
                  className="text-right cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("grossProfit")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Brut Kar
                    {renderSortIcon("grossProfit")}
                  </div>
                </TableHead>
                <TableHead
                  className="text-right cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("netProfit")}
                >
                  <div className="flex items-center justify-end gap-1">
                    Net Kar
                    {renderSortIcon("netProfit")}
                  </div>
                </TableHead>
                <TableHead
                  className="w-[150px] cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("margin")}
                >
                  <div className="flex items-center gap-1">
                    Kar Marji
                    {renderSortIcon("margin")}
                  </div>
                </TableHead>
                <TableHead
                  className="text-right cursor-pointer hover:bg-muted/50 select-none"
                  onClick={() => handleColumnSort("acos")}
                >
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <div className="flex items-center justify-end gap-1">
                        ACOS
                        {renderSortIcon("acos")}
                      </div>
                    </TooltipTrigger>
                    <TooltipContent>
                      <p>Advertising Cost of Sale</p>
                      <p className="text-xs text-muted-foreground">= (CPC / CVR) / Satış Fiyatı × 100</p>
                    </TooltipContent>
                  </Tooltip>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {visibleProducts.map((product) => (
                <TableRow
                  key={product.barcode}
                  className={cn(
                    "hover:bg-muted/50",
                    product.grossProfit < 0 && "bg-red-50/50 dark:bg-red-900/10"
                  )}
                >
                  <TableCell>
                    <div className="flex items-start gap-3">
                      {/* Product Image */}
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
                              alt={product.productName}
                              className="h-10 w-10 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                              onError={(e) => {
                                (e.target as HTMLImageElement).src =
                                  "https://via.placeholder.com/40?text=No";
                              }}
                            />
                          ) : (
                            <div className="h-10 w-10 rounded bg-[#F27A1A] flex items-center justify-center text-xs font-bold text-white group-hover:ring-2 ring-[#F27A1A] transition-all">
                              T
                            </div>
                          )}
                          <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                            <ExternalLink className="h-2 w-2 text-white" />
                          </div>
                        </a>
                      ) : product.image ? (
                        <img
                          src={product.image}
                          alt={product.productName}
                          className="h-10 w-10 rounded object-cover flex-shrink-0 border border-border"
                          onError={(e) => {
                            (e.target as HTMLImageElement).src =
                              "https://via.placeholder.com/40?text=No";
                          }}
                        />
                      ) : (
                        <div className="h-10 w-10 rounded bg-muted flex items-center justify-center text-xs font-medium text-muted-foreground flex-shrink-0">
                          ?
                        </div>
                      )}
                      <div className="min-w-0">
                        {product.productName.length > PRODUCT_NAME_LIMIT ? (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              {product.productUrl ? (
                                <a
                                  href={product.productUrl}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="font-medium text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                                >
                                  {truncateText(product.productName, PRODUCT_NAME_LIMIT)}
                                </a>
                              ) : (
                                <p className="font-medium text-sm text-foreground cursor-default">
                                  {truncateText(product.productName, PRODUCT_NAME_LIMIT)}
                                </p>
                              )}
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[300px]">
                              <p>{product.productName}</p>
                            </TooltipContent>
                          </Tooltip>
                        ) : product.productUrl ? (
                          <a
                            href={product.productUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="font-medium text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                          >
                            {product.productName}
                          </a>
                        ) : (
                          <p className="font-medium text-sm text-foreground">
                            {product.productName}
                          </p>
                        )}
                        <p className="text-xs text-muted-foreground">{product.barcode}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {product.categoryName || "-"}
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {product.totalSoldQuantity}
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {formatCurrency(product.revenue)}
                  </TableCell>
                  <TableCell className="text-right">
                    <span className="text-orange-600">
                      {formatCurrency(-(product.productCost || 0))}
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <span className="text-red-600">
                      {formatCurrency(-(product.estimatedCommission || 0))}
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <span
                      className={cn(
                        "font-medium",
                        product.grossProfit >= 0
                          ? "text-green-600 dark:text-green-400"
                          : "text-red-600 dark:text-red-400"
                      )}
                    >
                      {product.grossProfit >= 0 ? "+" : ""}
                      {formatCurrency(product.grossProfit)}
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <span
                      className={cn(
                        "font-medium",
                        product.netProfit >= 0
                          ? "text-green-600 dark:text-green-400"
                          : "text-red-600 dark:text-red-400"
                      )}
                    >
                      {product.netProfit >= 0 ? "+" : ""}
                      {formatCurrency(product.netProfit)}
                    </span>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <Progress
                        value={Math.min(Math.max(product.margin, 0), 100)}
                        className={cn(
                          "h-2 flex-1",
                          product.margin >= 20
                            ? "[&>div]:bg-green-500"
                            : product.margin >= 10
                            ? "[&>div]:bg-yellow-500"
                            : "[&>div]:bg-red-500"
                        )}
                      />
                      <span
                        className={cn(
                          "text-xs font-medium w-12 text-right",
                          product.margin >= 20
                            ? "text-green-600 dark:text-green-400"
                            : product.margin >= 10
                            ? "text-yellow-600 dark:text-yellow-400"
                            : "text-red-600 dark:text-red-400"
                        )}
                      >
                        {product.margin.toFixed(1)}%
                      </span>
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
                    {product.acos != null ? (
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <span
                            className={cn(
                              "font-medium",
                              product.acos <= 15
                                ? "text-green-600 dark:text-green-400"
                                : product.acos <= 30
                                ? "text-yellow-600 dark:text-yellow-400"
                                : "text-red-600 dark:text-red-400"
                            )}
                          >
                            {product.acos.toFixed(1)}%
                          </span>
                        </TooltipTrigger>
                        <TooltipContent>
                          <p>Reklam Maliyeti: {product.advertisingCostPerSale?.toFixed(2)} TL/satış</p>
                          {product.cpc != null && <p>CPC: {product.cpc.toFixed(2)} TL</p>}
                          {product.cvr != null && <p>CVR: {(product.cvr * 100).toFixed(2)}%</p>}
                        </TooltipContent>
                      </Tooltip>
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {/* Load More Button */}
        {hasMoreProducts && (
          <div className="p-4 border-t border-border text-center">
            <Button
              variant="outline"
              onClick={handleLoadMore}
              className="text-[#1D70F1] border-[#1D70F1] hover:bg-[#1D70F1]/10"
            >
              Daha fazla yukle ({Math.min(remainingCount, PRODUCTS_PER_PAGE)} / {remainingCount} kalan)
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
