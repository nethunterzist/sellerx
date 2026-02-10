"use client";

import * as React from "react";
import { CalendarIcon } from "lucide-react";
import {
  format,
  subDays,
  subMonths,
  startOfMonth,
  endOfMonth,
  startOfDay,
} from "date-fns";
import { tr } from "date-fns/locale";
import type { DateRange } from "react-day-picker";

// Note: tr from date-fns/locale is used for format() calls, not for Calendar

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

export type ExpenseDatePreset =
  | "today"
  | "yesterday"
  | "last7days"
  | "thisMonth"
  | "lastMonth"
  | "last3months"
  | "last12months"
  | "custom";

interface ExpenseDateFilterProps {
  dateRange: DateRange | undefined;
  onDateRangeChange: (
    range: DateRange | undefined,
    preset: ExpenseDatePreset | undefined
  ) => void;
  className?: string;
}

const presets: { label: string; value: ExpenseDatePreset }[] = [
  { label: "Bugün", value: "today" },
  { label: "Dün", value: "yesterday" },
  { label: "Son 7 Gün", value: "last7days" },
  { label: "Bu Ay", value: "thisMonth" },
  { label: "Geçen Ay", value: "lastMonth" },
  { label: "Son 3 Ay", value: "last3months" },
  { label: "Son 12 Ay", value: "last12months" },
];

export function getExpensePresetRange(preset: ExpenseDatePreset): DateRange {
  const today = startOfDay(new Date());

  switch (preset) {
    case "today":
      return { from: today, to: today };
    case "yesterday":
      const yesterday = subDays(today, 1);
      return { from: yesterday, to: yesterday };
    case "last7days":
      return { from: subDays(today, 6), to: today };
    case "thisMonth":
      return { from: startOfMonth(today), to: today };
    case "lastMonth":
      const lastMonth = subMonths(today, 1);
      return { from: startOfMonth(lastMonth), to: endOfMonth(lastMonth) };
    case "last3months":
      return { from: subMonths(today, 3), to: today };
    case "last12months":
      return { from: subMonths(today, 12), to: today };
    default:
      return { from: startOfMonth(today), to: today };
  }
}

export function ExpenseDateFilter({
  dateRange,
  onDateRangeChange,
  className,
}: ExpenseDateFilterProps) {
  const [isOpen, setIsOpen] = React.useState(false);
  const [selectedPreset, setSelectedPreset] =
    React.useState<ExpenseDatePreset | undefined>(undefined);

  const handleClearFilter = () => {
    setSelectedPreset(undefined);
    onDateRangeChange(undefined, undefined);
    setIsOpen(false);
  };

  const handlePresetClick = (preset: ExpenseDatePreset) => {
    const range = getExpensePresetRange(preset);
    setSelectedPreset(preset);
    onDateRangeChange(range, preset);
    setIsOpen(false);
  };

  const handleCalendarSelect = (range: DateRange | undefined) => {
    if (range) {
      setSelectedPreset("custom");
      onDateRangeChange(range, "custom");
    }
  };

  const displayText = React.useMemo(() => {
    if (!dateRange?.from) {
      return "Tüm Tarihler";
    }

    if (selectedPreset && selectedPreset !== "custom") {
      const preset = presets.find((p) => p.value === selectedPreset);
      return preset?.label || "Tüm Tarihler";
    }

    if (dateRange.to) {
      return `${format(dateRange.from, "dd MMM", { locale: tr })} - ${format(
        dateRange.to,
        "dd MMM yyyy",
        { locale: tr }
      )}`;
    }

    return format(dateRange.from, "dd MMM yyyy", { locale: tr });
  }, [dateRange, selectedPreset]);

  return (
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          className={cn(
            "justify-start text-left font-normal",
            !dateRange && "text-muted-foreground",
            className
          )}
        >
          <CalendarIcon className="mr-2 h-4 w-4" />
          {displayText}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="end">
        <div className="flex">
          {/* Preset Buttons */}
          <div className="border-r p-3 space-y-1">
            <p className="text-xs font-medium text-muted-foreground mb-2 px-2">
              Hızlı Seçim
            </p>
            <Button
              variant={!selectedPreset ? "secondary" : "ghost"}
              size="sm"
              className="w-full justify-start text-left"
              onClick={handleClearFilter}
            >
              Tümü
            </Button>
            {presets.map((preset) => (
              <Button
                key={preset.value}
                variant={selectedPreset === preset.value ? "secondary" : "ghost"}
                size="sm"
                className="w-full justify-start text-left"
                onClick={() => handlePresetClick(preset.value)}
              >
                {preset.label}
              </Button>
            ))}
          </div>

          {/* Calendar */}
          <div className="p-3">
            <Calendar
              mode="range"
              selected={dateRange}
              onSelect={handleCalendarSelect}
              numberOfMonths={1}
              locale="tr"
              disabled={{ after: new Date() }}
            />
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
