"use client";

import { useState, useMemo } from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  ChevronUp,
  ChevronDown,
  Search,
  Package,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { ProductDetail } from "@/types/dashboard";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ProductPerformanceTableProps {
  products?: ProductDetail[];
  isLoading?: boolean;
  title?: string;
}

type SortField = "totalSoldQuantity" | "revenue" | "grossProfit" | "stock" | "returnQuantity";
type SortDirection = "asc" | "desc";

export function ProductPerformanceTable({
  products,
  isLoading,
  title = "Urun Performansi",
}: ProductPerformanceTableProps) {
  const { formatCurrency } = useCurrency();
  const [searchQuery, setSearchQuery] = useState("");
  const [sortField, setSortField] = useState<SortField>("totalSoldQuantity");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDirection("desc");
    }
  };

  const filteredAndSortedProducts = useMemo(() => {
    if (!products) return [];

    let filtered = products;

    // Filter by search query
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = products.filter(
        (p) =>
          p.productName.toLowerCase().includes(query) ||
          p.barcode.toLowerCase().includes(query)
      );
    }

    // Sort
    return [...filtered].sort((a, b) => {
      const aValue = a[sortField];
      const bValue = b[sortField];
      const multiplier = sortDirection === "asc" ? 1 : -1;
      return (aValue - bValue) * multiplier;
    });
  }, [products, searchQuery, sortField, sortDirection]);

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) return null;
    return sortDirection === "asc" ? (
      <ChevronUp className="h-4 w-4 inline ml-1" />
    ) : (
      <ChevronDown className="h-4 w-4 inline ml-1" />
    );
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Package className="h-5 w-5 text-indigo-500" />
            <Skeleton className="h-5 w-40" />
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-10 w-64 mb-4" />
          <div className="space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  const hasProducts = products && products.length > 0;

  return (
    <Card>
      <CardHeader className="pb-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Package className="h-5 w-5 text-indigo-500" />
              {title}
            </CardTitle>
            <CardDescription>
              {hasProducts
                ? `${filteredAndSortedProducts.length} urun listeleniyor`
                : "Henuz urun verisi yok"}
            </CardDescription>
          </div>
          {hasProducts && (
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Urun ara..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {!hasProducts ? (
          <div className="text-center py-12 text-muted-foreground">
            <Package className="h-12 w-12 mx-auto mb-3 opacity-30" />
            <p className="text-sm">Henuz urun verisi yok</p>
          </div>
        ) : filteredAndSortedProducts.length === 0 ? (
          <div className="text-center py-12 text-muted-foreground">
            <Search className="h-12 w-12 mx-auto mb-3 opacity-30" />
            <p className="text-sm">Aramanizla eslesen urun bulunamadi</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="min-w-[200px]">Urun</TableHead>
                  <TableHead
                    className="cursor-pointer hover:bg-muted text-right"
                    onClick={() => handleSort("totalSoldQuantity")}
                  >
                    Satis <SortIcon field="totalSoldQuantity" />
                  </TableHead>
                  <TableHead
                    className="cursor-pointer hover:bg-muted text-right"
                    onClick={() => handleSort("revenue")}
                  >
                    Gelir <SortIcon field="revenue" />
                  </TableHead>
                  <TableHead
                    className="cursor-pointer hover:bg-muted text-right"
                    onClick={() => handleSort("grossProfit")}
                  >
                    Kar <SortIcon field="grossProfit" />
                  </TableHead>
                  <TableHead
                    className="cursor-pointer hover:bg-muted text-right"
                    onClick={() => handleSort("returnQuantity")}
                  >
                    Iade <SortIcon field="returnQuantity" />
                  </TableHead>
                  <TableHead
                    className="cursor-pointer hover:bg-muted text-right"
                    onClick={() => handleSort("stock")}
                  >
                    Stok <SortIcon field="stock" />
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredAndSortedProducts.map((product) => {
                  const profitMargin =
                    product.revenue > 0
                      ? (product.grossProfit / product.revenue) * 100
                      : 0;
                  const isLowStock = product.stock <= 5;
                  const hasReturns = product.returnQuantity > 0;
                  const returnRate =
                    product.totalSoldQuantity > 0
                      ? (product.returnQuantity / product.totalSoldQuantity) * 100
                      : 0;

                  return (
                    <TableRow key={product.barcode}>
                      <TableCell>
                        <div className="flex items-start gap-3">
                          {product.image ? (
                            <img
                              src={product.image}
                              alt={product.productName}
                              className="w-10 h-10 rounded object-cover"
                            />
                          ) : (
                            <div className="w-10 h-10 rounded bg-muted flex items-center justify-center">
                              <Package className="h-5 w-5 text-muted-foreground" />
                            </div>
                          )}
                          <div className="min-w-0">
                            <p className="font-medium text-sm line-clamp-1">
                              {product.productName}
                            </p>
                            <p className="text-xs text-muted-foreground">
                              {product.barcode}
                            </p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="font-medium">
                          {product.totalSoldQuantity}
                        </span>
                        <span className="text-muted-foreground text-xs ml-1">adet</span>
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="font-medium">
                          {formatCurrency(product.revenue)}
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1">
                          {profitMargin >= 20 ? (
                            <TrendingUp className="h-3 w-3 text-green-500" />
                          ) : profitMargin < 10 ? (
                            <TrendingDown className="h-3 w-3 text-red-500" />
                          ) : null}
                          <span
                            className={cn(
                              "font-medium",
                              profitMargin >= 20
                                ? "text-green-600 dark:text-green-400"
                                : profitMargin < 10
                                ? "text-red-600 dark:text-red-400"
                                : ""
                            )}
                          >
                            {formatCurrency(product.grossProfit)}
                          </span>
                        </div>
                        <p className="text-xs text-muted-foreground">
                          %{profitMargin.toFixed(1)} marj
                        </p>
                      </TableCell>
                      <TableCell className="text-right">
                        {hasReturns ? (
                          <div>
                            <span
                              className={cn(
                                "font-medium",
                                returnRate > 10 ? "text-red-600 dark:text-red-400" : "text-orange-600 dark:text-orange-400"
                              )}
                            >
                              {product.returnQuantity}
                            </span>
                            <p className="text-xs text-muted-foreground">
                              %{returnRate.toFixed(1)}
                            </p>
                          </div>
                        ) : (
                          <span className="text-muted-foreground">-</span>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          {isLowStock && (
                            <AlertTriangle className="h-4 w-4 text-yellow-500" />
                          )}
                          <Badge
                            variant={
                              product.stock === 0
                                ? "destructive"
                                : isLowStock
                                ? "secondary"
                                : "outline"
                            }
                          >
                            {product.stock === 0 ? "Tukendi" : product.stock}
                          </Badge>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
