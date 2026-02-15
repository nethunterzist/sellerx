"use client";

import * as React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { DayPicker, type DayPickerProps } from "react-day-picker";
import { tr, enUS } from "date-fns/locale";

import { cn } from "@/lib/utils";
import { buttonVariants } from "@/components/ui/button";

// Use a permissive type to avoid issues with DayPicker v9's discriminated union.
// DayPickerProps is a complex union that breaks when Omit is applied.
type CalendarBaseProps = {
  locale?: "tr" | "en";
  className?: string;
  classNames?: Record<string, string>;
  showOutsideDays?: boolean;
};

export type CalendarProps = CalendarBaseProps &
  Omit<Record<string, unknown>, keyof CalendarBaseProps>;

function Calendar({
  className,
  classNames,
  showOutsideDays = true,
  locale = "tr",
  ...props
}: CalendarProps) {
  const dateLocale = locale === "tr" ? tr : enUS;

  return (
    <DayPicker
      showOutsideDays={showOutsideDays}
      className={cn("p-3", className)}
      locale={dateLocale}
      classNames={{
        months: "flex flex-col sm:flex-row space-y-4 sm:space-x-4 sm:space-y-0",
        month: "space-y-4 relative",
        month_caption: "flex justify-start items-center pt-1 relative",
        nav: "absolute right-0 top-4 flex items-center gap-1 z-10",
        button_previous: cn(
          buttonVariants({ variant: "outline" }),
          "h-7 w-7 bg-transparent p-0 opacity-50 hover:opacity-100"
        ),
        button_next: cn(
          buttonVariants({ variant: "outline" }),
          "h-7 w-7 bg-transparent p-0 opacity-50 hover:opacity-100"
        ),
        table: "w-full border-collapse space-y-1",
        weekdays: "flex w-full justify-around",
        weekday:
          "text-muted-foreground rounded-md w-9 font-normal text-[0.8rem] text-center",
        row: "flex w-full mt-2",
        cell: cn(
          "h-9 w-9 text-center text-sm p-0 relative focus-within:relative focus-within:z-20",
          // Range start: pill left rounded with gradient background
          "[&:has(.rdp-range-start)]:rounded-l-full [&:has(.rdp-range-start)]:bg-gradient-to-r [&:has(.rdp-range-start)]:from-transparent [&:has(.rdp-range-start)]:to-primary/10",
          // Range end: pill right rounded with gradient background
          "[&:has(.rdp-range-end)]:rounded-r-full [&:has(.rdp-range-end)]:bg-gradient-to-l [&:has(.rdp-range-end)]:from-transparent [&:has(.rdp-range-end)]:to-primary/10",
          // Range middle: light primary background
          "[&:has(.rdp-range-middle)]:bg-primary/10"
        ),
        day: cn(
          buttonVariants({ variant: "ghost" }),
          "h-9 w-9 p-0 font-normal aria-selected:opacity-100 transition-all duration-150",
          "hover:bg-primary/15 hover:text-primary"
        ),
        range_start: cn(
          "rdp-range-start bg-primary text-primary-foreground rounded-full font-semibold",
          "hover:bg-primary hover:text-primary-foreground",
          "focus:bg-primary focus:text-primary-foreground"
        ),
        range_end: cn(
          "rdp-range-end bg-primary text-primary-foreground rounded-full font-semibold",
          "hover:bg-primary hover:text-primary-foreground",
          "focus:bg-primary focus:text-primary-foreground"
        ),
        range_middle: "rdp-range-middle bg-primary/10 text-foreground",
        selected: cn(
          "bg-primary text-primary-foreground rounded-full font-semibold",
          "hover:bg-primary hover:text-primary-foreground",
          "focus:bg-primary focus:text-primary-foreground"
        ),
        day_today: "bg-accent text-accent-foreground font-semibold",
        day_outside: "day-outside text-muted-foreground/50 aria-selected:bg-primary/5 aria-selected:text-muted-foreground/70",
        day_disabled: "text-muted-foreground opacity-50",
        day_hidden: "invisible",
        ...(classNames || {}),
      }}
      components={{
        Chevron: ({ orientation }) =>
          orientation === "left" ? (
            <ChevronLeft className="h-4 w-4" />
          ) : (
            <ChevronRight className="h-4 w-4" />
          ),
      }}
      {...(props as DayPickerProps)}
    />
  );
}
Calendar.displayName = "Calendar";

export { Calendar };
