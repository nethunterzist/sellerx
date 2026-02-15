/**
 * Lazy Excel Utilities
 *
 * Dynamic imports for heavy Excel libraries (xlsx, exceljs)
 * to reduce initial bundle size.
 *
 * Usage:
 * const { exportToXLSX } = await import('@/lib/utils/lazy-excel');
 * await exportToXLSX(data, filename);
 */

// XLSX library types
interface XLSXWorksheet {
  "!cols"?: Array<{ wch: number }>;
}

interface XLSXWorkbook {
  SheetNames: string[];
  Sheets: Record<string, XLSXWorksheet>;
}

interface XLSXLib {
  utils: {
    aoa_to_sheet: (data: unknown[][]) => XLSXWorksheet;
    book_new: () => XLSXWorkbook;
    book_append_sheet: (workbook: XLSXWorkbook, worksheet: XLSXWorksheet, name: string) => void;
  };
  writeFile: (workbook: XLSXWorkbook, filename: string) => void;
}

// ExcelJS types
interface ExcelJSWorksheet {
  addRow: (values: unknown[]) => void;
  columns: Array<{ width?: number }>;
}

interface ExcelJSWorkbook {
  addWorksheet: (name: string) => ExcelJSWorksheet;
  xlsx: {
    writeBuffer: () => Promise<ArrayBuffer>;
  };
}

/**
 * Lazy load XLSX library and export data
 */
export async function exportToXLSX(
  data: { headers: string[]; rows: unknown[][] },
  filename: string,
  sheetName: string = "Data",
  columnWidths?: number[]
): Promise<void> {
  // Dynamic import - only loads when called
  const XLSX = (await import("xlsx")) as XLSXLib;

  // Create worksheet
  const ws = XLSX.utils.aoa_to_sheet([data.headers, ...data.rows]);

  // Set column widths if provided
  if (columnWidths) {
    ws["!cols"] = columnWidths.map((wch) => ({ wch }));
  }

  // Create workbook
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, sheetName);

  // Download
  XLSX.writeFile(wb, filename);
}

/**
 * Lazy load ExcelJS library and export data with advanced formatting
 */
export async function exportToExcelJS(
  data: { headers: string[]; rows: unknown[][] },
  filename: string,
  sheetName: string = "Data",
  columnWidths?: number[]
): Promise<void> {
  // Dynamic imports - only loads when called
  const [ExcelJS, { saveAs }] = await Promise.all([
    import("exceljs").then((m) => m.default),
    import("file-saver"),
  ]);

  const workbook = new ExcelJS.Workbook() as ExcelJSWorkbook;
  const worksheet = workbook.addWorksheet(sheetName);

  // Add header row
  worksheet.addRow(data.headers);

  // Add data rows
  data.rows.forEach((row) => {
    worksheet.addRow(row);
  });

  // Set column widths if provided
  if (columnWidths) {
    worksheet.columns.forEach((col, i) => {
      col.width = columnWidths[i] || 15;
    });
  }

  // Write to buffer and save
  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  });
  saveAs(blob, filename);
}

/**
 * Format data for products export
 */
export function formatProductsForExport(
  products: Array<{
    name: string;
    sku: string;
    brand?: string;
    categoryName?: string;
    unitsSold: number;
    refunds: number;
    sales: number;
    commission?: number;
    grossProfit: number;
    netProfit: number;
    margin: number;
    roi: number;
    acos?: number;
    totalAdvertisingCost?: number;
    cpc?: number;
    cvr?: number;
  }>
): { headers: string[]; rows: unknown[][] } {
  const headers = [
    "Ürün",
    "SKU",
    "Marka",
    "Kategori",
    "Satılan",
    "İade",
    "Satış",
    "Komisyon",
    "Brüt Kâr",
    "Net Kâr",
    "Marj (%)",
    "ROI (%)",
    "ACOS (%)",
    "Reklam Maliyeti",
    "CPC",
    "CVR (%)",
  ];

  const rows = products.map((p) => [
    p.name,
    p.sku,
    p.brand || "",
    p.categoryName || "",
    p.unitsSold,
    p.refunds,
    p.sales,
    p.commission || 0,
    p.grossProfit,
    p.netProfit,
    p.margin,
    p.roi,
    p.acos != null ? p.acos.toFixed(2) : "-",
    p.totalAdvertisingCost != null ? p.totalAdvertisingCost.toFixed(2) : "-",
    p.cpc != null ? p.cpc.toFixed(2) : "-",
    p.cvr != null ? (p.cvr * 100).toFixed(2) : "-",
  ]);

  return { headers, rows };
}

/**
 * Format data for order items export
 */
export function formatOrderItemsForExport(
  orderItems: Array<{
    orderNumber: string;
    orderDate: string;
    productName: string;
    barcode: string;
    quantity: number;
    totalPrice: number;
    cost: number;
    commission: number;
    profit: number;
  }>,
  formatDate: (dateStr: string) => string
): { headers: string[]; rows: unknown[][] } {
  const headers = [
    "Sipariş No",
    "Tarih",
    "Ürün",
    "Barkod",
    "Adet",
    "Satış",
    "Maliyet",
    "Komisyon",
    "Kâr",
    "Marj (%)",
    "ROI (%)",
  ];

  const rows = orderItems.map((item) => {
    const margin =
      item.totalPrice > 0
        ? Math.round((item.profit / item.totalPrice) * 100)
        : 0;
    const roi = item.cost > 0 ? Math.round((item.profit / item.cost) * 100) : 0;
    return [
      item.orderNumber,
      formatDate(item.orderDate),
      item.productName,
      item.barcode,
      item.quantity,
      item.totalPrice,
      item.cost,
      item.commission,
      item.profit,
      margin,
      roi,
    ];
  });

  return { headers, rows };
}
