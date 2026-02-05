"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Plus, FlaskConical } from "lucide-react";
import {
  AddProductModal,
  StockTrackedProductsTable,
} from "@/components/stock-tracking";
import {
  useTrackedProducts,
} from "@/hooks/queries/use-stock-tracking";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useTranslations } from "next-intl";
import {
  MOCK_PRODUCTS,
} from "@/lib/mock/stock-tracking-mock-data";
import { TableSkeleton } from "@/components/ui/skeleton-blocks";

function StockTrackingPageSkeleton() {
  return (
    <div className="space-y-6">
      <TableSkeleton columns={6} rows={8} showImage={true} />
    </div>
  );
}

export default function StockTrackingPage() {
  const t = useTranslations("stockTracking");
  const { data: selectedStore } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const [showAddModal, setShowAddModal] = useState(false);
  const [mockMode, setMockMode] = useState(false);

  const { data: products, isLoading: productsLoading } = useTrackedProducts(storeId);

  // Use mock data when mock mode is active
  const displayProducts = mockMode ? MOCK_PRODUCTS : products;

  if (!storeId) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-muted-foreground">{t("noStore")}</p>
      </div>
    );
  }

  if (!mockMode && productsLoading) {
    return <StockTrackingPageSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Action Buttons */}
      <div className="flex items-center justify-end gap-2">
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

      {/* Mock Mode Banner */}
      {mockMode && (
        <div className="bg-amber-100 dark:bg-amber-900/30 border border-amber-300 dark:border-amber-800 rounded-lg p-3 flex items-center gap-2">
          <FlaskConical className="h-4 w-4 text-amber-600 dark:text-amber-400" />
          <span className="text-sm text-amber-800 dark:text-amber-200">
            {t("mockModeEnabled")}
          </span>
        </div>
      )}

      {/* Products Table */}
      <div>
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

      {/* Add Product Modal */}
      <AddProductModal
        open={showAddModal}
        onOpenChange={setShowAddModal}
        storeId={storeId}
      />
    </div>
  );
}
