"use client";

import { useState, useEffect, useMemo } from "react";
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
  Package,
  FileText,
  ExternalLink,
} from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  useAggregatedProducts,
  useCategoryCargoItems,
} from "@/hooks/queries/use-invoices";
import type {
  InvoiceDetail,
  AggregatedProduct,
  CargoInvoiceItem,
} from "@/types/invoice";
import { ProductCommissionPanel } from "./product-commission-panel";
import { ProductCargoPanel } from "./product-cargo-panel";

const ITEMS_PER_PAGE = 50;
const PRODUCT_NAME_LIMIT = 35;

function truncateText(text: string, limit: number): string {
  if (!text) return "-";
  if (text.length <= limit) return text;
  return text.slice(0, limit) + "...";
}

// Tab types
type TabType = "invoiceItems" | "products";

// Column configuration types
interface ColumnConfig {
  id: string;
  label: string;
  defaultVisible: boolean;
  alwaysVisible?: boolean;
}

// KARGO - Fatura Kalemleri columns (KOMISYON benzeri fatura odaklı)
const CARGO_ITEMS_COLUMN_CONFIG: ColumnConfig[] = [
  { id: "invoiceSerialNumber", label: "Fatura No", defaultVisible: true, alwaysVisible: true },
  { id: "orderNumber", label: "Sipariş No", defaultVisible: true },
  { id: "invoiceDate", label: "Fatura Tarihi", defaultVisible: true },
  { id: "amount", label: "Tutar", defaultVisible: true },
  { id: "desi", label: "Desi", defaultVisible: true },
  { id: "vatAmount", label: "KDV", defaultVisible: true },
];

// KOMISYON - Fatura Kalemleri columns (InvoiceDetail)
// Trendyol AZ Komisyon Faturası Excel ile uyumlu
// NOT: orderNumber, barcode, productName fatura seviyesinde boş geliyor, kaldırıldı
const COMMISSION_ITEMS_COLUMN_CONFIG: ColumnConfig[] = [
  { id: "invoiceNumber", label: "Fatura No", defaultVisible: true, alwaysVisible: true },
  { id: "invoiceType", label: "Fatura Tipi", defaultVisible: true },
  { id: "invoiceDate", label: "Fatura Tarihi", defaultVisible: true },
  { id: "amount", label: "Tutar (KDV Dahil)", defaultVisible: true },
  { id: "vatAmount", label: "KDV", defaultVisible: true },
];

// Products tab columns - KARGO
// Sıralama: Görsel → Ürün Adı → Barkod → Adet → Toplam Tutar → Toplam KDV → Fatura Sayısı → Toplam Desi → Detay
const KARGO_PRODUCTS_COLUMN_CONFIG: ColumnConfig[] = [
  { id: "productImage", label: "Görsel", defaultVisible: true, alwaysVisible: true },
  { id: "productName", label: "Ürün Adı", defaultVisible: true },
  { id: "barcode", label: "Barkod", defaultVisible: true },
  { id: "totalQuantity", label: "Adet", defaultVisible: true },
  { id: "totalAmount", label: "Toplam Tutar", defaultVisible: true },
  { id: "totalVatAmount", label: "Toplam KDV", defaultVisible: true },
  { id: "invoiceCount", label: "Fatura Sayısı", defaultVisible: true },
  { id: "totalDesi", label: "Toplam Desi", defaultVisible: true },
  { id: "actions", label: "Detay", defaultVisible: true, alwaysVisible: true },
];

// Products tab columns - KOMISYON
// Sıralama: Görsel → Ürün Adı → Barkod → Toplam Komisyon → Toplam KDV → Adet → Sipariş Sayısı → Detay
// Backend returns: barcode, productName, productImageUrl, productUrl, totalQuantity, totalCommission (as totalAmount), totalVatAmount, orderCount (as invoiceCount)
const KOMISYON_PRODUCTS_COLUMN_CONFIG: ColumnConfig[] = [
  { id: "productImage", label: "Görsel", defaultVisible: true, alwaysVisible: true },
  { id: "productName", label: "Ürün Adı", defaultVisible: true },
  { id: "barcode", label: "Barkod", defaultVisible: true },
  { id: "totalAmount", label: "Toplam Komisyon", defaultVisible: true },
  { id: "totalVatAmount", label: "Toplam KDV", defaultVisible: true },
  { id: "totalQuantity", label: "Adet", defaultVisible: true },
  { id: "invoiceCount", label: "Sipariş Sayısı", defaultVisible: true },
  { id: "actions", label: "Detay", defaultVisible: true, alwaysVisible: true },
];

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

interface InvoiceCategoryTableProps {
  category: "KARGO" | "KOMISYON";
  invoices: InvoiceDetail[];  // Parent'tan gelen veri
  isLoading?: boolean;
  totalElements?: number;
  onLoadMore?: () => void;
  hasMore?: boolean;
  // Ürünler tab'ı için
  storeId: string;
  startDate: string;
  endDate: string;
}

// Skeleton rows for loading state
function ItemRowSkeleton({ colCount }: { colCount: number }) {
  return (
    <TableRow>
      {Array.from({ length: colCount }).map((_, i) => (
        <TableCell key={i}>
          <Skeleton className="h-4 w-20" />
        </TableCell>
      ))}
    </TableRow>
  );
}

export function InvoiceCategoryTable({
  category,
  invoices = [],
  isLoading: parentIsLoading = false,
  totalElements: parentTotalElements = 0,
  onLoadMore,
  hasMore = false,
  storeId,
  startDate,
  endDate,
}: InvoiceCategoryTableProps) {
  const { formatCurrency } = useCurrency();

  // Tab state - Ürünler tab'ı varsayılan olarak ilk gelsin
  const [activeTab, setActiveTab] = useState<TabType>("products");

  // Search state
  const [searchQuery, setSearchQuery] = useState("");

  // Commission panel state (KOMISYON Ürünler detay paneli)
  const [selectedProduct, setSelectedProduct] = useState<AggregatedProduct | null>(null);
  const [commissionPanelOpen, setCommissionPanelOpen] = useState(false);

  // Cargo panel state (KARGO Ürünler detay paneli)
  const [selectedCargoProduct, setSelectedCargoProduct] = useState<AggregatedProduct | null>(null);
  const [cargoPanelOpen, setCargoPanelOpen] = useState(false);

  // Pagination states per tab
  const [productsVisibleCount, setProductsVisibleCount] = useState(ITEMS_PER_PAGE);
  const [cargoItemsPage, setCargoItemsPage] = useState(0);
  const [allCargoItems, setAllCargoItems] = useState<CargoInvoiceItem[]>([]);

  // Column visibility states (separate for each tab)
  const itemsColumnConfig = category === "KARGO" ? CARGO_ITEMS_COLUMN_CONFIG : COMMISSION_ITEMS_COLUMN_CONFIG;
  const productsColumnConfig = category === "KARGO" ? KARGO_PRODUCTS_COLUMN_CONFIG : KOMISYON_PRODUCTS_COLUMN_CONFIG;
  const itemsStorageKey = `invoice-${category.toLowerCase()}-items-columns`;
  const productsStorageKey = `invoice-${category.toLowerCase()}-products-columns`;

  const [itemsVisibleColumns, setItemsVisibleColumns] = useState<Set<string>>(() =>
    getDefaultVisibleColumns(itemsStorageKey, itemsColumnConfig)
  );
  const [productsVisibleColumns, setProductsVisibleColumns] = useState<Set<string>>(() =>
    getDefaultVisibleColumns(productsStorageKey, productsColumnConfig)
  );

  // Sorting states (separate for each tab)
  type ItemsSortField = "barcode" | "productName" | "orderNumber" | "amount" | "desi" | "invoiceDate" | "invoiceNumber" | "invoiceSerialNumber" | "invoiceType" | "vatAmount";
  type ProductsSortField = "barcode" | "productName" | "totalQuantity" | "totalAmount" | "totalVatAmount" | "invoiceCount" | "totalDesi";
  type SortDirection = "asc" | "desc";

  const [itemsSortField, setItemsSortField] = useState<ItemsSortField>("invoiceDate");
  const [itemsSortDirection, setItemsSortDirection] = useState<SortDirection>("desc");
  const [productsSortField, setProductsSortField] = useState<ProductsSortField>("totalAmount");
  const [productsSortDirection, setProductsSortDirection] = useState<SortDirection>("desc");

  // Fetch products data (this still uses its own hook)
  const {
    data: productsData,
    isLoading: productsLoading,
  } = useAggregatedProducts(
    storeId,
    category,
    startDate,
    endDate,
    activeTab === "products"
  );

  // Fetch cargo items data for KARGO category (use internal hook, not parent props)
  const {
    data: cargoItemsData,
    isLoading: cargoItemsLoading,
  } = useCategoryCargoItems(
    storeId,
    startDate,
    endDate,
    cargoItemsPage,
    ITEMS_PER_PAGE,
    category === "KARGO" && activeTab === "invoiceItems"
  );

  // Accumulate cargo items for lazy loading (only for KARGO)
  useEffect(() => {
    if (category === "KARGO" && cargoItemsData?.content) {
      if (cargoItemsPage === 0) {
        setAllCargoItems(cargoItemsData.content);
      } else {
        setAllCargoItems((prev) => {
          const newIds = new Set(cargoItemsData.content.map((i) => i.id));
          const filtered = prev.filter((i) => !newIds.has(i.id));
          return [...filtered, ...cargoItemsData.content];
        });
      }
    }
  }, [category, cargoItemsData, cargoItemsPage]);

  // Determine loading state based on category
  const itemsLoading = category === "KARGO" ? cargoItemsLoading : parentIsLoading;

  // Reset state when category/dates change
  useEffect(() => {
    setProductsVisibleCount(ITEMS_PER_PAGE);
    setCargoItemsPage(0);
    setAllCargoItems([]);
    setSearchQuery("");
    // Reset column visibility for the new category - Items tab
    const newItemsColumnConfig = category === "KARGO" ? CARGO_ITEMS_COLUMN_CONFIG : COMMISSION_ITEMS_COLUMN_CONFIG;
    const newItemsStorageKey = `invoice-${category.toLowerCase()}-items-columns`;
    setItemsVisibleColumns(getDefaultVisibleColumns(newItemsStorageKey, newItemsColumnConfig));
    // Reset column visibility for the new category - Products tab
    const newProductsColumnConfig = category === "KARGO" ? KARGO_PRODUCTS_COLUMN_CONFIG : KOMISYON_PRODUCTS_COLUMN_CONFIG;
    const newProductsStorageKey = `invoice-${category.toLowerCase()}-products-columns`;
    setProductsVisibleColumns(getDefaultVisibleColumns(newProductsStorageKey, newProductsColumnConfig));
    // Reset sorting to default for the category
    setItemsSortField("invoiceDate");
    setItemsSortDirection("desc");
    setProductsSortField("totalAmount");
    setProductsSortDirection("desc");
  }, [category, startDate, endDate]);

  // Save column visibility to localStorage
  useEffect(() => {
    localStorage.setItem(itemsStorageKey, JSON.stringify([...itemsVisibleColumns]));
  }, [itemsVisibleColumns, itemsStorageKey]);

  useEffect(() => {
    localStorage.setItem(productsStorageKey, JSON.stringify([...productsVisibleColumns]));
  }, [productsVisibleColumns, productsStorageKey]);

  // Toggle column visibility
  const toggleItemsColumn = (columnId: string) => {
    const config = itemsColumnConfig.find((c) => c.id === columnId);
    if (config?.alwaysVisible) return;
    setItemsVisibleColumns((prev) => {
      const next = new Set(prev);
      if (next.has(columnId)) next.delete(columnId);
      else next.add(columnId);
      return next;
    });
  };

  const toggleProductsColumn = (columnId: string) => {
    const config = productsColumnConfig.find((c) => c.id === columnId);
    if (config?.alwaysVisible) return;
    setProductsVisibleColumns((prev) => {
      const next = new Set(prev);
      if (next.has(columnId)) next.delete(columnId);
      else next.add(columnId);
      return next;
    });
  };

  // Handle sorting
  const handleItemsSort = (field: ItemsSortField) => {
    if (itemsSortField === field) {
      setItemsSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setItemsSortField(field);
      setItemsSortDirection("desc");
    }
  };

  const handleProductsSort = (field: ProductsSortField) => {
    if (productsSortField === field) {
      setProductsSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setProductsSortField(field);
      setProductsSortDirection("desc");
    }
  };

  // Filter items (use cargo items for KARGO, invoices prop for KOMISYON)
  const filteredItems = useMemo(() => {
    if (category === "KARGO") {
      // Use internally fetched cargo items for KARGO
      if (!allCargoItems || !Array.isArray(allCargoItems)) return [];
      return allCargoItems.filter((item: CargoInvoiceItem) =>
        (item.barcode?.toLowerCase() || "").includes(searchQuery.toLowerCase()) ||
        (item.productName?.toLowerCase() || "").includes(searchQuery.toLowerCase()) ||
        (item.orderNumber?.toLowerCase() || "").includes(searchQuery.toLowerCase()) ||
        (item.invoiceSerialNumber?.toLowerCase() || "").includes(searchQuery.toLowerCase())
      );
    } else {
      // Use invoices from props for KOMISYON
      // DDF... = Platform Hizmet Bedeli (ayrı cardda gösterilecek)
      // DCF.../AZC... = Komisyon Faturası (bu cardda gösterilecek)
      if (!invoices || !Array.isArray(invoices)) return [];
      return invoices
        // Filter out Platform Hizmet Bedeli (DDF...) - only show Komisyon Faturası (DCF/AZC)
        .filter((item: InvoiceDetail) => !item.invoiceNumber?.startsWith("DDF"))
        .filter((item: InvoiceDetail) =>
          (item.invoiceNumber?.toLowerCase() || "").includes(searchQuery.toLowerCase()) ||
          (item.invoiceType?.toLowerCase() || "").includes(searchQuery.toLowerCase())
        );
    }
  }, [category, allCargoItems, invoices, searchQuery]);

  // Sort items
  const sortedItems = useMemo(() => {
    return [...filteredItems].sort((a: any, b: any) => {
      let comparison = 0;
      switch (itemsSortField) {
        case "barcode":
        case "productName":
        case "orderNumber":
        case "invoiceNumber":
        case "invoiceType":
          comparison = (a[itemsSortField] || "").localeCompare(b[itemsSortField] || "", "tr");
          break;
        case "invoiceDate":
          comparison = new Date(a.invoiceDate || 0).getTime() - new Date(b.invoiceDate || 0).getTime();
          break;
        case "amount":
        case "desi":
        case "vatAmount":
          comparison = (a[itemsSortField] || 0) - (b[itemsSortField] || 0);
          break;
      }
      return itemsSortDirection === "asc" ? comparison : -comparison;
    });
  }, [filteredItems, itemsSortField, itemsSortDirection]);

  // Filter products
  const filteredProducts = useMemo(() => {
    if (!productsData?.products) return [];
    return productsData.products.filter((product) =>
      (product.barcode?.toLowerCase() || "").includes(searchQuery.toLowerCase()) ||
      (product.productName?.toLowerCase() || "").includes(searchQuery.toLowerCase())
    );
  }, [productsData, searchQuery]);

  // Sort products
  const sortedProducts = useMemo(() => {
    return [...filteredProducts].sort((a, b) => {
      let comparison = 0;
      switch (productsSortField) {
        case "barcode":
        case "productName":
          comparison = (a[productsSortField] || "").localeCompare(b[productsSortField] || "", "tr");
          break;
        case "totalQuantity":
        case "totalAmount":
        case "totalVatAmount":
        case "invoiceCount":
        case "totalDesi":
          comparison = (a[productsSortField] || 0) - (b[productsSortField] || 0);
          break;
      }
      return productsSortDirection === "asc" ? comparison : -comparison;
    });
  }, [filteredProducts, productsSortField, productsSortDirection]);

  const visibleProducts = sortedProducts.slice(0, productsVisibleCount);

  // Has more data
  const hasMoreItems = category === "KARGO"
    ? (cargoItemsData ? !cargoItemsData.last : false)
    : hasMore;
  const hasMoreProducts = sortedProducts.length > productsVisibleCount;

  // Total elements for display
  const itemsTotalElements = category === "KARGO"
    ? (cargoItemsData?.totalElements || 0)
    : parentTotalElements;

  // Load more handlers
  const handleLoadMoreItems = () => {
    if (category === "KARGO") {
      if (cargoItemsData && !cargoItemsData.last) {
        setCargoItemsPage((prev) => prev + 1);
      }
    } else {
      if (hasMore && onLoadMore) {
        onLoadMore();
      }
    }
  };

  const handleLoadMoreProducts = () => {
    setProductsVisibleCount((prev) => prev + ITEMS_PER_PAGE);
  };

  // Format date
  const formatDate = (dateStr?: string) => {
    if (!dateStr) return "-";
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

  // Calculate visible column count
  const itemsVisibleColCount = itemsColumnConfig.filter(
    (c) => c.alwaysVisible || itemsVisibleColumns.has(c.id)
  ).length;
  const productsVisibleColCount = productsColumnConfig.filter(
    (c) => c.alwaysVisible || productsVisibleColumns.has(c.id)
  ).length;

  // Get current tab's column config and visibility set
  const currentColumnConfig = activeTab === "invoiceItems" ? itemsColumnConfig : productsColumnConfig;
  const currentVisibleColumns = activeTab === "invoiceItems" ? itemsVisibleColumns : productsVisibleColumns;
  const currentToggleColumn = activeTab === "invoiceItems" ? toggleItemsColumn : toggleProductsColumn;

  const isLoading = activeTab === "invoiceItems" ? itemsLoading : productsLoading;

  return (
    <div className="bg-card rounded-lg border border-border">
      {/* Header with Tabs */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-4 border-b border-border">
        {/* Tabs - Ürünler ilk sırada */}
        <div className="flex items-center gap-1 bg-muted rounded-lg p-1">
          <Button
            variant={activeTab === "products" ? "secondary" : "ghost"}
            size="sm"
            className="h-8 gap-2"
            onClick={() => setActiveTab("products")}
          >
            <Package className="h-4 w-4" />
            Ürünler
          </Button>
          <Button
            variant={activeTab === "invoiceItems" ? "secondary" : "ghost"}
            size="sm"
            className="h-8 gap-2"
            onClick={() => setActiveTab("invoiceItems")}
          >
            <FileText className="h-4 w-4" />
            Fatura Kalemleri
          </Button>
        </div>

        {/* Search & Actions */}
        <div className="flex items-center gap-2">
          <div className="relative w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder={activeTab === "invoiceItems" ? "Kalem ara..." : "Ürün ara..."}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 h-9"
            />
          </div>

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
                  {currentColumnConfig.filter((c) => !c.alwaysVisible).map((col) => (
                    <label
                      key={col.id}
                      className="flex items-center gap-2 cursor-pointer hover:bg-muted/50 rounded px-1 py-0.5 -mx-1"
                    >
                      <Checkbox
                        checked={currentVisibleColumns.has(col.id)}
                        onCheckedChange={() => currentToggleColumn(col.id)}
                        className={cn(
                          "border-2",
                          currentVisibleColumns.has(col.id)
                            ? "border-[#1D70F1] bg-[#1D70F1] data-[state=checked]:bg-[#1D70F1] data-[state=checked]:border-[#1D70F1]"
                            : "border-gray-300 dark:border-gray-600 bg-transparent"
                        )}
                      />
                      <span
                        className={cn(
                          "text-sm",
                          currentVisibleColumns.has(col.id)
                            ? "text-[#1D70F1] font-medium"
                            : "text-muted-foreground"
                        )}
                      >
                        {col.label}
                      </span>
                    </label>
                  ))}
                </div>
              </div>
            </PopoverContent>
          </Popover>
        </div>
      </div>

      {/* Table Content */}
      <div className="overflow-x-auto">
        {activeTab === "invoiceItems" ? (
          // Items Table
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                {category === "KARGO" ? (
                  // KARGO columns - KOMISYON benzeri fatura odaklı
                  <>
                    <TableHead
                      className="cursor-pointer hover:bg-muted/50 select-none"
                      onClick={() => handleItemsSort("invoiceSerialNumber")}
                    >
                      <div className="flex items-center gap-1">
                        Fatura No
                        {itemsSortField === "invoiceSerialNumber" ? (
                          itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                        ) : (
                          <ArrowUpDown className="h-3 w-3 opacity-30" />
                        )}
                      </div>
                    </TableHead>
                    {itemsVisibleColumns.has("orderNumber") && (
                      <TableHead
                        className="cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleItemsSort("orderNumber")}
                      >
                        <div className="flex items-center gap-1">
                          Sipariş No
                          {itemsSortField === "orderNumber" ? (
                            itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {itemsVisibleColumns.has("invoiceDate") && (
                      <TableHead
                        className="cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleItemsSort("invoiceDate")}
                      >
                        <div className="flex items-center gap-1">
                          Fatura Tarihi
                          {itemsSortField === "invoiceDate" ? (
                            itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {itemsVisibleColumns.has("amount") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleItemsSort("amount")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Tutar
                          {itemsSortField === "amount" ? (
                            itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {itemsVisibleColumns.has("desi") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleItemsSort("desi")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Desi
                          {itemsSortField === "desi" ? (
                            itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {itemsVisibleColumns.has("vatAmount") && (
                      <TableHead className="text-right">KDV</TableHead>
                    )}
                  </>
                ) : (
                  // KOMISYON columns - Trendyol AZ Komisyon Faturası Excel uyumlu
                  <>
                    <TableHead
                      className="cursor-pointer hover:bg-muted/50 select-none"
                      onClick={() => handleItemsSort("invoiceNumber")}
                    >
                      <div className="flex items-center gap-1">
                        Fatura No
                        {itemsSortField === "invoiceNumber" ? (
                          itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                        ) : (
                          <ArrowUpDown className="h-3 w-3 opacity-30" />
                        )}
                      </div>
                    </TableHead>
                    {itemsVisibleColumns.has("invoiceType") && (
                      <TableHead
                        className="cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleItemsSort("invoiceType")}
                      >
                        <div className="flex items-center gap-1">
                          Fatura Tipi
                          {itemsSortField === "invoiceType" ? (
                            itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {itemsVisibleColumns.has("invoiceDate") && (
                      <TableHead
                        className="cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleItemsSort("invoiceDate")}
                      >
                        <div className="flex items-center gap-1">
                          Fatura Tarihi
                          {itemsSortField === "invoiceDate" ? (
                            itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {itemsVisibleColumns.has("amount") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleItemsSort("amount")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Tutar
                          {itemsSortField === "amount" ? (
                            itemsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {itemsVisibleColumns.has("vatAmount") && (
                      <TableHead className="text-right">KDV</TableHead>
                    )}
                  </>
                )}
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <>
                  <ItemRowSkeleton colCount={itemsVisibleColCount} />
                  <ItemRowSkeleton colCount={itemsVisibleColCount} />
                  <ItemRowSkeleton colCount={itemsVisibleColCount} />
                  <ItemRowSkeleton colCount={itemsVisibleColCount} />
                  <ItemRowSkeleton colCount={itemsVisibleColCount} />
                </>
              ) : sortedItems.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={itemsVisibleColCount} className="h-24 text-center text-muted-foreground">
                    {searchQuery ? "Arama sonucu bulunamadı" : "Henüz fatura kalemi yok"}
                  </TableCell>
                </TableRow>
              ) : (
                sortedItems.map((item: any, index: number) => (
                  <TableRow key={item.id || `${item.orderNumber}-${item.barcode}-${index}`} className="hover:bg-muted/50">
                    {category === "KARGO" ? (
                      // KARGO row - KOMISYON benzeri fatura odaklı
                      <>
                        <TableCell>
                          <span className="font-mono font-medium text-sm">{item.invoiceSerialNumber || "-"}</span>
                        </TableCell>
                        {itemsVisibleColumns.has("orderNumber") && (
                          <TableCell>
                            <span className="text-sm text-muted-foreground">{item.orderNumber || "-"}</span>
                          </TableCell>
                        )}
                        {itemsVisibleColumns.has("invoiceDate") && (
                          <TableCell>
                            <span className="text-sm">{formatDate(item.invoiceDate)}</span>
                          </TableCell>
                        )}
                        {itemsVisibleColumns.has("amount") && (
                          <TableCell className="text-right">
                            <span className="font-medium text-red-600">{formatCurrency(item.amount)}</span>
                          </TableCell>
                        )}
                        {itemsVisibleColumns.has("desi") && (
                          <TableCell className="text-right">
                            <span className="text-sm">{item.desi?.toFixed(2) || "-"}</span>
                          </TableCell>
                        )}
                        {itemsVisibleColumns.has("vatAmount") && (
                          <TableCell className="text-right">
                            <span className="text-sm text-muted-foreground">{formatCurrency(item.vatAmount || 0)}</span>
                          </TableCell>
                        )}
                      </>
                    ) : (
                      // KOMISYON row - Trendyol AZ Komisyon Faturası Excel uyumlu
                      <>
                        <TableCell>
                          <span className="font-mono font-medium text-sm">{item.invoiceNumber || "-"}</span>
                        </TableCell>
                        {itemsVisibleColumns.has("invoiceType") && (
                          <TableCell>
                            <span className="text-sm text-muted-foreground">{item.invoiceType || "-"}</span>
                          </TableCell>
                        )}
                        {itemsVisibleColumns.has("invoiceDate") && (
                          <TableCell>
                            <span className="text-sm">{formatDate(item.invoiceDate)}</span>
                          </TableCell>
                        )}
                        {itemsVisibleColumns.has("amount") && (
                          <TableCell className="text-right">
                            <span className={cn("font-medium", item.isDeduction ? "text-red-600" : "text-green-600")}>
                              {formatCurrency(item.amount)}
                            </span>
                          </TableCell>
                        )}
                        {itemsVisibleColumns.has("vatAmount") && (
                          <TableCell className="text-right">
                            <span className="text-sm text-muted-foreground">
                              {formatCurrency(item.vatAmount || 0)}
                            </span>
                          </TableCell>
                        )}
                      </>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        ) : (
          // Products Table
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                {category === "KOMISYON" ? (
                  // KOMISYON columns: Görsel → Ürün Adı → Barkod → Toplam Komisyon → Toplam KDV → Adet → Sipariş Sayısı
                  <>
                    <TableHead className="w-[60px]">Görsel</TableHead>
                    {productsVisibleColumns.has("productName") && (
                      <TableHead
                        className="min-w-[200px] cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("productName")}
                      >
                        <div className="flex items-center gap-1">
                          Ürün Adı
                          {productsSortField === "productName" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("barcode") && (
                      <TableHead
                        className="cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("barcode")}
                      >
                        <div className="flex items-center gap-1">
                          Barkod
                          {productsSortField === "barcode" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("totalAmount") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("totalAmount")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Toplam Komisyon
                          {productsSortField === "totalAmount" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("totalVatAmount") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("totalVatAmount")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Toplam KDV
                          {productsSortField === "totalVatAmount" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("totalQuantity") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("totalQuantity")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Adet
                          {productsSortField === "totalQuantity" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("invoiceCount") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("invoiceCount")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Sipariş Sayısı
                          {productsSortField === "invoiceCount" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {/* Detay Column - always visible for KOMISYON */}
                    <TableHead className="w-[80px] text-center">Detay</TableHead>
                  </>
                ) : (
                  // KARGO columns: Görsel → Ürün Adı → Barkod → Adet → Toplam Tutar → Toplam KDV → Fatura Sayısı → Toplam Desi → Detay
                  <>
                    <TableHead className="w-[60px]">Görsel</TableHead>
                    {productsVisibleColumns.has("productName") && (
                      <TableHead
                        className="min-w-[200px] cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("productName")}
                      >
                        <div className="flex items-center gap-1">
                          Ürün Adı
                          {productsSortField === "productName" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("barcode") && (
                      <TableHead
                        className="cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("barcode")}
                      >
                        <div className="flex items-center gap-1">
                          Barkod
                          {productsSortField === "barcode" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("totalQuantity") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("totalQuantity")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Adet
                          {productsSortField === "totalQuantity" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("totalAmount") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("totalAmount")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Toplam Tutar
                          {productsSortField === "totalAmount" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("totalVatAmount") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("totalVatAmount")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Toplam KDV
                          {productsSortField === "totalVatAmount" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("invoiceCount") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("invoiceCount")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Fatura Sayısı
                          {productsSortField === "invoiceCount" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {productsVisibleColumns.has("totalDesi") && (
                      <TableHead
                        className="text-right cursor-pointer hover:bg-muted/50 select-none"
                        onClick={() => handleProductsSort("totalDesi")}
                      >
                        <div className="flex items-center justify-end gap-1">
                          Toplam Desi
                          {productsSortField === "totalDesi" ? (
                            productsSortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-30" />
                          )}
                        </div>
                      </TableHead>
                    )}
                    {/* Detay Column - always visible for KARGO */}
                    <TableHead className="w-[80px] text-center">Detay</TableHead>
                  </>
                )}
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <>
                  <ItemRowSkeleton colCount={productsVisibleColCount} />
                  <ItemRowSkeleton colCount={productsVisibleColCount} />
                  <ItemRowSkeleton colCount={productsVisibleColCount} />
                  <ItemRowSkeleton colCount={productsVisibleColCount} />
                  <ItemRowSkeleton colCount={productsVisibleColCount} />
                </>
              ) : visibleProducts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={productsVisibleColCount} className="h-24 text-center text-muted-foreground">
                    {searchQuery ? "Arama sonucu bulunamadı" : "Henüz ürün verisi yok"}
                  </TableCell>
                </TableRow>
              ) : (
                visibleProducts.map((product: AggregatedProduct, index: number) => (
                  <TableRow key={product.barcode || index} className="hover:bg-muted/50">
                    {category === "KOMISYON" ? (
                      // KOMISYON row: Görsel → Ürün Adı → Barkod → Toplam Komisyon → Toplam KDV → Adet → Sipariş Sayısı
                      <>
                        {/* Product Image with Trendyol link */}
                        <TableCell>
                          {product.productUrl ? (
                            <a
                              href={product.productUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex-shrink-0 group relative"
                            >
                              {product.productImageUrl ? (
                                <img
                                  src={product.productImageUrl}
                                  alt={product.productName || "Ürün"}
                                  className="h-10 w-10 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                                  onError={(e) => {
                                    (e.target as HTMLImageElement).src = "https://via.placeholder.com/40?text=X";
                                  }}
                                />
                              ) : (
                                <div className="h-10 w-10 rounded flex items-center justify-center text-xs font-bold text-white group-hover:ring-2 ring-[#F27A1A] transition-all bg-[#F27A1A]">
                                  T
                                </div>
                              )}
                              <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                                <ExternalLink className="h-2.5 w-2.5 text-white" />
                              </div>
                            </a>
                          ) : product.productImageUrl ? (
                            <img
                              src={product.productImageUrl}
                              alt={product.productName || "Ürün"}
                              className="h-10 w-10 rounded object-cover flex-shrink-0 border border-border"
                              onError={(e) => {
                                (e.target as HTMLImageElement).src = "https://via.placeholder.com/40?text=X";
                              }}
                            />
                          ) : (
                            <div className="h-10 w-10 rounded flex items-center justify-center text-xs font-bold text-white flex-shrink-0 bg-gray-400">
                              ?
                            </div>
                          )}
                        </TableCell>
                        {/* Product Name with Tooltip */}
                        {productsVisibleColumns.has("productName") && (
                          <TableCell>
                            {product.productName && product.productName.length > PRODUCT_NAME_LIMIT ? (
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
                                    <span className="font-medium text-sm text-foreground cursor-default">
                                      {truncateText(product.productName, PRODUCT_NAME_LIMIT)}
                                    </span>
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
                                {product.productName || "-"}
                              </a>
                            ) : (
                              <span className="font-medium text-sm text-foreground">
                                {product.productName || "-"}
                              </span>
                            )}
                          </TableCell>
                        )}
                        {/* Barcode */}
                        {productsVisibleColumns.has("barcode") && (
                          <TableCell>
                            <span className="font-mono text-sm text-muted-foreground">{product.barcode}</span>
                          </TableCell>
                        )}
                        {/* Total Commission */}
                        {productsVisibleColumns.has("totalAmount") && (
                          <TableCell className="text-right">
                            <span className="font-medium text-red-600">{formatCurrency(product.totalAmount)}</span>
                          </TableCell>
                        )}
                        {/* Total VAT */}
                        {productsVisibleColumns.has("totalVatAmount") && (
                          <TableCell className="text-right">
                            <span className="text-sm text-muted-foreground">{formatCurrency(product.totalVatAmount || 0)}</span>
                          </TableCell>
                        )}
                        {/* Quantity */}
                        {productsVisibleColumns.has("totalQuantity") && (
                          <TableCell className="text-right">
                            <span className="text-sm font-medium">{product.totalQuantity}</span>
                          </TableCell>
                        )}
                        {/* Order Count */}
                        {productsVisibleColumns.has("invoiceCount") && (
                          <TableCell className="text-right">
                            <span className="text-sm">{product.invoiceCount}</span>
                          </TableCell>
                        )}
                        {/* Detay Button */}
                        <TableCell className="text-center">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 px-3 text-[#1D70F1] hover:text-[#1D70F1] hover:bg-[#1D70F1]/10"
                            onClick={() => {
                              setSelectedProduct(product);
                              setCommissionPanelOpen(true);
                            }}
                          >
                            Detay
                          </Button>
                        </TableCell>
                      </>
                    ) : (
                      // KARGO row: Görsel → Ürün Adı → Barkod → Adet → Toplam Tutar → Toplam KDV → Fatura Sayısı → Toplam Desi → Detay
                      <>
                        {/* Product Image with Trendyol link */}
                        <TableCell>
                          {product.productUrl ? (
                            <a
                              href={product.productUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex-shrink-0 group relative"
                            >
                              {product.productImageUrl ? (
                                <img
                                  src={product.productImageUrl}
                                  alt={product.productName || "Ürün"}
                                  className="h-10 w-10 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                                  onError={(e) => {
                                    (e.target as HTMLImageElement).src = "https://via.placeholder.com/40?text=X";
                                  }}
                                />
                              ) : (
                                <div className="h-10 w-10 rounded flex items-center justify-center text-xs font-bold text-white group-hover:ring-2 ring-[#F27A1A] transition-all bg-[#F27A1A]">
                                  T
                                </div>
                              )}
                              <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                                <ExternalLink className="h-2.5 w-2.5 text-white" />
                              </div>
                            </a>
                          ) : product.productImageUrl ? (
                            <img
                              src={product.productImageUrl}
                              alt={product.productName || "Ürün"}
                              className="h-10 w-10 rounded object-cover flex-shrink-0 border border-border"
                              onError={(e) => {
                                (e.target as HTMLImageElement).src = "https://via.placeholder.com/40?text=X";
                              }}
                            />
                          ) : (
                            <div className="h-10 w-10 rounded flex items-center justify-center text-xs font-bold text-white flex-shrink-0 bg-gray-400">
                              ?
                            </div>
                          )}
                        </TableCell>
                        {/* Product Name with Tooltip */}
                        {productsVisibleColumns.has("productName") && (
                          <TableCell>
                            {product.productName && product.productName.length > PRODUCT_NAME_LIMIT ? (
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
                                    <span className="font-medium text-sm text-foreground cursor-default">
                                      {truncateText(product.productName, PRODUCT_NAME_LIMIT)}
                                    </span>
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
                                {product.productName || "-"}
                              </a>
                            ) : (
                              <span className="font-medium text-sm text-foreground">
                                {product.productName || "-"}
                              </span>
                            )}
                          </TableCell>
                        )}
                        {/* Barcode */}
                        {productsVisibleColumns.has("barcode") && (
                          <TableCell>
                            <span className="font-mono text-sm text-muted-foreground">{product.barcode}</span>
                          </TableCell>
                        )}
                        {/* Quantity */}
                        {productsVisibleColumns.has("totalQuantity") && (
                          <TableCell className="text-right">
                            <span className="text-sm font-medium">{product.totalQuantity}</span>
                          </TableCell>
                        )}
                        {/* Total Amount */}
                        {productsVisibleColumns.has("totalAmount") && (
                          <TableCell className="text-right">
                            <span className="font-medium text-red-600">{formatCurrency(product.totalAmount)}</span>
                          </TableCell>
                        )}
                        {/* Total VAT */}
                        {productsVisibleColumns.has("totalVatAmount") && (
                          <TableCell className="text-right">
                            <span className="text-sm text-muted-foreground">{formatCurrency(product.totalVatAmount || 0)}</span>
                          </TableCell>
                        )}
                        {/* Invoice Count */}
                        {productsVisibleColumns.has("invoiceCount") && (
                          <TableCell className="text-right">
                            <span className="text-sm">{product.invoiceCount}</span>
                          </TableCell>
                        )}
                        {/* Total Desi */}
                        {productsVisibleColumns.has("totalDesi") && (
                          <TableCell className="text-right">
                            <span className="text-sm">{product.totalDesi?.toFixed(2) || "-"}</span>
                          </TableCell>
                        )}
                        {/* Detay Button */}
                        <TableCell className="text-center">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 px-3 text-amber-600 hover:text-amber-600 hover:bg-amber-600/10"
                            onClick={() => {
                              setSelectedCargoProduct(product);
                              setCargoPanelOpen(true);
                            }}
                          >
                            Detay
                          </Button>
                        </TableCell>
                      </>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </div>

      {/* Footer - Load More */}
      {activeTab === "invoiceItems" && hasMoreItems && !isLoading && (
        <div className="p-4 border-t border-border text-center">
          <Button
            variant="outline"
            onClick={handleLoadMoreItems}
            className="text-[#1D70F1] border-[#1D70F1] hover:bg-[#1D70F1]/10"
            disabled={itemsLoading}
          >
            Daha fazla yükle ({itemsTotalElements - sortedItems.length} kalan)
          </Button>
        </div>
      )}

      {activeTab === "products" && hasMoreProducts && !isLoading && (
        <div className="p-4 border-t border-border text-center">
          <Button
            variant="outline"
            onClick={handleLoadMoreProducts}
            className="text-[#1D70F1] border-[#1D70F1] hover:bg-[#1D70F1]/10"
          >
            Daha fazla yükle ({Math.min(ITEMS_PER_PAGE, sortedProducts.length - productsVisibleCount)} / {sortedProducts.length - productsVisibleCount} kalan)
          </Button>
        </div>
      )}

      {/* Count info */}
      <div className="px-4 pb-3 text-center text-xs text-muted-foreground">
        {activeTab === "invoiceItems" ? (
          <>
            {sortedItems.length} / {itemsTotalElements || sortedItems.length} fatura kalemi gösteriliyor
          </>
        ) : (
          <>
            {visibleProducts.length} / {productsData?.totalProducts || sortedProducts.length} ürün gösteriliyor
          </>
        )}
      </div>

      {/* Commission Detail Panel - KOMISYON Ürünler için */}
      {category === "KOMISYON" && (
        <ProductCommissionPanel
          open={commissionPanelOpen}
          onOpenChange={setCommissionPanelOpen}
          product={selectedProduct}
          storeId={storeId}
          startDate={startDate}
          endDate={endDate}
        />
      )}

      {/* Cargo Detail Panel - KARGO Ürünler için */}
      {category === "KARGO" && (
        <ProductCargoPanel
          open={cargoPanelOpen}
          onOpenChange={setCargoPanelOpen}
          product={selectedCargoProduct}
          storeId={storeId}
          startDate={startDate}
          endDate={endDate}
        />
      )}
    </div>
  );
}
