"use client";

import Image from "next/image";
import { Button } from "@/components/ui/button";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";
import type { RecommendedProduct } from "@/types/cross-sell";

interface CrossSellProductCardProps {
  product: RecommendedProduct;
  onRemove?: () => void;
  readonly?: boolean;
  compact?: boolean;
}

export function CrossSellProductCard({
  product,
  onRemove,
  readonly = false,
  compact = false,
}: CrossSellProductCardProps) {
  return (
    <div
      className={cn(
        "flex items-center gap-3 rounded-lg border bg-card p-2",
        compact ? "p-1.5" : "p-2"
      )}
    >
      {/* Product image */}
      <div
        className={cn(
          "relative shrink-0 rounded-md overflow-hidden bg-muted",
          compact ? "h-8 w-8" : "h-10 w-10"
        )}
      >
        {product.image ? (
          <Image
            src={product.image}
            alt={product.title}
            fill
            className="object-cover"
            sizes="40px"
          />
        ) : (
          <div className="h-full w-full flex items-center justify-center text-xs text-muted-foreground">
            ?
          </div>
        )}
      </div>

      {/* Product info */}
      <div className="flex-1 min-w-0">
        <p className={cn("font-medium truncate", compact ? "text-xs" : "text-sm")}>
          {product.title}
        </p>
        <p className="text-xs text-muted-foreground">
          {product.barcode} - {product.salePrice.toFixed(2)} TL
        </p>
      </div>

      {/* Remove button */}
      {!readonly && onRemove && (
        <Button
          variant="ghost"
          size="icon"
          className="h-6 w-6 shrink-0"
          onClick={onRemove}
        >
          <X className="h-3.5 w-3.5" />
        </Button>
      )}
    </div>
  );
}
