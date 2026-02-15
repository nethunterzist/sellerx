"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import {
  Search,
  Calendar as CalendarIcon,
  ChevronDown,
  ChevronUp,
  Check,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Calendar } from "@/components/ui/calendar";
import { format, subDays, startOfMonth, endOfMonth, startOfQuarter, endOfQuarter, subMonths, subQuarters } from "date-fns";
import { tr } from "date-fns/locale";
import type { DateRangePreset, ViewFilterConfig } from "@/types/dashboard";
// Period preset options with date calculation capability
interface PeriodOption {
  id: DateRangePreset;
  label: string;
  description?: string;
}

const periodOptions: PeriodOption[] = [
  { id: "default", label: "Bugün / Dün / Bu Ay / Geçen Ay", description: "4 kart görünümü" },
  { id: "last7days", label: "Son 7 Gün" },
  { id: "last14days", label: "Son 14 Gün" },
  { id: "last30days", label: "Son 30 Gün" },
  { id: "thisQuarter", label: "Bu Çeyrek" },
  { id: "lastQuarter", label: "Geçen Çeyrek" },
  { id: "custom", label: "Özel Aralık", description: "Tarih seçin" },
];

// Calculate date range for preset
function calculateDateRange(preset: DateRangePreset): { startDate: Date; endDate: Date; label: string } | null {
  const today = new Date();
  today.setHours(23, 59, 59, 999);

  switch (preset) {
    case "default":
      return null; // Uses 4-card mode, no custom date range
    case "last7days":
      return {
        startDate: subDays(today, 6),
        endDate: today,
        label: "Son 7 Gün",
      };
    case "last14days":
      return {
        startDate: subDays(today, 13),
        endDate: today,
        label: "Son 14 Gün",
      };
    case "last30days":
      return {
        startDate: subDays(today, 29),
        endDate: today,
        label: "Son 30 Gün",
      };
    case "thisQuarter":
      return {
        startDate: startOfQuarter(today),
        endDate: today,
        label: "Bu Çeyrek",
      };
    case "lastQuarter": {
      const lastQ = subQuarters(today, 1);
      return {
        startDate: startOfQuarter(lastQ),
        endDate: endOfQuarter(lastQ),
        label: "Geçen Çeyrek",
      };
    }
    case "custom":
      return null; // Will be handled by custom date picker
    default:
      return null;
  }
}

// Legacy period groups for backward compatibility
const periodGroups = [
  {
    id: "default",
    label: "Bugün / Dün / Bu Ay / Bu Ay (tahmin) / Geçen Ay",
    periods: ["today", "yesterday", "mtd", "mtdForecast", "lastMonth"],
  },
  {
    id: "basic",
    label: "Bugün / Dün / Bu Ay / Geçen Ay",
    periods: ["today", "yesterday", "mtd", "lastMonth"],
  },
  {
    id: "days",
    label: "Bugün / Dün / 7 gün / 14 gün / 30 gün",
    periods: ["today", "yesterday", "7days", "14days", "30days"],
  },
  {
    id: "weeks",
    label: "Bu Hafta / Geçen Hafta / 2 Hafta Önce / 3 Hafta Önce",
    periods: ["thisWeek", "lastWeek", "2weeksAgo", "3weeksAgo"],
  },
  {
    id: "months",
    label: "Bu Ay / Geçen Ay / 2 Ay Önce / 3 Ay Önce",
    periods: ["mtd", "lastMonth", "2monthsAgo", "3monthsAgo"],
  },
  {
    id: "daysAgo",
    label: "Bugün / Dün / 2 Gün Önce / 3 Gün Önce",
    periods: ["today", "yesterday", "2daysAgo", "3daysAgo"],
  },
  {
    id: "weekDays",
    label: "Bugün / Dün / 7 Gün Önce / 8 Gün Önce",
    periods: ["today", "yesterday", "7daysAgo", "8daysAgo"],
  },
  {
    id: "quarters",
    label: "Bu Çeyrek / Geçen Çeyrek / 2 Çeyrek Önce / 3 Çeyrek Önce",
    periods: ["thisQuarter", "lastQuarter", "2quartersAgo", "3quartersAgo"],
  },
  {
    id: "custom",
    label: "Özel Aralık",
    periods: ["custom"],
  },
];

const currencyOptions = [
  { id: "TRY", label: "TL", symbol: "₺" },
  { id: "USD", label: "USD", symbol: "$" },
  { id: "EUR", label: "EUR", symbol: "€" },
];

export interface ProductItem {
  id: string;
  name: string;
  sku: string;
  barcode?: string;
  image?: string;
}

interface DashboardFiltersProps {
  products?: ProductItem[];
  selectedProducts?: string[];
  onProductsChange?: (productIds: string[]) => void;
  selectedPeriodGroup?: string;
  onPeriodGroupChange?: (groupId: string) => void;
  /** New callback for date range changes. Called when preset or custom date is selected. */
  onDateRangeChange?: (startDate: Date, endDate: Date, label: string) => void;
  /** Called when "default" preset is selected (4-card view) */
  onDefaultViewChange?: () => void;
  selectedCurrency?: string;
  onCurrencyChange?: (currencyId: string) => void;
  /** Filter configuration - controls which filters are shown for current view */
  filterConfig?: ViewFilterConfig;
}

// Default filter config - show all filters
const DEFAULT_FILTER_CONFIG: ViewFilterConfig = {
  usesProducts: true,
  usesDateRange: true,
  usesCurrency: true,
};

export function DashboardFilters({
  products = [],
  selectedProducts = [],
  onProductsChange,
  selectedPeriodGroup = "default",
  onPeriodGroupChange,
  onDateRangeChange,
  onDefaultViewChange,
  selectedCurrency = "TRY",
  onCurrencyChange,
  filterConfig = DEFAULT_FILTER_CONFIG,
}: DashboardFiltersProps) {
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [periodOpen, setPeriodOpen] = useState(false);

  // Custom date picker state
  const [customDateOpen, setCustomDateOpen] = useState(false);
  const [customStartDate, setCustomStartDate] = useState<Date | undefined>(undefined);
  const [customEndDate, setCustomEndDate] = useState<Date | undefined>(undefined);
  const [datePickerError, setDatePickerError] = useState<string | null>(null);

  // Multi-period presets that use parallel API calls (4-card dynamic view)
  const multiPeriodPresets: DateRangePreset[] = ["basic", "days", "weeks", "months", "daysAgo", "weekDays", "quarters"];

  // Handle period selection
  const handlePeriodSelect = (preset: DateRangePreset) => {
    // Legacy callback for backward compatibility
    onPeriodGroupChange?.(preset);

    if (preset === "default") {
      // Switch to 4-card view
      onDefaultViewChange?.();
      setCustomStartDate(undefined);
      setCustomEndDate(undefined);
    } else if (preset === "custom") {
      // Open custom date picker
      setCustomDateOpen(true);
    } else if (multiPeriodPresets.includes(preset)) {
      // Multi-period presets: clear custom date range and use parallel API calls
      // The useDashboardStatsByPreset hook will handle the API calls
      onDefaultViewChange?.();
      setCustomStartDate(undefined);
      setCustomEndDate(undefined);
    } else {
      // Single-card presets: calculate and apply preset date range
      const range = calculateDateRange(preset);
      if (range) {
        onDateRangeChange?.(range.startDate, range.endDate, range.label);
        setCustomStartDate(undefined);
        setCustomEndDate(undefined);
      }
    }
    setPeriodOpen(false);
  };

  // Handle custom date range apply
  const handleCustomDateApply = () => {
    setDatePickerError(null);

    if (!customStartDate || !customEndDate) {
      setDatePickerError("Lütfen başlangıç ve bitiş tarihlerini seçin");
      return;
    }

    if (customStartDate > customEndDate) {
      setDatePickerError("Başlangıç tarihi bitiş tarihinden sonra olamaz");
      return;
    }

    const daysDiff = Math.ceil((customEndDate.getTime() - customStartDate.getTime()) / (1000 * 60 * 60 * 24));
    if (daysDiff > 365) {
      setDatePickerError("Maksimum 365 günlük aralık seçilebilir");
      return;
    }

    const label = `${format(customStartDate, "dd MMM", { locale: tr })} - ${format(customEndDate, "dd MMM yyyy", { locale: tr })}`;
    onDateRangeChange?.(customStartDate, customEndDate, label);
    setCustomDateOpen(false);
  };

  // Get current period label for display
  const getCurrentPeriodLabel = () => {
    const option = periodOptions.find(o => o.id === selectedPeriodGroup);
    return option?.label || "Dönem";
  };

  // Defensive guards: ensure arrays are always arrays (handles null case)
  const safeProducts = Array.isArray(products) ? products : [];
  const safeSelectedProducts = Array.isArray(selectedProducts) ? selectedProducts : [];

  // Filter products based on search query
  const filteredProducts = safeProducts.filter(
    (p) =>
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.barcode?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleProductToggle = (productId: string) => {
    const newSelected = safeSelectedProducts.includes(productId)
      ? safeSelectedProducts.filter((id) => id !== productId)
      : [...safeSelectedProducts, productId];
    onProductsChange?.(newSelected);
  };

  const handleSelectAll = () => {
    if (safeSelectedProducts.length === filteredProducts.length) {
      onProductsChange?.([]);
    } else {
      onProductsChange?.(filteredProducts.map((p) => p.id));
    }
  };

  const selectedCurrencyLabel =
    currencyOptions.find((c) => c.id === selectedCurrency)?.label || "TL";

  // Get selected product objects for badge display
  const selectedProductObjects = safeSelectedProducts
    .map(id => safeProducts.find(p => p.id === id))
    .filter(Boolean) as ProductItem[];

  // Remove a product from selection
  const handleRemoveProduct = (productId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    onProductsChange?.(safeSelectedProducts.filter(id => id !== productId));
  };

  return (
    <div className="flex items-center gap-4">
      {/* Search Bar with Product Dropdown */}
      {filterConfig.usesProducts ? (
        <Popover open={searchOpen} onOpenChange={setSearchOpen}>
          <PopoverTrigger asChild>
            <div className="flex-1 min-h-10 bg-gray-200 dark:bg-gray-800 rounded-md flex items-center gap-2 px-3 cursor-pointer hover:bg-gray-300 dark:hover:bg-gray-700 transition-colors">
              <Search className="h-4 w-4 text-muted-foreground flex-shrink-0" />
              {selectedProductObjects.length > 0 ? (
                <div className="flex items-center gap-1.5 flex-wrap flex-1 py-1.5">
                  {/* İlk 2 badge göster */}
                  {selectedProductObjects.slice(0, 2).map((product) => (
                    <span
                      key={product.id}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-[#1D70F1]/10 text-[#1D70F1] text-xs font-medium dark:bg-[#1D70F1]/20"
                    >
                      <span className="max-w-[120px] truncate">{product.name}</span>
                      <button
                        onClick={(e) => handleRemoveProduct(product.id, e)}
                        className="hover:bg-[#1D70F1]/20 rounded-full p-0.5 transition-colors"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </span>
                  ))}
                  {/* Kalan varsa "+N" göster */}
                  {selectedProductObjects.length > 2 && (
                    <span className="text-xs text-muted-foreground font-medium px-1">
                      +{selectedProductObjects.length - 2}
                    </span>
                  )}
                </div>
              ) : (
                <span className="text-muted-foreground text-sm">Ürün ara...</span>
              )}
            </div>
          </PopoverTrigger>
          <PopoverContent className="w-[var(--radix-popover-trigger-width)] min-w-[400px] p-0" align="start">
            {/* Search Input inside Popover */}
            <div className="p-3 border-b border-border">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  type="text"
                  placeholder="Ürün ara..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                  autoFocus
                />
              </div>
            </div>
            {/* Product List */}
            <ScrollArea className="h-[300px]">
              <div className="p-2">
                {filteredProducts.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-4">
                    Ürün bulunamadı
                  </p>
                ) : (
                  filteredProducts.map((product) => {
                    const isSelected = safeSelectedProducts.includes(product.id);
                    return (
                      <div
                        key={product.id}
                        className={cn(
                          "flex items-center gap-3 p-2 rounded-md cursor-pointer hover:bg-muted",
                          isSelected && "bg-blue-50 dark:bg-blue-900/20"
                        )}
                        onClick={() => {
                          if (isSelected) {
                            onProductsChange?.(safeSelectedProducts.filter(id => id !== product.id));
                          } else {
                            onProductsChange?.([...safeSelectedProducts, product.id]);
                          }
                        }}
                      >
                        <Checkbox
                          checked={isSelected}
                          className="pointer-events-none"
                        />
                        {product.image && (
                          <img
                            src={product.image}
                            alt={product.name}
                            className="w-8 h-8 rounded object-cover"
                          />
                        )}
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium truncate">{product.name}</p>
                          <p className="text-xs text-muted-foreground">{product.sku}</p>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </ScrollArea>
            {/* Footer with selection info */}
            <div className="p-3 border-t border-border flex items-center justify-between">
              <span className="text-sm text-muted-foreground">
                {safeSelectedProducts.length} ürün seçili
              </span>
              <div className="flex gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onProductsChange?.([])}
                >
                  Temizle
                </Button>
                <Button
                  size="sm"
                  onClick={() => setSearchOpen(false)}
                >
                  Tamam
                </Button>
              </div>
            </div>
          </PopoverContent>
        </Popover>
      ) : (
        <div className="flex-1" />
      )}

      {/* Right Side Filters */}
      <div className="flex items-center gap-2">
        {/* Period Dropdown - only show if view uses date range */}
        {filterConfig.usesDateRange && (
        <DropdownMenu open={periodOpen} onOpenChange={setPeriodOpen}>
          <DropdownMenuTrigger asChild>
            <Button
              variant="outline"
              className={cn(
                "gap-2 bg-background min-w-[120px]",
                periodOpen && "bg-muted"
              )}
            >
              <CalendarIcon className="h-4 w-4" />
              <span className="hidden sm:inline truncate max-w-[150px]">
                {selectedPeriodGroup === "default" ? "Dönem" : getCurrentPeriodLabel()}
              </span>
              {periodOpen ? (
                <ChevronUp className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent className="w-[420px]" align="start">
            <div className="p-1">
              {periodGroups.map((group) => (
                <DropdownMenuItem
                  key={group.id}
                  onClick={() => handlePeriodSelect(group.id as DateRangePreset)}
                  className="flex items-center gap-2 py-2.5"
                >
                  {selectedPeriodGroup === group.id ? (
                    <Check className="h-4 w-4 text-teal-600 flex-shrink-0" />
                  ) : (
                    <div className="w-4 flex-shrink-0" />
                  )}
                  <span
                    className={cn(
                      "text-sm",
                      selectedPeriodGroup === group.id && "text-teal-600 font-medium"
                    )}
                  >
                    {group.label}
                  </span>
                </DropdownMenuItem>
              ))}
            </div>
          </DropdownMenuContent>
        </DropdownMenu>
        )}

        {/* Custom Date Range Picker Dialog */}
        <Dialog open={customDateOpen} onOpenChange={setCustomDateOpen}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Özel Tarih Aralığı</DialogTitle>
            </DialogHeader>

            <div className="space-y-4 py-4">
              <div className="flex gap-4">
                {/* Start Date */}
                <div className="space-y-2 flex-1">
                  <label className="text-xs text-muted-foreground">Başlangıç</label>
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button
                        variant="outline"
                        className={cn(
                          "w-full justify-start text-left font-normal",
                          !customStartDate && "text-muted-foreground"
                        )}
                      >
                        <CalendarIcon className="mr-2 h-4 w-4" />
                        {customStartDate ? (
                          format(customStartDate, "dd MMM yyyy", { locale: tr })
                        ) : (
                          <span>Tarih seç</span>
                        )}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <Calendar
                        mode="single"
                        selected={customStartDate}
                        onSelect={setCustomStartDate}
                        disabled={(date) => date > new Date()}
                        locale="tr"
                      />
                    </PopoverContent>
                  </Popover>
                </div>

                {/* End Date */}
                <div className="space-y-2 flex-1">
                  <label className="text-xs text-muted-foreground">Bitiş</label>
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button
                        variant="outline"
                        className={cn(
                          "w-full justify-start text-left font-normal",
                          !customEndDate && "text-muted-foreground"
                        )}
                      >
                        <CalendarIcon className="mr-2 h-4 w-4" />
                        {customEndDate ? (
                          format(customEndDate, "dd MMM yyyy", { locale: tr })
                        ) : (
                          <span>Tarih seç</span>
                        )}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <Calendar
                        mode="single"
                        selected={customEndDate}
                        onSelect={setCustomEndDate}
                        disabled={(date) => date > new Date()}
                        locale="tr"
                      />
                    </PopoverContent>
                  </Popover>
                </div>
              </div>

              {datePickerError && (
                <p className="text-xs text-destructive">{datePickerError}</p>
              )}
            </div>

            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => {
                  setCustomDateOpen(false);
                  setDatePickerError(null);
                }}
              >
                İptal
              </Button>
              <Button onClick={handleCustomDateApply}>
                Uygula
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

      {/* Currency Dropdown - only show if view uses currency */}
      {filterConfig.usesCurrency && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" className="gap-2 bg-background min-w-[80px]">
              <span>{selectedCurrencyLabel}</span>
              <ChevronDown className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            {currencyOptions.map((option) => (
              <DropdownMenuItem
                key={option.id}
                onClick={() => onCurrencyChange?.(option.id)}
                className="flex items-center gap-2"
              >
                {selectedCurrency === option.id ? (
                  <Check className="h-4 w-4 text-teal-600" />
                ) : (
                  <div className="w-4" />
                )}
                <span
                  className={cn(selectedCurrency === option.id && "text-teal-600")}
                >
                  {option.label}
                </span>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
      </div>
    </div>
  );
}
