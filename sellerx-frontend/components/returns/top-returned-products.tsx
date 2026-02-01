"use client";

import { cn } from "@/lib/utils";
import { AlertTriangle, TrendingUp, Package } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import type { TopReturnedProduct } from "@/types/returns";
import { useCurrency } from "@/lib/contexts/currency-context";

interface TopReturnedProductsProps {
  products: TopReturnedProduct[];
  isLoading?: boolean;
}

function formatPercentage(value: number): string {
  return value.toFixed(1);
}

const PRODUCT_NAME_LIMIT = 40;

function truncateText(text: string, limit: number): string {
  if (text.length <= limit) return text;
  return text.slice(0, limit) + "...";
}

const riskColors: Record<string, { bg: string; text: string; label: string }> = {
  CRITICAL: { bg: "bg-red-100 dark:bg-red-900/30", text: "text-red-700 dark:text-red-400", label: "Kritik" },
  HIGH: { bg: "bg-orange-100 dark:bg-orange-900/30", text: "text-orange-700 dark:text-orange-400", label: "Yüksek" },
  MEDIUM: { bg: "bg-yellow-100 dark:bg-yellow-900/30", text: "text-yellow-700 dark:text-yellow-400", label: "Orta" },
  LOW: { bg: "bg-green-100 dark:bg-green-900/30", text: "text-green-700 dark:text-green-400", label: "Düşük" },
};

export function TopReturnedProducts({
  products,
  isLoading = false,
}: TopReturnedProductsProps) {
  const { formatCurrency } = useCurrency();

  if (isLoading) {
    return (
      <div className="bg-card rounded-lg border border-border">
        <div className="p-4 border-b border-border">
          <div className="flex items-center justify-between">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-4 w-16" />
          </div>
        </div>
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[300px]">Ürün</TableHead>
                <TableHead className="text-center">İade / Satış</TableHead>
                <TableHead className="text-center">İade Oranı</TableHead>
                <TableHead className="text-right">Toplam Zarar</TableHead>
                <TableHead className="text-center">Risk</TableHead>
                <TableHead>Sebepler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {[...Array(5)].map((_, index) => (
                <TableRow key={index}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <Skeleton className="h-10 w-10 rounded" />
                      <div className="min-w-0 space-y-1">
                        <Skeleton className="h-4 w-40" />
                        <Skeleton className="h-3 w-24" />
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-center">
                    <Skeleton className="h-4 w-16 mx-auto" />
                  </TableCell>
                  <TableCell className="text-center">
                    <Skeleton className="h-4 w-12 mx-auto" />
                  </TableCell>
                  <TableCell className="text-right">
                    <Skeleton className="h-4 w-20 ml-auto" />
                  </TableCell>
                  <TableCell className="text-center">
                    <Skeleton className="h-6 w-16 mx-auto rounded-full" />
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-1">
                      <Skeleton className="h-5 w-16 rounded" />
                      <Skeleton className="h-5 w-14 rounded" />
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
    );
  }

  if (products.length === 0) {
    return (
      <div className="bg-card rounded-lg border border-border p-6">
        <h3 className="text-lg font-semibold text-foreground mb-4">
          En Çok İade Edilen Ürünler
        </h3>
        <div className="h-64 flex items-center justify-center flex-col gap-2">
          <Package className="h-12 w-12 text-muted-foreground" />
          <p className="text-muted-foreground">Henüz iade verisi bulunmuyor</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-card rounded-lg border border-border">
      <div className="p-4 border-b border-border">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-foreground">
            En Çok İade Edilen Ürünler
          </h3>
          <span className="text-sm text-muted-foreground">{products.length} ürün</span>
        </div>
      </div>
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[300px]">Ürün</TableHead>
              <TableHead className="text-center">İade / Satış</TableHead>
              <TableHead className="text-center">İade Oranı</TableHead>
              <TableHead className="text-right">Toplam Zarar</TableHead>
              <TableHead className="text-center">Risk</TableHead>
              <TableHead>Sebepler</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {products.map((product, index) => {
              const risk = riskColors[product.riskLevel] || riskColors.LOW;
              return (
                <TableRow key={product.barcode || index}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      {product.imageUrl ? (
                        <div className="flex-shrink-0 group relative">
                          <img
                            src={product.imageUrl}
                            alt={product.productName}
                            className="h-10 w-10 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                            onError={(e) => {
                              (e.target as HTMLImageElement).src = "https://via.placeholder.com/40?text=No+Image";
                            }}
                          />
                        </div>
                      ) : (
                        <div className="h-10 w-10 rounded bg-muted flex items-center justify-center group relative">
                          <Package className="h-5 w-5 text-muted-foreground" />
                        </div>
                      )}
                      <div className="min-w-0">
                        {product.productName.length > PRODUCT_NAME_LIMIT ? (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <p className="font-medium text-foreground cursor-default">
                                {truncateText(product.productName, PRODUCT_NAME_LIMIT)}
                              </p>
                            </TooltipTrigger>
                            <TooltipContent side="top" className="max-w-[300px]">
                              <p>{product.productName}</p>
                            </TooltipContent>
                          </Tooltip>
                        ) : (
                          <p className="font-medium text-foreground">
                            {product.productName}
                          </p>
                        )}
                        <p className="text-xs text-muted-foreground">{product.barcode}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-center">
                    <span className="text-red-600 dark:text-red-400 font-medium">
                      {product.returnCount}
                    </span>
                    <span className="text-muted-foreground mx-1">/</span>
                    <span className="text-foreground">{product.soldCount}</span>
                  </TableCell>
                  <TableCell className="text-center">
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger>
                          <span
                            className={cn(
                              "inline-flex items-center gap-1 font-medium",
                              product.returnRate > 10
                                ? "text-red-600 dark:text-red-400"
                                : product.returnRate > 5
                                ? "text-orange-600 dark:text-orange-400"
                                : "text-foreground"
                            )}
                          >
                            {product.returnRate > 10 && (
                              <AlertTriangle className="h-3 w-3" />
                            )}
                            %{formatPercentage(product.returnRate)}
                          </span>
                        </TooltipTrigger>
                        <TooltipContent>
                          <p>
                            {product.returnRate > 10
                              ? "Kritik seviyede iade oranı!"
                              : product.returnRate > 5
                              ? "Yüksek iade oranı"
                              : "Normal seviyede"}
                          </p>
                        </TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                  </TableCell>
                  <TableCell className="text-right">
                    <span className="font-medium text-red-600">
                      {formatCurrency(product.totalLoss)}
                    </span>
                  </TableCell>
                  <TableCell className="text-center">
                    <span
                      className={cn(
                        "inline-flex px-2 py-1 text-xs font-medium rounded-full",
                        risk.bg,
                        risk.text
                      )}
                    >
                      {risk.label}
                    </span>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {product.topReasons.slice(0, 2).map((reason, idx) => (
                        <span
                          key={idx}
                          className="inline-flex px-2 py-0.5 text-xs bg-muted text-muted-foreground rounded"
                        >
                          {reason}
                        </span>
                      ))}
                      {product.topReasons.length > 2 && (
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger>
                              <span className="inline-flex px-2 py-0.5 text-xs bg-muted text-muted-foreground rounded cursor-help">
                                +{product.topReasons.length - 2}
                              </span>
                            </TooltipTrigger>
                            <TooltipContent>
                              <ul className="text-sm">
                                {product.topReasons.slice(2).map((reason, idx) => (
                                  <li key={idx}>{reason}</li>
                                ))}
                              </ul>
                            </TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
