"use client";

import { useState, useMemo, useCallback, useEffect } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  useInvoiceSummary,
  useInvoicesByCategory,
  useAllInvoices,
} from "@/hooks/queries/use-invoices";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Calendar as CalendarIcon, FileText } from "lucide-react";
import {
  InvoiceSummaryCards,
  InvoiceTable,
  InvoiceDetailPanel,
  InvoiceCategoryTable,
  InvoiceExportButton,
} from "@/components/financial";
import { format, subDays, startOfMonth, endOfMonth, subMonths, startOfYear, endOfYear, subYears } from "date-fns";
import { tr } from "date-fns/locale";
import type { InvoiceDetail } from "@/types/invoice";

// Category type matching summary cards
type CategoryKey = "KOMISYON" | "KARGO" | "ULUSLARARASI" | "CEZA" | "REKLAM" | "IADE" | "DIGER" | "ALL" | "KESINTI";

// Predefined date ranges
const DATE_PRESETS = [
  { label: "Son 7 Gün", value: "7d" },
  { label: "Son 30 Gün", value: "30d" },
  { label: "Son 90 Gün", value: "90d" },
  { label: "Bu Ay", value: "thisMonth" },
  { label: "Geçen Ay", value: "lastMonth" },
  { label: "Bu Yıl", value: "thisYear" },
  { label: "Geçen Yıl", value: "lastYear" },
  { label: "Özel", value: "custom" },
] as const;

type DatePreset = (typeof DATE_PRESETS)[number]["value"];

function getDateRange(preset: DatePreset): { start: Date; end: Date } {
  const today = new Date();
  const end = today;

  switch (preset) {
    case "7d":
      return { start: subDays(today, 7), end };
    case "30d":
      return { start: subDays(today, 30), end };
    case "90d":
      return { start: subDays(today, 90), end };
    case "thisMonth":
      return { start: startOfMonth(today), end };
    case "lastMonth":
      const lastMonth = subMonths(today, 1);
      return { start: startOfMonth(lastMonth), end: endOfMonth(lastMonth) };
    case "thisYear":
      return { start: startOfYear(today), end };
    case "lastYear":
      const lastYear = subYears(today, 1);
      return { start: startOfYear(lastYear), end: endOfYear(lastYear) };
    default:
      return { start: subDays(today, 30), end };
  }
}

const INVOICES_PER_PAGE = 50;

export default function InvoicesPage() {
  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Date range state
  const [datePreset, setDatePreset] = useState<DatePreset>("30d");
  const [customDateRange, setCustomDateRange] = useState<{
    start: Date | undefined;
    end: Date | undefined;
  }>({ start: undefined, end: undefined });
  const [calendarOpen, setCalendarOpen] = useState(false);

  // Category selection state
  const [selectedCategory, setSelectedCategory] = useState<CategoryKey | null>(null);

  // Pagination state for lazy loading
  const [page, setPage] = useState(0);
  const [allInvoices, setAllInvoices] = useState<InvoiceDetail[]>([]);

  // Detail panel state
  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceDetail | null>(null);
  const [detailPanelOpen, setDetailPanelOpen] = useState(false);

  // Calculate effective date range
  const dateRange = useMemo(() => {
    if (datePreset === "custom" && customDateRange.start && customDateRange.end) {
      return { start: customDateRange.start, end: customDateRange.end };
    }
    return getDateRange(datePreset);
  }, [datePreset, customDateRange]);

  const startDateStr = format(dateRange.start, "yyyy-MM-dd");
  const endDateStr = format(dateRange.end, "yyyy-MM-dd");

  // Fetch invoice summary
  const {
    data: summary,
    isLoading: summaryLoading,
    error: summaryError,
  } = useInvoiceSummary(storeId || undefined, startDateStr, endDateStr);

  // Fetch invoices based on category selection
  // KESINTI is a special pseudo-category that shows all deductions (isDeduction=true)
  // IADE uses backend filtering via getInvoicesByCategory (backend has special IADE handling)
  const isKesinti = selectedCategory === "KESINTI";
  // IADE now uses shouldFetchByCategory - backend handles IADE filtering properly
  const shouldFetchByCategory = selectedCategory && selectedCategory !== "ALL" && !isKesinti;

  const {
    data: categoryInvoices,
    isLoading: categoryLoading,
  } = useInvoicesByCategory(
    storeId || undefined,
    shouldFetchByCategory ? selectedCategory : undefined,
    startDateStr,
    endDateStr,
    page,
    INVOICES_PER_PAGE,
    !!shouldFetchByCategory
  );

  const {
    data: allInvoicesData,
    isLoading: allInvoicesLoading,
  } = useAllInvoices(
    storeId || undefined,
    startDateStr,
    endDateStr,
    page,
    INVOICES_PER_PAGE,
    !shouldFetchByCategory // Fetch all when ALL or KESINTI selected
  );

  // Determine which data to use
  const rawInvoiceData = shouldFetchByCategory ? categoryInvoices : allInvoicesData;
  const invoicesLoading = shouldFetchByCategory ? categoryLoading : allInvoicesLoading;

  // Filter for KESINTI - show all deduction categories (isDeduction=true)
  // Note: IADE filtering is now handled by backend via useInvoicesByCategory
  const invoiceData = useMemo(() => {
    if (!rawInvoiceData) return rawInvoiceData;

    // Only KESINTI needs client-side filtering (IADE uses backend filtering)
    if (!isKesinti) return rawInvoiceData;

    // Filter for KESINTI: show deductions (isDeduction=true)
    const filteredContent = rawInvoiceData.content.filter((inv) => inv.isDeduction === true);

    return {
      ...rawInvoiceData,
      content: filteredContent,
      totalElements: filteredContent.length,
    };
  }, [rawInvoiceData, isKesinti]);

  // Accumulate invoices for lazy loading
  useEffect(() => {
    if (invoiceData?.content) {
      if (page === 0) {
        setAllInvoices(invoiceData.content);
      } else {
        setAllInvoices((prev) => {
          // Avoid duplicates
          const newIds = new Set(invoiceData.content.map((i) => i.id));
          const filtered = prev.filter((i) => !newIds.has(i.id));
          return [...filtered, ...invoiceData.content];
        });
      }
    }
  }, [invoiceData, page]);

  // Reset pagination when filters change
  const resetPagination = useCallback(() => {
    setPage(0);
    setAllInvoices([]);
  }, []);

  const handleCategorySelect = (category: CategoryKey | null) => {
    // Toggle behavior: clicking the same category again deselects it (returns to ALL)
    if (category === selectedCategory) {
      setSelectedCategory(null);
    } else {
      setSelectedCategory(category);
    }
    resetPagination();
  };

  const handlePresetChange = (preset: DatePreset) => {
    setDatePreset(preset);
    resetPagination();
    if (preset !== "custom") {
      setCalendarOpen(false);
    }
  };

  const handleInvoiceSelect = (invoice: InvoiceDetail) => {
    setSelectedInvoice(invoice);
    setDetailPanelOpen(true);
  };

  const handleLoadMore = () => {
    if (invoiceData && !invoiceData.last) {
      setPage((prev) => prev + 1);
    }
  };

  const hasMore = invoiceData ? !invoiceData.last : false;
  const totalElements = invoiceData?.totalElements || 0;

  const isLoading = storeLoading || summaryLoading;

  if (!storeId && !storeLoading) {
    return (
      <div className="p-8">
        <p className="text-muted-foreground">Lütfen önce bir mağaza seçin.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header - Date on left, filters on right */}
      <div className="flex items-center justify-between gap-4">
        {/* Date Range Display - Left */}
        <div className="text-sm font-medium text-foreground">
          {format(dateRange.start, "d MMMM yyyy", { locale: tr })} -{" "}
          {format(dateRange.end, "d MMMM yyyy", { locale: tr })}
        </div>

        {/* Date Range Selector + Export - Right */}
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 bg-muted rounded-lg p-1">
            {DATE_PRESETS.filter((p) => p.value !== "custom").map((preset) => (
              <Button
                key={preset.value}
                variant={datePreset === preset.value ? "secondary" : "ghost"}
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => handlePresetChange(preset.value)}
              >
                {preset.label}
              </Button>
            ))}
            <Popover open={calendarOpen} onOpenChange={setCalendarOpen}>
              <PopoverTrigger asChild>
                <Button
                  variant={datePreset === "custom" ? "secondary" : "ghost"}
                  size="sm"
                  className="h-7 px-2 text-xs gap-1"
                  onClick={() => {
                    setDatePreset("custom");
                    setCalendarOpen(true);
                  }}
                >
                  <CalendarIcon className="h-3 w-3" />
                  {datePreset === "custom" &&
                  customDateRange.start &&
                  customDateRange.end
                    ? `${format(customDateRange.start, "dd MMM", { locale: tr })} - ${format(customDateRange.end, "dd MMM", { locale: tr })}`
                    : "Özel"}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="end">
                <Calendar
                  mode="range"
                  selected={{
                    from: customDateRange.start,
                    to: customDateRange.end,
                  }}
                  onSelect={(range) => {
                    setCustomDateRange({
                      start: range?.from,
                      end: range?.to,
                    });
                    if (range?.from && range?.to) {
                      setCalendarOpen(false);
                      resetPagination();
                    }
                  }}
                  numberOfMonths={2}
                  locale="tr"
                />
              </PopoverContent>
            </Popover>
          </div>

          {/* Excel Export Button */}
          <InvoiceExportButton
            invoices={allInvoices}
            filename={`faturalar-${startDateStr}-${endDateStr}`}
          />
        </div>
      </div>

      {/* Error State */}
      {summaryError && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <p className="text-red-800 dark:text-red-200 text-sm">
            Faturalar yüklenirken hata: {summaryError.message}
          </p>
        </div>
      )}

      {/* Summary Cards (Dashboard style) */}
      <InvoiceSummaryCards
        summary={summary}
        selectedCategory={selectedCategory}
        onCategorySelect={handleCategorySelect}
        isLoading={isLoading}
      />

      {/* Invoice Table - Show category table for KARGO/KOMISYON, otherwise regular table */}
      {(summary || invoicesLoading) && (
        selectedCategory === "KARGO" || selectedCategory === "KOMISYON" ? (
          <InvoiceCategoryTable
            category={selectedCategory}
            invoices={allInvoices}
            isLoading={invoicesLoading && page === 0}
            totalElements={totalElements}
            onLoadMore={handleLoadMore}
            hasMore={hasMore}
            storeId={storeId!}
            startDate={startDateStr}
            endDate={endDateStr}
          />
        ) : (
          <InvoiceTable
            invoices={allInvoices}
            isLoading={invoicesLoading && page === 0}
            onInvoiceSelect={handleInvoiceSelect}
            totalElements={totalElements}
            onLoadMore={handleLoadMore}
            hasMore={hasMore}
            selectedCategory={selectedCategory}
          />
        )
      )}

      {/* Invoice Detail Panel (Slide-in) */}
      <InvoiceDetailPanel
        open={detailPanelOpen}
        onOpenChange={setDetailPanelOpen}
        invoice={selectedInvoice}
        storeId={storeId}
      />

      {/* Empty State */}
      {!isLoading && !summary && (
        <div className="bg-muted border border-border rounded-lg p-8 text-center">
          <FileText className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
          <h3 className="text-lg font-medium text-foreground mb-2">
            Henüz fatura verisi yok
          </h3>
          <p className="text-muted-foreground mb-4">
            Seçilen tarih aralığında fatura bulunamadı.
          </p>
        </div>
      )}
    </div>
  );
}
