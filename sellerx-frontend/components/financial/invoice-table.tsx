"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import { cn } from "@/lib/utils";
import { useMediaQuery } from "@/hooks/useMediaQuery";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Search,
  ChevronDown,
  ChevronUp,
  ArrowUpDown,
  LayoutGrid,
  FileText,
  Receipt,
  Truck,
  Globe,
  AlertTriangle,
  Megaphone,
  RefreshCcw,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrency } from "@/lib/contexts/currency-context";
import type { InvoiceDetail, InvoiceCategory } from "@/types/invoice";
import { getCategoryDisplayName, INVOICE_CATEGORIES, fixInvoiceTypeName } from "@/types/invoice";

const INVOICES_PER_PAGE = 50;

// Column visibility configuration
interface ColumnConfig {
  id: string;
  label: string;
  defaultVisible: boolean;
  alwaysVisible?: boolean;
}

const INVOICE_COLUMN_CONFIG: ColumnConfig[] = [
  { id: "invoiceNumber", label: "Fatura No", defaultVisible: true, alwaysVisible: true },
  { id: "type", label: "Fatura Tipi", defaultVisible: true },
  { id: "category", label: "Kategori", defaultVisible: true },
  { id: "date", label: "Tarih", defaultVisible: true },
  { id: "amount", label: "Tutar", defaultVisible: true },
  { id: "vatAmount", label: "KDV", defaultVisible: true },
  { id: "orderNumber", label: "Sipariş No", defaultVisible: false },
  { id: "action", label: "", defaultVisible: true, alwaysVisible: true },
];

// Category icons
const categoryIcons: Record<string, React.ReactNode> = {
  KOMISYON: <Receipt className="h-4 w-4" />,
  KARGO: <Truck className="h-4 w-4" />,
  ULUSLARARASI: <Globe className="h-4 w-4" />,
  CEZA: <AlertTriangle className="h-4 w-4" />,
  REKLAM: <Megaphone className="h-4 w-4" />,
  IADE: <RefreshCcw className="h-4 w-4" />,
  DIGER: <FileText className="h-4 w-4" />,
};

// Category colors
const categoryColors: Record<string, string> = {
  KOMISYON: "text-blue-600 bg-blue-50 dark:bg-blue-900/20",
  KARGO: "text-amber-600 bg-amber-50 dark:bg-amber-900/20",
  ULUSLARARASI: "text-purple-600 bg-purple-50 dark:bg-purple-900/20",
  CEZA: "text-red-600 bg-red-50 dark:bg-red-900/20",
  REKLAM: "text-pink-600 bg-pink-50 dark:bg-pink-900/20",
  IADE: "text-green-600 bg-green-50 dark:bg-green-900/20",
  DIGER: "text-gray-600 bg-gray-50 dark:bg-gray-900/20",
};

// Get default visible columns from localStorage or use defaults
const getDefaultVisibleColumns = (storageKey: string, config: ColumnConfig[]): Set<string> => {
  if (typeof window !== "undefined") {
    const saved = localStorage.getItem(storageKey);
    if (saved) {
      try {
        return new Set(JSON.parse(saved));
      } catch {
        // Invalid JSON, use defaults
      }
    }
  }
  return new Set(config.filter((c) => c.defaultVisible).map((c) => c.id));
};

interface InvoiceTableProps {
  invoices: InvoiceDetail[];
  isLoading?: boolean;
  onInvoiceSelect?: (invoice: InvoiceDetail) => void;
  totalElements?: number;
  onLoadMore?: () => void;
  hasMore?: boolean;
  selectedCategory?: string | null;
}

// Skeleton row for loading state
function InvoiceRowSkeleton() {
  return (
    <TableRow>
      <TableCell>
        <Skeleton className="h-4 w-32" />
      </TableCell>
      <TableCell>
        <Skeleton className="h-4 w-24" />
      </TableCell>
      <TableCell>
        <Skeleton className="h-6 w-20" />
      </TableCell>
      <TableCell>
        <Skeleton className="h-4 w-20" />
      </TableCell>
      <TableCell className="text-right">
        <Skeleton className="h-4 w-16 ml-auto" />
      </TableCell>
      <TableCell className="text-right">
        <Skeleton className="h-4 w-12 ml-auto" />
      </TableCell>
      <TableCell>
        <Skeleton className="h-8 w-12" />
      </TableCell>
    </TableRow>
  );
}

// Mobile card skeleton
function InvoiceCardSkeleton() {
  return (
    <div className="bg-card border border-border rounded-lg p-4 space-y-3">
      <div className="flex justify-between items-start">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-5 w-20" />
      </div>
      <div className="flex justify-between items-center">
        <Skeleton className="h-6 w-24" />
        <Skeleton className="h-4 w-16" />
      </div>
    </div>
  );
}

// Mobile card component
interface InvoiceMobileCardProps {
  invoice: InvoiceDetail;
  onClick: () => void;
  formatCurrency: (value: number) => string;
}

function InvoiceMobileCard({ invoice, onClick, formatCurrency }: InvoiceMobileCardProps) {
  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString("tr-TR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div
      className="bg-card border border-border rounded-lg p-4 cursor-pointer hover:bg-muted/50 transition-colors active:bg-muted/70"
      onClick={onClick}
    >
      <div className="flex justify-between items-start mb-2">
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium truncate">{fixInvoiceTypeName(invoice.invoiceType)}</p>
          {invoice.invoiceNumber && (
            <p className="text-xs text-muted-foreground truncate">{invoice.invoiceNumber}</p>
          )}
        </div>
        <span
          className={cn(
            "ml-2 inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium shrink-0",
            categoryColors[invoice.invoiceCategory] || categoryColors.DIGER
          )}
        >
          {categoryIcons[invoice.invoiceCategory] || categoryIcons.DIGER}
          {getCategoryDisplayName(invoice.invoiceCategory)}
        </span>
      </div>
      <div className="flex justify-between items-center">
        <span
          className={cn(
            "text-base font-semibold",
            invoice.isDeduction ? "text-red-600" : "text-green-600"
          )}
        >
          {formatCurrency(invoice.amount)}
        </span>
        <span className="text-xs text-muted-foreground">
          {formatDate(invoice.invoiceDate)}
        </span>
      </div>
      {invoice.orderNumber && (
        <p className="text-xs text-muted-foreground mt-2 truncate">
          Siparis: {invoice.orderNumber}
        </p>
      )}
    </div>
  );
}

export function InvoiceTable({
  invoices,
  isLoading = false,
  onInvoiceSelect,
  totalElements,
  onLoadMore,
  hasMore = false,
  selectedCategory,
}: InvoiceTableProps) {
  const { formatCurrency } = useCurrency();
  const [searchQuery, setSearchQuery] = useState("");
  const [visibleCount, setVisibleCount] = useState(INVOICES_PER_PAGE);
  const isMobile = useMediaQuery("(max-width: 768px)");

  // Filter columns based on category
  // Each category has different data availability from the API
  const availableColumns = useMemo(() => {
    // Define which columns to hide per category
    const hiddenColumnsByCategory: Record<string, string[]> = {
      // KOMISYON: Some have orderNumber
      KOMISYON: [],
      // KARGO: Some have orderNumber
      KARGO: [],
      // ULUSLARARASI: Aggregated fees (no order)
      ULUSLARARASI: ["orderNumber"],
      // CEZA: Penalty invoices, orderNumber not available from API
      CEZA: ["orderNumber"],
      // REKLAM: Ad fees, orderNumber not available from API
      REKLAM: ["orderNumber"],
      // DIGER: Store-level deductions (campaigns, early payment), no order data
      DIGER: ["orderNumber"],
      // IADE: Refund payments from Trendyol (no order data)
      IADE: ["orderNumber"],
      // KESINTI: Deduction invoices, orderNumber not available from API
      KESINTI: ["orderNumber"],
    };

    const hiddenColumns = selectedCategory ? hiddenColumnsByCategory[selectedCategory] || [] : [];

    if (hiddenColumns.length === 0) {
      return INVOICE_COLUMN_CONFIG;
    }

    return INVOICE_COLUMN_CONFIG.filter((col) => !hiddenColumns.includes(col.id));
  }, [selectedCategory]);

  // Sorting state
  type SortField = "invoiceNumber" | "invoiceType" | "invoiceCategory" | "invoiceDate" | "amount" | "vatAmount";
  type SortDirection = "asc" | "desc";
  const [sortField, setSortField] = useState<SortField>("invoiceDate");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");

  // Column visibility state
  const [visibleColumns, setVisibleColumns] = useState<Set<string>>(() =>
    getDefaultVisibleColumns("invoice-table-columns", INVOICE_COLUMN_CONFIG)
  );

  // Save columns to localStorage
  useEffect(() => {
    localStorage.setItem("invoice-table-columns", JSON.stringify([...visibleColumns]));
  }, [visibleColumns]);

  // Toggle column visibility
  const toggleColumn = useCallback((columnId: string) => {
    const config = availableColumns.find((c) => c.id === columnId);
    if (config?.alwaysVisible) return;

    setVisibleColumns((prev) => {
      const next = new Set(prev);
      if (next.has(columnId)) {
        next.delete(columnId);
      } else {
        next.add(columnId);
      }
      return next;
    });
  }, [availableColumns]);

  // Handle column sort
  const handleColumnSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDirection("desc");
    }
  };

  // Filter and sort invoices
  const filteredInvoices = invoices
    .filter(
      (inv) =>
        (inv.invoiceNumber?.toLowerCase() || "").includes(searchQuery.toLowerCase()) ||
        (inv.invoiceType?.toLowerCase() || "").includes(searchQuery.toLowerCase()) ||
        (inv.orderNumber?.toLowerCase() || "").includes(searchQuery.toLowerCase())
    )
    .sort((a, b) => {
      let comparison = 0;

      switch (sortField) {
        case "invoiceNumber":
        case "invoiceType":
        case "invoiceCategory":
          comparison = (a[sortField] || "").localeCompare(b[sortField] || "", "tr");
          break;
        case "invoiceDate":
          comparison = new Date(a.invoiceDate).getTime() - new Date(b.invoiceDate).getTime();
          break;
        case "amount":
        case "vatAmount":
          comparison = (a[sortField] || 0) - (b[sortField] || 0);
          break;
      }

      return sortDirection === "asc" ? comparison : -comparison;
    });

  // Lazy loading
  const visibleInvoices = filteredInvoices.slice(0, visibleCount);
  const hasMoreInvoices = filteredInvoices.length > visibleCount || hasMore;
  const remainingCount = (totalElements || filteredInvoices.length) - visibleCount;

  // Reset visible count when search changes
  const handleSearchChange = (value: string) => {
    setSearchQuery(value);
    setVisibleCount(INVOICES_PER_PAGE);
  };

  // Load more invoices
  const handleLoadMore = () => {
    if (onLoadMore && visibleCount >= filteredInvoices.length) {
      onLoadMore();
    }
    setVisibleCount((prev) => prev + INVOICES_PER_PAGE);
  };

  // Format date for display
  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString("tr-TR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      });
    } catch {
      return dateStr;
    }
  };

  // Calculate visible column count for colSpan (use availableColumns)
  const visibleColumnCount = availableColumns.filter(
    (c) => c.alwaysVisible || visibleColumns.has(c.id)
  ).length;

  return (
    <div className="bg-card rounded-lg border border-border">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-border">
        {/* Search */}
        <div className="relative w-72">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Fatura ara..."
            value={searchQuery}
            onChange={(e) => handleSearchChange(e.target.value)}
            className="pl-9 h-9"
          />
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2">
          {/* Column Visibility Popover */}
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline" size="icon" className="h-8 w-8">
                <LayoutGrid className="h-4 w-4" />
              </Button>
            </PopoverTrigger>
            <PopoverContent align="end" className="w-56">
              <div className="space-y-3">
                <p className="text-sm font-medium text-foreground">Sütunlar</p>
                <div className="space-y-2">
                  {availableColumns.filter((c) => !c.alwaysVisible && c.id !== "action").map(
                    (col) => (
                      <label
                        key={col.id}
                        className="flex items-center gap-2 cursor-pointer hover:bg-muted/50 rounded px-1 py-0.5 -mx-1"
                      >
                        <Checkbox
                          checked={visibleColumns.has(col.id)}
                          onCheckedChange={() => toggleColumn(col.id)}
                          className={cn(
                            "border-2",
                            visibleColumns.has(col.id)
                              ? "border-[#1D70F1] bg-[#1D70F1] data-[state=checked]:bg-[#1D70F1] data-[state=checked]:border-[#1D70F1]"
                              : "border-gray-300 dark:border-gray-600 bg-transparent"
                          )}
                        />
                        <span
                          className={cn(
                            "text-sm",
                            visibleColumns.has(col.id)
                              ? "text-[#1D70F1] font-medium"
                              : "text-muted-foreground"
                          )}
                        >
                          {col.label}
                        </span>
                      </label>
                    )
                  )}
                </div>
              </div>
            </PopoverContent>
          </Popover>
        </div>
      </div>

      {/* Mobile Card View */}
      {isMobile ? (
        <div className="p-4 space-y-3">
          {isLoading ? (
            <>
              <InvoiceCardSkeleton />
              <InvoiceCardSkeleton />
              <InvoiceCardSkeleton />
              <InvoiceCardSkeleton />
              <InvoiceCardSkeleton />
            </>
          ) : filteredInvoices.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              {searchQuery ? "Arama sonucu bulunamadi" : "Henuz fatura verisi yok"}
            </div>
          ) : (
            visibleInvoices.map((invoice) => (
              <InvoiceMobileCard
                key={invoice.id}
                invoice={invoice}
                onClick={() => onInvoiceSelect?.(invoice)}
                formatCurrency={formatCurrency}
              />
            ))
          )}
        </div>
      ) : (
        /* Desktop Table View */
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                {availableColumns.some((c) => c.id === "invoiceNumber") && (
                  <TableHead
                    className="cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("invoiceNumber")}
                  >
                    <div className="flex items-center gap-1">
                      Fatura No
                      {sortField === "invoiceNumber" ? (
                        sortDirection === "asc" ? (
                          <ChevronUp className="h-3 w-3" />
                        ) : (
                          <ChevronDown className="h-3 w-3" />
                        )
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("type") && (
                  <TableHead
                    className="cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("invoiceType")}
                  >
                    <div className="flex items-center gap-1">
                      Fatura Tipi
                      {sortField === "invoiceType" ? (
                        sortDirection === "asc" ? (
                          <ChevronUp className="h-3 w-3" />
                        ) : (
                          <ChevronDown className="h-3 w-3" />
                        )
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("category") && (
                  <TableHead
                    className="cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("invoiceCategory")}
                  >
                    <div className="flex items-center gap-1">
                      Kategori
                      {sortField === "invoiceCategory" ? (
                        sortDirection === "asc" ? (
                          <ChevronUp className="h-3 w-3" />
                        ) : (
                          <ChevronDown className="h-3 w-3" />
                        )
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("date") && (
                  <TableHead
                    className="cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("invoiceDate")}
                  >
                    <div className="flex items-center gap-1">
                      Tarih
                      {sortField === "invoiceDate" ? (
                        sortDirection === "asc" ? (
                          <ChevronUp className="h-3 w-3" />
                        ) : (
                          <ChevronDown className="h-3 w-3" />
                        )
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("amount") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("amount")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      Tutar
                      {sortField === "amount" ? (
                        sortDirection === "asc" ? (
                          <ChevronUp className="h-3 w-3" />
                        ) : (
                          <ChevronDown className="h-3 w-3" />
                        )
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("vatAmount") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("vatAmount")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      KDV
                      {sortField === "vatAmount" ? (
                        sortDirection === "asc" ? (
                          <ChevronUp className="h-3 w-3" />
                        ) : (
                          <ChevronDown className="h-3 w-3" />
                        )
                      ) : (
                        <ArrowUpDown className="h-3 w-3 opacity-30" />
                      )}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("orderNumber") && availableColumns.some((c) => c.id === "orderNumber") && (
                  <TableHead>Siparis No</TableHead>
                )}
                <TableHead className="w-[60px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <>
                  <InvoiceRowSkeleton />
                  <InvoiceRowSkeleton />
                  <InvoiceRowSkeleton />
                  <InvoiceRowSkeleton />
                  <InvoiceRowSkeleton />
                </>
              ) : filteredInvoices.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={visibleColumnCount}
                    className="h-24 text-center text-muted-foreground"
                  >
                    {searchQuery ? "Arama sonucu bulunamadi" : "Henuz fatura verisi yok"}
                  </TableCell>
                </TableRow>
              ) : (
                visibleInvoices.map((invoice) => (
                  <TableRow
                    key={invoice.id}
                    className="hover:bg-muted/50 cursor-pointer"
                    onClick={() => onInvoiceSelect?.(invoice)}
                  >
                    {availableColumns.some((c) => c.id === "invoiceNumber") && (
                      <TableCell>
                        <span className="font-medium text-sm">{invoice.invoiceNumber || "-"}</span>
                      </TableCell>
                    )}
                    {visibleColumns.has("type") && (
                      <TableCell>
                        <span className="text-sm text-muted-foreground">{fixInvoiceTypeName(invoice.invoiceType)}</span>
                      </TableCell>
                    )}
                    {visibleColumns.has("category") && (
                      <TableCell>
                        <span
                          className={cn(
                            "inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-xs font-medium",
                            categoryColors[invoice.invoiceCategory] || categoryColors.DIGER
                          )}
                        >
                          {categoryIcons[invoice.invoiceCategory] || categoryIcons.DIGER}
                          {getCategoryDisplayName(invoice.invoiceCategory)}
                        </span>
                      </TableCell>
                    )}
                    {visibleColumns.has("date") && (
                      <TableCell>
                        <span className="text-sm">{formatDate(invoice.invoiceDate)}</span>
                      </TableCell>
                    )}
                    {visibleColumns.has("amount") && (
                      <TableCell className="text-right">
                        <span
                          className={cn(
                            "font-medium",
                            invoice.isDeduction ? "text-red-600" : "text-green-600"
                          )}
                        >
                          {formatCurrency(invoice.amount)}
                        </span>
                      </TableCell>
                    )}
                    {visibleColumns.has("vatAmount") && (
                      <TableCell className="text-right">
                        <span className="text-sm text-muted-foreground">
                          {formatCurrency(invoice.vatAmount)}
                        </span>
                      </TableCell>
                    )}
                    {visibleColumns.has("orderNumber") && availableColumns.some((c) => c.id === "orderNumber") && (
                      <TableCell>
                        <span className="text-sm text-muted-foreground">
                          {invoice.orderNumber || "-"}
                        </span>
                      </TableCell>
                    )}
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 text-[#1D70F1]"
                        onClick={(e) => {
                          e.stopPropagation();
                          onInvoiceSelect?.(invoice);
                        }}
                      >
                        Detay
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Footer - Load More */}
      {hasMoreInvoices && !isLoading && (
        <div className="p-4 border-t border-border text-center">
          <Button
            variant="outline"
            onClick={handleLoadMore}
            className="text-[#1D70F1] border-[#1D70F1] hover:bg-[#1D70F1]/10"
          >
            Daha fazla yükle (
            {Math.min(remainingCount > 0 ? remainingCount : INVOICES_PER_PAGE, INVOICES_PER_PAGE)} /{" "}
            {remainingCount > 0 ? remainingCount : "?"} kalan)
          </Button>
        </div>
      )}

      {/* Count info */}
      {filteredInvoices.length > 0 && (
        <div className="px-4 pb-3 text-center text-xs text-muted-foreground">
          {visibleInvoices.length} / {totalElements || filteredInvoices.length} fatura gösteriliyor
        </div>
      )}
    </div>
  );
}
