"use client";

import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  TrendingUp,
  TrendingDown,
  ExternalLink,
  ChevronDown,
  ChevronUp,
  Scale,
} from "lucide-react";

interface ProductData {
  productName: string;
  barcode: string;
  brand?: string;
  image?: string;
  productUrl?: string;
  revenue: number;
  grossProfit: number;
}

interface ProfitLossSideBySideProps {
  products?: ProductData[];
  isLoading?: boolean;
  formatCurrency: (value: number) => string;
}

const INITIAL_VISIBLE_COUNT = 10;
const PRODUCT_NAME_LIMIT = 35;

function truncateText(text: string, limit: number): string {
  if (text.length <= limit) return text;
  return text.slice(0, limit) + "...";
}

function ProductListSkeleton() {
  return (
    <div className="space-y-2">
      {[1, 2, 3, 4, 5].map((i) => (
        <div key={i} className="flex items-center justify-between py-2">
          <div className="flex items-center gap-2">
            <Skeleton className="h-8 w-8 rounded" />
            <Skeleton className="h-4 w-32" />
          </div>
          <Skeleton className="h-4 w-16" />
        </div>
      ))}
    </div>
  );
}

function ProductItem({
  product,
  formatCurrency,
  isProfit,
}: {
  product: ProductData & { margin: number };
  formatCurrency: (value: number) => string;
  isProfit: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-border last:border-0 hover:bg-muted/50 rounded-md px-2 -mx-2">
      <div className="flex items-center gap-2 min-w-0 flex-1">
        {/* Product Image */}
        {product.productUrl ? (
          <a
            href={product.productUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-shrink-0 group relative"
          >
            {product.image ? (
              <img
                src={product.image}
                alt={product.productName}
                className="h-8 w-8 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                onError={(e) => {
                  (e.target as HTMLImageElement).src =
                    "https://via.placeholder.com/32?text=No";
                }}
              />
            ) : (
              <div className="h-8 w-8 rounded bg-[#F27A1A] flex items-center justify-center text-xs font-bold text-white group-hover:ring-2 ring-[#F27A1A] transition-all">
                T
              </div>
            )}
            <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
              <ExternalLink className="h-2 w-2 text-white" />
            </div>
          </a>
        ) : product.image ? (
          <img
            src={product.image}
            alt={product.productName}
            className="h-8 w-8 rounded object-cover flex-shrink-0 border border-border"
            onError={(e) => {
              (e.target as HTMLImageElement).src =
                "https://via.placeholder.com/32?text=No";
            }}
          />
        ) : (
          <div className="h-8 w-8 rounded bg-muted flex items-center justify-center text-xs font-medium text-muted-foreground flex-shrink-0">
            ?
          </div>
        )}

        {/* Product Name */}
        <div className="min-w-0">
          {product.productName.length > PRODUCT_NAME_LIMIT ? (
            <Tooltip>
              <TooltipTrigger asChild>
                {product.productUrl ? (
                  <a
                    href={product.productUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer block truncate"
                  >
                    {truncateText(product.productName, PRODUCT_NAME_LIMIT)}
                  </a>
                ) : (
                  <span className="text-sm text-foreground block truncate">
                    {truncateText(product.productName, PRODUCT_NAME_LIMIT)}
                  </span>
                )}
              </TooltipTrigger>
              <TooltipContent side="top" className="max-w-[250px]">
                <p>{product.productName}</p>
              </TooltipContent>
            </Tooltip>
          ) : product.productUrl ? (
            <a
              href={product.productUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-foreground hover:text-[#F27A1A] hover:underline cursor-pointer block truncate"
            >
              {product.productName}
            </a>
          ) : (
            <span className="text-sm text-foreground block truncate">
              {product.productName}
            </span>
          )}
          <span className="text-xs text-muted-foreground">
            {product.margin.toFixed(1)}% marj
          </span>
        </div>
      </div>

      {/* Profit/Loss Amount */}
      <span
        className={cn(
          "font-medium text-sm flex-shrink-0 ml-2",
          isProfit
            ? "text-green-600 dark:text-green-400"
            : "text-red-600 dark:text-red-400"
        )}
      >
        {isProfit ? "+" : ""}
        {formatCurrency(product.grossProfit)}
      </span>
    </div>
  );
}

function ProductList({
  products,
  formatCurrency,
  isProfit,
  totalAmount,
  isLoading,
}: {
  products: (ProductData & { margin: number })[];
  formatCurrency: (value: number) => string;
  isProfit: boolean;
  totalAmount: number;
  isLoading?: boolean;
}) {
  const [visibleCount] = useState(INITIAL_VISIBLE_COUNT);
  const [isExpanded, setIsExpanded] = useState(false);

  const visibleProducts = isExpanded
    ? products
    : products.slice(0, visibleCount);
  const hasMore = products.length > visibleCount;

  const toggleExpand = () => {
    if (isExpanded) {
      setIsExpanded(false);
    } else {
      setIsExpanded(true);
    }
  };

  if (isLoading) {
    return <ProductListSkeleton />;
  }

  if (products.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        <p className="text-sm">
          {isProfit ? "Kar eden urun yok" : "Zarar eden urun yok"}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {/* Summary */}
      <div
        className={cn(
          "flex justify-between items-center py-2 px-3 rounded-lg mb-3",
          isProfit
            ? "bg-green-100 dark:bg-green-900/30"
            : "bg-red-100 dark:bg-red-900/30"
        )}
      >
        <span className="text-sm font-medium">
          Toplam ({products.length} urun)
        </span>
        <span
          className={cn(
            "font-bold",
            isProfit
              ? "text-green-700 dark:text-green-300"
              : "text-red-700 dark:text-red-300"
          )}
        >
          {isProfit ? "+" : ""}
          {formatCurrency(totalAmount)}
        </span>
      </div>

      {/* Product List */}
      <div className="space-y-1">
        {visibleProducts.map((product) => (
          <ProductItem
            key={product.barcode}
            product={product}
            formatCurrency={formatCurrency}
            isProfit={isProfit}
          />
        ))}
      </div>

      {/* Show More/Less Button */}
      {hasMore && (
        <Button
          variant="ghost"
          size="sm"
          onClick={toggleExpand}
          className="w-full mt-2 text-muted-foreground hover:text-foreground"
        >
          {isExpanded ? (
            <>
              <ChevronUp className="h-4 w-4 mr-1" />
              Daha az goster
            </>
          ) : (
            <>
              <ChevronDown className="h-4 w-4 mr-1" />
              Tumu goster ({products.length - visibleCount} daha)
            </>
          )}
        </Button>
      )}
    </div>
  );
}

export function ProfitLossSideBySide({
  products,
  isLoading,
  formatCurrency,
}: ProfitLossSideBySideProps) {
  // Split products into profitable and loss-making
  const { profitableProducts, lossProducts, totalProfit, totalLoss } =
    useMemo(() => {
      if (!products) {
        return {
          profitableProducts: [],
          lossProducts: [],
          totalProfit: 0,
          totalLoss: 0,
        };
      }

      const profitable: (ProductData & { margin: number })[] = [];
      const loss: (ProductData & { margin: number })[] = [];
      let profitSum = 0;
      let lossSum = 0;

      products.forEach((p) => {
        const margin = p.revenue > 0 ? (p.grossProfit / p.revenue) * 100 : 0;
        const productWithMargin = { ...p, margin };

        if (p.grossProfit > 0) {
          profitable.push(productWithMargin);
          profitSum += p.grossProfit;
        } else if (p.grossProfit < 0) {
          loss.push(productWithMargin);
          lossSum += p.grossProfit;
        }
      });

      // Sort by absolute profit/loss amount (highest first)
      profitable.sort((a, b) => b.grossProfit - a.grossProfit);
      loss.sort((a, b) => a.grossProfit - b.grossProfit);

      return {
        profitableProducts: profitable,
        lossProducts: loss,
        totalProfit: profitSum,
        totalLoss: lossSum,
      };
    }, [products]);

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Scale className="h-5 w-5" />
            Kar/Zarar Karsilastirma
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid md:grid-cols-2 gap-6">
            <ProductListSkeleton />
            <ProductListSkeleton />
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!products || products.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Scale className="h-5 w-5" />
            Kar/Zarar Karsilastirma
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8 text-muted-foreground">
            <Scale className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p className="text-sm">Henuz urun verisi yok</p>
            <p className="text-xs mt-1">
              Satis yapmaya basladiginizda urunler burada gorunecek
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-lg flex items-center gap-2">
          <Scale className="h-5 w-5" />
          Kar/Zarar Karsilastirma
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid md:grid-cols-2 gap-6">
          {/* Profitable Products Column */}
          <div>
            <div className="flex items-center gap-2 mb-3 pb-2 border-b border-green-200 dark:border-green-800">
              <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400" />
              <h3 className="font-semibold text-green-700 dark:text-green-300">
                Kar Eden Urunler
              </h3>
            </div>
            <ProductList
              products={profitableProducts}
              formatCurrency={formatCurrency}
              isProfit={true}
              totalAmount={totalProfit}
              isLoading={isLoading}
            />
          </div>

          {/* Loss Products Column */}
          <div>
            <div className="flex items-center gap-2 mb-3 pb-2 border-b border-red-200 dark:border-red-800">
              <TrendingDown className="h-5 w-5 text-red-600 dark:text-red-400" />
              <h3 className="font-semibold text-red-700 dark:text-red-300">
                Zarar Eden Urunler
              </h3>
            </div>
            <ProductList
              products={lossProducts}
              formatCurrency={formatCurrency}
              isProfit={false}
              totalAmount={totalLoss}
              isLoading={isLoading}
            />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
