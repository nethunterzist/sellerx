"use client";

import * as React from "react";
import { CalendarIcon } from "lucide-react";
import { format, subDays, startOfMonth, endOfMonth, subMonths } from "date-fns";
import { tr } from "date-fns/locale";
import type { DateRange } from "react-day-picker";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

export type DateRangePreset =
  | "today"
  | "yesterday"
  | "last7days"
  | "last30days"
  | "thisMonth"
  | "lastMonth"
  | "custom";

interface DateRangePickerProps {
  dateRange: DateRange | undefined;
  onDateRangeChange: (range: DateRange | undefined, preset: DateRangePreset) => void;
  className?: string;
}

const presets: { label: string; value: DateRangePreset }[] = [
  { label: "Bugun", value: "today" },
  { label: "Dun", value: "yesterday" },
  { label: "Son 7 Gun", value: "last7days" },
  { label: "Son 30 Gun", value: "last30days" },
  { label: "Bu Ay", value: "thisMonth" },
  { label: "Gecen Ay", value: "lastMonth" },
];

function getPresetRange(preset: DateRangePreset): DateRange {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  switch (preset) {
    case "today":
      return { from: today, to: today };
    case "yesterday":
      const yesterday = subDays(today, 1);
      return { from: yesterday, to: yesterday };
    case "last7days":
      return { from: subDays(today, 6), to: today };
    case "last30days":
      return { from: subDays(today, 29), to: today };
    case "thisMonth":
      return { from: startOfMonth(today), to: today };
    case "lastMonth":
      const lastMonth = subMonths(today, 1);
      return { from: startOfMonth(lastMonth), to: endOfMonth(lastMonth) };
    default:
      return { from: subDays(today, 29), to: today };
  }
}

export function DateRangePicker({
  dateRange,
  onDateRangeChange,
  className,
}: DateRangePickerProps) {
  const [isOpen, setIsOpen] = React.useState(false);
  const [selectedPreset, setSelectedPreset] = React.useState<DateRangePreset>("last30days");

  const handlePresetClick = (preset: DateRangePreset) => {
    const range = getPresetRange(preset);
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
      return "Tarih secin";
    }

    if (selectedPreset !== "custom") {
      const preset = presets.find((p) => p.value === selectedPreset);
      return preset?.label || "Tarih secin";
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
              Hizli Secim
            </p>
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

// Export helper function for external use
export { getPresetRange };
