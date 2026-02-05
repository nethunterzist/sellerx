"use client";

import { useState } from "react";
import Link from "next/link";
import { AlertTriangle, X, ArrowRight } from "lucide-react";
import { useStockDepletion } from "@/hooks/queries/use-purchasing";

interface StockDepletionBannerProps {
  storeId: string | undefined;
}

export function StockDepletionBanner({ storeId }: StockDepletionBannerProps) {
  const [dismissed, setDismissed] = useState(false);
  const { data: depletedProducts } = useStockDepletion(storeId);

  if (dismissed || !depletedProducts || depletedProducts.length === 0) {
    return null;
  }

  const count = depletedProducts.length;

  return (
    <div className="relative flex items-center gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm dark:border-amber-800 dark:bg-amber-950/50">
      <AlertTriangle className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
      <div className="flex-1">
        <span className="font-medium text-amber-800 dark:text-amber-200">
          {count} {count === 1 ? "ürünün" : "ürünün"} FIFO stoğu tükendi.
        </span>{" "}
        <span className="text-amber-700 dark:text-amber-300">
          Bu ürünlerin maliyeti son bilinen maliyet üzerinden hesaplanıyor. Yeni satın alma siparişi oluşturmayı düşünün.
        </span>
      </div>
      <Link
        href="/purchasing"
        className="inline-flex shrink-0 items-center gap-1 rounded-md bg-amber-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-amber-700 transition-colors"
      >
        Satın Alma
        <ArrowRight className="h-3 w-3" />
      </Link>
      <button
        onClick={() => setDismissed(true)}
        className="shrink-0 rounded-md p-1 text-amber-600 hover:bg-amber-100 dark:text-amber-400 dark:hover:bg-amber-900"
        aria-label="Kapat"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
