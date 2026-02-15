"use client";

import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import { Search, ChevronLeft, ChevronRight, ArrowUpDown, ImageOff, X, ExternalLink } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Checkbox } from "@/components/ui/checkbox";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { BuyboxProduct, BuyboxProductsResponse, BuyboxStatus } from "@/types/product";

interface BuyboxProductsTableProps {
  products?: BuyboxProductsResponse;
  isLoading?: boolean;
  page: number;
  onPageChange: (page: number) => void;
  selectedBarcodes: string[];
  onBarcodesChange: (barcodes: string[]) => void;
  allProducts: BuyboxProduct[];
  statusFilter: BuyboxStatus | "";
  onStatusFilterChange: (status: BuyboxStatus | "") => void;
}

function formatRelativeTime(dateStr: string | null): string {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffMin < 1) return "Az önce";
  if (diffMin < 60) return `${diffMin} dk önce`;
  if (diffHour < 24) return `${diffHour} saat önce`;
  if (diffDay < 7) return `${diffDay} gün önce`;
  return date.toLocaleDateString("tr-TR");
}

function getStatusBadge(status: BuyboxStatus) {
  switch (status) {
    case "WINNING":
      return <Badge className="bg-green-100 text-green-700 hover:bg-green-100 border-green-200">Kazanıyor</Badge>;
    case "LOSING":
      return <Badge className="bg-red-100 text-red-700 hover:bg-red-100 border-red-200">Kaybediyor</Badge>;
    case "NO_COMPETITION":
      return <Badge className="bg-gray-100 text-gray-600 hover:bg-gray-100 border-gray-200">Rakipsiz</Badge>;
    case "NOT_CHECKED":
      return <Badge className="bg-yellow-100 text-yellow-700 hover:bg-yellow-100 border-yellow-200">Kontrol Edilmedi</Badge>;
  }
}

function getOrderBadge(order: number | null) {
  if (order === null) return <span className="text-muted-foreground">-</span>;
  if (order === 1)
    return <Badge className="bg-green-100 text-green-700 hover:bg-green-100 border-green-200">1. sıra</Badge>;
  if (order <= 3)
    return <Badge className="bg-orange-100 text-orange-700 hover:bg-orange-100 border-orange-200">{order}. sıra</Badge>;
  return <Badge className="bg-red-100 text-red-700 hover:bg-red-100 border-red-200">{order}. sıra</Badge>;
}

export function BuyboxProductsTable({
  products,
  isLoading = false,
  page,
  onPageChange,
  selectedBarcodes,
  onBarcodesChange,
  allProducts,
  statusFilter,
  onStatusFilterChange,
}: BuyboxProductsTableProps) {
  const content = products?.content || [];
  const totalPages = products?.totalPages || 0;
  const totalElements = products?.totalElements || 0;

  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  const filteredPopoverProducts = useMemo(() => {
    if (!searchQuery) return allProducts;
    const q = searchQuery.toLowerCase();
    return allProducts.filter(
      (p) =>
        p.title.toLowerCase().includes(q) ||
        p.barcode.toLowerCase().includes(q)
    );
  }, [allProducts, searchQuery]);

  const selectedProductObjects = useMemo(
    () => allProducts.filter((p) => selectedBarcodes.includes(p.barcode)),
    [allProducts, selectedBarcodes]
  );

  const handleToggleProduct = (barcode: string) => {
    if (selectedBarcodes.includes(barcode)) {
      onBarcodesChange(selectedBarcodes.filter((b) => b !== barcode));
    } else {
      onBarcodesChange([...selectedBarcodes, barcode]);
    }
  };

  const handleRemoveProduct = (barcode: string, e: React.MouseEvent) => {
    e.stopPropagation();
    onBarcodesChange(selectedBarcodes.filter((b) => b !== barcode));
  };

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <Popover open={searchOpen} onOpenChange={setSearchOpen}>
          <PopoverTrigger asChild>
            <div className="flex-1 min-h-10 bg-gray-200 dark:bg-gray-800 rounded-md flex items-center gap-2 px-3 cursor-pointer hover:bg-gray-300 dark:hover:bg-gray-700 transition-colors">
              <Search className="h-4 w-4 text-muted-foreground flex-shrink-0" />
              {selectedProductObjects.length > 0 ? (
                <div className="flex items-center gap-1.5 flex-wrap flex-1 py-1.5">
                  {selectedProductObjects.slice(0, 2).map((product) => (
                    <span
                      key={product.barcode}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-[#1D70F1]/10 text-[#1D70F1] text-xs font-medium dark:bg-[#1D70F1]/20"
                    >
                      <span className="max-w-[120px] truncate">{product.title}</span>
                      <button
                        onClick={(e) => handleRemoveProduct(product.barcode, e)}
                        className="hover:bg-[#1D70F1]/20 rounded-full p-0.5 transition-colors"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </span>
                  ))}
                  {selectedProductObjects.length > 2 && (
                    <span className="text-xs text-muted-foreground font-medium px-1">
                      +{selectedProductObjects.length - 2}
                    </span>
                  )}
                </div>
              ) : (
                <span className="text-muted-foreground text-sm">Ürün ara...</span>
              )}
            </div>
          </PopoverTrigger>
          <PopoverContent className="w-[var(--radix-popover-trigger-width)] min-w-[400px] p-0" align="start">
            <div className="p-3 border-b border-border">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  type="text"
                  placeholder="Ürün ara..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                  autoFocus
                />
              </div>
            </div>
            <ScrollArea className="h-[300px]">
              <div className="p-2">
                {filteredPopoverProducts.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-4">
                    Ürün bulunamadı
                  </p>
                ) : (
                  filteredPopoverProducts.map((product) => {
                    const isSelected = selectedBarcodes.includes(product.barcode);
                    return (
                      <div
                        key={product.barcode}
                        className={cn(
                          "flex items-center gap-3 p-2 rounded-md cursor-pointer hover:bg-muted",
                          isSelected && "bg-blue-50 dark:bg-blue-900/20"
                        )}
                        onClick={() => handleToggleProduct(product.barcode)}
                      >
                        <Checkbox
                          checked={isSelected}
                          className="pointer-events-none"
                        />
                        {product.image && (
                          <img
                            src={product.image}
                            alt={product.title}
                            className="w-8 h-8 rounded object-cover"
                          />
                        )}
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium truncate">{product.title}</p>
                          <p className="text-xs text-muted-foreground">{product.barcode}</p>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </ScrollArea>
            <div className="p-3 border-t border-border flex items-center justify-between">
              <span className="text-sm text-muted-foreground">
                {selectedBarcodes.length} ürün seçili
              </span>
              <div className="flex gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onBarcodesChange([])}
                >
                  Temizle
                </Button>
                <Button
                  size="sm"
                  onClick={() => setSearchOpen(false)}
                >
                  Tamam
                </Button>
              </div>
            </div>
          </PopoverContent>
        </Popover>
        <Select
          value={statusFilter || "ALL"}
          onValueChange={(val) => onStatusFilterChange(val === "ALL" ? "" : val as BuyboxStatus)}
        >
          <SelectTrigger className="w-[180px]">
            <SelectValue placeholder="Durum Filtresi" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tüm Durumlar</SelectItem>
            <SelectItem value="WINNING">Kazanıyor</SelectItem>
            <SelectItem value="LOSING">Kaybediyor</SelectItem>
            <SelectItem value="NO_COMPETITION">Rakipsiz</SelectItem>
            <SelectItem value="NOT_CHECKED">Kontrol Edilmedi</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <div className="rounded-lg border border-border bg-card overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[60px]">Görsel</TableHead>
              <TableHead>Ürün Adı</TableHead>
              <TableHead className="text-right">Liste Fiyatın</TableHead>
              <TableHead className="text-right">Buybox Fiyatı</TableHead>
              <TableHead className="text-right">Fark</TableHead>
              <TableHead className="text-center">Sıran</TableHead>
              <TableHead className="text-center">Durum</TableHead>
              <TableHead className="text-right">Son Kontrol</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 10 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell><Skeleton className="h-10 w-10 rounded" /></TableCell>
                  <TableCell>
                    <div className="space-y-1">
                      <Skeleton className="h-4 w-48" />
                      <Skeleton className="h-3 w-24" />
                    </div>
                  </TableCell>
                  <TableCell className="text-right"><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
                  <TableCell className="text-right"><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
                  <TableCell className="text-right"><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
                  <TableCell className="text-center"><Skeleton className="h-5 w-14 mx-auto" /></TableCell>
                  <TableCell className="text-center"><Skeleton className="h-5 w-20 mx-auto" /></TableCell>
                  <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
                </TableRow>
              ))
            ) : content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="h-32 text-center">
                  <div className="flex flex-col items-center gap-2 text-muted-foreground">
                    <ArrowUpDown className="h-8 w-8" />
                    <p className="text-sm">
                      {selectedBarcodes.length > 0 || statusFilter
                        ? "Filtrelere uygun ürün bulunamadı"
                        : "Henüz buybox verisi yok."}
                    </p>
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              content.map((product) => (
                <TableRow key={product.productId}>
                  <TableCell>
                    {product.productUrl ? (
                      <a
                        href={product.productUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex-shrink-0 group relative block"
                      >
                        {product.image ? (
                          <img
                            src={product.image}
                            alt={product.title}
                            className="h-10 w-10 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                            loading="lazy"
                          />
                        ) : (
                          <div className="h-10 w-10 rounded bg-muted flex items-center justify-center group-hover:ring-2 ring-[#F27A1A] transition-all">
                            <ImageOff className="h-4 w-4 text-muted-foreground" />
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
                        className="h-10 w-10 rounded object-cover"
                        loading="lazy"
                      />
                    ) : (
                      <div className="h-10 w-10 rounded bg-muted flex items-center justify-center">
                        <ImageOff className="h-4 w-4 text-muted-foreground" />
                      </div>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="min-w-0">
                      {product.title.length > 40 ? (
                        <Tooltip>
                          <TooltipTrigger asChild>
                            {product.productUrl ? (
                              <a
                                href={product.productUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-sm font-medium text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer truncate max-w-[300px] block"
                              >
                                {product.title.slice(0, 40)}...
                              </a>
                            ) : (
                              <p className="text-sm font-medium text-foreground truncate max-w-[300px]">
                                {product.title.slice(0, 40)}...
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
                          className="text-sm font-medium text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                        >
                          {product.title}
                        </a>
                      ) : (
                        <p className="text-sm font-medium text-foreground">
                          {product.title}
                        </p>
                      )}
                      <p className="text-xs text-muted-foreground">{product.barcode}</p>
                    </div>
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {product.salePrice.toFixed(2)} TL
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {product.buyboxPrice !== null ? `${product.buyboxPrice.toFixed(2)} TL` : "-"}
                  </TableCell>
                  <TableCell className="text-right">
                    {product.priceDifference !== null ? (
                      <span
                        className={cn(
                          "font-medium",
                          product.priceDifference > 0 && "text-red-600",
                          product.priceDifference < 0 && "text-green-600",
                          product.priceDifference === 0 && "text-muted-foreground"
                        )}
                      >
                        {product.priceDifference > 0 ? "+" : ""}
                        {product.priceDifference.toFixed(2)} TL
                      </span>
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </TableCell>
                  <TableCell className="text-center">
                    {getOrderBadge(product.buyboxOrder)}
                  </TableCell>
                  <TableCell className="text-center">
                    {getStatusBadge(product.buyboxStatus)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {formatRelativeTime(product.buyboxUpdatedAt)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Toplam {totalElements} ürün
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(page - 1)}
              disabled={page === 0}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-sm text-muted-foreground">
              {page + 1} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
