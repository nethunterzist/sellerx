"use client";

import { cn } from "@/lib/utils";
import { AlertTriangle } from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useCurrency } from "@/lib/contexts/currency-context";

type CardVariant = "today" | "yesterday" | "thisMonth" | "thisMonthForecast" | "lastMonth" | "custom";

interface PeriodCardProps {
  title: string;
  dateRange: string;
  sales: number;
  ordersUnits: string;
  refunds: number;
  invoicedDeductions: number; // Kesilen Faturalar (REKLAM + CEZA + ULUSLARARASI + DIGER - IADE)
  grossProfit: number;
  netProfit: number;
  vatDifference?: number;
  stoppage?: number;
  commission?: number;
  productCosts?: number;
  shippingCost?: number;
  itemsWithoutCost?: number;
  variant?: CardVariant;
  percentageChange?: number;
  salesChange?: number;
  netProfitChange?: number;
  isSelected?: boolean;
  disabled?: boolean; // Tıklanamaz kartlar için (örn: forecast)
  onClick?: () => void;
  onDetailClick?: () => void;
}

const headerColors: Record<CardVariant, string> = {
  today: "bg-[#3B82F6]", // Blue
  yesterday: "bg-[#14B8A6]", // Teal
  thisMonth: "bg-[#0D9488]", // Dark Teal
  thisMonthForecast: "bg-[#047857]", // Dark Green
  lastMonth: "bg-[#F59E0B]", // Orange
  custom: "bg-[#8B5CF6]", // Purple/Violet
};

const borderColors: Record<CardVariant, string> = {
  today: "border-b-[#3B82F6]", // Blue
  yesterday: "border-b-[#14B8A6]", // Teal
  thisMonth: "border-b-[#0D9488]", // Dark Teal
  thisMonthForecast: "border-b-[#047857]", // Dark Green
  lastMonth: "border-b-[#F59E0B]", // Orange
  custom: "border-b-[#8B5CF6]", // Purple/Violet
};

const hoverBorderColors: Record<CardVariant, string> = {
  today: "hover:border-b-[rgba(59,130,246,0.5)]", // Blue 50% opacity
  yesterday: "hover:border-b-[rgba(20,184,166,0.5)]", // Teal 50% opacity
  thisMonth: "hover:border-b-[rgba(13,148,136,0.5)]", // Dark Teal 50% opacity
  thisMonthForecast: "hover:border-b-[rgba(4,120,87,0.5)]", // Dark Green 50% opacity
  lastMonth: "hover:border-b-[rgba(245,158,11,0.5)]", // Orange 50% opacity
  custom: "hover:border-b-[rgba(139,92,246,0.5)]", // Purple/Violet 50% opacity
};

function formatPercentage(value: number): string {
  const formatted = Math.abs(value).toFixed(1);
  return value >= 0 ? `+${formatted}%` : `-${formatted}%`;
}

export function PeriodCard({
  title,
  dateRange,
  sales,
  ordersUnits,
  refunds,
  invoicedDeductions,
  grossProfit,
  netProfit,
  vatDifference = 0,
  stoppage = 0,
  commission = 0,
  productCosts = 0,
  shippingCost = 0,
  itemsWithoutCost = 0,
  variant = "today",
  percentageChange,
  salesChange,
  netProfitChange,
  isSelected,
  disabled = false,
  onClick,
  onDetailClick,
}: PeriodCardProps) {
  const { formatCurrency } = useCurrency();
  const headerColor = headerColors[variant];
  const borderColor = borderColors[variant];
  const hoverBorderColor = hoverBorderColors[variant];

  return (
    <div
      className={cn(
        "flex flex-col rounded-lg overflow-hidden min-w-[220px] flex-1 bg-card border border-border shadow-sm transition-all border-b-4",
        disabled
          ? "cursor-default border-b-transparent" // Disabled: no pointer, no hover border
          : cn(
              "cursor-pointer hover:shadow-md",
              isSelected ? borderColor : `border-b-transparent ${hoverBorderColor}`
            )
      )}
      onClick={disabled ? undefined : onClick}
    >
      {/* Colored Header */}
      <div className={cn("px-4 py-3", headerColor)}>
        <h3 className="text-sm font-semibold text-white">{title}</h3>
        <p className="text-xs text-white/80">{dateRange}</p>
      </div>

      {/* Card Body */}
      <div className="p-4 flex flex-col flex-1">
        {/* Sales */}
        <div className="mb-4">
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Satışlar</span>
            {salesChange !== undefined && (
              <span className={cn(
                "text-xs font-medium",
                salesChange >= 0 ? "text-green-600" : "text-red-600"
              )}>
                {formatPercentage(salesChange)}
              </span>
            )}
          </div>
          <p className="text-2xl font-bold text-foreground">
            {formatCurrency(sales)}
          </p>
        </div>

        {/* Orders & Refunds */}
        <div className="flex justify-between text-xs mb-3">
          <div>
            <span className="text-muted-foreground">Sipariş / Adet</span>
            <p className="font-medium text-foreground">{ordersUnits}</p>
          </div>
          <div className="text-right">
            <span className="text-muted-foreground">İadeler</span>
            <p className={cn(
              "font-medium",
              refunds > 0 ? "text-blue-600" : "text-foreground"
            )}>
              {refunds}
            </p>
          </div>
        </div>

        {/* Ürün Maliyeti & Kesilen Faturalar */}
        <div className="flex justify-between text-xs mb-3">
          <div>
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <div className="cursor-help">
                    <span className="text-muted-foreground flex items-center gap-1">
                      Ürün Maliyeti
                      {itemsWithoutCost > 0 && (
                        <AlertTriangle className="h-3 w-3 text-amber-500" />
                      )}
                    </span>
                    <p className="font-medium text-orange-600">
                      {formatCurrency(-productCosts)}
                    </p>
                  </div>
                </TooltipTrigger>
                <TooltipContent>
                  <p>Satılan ürünlerin toplam maliyeti</p>
                  {itemsWithoutCost > 0 && (
                    <p className="text-amber-500 text-xs mt-1">
                      {itemsWithoutCost} ürünün maliyeti eksik
                    </p>
                  )}
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </div>
          <div className="text-right">
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <div className="cursor-help">
                    <span className="text-muted-foreground">Kesilen Faturalar</span>
                    <p className="font-medium text-red-600">
                      {formatCurrency(-invoicedDeductions)}
                    </p>
                  </div>
                </TooltipTrigger>
                <TooltipContent>
                  <p>Reklam, ceza ve diğer kesintiler</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    Komisyon ve kargo hariç
                  </p>
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </div>
        </div>

        {/* Komisyon & Kargo Maliyeti */}
        <div className="flex justify-between text-xs mb-3">
          <div>
            <span className="text-muted-foreground">Komisyon</span>
            <p className="font-medium text-red-600">
              {formatCurrency(-commission)}
            </p>
          </div>
          <div className="text-right">
            <span className="text-muted-foreground">Kargo Maliyeti</span>
            <p className="font-medium text-orange-600">
              {formatCurrency(-shippingCost)}
            </p>
          </div>
        </div>

        {/* Gross & Net Profit */}
        <div className="flex justify-between text-xs border-t border-border pt-3 mt-auto">
          <div>
            <span className="text-muted-foreground">Brüt Kâr</span>
            <p className={cn(
              "font-medium",
              grossProfit >= 0 ? "text-green-600" : "text-red-600"
            )}>
              {formatCurrency(grossProfit)}
            </p>
          </div>
          <div className="text-right">
            <div className="flex items-center justify-end gap-1">
              <span className="text-muted-foreground">Net Kâr</span>
              {netProfitChange !== undefined && (
                <span className={cn(
                  "text-xs font-medium",
                  netProfitChange >= 0 ? "text-green-600" : "text-red-600"
                )}>
                  {formatPercentage(netProfitChange)}
                </span>
              )}
            </div>
            <p className={cn(
              "font-medium",
              netProfit >= 0 ? "text-green-600" : "text-red-600"
            )}>
              {formatCurrency(netProfit)}
            </p>
          </div>
        </div>

        {/* Detay Bölümü - Gri çizgi ve ortalanmış buton */}
        <div className="mt-2 pt-2 border-t border-border">
          <button
            className="w-full text-xs font-medium text-muted-foreground hover:text-foreground transition-colors"
            onClick={(e) => {
              e.stopPropagation();
              onDetailClick?.();
            }}
          >
            Detay
          </button>
        </div>
      </div>
    </div>
  );
}
