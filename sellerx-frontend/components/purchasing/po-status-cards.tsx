"use client";

import type { PurchaseOrderStats, PurchaseOrderStatus } from "@/types/purchasing";
import { useCurrency } from "@/lib/contexts/currency-context";
import { cn } from "@/lib/utils";
import { FileText, PackageCheck, Truck, CheckCircle } from "lucide-react";

interface StatusCardProps {
  label: string;
  count: number;
  totalCost: number;
  totalUnits: number;
  icon: React.ElementType;
  colorClass: string;
  activeBorderClass: string;
  isActive: boolean;
  onClick: () => void;
}

function StatusCard({
  label,
  count,
  totalCost,
  totalUnits,
  icon: Icon,
  colorClass,
  activeBorderClass,
  isActive,
  onClick,
}: StatusCardProps) {
  const { formatCurrency } = useCurrency();

  return (
    <button
      onClick={onClick}
      className={cn(
        "bg-card rounded-lg border p-4 text-left transition-all hover:shadow-md",
        isActive ? activeBorderClass : "border-border"
      )}
    >
      <div className="flex items-center gap-3 mb-3">
        <div
          className={cn(
            "h-10 w-10 rounded-full flex items-center justify-center",
            colorClass
          )}
        >
          <Icon className="h-5 w-5" />
        </div>
        <div>
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="text-2xl font-bold text-foreground">{count}</p>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-2 text-xs">
        <div>
          <p className="text-muted-foreground">Toplam</p>
          <p className="font-medium text-foreground">{formatCurrency(totalCost)}</p>
        </div>
        <div>
          <p className="text-muted-foreground">Birim</p>
          <p className="font-medium text-foreground">{totalUnits.toLocaleString()}</p>
        </div>
      </div>
    </button>
  );
}

interface POStatusCardsProps {
  stats: PurchaseOrderStats | undefined;
  isLoading: boolean;
  activeStatus: PurchaseOrderStatus | null;
  onStatusClick: (status: PurchaseOrderStatus | null) => void;
}

export function POStatusCards({
  stats,
  isLoading,
  activeStatus,
  onStatusClick,
}: POStatusCardsProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[...Array(4)].map((_, i) => (
          <div
            key={i}
            className="bg-card rounded-lg border border-border p-4 animate-pulse"
          >
            <div className="h-16 bg-muted rounded" />
          </div>
        ))}
      </div>
    );
  }

  if (!stats) return null;

  const statusConfig = [
    {
      status: "DRAFT" as PurchaseOrderStatus,
      label: "Taslak",
      icon: FileText,
      colorClass: "bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400",
      activeBorderClass: "border-gray-400 ring-2 ring-gray-300/30 dark:border-gray-500 dark:ring-gray-500/20",
      stats: stats.draft,
    },
    {
      status: "ORDERED" as PurchaseOrderStatus,
      label: "Sipariş Verildi",
      icon: PackageCheck,
      colorClass: "bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400",
      activeBorderClass: "border-blue-500 ring-2 ring-blue-300/30 dark:border-blue-400 dark:ring-blue-400/20",
      stats: stats.ordered,
    },
    {
      status: "SHIPPED" as PurchaseOrderStatus,
      label: "Gönderildi",
      icon: Truck,
      colorClass: "bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400",
      activeBorderClass: "border-amber-500 ring-2 ring-amber-300/30 dark:border-amber-400 dark:ring-amber-400/20",
      stats: stats.shipped,
    },
    {
      status: "CLOSED" as PurchaseOrderStatus,
      label: "Kapatıldı",
      icon: CheckCircle,
      colorClass: "bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400",
      activeBorderClass: "border-green-500 ring-2 ring-green-300/30 dark:border-green-400 dark:ring-green-400/20",
      stats: stats.closed,
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {statusConfig.map((config) => (
        <StatusCard
          key={config.status}
          label={config.label}
          count={config.stats.count}
          totalCost={config.stats.totalCost}
          totalUnits={config.stats.totalUnits}
          icon={config.icon}
          colorClass={config.colorClass}
          activeBorderClass={config.activeBorderClass}
          isActive={activeStatus === config.status}
          onClick={() =>
            onStatusClick(activeStatus === config.status ? null : config.status)
          }
        />
      ))}
    </div>
  );
}
