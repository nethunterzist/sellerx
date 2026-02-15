"use client";

import { cn } from "@/lib/utils";
import {
  Receipt,
  Package,
  ExternalLink,
  ShoppingCart,
  Tag,
  Percent,
  Calculator,
  Undo2,
  ArrowLeftRight,
  Scale,
  BadgePercent,
  XCircle,
} from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetTitle,
} from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { useCurrency } from "@/lib/contexts/currency-context";
import { useProductCommissionBreakdown } from "@/hooks/queries/use-invoices";
import type { AggregatedProduct, TransactionTypeBreakdownItem } from "@/types/invoice";

interface ProductCommissionPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  product: AggregatedProduct | null;
  storeId: string;
  startDate: string;
  endDate: string;
}

// Transaction type colors and icons
const transactionTypeConfig: Record<string, { icon: React.ReactNode; color: string; bgColor: string }> = {
  Sale: {
    icon: <ShoppingCart className="h-4 w-4" />,
    color: "text-green-600",
    bgColor: "bg-green-100 dark:bg-green-900/30",
  },
  Coupon: {
    icon: <Tag className="h-4 w-4" />,
    color: "text-purple-600",
    bgColor: "bg-purple-100 dark:bg-purple-900/30",
  },
  Discount: {
    icon: <Percent className="h-4 w-4" />,
    color: "text-orange-600",
    bgColor: "bg-orange-100 dark:bg-orange-900/30",
  },
  Commission: {
    icon: <Receipt className="h-4 w-4" />,
    color: "text-blue-600",
    bgColor: "bg-blue-100 dark:bg-blue-900/30",
  },
  DiscountCancel: {
    icon: <XCircle className="h-4 w-4" />,
    color: "text-orange-500",
    bgColor: "bg-orange-50 dark:bg-orange-900/20",
  },
  CouponCancel: {
    icon: <XCircle className="h-4 w-4" />,
    color: "text-purple-500",
    bgColor: "bg-purple-50 dark:bg-purple-900/20",
  },
  ManualRefund: {
    icon: <Undo2 className="h-4 w-4" />,
    color: "text-red-500",
    bgColor: "bg-red-50 dark:bg-red-900/20",
  },
  ManualRefundCancel: {
    icon: <XCircle className="h-4 w-4" />,
    color: "text-red-400",
    bgColor: "bg-red-50 dark:bg-red-900/10",
  },
  TYDiscount: {
    icon: <BadgePercent className="h-4 w-4" />,
    color: "text-teal-600",
    bgColor: "bg-teal-50 dark:bg-teal-900/20",
  },
  TYDiscountCancel: {
    icon: <XCircle className="h-4 w-4" />,
    color: "text-teal-400",
    bgColor: "bg-teal-50 dark:bg-teal-900/10",
  },
  TYCoupon: {
    icon: <Tag className="h-4 w-4" />,
    color: "text-teal-600",
    bgColor: "bg-teal-50 dark:bg-teal-900/20",
  },
  TYCouponCancel: {
    icon: <XCircle className="h-4 w-4" />,
    color: "text-teal-400",
    bgColor: "bg-teal-50 dark:bg-teal-900/10",
  },
  ProvisionPositive: {
    icon: <Scale className="h-4 w-4" />,
    color: "text-sky-600",
    bgColor: "bg-sky-50 dark:bg-sky-900/20",
  },
  ProvisionNegative: {
    icon: <Scale className="h-4 w-4" />,
    color: "text-sky-500",
    bgColor: "bg-sky-50 dark:bg-sky-900/20",
  },
  CommissionPositive: {
    icon: <ArrowLeftRight className="h-4 w-4" />,
    color: "text-indigo-600",
    bgColor: "bg-indigo-50 dark:bg-indigo-900/20",
  },
  CommissionNegative: {
    icon: <ArrowLeftRight className="h-4 w-4" />,
    color: "text-indigo-500",
    bgColor: "bg-indigo-50 dark:bg-indigo-900/20",
  },
};

// Get display name for transaction type
function getTransactionTypeDisplay(type: string): string {
  const displayNames: Record<string, string> = {
    Sale: "Satış",
    Coupon: "Kupon",
    Discount: "İndirim",
    Commission: "Komisyon",
    DiscountCancel: "İndirim İptali",
    CouponCancel: "Kupon İptali",
    ManualRefund: "Kısmi İade",
    ManualRefundCancel: "Kısmi İade İptali",
    TYDiscount: "Trendyol İndirimi",
    TYDiscountCancel: "Trendyol İndirim İptali",
    TYCoupon: "Trendyol Kuponu",
    TYCouponCancel: "Trendyol Kupon İptali",
    ProvisionPositive: "Karşılık (+)",
    ProvisionNegative: "Karşılık (-)",
    CommissionPositive: "Komisyon Düzeltme (+)",
    CommissionNegative: "Komisyon Düzeltme (-)",
  };
  return displayNames[type] || type;
}

// Row component for consistent styling
function DetailRow({
  label,
  value,
  isNegative,
  isBold,
}: {
  label: string;
  value: string | React.ReactNode;
  isNegative?: boolean;
  isBold?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-2.5 px-4 border-b border-border">
      <span className={cn("text-sm text-muted-foreground", isBold && "font-semibold text-foreground")}>
        {label}
      </span>
      <span
        className={cn(
          "text-sm",
          isBold ? "font-semibold" : "font-medium",
          isNegative ? "text-red-600" : "text-foreground"
        )}
      >
        {value}
      </span>
    </div>
  );
}

// Check if transaction type is a deduction (İndirim, Kupon etc.) that reduces commission
function isDeductionType(transactionType: string): boolean {
  const deductionTypes = [
    "Discount", "Coupon", "İndirim", "Kupon",
    "DiscountCancel", "CouponCancel",
    "TYDiscount", "TYDiscountCancel", "TYCoupon", "TYCouponCancel",
    "CommissionNegative",
  ];
  return deductionTypes.includes(transactionType);
}

// Transaction type breakdown row
function TransactionTypeRow({
  item,
  formatCurrency,
}: {
  item: TransactionTypeBreakdownItem;
  formatCurrency: (n: number) => string;
}) {
  const config = transactionTypeConfig[item.transactionType] || transactionTypeConfig.Commission;
  const isDeduction = isDeductionType(item.transactionType);

  // For deductions (İndirim, Kupon): show as negative and green (reduces seller's payment to Trendyol)
  // For sales: show as positive and red (seller pays to Trendyol)
  const displayAmount = isDeduction ? -Math.abs(item.totalCommission) : item.totalCommission;
  const displayVat = isDeduction ? -Math.abs(item.totalVatAmount) : item.totalVatAmount;

  return (
    <div className={cn("flex items-center justify-between py-3 px-4 border-b border-border", config.bgColor)}>
      <div className="flex items-center gap-3">
        <div className={cn("p-2 rounded-lg", config.bgColor, config.color)}>
          {config.icon}
        </div>
        <div>
          <p className="text-sm font-medium text-foreground">
            {item.transactionTypeDisplay || getTransactionTypeDisplay(item.transactionType)}
          </p>
          <p className="text-xs text-muted-foreground">
            {item.itemCount} işlem
            {item.averageCommissionRate && ` · Ort. %${item.averageCommissionRate.toFixed(2)}`}
          </p>
        </div>
      </div>
      <div className="text-right">
        <p className={cn("font-semibold", isDeduction ? "text-green-600" : "text-red-600")}>
          {isDeduction ? "-" : "+"}{formatCurrency(Math.abs(displayAmount))}
        </p>
        {item.totalVatAmount > 0 && (
          <p className="text-xs text-muted-foreground">
            KDV: {isDeduction ? "-" : ""}{formatCurrency(Math.abs(displayVat))}
          </p>
        )}
      </div>
    </div>
  );
}

// Loading skeleton
function LoadingSkeleton() {
  return (
    <div className="divide-y divide-border">
      <div className="p-4">
        <div className="flex items-start gap-3">
          <Skeleton className="h-16 w-16 rounded" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-5 w-full" />
          </div>
        </div>
      </div>
      <div className="h-2 bg-muted" />
      {[...Array(3)].map((_, i) => (
        <div key={i} className="flex items-center justify-between p-4">
          <div className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-lg" />
            <div className="space-y-1">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-3 w-16" />
            </div>
          </div>
          <Skeleton className="h-5 w-24" />
        </div>
      ))}
    </div>
  );
}

export function ProductCommissionPanel({
  open,
  onOpenChange,
  product,
  storeId,
  startDate,
  endDate,
}: ProductCommissionPanelProps) {
  const { formatCurrency } = useCurrency();

  // Fetch breakdown data when panel opens
  const {
    data: breakdownData,
    isLoading,
    isError,
  } = useProductCommissionBreakdown(
    storeId,
    product?.barcode,
    startDate,
    endDate,
    open && !!product?.barcode
  );

  if (!product) return null;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[420px] sm:max-w-[420px] p-0 overflow-y-auto">
        <SheetTitle className="sr-only">Komisyon Detayları - {product.productName}</SheetTitle>

        {/* Header with product info */}
        <div className="sticky top-0 bg-blue-500 z-10">
          <div className="p-4">
            <div className="flex items-center gap-3 text-white">
              <Receipt className="h-5 w-5" />
              <div>
                <p className="text-sm font-medium opacity-90">Komisyon Detayı</p>
                <p className="text-lg font-bold">İşlem Tipi Dağılımı</p>
              </div>
            </div>
          </div>
        </div>

        {isLoading ? (
          <LoadingSkeleton />
        ) : isError ? (
          <div className="p-8 text-center">
            <p className="text-sm text-muted-foreground">Veri yüklenirken hata oluştu.</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-4"
              onClick={() => onOpenChange(false)}
            >
              Kapat
            </Button>
          </div>
        ) : (
          <div className="divide-y divide-border">
            {/* Product Info */}
            <div className="p-4">
              <div className="flex items-start gap-3">
                {(breakdownData?.productImageUrl || product.productImageUrl) ? (
                  <a
                    href={breakdownData?.productUrl || product.productUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex-shrink-0 group relative"
                  >
                    <img
                      src={breakdownData?.productImageUrl || product.productImageUrl}
                      alt={product.productName || "Ürün"}
                      className="h-16 w-16 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = "https://via.placeholder.com/64?text=X";
                      }}
                    />
                    <div className="absolute -top-1 -right-1 bg-[#F27A1A] rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                      <ExternalLink className="h-2.5 w-2.5 text-white" />
                    </div>
                  </a>
                ) : (
                  <div className="h-16 w-16 rounded flex items-center justify-center text-sm font-bold text-white flex-shrink-0 bg-[#F27A1A]">
                    T
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-muted-foreground font-mono">
                    {product.barcode}
                  </p>
                  <p className="text-sm font-medium text-foreground mt-0.5 line-clamp-2">
                    {breakdownData?.productName || product.productName || "Bilinmeyen Ürün"}
                  </p>
                </div>
              </div>
            </div>

            {/* Divider */}
            <div className="h-2 bg-muted" />

            {/* Summary Stats - Net Komisyon Hesaplama */}
            <div className="py-3 px-4 border-b border-border bg-blue-50 dark:bg-blue-900/20">
              <div className="flex items-center gap-2 mb-2">
                <Calculator className="h-4 w-4 text-blue-600" />
                <span className="text-sm font-semibold text-foreground">
                  Net Komisyon Hesabı
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                Net Komisyon = Satış - İndirim - Kupon
              </p>
            </div>

            {/* Commission Breakdown Values */}
            {breakdownData?.saleCommission !== undefined && (
              <DetailRow
                label="Satış Komisyonu"
                value={`+${formatCurrency(breakdownData.saleCommission)}`}
                isNegative={true}
              />
            )}
            {breakdownData?.discountCommission !== undefined && breakdownData.discountCommission > 0 && (
              <DetailRow
                label="İndirim Komisyonu"
                value={
                  <span className="text-green-600">
                    -{formatCurrency(breakdownData.discountCommission)}
                  </span>
                }
              />
            )}
            {breakdownData?.couponCommission !== undefined && breakdownData.couponCommission > 0 && (
              <DetailRow
                label="Kupon Komisyonu"
                value={
                  <span className="text-green-600">
                    -{formatCurrency(breakdownData.couponCommission)}
                  </span>
                }
              />
            )}

            {/* Divider before net total */}
            <div className="h-1 bg-border" />

            <DetailRow
              label="Net Komisyon"
              value={formatCurrency(breakdownData?.totalCommission || product.totalAmount)}
              isBold
              isNegative
            />

            <DetailRow
              label="Net KDV (%20)"
              value={formatCurrency(breakdownData?.totalVatAmount || product.totalVatAmount || 0)}
            />

            <DetailRow
              label="İşlem Sayısı"
              value={`${breakdownData?.totalItemCount || product.totalQuantity} adet`}
            />

            <DetailRow
              label="Sipariş Sayısı"
              value={`${breakdownData?.orderCount || product.invoiceCount} sipariş`}
            />

            {/* Divider */}
            <div className="h-2 bg-muted" />

            {/* Transaction Type Breakdown */}
            <div className="py-3 px-4 border-b border-border bg-muted/30">
              <div className="flex items-center gap-2">
                <Receipt className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm font-semibold text-foreground">
                  İşlem Tipi Dağılımı
                </span>
                {breakdownData?.breakdown && (
                  <Badge variant="secondary" className="ml-auto text-xs">
                    {breakdownData.breakdown.length} tip
                  </Badge>
                )}
              </div>
            </div>

            {breakdownData?.breakdown && breakdownData.breakdown.length > 0 ? (
              breakdownData.breakdown.map((item, index) => (
                <TransactionTypeRow
                  key={item.transactionType || index}
                  item={item}
                  formatCurrency={formatCurrency}
                />
              ))
            ) : (
              <div className="p-6 text-center">
                <Package className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">
                  İşlem tipi dağılımı bulunamadı
                </p>
              </div>
            )}

            {/* View on Trendyol Button */}
            {(breakdownData?.productUrl || product.productUrl) && (
              <div className="p-4 border-t border-border">
                <Button
                  variant="outline"
                  className="w-full gap-2"
                  onClick={() => window.open(breakdownData?.productUrl || product.productUrl, "_blank")}
                >
                  <ExternalLink className="h-4 w-4" />
                  Trendyol'da Görüntüle
                </Button>
              </div>
            )}
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
