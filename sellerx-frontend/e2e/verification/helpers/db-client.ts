/**
 * PostgreSQL direct query client for triple data verification.
 * Connects to local sellerx_db and runs verification queries.
 */
import { Pool, type PoolConfig } from "pg";

const poolConfig: PoolConfig = {
  host: process.env.DB_HOST || "localhost",
  port: Number(process.env.DB_PORT) || 5432,
  database: process.env.DB_NAME || "sellerx_db",
  user: process.env.DB_USER || "postgres",
  password: process.env.DB_PASSWORD || "postgres123",
  max: 5,
  idleTimeoutMillis: 30000,
};

let pool: Pool | null = null;

function getPool(): Pool {
  if (!pool) {
    pool = new Pool(poolConfig);
  }
  return pool;
}

export async function closePool(): Promise<void> {
  if (pool) {
    await pool.end();
    pool = null;
  }
}

async function query<T = Record<string, unknown>>(
  sql: string,
  params: unknown[] = []
): Promise<T[]> {
  const p = getPool();
  const result = await p.query(sql, params);
  return result.rows as T[];
}

/**
 * Format a Date as YYYY-MM-DD in local timezone.
 * IMPORTANT: Do NOT use toISOString().split("T")[0] — that converts to UTC,
 * which shifts dates back 1 day for UTC+3 (Turkey) when local time is 00:00-02:59.
 * The backend uses Europe/Istanbul timezone, so we must match local dates.
 */
export function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

// ─── Store lookup ──────────────────────────────────────────────

export async function getStoreIdForUser(
  email: string
): Promise<string | null> {
  const rows = await query<{ id: string }>(
    `SELECT s.id::text
     FROM stores s
     JOIN users u ON s.user_id = u.id
     WHERE u.email = $1
     LIMIT 1`,
    [email]
  );
  return rows[0]?.id ?? null;
}

// Backend (DashboardStatsService.java) uses specific status filter for revenue orders:
// IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
// This EXCLUDES 'Returned' and 'Cancelled' orders from revenue
const REVENUE_STATUSES = `'Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked'`;

// ─── Dashboard Stats ───────────────────────────────────────────

export interface DbDashboardStats {
  totalRevenue: number;
  totalOrders: number;
  totalProductsSold: number;
  returnCount: number;
  totalEstimatedCommission: number;
  totalShippingCost: number;
  totalProductCosts: number;
  grossProfit: number;
}

export async function getDashboardStats(
  storeId: string,
  startDate: string,
  endDate: string
): Promise<DbDashboardStats> {
  const orderRows = await query<{
    total_revenue: string;
    total_orders: string;
    total_products_sold: string;
    total_product_cost: string;
  }>(
    `SELECT
       COALESCE(SUM(o.total_price), 0) AS total_revenue,
       COUNT(*)::text AS total_orders,
       COALESCE(SUM(
         (SELECT COALESCE(SUM((item->>'quantity')::int), 0)
          FROM jsonb_array_elements(o.order_items) AS item)
       ), 0)::text AS total_products_sold,
       COALESCE(SUM(
         (SELECT COALESCE(SUM(
           CASE WHEN (item->>'cost')::numeric > 0
                THEN (item->>'cost')::numeric * COALESCE((item->>'quantity')::int, 0)
                ELSE 0
           END
         ), 0) FROM jsonb_array_elements(o.order_items) AS item)
       ), 0) AS total_product_cost
     FROM trendyol_orders o
     WHERE o.store_id = $1::uuid
       AND o.order_date >= ($2 || ' 00:00:00')::timestamp
       AND o.order_date <= ($3 || ' 23:59:59')::timestamp
       AND o.status IN (${REVENUE_STATUSES})`,
    [storeId, startDate, endDate]
  );

  // Return count (orders with 'Returned' status in the period)
  const returnRows = await query<{ return_count: string }>(
    `SELECT COUNT(*)::text AS return_count
     FROM trendyol_orders
     WHERE store_id = $1::uuid
       AND order_date >= ($2 || ' 00:00:00')::timestamp
       AND order_date <= ($3 || ' 23:59:59')::timestamp
       AND status = 'Returned'`,
    [storeId, startDate, endDate]
  );

  // Commission from deduction invoices (matches backend DashboardStatsService)
  // Backend: deductionInvoiceRepository.sumCommissionFeesByStoreIdAndDateRange()
  const commissionRows = await query<{ total_commission: string }>(
    `SELECT COALESCE(SUM(debt), 0) AS total_commission
     FROM trendyol_deduction_invoices
     WHERE store_id = $1::uuid
       AND transaction_date >= ($2 || ' 00:00:00')::timestamp
       AND transaction_date < (($3::date + 1) || ' 00:00:00')::timestamp
       AND transaction_type IN ('Komisyon Faturası', 'AZ - Komisyon Faturası', 'AZ-Komisyon Geliri')`,
    [storeId, startDate, endDate]
  );

  // Shipping cost from deduction invoices (matches backend calculateShippingCosts)
  // Backend: deductionInvoiceRepository.sumCargoFeesByStoreIdAndDateRange()
  const shippingRows = await query<{ total_shipping: string }>(
    `SELECT COALESCE(SUM(debt), 0) AS total_shipping
     FROM trendyol_deduction_invoices
     WHERE store_id = $1::uuid
       AND transaction_date >= ($2 || ' 00:00:00')::timestamp
       AND transaction_date < (($3::date + 1) || ' 00:00:00')::timestamp
       AND transaction_type IN ('Kargo Fatura', 'Kargo Faturası', 'AZ - Kargo Fatura')`,
    [storeId, startDate, endDate]
  );

  const row = orderRows[0];
  const totalRevenue = parseFloat(row?.total_revenue || "0");
  const totalOrders = parseInt(row?.total_orders || "0", 10);
  const totalProductsSold = parseInt(row?.total_products_sold || "0", 10);
  const totalCommission = parseFloat(commissionRows[0]?.total_commission || "0");
  const totalProductCosts = parseFloat(row?.total_product_cost || "0");
  const returnCount = parseInt(returnRows[0]?.return_count || "0", 10);
  const totalShippingCost = parseFloat(shippingRows[0]?.total_shipping || "0");

  return {
    totalRevenue,
    totalOrders,
    totalProductsSold,
    returnCount,
    totalEstimatedCommission: totalCommission,
    totalShippingCost,
    totalProductCosts,
    // Backend gross profit = Revenue - Product Costs (no commission/shipping deduction)
    grossProfit: totalRevenue - totalProductCosts,
  };
}

// ─── Orders ────────────────────────────────────────────────────

export interface DbOrder {
  orderNumber: string;
  orderDate: string;
  totalPrice: number;
  status: string;
  productCount: number;
}

export async function getOrders(
  storeId: string,
  startDate: string,
  endDate: string,
  limit = 20
): Promise<DbOrder[]> {
  const rows = await query<{
    ty_order_number: string;
    order_date: string;
    total_price: string;
    status: string;
    product_count: string;
  }>(
    `SELECT
       ty_order_number,
       order_date::text,
       total_price::text,
       status,
       jsonb_array_length(COALESCE(order_items, '[]'::jsonb))::text AS product_count
     FROM trendyol_orders
     WHERE store_id = $1::uuid
       AND order_date >= ($2 || ' 00:00:00')::timestamp
       AND order_date <= ($3 || ' 23:59:59')::timestamp
     ORDER BY order_date DESC
     LIMIT $4`,
    [storeId, startDate, endDate, limit]
  );

  return rows.map((r) => ({
    orderNumber: r.ty_order_number,
    orderDate: r.order_date,
    totalPrice: parseFloat(r.total_price),
    status: r.status,
    productCount: parseInt(r.product_count, 10),
  }));
}

export async function getOrderByNumber(
  orderNumber: string
): Promise<DbOrder | null> {
  const rows = await query<{
    ty_order_number: string;
    order_date: string;
    total_price: string;
    status: string;
    product_count: string;
  }>(
    `SELECT
       ty_order_number,
       order_date::text,
       total_price::text,
       status,
       jsonb_array_length(COALESCE(order_items, '[]'::jsonb))::text AS product_count
     FROM trendyol_orders
     WHERE ty_order_number = $1
     LIMIT 1`,
    [orderNumber]
  );

  if (rows.length === 0) return null;
  const r = rows[0];
  return {
    orderNumber: r.ty_order_number,
    orderDate: r.order_date,
    totalPrice: parseFloat(r.total_price),
    status: r.status,
    productCount: parseInt(r.product_count, 10),
  };
}

// ─── Products ──────────────────────────────────────────────────

export interface DbProduct {
  barcode: string;
  name: string;
  salePrice: number;
  trendyolQuantity: number;
  lastCommissionRate: number | null;
  status: string;
}

export async function getProducts(
  storeId: string,
  limit = 20
): Promise<DbProduct[]> {
  const rows = await query<{
    barcode: string;
    title: string;
    sale_price: string;
    trendyol_quantity: string;
    last_commission_rate: string | null;
    on_sale: boolean;
    archived: boolean;
    approved: boolean;
  }>(
    `SELECT
       barcode,
       title,
       sale_price::text,
       trendyol_quantity::text,
       last_commission_rate::text,
       on_sale,
       archived,
       approved
     FROM trendyol_products
     WHERE store_id = $1::uuid
       AND barcode IS NOT NULL
     ORDER BY sale_price DESC
     LIMIT $2`,
    [storeId, limit]
  );

  return rows.map((r) => ({
    barcode: r.barcode,
    name: r.title,
    salePrice: parseFloat(r.sale_price || "0"),
    trendyolQuantity: parseInt(r.trendyol_quantity || "0", 10),
    lastCommissionRate: r.last_commission_rate
      ? parseFloat(r.last_commission_rate)
      : null,
    status: r.on_sale ? "Satışta" : r.archived ? "Arşiv" : r.approved ? "Onaylı" : "Pasif",
  }));
}

export async function getProductByBarcode(
  storeId: string,
  barcode: string
): Promise<DbProduct | null> {
  const rows = await query<{
    barcode: string;
    title: string;
    sale_price: string;
    trendyol_quantity: string;
    last_commission_rate: string | null;
    on_sale: boolean;
    archived: boolean;
    approved: boolean;
  }>(
    `SELECT barcode, title, sale_price::text, trendyol_quantity::text,
            last_commission_rate::text, on_sale, archived, approved
     FROM trendyol_products
     WHERE store_id = $1::uuid AND barcode = $2
     LIMIT 1`,
    [storeId, barcode]
  );

  if (rows.length === 0) return null;
  const r = rows[0];
  return {
    barcode: r.barcode,
    name: r.title,
    salePrice: parseFloat(r.sale_price || "0"),
    trendyolQuantity: parseInt(r.trendyol_quantity || "0", 10),
    lastCommissionRate: r.last_commission_rate
      ? parseFloat(r.last_commission_rate)
      : null,
    status: r.on_sale ? "Satışta" : r.archived ? "Arşiv" : r.approved ? "Onaylı" : "Pasif",
  };
}

// ─── Invoices ──────────────────────────────────────────────────

export interface DbInvoiceSummary {
  category: string;
  count: number;
  totalAmount: number;
}

export async function getInvoiceSummary(
  storeId: string,
  startDate: string,
  endDate: string
): Promise<DbInvoiceSummary[]> {
  // Backend logic (TrendyolInvoiceService.java):
  // 1. If credit > 0 → refund → category overridden to 'IADE', amount = credit
  // 2. Otherwise → deduction → category from TYPE_TO_CATEGORY map, amount = debt
  // Note: 'AZ-Yurtdışı Operasyon Bedeli %18' has a corrupted key in backend map → falls to DIGER
  const rows = await query<{
    category: string;
    cnt: string;
    total_amount: string;
  }>(
    `SELECT
       category,
       COUNT(*)::text AS cnt,
       SUM(inv_amount)::text AS total_amount
     FROM (
       SELECT
         CASE
           -- Refund override: any invoice with credit > 0 → IADE
           WHEN credit > 0 THEN 'IADE'
           -- Normal category mapping (only for non-refund deductions)
           WHEN transaction_type IN ('Komisyon Faturası', 'AZ - Komisyon Faturası', 'AZ-Komisyon Geliri') THEN 'KOMISYON'
           WHEN transaction_type IN ('Platform Hizmet Bedeli', 'AZ-Platform Hizmet Bedeli') THEN 'PLATFORM_UCRETLERI'
           WHEN transaction_type IN ('Kargo Fatura', 'Kargo Faturası', 'AZ - Kargo Fatura',
                'MP Kargo İtiraz İade Faturası') THEN 'KARGO'
           WHEN transaction_type IN ('Reklam Bedeli', 'Sabit Bütçeli Influencer Reklam Bedeli',
                'Komisyonlu İnfluencer Reklam Bedeli', 'Komisyonlu Influencer Reklam Bedeli',
                'Kurumsal Kampanya Yansıtma Bedeli') THEN 'REKLAM'
           WHEN transaction_type IN ('Eksik Ürün Faturası', 'Yanlış Ürün Faturası', 'YANLIS URUN FATURASI',
                'Kusurlu Ürün Faturası', 'Tedarik Edememe', 'TEDARIK EDEMEME FATURASI',
                'Termin Gecikme Bedeli', 'Teslim Kontrol Faturası') THEN 'CEZA'
           WHEN transaction_type IN ('Tazmin Faturası', 'Yurtdışı Operasyon Iade Bedeli') THEN 'IADE'
           WHEN transaction_type IN ('Uluslararası Hizmet Bedeli', 'AZ-Uluslararası Hizmet Bedeli',
                'AZ-Yurtdışı Operasyon Bedeli',
                'Yurt Dışı Operasyon Bedeli') THEN 'ULUSLARARASI'
           WHEN transaction_type IN ('Erken Ödeme Kesinti Faturası', 'Fatura Kontör Satış Bedeli',
                'Müşteri Duyuruları Faturası', 'TEX Tazmin - İşleme- %0') THEN 'DIGER'
           ELSE 'DIGER'
         END AS category,
         -- Refunds use credit as amount, deductions use debt
         CASE WHEN credit > 0 THEN credit ELSE debt END AS inv_amount
       FROM trendyol_deduction_invoices
       WHERE store_id = $1::uuid
         AND transaction_date >= ($2 || ' 00:00:00')::timestamp
         AND transaction_date <= ($3 || ' 23:59:59')::timestamp
     ) sub
     GROUP BY category
     ORDER BY total_amount DESC`,
    [storeId, startDate, endDate]
  );

  return rows.map((r) => ({
    category: r.category,
    count: parseInt(r.cnt, 10),
    totalAmount: parseFloat(r.total_amount),
  }));
}

// ─── Returns ───────────────────────────────────────────────────

export interface DbReturnAnalytics {
  totalReturns: number;
  totalReturnAmount: number;
  returnRate: number;
  returnCount: number;
  totalLoss: number;
  totalRefundAmount: number;
}

export async function getReturnAnalytics(
  storeId: string,
  startDate: string,
  endDate: string
): Promise<DbReturnAnalytics> {
  // Backend uses trendyol_orders with status='Returned', NOT return_records table
  const returnRows = await query<{
    total_returns: string;
    total_items: string;
  }>(
    `SELECT
       COUNT(*)::text AS total_returns,
       COALESCE(SUM(
         (SELECT COALESCE(SUM((item->>'quantity')::int), 0)
          FROM jsonb_array_elements(o.order_items) AS item)
       ), 0)::text AS total_items
     FROM trendyol_orders o
     WHERE o.store_id = $1::uuid
       AND o.order_date >= ($2 || ' 00:00:00')::timestamp
       AND o.order_date <= ($3 || ' 23:59:59')::timestamp
       AND o.status = 'Returned'`,
    [storeId, startDate, endDate]
  );

  const orderCountRows = await query<{ cnt: string }>(
    `SELECT COUNT(*)::text AS cnt
     FROM trendyol_orders
     WHERE store_id = $1::uuid
       AND order_date >= ($2 || ' 00:00:00')::timestamp
       AND order_date <= ($3 || ' 23:59:59')::timestamp
       AND status NOT IN ('Cancelled')`,
    [storeId, startDate, endDate]
  );

  const totalReturns = parseInt(returnRows[0]?.total_returns || "0", 10);
  const totalItems = parseInt(returnRows[0]?.total_items || "0", 10);
  const orderCount = parseInt(orderCountRows[0]?.cnt || "0", 10);
  const returnRate = orderCount > 0 ? (totalReturns / orderCount) * 100 : 0;

  // Note: totalLoss calculation is complex in backend (involves shipping costs, packaging, product costs)
  // For verification we compare totalReturns and returnRate, totalLoss is approximate
  return {
    totalReturns,
    totalReturnAmount: 0, // Not easily calculable from DB alone
    returnRate,
    returnCount: totalReturns,
    totalLoss: 0, // Complex calculation involving cargo invoices, not replicated here
    totalRefundAmount: 0,
  };
}

// ─── Product Sales Stats (for dashboard products table) ────────

export interface DbProductSalesStats {
  barcode: string;
  unitsSold: number;
  totalSales: number;
}

export async function getProductSalesStats(
  storeId: string,
  startDate: string,
  endDate: string,
  limit = 10
): Promise<DbProductSalesStats[]> {
  const rows = await query<{
    barcode: string;
    units_sold: string;
    total_sales: string;
  }>(
    `SELECT
       item->>'barcode' AS barcode,
       SUM((item->>'quantity')::int)::text AS units_sold,
       SUM(COALESCE((item->>'price')::numeric, 0))::text AS total_sales
     FROM trendyol_orders o,
          jsonb_array_elements(o.order_items) AS item
     WHERE o.store_id = $1::uuid
       AND o.order_date >= ($2 || ' 00:00:00')::timestamp
       AND o.order_date <= ($3 || ' 23:59:59')::timestamp
       AND o.status IN (${REVENUE_STATUSES})
     GROUP BY item->>'barcode'
     ORDER BY SUM(COALESCE((item->>'price')::numeric, 0)) DESC
     LIMIT $4`,
    [storeId, startDate, endDate, limit]
  );

  return rows.map((r) => ({
    barcode: r.barcode,
    unitsSold: parseInt(r.units_sold, 10),
    totalSales: parseFloat(r.total_sales),
  }));
}
