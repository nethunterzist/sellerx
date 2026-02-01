"use client";

import { cn } from "@/lib/utils";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Receipt,
  Truck,
  Globe,
  AlertTriangle,
  Megaphone,
  RefreshCcw,
  FileText,
  TrendingDown,
  TrendingUp,
  Info,
} from "lucide-react";
import type { InvoiceSummary, CategorySummary } from "@/types/invoice";
import { getCategoryDisplayName } from "@/types/invoice";

type CategoryKey = "KOMISYON" | "KARGO" | "ULUSLARARASI" | "CEZA" | "REKLAM" | "IADE" | "DIGER" | "ALL" | "KESINTI";

interface InvoiceSummaryCardsProps {
  summary: InvoiceSummary | undefined;
  selectedCategory: CategoryKey | null;
  onCategorySelect: (category: CategoryKey | null) => void;
  isLoading?: boolean;
}

// Category descriptions for tooltips
const categoryDescriptions: Record<string, string> = {
  ALL: "Tum kategorilerdeki faturalarin toplami",
  KESINTI: "Trendyol tarafindan kesilen tum bedeller",
  KOMISYON: "Trendyol'un satis basina aldigi komisyon bedelleri",
  KARGO: "Kargo ve teslimat ile ilgili masraflar",
  ULUSLARARASI: "Yurtdisi satis ve operasyon bedelleri",
  CEZA: "Ceza ve kesinti bedelleri (tedarik edememe, gecikme vb.)",
  REKLAM: "Reklam ve influencer kampanya bedelleri",
  IADE: "Iade ve itiraz sonucu geri alinan tutarlar",
  DIGER: "Diger finansal islemler",
};

// Category colors matching the plan
const categoryColors: Record<CategoryKey, { bg: string; border: string; hoverBorder: string }> = {
  ALL: {
    bg: "bg-gradient-to-r from-slate-600 to-slate-700",
    border: "border-b-slate-600",
    hoverBorder: "hover:border-b-slate-400",
  },
  KESINTI: {
    bg: "bg-red-600",
    border: "border-b-red-600",
    hoverBorder: "hover:border-b-red-400",
  },
  KOMISYON: {
    bg: "bg-blue-500",
    border: "border-b-blue-500",
    hoverBorder: "hover:border-b-blue-300",
  },
  KARGO: {
    bg: "bg-amber-500",
    border: "border-b-amber-500",
    hoverBorder: "hover:border-b-amber-300",
  },
  ULUSLARARASI: {
    bg: "bg-purple-500",
    border: "border-b-purple-500",
    hoverBorder: "hover:border-b-purple-300",
  },
  CEZA: {
    bg: "bg-red-500",
    border: "border-b-red-500",
    hoverBorder: "hover:border-b-red-300",
  },
  REKLAM: {
    bg: "bg-pink-500",
    border: "border-b-pink-500",
    hoverBorder: "hover:border-b-pink-300",
  },
  IADE: {
    bg: "bg-green-500",
    border: "border-b-green-500",
    hoverBorder: "hover:border-b-green-300",
  },
  DIGER: {
    bg: "bg-gray-500",
    border: "border-b-gray-500",
    hoverBorder: "hover:border-b-gray-300",
  },
};

// Category icons
const categoryIcons: Record<CategoryKey, React.ReactNode> = {
  ALL: <FileText className="h-4 w-4" />,
  KESINTI: <AlertTriangle className="h-4 w-4" />,
  KOMISYON: <Receipt className="h-4 w-4" />,
  KARGO: <Truck className="h-4 w-4" />,
  ULUSLARARASI: <Globe className="h-4 w-4" />,
  CEZA: <AlertTriangle className="h-4 w-4" />,
  REKLAM: <Megaphone className="h-4 w-4" />,
  IADE: <RefreshCcw className="h-4 w-4" />,
  DIGER: <FileText className="h-4 w-4" />,
};

interface SummaryCardProps {
  title: string;
  subtitle?: string;
  invoiceCount: number;
  totalAmount: number;
  vatAmount?: number;
  category: CategoryKey;
  isSelected: boolean;
  onClick: () => void;
  isTotal?: boolean;
}

// Format amount with +/- prefix based on category
function formatAmountWithSign(amount: number, category: CategoryKey, formatCurrency: (value: number) => string): string {
  // IADE is refund (money back to seller) = positive/+
  // ALL is net total, no prefix needed
  // Everything else is deduction = negative/-
  if (category === "ALL") {
    return formatCurrency(amount);
  }

  const isRefund = category === "IADE";
  const prefix = isRefund ? "+" : "-";
  return `${prefix}${formatCurrency(Math.abs(amount))}`;
}

// Default colors for unknown categories
const defaultColors = {
  bg: "bg-gray-500",
  border: "border-b-gray-500",
  hoverBorder: "hover:border-b-gray-300",
};

function SummaryCard({
  title,
  subtitle,
  invoiceCount,
  totalAmount,
  vatAmount,
  category,
  isSelected,
  onClick,
  isTotal = false,
}: SummaryCardProps) {
  const { formatCurrency } = useCurrency();
  const colors = categoryColors[category] || defaultColors;
  const icon = categoryIcons[category] || <FileText className="h-4 w-4" />;
  // IADE is refund (money back to seller) = green, everything else is deduction = red
  const isRefund = category === "IADE";
  const description = categoryDescriptions[category] || "Fatura kategorisi";

  return (
    <div
      className={cn(
        "flex flex-col rounded-lg overflow-hidden min-w-[220px] flex-1 bg-card border border-border shadow-sm transition-all border-b-4 cursor-pointer hover:shadow-md",
        isSelected ? colors.border : `border-b-transparent ${colors.hoverBorder}`
      )}
      onClick={onClick}
    >
      {/* Colored Header */}
      <div className={cn("px-4 py-3", colors.bg)}>
        <div className="flex items-center gap-2">
          <span className="text-white/80">{icon}</span>
          <h3 className="text-sm font-semibold text-white">{title}</h3>
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Info className="h-4 w-4 text-white/60 cursor-help hover:text-white/90 transition-colors ml-auto" />
              </TooltipTrigger>
              <TooltipContent side="top" className="max-w-xs">
                <p>{description}</p>
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>
        {subtitle && <p className="text-xs text-white/80 mt-0.5">{subtitle}</p>}
      </div>

      {/* Card Body */}
      <div className="p-4 flex flex-col flex-1">
        {/* Main Amount - Fixed height for alignment */}
        <div className="mb-4 h-[52px]">
          <span className="text-xs text-muted-foreground">Toplam Tutar</span>
          <div className="flex items-center gap-2 h-8">
            <p
              className={cn(
                "text-xl font-bold whitespace-nowrap",
                isTotal
                  ? "text-foreground"
                  : isRefund
                    ? "text-green-600"
                    : "text-red-600"
              )}
            >
              {formatAmountWithSign(totalAmount, category, formatCurrency)}
            </p>
            {!isTotal && (
              <span className="text-muted-foreground flex-shrink-0">
                {isRefund ? (
                  <TrendingUp className="h-4 w-4 text-green-500" />
                ) : (
                  <TrendingDown className="h-4 w-4 text-red-500" />
                )}
              </span>
            )}
          </div>
        </div>

        {/* Stats Row */}
        <div className="flex justify-between text-xs border-t border-border pt-3 mt-auto">
          <div>
            <span className="text-muted-foreground">Fatura Sayisi</span>
            <p className="font-medium text-foreground">{invoiceCount}</p>
          </div>
          {vatAmount !== undefined && (
            <div className="text-right">
              <span className="text-muted-foreground">KDV</span>
              <p className="font-medium text-foreground">{formatCurrency(vatAmount)}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// Skeleton loader for cards
function SummaryCardSkeleton() {
  return (
    <div className="flex flex-col rounded-lg overflow-hidden min-w-[220px] flex-1 bg-card border border-border shadow-sm">
      <div className="px-4 py-3 bg-muted animate-pulse">
        <div className="h-4 w-24 bg-muted-foreground/20 rounded" />
      </div>
      <div className="p-4">
        <div className="h-3 w-16 bg-muted rounded mb-2" />
        <div className="h-7 w-32 bg-muted rounded mb-4" />
        <div className="flex justify-between border-t border-border pt-3">
          <div className="h-4 w-12 bg-muted rounded" />
          <div className="h-4 w-16 bg-muted rounded" />
        </div>
      </div>
    </div>
  );
}

export function InvoiceSummaryCards({
  summary,
  selectedCategory,
  onCategorySelect,
  isLoading = false,
}: InvoiceSummaryCardsProps) {
  if (isLoading) {
    return (
      <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-thin scrollbar-thumb-border">
        {[...Array(5)].map((_, i) => (
          <SummaryCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (!summary) {
    return null;
  }

  // Build category data from summary
  const categoryData: CategorySummary[] = summary.invoicesByCategory || [];

  // Calculate total KDV for deduction categories
  const deductionCategories = ["KOMISYON", "KARGO", "CEZA", "REKLAM", "DIGER", "ULUSLARARASI"];
  const totalDeductionVat = categoryData.reduce((sum, c) => {
    if (deductionCategories.includes(c.category)) {
      return sum + (c.totalVatAmount || 0);
    }
    return sum;
  }, 0);

  // Build all cards data for unified sorting
  type CardData = {
    key: string;
    title: string;
    invoiceCount: number;
    totalAmount: number;
    vatAmount?: number;
    category: CategoryKey;
    isSelected: boolean;
    onClick: () => void;
  };

  const allCards: CardData[] = [];

  // Add Kesintiler card
  if (summary.totalDeductions !== 0) {
    allCards.push({
      key: "KESINTI",
      title: "Kesintiler",
      invoiceCount: categoryData.reduce((sum, c) => {
        if (deductionCategories.includes(c.category)) {
          return sum + c.invoiceCount;
        }
        return sum;
      }, 0),
      totalAmount: -Math.abs(summary.totalDeductions),
      vatAmount: totalDeductionVat,
      category: "KESINTI",
      isSelected: selectedCategory === null || selectedCategory === "KESINTI",
      onClick: () => onCategorySelect("KESINTI"),
    });
  }

  // Add Geri Yatan Ödeme card
  if (summary.totalRefunds !== 0) {
    allCards.push({
      key: "IADE",
      title: "Geri Yatan Odeme",
      invoiceCount: summary.invoicesByType?.filter((t) => !t.isDeduction).reduce((sum, t) => sum + t.invoiceCount, 0) || 0,
      totalAmount: summary.totalRefunds,
      category: "IADE",
      isSelected: selectedCategory === "IADE",
      onClick: () => onCategorySelect("IADE"),
    });
  }

  // Add category cards (excluding IADE since it's shown as Geri Yatan Ödeme)
  categoryData
    .filter((cat) => cat.category !== "IADE")
    .forEach((cat) => {
      allCards.push({
        key: cat.category,
        title: getCategoryDisplayName(cat.category),
        invoiceCount: cat.invoiceCount,
        totalAmount: cat.totalAmount,
        vatAmount: cat.totalVatAmount,
        category: cat.category as CategoryKey,
        isSelected: selectedCategory === cat.category,
        onClick: () => onCategorySelect(cat.category as CategoryKey),
      });
    });

  // Sort all cards by absolute amount (highest to lowest)
  allCards.sort((a, b) => Math.abs(b.totalAmount) - Math.abs(a.totalAmount));

  return (
    <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-thin scrollbar-thumb-border">
      {allCards.map((card) => (
        <SummaryCard
          key={card.key}
          title={card.title}
          invoiceCount={card.invoiceCount}
          totalAmount={card.totalAmount}
          vatAmount={card.vatAmount}
          category={card.category}
          isSelected={card.isSelected}
          onClick={card.onClick}
        />
      ))}
    </div>
  );
}
