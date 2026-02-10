"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  MinusCircle,
  ChevronRight,
  ChevronDown,
  Building2,
  FileText,
  Store,
} from "lucide-react";

interface ExpenseItem {
  label: string;
  value: number;
}

interface ExpenseGroup {
  id: string;
  title: string;
  icon: React.ReactNode;
  items: ExpenseItem[];
  total: number;
}

export interface CollapsibleExpenseBreakdownProps {
  isLoading?: boolean;
  formatCurrency: (value: number) => string;

  // Direct expense items (always visible)
  productCost: number;
  commission: number;
  stoppage: number;
  shippingCost: number;
  returnCost: number;

  // Platform fees (collapsible group)
  platformFees: {
    internationalServiceFee?: number;
    overseasOperationFee?: number;
    terminDelayFee?: number;
    platformServiceFee?: number;
    invoiceCreditFee?: number;
    unsuppliedFee?: number;
    azOverseasOperationFee?: number;
    azPlatformServiceFee?: number;
    packagingServiceFee?: number;
    warehouseServiceFee?: number;
    callCenterFee?: number;
    photoShootingFee?: number;
    integrationFee?: number;
    storageServiceFee?: number;
    otherPlatformFees?: number;
  };

  // Invoiced deductions (collapsible group)
  invoicedDeductions: {
    advertisingFees?: number;
    penaltyFees?: number;
    internationalFees?: number;
    otherFees?: number;
    refunds?: number; // Positive value, will be shown as negative (reduces expenses)
  };

  // Store expenses by category (collapsible group)
  expensesByCategory?: Record<string, number>;
  totalExpenseAmount: number;
}

// Platform fee label mapping (Turkish)
const platformFeeLabels: Record<string, string> = {
  internationalServiceFee: "Uluslararasi Hizmet Bedeli",
  overseasOperationFee: "Yurt Disi Operasyon Bedeli",
  terminDelayFee: "Termin Gecikme Bedeli",
  platformServiceFee: "Platform Hizmet Bedeli",
  invoiceCreditFee: "Fatura Kontor Satis Bedeli",
  unsuppliedFee: "Tedarik Edememe",
  azOverseasOperationFee: "AZ-Yurtdisi Operasyon Bedeli",
  azPlatformServiceFee: "AZ-Platform Hizmet Bedeli",
  packagingServiceFee: "Paketleme Hizmet Bedeli",
  warehouseServiceFee: "Depo Hizmet Bedeli",
  callCenterFee: "Cagri Merkezi Bedeli",
  photoShootingFee: "Fotograf Cekim Bedeli",
  integrationFee: "Entegrasyon Bedeli",
  storageServiceFee: "Depolama Hizmet Bedeli",
  otherPlatformFees: "Diger Platform Ucretleri",
};

// Invoiced deduction label mapping (Turkish)
const invoicedDeductionLabels: Record<string, string> = {
  advertisingFees: "Reklam",
  penaltyFees: "Ceza",
  internationalFees: "Uluslararasi",
  otherFees: "Diger",
  refunds: "Iade (Dusulecek)",
};

function CollapsibleExpenseGroup({
  group,
  formatCurrency,
}: {
  group: ExpenseGroup;
  formatCurrency: (value: number) => string;
}) {
  const [isOpen, setIsOpen] = useState(false);

  // Don't render if total is 0
  if (group.total === 0) return null;

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger className="w-full">
        <div className="flex justify-between items-center py-2 px-1 hover:bg-muted/50 rounded-md transition-colors cursor-pointer">
          <div className="flex items-center gap-2">
            {isOpen ? (
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            ) : (
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            )}
            <span className="flex items-center gap-2 text-muted-foreground text-sm">
              {group.icon}
              {group.title}
            </span>
          </div>
          <span className="font-medium text-red-600 dark:text-red-400">
            -{formatCurrency(group.total)}
          </span>
        </div>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="ml-6 pl-4 border-l-2 border-muted space-y-1 py-1">
          {group.items.map((item, index) => (
            <div
              key={index}
              className="flex justify-between items-center py-1 text-sm"
            >
              <span className="text-muted-foreground">{item.label}</span>
              <span
                className={cn(
                  "font-medium",
                  item.value >= 0
                    ? "text-red-500 dark:text-red-400"
                    : "text-green-500 dark:text-green-400"
                )}
              >
                {item.value >= 0 ? "-" : "+"}
                {formatCurrency(Math.abs(item.value))}
              </span>
            </div>
          ))}
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}

function ExpenseRow({
  label,
  value,
  formatCurrency,
}: {
  label: string;
  value: number;
  formatCurrency: (value: number) => string;
}) {
  // Don't render if value is 0
  if (value === 0) return null;

  return (
    <div className="flex justify-between items-center py-1.5 border-b border-border last:border-0">
      <span className="text-muted-foreground text-sm">{label}</span>
      <span className="font-medium text-red-600 dark:text-red-400">
        -{formatCurrency(value)}
      </span>
    </div>
  );
}

export function CollapsibleExpenseBreakdown({
  isLoading,
  formatCurrency,
  productCost,
  commission,
  stoppage,
  shippingCost,
  returnCost,
  platformFees,
  invoicedDeductions,
  expensesByCategory,
  totalExpenseAmount,
}: CollapsibleExpenseBreakdownProps) {
  // Calculate platform fees total and items
  const platformFeeItems: ExpenseItem[] = Object.entries(platformFees)
    .filter(([, value]) => value && value > 0)
    .map(([key, value]) => ({
      label: platformFeeLabels[key] || key,
      value: value!,
    }));
  const platformFeesTotal = platformFeeItems.reduce(
    (sum, item) => sum + item.value,
    0
  );

  // Calculate invoiced deductions total and items
  const invoicedItems: ExpenseItem[] = [];
  let invoicedTotal = 0;

  if (invoicedDeductions.advertisingFees && invoicedDeductions.advertisingFees > 0) {
    invoicedItems.push({
      label: invoicedDeductionLabels.advertisingFees,
      value: invoicedDeductions.advertisingFees,
    });
    invoicedTotal += invoicedDeductions.advertisingFees;
  }
  if (invoicedDeductions.penaltyFees && invoicedDeductions.penaltyFees > 0) {
    invoicedItems.push({
      label: invoicedDeductionLabels.penaltyFees,
      value: invoicedDeductions.penaltyFees,
    });
    invoicedTotal += invoicedDeductions.penaltyFees;
  }
  if (invoicedDeductions.internationalFees && invoicedDeductions.internationalFees > 0) {
    invoicedItems.push({
      label: invoicedDeductionLabels.internationalFees,
      value: invoicedDeductions.internationalFees,
    });
    invoicedTotal += invoicedDeductions.internationalFees;
  }
  if (invoicedDeductions.otherFees && invoicedDeductions.otherFees > 0) {
    invoicedItems.push({
      label: invoicedDeductionLabels.otherFees,
      value: invoicedDeductions.otherFees,
    });
    invoicedTotal += invoicedDeductions.otherFees;
  }
  if (invoicedDeductions.refunds && invoicedDeductions.refunds > 0) {
    invoicedItems.push({
      label: invoicedDeductionLabels.refunds,
      value: -invoicedDeductions.refunds, // Negative because it reduces expenses
    });
    invoicedTotal -= invoicedDeductions.refunds;
  }

  // Calculate store expenses items
  const storeExpenseItems: ExpenseItem[] = expensesByCategory
    ? Object.entries(expensesByCategory)
        .filter(([, value]) => value > 0)
        .map(([category, value]) => ({
          label: category,
          value,
        }))
    : [];

  // Build expense groups
  const expenseGroups: ExpenseGroup[] = [
    {
      id: "platform",
      title: "Platform Ucretleri",
      icon: <Building2 className="h-4 w-4" />,
      items: platformFeeItems,
      total: platformFeesTotal,
    },
    {
      id: "invoiced",
      title: "Kesilen Faturalar",
      icon: <FileText className="h-4 w-4" />,
      items: invoicedItems,
      total: Math.max(invoicedTotal, 0),
    },
    {
      id: "store",
      title: "Magaza Giderleri",
      icon: <Store className="h-4 w-4" />,
      items: storeExpenseItems,
      total: totalExpenseAmount,
    },
  ];

  // Calculate grand total
  const grandTotal =
    productCost +
    commission +
    stoppage +
    platformFeesTotal +
    shippingCost +
    returnCost +
    Math.max(invoicedTotal, 0) +
    totalExpenseAmount;

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">
            <Skeleton className="h-5 w-32" />
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="flex justify-between">
              <Skeleton className="h-4 w-28" />
              <Skeleton className="h-4 w-20" />
            </div>
          ))}
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-lg flex items-center gap-2">
          <MinusCircle className="h-5 w-5 text-red-600 dark:text-red-400" />
          Giderler
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-1">
        {/* Direct expense items */}
        <ExpenseRow
          label="Urun Maliyeti"
          value={productCost}
          formatCurrency={formatCurrency}
        />
        <ExpenseRow
          label="Tahmini Komisyon"
          value={commission}
          formatCurrency={formatCurrency}
        />
        <ExpenseRow
          label="Stopaj"
          value={stoppage}
          formatCurrency={formatCurrency}
        />

        {/* Collapsible expense groups */}
        {expenseGroups.map((group) => (
          <CollapsibleExpenseGroup
            key={group.id}
            group={group}
            formatCurrency={formatCurrency}
          />
        ))}

        {/* More direct expense items */}
        <ExpenseRow
          label="Kargo Maliyeti"
          value={shippingCost}
          formatCurrency={formatCurrency}
        />
        <ExpenseRow
          label="Iade Maliyeti"
          value={returnCost}
          formatCurrency={formatCurrency}
        />

        {/* Total */}
        <div className="flex justify-between items-center pt-3 mt-2 border-t-2 border-red-200 dark:border-red-800">
          <span className="font-semibold">Toplam</span>
          <span className="font-bold text-lg text-red-600 dark:text-red-400">
            -{formatCurrency(grandTotal)}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
