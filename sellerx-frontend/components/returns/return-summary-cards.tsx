"use client";

import { cn } from "@/lib/utils";
import { Package, Percent, TrendingDown, Calculator } from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrency } from "@/lib/contexts/currency-context";

interface ReturnSummaryCardsProps {
  totalReturns: number;
  totalReturnedItems: number;
  returnRate: number;
  totalReturnLoss: number;
  avgLossPerReturn: number;
  isLoading?: boolean;
}

function formatPercentage(value: number): string {
  return value.toFixed(2);
}

export function ReturnSummaryCards({
  totalReturns,
  totalReturnedItems,
  returnRate,
  totalReturnLoss,
  avgLossPerReturn,
  isLoading = false,
}: ReturnSummaryCardsProps) {
  const { formatCurrency } = useCurrency();

  const cards = [
    {
      title: "Toplam İade",
      value: totalReturns.toString(),
      subValue: `${totalReturnedItems} adet ürün`,
      icon: Package,
      iconBg: "bg-red-100",
      iconColor: "text-red-600",
      tooltip: "Toplam iade edilen sipariş ve ürün sayısı",
    },
    {
      title: "İade Oranı",
      value: `%${formatPercentage(returnRate)}`,
      subValue: "Sipariş bazlı",
      icon: Percent,
      iconBg: "bg-orange-100",
      iconColor: "text-orange-600",
      tooltip: "İade edilen sipariş sayısı / Toplam sipariş sayısı",
      highlight: returnRate > 5,
    },
    {
      title: "Toplam Zarar",
      value: formatCurrency(totalReturnLoss),
      subValue: "Tahmini toplam kayıp",
      icon: TrendingDown,
      iconBg: "bg-red-100",
      iconColor: "text-red-600",
      tooltip: "Ürün maliyeti + Kargo + Komisyon + Ambalaj",
    },
    {
      title: "Ortalama Zarar/İade",
      value: formatCurrency(avgLossPerReturn),
      subValue: "İade başına maliyet",
      icon: Calculator,
      iconBg: "bg-purple-100",
      iconColor: "text-purple-600",
      tooltip: "Toplam zarar / İade sayısı",
    },
  ];

  // Skeleton loading state
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[...Array(4)].map((_, index) => (
          <div
            key={index}
            className="bg-card rounded-lg border border-border p-4"
          >
            <div className="flex items-start gap-3">
              <Skeleton className="h-10 w-10 rounded-full flex-shrink-0" />
              <div className="min-w-0 flex-1 space-y-2">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-7 w-28" />
                <Skeleton className="h-3 w-24" />
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => (
        <TooltipProvider key={card.title}>
          <Tooltip>
            <TooltipTrigger asChild>
              <div
                className={cn(
                  "bg-card rounded-lg border border-border p-4 cursor-help transition-shadow hover:shadow-md",
                  card.highlight && "border-orange-300 bg-orange-50 dark:bg-orange-900/20"
                )}
              >
                <div className="flex items-start gap-3">
                  <div
                    className={cn(
                      "h-10 w-10 rounded-full flex items-center justify-center flex-shrink-0",
                      card.iconBg
                    )}
                  >
                    <card.icon className={cn("h-5 w-5", card.iconColor)} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm text-muted-foreground truncate">{card.title}</p>
                    <p
                      className={cn(
                        "text-xl font-bold text-foreground truncate",
                        card.highlight && "text-orange-600"
                      )}
                    >
                      {card.value}
                    </p>
                    <p className="text-xs text-muted-foreground truncate">{card.subValue}</p>
                  </div>
                </div>
              </div>
            </TooltipTrigger>
            <TooltipContent>
              <p>{card.tooltip}</p>
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>
      ))}
    </div>
  );
}
