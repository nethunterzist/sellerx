"use client";

import { cn } from "@/lib/utils";
import { Trophy, TrendingDown, Users, Target } from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Skeleton } from "@/components/ui/skeleton";
import type { BuyboxSummary } from "@/types/product";

interface BuyboxSummaryCardsProps {
  summary?: BuyboxSummary;
  isLoading?: boolean;
}

export function BuyboxSummaryCards({ summary, isLoading = false }: BuyboxSummaryCardsProps) {
  const cards = [
    {
      title: "Buybox Kazanan",
      value: summary ? `${summary.buyboxWinning} ürün` : "-",
      subValue: summary ? `Toplam ${summary.totalProducts} üründen` : "",
      icon: Trophy,
      iconBg: "bg-green-100 dark:bg-green-900/30",
      iconColor: "text-green-600",
      tooltip: "Buybox'ı kazandığınız ürün sayısı (1. sırada)",
    },
    {
      title: "Buybox Kaybedilen",
      value: summary ? `${summary.buyboxLosing} ürün` : "-",
      subValue: summary ? "Rakipler önde" : "",
      icon: TrendingDown,
      iconBg: "bg-red-100 dark:bg-red-900/30",
      iconColor: "text-red-600",
      tooltip: "Buybox sıralamasında 1. sırada olmadığınız ürünler",
      highlight: summary ? summary.buyboxLosing > 0 : false,
    },
    {
      title: "Rakipli Ürünler",
      value: summary ? `${summary.withCompetitors} ürün` : "-",
      subValue: summary ? "Birden fazla satıcı" : "",
      icon: Users,
      iconBg: "bg-orange-100 dark:bg-orange-900/30",
      iconColor: "text-orange-600",
      tooltip: "Aynı ürünü satan birden fazla satıcının olduğu ürünler",
    },
    {
      title: "Kazanma Oranı",
      value: summary ? `%${summary.winRate.toFixed(1)}` : "-",
      subValue: summary ? `${summary.buyboxWinning}/${summary.buyboxWinning + summary.buyboxLosing}` : "",
      icon: Target,
      iconBg: "bg-blue-100 dark:bg-blue-900/30",
      iconColor: "text-blue-600",
      tooltip: "Buybox kazanan / (Kazanan + Kaybeden) oranı",
    },
  ];

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[...Array(4)].map((_, index) => (
          <div key={index} className="bg-card rounded-lg border border-border p-4">
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
                  card.highlight && "border-red-300 bg-red-50 dark:bg-red-900/20"
                )}
              >
                <div className="flex items-start gap-3">
                  <div className={cn("h-10 w-10 rounded-full flex items-center justify-center flex-shrink-0", card.iconBg)}>
                    <card.icon className={cn("h-5 w-5", card.iconColor)} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm text-muted-foreground truncate">{card.title}</p>
                    <p className={cn("text-xl font-bold text-foreground truncate", card.highlight && "text-red-600")}>
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
