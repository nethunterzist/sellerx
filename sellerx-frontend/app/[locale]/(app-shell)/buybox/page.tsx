"use client";

import { useState, useMemo } from "react";
import { motion } from "motion/react";
import { AlertTriangle } from "lucide-react";
import { BuyboxSummaryCards, BuyboxProductsTable } from "@/components/buybox";
import { useBuyboxSummary, useBuyboxProducts } from "@/hooks/queries/use-buybox";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import type { BuyboxStatus } from "@/types/product";

export default function BuyboxPage() {
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const selectedStoreId = selectedStore?.selectedStoreId;

  const [page, setPage] = useState(0);
  const [selectedBarcodes, setSelectedBarcodes] = useState<string[]>([]);
  const [statusFilter, setStatusFilter] = useState<BuyboxStatus | "">("");

  const { data: summary, isLoading: summaryLoading } = useBuyboxSummary(selectedStoreId);
  const { data: allProductsData, isLoading: productsLoading } = useBuyboxProducts(selectedStoreId, {
    page: 0,
    size: 200,
    status: statusFilter,
  });

  const allProducts = allProductsData?.content || [];

  const filteredProducts = useMemo(() => {
    if (selectedBarcodes.length === 0) return allProducts;
    return allProducts.filter((p) => selectedBarcodes.includes(p.barcode));
  }, [allProducts, selectedBarcodes]);

  // Client-side pagination
  const pageSize = 20;
  const totalElements = filteredProducts.length;
  const totalPages = Math.ceil(totalElements / pageSize);
  const pagedProducts = filteredProducts.slice(page * pageSize, (page + 1) * pageSize);

  const paginatedResponse = {
    content: pagedProducts,
    totalElements,
    totalPages,
    number: page,
    size: pageSize,
    first: page === 0,
    last: page >= totalPages - 1,
  };

  if (!selectedStoreId && !storeLoading) {
    return (
      <div className="p-8">
        <div className="flex items-center gap-2 text-amber-600">
          <AlertTriangle className="h-5 w-5" />
          <p>Lutfen once bir magaza secin.</p>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      {/* Summary Cards */}
      <BuyboxSummaryCards summary={summary} isLoading={summaryLoading} />

      {/* Products Table */}
      <BuyboxProductsTable
        products={paginatedResponse}
        isLoading={productsLoading}
        page={page}
        onPageChange={setPage}
        selectedBarcodes={selectedBarcodes}
        onBarcodesChange={(barcodes) => { setSelectedBarcodes(barcodes); setPage(0); }}
        allProducts={allProducts}
        statusFilter={statusFilter}
        onStatusFilterChange={(val) => { setStatusFilter(val); setPage(0); }}
      />
    </motion.div>
  );
}
