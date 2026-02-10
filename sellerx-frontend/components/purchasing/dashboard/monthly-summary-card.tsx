"use client";

import { useMemo } from "react";
import { useCurrency } from "@/lib/contexts/currency-context";
import { usePurchaseSummary } from "@/hooks/queries/use-purchasing";
import { Package, TrendingUp, Calculator, Archive } from "lucide-react";
import { format } from "date-fns";
import { tr } from "date-fns/locale";

interface MonthlySummaryCardProps {
  storeId: string | undefined;
  startDate?: string;
  endDate?: string;
}

export function MonthlySummaryCard({ storeId, startDate: propStartDate, endDate: propEndDate }: MonthlySummaryCardProps) {
  const { formatCurrency } = useCurrency();

  // If no date range provided, use current month
  const { startDate, endDate, displayTitle, displayBadge } = useMemo(() => {
    if (propStartDate && propEndDate) {
      // Format the date range for display
      const fromDate = new Date(propStartDate);
      const toDate = new Date(propEndDate);
      const fromStr = format(fromDate, "d MMM", { locale: tr });
      const toStr = format(toDate, "d MMM yyyy", { locale: tr });
      return {
        startDate: propStartDate,
        endDate: propEndDate,
        displayTitle: `${fromStr} - ${toStr} Ozeti`,
        displayBadge: "Secilen Tarih",
      };
    }
    // Default: current month
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    const monthName = now.toLocaleDateString("tr-TR", { month: "long", year: "numeric" });
    return {
      startDate: startOfMonth.toISOString().split("T")[0],
      endDate: endOfMonth.toISOString().split("T")[0],
      displayTitle: `${monthName} Ozeti`,
      displayBadge: "Bu Ay",
    };
  }, [propStartDate, propEndDate]);

  const { data: summary, isLoading } = usePurchaseSummary(storeId, startDate, endDate);

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border border-border p-6 animate-pulse">
        <div className="h-5 w-32 bg-muted rounded mb-4" />
        <div className="space-y-4">
          <div className="h-8 w-24 bg-muted rounded" />
          <div className="h-4 w-40 bg-muted rounded" />
        </div>
      </div>
    );
  }

  const stats = [
    {
      label: "Toplam Alim",
      value: formatCurrency(summary?.totalPurchaseAmount || 0),
      icon: Package,
      color: "text-blue-600 dark:text-blue-400",
      bgColor: "bg-blue-50 dark:bg-blue-900/20",
    },
    {
      label: "Urun Sayisi",
      value: `${(summary?.totalUnits || 0).toLocaleString()} adet`,
      icon: Archive,
      color: "text-purple-600 dark:text-purple-400",
      bgColor: "bg-purple-50 dark:bg-purple-900/20",
    },
    {
      label: "Ort. Maliyet",
      value: formatCurrency(summary?.averageCostPerUnit || 0) + "/adet",
      icon: Calculator,
      color: "text-amber-600 dark:text-amber-400",
      bgColor: "bg-amber-50 dark:bg-amber-900/20",
    },
    {
      label: "Siparis Sayisi",
      value: (summary?.totalOrders || 0).toString(),
      icon: TrendingUp,
      color: "text-green-600 dark:text-green-400",
      bgColor: "bg-green-50 dark:bg-green-900/20",
    },
  ];

  return (
    <div className="bg-card rounded-xl border border-border p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="font-semibold text-foreground">{displayTitle}</h3>
        <span className="text-xs text-muted-foreground bg-muted px-2 py-1 rounded-full">
          {displayBadge}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-4">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div key={index} className="space-y-2">
              <div className="flex items-center gap-2">
                <div className={`p-1.5 rounded-lg ${stat.bgColor}`}>
                  <Icon className={`h-3.5 w-3.5 ${stat.color}`} />
                </div>
                <span className="text-xs text-muted-foreground">{stat.label}</span>
              </div>
              <p className="text-lg font-semibold text-foreground pl-8">{stat.value}</p>
            </div>
          );
        })}
      </div>
    </div>
  );
}
