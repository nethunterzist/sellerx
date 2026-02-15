'use client';

import { BarChart3, Percent, Target, TrendingDown, TrendingUp } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { MetricCard } from '@/components/ui/metric-card';
import type { CalculatorResult } from '@/types/calculator';
import { formatCurrency, formatPercentage } from '@/lib/utils/calculator';

interface CalculatorSummaryProps {
  results: CalculatorResult;
  quantity: number;
  targetMargin?: number;
}

export function CalculatorSummary({
  results,
  quantity,
  targetMargin = 0,
}: CalculatorSummaryProps) {
  const isProfit = results.netProfit >= 0;
  const hasSuggestedPrice = results.suggestedSalePrice > 0 && targetMargin > 0;

  return (
    <div className="space-y-3">
      {/* Hero Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <MetricCard
          title="Net Kâr"
          icon={isProfit ? TrendingUp : TrendingDown}
          headerColor={isProfit ? 'bg-green-500' : 'bg-red-500'}
          metricValue={formatCurrency(results.netProfit)}
          metricColor={
            isProfit
              ? 'text-green-600 dark:text-green-400'
              : 'text-red-600 dark:text-red-400'
          }
          footerStats={[
            ...(quantity > 1
              ? [{ label: 'Birim kâr', value: formatCurrency(results.netProfit / quantity) }]
              : []),
            { label: 'Toplam yatırım', value: formatCurrency(results.totalInvestment) },
          ]}
        />

        <MetricCard
          title="Kâr Marjı"
          icon={Percent}
          headerColor="bg-blue-500"
          metricValue={formatPercentage(results.margin)}
          metricColor={
            results.margin >= 0
              ? 'text-blue-600 dark:text-blue-400'
              : 'text-red-600 dark:text-red-400'
          }
          footerStats={[
            { label: 'Hesaplama', value: 'Net Kâr / Satış' },
          ]}
        />

        <MetricCard
          title="ROI"
          icon={BarChart3}
          headerColor="bg-purple-500"
          metricValue={formatPercentage(results.roi)}
          metricColor={
            results.roi >= 0
              ? 'text-purple-600 dark:text-purple-400'
              : 'text-red-600 dark:text-red-400'
          }
          footerStats={[
            { label: 'Hesaplama', value: 'Net Kâr / Yatırım' },
          ]}
        />
      </div>

      {/* Önerilen Fiyat Banner */}
      {hasSuggestedPrice && (
        <div className="flex items-center gap-2 px-3 py-2 rounded-md bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800">
          <Target className="h-4 w-4 text-amber-600 dark:text-amber-400 shrink-0" />
          <span className="text-sm text-muted-foreground">Önerilen Fiyat:</span>
          <span className="text-sm font-bold text-amber-700 dark:text-amber-400">
            {formatCurrency(results.suggestedSalePrice)}
          </span>
          <Badge variant="outline" className="ml-auto text-xs">
            %{(targetMargin * 100).toFixed(0)} marj
          </Badge>
        </div>
      )}
    </div>
  );
}
