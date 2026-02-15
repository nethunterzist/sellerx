"use client";

import { useState, useMemo } from "react";
import { motion } from "motion/react";
import { FadeIn } from "@/components/motion/fade-in";
import { NumberTicker } from "@/components/motion/number-ticker";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useInvoiceSummary } from "@/hooks/queries/use-invoices";
import { useCurrency } from "@/lib/contexts/currency-context";
import { Button } from "@/components/ui/button";
import {
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  ChevronUp,
  Calculator,
  ShoppingBag,
  FileText,
  Package,
  Receipt,
  Scale,
  TrendingUp,
  TrendingDown,
  AlertTriangle,
  Info,
} from "lucide-react";
import { useStoreExpenses } from "@/hooks/queries/use-expenses";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

// ─── Skeleton ───────────────────────────────────────────────
function KdvPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Skeleton className="h-9 w-9 rounded-md" />
        <Skeleton className="h-6 w-40" />
        <Skeleton className="h-9 w-9 rounded-md" />
      </div>
      <div className="flex gap-4 overflow-x-auto pb-2">
        {[0, 1, 2].map((i) => (
          <div key={i} className="flex flex-col rounded-lg overflow-hidden min-w-[260px] flex-1 bg-card border border-border shadow-sm">
            <div className="px-4 py-3 bg-muted animate-pulse">
              <Skeleton className="h-4 w-24 bg-muted-foreground/20" />
              <Skeleton className="h-3 w-32 bg-muted-foreground/20 mt-1" />
            </div>
            <div className="p-4">
              <Skeleton className="h-3 w-16 mb-2" />
              <Skeleton className="h-8 w-36 mb-4" />
              <div className="flex justify-between border-t border-border pt-3">
                <Skeleton className="h-4 w-16" />
                <Skeleton className="h-4 w-20" />
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="rounded-lg border bg-card overflow-hidden">
            <div className="px-4 py-3 bg-muted animate-pulse">
              <Skeleton className="h-4 w-32 bg-muted-foreground/20" />
            </div>
            <div className="p-4 space-y-3">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-4 w-1/2" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Helpers ────────────────────────────────────────────────
function getMonthRange(date: Date): { startDate: string; endDate: string } {
  const year = date.getFullYear();
  const month = date.getMonth();
  const start = new Date(year, month, 1);
  const end = new Date(year, month + 1, 0);
  return {
    startDate: start.toISOString().split("T")[0],
    endDate: end.toISOString().split("T")[0],
  };
}

function formatMonthLabel(date: Date): string {
  return date.toLocaleDateString("tr-TR", { year: "numeric", month: "long" });
}

// Known Turkish VAT rates
const KNOWN_VAT_RATES = [0, 1, 8, 10, 18, 20];

function calculateVatRate(item: { totalAmount: number; totalVatAmount: number }): number {
  if (item.totalAmount === 0 || item.totalVatAmount === 0) return 0;
  const baseAmount = item.totalAmount - item.totalVatAmount;
  if (baseAmount === 0) return 0;
  const rate = (item.totalVatAmount / baseAmount) * 100;
  let closest = KNOWN_VAT_RATES[0];
  let minDiff = Math.abs(rate - closest);
  for (const knownRate of KNOWN_VAT_RATES) {
    const diff = Math.abs(rate - knownRate);
    if (diff < minDiff) {
      minDiff = diff;
      closest = knownRate;
    }
  }
  return closest;
}

// ─── Summary Card (period-card stili) ───────────────────────
interface SummaryCardProps {
  title: string;
  subtitle: string;
  amount: number;
  icon: React.ReactNode;
  headerColor: string;
  amountColor: string;
  children?: React.ReactNode;
  formatCurrency: (value: number) => string;
  negative?: boolean;
}

function SummaryCard({ title, subtitle, amount, icon, headerColor, amountColor, children, formatCurrency, negative }: SummaryCardProps) {
  return (
    <div className="flex flex-col rounded-lg overflow-hidden min-w-[260px] flex-1 bg-card border border-border shadow-sm hover:shadow-md transition-all">
      <div className={cn("px-4 py-3", headerColor)}>
        <div className="flex items-center gap-2">
          <span className="text-white/80">{icon}</span>
          <h3 className="text-sm font-semibold text-white">{title}</h3>
        </div>
        <p className="text-xs text-white/70 mt-0.5">{subtitle}</p>
      </div>
      <div className="p-4 flex flex-col flex-1">
        <div className="mb-3">
          <span className="text-xs text-muted-foreground">Toplam KDV</span>
          <p className={cn("text-2xl font-bold", amountColor)}>
            {negative && amount > 0 && "\u2212"}{formatCurrency(Math.abs(amount))}
          </p>
        </div>
        {children}
      </div>
    </div>
  );
}

// ─── Detail Card (invoice-summary-card stili) ───────────────
interface DetailCardProps {
  title: string;
  icon: React.ReactNode;
  headerColor: string;
  tooltip?: string;
  stats?: { label: string; value: string }[];
  children?: React.ReactNode;
  isEmpty?: boolean;
}

function DetailCard({ title, icon, headerColor, tooltip, stats, children, isEmpty }: DetailCardProps) {
  if (isEmpty) return null;
  return (
    <div className="flex flex-col rounded-lg overflow-hidden bg-card border border-border shadow-sm hover:shadow-md transition-all">
      <div className={cn("px-4 py-2.5 flex items-center gap-2", headerColor)}>
        <span className="text-white/80">{icon}</span>
        <h3 className="text-sm font-semibold text-white">{title}</h3>
        {tooltip && (
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Info className="h-3.5 w-3.5 text-white/50 hover:text-white/90 transition-colors ml-auto cursor-help" />
              </TooltipTrigger>
              <TooltipContent side="top" className="max-w-xs">
                <p>{tooltip}</p>
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
        )}
      </div>
      <div className="p-4 flex flex-col flex-1">
        {children}
        {stats && stats.length > 0 && (
          <div className="flex justify-between text-xs border-t border-border pt-3 mt-auto">
            {stats.map((s, i) => (
              <div key={i} className={i > 0 ? "text-right" : ""}>
                <span className="text-muted-foreground">{s.label}</span>
                <p className="font-semibold text-foreground">{s.value}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Main Page ──────────────────────────────────────────────
export default function KdvPage() {
  const [selectedMonth, setSelectedMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { startDate, endDate } = getMonthRange(selectedMonth);

  const { data: summary, isLoading } = useInvoiceSummary(
    storeId || undefined,
    startDate,
    endDate
  );

  const { formatCurrency } = useCurrency();

  const { data: expensesData } = useStoreExpenses(storeId || undefined);
  const [productsExpanded, setProductsExpanded] = useState(false);
  const [salesVatExpanded, setSalesVatExpanded] = useState(true);
  const [purchaseVatExpanded, setPurchaseVatExpanded] = useState(true);
  const [expensesExpanded, setExpensesExpanded] = useState(true);

  // Filter expenses for selected month
  const monthlyExpenses = useMemo(() => {
    if (!expensesData?.expenses) return [];
    return expensesData.expenses
      .filter((exp) => {
        const expDate = new Date(exp.date);
        return (
          expDate.getFullYear() === selectedMonth.getFullYear() &&
          expDate.getMonth() === selectedMonth.getMonth() &&
          exp.vatRate != null && exp.vatRate > 0
        );
      })
      .sort((a, b) => (b.amount + (b.vatAmount ?? 0)) - (a.amount + (a.vatAmount ?? 0)));
  }, [expensesData?.expenses, selectedMonth]);

  // Calculate expense totals
  const expenseTotals = useMemo(() => {
    return monthlyExpenses.reduce(
      (acc, exp) => ({
        totalAmount: acc.totalAmount + exp.amount + (exp.vatAmount ?? 0),
        totalVatAmount: acc.totalVatAmount + (exp.vatAmount ?? 0),
      }),
      { totalAmount: 0, totalVatAmount: 0 }
    );
  }, [monthlyExpenses]);

  // Filter invoice types where totalVatAmount > 0
  const vatItems = useMemo(() => {
    if (!summary?.invoicesByType) return [];
    return summary.invoicesByType.filter((item) => item.totalVatAmount > 0);
  }, [summary?.invoicesByType]);

  // Calculate totals
  const totals = useMemo(() => {
    return vatItems.reduce(
      (acc, item) => ({
        totalAmount: acc.totalAmount + item.totalAmount,
        totalVatAmount: acc.totalVatAmount + item.totalVatAmount,
      }),
      { totalAmount: 0, totalVatAmount: 0 }
    );
  }, [vatItems]);

  // KDV values
  const salesVatKdv = summary?.salesVat?.totalVatAmount ?? 0;
  const invoiceKdv = totals.totalVatAmount;
  const purchaseVatKdv = summary?.purchaseVat?.totalPurchaseVatAmount ?? 0;
  const expenseKdv = expenseTotals.totalVatAmount;
  const totalGiderKdv = invoiceKdv + purchaseVatKdv + expenseKdv;
  const netKdv = salesVatKdv - totalGiderKdv;
  const hasData = !isLoading && (vatItems.length > 0 || salesVatKdv > 0);

  // Progress bar percentage
  const totalBar = salesVatKdv + totalGiderKdv;
  const salesPercent = totalBar > 0 ? (salesVatKdv / totalBar) * 100 : 50;

  const handlePrevMonth = () => {
    setSelectedMonth(
      (prev) => new Date(prev.getFullYear(), prev.getMonth() - 1, 1)
    );
  };

  const handleNextMonth = () => {
    const now = new Date();
    const nextMonth = new Date(
      selectedMonth.getFullYear(),
      selectedMonth.getMonth() + 1,
      1
    );
    if (nextMonth <= new Date(now.getFullYear(), now.getMonth(), 1)) {
      setSelectedMonth(nextMonth);
    }
  };

  const isCurrentMonth =
    selectedMonth.getFullYear() === new Date().getFullYear() &&
    selectedMonth.getMonth() === new Date().getMonth();

  if (storeLoading || isLoading) {
    return <KdvPageSkeleton />;
  }

  if (!storeId) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-muted-foreground">Lutfen bir magaza secin.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* ─── Header + Month Selector ─── */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-primary/10">
            <Calculator className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-xl font-bold">KDV Hesaplama</h1>
            <p className="text-xs text-muted-foreground">Aylik KDV ozeti ve detaylari</p>
          </div>
        </div>
        <div className="flex items-center gap-2 bg-muted/50 rounded-lg px-1 py-1">
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handlePrevMonth}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm font-medium min-w-[150px] text-center">
            {formatMonthLabel(selectedMonth)}
          </span>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={handleNextMonth}
            disabled={isCurrentMonth}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {!hasData ? (
        /* ─── Empty State ─── */
        <div className="flex flex-col items-center justify-center h-64 border rounded-lg bg-muted/10">
          <div className="p-4 rounded-full bg-muted/30 mb-4">
            <Calculator className="h-10 w-10 text-muted-foreground/40" />
          </div>
          <p className="text-lg font-medium text-muted-foreground mb-1">
            Bu donemde KDV verisi bulunamadi
          </p>
          <p className="text-sm text-muted-foreground/70 max-w-md text-center">
            {formatMonthLabel(selectedMonth)} donemine ait KDV&apos;li fatura, satis veya gider kaydi bulunmamaktadir.
          </p>
        </div>
      ) : (
        <>
          {/* ─── Summary Cards (3 cards, period-card style) ─── */}
          <div className="flex gap-4 overflow-x-auto pb-2">
            {/* Hesaplanan KDV (Satis) */}
            <SummaryCard
              title="Hesaplanan KDV"
              subtitle="Musteriden tahsil edilen satis KDV'si"
              amount={salesVatKdv}
              icon={<TrendingUp className="h-4 w-4" />}
              headerColor="bg-emerald-600"
              amountColor="text-emerald-700 dark:text-emerald-400"
              formatCurrency={formatCurrency}
            >
              <div className="flex justify-between text-xs border-t border-border pt-3 mt-auto">
                <div>
                  <span className="text-muted-foreground">Siparis Adedi</span>
                  <p className="font-semibold text-foreground">
                    {summary?.salesVat?.totalItemsSold ?? 0}
                  </p>
                </div>
                <div className="text-right">
                  <span className="text-muted-foreground">Satis Tutari</span>
                  <p className="font-semibold text-foreground">
                    {formatCurrency(summary?.salesVat?.totalSalesAmount ?? 0)}
                  </p>
                </div>
              </div>
            </SummaryCard>

            {/* Indirilecek KDV (Gider) */}
            <SummaryCard
              title="Indirilecek KDV"
              subtitle="Odedigimiz toplam KDV"
              amount={totalGiderKdv}
              icon={<TrendingDown className="h-4 w-4" />}
              headerColor="bg-red-500"
              amountColor="text-red-700 dark:text-red-400"
              formatCurrency={formatCurrency}
            >
              <div className="space-y-1.5 mb-3">
                {invoiceKdv > 0 && (
                  <div className="flex justify-between text-xs">
                    <span className="text-muted-foreground">Faturalar</span>
                    <span className="font-medium">{formatCurrency(invoiceKdv)}</span>
                  </div>
                )}
                {purchaseVatKdv > 0 && (
                  <div className="flex justify-between text-xs">
                    <span className="text-muted-foreground">Mal Alis</span>
                    <span className="font-medium">{formatCurrency(purchaseVatKdv)}</span>
                  </div>
                )}
                {expenseKdv > 0 && (
                  <div className="flex justify-between text-xs">
                    <span className="text-muted-foreground">Diger Giderler</span>
                    <span className="font-medium">{formatCurrency(expenseKdv)}</span>
                  </div>
                )}
              </div>
              <div className="flex justify-between text-xs border-t border-border pt-3 mt-auto">
                <div>
                  <span className="text-muted-foreground">Kalem Sayisi</span>
                  <p className="font-semibold text-foreground">
                    {vatItems.length + (summary?.purchaseVat?.totalItemsPurchased ? 1 : 0) + monthlyExpenses.length}
                  </p>
                </div>
                <div className="text-right">
                  <span className="text-muted-foreground">Toplam Bedel</span>
                  <p className="font-semibold text-foreground">
                    {formatCurrency(totals.totalAmount + (summary?.purchaseVat?.totalPurchaseCostExclVat ?? 0) + expenseTotals.totalAmount)}
                  </p>
                </div>
              </div>
            </SummaryCard>

            {/* Net KDV */}
            <SummaryCard
              title={netKdv >= 0 ? "Odenecek KDV" : "Devreden KDV"}
              subtitle={netKdv >= 0 ? "Devlete odemeniz gereken KDV" : "Devletin size borclu oldugu KDV"}
              amount={netKdv}
              icon={<Scale className="h-4 w-4" />}
              headerColor={netKdv >= 0 ? "bg-amber-500" : "bg-blue-500"}
              amountColor={netKdv >= 0 ? "text-amber-700 dark:text-amber-400" : "text-blue-700 dark:text-blue-400"}
              formatCurrency={formatCurrency}
              negative={netKdv > 0}
            >
              <div className="mt-auto">
                <div className="flex justify-between text-xs border-t border-border pt-3">
                  <div>
                    <span className="text-muted-foreground">Hesaplanan</span>
                    <p className="font-semibold text-emerald-600">{formatCurrency(salesVatKdv)}</p>
                  </div>
                  <div className="text-right">
                    <span className="text-muted-foreground">Indirilecek</span>
                    <p className="font-semibold text-red-600">{formatCurrency(totalGiderKdv)}</p>
                  </div>
                </div>
              </div>
            </SummaryCard>
          </div>

          {/* ─── Distribution Bar ─── */}
          {totalBar > 0 && (
            <FadeIn direction="up" delay={0.2} duration={0.4}>
              <div className="space-y-2">
                {/* Labels with animated percentages */}
                <div className="flex items-center justify-between text-xs text-muted-foreground">
                  <span className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 inline-block" />
                    Hesaplanan KDV (
                    <NumberTicker value={salesPercent} decimals={0} suffix="%" />
                    )
                  </span>
                  <span className="flex items-center gap-1.5">
                    Indirilecek KDV (
                    <NumberTicker value={100 - salesPercent} decimals={0} suffix="%" />
                    )
                    <span className="w-2.5 h-2.5 rounded-full bg-red-400 inline-block" />
                  </span>
                </div>

                {/* Progress Bar with Motion */}
                <div className="h-3 rounded-full overflow-hidden flex bg-muted/30 border relative group">
                  {/* Hesaplanan KDV (Yeşil) */}
                  <motion.div
                    className="bg-emerald-500 dark:bg-emerald-400 relative overflow-hidden"
                    initial={{ width: 0 }}
                    animate={{ width: `${salesPercent}%` }}
                    transition={{
                      type: "spring",
                      stiffness: 100,
                      damping: 20,
                      duration: 0.6,
                    }}
                  >
                    {/* Shimmer effect */}
                    <motion.div
                      className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent"
                      initial={{ x: "-100%" }}
                      animate={{ x: "200%" }}
                      transition={{
                        duration: 2,
                        repeat: Infinity,
                        ease: "linear",
                      }}
                    />
                  </motion.div>

                  {/* İndirilecek KDV (Kırmızı) */}
                  <motion.div
                    className="bg-red-400 dark:bg-red-500"
                    initial={{ width: "100%" }}
                    animate={{ width: `${100 - salesPercent}%` }}
                    transition={{
                      type: "spring",
                      stiffness: 100,
                      damping: 20,
                      duration: 0.6,
                    }}
                  />

                  {/* Hover tooltip */}
                  <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <span className="text-[10px] font-bold text-white drop-shadow-md">
                      {formatCurrency(salesVatKdv)} / {formatCurrency(totalGiderKdv)}
                    </span>
                  </div>
                </div>
              </div>
            </FadeIn>
          )}

          {/* ─── Row 1: Trendyol Fatura KDV'leri | Diğer Gider KDV'leri (50%-50%) ─── */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

            {/* Left: Trendyol Fatura KDV'leri */}
            <DetailCard
              title="Trendyol Fatura KDV'leri"
              icon={<FileText className="h-4 w-4" />}
              headerColor="bg-orange-500"
              tooltip="Trendyol tarafindan kesilen faturalardaki KDV tutarlari (komisyon, kargo, ceza vb.)"
              isEmpty={vatItems.length === 0}
              stats={[
                { label: "Fatura Tipi", value: `${vatItems.length} kalem` },
                { label: "Toplam KDV", value: formatCurrency(invoiceKdv) },
              ]}
            >
              <div className="space-y-1.5 mb-3">
                {vatItems.map((item) => {
                  const vatRate = item.vatRate != null ? item.vatRate : calculateVatRate(item);
                  return (
                    <div key={item.invoiceTypeCode} className="flex items-center justify-between text-xs bg-muted/30 rounded-md px-3 py-2">
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{item.invoiceType}</span>
                        <span className="text-muted-foreground">%{vatRate}</span>
                      </div>
                      <div className="flex items-center gap-4">
                        <span className="text-muted-foreground">{formatCurrency(item.totalAmount)}</span>
                        <span className="font-semibold text-red-600 dark:text-red-400 min-w-[80px] text-right">
                          {formatCurrency(item.totalVatAmount)}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </DetailCard>

            {/* Right: Diğer Gider KDV'leri */}
            <DetailCard
              title="Diger Gider KDV'leri"
              icon={<Receipt className="h-4 w-4" />}
              headerColor="bg-purple-500"
              tooltip="Platform disinda odediginiz giderlerin KDV'si (ofis, muhasebe, paketleme vb.)"
              isEmpty={monthlyExpenses.length === 0}
              stats={[
                { label: "Kalem Sayisi", value: `${monthlyExpenses.length} kalem` },
                { label: "KDV Tutari", value: formatCurrency(expenseKdv) },
              ]}
            >
              <div className="mb-3">
                <button
                  className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors w-full"
                  onClick={() => setExpensesExpanded(!expensesExpanded)}
                >
                  Gider Detaylari ({monthlyExpenses.length} kalem)
                  {expensesExpanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                </button>
                {expensesExpanded && (
                  <div className="mt-2 space-y-1.5">
                    {monthlyExpenses.map((exp) => (
                      <div key={exp.id} className="flex items-center justify-between text-xs bg-muted/30 rounded-md px-3 py-2">
                        <div className="flex items-center gap-2 min-w-0 flex-1 mr-4">
                          <span className="font-medium truncate">
                            {exp.expenseCategoryName}
                            {exp.name && <span className="text-muted-foreground"> — {exp.name}</span>}
                          </span>
                          {exp.vatRate != null && (
                            <span className="text-muted-foreground flex-shrink-0">%{exp.vatRate}</span>
                          )}
                        </div>
                        <div className="flex items-center gap-4 flex-shrink-0">
                          <span className="text-muted-foreground">{formatCurrency(exp.amount + (exp.vatAmount ?? 0))}</span>
                          <span className="font-semibold text-purple-600 dark:text-purple-400 min-w-[80px] text-right">
                            {exp.vatAmount ? formatCurrency(exp.vatAmount) : "—"}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </DetailCard>
          </div>

          {/* ─── Row 2: Satış KDV'si | Mal Alış KDV'si (50%-50%) ─── */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

            {/* Left: Satış KDV'si */}
            <DetailCard
              title="Satis KDV'si (Tahsil Edilen)"
              icon={<ShoppingBag className="h-4 w-4" />}
              headerColor="bg-emerald-600"
              tooltip="Musteriden tahsil edilen KDV. Satislariniz uzerinden hesaplanan KDV tutarlari."
              isEmpty={!summary?.salesVat || summary.salesVat.totalSalesAmount === 0}
              stats={[
                { label: "Toplam Adet", value: String(summary?.salesVat?.totalItemsSold ?? 0) },
                { label: "KDV Tutari", value: formatCurrency(salesVatKdv) },
              ]}
            >
            {/* Product-based breakdown */}
            {summary?.salesVat?.byProduct && summary.salesVat.byProduct.length > 0 && (
              <div className="mb-3">
                <button
                  className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors w-full"
                  onClick={() => setProductsExpanded(!productsExpanded)}
                >
                  Urun Bazli Kirilim ({summary.salesVat.byProduct.length} urun)
                  {productsExpanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                </button>
                {productsExpanded && (
                  <div className="mt-2 space-y-1.5 max-h-[400px] overflow-y-auto">
                    {summary.salesVat.byProduct.map((product) => (
                      <div key={product.barcode} className="flex items-center gap-3 text-xs bg-muted/30 rounded-md px-3 py-2.5 hover:bg-muted/50 transition-colors">
                        {/* Ürün Görseli */}
                        {product.image ? (
                          <img
                            src={product.image}
                            alt={product.productName}
                            className="w-12 h-12 object-cover rounded-md flex-shrink-0"
                            onError={(e) => {
                              e.currentTarget.style.display = 'none';
                            }}
                          />
                        ) : (
                          <div className="w-12 h-12 bg-muted rounded-md flex items-center justify-center flex-shrink-0">
                            <ShoppingBag className="h-6 w-6 text-muted-foreground" />
                          </div>
                        )}

                        {/* Ürün Detayları */}
                        <div className="flex items-center justify-between min-w-0 flex-1">
                          <div className="flex flex-col gap-1 min-w-0 flex-1">
                            {/* Ürün Adı + Link */}
                            {product.productUrl ? (
                              <a
                                href={product.productUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="font-medium truncate hover:text-emerald-600 transition-colors"
                              >
                                {product.productName}
                              </a>
                            ) : (
                              <span className="font-medium truncate">{product.productName}</span>
                            )}

                            {/* Marka + Adet + KDV Oranı */}
                            <div className="flex items-center gap-2 text-muted-foreground flex-shrink-0">
                              {product.brand && (
                                <>
                                  <span className="font-medium">{product.brand}</span>
                                  <span>•</span>
                                </>
                              )}
                              <span>{product.quantity} adet</span>
                              <span>•</span>
                              <span>%{product.vatRate} KDV</span>
                            </div>
                          </div>

                          {/* Tutar Bilgileri */}
                          <div className="flex items-center gap-4 ml-4 flex-shrink-0">
                            <span className="text-muted-foreground">{formatCurrency(product.salesAmount)}</span>
                            <span className="font-semibold text-emerald-600 dark:text-emerald-400 min-w-[80px] text-right">
                              {formatCurrency(product.vatAmount)}
                            </span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Rate-based breakdown */}
            <div className="mb-3">
              <button
                className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors w-full"
                onClick={() => setSalesVatExpanded(!salesVatExpanded)}
              >
                Oran Bazli Kirilim
                {salesVatExpanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
              </button>
              {salesVatExpanded && summary?.salesVat?.byRate && (
                <div className="mt-2 space-y-1.5">
                  {summary.salesVat.byRate.map((rate) => (
                    <div key={rate.vatRate} className="flex items-center justify-between text-xs bg-muted/30 rounded-md px-3 py-2">
                      <div className="flex items-center gap-2">
                        <span className="font-medium">%{rate.vatRate}</span>
                        <span className="text-muted-foreground">({rate.itemCount} adet)</span>
                      </div>
                      <div className="flex items-center gap-4">
                        <span className="text-muted-foreground">{formatCurrency(rate.salesAmount)}</span>
                        <span className="font-semibold text-emerald-600 dark:text-emerald-400 min-w-[80px] text-right">
                          {formatCurrency(rate.vatAmount)}
                        </span>
                      </div>
                    </div>
                  ))}
                  {summary.salesVat.itemsWithoutVatRate > 0 && (
                    <div className="flex items-center gap-2 text-xs text-amber-600 dark:text-amber-400 px-3 py-2 bg-amber-50 dark:bg-amber-950/30 rounded-md">
                      <AlertTriangle className="h-3.5 w-3.5 flex-shrink-0" />
                      {summary.salesVat.itemsWithoutVatRate} adet urunun KDV orani bulunamadi.
                    </div>
                  )}
                </div>
              )}
            </div>
          </DetailCard>

            {/* Right: Mal Alış KDV'si */}
            <DetailCard
              title="Mal Alis KDV'si"
              icon={<Package className="h-4 w-4" />}
              headerColor="bg-blue-500"
              tooltip="Satin alma siparislerindeki (PO) mallarin KDV'si. Stok giris tarihine gore hesaplanir."
              isEmpty={!summary?.purchaseVat || summary.purchaseVat.totalPurchaseVatAmount === 0}
              stats={[
                { label: "Toplam Adet", value: `${summary?.purchaseVat?.totalItemsPurchased ?? 0} adet` },
                { label: "KDV Tutari", value: formatCurrency(purchaseVatKdv) },
              ]}
            >
              <div className="mb-3">
                {/* Main totals */}
                <div className="flex items-center justify-between text-xs bg-muted/30 rounded-md px-3 py-2 mb-1.5">
                  <span className="font-medium">Maliyet (KDV Haric)</span>
                  <span className="font-semibold">{formatCurrency(summary?.purchaseVat?.totalPurchaseCostExclVat ?? 0)}</span>
                </div>

                {/* Expandable rate breakdown */}
                {summary?.purchaseVat?.byRate && summary.purchaseVat.byRate.length > 0 && (
                  <>
                    <button
                      className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors w-full mt-2"
                      onClick={() => setPurchaseVatExpanded(!purchaseVatExpanded)}
                    >
                      Oran Bazli Kirilim
                      {purchaseVatExpanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                    </button>
                    {purchaseVatExpanded && (
                      <div className="mt-2 space-y-1.5">
                        {summary.purchaseVat.byRate.map((rate) => (
                          <div key={rate.vatRate} className="flex items-center justify-between text-xs bg-muted/30 rounded-md px-3 py-2">
                            <div className="flex items-center gap-2">
                              <span className="font-medium">%{rate.vatRate}</span>
                              <span className="text-muted-foreground">({rate.itemCount} adet)</span>
                            </div>
                            <div className="flex items-center gap-4">
                              <span className="text-muted-foreground">{formatCurrency(rate.costAmount)}</span>
                              <span className="font-semibold text-blue-600 dark:text-blue-400 min-w-[80px] text-right">
                                {formatCurrency(rate.vatAmount)}
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div>
            </DetailCard>
          </div>

          {/* ─── Grand Total Card ─── */}
          <div className="rounded-lg border bg-card shadow-sm overflow-hidden">
            <div className="bg-gradient-to-r from-slate-700 to-slate-800 px-5 py-3">
              <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                <Calculator className="h-4 w-4 text-white/80" />
                KDV Ozet Tablosu
              </h3>
            </div>
            <div className="p-5">
              <div className="grid grid-cols-3 gap-6 text-center">
                <div>
                  <p className="text-xs text-muted-foreground mb-1">Hesaplanan KDV</p>
                  <p className="text-lg font-bold text-emerald-600 dark:text-emerald-400">
                    {formatCurrency(salesVatKdv)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground mb-1">Indirilecek KDV</p>
                  <p className="text-lg font-bold text-red-600 dark:text-red-400">
                    {formatCurrency(totalGiderKdv)}
                  </p>
                </div>
                <div className={cn(
                  "rounded-lg py-2 -my-2 px-3",
                  netKdv >= 0 ? "bg-amber-50 dark:bg-amber-950/30" : "bg-blue-50 dark:bg-blue-950/30"
                )}>
                  <p className="text-xs text-muted-foreground mb-1">
                    {netKdv >= 0 ? "Odenecek KDV" : "Devreden KDV"}
                  </p>
                  <p className={cn(
                    "text-lg font-bold",
                    netKdv >= 0 ? "text-amber-700 dark:text-amber-400" : "text-blue-700 dark:text-blue-400"
                  )}>
                    {netKdv < 0 && "\u2212"}{formatCurrency(Math.abs(netKdv))}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
