"use client";

import { useState } from "react";
import { Plus, RefreshCw, AlertCircle, FlaskConical } from "lucide-react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  useBuyboxDashboard,
  useBuyboxProducts,
  useBuyboxAlerts,
  useMarkAlertsRead,
} from "@/hooks/queries/use-buybox";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  BuyboxStatusCards,
  BuyboxAlertsList,
  BuyboxProductTable,
  BuyboxAddProductModal,
} from "@/components/buybox";
import { useTranslations } from "next-intl";
import { MOCK_PRODUCTS, MOCK_ALERTS, MOCK_DASHBOARD } from "@/lib/mock/buybox-mock-data";
import { Skeleton } from "@/components/ui/skeleton";
import {
  StatCardSkeleton,
  FilterBarSkeleton,
  TableSkeleton,
  ListItemSkeleton,
} from "@/components/ui/skeleton-blocks";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

function BuyboxPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => <StatCardSkeleton key={i} />)}
      </div>
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <FilterBarSkeleton showSearch={true} buttonCount={1} />
          <div className="mt-4">
            <TableSkeleton columns={6} rows={8} showImage={true} />
          </div>
        </div>
        <Card>
          <CardHeader><Skeleton className="h-5 w-24" /></CardHeader>
          <CardContent><ListItemSkeleton count={5} /></CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function BuyboxPage() {
  const t = useTranslations("Buybox");
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [mockMode, setMockMode] = useState(false);

  const {
    data: dashboard,
    isLoading: dashboardLoading,
    error: dashboardError,
    refetch: refetchDashboard,
  } = useBuyboxDashboard(storeId || "");

  const {
    data: products,
    isLoading: productsLoading,
    error: productsError,
    refetch: refetchProducts,
  } = useBuyboxProducts(storeId || "");

  const {
    data: alerts,
    isLoading: alertsLoading,
    error: alertsError,
    refetch: refetchAlerts
  } = useBuyboxAlerts(storeId || "");

  const markAlertsRead = useMarkAlertsRead(storeId || "");

  const handleRefresh = () => {
    if (mockMode) return;
    refetchDashboard();
    refetchProducts();
    refetchAlerts();
  };

  const handleMarkAlertsRead = async () => {
    if (mockMode) return;
    try {
      await markAlertsRead.mutateAsync();
    } catch (error) {
      // Error handled by mutation
    }
  };

  const handleMarkAlertAsRead = async (alertId: string) => {
    if (mockMode) return;
    // Single alert mark as read - implement if API supports
  };

  // Display data based on mock mode
  const displayDashboard = mockMode ? MOCK_DASHBOARD : dashboard;
  const displayProducts = mockMode ? MOCK_PRODUCTS : (Array.isArray(products) ? products : []);
  const displayAlerts = mockMode ? MOCK_ALERTS : (Array.isArray(alerts) ? alerts : []);

  const isLoading = !mockMode && (storeLoading || dashboardLoading || productsLoading);
  const hasError = !mockMode && (dashboardError || productsError || alertsError);

  if (!storeId && !storeLoading && !mockMode) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <h2 className="text-xl font-semibold mb-2">{t("noStore")}</h2>
        <p className="text-muted-foreground">
          {t("noStore")}
        </p>
        <Button
          variant="outline"
          className="mt-4"
          onClick={() => setMockMode(true)}
        >
          <FlaskConical className="h-4 w-4 mr-2" />
          Mock Mod ile Gor
        </Button>
      </div>
    );
  }

  if (isLoading) {
    return <BuyboxPageSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">{t("title")}</h1>
          <p className="text-muted-foreground">
            {t("description")}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant={mockMode ? "default" : "outline"}
            size="sm"
            onClick={() => setMockMode(!mockMode)}
            className={mockMode ? "bg-amber-500 hover:bg-amber-600 text-white" : ""}
          >
            <FlaskConical className="h-4 w-4 mr-2" />
            Mock
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            disabled={mockMode}
          >
            <RefreshCw className="h-4 w-4 mr-2" />
            {t("refresh")}
          </Button>
          <Button
            size="sm"
            onClick={() => setIsAddModalOpen(true)}
            disabled={mockMode || (displayProducts.length >= 10)}
          >
            <Plus className="h-4 w-4 mr-2" />
            {t("addProduct")}
          </Button>
        </div>
      </div>

      {/* Mock Mode Banner */}
      {mockMode && (
        <Alert className="bg-amber-50 border-amber-200">
          <FlaskConical className="h-4 w-4 text-amber-600" />
          <AlertDescription className="text-amber-800">
            {t("mockModeEnabled")}
          </AlertDescription>
        </Alert>
      )}

      {/* Error State */}
      {hasError && !isLoading && !mockMode && (
        <div className="flex flex-col items-center justify-center min-h-[300px] text-center border rounded-lg bg-muted/10">
          <AlertCircle className="h-12 w-12 text-muted-foreground mb-4" />
          <h2 className="text-lg font-semibold mb-2">Buybox Ozelligi Yakinda</h2>
          <p className="text-muted-foreground max-w-md mb-4">
            Buybox takip ozelligi henuz aktif degil. Mock mod ile ornek verileri gorebilirsiniz.
          </p>
          <Button
            variant="outline"
            onClick={() => setMockMode(true)}
          >
            <FlaskConical className="h-4 w-4 mr-2" />
            Mock Mod ile Gor
          </Button>
        </div>
      )}

      {/* Main Content */}
      {(!hasError || mockMode) && (
        <>
          {/* Status Cards */}
          <BuyboxStatusCards dashboard={displayDashboard} isLoading={isLoading} />

          {/* Grid Layout: Product Table (2/3) + Alerts Sidebar (1/3) */}
          <div className="grid gap-6 lg:grid-cols-3">
            {/* Product Table */}
            <div className="lg:col-span-2">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold">{t("trackedProducts")}</h2>
                <span className="text-sm text-muted-foreground">
                  {t("trackedProductsDesc", { count: displayProducts.length, max: 10 })}
                </span>
              </div>
              <BuyboxProductTable
                products={displayProducts}
                isLoading={isLoading}
                storeId={storeId || "mock-store"}
              />
            </div>

            {/* Alerts Sidebar */}
            <div className="lg:col-span-1">
              <BuyboxAlertsList
                alerts={displayAlerts}
                storeId={storeId}
                isLoading={!mockMode && alertsLoading}
                showMarkAllRead={true}
                maxHeight="500px"
                mockMode={mockMode}
                onMarkAsRead={handleMarkAlertAsRead}
                onMarkAllAsRead={handleMarkAlertsRead}
              />
            </div>
          </div>
        </>
      )}

      {/* Add Product Modal */}
      <BuyboxAddProductModal
        open={isAddModalOpen}
        onOpenChange={setIsAddModalOpen}
        storeId={storeId || ""}
        trackedProducts={Array.isArray(products) ? products : []}
      />
    </div>
  );
}
