/**
 * ProductsTable module exports
 * Main component is in ../products-table.tsx for backward compatibility
 */

// Type exports
export * from "./types";

// Column configuration exports
export {
  PRODUCT_COLUMN_CONFIG,
  ORDER_COLUMN_CONFIG,
  COLUMN_CONFIG,
  PRODUCTS_PER_PAGE,
  PRODUCT_NAME_LIMIT,
  getDefaultVisibleColumns,
  createColumnToggler,
} from "./column-config";

// Re-export the main component from parent directory for convenience
// Note: Main ProductsTable component remains in products-table.tsx for backward compatibility
