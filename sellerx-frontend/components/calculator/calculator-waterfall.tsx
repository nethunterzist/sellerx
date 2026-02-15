'use client';

import { useMemo } from 'react';

import { Separator } from '@/components/ui/separator';
import type { CostLineItem } from '@/types/calculator';
import { formatCurrency } from '@/lib/utils/calculator';

interface CalculatorWaterfallProps {
  salePriceExVat: number;
  costs: CostLineItem[];
  netProfit: number;
}

export function CalculatorWaterfall({
  salePriceExVat,
  costs,
  netProfit,
}: CalculatorWaterfallProps) {
  const items = useMemo(() => {
    const activeCosts = costs.filter((c) => c.amountExVat > 0);
    const maxValue = salePriceExVat > 0 ? salePriceExVat : 1;

    return {
      sale: {
        label: 'Satış (KDV Hariç)',
        value: salePriceExVat,
        width: 100,
      },
      costs: activeCosts.map((c) => ({
        label: c.name,
        value: c.amountExVat,
        width: Math.min(100, Math.max(2, (c.amountExVat / maxValue) * 100)),
      })),
      profit: {
        label: 'Net Kâr',
        value: netProfit,
        width: Math.min(100, Math.max(2, (Math.abs(netProfit) / maxValue) * 100)),
      },
    };
  }, [salePriceExVat, costs, netProfit]);

  if (salePriceExVat <= 0) {
    return (
      <div className="flex items-center justify-center h-[200px] text-sm text-muted-foreground">
        Satış fiyatı giriniz
      </div>
    );
  }

  const isProfit = netProfit >= 0;

  return (
    <div className="space-y-2 overflow-hidden">
      {/* Sale row */}
      <div className="flex items-center gap-3">
        <span className="text-xs text-muted-foreground w-28 shrink-0 truncate text-right">
          {items.sale.label}
        </span>
        <div className="flex-1 h-7 relative">
          <div
            className="h-full rounded-md bg-green-500/80 dark:bg-green-500/60 flex items-center px-2.5 transition-all duration-500"
            style={{ width: `${items.sale.width}%` }}
          >
            <span className="text-xs font-medium text-white truncate">
              {formatCurrency(items.sale.value)}
            </span>
          </div>
        </div>
      </div>

      {/* Cost rows */}
      {items.costs.map((cost, i) => {
        const showInside = cost.width > 30;
        return (
          <div key={i} className="flex items-center gap-3">
            <span className="text-xs text-muted-foreground w-28 shrink-0 truncate text-right">
              {cost.label}
            </span>
            <div className="flex-1 h-6 flex items-center">
              <div
                className="h-full rounded-md bg-red-500/70 dark:bg-red-500/50 flex items-center px-2 transition-all duration-500 shrink-0"
                style={{ width: `${cost.width}%` }}
              >
                {showInside && (
                  <span className="text-[11px] font-medium text-white whitespace-nowrap">
                    {formatCurrency(cost.value)}
                  </span>
                )}
              </div>
              {!showInside && (
                <span className="text-[11px] text-muted-foreground whitespace-nowrap ml-2">
                  {formatCurrency(cost.value)}
                </span>
              )}
            </div>
          </div>
        );
      })}

      <Separator className="my-1" />

      {/* Net profit row */}
      <div className="flex items-center gap-3">
        <span className="text-xs font-semibold text-foreground w-28 shrink-0 truncate text-right">
          {items.profit.label}
        </span>
        <div className="flex-1 h-7 flex items-center">
          <div
            className={`h-full rounded-md flex items-center px-2.5 transition-all duration-500 shrink-0 ${
              isProfit
                ? 'bg-green-500/80 dark:bg-green-500/60'
                : 'bg-red-500/80 dark:bg-red-500/60'
            }`}
            style={{ width: `${items.profit.width}%` }}
          >
            {items.profit.width > 30 && (
              <span className="text-xs font-bold text-white whitespace-nowrap">
                {formatCurrency(items.profit.value)}
              </span>
            )}
          </div>
          {items.profit.width <= 30 && (
            <span className={`text-xs font-bold whitespace-nowrap ml-2 ${
              isProfit ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
            }`}>
              {formatCurrency(items.profit.value)}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
