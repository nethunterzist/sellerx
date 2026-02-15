"use client";

import * as React from "react";
import { useState, useMemo, useCallback } from "react";
import { Calendar } from "@/components/ui/calendar";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Calendar as CalendarIcon, X, Check } from "lucide-react";
import { format, subDays, startOfMonth, endOfMonth, subMonths, startOfYear, endOfYear, subYears } from "date-fns";
import { tr, enUS } from "date-fns/locale";
import { cn } from "@/lib/utils";

// Default presets
const DEFAULT_PRESETS = [
  { label: "Son 7 Gün", value: "7d" },
  { label: "Son 30 Gün", value: "30d" },
  { label: "Son 90 Gün", value: "90d" },
  { label: "Bu Ay", value: "thisMonth" },
  { label: "Geçen Ay", value: "lastMonth" },
  { label: "Bu Yıl", value: "thisYear" },
  { label: "Geçen Yıl", value: "lastYear" },
] as const;

type PresetValue = (typeof DEFAULT_PRESETS)[number]["value"] | "custom";

export interface DateRangePickerPreset {
  label: string;
  value: string;
}

export interface DateRangePickerProps {
  value: { from: Date; to: Date } | undefined;
  onChange: (range: { from: Date; to: Date } | undefined) => void;
  presets?: DateRangePickerPreset[];
  defaultPreset?: string;
  locale?: "tr" | "en";
  placeholder?: string;
  className?: string;
  disabled?: boolean;
  align?: "start" | "center" | "end";
}

function getDateRangeFromPreset(preset: string): { from: Date; to: Date } {
  const today = new Date();
  const end = today;

  switch (preset) {
    case "all":
      return { from: subYears(today, 3), to: end };
    case "7d":
      return { from: subDays(today, 7), to: end };
    case "30d":
      return { from: subDays(today, 30), to: end };
    case "90d":
      return { from: subDays(today, 90), to: end };
    case "thisMonth":
      return { from: startOfMonth(today), to: end };
    case "lastMonth":
      const lastMonth = subMonths(today, 1);
      return { from: startOfMonth(lastMonth), to: endOfMonth(lastMonth) };
    case "thisYear":
      return { from: startOfYear(today), to: end };
    case "lastYear":
      const lastYear = subYears(today, 1);
      return { from: startOfYear(lastYear), to: endOfYear(lastYear) };
    default:
      return { from: subDays(today, 30), to: end };
  }
}

export function DateRangePicker({
  value,
  onChange,
  presets = DEFAULT_PRESETS as unknown as DateRangePickerPreset[],
  defaultPreset = "30d",
  locale = "tr",
  placeholder = "Tarih seçin",
  className,
  disabled = false,
  align = "end",
}: DateRangePickerProps) {
  const [open, setOpen] = useState(false);
  const [activePreset, setActivePreset] = useState<PresetValue | null>(defaultPreset as PresetValue);

  // Track if user is in custom picking mode (should not show old value)
  const [isCustomPicking, setIsCustomPicking] = useState(false);

  // Pending selection state (not yet applied)
  const [pendingRange, setPendingRange] = useState<{
    from: Date | undefined;
    to: Date | undefined;
  }>({ from: undefined, to: undefined });

  // Hover state for range preview
  const [hoveredDay, setHoveredDay] = useState<Date | undefined>(undefined);

  const dateLocale = locale === "tr" ? tr : enUS;

  // Calculate display range (what shows in trigger button)
  const displayRange = value;

  // Calculate selection phase
  const selectionPhase = useMemo(() => {
    if (!pendingRange.from) return "start";
    if (pendingRange.from && !pendingRange.to) {
      return "end";
    }
    return "complete";
  }, [pendingRange]);

  // Calculate visual range (what shows in calendar)
  const visualRange = useMemo(() => {
    // If selecting second date and hovering, show preview
    if (selectionPhase === "end" && hoveredDay && pendingRange.from) {
      const from = pendingRange.from;
      const to = hoveredDay;
      // Ensure from is before to
      if (from <= to) {
        return { from, to };
      }
      return { from: to, to: from };
    }

    // Show pending range if any
    if (pendingRange.from) {
      return {
        from: pendingRange.from,
        to: pendingRange.to, // undefined kalabilir - react-day-picker bunu anlar
      };
    }

    // If in custom picking mode and no pending range, show nothing (clear old selection)
    if (isCustomPicking) {
      return undefined;
    }

    // Fall back to current value
    return value ? { from: value.from, to: value.to } : undefined;
  }, [selectionPhase, hoveredDay, pendingRange, value, isCustomPicking]);

  // Reset pending state when popover opens/closes
  const handleOpenChange = useCallback((isOpen: boolean) => {
    setOpen(isOpen);
    if (!isOpen) {
      // Reset pending state when closing
      setPendingRange({ from: undefined, to: undefined });
      setHoveredDay(undefined);
      setIsCustomPicking(false);
    }
  }, []);

  // Handle preset click
  const handlePresetClick = useCallback((preset: string) => {
    const range = getDateRangeFromPreset(preset);
    setActivePreset(preset as PresetValue);
    setPendingRange({ from: range.from, to: range.to });
    setIsCustomPicking(false); // Exit custom picking mode
  }, []);

  // Handle calendar selection (react-day-picker v9 API)
  const handleSelect = useCallback((range: { from?: Date; to?: Date } | undefined) => {
    setHoveredDay(undefined);
    setActivePreset("custom");
    setIsCustomPicking(true); // We're now in custom mode

    if (!range) {
      setPendingRange({ from: undefined, to: undefined });
      return;
    }

    // react-day-picker v9: First click gives { from: Date, to: undefined }
    // Second click gives { from: Date, to: Date }
    if (range.from && !range.to) {
      // First click - just the start date (to must be undefined so react-day-picker waits for second click)
      setPendingRange({ from: range.from, to: undefined });
    } else if (range.from && range.to) {
      // Second click or complete range
      setPendingRange({ from: range.from, to: range.to });
    } else {
      setPendingRange({ from: undefined, to: undefined });
    }
  }, []);

  // Handle hover for preview
  const handleDayMouseEnter = useCallback((day: Date) => {
    if (selectionPhase === "end") {
      setHoveredDay(day);
    }
  }, [selectionPhase]);

  const handleDayMouseLeave = useCallback(() => {
    setHoveredDay(undefined);
  }, []);

  // Handle Apply button
  const handleApply = useCallback(() => {
    if (pendingRange.from && pendingRange.to) {
      onChange({ from: pendingRange.from, to: pendingRange.to });
      setOpen(false);
      setPendingRange({ from: undefined, to: undefined });
      setIsCustomPicking(false);
    }
  }, [pendingRange, onChange]);

  // Handle Clear button
  const handleClear = useCallback(() => {
    setPendingRange({ from: undefined, to: undefined });
    setActivePreset(null);
    setIsCustomPicking(true); // Keep in custom mode so old value doesn't show
  }, []);

  // Check if Apply is enabled
  const canApply = pendingRange.from && pendingRange.to;

  // Format display text for pending range
  const pendingDisplayText = useMemo(() => {
    if (!pendingRange.from) return null;
    if (!pendingRange.to) {
      return `${format(pendingRange.from, "d MMM yyyy", { locale: dateLocale })} - ?`;
    }
    return `${format(pendingRange.from, "d MMM yyyy", { locale: dateLocale })} - ${format(pendingRange.to, "d MMM yyyy", { locale: dateLocale })}`;
  }, [pendingRange, dateLocale]);

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          disabled={disabled}
          className={cn(
            "justify-start text-left font-normal h-9 px-3 gap-2",
            !displayRange && "text-muted-foreground",
            className
          )}
        >
          <CalendarIcon className="h-4 w-4 text-muted-foreground" />
          {displayRange ? (
            <span>
              {format(displayRange.from, "dd MMM", { locale: dateLocale })} -{" "}
              {format(displayRange.to, "dd MMM yyyy", { locale: dateLocale })}
            </span>
          ) : (
            <span>{placeholder}</span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent
        className="w-auto p-0 shadow-xl border-0 rounded-xl overflow-hidden"
        align={align}
        sideOffset={8}
      >
        <div className="flex">
          {/* Left Sidebar - Presets */}
          <div className="w-[150px] border-r bg-muted/30 p-3 space-y-1">
            <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3 px-2">
              Hızlı Seçim
            </div>
            {presets.map((preset) => (
              <button
                key={preset.value}
                type="button"
                onClick={() => handlePresetClick(preset.value)}
                className={cn(
                  "w-full text-left px-3 py-2 text-sm rounded-lg transition-colors",
                  activePreset === preset.value
                    ? "bg-primary/10 text-primary font-medium border-l-2 border-primary"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                )}
              >
                {preset.label}
              </button>
            ))}
            <div className="border-t border-border my-2" />
            <button
              type="button"
              onClick={() => {
                setActivePreset("custom");
                setPendingRange({ from: undefined, to: undefined });
                setIsCustomPicking(true); // Enter custom picking mode - clears old visual
              }}
              className={cn(
                "w-full text-left px-3 py-2 text-sm rounded-lg transition-colors",
                activePreset === "custom"
                  ? "bg-primary/10 text-primary font-medium border-l-2 border-primary"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              Özel Aralık
            </button>
          </div>

          {/* Right Side - Calendar */}
          <div className="flex flex-col">
            {/* Calendar */}
            <div className="p-4">
              <Calendar
                mode="range"
                selected={visualRange}
                onSelect={handleSelect}
                onDayMouseEnter={handleDayMouseEnter}
                onDayMouseLeave={handleDayMouseLeave}
                numberOfMonths={2}
                locale={locale}
                disabled={{ after: new Date() }}
                className="rounded-md"
              />
            </div>

            {/* Footer */}
            <div className="flex items-center justify-between px-4 py-3 border-t bg-muted/30">
              <div className="text-sm">
                {pendingDisplayText ? (
                  <span className="font-medium text-foreground">
                    {pendingDisplayText}
                  </span>
                ) : (
                  <span className="text-muted-foreground">
                    Tarih aralığı seçin
                  </span>
                )}
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleClear}
                  className="h-8 px-3 text-muted-foreground hover:text-foreground"
                >
                  <X className="h-4 w-4 mr-1" />
                  Temizle
                </Button>
                <Button
                  size="sm"
                  onClick={handleApply}
                  disabled={!canApply}
                  className="h-8 px-4"
                >
                  <Check className="h-4 w-4 mr-1" />
                  Uygula
                </Button>
              </div>
            </div>
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
