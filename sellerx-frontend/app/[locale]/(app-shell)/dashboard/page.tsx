"use client";

import { useMemo, useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useDashboardStats, useDashboardStatsByRange, useDashboardStatsByPreset } from "@/hooks/useDashboardStats";
import { useProductsByStorePaginatedFull } from "@/hooks/queries/use-products";
import { useDashboardFilters } from "@/hooks/useDashboardFilters";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import {
  PeriodCards,
  ProductsTable,
  DashboardFilters,
  ChartView,
  PLView,
  TrendsView,
  CitiesView,
} from "@/components/dashboard";
import { StockDepletionBanner } from "@/components/dashboard/stock-depletion-banner";
import { useCityStats } from "@/hooks/queries/use-city-stats";
import type { DashboardViewType } from "@/components/dashboard/dashboard-tabs";
import type { ProductItem } from "@/components/dashboard/dashboard-filters";
import type { ProductDetail } from "@/types/dashboard";
import type { TrendyolProduct } from "@/types/product";

export default function DashboardPage() {
  const searchParams = useSearchParams();

  // Get active view from URL query params (synced with header tabs)
  const activeView = (searchParams.get("view") || "tiles") as DashboardViewType;

  // Use centralized filter hook - maintains independent state per tab
  const {
    selectedProducts,
    setSelectedProducts,
    selectedPeriodGroup,
    setSelectedPeriodGroup,
    customDateRange,
    selectedPeriod,
    setSelectedPeriod,
    selectedComparison,
    setSelectedComparison,
    selectedCurrency,
    setSelectedCurrency,
    filterConfig,
    handleDateRangeChange,
    handleDefaultViewChange,
  } = useDashboardFilters(activeView);

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Fetch default 4-period stats (always fetched for product dropdown)
  const { data: stats, isLoading: statsLoading, error } = useDashboardStats(storeId || undefined);

  // Fetch custom date range stats when a custom period is selected
  const {
    data: customStats,
    isLoading: customStatsLoading,
    error: customStatsError,
  } = useDashboardStatsByRange(
    storeId || undefined,
    customDateRange?.startDate,
    customDateRange?.endDate,
    customDateRange?.label
  );

  // Fetch multi-period stats for preset period groups (weeks, months, quarters, etc.)
  const {
    data: presetStats,
    isLoading: presetStatsLoading,
    error: presetStatsError,
    isMultiPeriod,
  } = useDashboardStatsByPreset(storeId || undefined, selectedPeriodGroup);

  // Determine if we're in custom date range mode
  const isCustomRange = customDateRange !== null;

  // Dynamic mode için seçili kart index'i
  const [selectedDynamicIndex, setSelectedDynamicIndex] = useState(0);

  // selectedPeriodGroup değiştiğinde dynamic index'i sıfırla
  useEffect(() => {
    setSelectedDynamicIndex(0);
  }, [selectedPeriodGroup]);

  // City stats for the cities view - defaults to last 30 days if no custom range
  const cityStatsStartDate = customDateRange?.startDate || format(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), "yyyy-MM-dd");
  const cityStatsEndDate = customDateRange?.endDate || format(new Date(), "yyyy-MM-dd");
  // Use first selected product barcode for city stats filtering (single product filter)
  const cityStatsProductBarcode = selectedProducts.length === 1 ? selectedProducts[0] : undefined;
  const { data: cityStats, isLoading: cityStatsLoading } = useCityStats({
    storeId,
    startDate: cityStatsStartDate,
    endDate: cityStatsEndDate,
    productBarcode: cityStatsProductBarcode,
  });

  // Fetch full product data from TrendyolProduct for additional details
  // Reduced from 500 to 100 for performance - dashboard only shows top 10 products
  const { data: productListResponse } = useProductsByStorePaginatedFull(storeId, { size: 100 });

  // Create a lookup map by barcode for TrendyolProduct data
  const trendyolProductMap = useMemo(() => {
    const map = new Map<string, TrendyolProduct>();
    if (productListResponse?.products) {
      productListResponse.products.forEach((product) => {
        if (product.barcode) {
          map.set(product.barcode, product);
        }
      });
    }
    return map;
  }, [productListResponse]);

  // Get all unique products from stats for the search dropdown
  const allProducts = useMemo<ProductItem[]>(() => {
    if (!stats) return [];

    const productMap = new Map<string, ProductItem>();

    const addProducts = (products?: ProductDetail[]) => {
      products?.forEach((p) => {
        const id = p.barcode;
        if (id && !productMap.has(id)) {
          productMap.set(id, {
            id,
            name: p.productName,
            sku: p.barcode || "",
            barcode: p.barcode,
            image: p.image,
          });
        }
      });
    };

    addProducts(stats.today?.products);
    addProducts(stats.yesterday?.products);
    addProducts(stats.thisMonth?.products);
    addProducts(stats.lastMonth?.products);

    return Array.from(productMap.values());
  }, [stats]);

  // Transform dashboard products to table format with TrendyolProduct enrichment
  const transformProducts = (products: ProductDetail[] | undefined) => {
    if (!products) return undefined;

    return products.map((p, index) => {
      // Find matching TrendyolProduct for additional data
      const trendyolProduct = p.barcode ? trendyolProductMap.get(p.barcode) : undefined;

      // Calculate COGS from cost history if available
      const latestCost = trendyolProduct?.costAndStockInfo?.[0];
      const cogs = latestCost ? latestCost.unitCost * p.totalSoldQuantity : 0;

      return {
        id: p.barcode || String(index),
        name: p.productName,
        sku: p.barcode,
        image: p.image || trendyolProduct?.image, // Ürün görseli URL
        cogs,
        stock: trendyolProduct?.trendyolQuantity ?? p.stock ?? 0,
        marketplace: "trendyol" as const,
        unitsSold: p.totalSoldQuantity,
        refunds: p.returnQuantity,
        sales: p.revenue,
        ads: 0, // Reklam verisi yok
        grossProfit: p.grossProfit,
        netProfit: p.netProfit ?? (p.grossProfit - p.estimatedCommission),
        margin: p.revenue > 0 ? Math.round((p.grossProfit / p.revenue) * 100) : 0,
        roi: p.roi ?? (cogs > 0 ? Math.round(((p.grossProfit - p.estimatedCommission) / cogs) * 100) : 0),
        // TrendyolProduct'tan gelen ek veriler
        categoryName: trendyolProduct?.categoryName,
        brand: trendyolProduct?.brand,
        salePrice: trendyolProduct?.salePrice,
        vatRate: trendyolProduct?.vatRate,
        commissionRate: trendyolProduct?.commissionRate,
        trendyolQuantity: trendyolProduct?.trendyolQuantity,
        productUrl: p.productUrl || trendyolProduct?.productUrl,
        status: trendyolProduct ? {
          onSale: trendyolProduct.onSale,
          approved: trendyolProduct.approved,
          hasActiveCampaign: trendyolProduct.hasActiveCampaign,
          archived: trendyolProduct.archived,
          blacklisted: trendyolProduct.blacklisted,
          rejected: trendyolProduct.rejected,
        } : undefined,
        costHistory: trendyolProduct?.costAndStockInfo?.map((item) => ({
          stockDate: item.stockDate,
          quantity: item.quantity,
          unitCost: item.unitCost,
          costVatRate: item.costVatRate,
          usedQuantity: item.usedQuantity,
        })),
        commission: p.estimatedCommission,
        // ============== YENİ: Backend'den gelen 32 Metrik Alanları ==============
        // İndirimler & Kuponlar
        sellerDiscount: p.sellerDiscount ?? 0,
        platformDiscount: p.platformDiscount ?? 0,
        couponDiscount: p.couponDiscount ?? 0,
        totalDiscount: p.totalDiscount ?? 0,
        // Net Ciro
        netRevenue: p.netRevenue ?? p.revenue,
        // Maliyetler
        productCost: p.productCost ?? cogs,
        shippingCost: p.shippingCost ?? 0,
        isShippingEstimated: p.isShippingEstimated, // true: tahmini kargo, false: gerçek kargo faturası
        refundCost: p.refundCost ?? 0,
        // Oranlar
        refundRate: p.refundRate ?? 0,
        profitMargin: p.profitMargin ?? (p.revenue > 0 ? (p.grossProfit / p.revenue) * 100 : 0),
        // İade detayları
        returnQuantity: p.returnQuantity ?? 0,
      };
    });
  };

  // Get products based on selected period, multi-period preset, or custom date range
  const getProductsForPeriod = () => {
    // If multi-period preset is active, use the selected period's products
    if (isMultiPeriod && presetStats && presetStats.length > 0) {
      console.log('[DEBUG] Dynamic mode - selectedDynamicIndex:', selectedDynamicIndex);
      console.log('[DEBUG] presetStats length:', presetStats.length);
      const selectedStats = presetStats[selectedDynamicIndex]?.stats;
      console.log('[DEBUG] selectedStats products count:', selectedStats?.products?.length);
      return selectedStats?.products;
    }

    // If custom date range is selected, use custom stats products
    if (isCustomRange && customStats) {
      return customStats.products;
    }

    // Otherwise use default 4-period stats
    if (!stats) return undefined;
    switch (selectedPeriod) {
      case "today":
        return stats.today?.products;
      case "yesterday":
        return stats.yesterday?.products;
      case "thisMonth":
        return stats.thisMonth?.products;
      case "lastMonth":
        return stats.lastMonth?.products;
      default:
        return stats.today?.products;
    }
  };

  // Get orders based on selected period, multi-period preset, or custom date range
  const getOrdersForPeriod = () => {
    // If multi-period preset is active, use the selected period's orders
    if (isMultiPeriod && presetStats && presetStats.length > 0) {
      const selectedStats = presetStats[selectedDynamicIndex]?.stats;
      return selectedStats?.orders;
    }

    // If custom date range is selected, use custom stats orders
    if (isCustomRange && customStats) {
      return customStats.orders;
    }

    // Otherwise use default 4-period stats
    if (!stats) return undefined;
    switch (selectedPeriod) {
      case "today":
        return stats.today?.orders;
      case "yesterday":
        return stats.yesterday?.orders;
      case "thisMonth":
        return stats.thisMonth?.orders;
      case "lastMonth":
        return stats.lastMonth?.orders;
      default:
        return stats.today?.orders;
    }
  };

  // Filter products based on selected products from search
  const periodProducts = getProductsForPeriod();
  const filteredPeriodProducts = useMemo(() => {
    if (!periodProducts) return undefined;
    if (selectedProducts.length === 0) return periodProducts;

    return periodProducts.filter((p) => {
      return selectedProducts.includes(p.barcode);
    });
  }, [periodProducts, selectedProducts]);

  const tableProducts = transformProducts(filteredPeriodProducts);

  // Get orders for the selected period
  const periodOrders = getOrdersForPeriod();

  // Calculate date range display text for custom range
  const getCustomDateRangeDisplay = () => {
    if (!customDateRange) return "";
    // Parse ISO dates and format for display
    const start = new Date(customDateRange.startDate);
    const end = new Date(customDateRange.endDate);
    return `${format(start, "d MMM", { locale: tr })} - ${format(end, "d MMM yyyy", { locale: tr })}`;
  };

  // Combined loading state - check multi-period, custom range, or default
  const isLoading = isMultiPeriod
    ? presetStatsLoading
    : isCustomRange
      ? customStatsLoading
      : statsLoading;
  const displayError = isMultiPeriod
    ? presetStatsError
    : isCustomRange
      ? customStatsError
      : error;

  // Render content based on active view
  const renderViewContent = () => {
    switch (activeView) {
      case "tiles":
        return (
          <>
            {/* Period Cards - Dynamic mode for presets, Single mode for custom range, Multi mode for default */}
            <section>
              {isMultiPeriod && presetStats ? (
                <PeriodCards
                  mode="dynamic"
                  periodData={presetStats.map((p) => ({
                    stats: p.stats,
                    label: p.label,
                    shortLabel: p.shortLabel,
                    dateRange: p.dateRange,
                    color: p.color,
                  }))}
                  isLoading={presetStatsLoading}
                  selectedIndex={selectedDynamicIndex}
                  onPeriodSelect={(index) => setSelectedDynamicIndex(index)}
                />
              ) : isCustomRange ? (
                <PeriodCards
                  mode="single"
                  customStats={customStats}
                  customTitle={customDateRange?.label}
                  customDateRange={getCustomDateRangeDisplay()}
                  isLoading={customStatsLoading}
                />
              ) : (
                <PeriodCards
                  mode="multi"
                  stats={stats}
                  isLoading={statsLoading}
                  selectedPeriod={selectedPeriod}
                  onPeriodSelect={setSelectedPeriod}
                />
              )}
            </section>

            {/* Products Table */}
            <section>
              <ProductsTable
                products={tableProducts}
                orders={periodOrders}
                isLoading={isLoading || storeLoading}
              />
            </section>
          </>
        );

      case "chart":
        return (
          <section>
            {/* Chart view uses 4-period stats for comparison */}
            <ChartView
              stats={stats}
              isLoading={statsLoading}
              selectedProducts={selectedProducts}
            />
          </section>
        );

      case "pl":
        return (
          <section>
            {/* P&L view uses multi-period stats */}
            <PLView storeId={storeId} />
          </section>
        );

      case "trends":
        return (
          <section>
            {/* Trends view uses 4-period stats for comparison */}
            <TrendsView stats={stats} isLoading={statsLoading} trendyolProductMap={trendyolProductMap} />
          </section>
        );

      case "cities":
        return (
          <section>
            {/* Cities view shows Turkey map with order distribution */}
            <CitiesView cityStats={cityStats} isLoading={cityStatsLoading} />
          </section>
        );

      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Stock Depletion Warning */}
      <StockDepletionBanner storeId={storeId} />

      {/* Error Message */}
      {displayError && (
        <p className="text-sm text-red-500">
          İstatistikler yüklenirken hata: {displayError.message}
        </p>
      )}

      {/* Dashboard Filters */}
      <section>
        <DashboardFilters
          products={allProducts}
          selectedProducts={selectedProducts}
          onProductsChange={setSelectedProducts}
          selectedPeriodGroup={selectedPeriodGroup}
          onPeriodGroupChange={setSelectedPeriodGroup}
          onDateRangeChange={handleDateRangeChange}
          onDefaultViewChange={handleDefaultViewChange}
          selectedComparison={selectedComparison}
          onComparisonChange={setSelectedComparison}
          selectedCurrency={selectedCurrency}
          onCurrencyChange={setSelectedCurrency}
          filterConfig={filterConfig}
        />
      </section>

      {/* View Content - Tab navigation is in header */}
      {renderViewContent()}
    </div>
  );
}
