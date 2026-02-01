"use client";

import { useState, useEffect } from "react";
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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Checkbox } from "@/components/ui/checkbox";
import { Search, ChevronDown, ChevronUp, Download, MoreHorizontal, ArrowUpDown, ExternalLink, LayoutGrid } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { ProductDetailPanel, type ProductDetailData } from "./product-detail-panel";
import { OrderDetailPanel } from "./order-detail-panel";
import { useCurrency } from "@/lib/contexts/currency-context";
import type { OrderDetail, OrderDetailPanelData } from "@/types/dashboard";

interface ProductStatus {
  onSale: boolean;
  approved: boolean;
  hasActiveCampaign: boolean;
  archived: boolean;
  blacklisted: boolean;
  rejected?: boolean;
}

interface CostHistoryItem {
  stockDate: string;
  quantity: number;
  unitCost: number;
  costVatRate?: number;
  usedQuantity?: number;
}

interface Product {
  id: string;
  name: string;
  sku: string;
  image?: string;
  cogs: number;
  stock: number;
  marketplace: "trendyol" | "hepsiburada";
  unitsSold: number;
  refunds: number;
  sales: number;
  grossProfit: number;
  netProfit: number;
  margin: number;
  roi: number;
  commission?: number;

  // ============== YENİ: 32 Metrik Alanları ==============

  // İndirimler & Kuponlar
  sellerDiscount?: number;
  platformDiscount?: number;
  couponDiscount?: number;
  totalDiscount?: number;

  // Net Ciro
  netRevenue?: number;

  // Maliyetler
  productCost?: number;
  shippingCost?: number;
  refundCost?: number;

  // Oranlar
  refundRate?: number;
  profitMargin?: number;

  // İade detayları
  returnQuantity?: number;

  // TrendyolProduct'tan gelen ek veriler
  categoryName?: string;
  brand?: string;
  salePrice?: number;
  vatRate?: number;
  commissionRate?: number;
  trendyolQuantity?: number;
  productUrl?: string;
  status?: ProductStatus;
  costHistory?: CostHistoryItem[];
}

interface ProductsTableProps {
  products?: Product[];
  orders?: OrderDetail[];
  isLoading?: boolean;
}

// Demo products data
const demoProducts: Product[] = [
  {
    id: "1",
    name: "Small Gift Box 5.5*5*2.5cm",
    sku: "SKU 41",
    cogs: 11.99,
    stock: 0,
    marketplace: "trendyol",
    unitsSold: 14,
    refunds: 2,
    sales: 161.86,
    grossProfit: 76.94,
    netProfit: 76.94,
    margin: 48,
    roi: 450,
  },
  {
    id: "2",
    name: "Jewelry Packaging Gift Box 2.5*2.5*3cm",
    sku: "SKU 2",
    cogs: 11.99,
    stock: 1613,
    marketplace: "trendyol",
    unitsSold: 11,
    refunds: 0,
    sales: 103.89,
    grossProfit: 44.20,
    netProfit: 44.20,
    margin: 43,
    roi: 309,
  },
  {
    id: "3",
    name: "Paper Jewelry Earring Storage Box",
    sku: "SKU 78",
    cogs: 11.99,
    stock: 0,
    marketplace: "hepsiburada",
    unitsSold: 7,
    refunds: 0,
    sales: 83.93,
    grossProfit: 45.71,
    netProfit: 45.71,
    margin: 54,
    roi: 653,
  },
  {
    id: "4",
    name: "Corrugated Box 11*6*4cm",
    sku: "SKU 76",
    cogs: 23.99,
    stock: 0,
    marketplace: "trendyol",
    unitsSold: 7,
    refunds: 1,
    sales: 173.53,
    grossProfit: 56.74,
    netProfit: 56.74,
    margin: 33,
    roi: 135,
  },
  {
    id: "5",
    name: "Carton Black Boxes 2 pieces",
    sku: "SKU 47",
    cogs: 7.99,
    stock: 0,
    marketplace: "hepsiburada",
    unitsSold: 5,
    refunds: 1,
    sales: 46.95,
    grossProfit: -2.26,
    netProfit: -2.26,
    margin: -5,
    roi: -17,
  },
];

const PRODUCT_NAME_LIMIT = 40;

function truncateText(text: string, limit: number): string {
  if (text.length <= limit) return text;
  return text.slice(0, limit) + "...";
}

// Skeleton row for loading state
function ProductRowSkeleton() {
  return (
    <TableRow>
      <TableCell>
        <div className="flex items-start gap-3">
          <Skeleton className="h-12 w-12 rounded flex-shrink-0" />
          <div className="min-w-0 space-y-2">
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-3 w-32" />
            <Skeleton className="h-3 w-20" />
          </div>
        </div>
      </TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-8 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-6 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-10 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-6 w-12" /></TableCell>
    </TableRow>
  );
}

// Skeleton row for order items loading state
function OrderItemRowSkeleton({ visibleColumns }: { visibleColumns: Set<string> }) {
  return (
    <TableRow>
      <TableCell>
        <div className="min-w-0 space-y-2">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-3 w-40" />
          <Skeleton className="h-3 w-20" />
        </div>
      </TableCell>
      <TableCell>
        <div className="min-w-0 space-y-2">
          <Skeleton className="h-4 w-48" />
          <Skeleton className="h-3 w-24" />
        </div>
      </TableCell>
      {visibleColumns.has("quantity") && (
        <TableCell className="text-right"><Skeleton className="h-4 w-8 ml-auto" /></TableCell>
      )}
      {visibleColumns.has("orderSales") && (
        <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      )}
      {visibleColumns.has("cost") && (
        <TableCell className="text-right"><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
      )}
      {visibleColumns.has("orderCommission") && (
        <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      )}
      {visibleColumns.has("orderGrossProfit") && (
        <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      )}
      {visibleColumns.has("orderMargin") && (
        <TableCell className="text-right"><Skeleton className="h-4 w-10 ml-auto" /></TableCell>
      )}
      {visibleColumns.has("orderRoi") && (
        <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
      )}
      <TableCell><Skeleton className="h-6 w-12" /></TableCell>
    </TableRow>
  );
}

const PRODUCTS_PER_PAGE = 50;

// Column visibility configuration
interface ColumnConfig {
  id: string;
  label: string;
  defaultVisible: boolean;
  alwaysVisible?: boolean;
}

// Products tab column config
const PRODUCT_COLUMN_CONFIG: ColumnConfig[] = [
  { id: "product", label: "Ürün", defaultVisible: true, alwaysVisible: true },
  { id: "unitsSold", label: "Satılan", defaultVisible: true },
  { id: "refunds", label: "İade", defaultVisible: true },
  { id: "sales", label: "Satış", defaultVisible: true },
  { id: "commission", label: "Komisyon", defaultVisible: true },
  { id: "grossProfit", label: "Brüt Kâr", defaultVisible: true },
  { id: "netProfit", label: "Net Kâr", defaultVisible: true },
  { id: "margin", label: "Marj", defaultVisible: true },
  { id: "roi", label: "ROI", defaultVisible: true },
  { id: "action", label: "Detay", defaultVisible: true, alwaysVisible: true },
];

// Order Items tab column config
const ORDER_COLUMN_CONFIG: ColumnConfig[] = [
  { id: "order", label: "Sipariş", defaultVisible: true, alwaysVisible: true },
  { id: "orderProduct", label: "Ürün", defaultVisible: true, alwaysVisible: true },
  { id: "quantity", label: "Adet", defaultVisible: true },
  { id: "orderSales", label: "Satış", defaultVisible: true },
  { id: "cost", label: "Maliyet", defaultVisible: true },
  { id: "orderCommission", label: "Komisyon", defaultVisible: true },
  { id: "orderGrossProfit", label: "Brüt Kâr", defaultVisible: true },
  { id: "orderMargin", label: "Marj", defaultVisible: true },
  { id: "orderRoi", label: "ROI", defaultVisible: true },
  { id: "orderAction", label: "Detay", defaultVisible: true, alwaysVisible: true },
];

// Backward compatibility alias
const COLUMN_CONFIG = PRODUCT_COLUMN_CONFIG;

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
  return new Set(config.filter(c => c.defaultVisible).map(c => c.id));
};

export function ProductsTable({ products, orders, isLoading }: ProductsTableProps) {
  const { formatCurrency } = useCurrency();
  const [activeTab, setActiveTab] = useState<"products" | "orders">("products");
  const [searchQuery, setSearchQuery] = useState("");
  const [detailPanelOpen, setDetailPanelOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<ProductDetailData | null>(null);
  const [visibleCount, setVisibleCount] = useState(PRODUCTS_PER_PAGE);

  // OrderDetailPanel state
  const [orderDetailOpen, setOrderDetailOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<OrderDetailPanelData | null>(null);

  // ========== PRODUCTS TAB STATES ==========
  // Sorting state for products
  type ProductSortField = "name" | "sku" | "brand" | "unitsSold" | "refunds" | "sales" | "commission" | "grossProfit" | "netProfit" | "margin" | "roi";
  type SortDirection = "asc" | "desc";
  const [productSortField, setProductSortField] = useState<ProductSortField>("sales");
  const [productSortDirection, setProductSortDirection] = useState<SortDirection>("desc");

  // Column visibility state for products
  const [productVisibleColumns, setProductVisibleColumns] = useState<Set<string>>(() =>
    getDefaultVisibleColumns("dashboard-product-columns", PRODUCT_COLUMN_CONFIG)
  );

  // ========== ORDER ITEMS TAB STATES ==========
  // Sorting state for orders
  type OrderSortField = "orderNumber" | "orderDate" | "productName" | "quantity" | "totalPrice" | "cost" | "commission" | "profit" | "margin" | "roi";
  const [orderSortField, setOrderSortField] = useState<OrderSortField>("orderDate");
  const [orderSortDirection, setOrderSortDirection] = useState<SortDirection>("desc");

  // Column visibility state for orders
  const [orderVisibleColumns, setOrderVisibleColumns] = useState<Set<string>>(() =>
    getDefaultVisibleColumns("dashboard-order-columns", ORDER_COLUMN_CONFIG)
  );

  // ========== BACKWARD COMPATIBILITY ==========
  // Alias for backward compatibility
  const sortField = productSortField;
  const sortDirection = productSortDirection;
  const visibleColumns = productVisibleColumns;

  // Product column sort handler
  const handleColumnSort = (field: ProductSortField) => {
    if (productSortField === field) {
      setProductSortDirection(prev => prev === "asc" ? "desc" : "asc");
    } else {
      setProductSortField(field);
      setProductSortDirection("desc");
    }
  };

  // Order column sort handler
  const handleOrderColumnSort = (field: OrderSortField) => {
    if (orderSortField === field) {
      setOrderSortDirection(prev => prev === "asc" ? "desc" : "asc");
    } else {
      setOrderSortField(field);
      setOrderSortDirection("desc");
    }
  };

  // Save product columns to localStorage
  useEffect(() => {
    localStorage.setItem("dashboard-product-columns", JSON.stringify([...productVisibleColumns]));
  }, [productVisibleColumns]);

  // Save order columns to localStorage
  useEffect(() => {
    localStorage.setItem("dashboard-order-columns", JSON.stringify([...orderVisibleColumns]));
  }, [orderVisibleColumns]);

  // Toggle product column visibility
  const toggleProductColumn = (columnId: string) => {
    const config = PRODUCT_COLUMN_CONFIG.find(c => c.id === columnId);
    if (config?.alwaysVisible) return;

    setProductVisibleColumns(prev => {
      const next = new Set(prev);
      if (next.has(columnId)) {
        next.delete(columnId);
      } else {
        next.add(columnId);
      }
      return next;
    });
  };

  // Toggle order column visibility
  const toggleOrderColumn = (columnId: string) => {
    const config = ORDER_COLUMN_CONFIG.find(c => c.id === columnId);
    if (config?.alwaysVisible) return;

    setOrderVisibleColumns(prev => {
      const next = new Set(prev);
      if (next.has(columnId)) {
        next.delete(columnId);
      } else {
        next.add(columnId);
      }
      return next;
    });
  };

  // Backward compatibility alias
  const toggleColumn = toggleProductColumn;

  // Calculate visible column count for colSpan
  const visibleColumnCount = PRODUCT_COLUMN_CONFIG.filter(c =>
    c.alwaysVisible || productVisibleColumns.has(c.id)
  ).length;

  const orderVisibleColumnCount = ORDER_COLUMN_CONFIG.filter(c =>
    c.alwaysVisible || orderVisibleColumns.has(c.id)
  ).length;

  // Product'ı detay paneli formatına dönüştür (32 Metrik)
  const handleOpenDetail = (product: Product) => {
    // İndirim toplamı hesaplama
    const totalDiscount = product.totalDiscount ??
      ((product.sellerDiscount ?? 0) + (product.platformDiscount ?? 0) + (product.couponDiscount ?? 0));

    // Net ciro hesaplama
    const netRevenue = product.netRevenue ?? (product.sales - totalDiscount);

    // Ürün maliyeti
    const productCost = product.productCost ?? product.cogs;

    // İade oranı
    const refundRate = product.refundRate ??
      (product.unitsSold > 0 ? (product.refunds / product.unitsSold) * 100 : 0);

    // Kar marjı
    const profitMargin = product.profitMargin ?? product.margin;

    const detailData: ProductDetailData = {
      id: product.id,
      name: product.name,
      sku: product.sku,
      barcode: product.sku,
      image: product.image,

      // ============== TEMEL METRİKLER ==============
      sales: product.sales,
      units: product.unitsSold,
      returnQuantity: product.returnQuantity ?? product.refunds,

      // ============== İNDİRİMLER & KUPONLAR ==============
      sellerDiscount: product.sellerDiscount ?? 0,
      platformDiscount: product.platformDiscount ?? 0,
      couponDiscount: product.couponDiscount ?? 0,
      totalDiscount: totalDiscount,

      // ============== NET CİRO ==============
      netRevenue: netRevenue,

      // ============== MALİYETLER ==============
      productCost: productCost,
      shippingCost: product.shippingCost ?? 0,
      refundCost: product.refundCost ?? 0,

      // ============== KOMİSYON ==============
      commission: product.commission ?? 0,

      // ============== KÂR METRİKLERİ ==============
      grossProfit: product.grossProfit,
      netProfit: product.netProfit,

      // ============== ORANLAR ==============
      refundRate: refundRate,
      profitMargin: profitMargin,
      roi: product.roi,

      // ============== ESKİ ALANLAR (Geriye Uyumluluk) ==============
      promo: 0, // Deprecate edildi, indirimler ayrı gösteriliyor
      costOfGoods: productCost,
      refundPercentage: refundRate,
      margin: profitMargin,

      // TrendyolProduct'tan gelen ek veriler
      categoryName: product.categoryName,
      brand: product.brand,
      salePrice: product.salePrice,
      vatRate: product.vatRate,
      commissionRate: product.commissionRate,
      trendyolQuantity: product.trendyolQuantity,
      productUrl: product.productUrl,
      status: product.status,
      costHistory: product.costHistory,
    };
    setSelectedProduct(detailData);
    setDetailPanelOpen(true);
  };

  // Don't use demo data - only show real data or empty/loading state
  const filteredProducts = (products?.filter(
    (p) =>
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.sku.toLowerCase().includes(searchQuery.toLowerCase())
  ) || []).sort((a, b) => {
    let comparison = 0;

    // String fields - alphabetical sorting
    if (sortField === "name" || sortField === "sku" || sortField === "brand") {
      const aVal = a[sortField] || "";
      const bVal = b[sortField] || "";
      comparison = aVal.localeCompare(bVal, "tr");
    }
    // Numeric fields - numerical sorting
    else {
      const aVal = a[sortField] ?? 0;
      const bVal = b[sortField] ?? 0;
      comparison = aVal - bVal;
    }

    return sortDirection === "asc" ? comparison : -comparison;
  });

  // Lazy loading: show only visibleCount products
  const visibleProducts = filteredProducts.slice(0, visibleCount);
  const hasMoreProducts = filteredProducts.length > visibleCount;
  const remainingCount = filteredProducts.length - visibleCount;

  // Reset visible count when search changes
  const handleSearchChange = (value: string) => {
    setSearchQuery(value);
    setVisibleCount(PRODUCTS_PER_PAGE);
  };

  // Load more products
  const handleLoadMore = () => {
    setVisibleCount((prev) => prev + PRODUCTS_PER_PAGE);
  };

  // Flatten orders into individual order items (one row per product per order)
  interface OrderItem {
    id: string;
    orderNumber: string;
    orderDate: string;
    productName: string;
    barcode: string;
    quantity: number;
    unitPrice: number;
    totalPrice: number;
    cost: number;
    profit: number;
    orderTotalPrice: number;
    returnPrice: number;
    commission: number;
    stoppage: number;
    productUrl?: string;
  }

  // Create a barcode to productUrl lookup map
  const barcodeToProductUrl = new Map<string, string>();
  (products || []).forEach((p) => {
    if (p.sku && p.productUrl) {
      barcodeToProductUrl.set(p.sku, p.productUrl);
    }
  });

  const flattenedOrderItems: OrderItem[] = (orders || []).flatMap((order) =>
    (order.products || []).map((product, idx) => ({
      id: `${order.orderNumber}-${idx}`,
      orderNumber: order.orderNumber || "",
      orderDate: order.orderDate || "",
      productName: product.productName || "Bilinmeyen Ürün",
      barcode: product.barcode || "",
      quantity: product.quantity ?? 0,
      unitPrice: product.unitPrice ?? 0,
      totalPrice: product.totalPrice ?? 0,
      cost: product.cost ?? 0,
      profit: product.profit ?? 0,
      orderTotalPrice: order.totalPrice ?? 0,
      returnPrice: order.returnPrice ?? 0,
      commission: product.commission ?? 0, // Product-level commission from backend
      stoppage: (order.stoppage ?? 0) / (order.products?.length || 1),
      productUrl: barcodeToProductUrl.get(product.barcode || ""),
    }))
  );

  // Filter and sort order items
  const filteredOrderItems = flattenedOrderItems
    .filter(
      (item) =>
        item.productName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.barcode.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.orderNumber.toLowerCase().includes(searchQuery.toLowerCase())
    )
    .sort((a, b) => {
      let comparison = 0;

      // String fields - alphabetical sorting
      if (orderSortField === "orderNumber" || orderSortField === "productName") {
        const aVal = orderSortField === "orderNumber" ? a.orderNumber : a.productName;
        const bVal = orderSortField === "orderNumber" ? b.orderNumber : b.productName;
        comparison = aVal.localeCompare(bVal, "tr");
      }
      // Date field
      else if (orderSortField === "orderDate") {
        comparison = new Date(a.orderDate).getTime() - new Date(b.orderDate).getTime();
      }
      // Numeric fields
      else {
        const fieldMap: Record<string, keyof OrderItem> = {
          quantity: "quantity",
          totalPrice: "totalPrice",
          cost: "cost",
          commission: "commission",
          profit: "profit",
        };
        const field = fieldMap[orderSortField] || "totalPrice";
        const aVal = (a[field] as number) ?? 0;
        const bVal = (b[field] as number) ?? 0;
        comparison = aVal - bVal;
      }

      return orderSortDirection === "asc" ? comparison : -comparison;
    });

  // Lazy loading for order items
  const visibleOrderItems = filteredOrderItems.slice(0, visibleCount);
  const hasMoreOrderItems = filteredOrderItems.length > visibleCount;
  const remainingOrderItemsCount = filteredOrderItems.length - visibleCount;

  // Handle order row click - open OrderDetailPanel
  const handleOrderClick = (orderNumber: string) => {
    const order = orders?.find(o => o.orderNumber === orderNumber);
    if (order) {
      const totalProductCost = order.products.reduce((sum, p) => sum + (p.totalCost || 0), 0);
      const shippingCost = order.estimatedShippingCost || 0;
      const netProfit = order.grossProfit - order.estimatedCommission - shippingCost - order.stoppage;
      const profitMargin = order.revenue > 0 ? (order.grossProfit / order.revenue) * 100 : 0;
      const roi = totalProductCost > 0 ? (netProfit / totalProductCost) * 100 : 0;

      const panelData: OrderDetailPanelData = {
        orderNumber: order.orderNumber,
        orderDate: order.orderDate,
        products: order.products,
        totalPrice: order.totalPrice,
        returnPrice: order.returnPrice,
        revenue: order.revenue,
        totalProductCost,
        estimatedCommission: order.estimatedCommission,
        estimatedShippingCost: shippingCost,
        stoppage: order.stoppage,
        grossProfit: order.grossProfit,
        netProfit,
        profitMargin,
        roi,
      };
      setSelectedOrder(panelData);
      setOrderDetailOpen(true);
    }
  };

  // Format date for display
  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString("tr-TR", {
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="bg-card rounded-lg border border-border">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-border">
        {/* Tabs */}
        <div className="flex items-center gap-1">
          {(["products", "orders"] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                "px-3 py-1.5 text-sm font-medium rounded transition-colors",
                activeTab === tab
                  ? "bg-[#E8F1FE] text-[#1D70F1] dark:bg-[#1D70F1]/20"
                  : "text-muted-foreground hover:bg-muted"
              )}
            >
              {tab === "products" ? "Ürünler" : "Sipariş Kalemleri"}
            </button>
          ))}
        </div>

        {/* Actions - Conditional based on activeTab */}
        <div className="flex items-center gap-2">
          {activeTab === "products" ? (
            /* Products Tab Actions */
            <>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="sm" className="h-8 gap-1.5">
                    Grupla: {productSortField === "name" ? "Ürün" : productSortField === "sku" ? "SKU" : productSortField === "brand" ? "Marka" : ""}
                    <ChevronDown className="h-3.5 w-3.5" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem onClick={() => handleColumnSort("name")}>Ürün</DropdownMenuItem>
                  <DropdownMenuItem onClick={() => handleColumnSort("sku")}>SKU</DropdownMenuItem>
                  <DropdownMenuItem onClick={() => handleColumnSort("brand")}>Marka</DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>

              <Button variant="outline" size="icon" className="h-8 w-8">
                <Download className="h-4 w-4" />
              </Button>

              {/* Product Column Visibility Popover */}
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
                      {PRODUCT_COLUMN_CONFIG.filter(c => !c.alwaysVisible).map((col) => (
                        <label key={col.id} className="flex items-center gap-2 cursor-pointer hover:bg-muted/50 rounded px-1 py-0.5 -mx-1">
                          <Checkbox
                            checked={productVisibleColumns.has(col.id)}
                            onCheckedChange={() => toggleProductColumn(col.id)}
                            className={cn(
                              "border-2",
                              productVisibleColumns.has(col.id)
                                ? "border-[#1D70F1] bg-[#1D70F1] data-[state=checked]:bg-[#1D70F1] data-[state=checked]:border-[#1D70F1]"
                                : "border-gray-300 dark:border-gray-600 bg-transparent"
                            )}
                          />
                          <span className={cn(
                            "text-sm",
                            productVisibleColumns.has(col.id) ? "text-[#1D70F1] font-medium" : "text-muted-foreground"
                          )}>
                            {col.label}
                          </span>
                        </label>
                      ))}
                    </div>
                  </div>
                </PopoverContent>
              </Popover>
            </>
          ) : (
            /* Order Items Tab Actions */
            <>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="sm" className="h-8 gap-1.5">
                    Sırala: {orderSortField === "orderDate" ? "Tarih" : orderSortField === "orderNumber" ? "Sipariş No" : orderSortField === "productName" ? "Ürün" : orderSortField === "totalPrice" ? "Satış" : orderSortField === "profit" ? "Kâr" : ""}
                    <ChevronDown className="h-3.5 w-3.5" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem onClick={() => handleOrderColumnSort("orderDate")}>Tarih</DropdownMenuItem>
                  <DropdownMenuItem onClick={() => handleOrderColumnSort("orderNumber")}>Sipariş No</DropdownMenuItem>
                  <DropdownMenuItem onClick={() => handleOrderColumnSort("productName")}>Ürün</DropdownMenuItem>
                  <DropdownMenuItem onClick={() => handleOrderColumnSort("totalPrice")}>Satış</DropdownMenuItem>
                  <DropdownMenuItem onClick={() => handleOrderColumnSort("profit")}>Kâr</DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>

              <Button variant="outline" size="icon" className="h-8 w-8">
                <Download className="h-4 w-4" />
              </Button>

              {/* Order Column Visibility Popover */}
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
                      {ORDER_COLUMN_CONFIG.filter(c => !c.alwaysVisible).map((col) => (
                        <label key={col.id} className="flex items-center gap-2 cursor-pointer hover:bg-muted/50 rounded px-1 py-0.5 -mx-1">
                          <Checkbox
                            checked={orderVisibleColumns.has(col.id)}
                            onCheckedChange={() => toggleOrderColumn(col.id)}
                            className={cn(
                              "border-2",
                              orderVisibleColumns.has(col.id)
                                ? "border-[#1D70F1] bg-[#1D70F1] data-[state=checked]:bg-[#1D70F1] data-[state=checked]:border-[#1D70F1]"
                                : "border-gray-300 dark:border-gray-600 bg-transparent"
                            )}
                          />
                          <span className={cn(
                            "text-sm",
                            orderVisibleColumns.has(col.id) ? "text-[#1D70F1] font-medium" : "text-muted-foreground"
                          )}>
                            {col.label}
                          </span>
                        </label>
                      ))}
                    </div>
                  </div>
                </PopoverContent>
              </Popover>
            </>
          )}
        </div>
      </div>

      {/* Table - Conditional rendering based on activeTab */}
      <div className="overflow-x-auto">
        {activeTab === "products" ? (
          /* Products Table */
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="w-[300px]">Ürün</TableHead>
                {visibleColumns.has("unitsSold") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("unitsSold")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      Satılan
                      {sortField === "unitsSold" ? (
                        sortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                      ) : <ArrowUpDown className="h-3 w-3 opacity-30" />}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("refunds") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("refunds")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      İade
                      {sortField === "refunds" ? (
                        sortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                      ) : <ArrowUpDown className="h-3 w-3 opacity-30" />}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("sales") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("sales")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      Satış
                      {sortField === "sales" ? (
                        sortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                      ) : <ArrowUpDown className="h-3 w-3 opacity-30" />}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("commission") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("commission")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      Komisyon
                      {sortField === "commission" ? (
                        sortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                      ) : <ArrowUpDown className="h-3 w-3 opacity-30" />}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("grossProfit") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("grossProfit")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      Brüt Kâr
                      {sortField === "grossProfit" ? (
                        sortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                      ) : <ArrowUpDown className="h-3 w-3 opacity-30" />}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("netProfit") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("netProfit")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      Net Kâr
                      {sortField === "netProfit" ? (
                        sortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                      ) : <ArrowUpDown className="h-3 w-3 opacity-30" />}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("margin") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("margin")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      Marj
                      {sortField === "margin" ? (
                        sortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                      ) : <ArrowUpDown className="h-3 w-3 opacity-30" />}
                    </div>
                  </TableHead>
                )}
                {visibleColumns.has("roi") && (
                  <TableHead
                    className="text-right cursor-pointer hover:bg-muted/50 select-none"
                    onClick={() => handleColumnSort("roi")}
                  >
                    <div className="flex items-center justify-end gap-1">
                      ROI
                      {sortField === "roi" ? (
                        sortDirection === "asc" ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />
                      ) : <ArrowUpDown className="h-3 w-3 opacity-30" />}
                    </div>
                  </TableHead>
                )}
                <TableHead className="w-[50px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <>
                  <ProductRowSkeleton />
                  <ProductRowSkeleton />
                  <ProductRowSkeleton />
                  <ProductRowSkeleton />
                  <ProductRowSkeleton />
                </>
              ) : filteredProducts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={visibleColumnCount} className="h-24 text-center text-muted-foreground">
                    {searchQuery ? "Arama sonucu bulunamadı" : "Henüz ürün verisi yok"}
                  </TableCell>
                </TableRow>
              ) : (
                visibleProducts.map((product) => (
                  <TableRow key={product.id} className="hover:bg-muted/50">
                    <TableCell>
                      <div className="flex items-start gap-3">
                        {/* Product Image - Trendyol Link */}
                        {product.productUrl ? (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <a
                                href={product.productUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex-shrink-0 group relative"
                              >
                                {product.image ? (
                                  <img
                                    src={product.image}
                                    alt={product.name}
                                    className="h-12 w-12 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                                    onError={(e) => {
                                      (e.target as HTMLImageElement).src = "https://via.placeholder.com/48?text=No+Image";
                                    }}
                                  />
                                ) : (
                                  <div
                                    className={cn(
                                      "h-12 w-12 rounded flex items-center justify-center text-xs font-bold text-white group-hover:ring-2 ring-[#F27A1A] transition-all",
                                      product.marketplace === "trendyol"
                                        ? "bg-[#F27A1A]"
                                        : "bg-[#FF6000]"
                                    )}
                                  >
                                    {product.marketplace === "trendyol" ? "T" : "H"}
                                  </div>
                                )}
                                <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                                  <ExternalLink className="h-2.5 w-2.5 text-white" />
                                </div>
                              </a>
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[200px]">
                              <p className="text-xs">Trendyol&apos;da görüntüle</p>
                            </TooltipContent>
                          </Tooltip>
                        ) : product.image ? (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <img
                                src={product.image}
                                alt={product.name}
                                className="h-12 w-12 rounded object-cover flex-shrink-0 border border-border cursor-help"
                                onError={(e) => {
                                  (e.target as HTMLImageElement).src = "https://via.placeholder.com/48?text=No+Image";
                                }}
                              />
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[300px]">
                              <p className="text-xs">{product.name}</p>
                            </TooltipContent>
                          </Tooltip>
                        ) : (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <div
                                className={cn(
                                  "h-12 w-12 rounded flex items-center justify-center text-xs font-bold text-white flex-shrink-0 cursor-help",
                                  product.marketplace === "trendyol"
                                    ? "bg-[#F27A1A]"
                                    : "bg-[#FF6000]"
                                )}
                              >
                                {product.marketplace === "trendyol" ? "T" : "H"}
                              </div>
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[300px]">
                              <p className="text-xs">{product.name}</p>
                            </TooltipContent>
                          </Tooltip>
                        )}
                        <div className="min-w-0">
                          {product.name.length > PRODUCT_NAME_LIMIT ? (
                            <Tooltip>
                              <TooltipTrigger asChild>
                                {product.productUrl ? (
                                  <a
                                    href={product.productUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="font-medium text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                                  >
                                    {truncateText(product.name, PRODUCT_NAME_LIMIT)}
                                  </a>
                                ) : (
                                  <p className="font-medium text-sm text-foreground cursor-default">
                                    {truncateText(product.name, PRODUCT_NAME_LIMIT)}
                                  </p>
                                )}
                              </TooltipTrigger>
                              <TooltipContent side="top" className="max-w-[300px]">
                                <p>{product.name}</p>
                              </TooltipContent>
                            </Tooltip>
                          ) : product.productUrl ? (
                            <a
                              href={product.productUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="font-medium text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                            >
                              {product.name}
                            </a>
                          ) : (
                            <p className="font-medium text-sm text-foreground">
                              {product.name}
                            </p>
                          )}
                          <p className="text-xs text-muted-foreground">
                            {product.sku} - Maliyet: {formatCurrency(product.cogs)}
                          </p>
                          <p className="text-xs text-muted-foreground/70">
                            Stok: {product.stock}
                          </p>
                        </div>
                      </div>
                    </TableCell>
                    {visibleColumns.has("unitsSold") && (
                      <TableCell className="text-right font-medium">
                        {product.unitsSold}
                      </TableCell>
                    )}
                    {visibleColumns.has("refunds") && (
                      <TableCell className="text-right">
                        <span className={product.refunds > 0 ? "text-[#1D70F1]" : ""}>
                          {product.refunds}
                        </span>
                      </TableCell>
                    )}
                    {visibleColumns.has("sales") && (
                      <TableCell className="text-right font-medium">
                        {formatCurrency(product.sales)}
                      </TableCell>
                    )}
                    {visibleColumns.has("commission") && (
                      <TableCell className="text-right">
                        <span className="text-red-600">
                          {formatCurrency(-(product.commission || 0))}
                        </span>
                      </TableCell>
                    )}
                    {visibleColumns.has("grossProfit") && (
                      <TableCell className="text-right">
                        <span className={product.grossProfit >= 0 ? "text-green-600" : "text-red-600"}>
                          {formatCurrency(product.grossProfit)}
                        </span>
                      </TableCell>
                    )}
                    {visibleColumns.has("netProfit") && (
                      <TableCell className="text-right">
                        <span className={product.netProfit >= 0 ? "text-green-600" : "text-red-600"}>
                          {formatCurrency(product.netProfit)}
                        </span>
                      </TableCell>
                    )}
                    {visibleColumns.has("margin") && (
                      <TableCell className="text-right">
                        <span className={product.margin >= 0 ? "text-green-600" : "text-red-600"}>
                          {product.margin}%
                        </span>
                      </TableCell>
                    )}
                    {visibleColumns.has("roi") && (
                      <TableCell className="text-right">
                        <span className={product.roi >= 0 ? "text-green-600" : "text-red-600"}>
                          {product.roi}%
                        </span>
                      </TableCell>
                    )}
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 text-[#1D70F1]"
                        onClick={() => handleOpenDetail(product)}
                      >
                        Detay
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        ) : (
          /* Order Items Table */
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="w-[200px]">Sipariş</TableHead>
                <TableHead className="w-[250px]">Ürün</TableHead>
                {orderVisibleColumns.has("quantity") && (
                  <TableHead className="text-right">Adet</TableHead>
                )}
                {orderVisibleColumns.has("orderSales") && (
                  <TableHead className="text-right">Satış</TableHead>
                )}
                {orderVisibleColumns.has("cost") && (
                  <TableHead className="text-right">Maliyet</TableHead>
                )}
                {orderVisibleColumns.has("orderCommission") && (
                  <TableHead className="text-right">Komisyon</TableHead>
                )}
                {orderVisibleColumns.has("orderGrossProfit") && (
                  <TableHead className="text-right">Brüt Kâr</TableHead>
                )}
                {orderVisibleColumns.has("orderMargin") && (
                  <TableHead className="text-right">Marj</TableHead>
                )}
                {orderVisibleColumns.has("orderRoi") && (
                  <TableHead className="text-right">ROI</TableHead>
                )}
                <TableHead className="w-[50px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <>
                  <OrderItemRowSkeleton visibleColumns={orderVisibleColumns} />
                  <OrderItemRowSkeleton visibleColumns={orderVisibleColumns} />
                  <OrderItemRowSkeleton visibleColumns={orderVisibleColumns} />
                  <OrderItemRowSkeleton visibleColumns={orderVisibleColumns} />
                  <OrderItemRowSkeleton visibleColumns={orderVisibleColumns} />
                </>
              ) : filteredOrderItems.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={orderVisibleColumnCount} className="h-24 text-center text-muted-foreground">
                    {searchQuery ? "Arama sonucu bulunamadı" : "Henüz sipariş verisi yok"}
                  </TableCell>
                </TableRow>
              ) : (
                visibleOrderItems.map((item) => {
                  const margin = item.totalPrice > 0 ? Math.round((item.profit / item.totalPrice) * 100) : 0;
                  const roi = item.cost > 0 ? Math.round((item.profit / item.cost) * 100) : 0;

                  return (
                    <TableRow
                      key={item.id}
                      className="hover:bg-muted/50"
                    >
                      <TableCell>
                        <div className="min-w-0">
                          <p className="font-medium text-sm text-foreground">
                            #{item.orderNumber}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {formatDate(item.orderDate)}
                          </p>
                          <p className="text-xs text-muted-foreground/70">
                            Toplam: {formatCurrency(item.orderTotalPrice)}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="min-w-0">
                          {item.productName.length > PRODUCT_NAME_LIMIT ? (
                            <Tooltip>
                              <TooltipTrigger asChild>
                                {item.productUrl ? (
                                  <a
                                    href={item.productUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="font-medium text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                                  >
                                    {truncateText(item.productName, PRODUCT_NAME_LIMIT)}
                                  </a>
                                ) : (
                                  <p className="font-medium text-sm text-foreground cursor-default">
                                    {truncateText(item.productName, PRODUCT_NAME_LIMIT)}
                                  </p>
                                )}
                              </TooltipTrigger>
                              <TooltipContent side="top" className="max-w-[300px]">
                                <p>{item.productName}</p>
                              </TooltipContent>
                            </Tooltip>
                          ) : item.productUrl ? (
                            <a
                              href={item.productUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="font-medium text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer"
                            >
                              {item.productName}
                            </a>
                          ) : (
                            <p className="font-medium text-sm text-foreground">
                              {item.productName}
                            </p>
                          )}
                          <p className="text-xs text-muted-foreground">
                            {item.barcode}
                          </p>
                        </div>
                      </TableCell>
                      {orderVisibleColumns.has("quantity") && (
                        <TableCell className="text-right font-medium">
                          {item.quantity}
                        </TableCell>
                      )}
                      {orderVisibleColumns.has("orderSales") && (
                        <TableCell className="text-right font-medium">
                          {formatCurrency(item.totalPrice)}
                        </TableCell>
                      )}
                      {orderVisibleColumns.has("cost") && (
                        <TableCell className="text-right">
                          <span className="text-orange-600">
                            {formatCurrency(-item.cost)}
                          </span>
                        </TableCell>
                      )}
                      {orderVisibleColumns.has("orderCommission") && (
                        <TableCell className="text-right">
                          <span className="text-red-600">
                            {formatCurrency(-item.commission)}
                          </span>
                        </TableCell>
                      )}
                      {orderVisibleColumns.has("orderGrossProfit") && (
                        <TableCell className="text-right">
                          <span className={item.profit >= 0 ? "text-green-600" : "text-red-600"}>
                            {formatCurrency(item.profit)}
                          </span>
                        </TableCell>
                      )}
                      {orderVisibleColumns.has("orderMargin") && (
                        <TableCell className="text-right">
                          <span className={margin >= 0 ? "text-green-600" : "text-red-600"}>
                            {margin}%
                          </span>
                        </TableCell>
                      )}
                      {orderVisibleColumns.has("orderRoi") && (
                        <TableCell className="text-right">
                          <span className={roi >= 0 ? "text-green-600" : "text-red-600"}>
                            {roi}%
                          </span>
                        </TableCell>
                      )}
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-8 text-[#1D70F1]"
                          onClick={() => handleOrderClick(item.orderNumber)}
                        >
                          Detay
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        )}
      </div>

      {/* Footer - Load More */}
      {activeTab === "products" ? (
        hasMoreProducts && (
          <div className="p-4 border-t border-border text-center">
            <Button
              variant="outline"
              onClick={handleLoadMore}
              className="text-[#1D70F1] border-[#1D70F1] hover:bg-[#1D70F1]/10"
            >
              Daha fazla yükle ({Math.min(remainingCount, PRODUCTS_PER_PAGE)} / {remainingCount} kalan)
            </Button>
          </div>
        )
      ) : (
        hasMoreOrderItems && (
          <div className="p-4 border-t border-border text-center">
            <Button
              variant="outline"
              onClick={handleLoadMore}
              className="text-[#1D70F1] border-[#1D70F1] hover:bg-[#1D70F1]/10"
            >
              Daha fazla yükle ({Math.min(remainingOrderItemsCount, PRODUCTS_PER_PAGE)} / {remainingOrderItemsCount} kalan)
            </Button>
          </div>
        )
      )}

      {/* Count info */}
      {activeTab === "products" ? (
        filteredProducts.length > 0 && (
          <div className="px-4 pb-3 text-center text-xs text-muted-foreground">
            {visibleProducts.length} / {filteredProducts.length} ürün gösteriliyor
          </div>
        )
      ) : (
        filteredOrderItems.length > 0 && (
          <div className="px-4 pb-3 text-center text-xs text-muted-foreground">
            {visibleOrderItems.length} / {filteredOrderItems.length} sipariş kalemi gösteriliyor
          </div>
        )
      )}

      {/* Product Detail Panel */}
      <ProductDetailPanel
        open={detailPanelOpen}
        onOpenChange={setDetailPanelOpen}
        product={selectedProduct}
      />

      {/* Order Detail Panel */}
      <OrderDetailPanel
        open={orderDetailOpen}
        onOpenChange={setOrderDetailOpen}
        order={selectedOrder}
      />
    </div>
  );
}
