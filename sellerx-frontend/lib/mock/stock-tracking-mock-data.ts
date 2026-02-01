// Mock data for Stock Tracking feature visualization
import type {
  TrackedProduct,
  StockSnapshot,
  StockAlert,
  StockTrackingDashboard,
  TrackedProductDetail,
  StockStatistics,
  StockAlertType,
  StockAlertSeverity,
} from "@/types/stock-tracking";

// Helper to generate a date N days ago
function daysAgo(n: number): string {
  const date = new Date();
  date.setDate(date.getDate() - n);
  return date.toISOString();
}

// 5 Mock Products with different stock states
export const MOCK_PRODUCTS: TrackedProduct[] = [
  {
    id: "mock-1",
    storeId: "mock-store",
    trendyolProductId: 123456789,
    productUrl: "https://www.trendyol.com/spigen/iphone-15-pro-max-kilif-p-123456789",
    productName: "iPhone 15 Pro Max Ultra Hybrid Kılıf",
    brandName: "Spigen",
    imageUrl: "https://cdn.dsmcdn.com/ty851/product/media/images/20230522/15/358273826/932876839/1/1_org_zoom.jpg",
    lastStockQuantity: 45,
    lastPrice: 299.99,
    lastCheckedAt: daysAgo(0),
    isActive: true,
    alertOnOutOfStock: true,
    alertOnLowStock: true,
    lowStockThreshold: 10,
    alertOnStockIncrease: false,
    alertOnBackInStock: true,
    createdAt: daysAgo(30),
    updatedAt: daysAgo(0),
    unreadAlertCount: 0,
  },
  {
    id: "mock-2",
    storeId: "mock-store",
    trendyolProductId: 234567890,
    productUrl: "https://www.trendyol.com/samsung/galaxy-s24-ekran-koruyucu-p-234567890",
    productName: "Samsung Galaxy S24 Ultra Temperli Cam Ekran Koruyucu",
    brandName: "Samsung",
    imageUrl: "https://cdn.dsmcdn.com/ty1083/product/media/images/prod/QC/20240318/13/ef59ef62-c428-3a7d-a9f4-f8e05d6f7e98/1_org_zoom.jpg",
    lastStockQuantity: 8,
    lastPrice: 149.99,
    lastCheckedAt: daysAgo(0),
    isActive: true,
    alertOnOutOfStock: true,
    alertOnLowStock: true,
    lowStockThreshold: 10,
    alertOnStockIncrease: false,
    alertOnBackInStock: true,
    createdAt: daysAgo(25),
    updatedAt: daysAgo(0),
    unreadAlertCount: 1,
  },
  {
    id: "mock-3",
    storeId: "mock-store",
    trendyolProductId: 345678901,
    productUrl: "https://www.trendyol.com/xiaomi/14-usb-c-sarj-kablosu-p-345678901",
    productName: "Xiaomi 14 Pro USB-C Hızlı Şarj Kablosu 100W",
    brandName: "Xiaomi",
    imageUrl: "https://cdn.dsmcdn.com/ty959/product/media/images/20230803/16/398217449/977698557/1/1_org_zoom.jpg",
    lastStockQuantity: 0,
    lastPrice: 89.99,
    lastCheckedAt: daysAgo(0),
    isActive: true,
    alertOnOutOfStock: true,
    alertOnLowStock: true,
    lowStockThreshold: 5,
    alertOnStockIncrease: true,
    alertOnBackInStock: true,
    createdAt: daysAgo(20),
    updatedAt: daysAgo(0),
    unreadAlertCount: 2,
  },
  {
    id: "mock-4",
    storeId: "mock-store",
    trendyolProductId: 456789012,
    productUrl: "https://www.trendyol.com/apple/watch-ultra-2-kordon-p-456789012",
    productName: "Apple Watch Ultra 2 Ocean Band Kordon",
    brandName: "Apple",
    imageUrl: "https://cdn.dsmcdn.com/ty1041/product/media/images/prod/SPM/PIM/20240218/20/3c89b6e8-8523-3bff-93ab-f68f5bda3fe2/1_org_zoom.jpg",
    lastStockQuantity: 120,
    lastPrice: 599.99,
    lastCheckedAt: daysAgo(0),
    isActive: true,
    alertOnOutOfStock: true,
    alertOnLowStock: true,
    lowStockThreshold: 15,
    alertOnStockIncrease: false,
    alertOnBackInStock: true,
    createdAt: daysAgo(15),
    updatedAt: daysAgo(0),
    unreadAlertCount: 0,
  },
  {
    id: "mock-5",
    storeId: "mock-store",
    trendyolProductId: 567890123,
    productUrl: "https://www.trendyol.com/apple/airpods-pro-2-kilif-p-567890123",
    productName: "AirPods Pro 2 Silikon Koruyucu Kılıf",
    brandName: "Apple",
    imageUrl: "https://cdn.dsmcdn.com/ty850/product/media/images/20230521/23/358120830/932699870/1/1_org_zoom.jpg",
    lastStockQuantity: 3,
    lastPrice: 199.99,
    lastCheckedAt: daysAgo(0),
    isActive: true,
    alertOnOutOfStock: true,
    alertOnLowStock: true,
    lowStockThreshold: 5,
    alertOnStockIncrease: false,
    alertOnBackInStock: true,
    createdAt: daysAgo(10),
    updatedAt: daysAgo(0),
    unreadAlertCount: 1,
  },
];

// Generate realistic 30-day stock snapshots with sales patterns
export function generateMockSnapshots(productId: string): StockSnapshot[] {
  const snapshots: StockSnapshot[] = [];
  const product = MOCK_PRODUCTS.find((p) => p.id === productId);
  const basePrice = product?.lastPrice || 299.99;

  // Start with different initial stocks based on product
  let currentStock =
    productId === "mock-1"
      ? 80
      : productId === "mock-2"
        ? 50
        : productId === "mock-3"
          ? 60
          : productId === "mock-4"
            ? 100
            : 40;

  // Seed for consistent random but varied patterns
  const seed = parseInt(productId.replace("mock-", "")) || 1;

  for (let i = 30; i >= 0; i--) {
    const date = new Date();
    date.setDate(date.getDate() - i);

    // Calculate previous values
    const previousQuantity = snapshots.length > 0 ? snapshots[snapshots.length - 1].quantity : null;

    // Realistic stock movement pattern
    const random = Math.sin(seed * i * 0.5) * 0.5 + 0.5; // Pseudo-random 0-1

    if (random > 0.85) {
      // 15% chance: Restock event (+20 to +50)
      currentStock += Math.floor(random * 30) + 20;
    } else if (random > 0.3) {
      // 55% chance: Normal sales (-1 to -5)
      currentStock = Math.max(0, currentStock - Math.floor(random * 5) - 1);
    }
    // 30% chance: No change

    // Special patterns for specific products
    if (productId === "mock-3" && i < 5) {
      // Xiaomi product goes out of stock recently
      currentStock = 0;
    }
    if (productId === "mock-4" && i === 10) {
      // Apple Watch gets big restock
      currentStock = 120;
    }

    const quantityChange = previousQuantity !== null ? currentStock - previousQuantity : null;

    snapshots.push({
      id: `snapshot-${productId}-${i}`,
      trackedProductId: productId,
      quantity: currentStock,
      inStock: currentStock > 0,
      price: basePrice + (Math.sin(i * 0.3) * 10), // Small price fluctuation
      previousQuantity,
      quantityChange,
      checkedAt: date.toISOString(),
    });
  }

  return snapshots;
}

// Mock alerts for sidebar
export const MOCK_ALERTS: StockAlert[] = [
  {
    id: "alert-1",
    trackedProductId: "mock-3",
    productName: "Xiaomi 14 Pro USB-C Hızlı Şarj Kablosu 100W",
    productImageUrl: "https://cdn.dsmcdn.com/ty959/product/media/images/20230803/16/398217449/977698557/1/1_org_zoom.jpg",
    alertType: "OUT_OF_STOCK" as StockAlertType,
    severity: "CRITICAL" as StockAlertSeverity,
    title: "Stok Tükendi",
    message: "Xiaomi 14 Pro USB-C Hızlı Şarj Kablosu 100W ürünü tükendi!",
    oldQuantity: 3,
    newQuantity: 0,
    threshold: null,
    isRead: false,
    readAt: null,
    createdAt: daysAgo(1),
  },
  {
    id: "alert-2",
    trackedProductId: "mock-2",
    productName: "Samsung Galaxy S24 Ultra Temperli Cam Ekran Koruyucu",
    productImageUrl: "https://cdn.dsmcdn.com/ty1083/product/media/images/prod/QC/20240318/13/ef59ef62-c428-3a7d-a9f4-f8e05d6f7e98/1_org_zoom.jpg",
    alertType: "LOW_STOCK" as StockAlertType,
    severity: "HIGH" as StockAlertSeverity,
    title: "Düşük Stok",
    message: "Samsung Galaxy S24 Ultra Temperli Cam Ekran Koruyucu stoku düşük (8 adet)!",
    oldQuantity: 12,
    newQuantity: 8,
    threshold: 10,
    isRead: false,
    readAt: null,
    createdAt: daysAgo(2),
  },
  {
    id: "alert-3",
    trackedProductId: "mock-5",
    productName: "AirPods Pro 2 Silikon Koruyucu Kılıf",
    productImageUrl: "https://cdn.dsmcdn.com/ty850/product/media/images/20230521/23/358120830/932699870/1/1_org_zoom.jpg",
    alertType: "LOW_STOCK" as StockAlertType,
    severity: "HIGH" as StockAlertSeverity,
    title: "Düşük Stok",
    message: "AirPods Pro 2 Silikon Koruyucu Kılıf stoku düşük (3 adet)!",
    oldQuantity: 7,
    newQuantity: 3,
    threshold: 5,
    isRead: true,
    readAt: daysAgo(1),
    createdAt: daysAgo(3),
  },
  {
    id: "alert-4",
    trackedProductId: "mock-4",
    productName: "Apple Watch Ultra 2 Ocean Band Kordon",
    productImageUrl: "https://cdn.dsmcdn.com/ty1041/product/media/images/prod/SPM/PIM/20240218/20/3c89b6e8-8523-3bff-93ab-f68f5bda3fe2/1_org_zoom.jpg",
    alertType: "STOCK_INCREASED" as StockAlertType,
    severity: "LOW" as StockAlertSeverity,
    title: "Stok Arttı",
    message: "Apple Watch Ultra 2 Ocean Band Kordon stoğu arttı: 50 → 120",
    oldQuantity: 50,
    newQuantity: 120,
    threshold: null,
    isRead: true,
    readAt: daysAgo(5),
    createdAt: daysAgo(10),
  },
];

// Mock dashboard statistics
export const MOCK_DASHBOARD: StockTrackingDashboard = {
  totalTrackedProducts: 5,
  activeTrackedProducts: 5,
  outOfStockProducts: 1,
  lowStockProducts: 2,
  outOfStockAlertsToday: 0,
  lowStockAlertsToday: 1,
  backInStockAlertsToday: 0,
  totalUnreadAlerts: 2,
  recentAlerts: MOCK_ALERTS.slice(0, 3),
  outOfStockList: MOCK_PRODUCTS.filter((p) => p.lastStockQuantity === 0),
  lowStockList: MOCK_PRODUCTS.filter(
    (p) => p.lastStockQuantity !== null && p.lastStockQuantity > 0 && p.lastStockQuantity <= p.lowStockThreshold
  ),
};

// Get mock product detail with snapshots
export function getMockProductDetail(id: string): TrackedProductDetail {
  const product = MOCK_PRODUCTS.find((p) => p.id === id) || MOCK_PRODUCTS[0];
  const snapshots = generateMockSnapshots(id);
  const productAlerts = MOCK_ALERTS.filter((a) => a.trackedProductId === id);

  // Calculate statistics from snapshots
  const quantities = snapshots.map((s) => s.quantity);
  const minStock = Math.min(...quantities);
  const maxStock = Math.max(...quantities);
  const avgStock = quantities.reduce((a, b) => a + b, 0) / quantities.length;
  const outOfStockCount = snapshots.filter((s) => s.quantity === 0).length;
  const lastOutOfStock = snapshots.find((s) => s.quantity === 0)?.checkedAt || null;

  const statistics: StockStatistics = {
    minStock,
    maxStock,
    avgStock,
    totalChecks: snapshots.length,
    outOfStockCount,
    lastOutOfStock,
  };

  return {
    product,
    snapshots,
    alerts: productAlerts,
    statistics,
  };
}
