/**
 * Sync Logger Utility
 * Provides diff algorithms for comparing React Query cache data
 * Development-only - all functions return empty results in production
 */

import type { SyncDataType, SyncDiff, SyncDataTypeConfig } from "@/types/sync-log";

// Import query keys
import { orderKeys } from "@/hooks/queries/use-orders";
import { productKeys } from "@/hooks/queries/use-products";
import { dashboardKeys } from "@/hooks/useDashboardStats";
import { financialKeys } from "@/hooks/queries/use-financial";
import { returnKeys, claimKeys } from "@/hooks/queries/use-returns";
import { qaKeys } from "@/hooks/queries/use-qa";
import { expenseKeys } from "@/hooks/queries/use-expenses";

const isDev = process.env.NODE_ENV === "development";

// Data type configurations with Turkish labels
export const SYNC_DATA_TYPES: SyncDataTypeConfig[] = [
  { type: "orders", label: "Siparişler", icon: "📦", queryKeyFn: orderKeys.byStore },
  { type: "products", label: "Ürünler", icon: "🏷️", queryKeyFn: productKeys.byStore },
  { type: "dashboard", label: "Dashboard", icon: "📊", queryKeyFn: dashboardKeys.stats },
  { type: "financial", label: "Finansal", icon: "💰", queryKeyFn: financialKeys.storeStats },
  { type: "returns", label: "İadeler", icon: "↩️", queryKeyFn: returnKeys.analyticsByStore },
  { type: "claims", label: "Talepler", icon: "📋", queryKeyFn: () => claimKeys.lists() },
  { type: "qa", label: "Soru-Cevap", icon: "❓", queryKeyFn: qaKeys.questionsByStore },
  { type: "expenses", label: "Giderler", icon: "💸", queryKeyFn: expenseKeys.byStore },
];

// Helper to safely get count from various data structures
function getCount(data: unknown): number {
  if (!data) return 0;
  if (Array.isArray(data)) return data.length;
  if (typeof data === "object" && data !== null) {
    const obj = data as Record<string, unknown>;
    if (typeof obj.totalElements === "number") return obj.totalElements;
    if (typeof obj.total === "number") return obj.total;
    if (typeof obj.length === "number") return obj.length;
    if (Array.isArray(obj.content)) return obj.content.length;
    if (Array.isArray(obj.products)) return obj.products.length;
    if (Array.isArray(obj.items)) return obj.items.length;
  }
  return 0;
}

// Diff for orders - tracks count and status changes
function diffOrders(before: unknown, after: unknown): Partial<SyncDiff> {
  if (!before && !after) return { hasChanges: false, summary: "Veri yok" };
  if (!before && after) {
    const count = getCount(after);
    return { hasChanges: true, summary: `${count} sipariş yüklendi` };
  }
  if (before && !after) return { hasChanges: true, summary: "Veriler temizlendi" };

  const beforeCount = getCount(before);
  const afterCount = getCount(after);
  const countDiff = afterCount - beforeCount;

  // Count status changes by comparing order arrays
  let statusChanges = 0;
  const beforeObj = before as { content?: Array<{ orderNumber?: string; tyOrderNumber?: string; status?: string }> };
  const afterObj = after as { content?: Array<{ orderNumber?: string; tyOrderNumber?: string; status?: string }> };

  if (beforeObj.content && afterObj.content) {
    const beforeMap = new Map(
      beforeObj.content.map((o) => [o.orderNumber || o.tyOrderNumber, o.status])
    );
    afterObj.content.forEach((order) => {
      const key = order.orderNumber || order.tyOrderNumber;
      const prevStatus = beforeMap.get(key);
      if (prevStatus && prevStatus !== order.status) {
        statusChanges++;
      }
    });
  }

  const parts: string[] = [];
  if (countDiff > 0) parts.push(`${countDiff} yeni sipariş`);
  if (countDiff < 0) parts.push(`${Math.abs(countDiff)} sipariş silindi`);
  if (statusChanges > 0) parts.push(`${statusChanges} durum değişikliği`);

  return {
    hasChanges: countDiff !== 0 || statusChanges > 0,
    summary: parts.length > 0 ? parts.join(", ") : "Değişiklik yok",
    details: {
      added: countDiff > 0 ? countDiff : undefined,
      removed: countDiff < 0 ? Math.abs(countDiff) : undefined,
      updated: statusChanges > 0 ? statusChanges : undefined,
    },
  };
}

// Diff for products - tracks count and price changes
function diffProducts(before: unknown, after: unknown): Partial<SyncDiff> {
  if (!before && !after) return { hasChanges: false, summary: "Veri yok" };
  if (!before && after) {
    const count = getCount(after);
    return { hasChanges: true, summary: `${count} ürün yüklendi` };
  }
  if (before && !after) return { hasChanges: true, summary: "Veriler temizlendi" };

  const beforeCount = getCount(before);
  const afterCount = getCount(after);
  const countDiff = afterCount - beforeCount;

  // Count price changes
  let priceChanges = 0;
  const beforeObj = before as { products?: Array<{ barcode?: string; salePrice?: number }> };
  const afterObj = after as { products?: Array<{ barcode?: string; salePrice?: number }> };

  if (beforeObj.products && afterObj.products) {
    const beforeMap = new Map(beforeObj.products.map((p) => [p.barcode, p.salePrice]));
    afterObj.products.forEach((product) => {
      const prevPrice = beforeMap.get(product.barcode);
      if (prevPrice !== undefined && prevPrice !== product.salePrice) {
        priceChanges++;
      }
    });
  }

  const parts: string[] = [];
  if (countDiff > 0) parts.push(`${countDiff} yeni ürün`);
  if (countDiff < 0) parts.push(`${Math.abs(countDiff)} ürün silindi`);
  if (priceChanges > 0) parts.push(`${priceChanges} fiyat güncellemesi`);

  return {
    hasChanges: countDiff !== 0 || priceChanges > 0,
    summary: parts.length > 0 ? parts.join(", ") : "Değişiklik yok",
    details: {
      added: countDiff > 0 ? countDiff : undefined,
      removed: countDiff < 0 ? Math.abs(countDiff) : undefined,
      updated: priceChanges > 0 ? priceChanges : undefined,
    },
  };
}

// Diff for dashboard - tracks revenue and order count metrics
function diffDashboard(before: unknown, after: unknown): Partial<SyncDiff> {
  if (!before && !after) return { hasChanges: false, summary: "Veri yok" };
  if (!before && after) return { hasChanges: true, summary: "Dashboard verileri yüklendi" };
  if (before && !after) return { hasChanges: true, summary: "Veriler temizlendi" };

  const metrics: Array<{ field: string; label: string; from: number; to: number; formatted: string }> = [];

  // Compare today's stats
  const beforeObj = before as { today?: { totalRevenue?: number; orderCount?: number; totalProfit?: number } };
  const afterObj = after as { today?: { totalRevenue?: number; orderCount?: number; totalProfit?: number } };

  const todayBefore = beforeObj?.today;
  const todayAfter = afterObj?.today;

  if (todayBefore && todayAfter) {
    if (todayBefore.totalRevenue !== todayAfter.totalRevenue) {
      const diff = (todayAfter.totalRevenue || 0) - (todayBefore.totalRevenue || 0);
      metrics.push({
        field: "totalRevenue",
        label: "Gelir",
        from: todayBefore.totalRevenue || 0,
        to: todayAfter.totalRevenue || 0,
        formatted: `${diff >= 0 ? "+" : ""}₺${diff.toLocaleString("tr-TR")}`,
      });
    }
    if (todayBefore.orderCount !== todayAfter.orderCount) {
      const diff = (todayAfter.orderCount || 0) - (todayBefore.orderCount || 0);
      metrics.push({
        field: "orderCount",
        label: "Sipariş",
        from: todayBefore.orderCount || 0,
        to: todayAfter.orderCount || 0,
        formatted: `${diff >= 0 ? "+" : ""}${diff}`,
      });
    }
    if (todayBefore.totalProfit !== todayAfter.totalProfit) {
      const diff = (todayAfter.totalProfit || 0) - (todayBefore.totalProfit || 0);
      metrics.push({
        field: "totalProfit",
        label: "Kar",
        from: todayBefore.totalProfit || 0,
        to: todayAfter.totalProfit || 0,
        formatted: `${diff >= 0 ? "+" : ""}₺${diff.toLocaleString("tr-TR")}`,
      });
    }
  }

  const hasChanges = metrics.length > 0;
  const summary = hasChanges
    ? `Bugün: ${metrics.map((m) => `${m.formatted} ${m.label.toLowerCase()}`).join(", ")}`
    : "Değişiklik yok";

  return { hasChanges, summary, details: { metrics } };
}

// Generic count-based diff for simpler data types
function diffByCount(label: string, before: unknown, after: unknown): Partial<SyncDiff> {
  const beforeCount = getCount(before);
  const afterCount = getCount(after);
  const diff = afterCount - beforeCount;

  if (diff === 0 && beforeCount === afterCount) {
    return { hasChanges: false, summary: beforeCount > 0 ? "Değişiklik yok" : "Veri yok" };
  }

  if (beforeCount === 0 && afterCount > 0) {
    return {
      hasChanges: true,
      summary: `${afterCount} ${label} yüklendi`,
      details: { added: afterCount },
    };
  }

  return {
    hasChanges: diff !== 0,
    summary: diff > 0 ? `${diff} yeni ${label}` : diff < 0 ? `${Math.abs(diff)} ${label} silindi` : "Değişiklik yok",
    details: {
      added: diff > 0 ? diff : undefined,
      removed: diff < 0 ? Math.abs(diff) : undefined,
    },
  };
}

// Main diff dispatcher
export function computeDiff(
  dataType: SyncDataType,
  config: SyncDataTypeConfig,
  before: unknown,
  after: unknown
): SyncDiff {
  // In production, return empty diff
  if (!isDev) {
    return {
      dataType,
      label: config.label,
      icon: config.icon,
      hasChanges: false,
      summary: "",
    };
  }

  let result: Partial<SyncDiff>;

  switch (dataType) {
    case "orders":
      result = diffOrders(before, after);
      break;
    case "products":
      result = diffProducts(before, after);
      break;
    case "dashboard":
      result = diffDashboard(before, after);
      break;
    case "financial":
      result = diffByCount("finansal kayıt", before, after);
      break;
    case "returns":
      result = diffByCount("iade", before, after);
      break;
    case "claims":
      result = diffByCount("talep", before, after);
      break;
    case "qa":
      result = diffByCount("soru", before, after);
      break;
    case "expenses":
      result = diffByCount("gider", before, after);
      break;
    default:
      result = { hasChanges: false, summary: "Bilinmeyen veri tipi" };
  }

  return {
    dataType,
    label: config.label,
    icon: config.icon,
    hasChanges: result.hasChanges ?? false,
    summary: result.summary ?? "",
    details: result.details,
  };
}

// Utility: Generate unique ID
export function generateSyncLogId(): string {
  return `sync-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

// Utility: Format timestamp for display
export function formatSyncTime(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString("tr-TR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

// Utility: Format duration
export function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  return `~${(ms / 1000).toFixed(1)}s`;
}
