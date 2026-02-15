"use client";

import { useState, useRef, useEffect } from "react";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { useProductSearch } from "@/hooks/queries/use-cross-sell";
import { CrossSellProductCard } from "./cross-sell-product-card";
import { Search, Loader2, Package } from "lucide-react";
import { cn } from "@/lib/utils";
import type { RecommendedProduct, ProductSearchResult } from "@/types/cross-sell";

interface CrossSellProductSelectorProps {
  storeId: string;
  selectedProducts: RecommendedProduct[];
  onProductsChange: (products: RecommendedProduct[]) => void;
  maxProducts?: number;
}

export function CrossSellProductSelector({
  storeId,
  selectedProducts,
  onProductsChange,
  maxProducts = 5,
}: CrossSellProductSelectorProps) {
  const t = useTranslations("qa.crossSell.productSelector");
  const [searchQuery, setSearchQuery] = useState("");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const { data: searchResults, isLoading } = useProductSearch(
    storeId,
    searchQuery
  );

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const selectedBarcodes = new Set(selectedProducts.map((p) => p.barcode));

  const filteredResults = (searchResults || []).filter(
    (result) => !selectedBarcodes.has(result.barcode)
  );

  const handleSelectProduct = (product: ProductSearchResult) => {
    if (selectedProducts.length >= maxProducts) return;

    const newProduct: RecommendedProduct = {
      barcode: product.barcode,
      title: product.title,
      image: product.image,
      salePrice: product.salePrice,
      displayOrder: selectedProducts.length,
    };

    onProductsChange([...selectedProducts, newProduct]);
    setSearchQuery("");
    setIsDropdownOpen(false);
  };

  const handleRemoveProduct = (barcode: string) => {
    const updated = selectedProducts
      .filter((p) => p.barcode !== barcode)
      .map((p, i) => ({ ...p, displayOrder: i }));
    onProductsChange(updated);
  };

  const isAtLimit = selectedProducts.length >= maxProducts;

  return (
    <div className="space-y-3">
      {/* Search input */}
      <div ref={containerRef} className="relative">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              if (e.target.value.length >= 2) {
                setIsDropdownOpen(true);
              }
            }}
            onFocus={() => {
              if (searchQuery.length >= 2) setIsDropdownOpen(true);
            }}
            placeholder={
              isAtLimit
                ? t("limitReached", { max: maxProducts })
                : t("searchPlaceholder")
            }
            className="pl-9"
            disabled={isAtLimit}
          />
          {isLoading && (
            <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 animate-spin text-muted-foreground" />
          )}
        </div>

        {/* Search results dropdown */}
        {isDropdownOpen && searchQuery.length >= 2 && (
          <div className="absolute z-50 mt-1 w-full max-h-64 overflow-y-auto rounded-lg border bg-popover shadow-lg">
            {isLoading ? (
              <div className="flex items-center justify-center gap-2 p-4 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" />
                {t("searching")}
              </div>
            ) : filteredResults.length === 0 ? (
              <div className="flex items-center justify-center gap-2 p-4 text-sm text-muted-foreground">
                <Package className="h-4 w-4" />
                {t("noResults")}
              </div>
            ) : (
              filteredResults.map((product) => (
                <button
                  key={product.barcode}
                  type="button"
                  onClick={() => handleSelectProduct(product)}
                  className={cn(
                    "flex items-center gap-3 w-full px-3 py-2 text-left text-sm",
                    "hover:bg-muted/50 transition-colors",
                    "border-b last:border-b-0"
                  )}
                >
                  <div className="h-8 w-8 shrink-0 rounded bg-muted overflow-hidden">
                    {product.image ? (
                      <img
                        src={product.image}
                        alt=""
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="h-full w-full flex items-center justify-center text-xs text-muted-foreground">
                        ?
                      </div>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium truncate">{product.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {product.barcode} - {product.salePrice.toFixed(2)} TL
                      {product.trendyolQuantity <= 0 && (
                        <span className="ml-1 text-red-500">
                          ({t("outOfStock")})
                        </span>
                      )}
                    </p>
                  </div>
                </button>
              ))
            )}
          </div>
        )}
      </div>

      {/* Selected products */}
      {selectedProducts.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs text-muted-foreground">
            {t("selectedCount", {
              count: selectedProducts.length,
              max: maxProducts,
            })}
          </p>
          <div className="space-y-1.5">
            {selectedProducts.map((product) => (
              <CrossSellProductCard
                key={product.barcode}
                product={product}
                onRemove={() => handleRemoveProduct(product.barcode)}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
