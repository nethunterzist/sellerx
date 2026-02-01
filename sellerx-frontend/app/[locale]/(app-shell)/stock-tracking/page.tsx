"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Plus, FlaskConical } from "lucide-react";
import {
  StockDashboardCards,
  AddProductModal,
  StockTrackedProductsTable,
  StockAlertsList,
} from "@/components/stock-tracking";
import {
  useStockTrackingDashboard,
  useTrackedProducts,
  useStockAlerts,
} from "@/hooks/queries/use-stock-tracking";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useTranslations } from "next-intl";
import {
  MOCK_PRODUCTS,
  MOCK_DASHBOARD,
  MOCK_ALERTS,
} from "@/lib/mock/stock-tracking-mock-data";
import { Skeleton } from "@/components/ui/skeleton";
import {
  StatCardSkeleton,
  TableSkeleton,
  ListItemSkeleton,
} from "@/components/ui/skeleton-blocks";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

function StockTrackingPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => <StatCardSkeleton key={i} />)}
      </div>
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <TableSkeleton columns={6} rows={8} showImage={true} />
        </div>
        <Card>
          <CardHeader><Skeleton className="h-5 w-24" /></CardHeader>
          <CardContent><ListItemSkeleton count={5} /></CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function StockTrackingPage() {
  const t = useTranslations("stockTracking");
  const { data: selectedStore } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const [showAddModal, setShowAddModal] = useState(false);
  const [mockMode, setMockMode] = useState(false);

  const { data: dashboard, isLoading: dashboardLoading } = useStockTrackingDashboard(storeId);
  const { data: products, isLoading: productsLoading } = useTrackedProducts(storeId);
  const { data: alerts, isLoading: alertsLoading } = useStockAlerts(storeId);

  // Use mock data when mock mode is active
  const displayDashboard = mockMode ? MOCK_DASHBOARD : dashboard;
  const displayProducts = mockMode ? MOCK_PRODUCTS : products;
  const displayAlerts = mockMode ? MOCK_ALERTS : alerts;

  if (!storeId) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-muted-foreground">{t("noStore")}</p>
      </div>
    );
  }

  if (!mockMode && (dashboardLoading || productsLoading || alertsLoading)) {
    return <StockTrackingPageSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{t("title")}</h1>
          <p className="text-muted-foreground">{t("description")}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant={mockMode ? "default" : "outline"}
            size="sm"
            onClick={() => setMockMode(!mockMode)}
          >
            <FlaskConical className="h-4 w-4" />
            <span className="ml-2 hidden sm:inline">Mock</span>
          </Button>
          <Button onClick={() => setShowAddModal(true)}>
            <Plus className="h-4 w-4 mr-2" />
            {t("addProduct")}
          </Button>
        </div>
      </div>

      {/* Mock Mode Banner */}
      {mockMode && (
        <div className="bg-amber-100 dark:bg-amber-900/30 border border-amber-300 dark:border-amber-800 rounded-lg p-3 flex items-center gap-2">
          <FlaskConical className="h-4 w-4 text-amber-600 dark:text-amber-400" />
          <span className="text-sm text-amber-800 dark:text-amber-200">
            {t("mockModeEnabled")}
          </span>
        </div>
      )}

      {/* Dashboard Cards */}
      <StockDashboardCards dashboard={displayDashboard} isLoading={mockMode ? false : dashboardLoading} />

      {/* Main Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Products Table */}
        <div className="lg:col-span-2">
          <div className="mb-4">
            <h2 className="text-lg font-semibold">{t("trackedProducts")}</h2>
            <p className="text-sm text-muted-foreground">
              {t("trackedProductsDesc", { count: displayProducts?.length ?? 0, max: 10 })}
            </p>
          </div>
          <StockTrackedProductsTable
            products={displayProducts}
            storeId={storeId}
            isLoading={mockMode ? false : productsLoading}
            mockMode={mockMode}
          />
        </div>

        {/* Alerts Sidebar */}
        <div className="lg:col-span-1">
          <StockAlertsList
            alerts={displayAlerts}
            storeId={storeId}
            isLoading={mockMode ? false : alertsLoading}
            maxHeight="500px"
          />
        </div>
      </div>

      {/* Add Product Modal */}
      <AddProductModal
        open={showAddModal}
        onOpenChange={setShowAddModal}
        storeId={storeId}
      />
    </div>
  );
}
