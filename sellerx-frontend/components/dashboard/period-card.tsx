"use client";

import { cn } from "@/lib/utils";
import { ChevronDown } from "lucide-react";

interface PeriodCardProps {
  title: string;
  dateRange: string;
  sales: number;
  ordersUnits: string;
  refunds: number;
  advCost: number;
  estPayout: number;
  grossProfit: number;
  netProfit: number;
  isToday?: boolean;
  percentageChange?: number;
}

function formatCurrency(value: number): string {
  const absValue = Math.abs(value);
  const formatted = new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(absValue);
  return value < 0 ? `-${formatted}` : formatted;
}

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
  advCost,
  estPayout,
  grossProfit,
  netProfit,
  isToday = false,
  percentageChange,
}: PeriodCardProps) {
  return (
    <div
      className={cn(
        "flex flex-col rounded-lg p-4 min-w-[180px] flex-1",
        isToday
          ? "bg-[#1D70F1] text-white"
          : "bg-white border border-[#DDDDDD]"
      )}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-2">
        <div>
          <h3 className={cn(
            "text-sm font-semibold",
            isToday ? "text-white" : "text-gray-900"
          )}>
            {title}
          </h3>
          <p className={cn(
            "text-xs",
            isToday ? "text-white/70" : "text-gray-500"
          )}>
            {dateRange}
          </p>
        </div>
        {percentageChange !== undefined && (
          <span
            className={cn(
              "text-xs font-medium px-1.5 py-0.5 rounded",
              percentageChange >= 0
                ? "bg-green-100 text-green-700"
                : "bg-red-100 text-red-700"
            )}
          >
            {formatPercentage(percentageChange)}
          </span>
        )}
      </div>

      {/* Sales */}
      <div className="mb-3">
        <p className={cn(
          "text-xs mb-0.5",
          isToday ? "text-white/70" : "text-gray-500"
        )}>
          Satışlar
        </p>
        <p className={cn(
          "text-2xl font-bold",
          isToday ? "text-white" : "text-gray-900"
        )}>
          {formatCurrency(sales)} TL
        </p>
      </div>

      {/* Orders & Refunds */}
      <div className="flex justify-between text-xs mb-2">
        <div>
          <span className={isToday ? "text-white/70" : "text-gray-500"}>
            Sipariş / Adet
          </span>
          <p className={cn(
            "font-medium",
            isToday ? "text-white" : "text-gray-900"
          )}>
            {ordersUnits}
          </p>
        </div>
        <div className="text-right">
          <span className={isToday ? "text-white/70" : "text-gray-500"}>
            İadeler
          </span>
          <p className={cn(
            "font-medium",
            isToday ? "text-white" : refunds > 0 ? "text-[#1D70F1]" : "text-gray-900"
          )}>
            {refunds}
          </p>
        </div>
      </div>

      {/* Adv Cost & Est Payout */}
      <div className="flex justify-between text-xs mb-2">
        <div>
          <span className={isToday ? "text-white/70" : "text-gray-500"}>
            Reklam Gideri
          </span>
          <p className={cn(
            "font-medium",
            isToday ? "text-white" : advCost < 0 ? "text-red-600" : "text-gray-900"
          )}>
            {formatCurrency(advCost)} TL
          </p>
        </div>
        <div className="text-right">
          <span className={isToday ? "text-white/70" : "text-gray-500"}>
            Tah. Ödeme
          </span>
          <p className={cn(
            "font-medium",
            isToday ? "text-white" : "text-gray-900"
          )}>
            {formatCurrency(estPayout)} TL
          </p>
        </div>
      </div>

      {/* Gross & Net Profit */}
      <div className="flex justify-between text-xs border-t pt-2 mt-auto"
        style={{ borderColor: isToday ? "rgba(255,255,255,0.2)" : "#DDDDDD" }}
      >
        <div>
          <span className={isToday ? "text-white/70" : "text-gray-500"}>
            Brüt Kâr
          </span>
          <p className={cn(
            "font-medium",
            isToday ? "text-white" : grossProfit >= 0 ? "text-green-600" : "text-red-600"
          )}>
            {formatCurrency(grossProfit)} TL
          </p>
        </div>
        <div className="text-right">
          <span className={isToday ? "text-white/70" : "text-gray-500"}>
            Net Kâr
          </span>
          <p className={cn(
            "font-medium",
            isToday ? "text-white" : netProfit >= 0 ? "text-green-600" : "text-red-600"
          )}>
            {formatCurrency(netProfit)} TL
          </p>
        </div>
      </div>

      {/* More Link */}
      <button
        className={cn(
          "mt-3 text-xs font-medium hover:underline self-start",
          isToday ? "text-white/80 hover:text-white" : "text-[#1D70F1]"
        )}
      >
        Detay
      </button>
    </div>
  );
}
