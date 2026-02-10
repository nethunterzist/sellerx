/**
 * Column configurations for ProductsTable
 * Extracted from products-table.tsx for better modularity
 */

import type { ColumnConfig } from "./types";

// Products tab column configuration
export const PRODUCT_COLUMN_CONFIG: ColumnConfig[] = [
  { id: "product", label: "Ürün", defaultVisible: true, alwaysVisible: true },
  { id: "unitsSold", label: "Satılan", defaultVisible: true },
  { id: "refunds", label: "İade", defaultVisible: true },
  { id: "sales", label: "Satış", defaultVisible: true },
  { id: "commission", label: "Komisyon", defaultVisible: true },
  { id: "grossProfit", label: "Brüt Kâr", defaultVisible: true },
  { id: "netProfit", label: "Net Kâr", defaultVisible: true },
  { id: "margin", label: "Marj", defaultVisible: true },
  { id: "roi", label: "ROI", defaultVisible: true },
  { id: "acos", label: "ACOS", defaultVisible: false }, // Advertising cost ratio
  { id: "action", label: "Detay", defaultVisible: true, alwaysVisible: true },
];

// Order Items tab column configuration
export const ORDER_COLUMN_CONFIG: ColumnConfig[] = [
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
export const COLUMN_CONFIG = PRODUCT_COLUMN_CONFIG;

// Constants
export const PRODUCTS_PER_PAGE = 50;
export const PRODUCT_NAME_LIMIT = 40;

/**
 * Get default visible columns from localStorage or use defaults
 */
export const getDefaultVisibleColumns = (
  storageKey: string,
  config: ColumnConfig[]
): Set<string> => {
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

/**
 * Toggle column visibility helper
 */
export const createColumnToggler = (
  config: ColumnConfig[],
  setVisibleColumns: React.Dispatch<React.SetStateAction<Set<string>>>
) => {
  return (columnId: string) => {
    const columnConfig = config.find((c) => c.id === columnId);
    if (columnConfig?.alwaysVisible) return;

    setVisibleColumns((prev) => {
      const next = new Set(prev);
      if (next.has(columnId)) {
        next.delete(columnId);
      } else {
        next.add(columnId);
      }
      return next;
    });
  };
};
