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
  TrendingUp,
  TrendingDown,
  Minus,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
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
import { format, subDays } from "date-fns";
import { tr } from "date-fns/locale";

// Period options for profit page
export type ProfitPeriod = "today" | "yesterday" | "thisMonth" | "lastMonth" | "custom";

interface PeriodOption {
  id: ProfitPeriod;
  label: string;
}

const periodOptions: PeriodOption[] = [
  { id: "today", label: "Bugun" },
  { id: "yesterday", label: "Dun" },
  { id: "thisMonth", label: "Bu Ay" },
  { id: "lastMonth", label: "Gecen Ay" },
  { id: "custom", label: "Ozel Tarih" },
];

// Profit filter options
export type ProfitFilter = "all" | "profit" | "loss";

interface ProfitFilterOption {
  id: ProfitFilter;
  label: string;
  icon: React.ReactNode;
}

const profitFilterOptions: ProfitFilterOption[] = [
  { id: "all", label: "Tumunu Goster", icon: <Minus className="h-4 w-4" /> },
  { id: "profit", label: "Kar Edenler", icon: <TrendingUp className="h-4 w-4 text-green-600" /> },
  { id: "loss", label: "Zarar Edenler", icon: <TrendingDown className="h-4 w-4 text-red-600" /> },
];

export interface ProfitFiltersProps {
  // Period selection
  selectedPeriod: ProfitPeriod;
  onPeriodChange: (period: ProfitPeriod) => void;
  customDateRange?: { startDate: string; endDate: string };
  onCustomDateRangeChange?: (range: { startDate: string; endDate: string } | null) => void;

  // Profit/Loss filter
  profitFilter: ProfitFilter;
  onProfitFilterChange: (filter: ProfitFilter) => void;

  // Category filter
  selectedCategory: string | null;
  categories: string[];
  onCategoryChange: (category: string | null) => void;

  // Search
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

export function ProfitFilters({
  selectedPeriod,
  onPeriodChange,
  customDateRange,
  onCustomDateRangeChange,
  profitFilter,
  onProfitFilterChange,
  selectedCategory,
  categories,
  onCategoryChange,
  searchQuery,
  onSearchChange,
}: ProfitFiltersProps) {
  const [periodOpen, setPeriodOpen] = useState(false);
  const [categoryOpen, setCategoryOpen] = useState(false);

  // Custom date picker state
  const [customDateOpen, setCustomDateOpen] = useState(false);
  const [customStartDate, setCustomStartDate] = useState<Date | undefined>(
    customDateRange ? new Date(customDateRange.startDate) : undefined
  );
  const [customEndDate, setCustomEndDate] = useState<Date | undefined>(
    customDateRange ? new Date(customDateRange.endDate) : undefined
  );
  const [datePickerError, setDatePickerError] = useState<string | null>(null);

  // Handle period selection
  const handlePeriodSelect = (period: ProfitPeriod) => {
    if (period === "custom") {
      setCustomDateOpen(true);
    } else {
      onPeriodChange(period);
      onCustomDateRangeChange?.(null);
    }
    setPeriodOpen(false);
  };

  // Handle custom date range apply
  const handleCustomDateApply = () => {
    setDatePickerError(null);

    if (!customStartDate || !customEndDate) {
      setDatePickerError("Lutfen baslangic ve bitis tarihlerini secin");
      return;
    }

    if (customStartDate > customEndDate) {
      setDatePickerError("Baslangic tarihi bitis tarihinden sonra olamaz");
      return;
    }

    const daysDiff = Math.ceil((customEndDate.getTime() - customStartDate.getTime()) / (1000 * 60 * 60 * 24));
    if (daysDiff > 365) {
      setDatePickerError("Maksimum 365 gunluk aralik secilebilir");
      return;
    }

    onPeriodChange("custom");
    onCustomDateRangeChange?.({
      startDate: format(customStartDate, "yyyy-MM-dd"),
      endDate: format(customEndDate, "yyyy-MM-dd"),
    });
    setCustomDateOpen(false);
  };

  // Get current period label
  const getCurrentPeriodLabel = () => {
    if (selectedPeriod === "custom" && customDateRange) {
      const start = new Date(customDateRange.startDate);
      const end = new Date(customDateRange.endDate);
      return `${format(start, "dd MMM", { locale: tr })} - ${format(end, "dd MMM", { locale: tr })}`;
    }
    return periodOptions.find((p) => p.id === selectedPeriod)?.label || "Donem";
  };

  // Get profit filter label
  const getProfitFilterLabel = () => {
    return profitFilterOptions.find((f) => f.id === profitFilter)?.label || "Tumunu Goster";
  };

  return (
    <div className="flex flex-wrap items-center gap-3">
      {/* Search Input */}
      <div className="relative flex-1 min-w-[200px] max-w-[300px]">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          type="text"
          placeholder="Urun ara (isim veya barkod)"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-10"
        />
        {searchQuery && (
          <button
            onClick={() => onSearchChange("")}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* Period Dropdown */}
      <DropdownMenu open={periodOpen} onOpenChange={setPeriodOpen}>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            className={cn(
              "gap-2 min-w-[140px]",
              periodOpen && "bg-muted"
            )}
          >
            <CalendarIcon className="h-4 w-4" />
            <span className="truncate">{getCurrentPeriodLabel()}</span>
            {periodOpen ? (
              <ChevronUp className="h-4 w-4" />
            ) : (
              <ChevronDown className="h-4 w-4" />
            )}
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="w-[180px]">
          {periodOptions.map((option) => (
            <DropdownMenuItem
              key={option.id}
              onClick={() => handlePeriodSelect(option.id)}
              className="flex items-center gap-2"
            >
              {selectedPeriod === option.id ? (
                <Check className="h-4 w-4 text-[#1D70F1]" />
              ) : (
                <div className="w-4" />
              )}
              <span className={cn(selectedPeriod === option.id && "text-[#1D70F1] font-medium")}>
                {option.label}
              </span>
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Custom Date Range Dialog */}
      <Dialog open={customDateOpen} onOpenChange={setCustomDateOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Ozel Tarih Araligi</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="flex gap-4">
              {/* Start Date */}
              <div className="space-y-2 flex-1">
                <label className="text-xs text-muted-foreground">Baslangic</label>
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
                        <span>Tarih sec</span>
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
                <label className="text-xs text-muted-foreground">Bitis</label>
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
                        <span>Tarih sec</span>
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
              Iptal
            </Button>
            <Button onClick={handleCustomDateApply}>Uygula</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Profit/Loss Filter Toggle */}
      <div className="flex items-center rounded-lg border border-border p-1 gap-1">
        {profitFilterOptions.map((option) => (
          <button
            key={option.id}
            onClick={() => onProfitFilterChange(option.id)}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors",
              profitFilter === option.id
                ? "bg-[#1D70F1] text-white"
                : "text-muted-foreground hover:bg-muted"
            )}
          >
            {option.icon}
            <span className="hidden sm:inline">{option.label}</span>
          </button>
        ))}
      </div>

      {/* Category Dropdown */}
      {categories.length > 0 && (
        <DropdownMenu open={categoryOpen} onOpenChange={setCategoryOpen}>
          <DropdownMenuTrigger asChild>
            <Button
              variant="outline"
              className={cn(
                "gap-2 min-w-[120px]",
                categoryOpen && "bg-muted"
              )}
            >
              <span className="truncate">
                {selectedCategory || "Tum Kategoriler"}
              </span>
              {categoryOpen ? (
                <ChevronUp className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="w-[200px] max-h-[300px] overflow-y-auto">
            <DropdownMenuItem
              onClick={() => {
                onCategoryChange(null);
                setCategoryOpen(false);
              }}
              className="flex items-center gap-2"
            >
              {selectedCategory === null ? (
                <Check className="h-4 w-4 text-[#1D70F1]" />
              ) : (
                <div className="w-4" />
              )}
              <span className={cn(selectedCategory === null && "text-[#1D70F1] font-medium")}>
                Tum Kategoriler
              </span>
            </DropdownMenuItem>
            {categories.map((category) => (
              <DropdownMenuItem
                key={category}
                onClick={() => {
                  onCategoryChange(category);
                  setCategoryOpen(false);
                }}
                className="flex items-center gap-2"
              >
                {selectedCategory === category ? (
                  <Check className="h-4 w-4 text-[#1D70F1]" />
                ) : (
                  <div className="w-4" />
                )}
                <span className={cn(selectedCategory === category && "text-[#1D70F1] font-medium")}>
                  {category}
                </span>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </div>
  );
}
