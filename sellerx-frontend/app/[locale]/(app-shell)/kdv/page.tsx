"use client";

import { useState, useMemo } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useInvoiceSummary } from "@/hooks/queries/use-invoices";
import { useCurrency } from "@/lib/contexts/currency-context";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { ChevronLeft, ChevronRight, ChevronDown, ChevronUp, Calculator, AlertTriangle, Receipt, ShoppingBag, Scale, FileText, Package, TrendingUp, TrendingDown } from "lucide-react";
import { useStoreExpenses } from "@/hooks/queries/use-expenses";
import { Skeleton } from "@/components/ui/skeleton";
import { StatCardSkeleton, TableSkeleton } from "@/components/ui/skeleton-blocks";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

function KdvPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Skeleton className="h-9 w-9 rounded-md" />
        <Skeleton className="h-6 w-40" />
        <Skeleton className="h-9 w-9 rounded-md" />
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => <StatCardSkeleton key={i} />)}
      </div>
      <Card>
        <CardHeader><Skeleton className="h-5 w-32" /></CardHeader>
        <CardContent><Skeleton className="h-8 w-full rounded-lg" /></CardContent>
      </Card>
      <TableSkeleton columns={5} rows={8} />
    </div>
  );
}

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
  const [expensesExpanded, setExpensesExpanded] = useState(false);
  const [salesVatExpanded, setSalesVatExpanded] = useState(false);

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

  // KDV Mahsuplasma values
  const salesVatKdv = summary?.salesVat?.totalVatAmount ?? 0;
  const invoiceKdv = totals.totalVatAmount;
  const cogsKdv = summary?.costOfGoodsSold?.totalCostVatAmount ?? 0;
  const expenseKdv = expenseTotals.totalVatAmount;
  const totalGiderKdv = invoiceKdv + cogsKdv + expenseKdv;
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
      {/* Header + Month Selector */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Calculator className="h-6 w-6 text-primary" />
          <h1 className="text-2xl font-bold">KDV Hesaplama</h1>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon" onClick={handlePrevMonth}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm font-medium min-w-[160px] text-center">
            {formatMonthLabel(selectedMonth)}
          </span>
          <Button
            variant="outline"
            size="icon"
            onClick={handleNextMonth}
            disabled={isCurrentMonth}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* KDV Mahsuplasma Summary Cards - TOP */}
      {!hasData ? (
        /* Improved empty state */
        <div className="flex flex-col items-center justify-center h-56 border rounded-lg bg-muted/20">
          <Calculator className="h-12 w-12 text-muted-foreground/40 mb-4" />
          <p className="text-lg font-medium text-muted-foreground mb-1">
            Bu donemde KDV verisi bulunamadi
          </p>
          <p className="text-sm text-muted-foreground/70 max-w-md text-center">
            {formatMonthLabel(selectedMonth)} donemine ait KDV&apos;li fatura, satis veya gider kaydi bulunmamaktadir. Veriler senkronize edildikten sonra burada gorunecektir.
          </p>
        </div>
      ) : (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Topladigimiz KDV */}
            <div className="rounded-xl border bg-green-50 dark:bg-green-950/30 p-5 space-y-1">
              <div className="flex items-center gap-2 text-green-600 dark:text-green-400">
                <TrendingUp className="h-4 w-4" />
                <p className="text-sm font-medium">Topladigimiz KDV</p>
              </div>
              <p className="text-2xl font-bold text-green-700 dark:text-green-400">
                {formatCurrency(salesVatKdv)}
              </p>
              <p className="text-xs text-muted-foreground">
                Musteriden tahsil edilen satis KDV&apos;si
              </p>
            </div>

            {/* Odedigimiz KDV */}
            <div className="rounded-xl border bg-red-50 dark:bg-red-950/30 p-5 space-y-1">
              <div className="flex items-center gap-2 text-red-600 dark:text-red-400">
                <TrendingDown className="h-4 w-4" />
                <p className="text-sm font-medium">Odedigimiz KDV</p>
              </div>
              <p className="text-2xl font-bold text-red-700 dark:text-red-400">
                {formatCurrency(totalGiderKdv)}
              </p>
              <div className="text-xs text-muted-foreground space-y-0.5">
                <p>Faturalar: {formatCurrency(invoiceKdv)} · Maliyet: {formatCurrency(cogsKdv)}{expenseKdv > 0 ? ` · Gider: ${formatCurrency(expenseKdv)}` : ""}</p>
              </div>
            </div>

            {/* NET KDV */}
            <div className={`rounded-xl border p-5 space-y-1 ${
              netKdv >= 0
                ? "bg-amber-50 dark:bg-amber-950/30"
                : "bg-blue-50 dark:bg-blue-950/30"
            }`}>
              <div className={`flex items-center gap-2 ${
                netKdv >= 0 ? "text-amber-600 dark:text-amber-400" : "text-blue-600 dark:text-blue-400"
              }`}>
                <Scale className="h-4 w-4" />
                <p className="text-sm font-medium">NET KDV</p>
              </div>
              <p className={`text-2xl font-bold ${
                netKdv >= 0
                  ? "text-amber-700 dark:text-amber-400"
                  : "text-blue-700 dark:text-blue-400"
              }`}>
                {netKdv < 0 && "\u2212"}{formatCurrency(Math.abs(netKdv))}
              </p>
              <p className="text-xs text-muted-foreground">
                {netKdv >= 0
                  ? "Devlete odemeniz gereken KDV"
                  : "Devletin size borclu oldugu KDV (devreden)"}
              </p>
            </div>
          </div>

          {/* Distribution Bar */}
          {totalBar > 0 && (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>Satis KDV ({salesPercent.toFixed(0)}%)</span>
                <span>Gider KDV ({(100 - salesPercent).toFixed(0)}%)</span>
              </div>
              <div className="h-3 rounded-full overflow-hidden flex bg-muted/30 border">
                <div
                  className="bg-green-500 dark:bg-green-400 transition-all duration-500"
                  style={{ width: `${salesPercent}%` }}
                />
                <div
                  className="bg-red-400 dark:bg-red-500 transition-all duration-500"
                  style={{ width: `${100 - salesPercent}%` }}
                />
              </div>
            </div>
          )}

          {/* Detail Table */}
          <div className="border rounded-lg overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/30">
                  <TableHead>Kalem</TableHead>
                  <TableHead className="text-right">KDV Orani</TableHead>
                  <TableHead className="text-right">Bedel (KDV Dahil)</TableHead>
                  <TableHead className="text-right">KDV Tutari</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {/* === SATIS KDV'SI === */}
                {summary?.salesVat && summary.salesVat.totalSalesAmount > 0 && (
                  <>
                    <TableRow className="bg-green-50/60 dark:bg-green-950/20 border-t-2 border-green-200 dark:border-green-900">
                      <TableCell colSpan={4} className="py-1.5">
                        <div className="flex items-center gap-2 text-xs font-semibold text-green-700 dark:text-green-400 uppercase tracking-wide">
                          <ShoppingBag className="h-3.5 w-3.5" />
                          Satis KDV&apos;si (Tahsil Edilen)
                        </div>
                      </TableCell>
                    </TableRow>
                    <TableRow
                      className="cursor-pointer hover:bg-green-50 dark:hover:bg-green-950/30 transition-colors"
                      onClick={() => setSalesVatExpanded(!salesVatExpanded)}
                    >
                      <TableCell className="font-medium pl-8">
                        <div className="flex items-center gap-2">
                          Tum Satislar
                          <span className="text-xs text-muted-foreground">
                            ({summary.salesVat.totalItemsSold} adet)
                          </span>
                          {salesVatExpanded ? (
                            <ChevronUp className="h-3.5 w-3.5 text-muted-foreground" />
                          ) : (
                            <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">&mdash;</TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(summary.salesVat.totalSalesAmount)}
                      </TableCell>
                      <TableCell className="text-right font-medium text-green-700 dark:text-green-400">
                        {formatCurrency(summary.salesVat.totalVatAmount)}
                      </TableCell>
                    </TableRow>
                    {salesVatExpanded &&
                      summary.salesVat.byRate.map((rate) => (
                        <TableRow
                          key={rate.vatRate}
                          className="bg-green-50/30 dark:bg-green-950/10"
                        >
                          <TableCell className="pl-14 text-sm">
                            <span className="text-foreground">%{rate.vatRate} KDV Oranli Satislar</span>
                            <span className="text-muted-foreground ml-1.5">
                              &mdash; {rate.itemCount} adet
                            </span>
                          </TableCell>
                          <TableCell className="text-right text-sm">%{rate.vatRate}</TableCell>
                          <TableCell className="text-right text-sm">
                            {formatCurrency(rate.salesAmount)}
                          </TableCell>
                          <TableCell className="text-right text-sm">
                            {formatCurrency(rate.vatAmount)}
                          </TableCell>
                        </TableRow>
                      ))}
                    {salesVatExpanded && summary.salesVat.itemsWithoutVatRate > 0 && (
                      <TableRow className="bg-amber-50 dark:bg-amber-950/30">
                        <TableCell colSpan={4}>
                          <div className="flex items-center gap-2 text-xs text-amber-700 dark:text-amber-400 pl-8">
                            <AlertTriangle className="h-3.5 w-3.5 flex-shrink-0" />
                            <span>
                              {summary.salesVat.itemsWithoutVatRate} adet urunun satis KDV orani bulunamadi.
                            </span>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </>
                )}

                {/* === GIDER KDV'LERI === */}
                {(vatItems.length > 0 || (summary?.costOfGoodsSold && summary.costOfGoodsSold.totalCostIncludingVat > 0) || monthlyExpenses.length > 0) && (
                  <TableRow className="bg-red-50/60 dark:bg-red-950/20 border-t-2 border-red-200 dark:border-red-900">
                    <TableCell colSpan={4} className="py-1.5">
                      <div className="flex items-center gap-2 text-xs font-semibold text-red-700 dark:text-red-400 uppercase tracking-wide">
                        <FileText className="h-3.5 w-3.5" />
                        Gider KDV&apos;leri (Odenen)
                      </div>
                    </TableCell>
                  </TableRow>
                )}

                {/* Trendyol Faturalari */}
                {vatItems.map((item) => {
                  const vatRate = item.vatRate != null ? item.vatRate : calculateVatRate(item);
                  return (
                    <TableRow key={item.invoiceTypeCode} className="hover:bg-muted/30 transition-colors">
                      <TableCell className="font-medium pl-8">
                        {item.invoiceType}
                      </TableCell>
                      <TableCell className="text-right">%{vatRate}</TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(item.totalAmount)}
                      </TableCell>
                      <TableCell className="text-right text-red-600 dark:text-red-400">
                        {formatCurrency(item.totalVatAmount)}
                      </TableCell>
                    </TableRow>
                  );
                })}

                {/* COGS Row */}
                {summary?.costOfGoodsSold && summary.costOfGoodsSold.totalCostIncludingVat > 0 && (
                  <TableRow className="hover:bg-muted/30 transition-colors">
                    <TableCell className="font-medium pl-8">
                      <div className="flex items-center gap-2">
                        <Package className="h-4 w-4 text-muted-foreground" />
                        Satilan Urunun KDV
                        <span className="text-xs text-muted-foreground">
                          ({summary.costOfGoodsSold.totalItemsSold} adet)
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      %{(() => {
                        const cogs = summary!.costOfGoodsSold!;
                        const base = cogs.totalCostIncludingVat - cogs.totalCostVatAmount;
                        if (base <= 0) return 0;
                        return Math.round((cogs.totalCostVatAmount / base) * 100 * 10) / 10;
                      })()}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatCurrency(summary.costOfGoodsSold.totalCostIncludingVat)}
                    </TableCell>
                    <TableCell className="text-right text-red-600 dark:text-red-400">
                      {formatCurrency(summary.costOfGoodsSold.totalCostVatAmount)}
                    </TableCell>
                  </TableRow>
                )}
                {/* COGS Warnings */}
                {summary?.costOfGoodsSold && (summary.costOfGoodsSold.itemsWithoutCost > 0 || summary.costOfGoodsSold.itemsWithoutCostVat > 0) && (
                  <TableRow className="bg-amber-50 dark:bg-amber-950/30">
                    <TableCell colSpan={4}>
                      <div className="flex items-center gap-2 text-xs text-amber-700 dark:text-amber-400 pl-8">
                        <AlertTriangle className="h-3.5 w-3.5 flex-shrink-0" />
                        <span>
                          {summary.costOfGoodsSold.itemsWithoutCost > 0 && (
                            <>{summary.costOfGoodsSold.itemsWithoutCost} adet urunun maliyeti girilmemis. </>
                          )}
                          {summary.costOfGoodsSold.itemsWithoutCostVat > 0 && (
                            <>{summary.costOfGoodsSold.itemsWithoutCostVat} adet urunun maliyet KDV orani girilmemis.</>
                          )}
                        </span>
                      </div>
                    </TableCell>
                  </TableRow>
                )}

                {/* Expenses */}
                {monthlyExpenses.length > 0 && (
                  <>
                    <TableRow
                      className="cursor-pointer hover:bg-muted/30 transition-colors"
                      onClick={() => setExpensesExpanded(!expensesExpanded)}
                    >
                      <TableCell className="font-medium pl-8">
                        <div className="flex items-center gap-2">
                          <Receipt className="h-4 w-4 text-muted-foreground" />
                          Diger Giderler
                          <span className="text-xs text-muted-foreground">
                            ({monthlyExpenses.length} kalem)
                          </span>
                          {expensesExpanded ? (
                            <ChevronUp className="h-3.5 w-3.5 text-muted-foreground" />
                          ) : (
                            <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">&mdash;</TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(expenseTotals.totalAmount)}
                      </TableCell>
                      <TableCell className="text-right text-red-600 dark:text-red-400">
                        {formatCurrency(expenseTotals.totalVatAmount)}
                      </TableCell>
                    </TableRow>
                    {expensesExpanded &&
                      monthlyExpenses.map((exp) => (
                        <TableRow
                          key={exp.id}
                          className="bg-muted/10"
                        >
                          <TableCell className="pl-14 text-sm">
                            <span className="text-foreground">{exp.expenseCategoryName}</span>
                            {exp.name && (
                              <span className="text-muted-foreground ml-1.5">&mdash; {exp.name}</span>
                            )}
                          </TableCell>
                          <TableCell className="text-right text-sm">
                            {exp.vatRate != null ? `%${exp.vatRate}` : "&mdash;"}
                          </TableCell>
                          <TableCell className="text-right text-sm">
                            {formatCurrency(exp.amount + (exp.vatAmount ?? 0))}
                          </TableCell>
                          <TableCell className="text-right text-sm">
                            {exp.vatAmount ? formatCurrency(exp.vatAmount) : "&mdash;"}
                          </TableCell>
                        </TableRow>
                      ))}
                  </>
                )}

                {/* === GIDER SUBTOTAL === */}
                {(vatItems.length > 0 || (summary?.costOfGoodsSold && summary.costOfGoodsSold.totalCostIncludingVat > 0) || monthlyExpenses.length > 0) && (
                  <TableRow className="bg-red-50/40 dark:bg-red-950/15 font-medium">
                    <TableCell className="pl-8 text-sm text-red-700 dark:text-red-400">
                      Gider KDV Toplami
                    </TableCell>
                    <TableCell />
                    <TableCell className="text-right text-sm">
                      {formatCurrency(
                        totals.totalAmount +
                        expenseTotals.totalAmount +
                        (summary?.costOfGoodsSold?.totalCostIncludingVat ?? 0)
                      )}
                    </TableCell>
                    <TableCell className="text-right text-sm font-semibold text-red-700 dark:text-red-400">
                      {formatCurrency(totalGiderKdv)}
                    </TableCell>
                  </TableRow>
                )}

                {/* === GENEL TOPLAM === */}
                <TableRow className="bg-muted font-bold border-t-2 border-foreground/20">
                  <TableCell className="text-base">Genel Toplam</TableCell>
                  <TableCell />
                  <TableCell className="text-right text-base">
                    {formatCurrency(
                      totals.totalAmount +
                      expenseTotals.totalAmount +
                      (summary?.salesVat?.totalSalesAmount ?? 0) +
                      (summary?.costOfGoodsSold?.totalCostIncludingVat ?? 0)
                    )}
                  </TableCell>
                  <TableCell className="text-right text-base">
                    {formatCurrency(
                      totals.totalVatAmount +
                      expenseTotals.totalVatAmount +
                      (summary?.salesVat?.totalVatAmount ?? 0) +
                      (summary?.costOfGoodsSold?.totalCostVatAmount ?? 0)
                    )}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
        </>
      )}
    </div>
  );
}

// Known Turkish VAT rates
const KNOWN_VAT_RATES = [0, 1, 8, 10, 18, 20];

// Calculate VAT rate from backend amounts and snap to nearest known rate
function calculateVatRate(item: {
  totalAmount: number;
  totalVatAmount: number;
}): number {
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
