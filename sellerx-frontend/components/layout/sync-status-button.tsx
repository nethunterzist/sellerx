"use client";

const isDev = process.env.NODE_ENV === "development";

import { useState, useEffect } from "react";
import { RefreshCw, Check, AlertCircle, Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  DropdownMenuLabel,
} from "@/components/ui/dropdown-menu";
import { useSyncProducts } from "@/hooks/queries/use-products";
import { useSyncOrders } from "@/hooks/queries/use-orders";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { cn } from "@/lib/utils";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface SyncInfo {
  products: string | null;
  orders: string | null;
}

const SYNC_STORAGE_KEY = "sellerx_sync_info";

function getSyncInfo(storeId: string): SyncInfo {
  if (typeof window === "undefined") return { products: null, orders: null };

  try {
    const stored = localStorage.getItem(`${SYNC_STORAGE_KEY}_${storeId}`);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (e) {
    // Ignore parse errors
  }
  return { products: null, orders: null };
}

function setSyncInfo(storeId: string, type: "products" | "orders") {
  if (typeof window === "undefined") return;

  const current = getSyncInfo(storeId);
  const updated = { ...current, [type]: new Date().toISOString() };
  localStorage.setItem(`${SYNC_STORAGE_KEY}_${storeId}`, JSON.stringify(updated));
}

function formatSyncTime(isoString: string | null): string {
  if (!isoString) return "Hiç senkronize edilmedi";

  try {
    const date = new Date(isoString);
    return formatDistanceToNow(date, { addSuffix: true, locale: tr });
  } catch {
    return "Bilinmiyor";
  }
}

export function SyncStatusButton() {
  const { data: selectedStoreData } = useSelectedStore();
  const selectedStoreId = selectedStoreData?.selectedStoreId;

  const syncProducts = useSyncProducts();
  const syncOrders = useSyncOrders();

  const [syncInfo, setSyncInfoState] = useState<SyncInfo>({ products: null, orders: null });
  const [isOpen, setIsOpen] = useState(false);

  // Load sync info from localStorage
  useEffect(() => {
    if (selectedStoreId) {
      setSyncInfoState(getSyncInfo(selectedStoreId));
    }
  }, [selectedStoreId]);

  const isSyncing = syncProducts.isPending || syncOrders.isPending;
  const hasError = syncProducts.isError || syncOrders.isError;

  const handleSyncProducts = async () => {
    if (!selectedStoreId) return;

    try {
      await syncProducts.mutateAsync(selectedStoreId);
      setSyncInfo(selectedStoreId, "products");
      setSyncInfoState(getSyncInfo(selectedStoreId));
    } catch (error) {
      if (isDev) console.error("Product sync failed:", error);
    }
  };

  const handleSyncOrders = async () => {
    if (!selectedStoreId) return;

    try {
      await syncOrders.mutateAsync(selectedStoreId);
      setSyncInfo(selectedStoreId, "orders");
      setSyncInfoState(getSyncInfo(selectedStoreId));
    } catch (error) {
      if (isDev) console.error("Order sync failed:", error);
    }
  };

  const handleSyncAll = async () => {
    if (!selectedStoreId) return;

    try {
      await Promise.all([
        syncProducts.mutateAsync(selectedStoreId),
        syncOrders.mutateAsync(selectedStoreId),
      ]);
      setSyncInfo(selectedStoreId, "products");
      setSyncInfo(selectedStoreId, "orders");
      setSyncInfoState(getSyncInfo(selectedStoreId));
    } catch (error) {
      if (isDev) console.error("Sync failed:", error);
    }
  };

  if (!selectedStoreId) {
    return null;
  }

  return (
    <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          size="sm"
          className={cn(
            "h-8 gap-1.5 text-sm",
            isSyncing && "animate-pulse",
            hasError && "border-red-300 text-red-600"
          )}
        >
          <RefreshCw className={cn("h-3.5 w-3.5", isSyncing && "animate-spin")} />
          <span className="hidden sm:inline">
            {isSyncing ? "Senkronize ediliyor..." : "Senkronize Et"}
          </span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        <DropdownMenuLabel className="flex items-center gap-2">
          <Clock className="h-4 w-4 text-gray-400" />
          Son Senkronizasyon
        </DropdownMenuLabel>

        {/* Sync Status */}
        <div className="px-2 py-2 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="text-gray-500">Ürünler:</span>
            <span className={cn(
              "font-medium",
              syncInfo.products ? "text-green-600" : "text-gray-400"
            )}>
              {formatSyncTime(syncInfo.products)}
            </span>
          </div>
          <div className="flex items-center justify-between text-xs">
            <span className="text-gray-500">Siparişler:</span>
            <span className={cn(
              "font-medium",
              syncInfo.orders ? "text-green-600" : "text-gray-400"
            )}>
              {formatSyncTime(syncInfo.orders)}
            </span>
          </div>
        </div>

        <DropdownMenuSeparator />

        {/* Error Messages */}
        {syncProducts.isError && (
          <div className="px-2 py-1.5 flex items-center gap-2 text-xs text-red-600 bg-red-50 rounded mx-1 mb-2">
            <AlertCircle className="h-3.5 w-3.5" />
            Ürün senkronizasyonu başarısız
          </div>
        )}
        {syncOrders.isError && (
          <div className="px-2 py-1.5 flex items-center gap-2 text-xs text-red-600 bg-red-50 rounded mx-1 mb-2">
            <AlertCircle className="h-3.5 w-3.5" />
            Sipariş senkronizasyonu başarısız
          </div>
        )}

        {/* Sync Actions */}
        <DropdownMenuItem
          onClick={handleSyncAll}
          disabled={isSyncing}
          className="gap-2"
        >
          <RefreshCw className={cn("h-4 w-4", isSyncing && "animate-spin")} />
          Tümünü Senkronize Et
        </DropdownMenuItem>

        <DropdownMenuSeparator />

        <DropdownMenuItem
          onClick={handleSyncProducts}
          disabled={syncProducts.isPending}
          className="gap-2"
        >
          {syncProducts.isPending ? (
            <RefreshCw className="h-4 w-4 animate-spin" />
          ) : syncProducts.isSuccess ? (
            <Check className="h-4 w-4 text-green-500" />
          ) : (
            <RefreshCw className="h-4 w-4" />
          )}
          Ürünleri Senkronize Et
        </DropdownMenuItem>

        <DropdownMenuItem
          onClick={handleSyncOrders}
          disabled={syncOrders.isPending}
          className="gap-2"
        >
          {syncOrders.isPending ? (
            <RefreshCw className="h-4 w-4 animate-spin" />
          ) : syncOrders.isSuccess ? (
            <Check className="h-4 w-4 text-green-500" />
          ) : (
            <RefreshCw className="h-4 w-4" />
          )}
          Siparişleri Senkronize Et
        </DropdownMenuItem>

        <DropdownMenuSeparator />

        <div className="px-2 py-1.5 text-[10px] text-gray-400">
          Otomatik senkronizasyon: Ürünler her gün 06:15'te, siparişler her 6 saatte bir
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
