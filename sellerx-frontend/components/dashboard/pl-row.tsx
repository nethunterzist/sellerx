"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { ChevronRight, ChevronDown } from "lucide-react";
import { TableRow, TableCell } from "@/components/ui/table";
import { useCurrency } from "@/lib/contexts/currency-context";

interface PLRowProps {
  label: string;
  values: (number | string)[];
  isNegative?: boolean;
  isBold?: boolean;
  isExpandable?: boolean;
  children?: React.ReactNode;
  isCurrency?: boolean;
}

export function PLRow({
  label,
  values,
  isNegative,
  isBold,
  isExpandable,
  children,
  isCurrency = true,
}: PLRowProps) {
  const [isOpen, setIsOpen] = useState(false);
  const { formatCurrency } = useCurrency();

  const formatValue = (value: number | string, isCurrencyVal: boolean, showNegative?: boolean): string => {
    if (typeof value === "string") return value;

    if (isCurrencyVal) {
      const absValue = Math.abs(value);
      const formatted = formatCurrency(absValue);
      // For negative display, prepend minus sign
      if (showNegative && value !== 0) {
        return `-${formatted}`;
      }
      return formatted;
    }

    return value.toLocaleString("tr-TR");
  };

  return (
    <>
      <TableRow
        className={cn(
          "hover:bg-muted/50 transition-colors",
          isExpandable && "cursor-pointer",
          isBold && "bg-muted/50"
        )}
        onClick={() => isExpandable && setIsOpen(!isOpen)}
      >
        <TableCell className="font-medium text-foreground">
          <div className="flex items-center gap-2">
            {isExpandable && (
              isOpen ? (
                <ChevronDown className="h-4 w-4 text-muted-foreground" />
              ) : (
                <ChevronRight className="h-4 w-4 text-muted-foreground" />
              )
            )}
            {!isExpandable && <div className="w-4" />}
            <span className={cn(isBold && "font-semibold")}>{label}</span>
          </div>
        </TableCell>
        {values.map((value, index) => (
          <TableCell
            key={index}
            className={cn(
              "text-right",
              isBold && "font-semibold",
              typeof value === "number" && value < 0 && "text-red-600",
              isNegative && typeof value === "number" && value !== 0 && "text-red-600"
            )}
          >
            {formatValue(value, isCurrency, isNegative && typeof value === "number" && value > 0)}
          </TableCell>
        ))}
      </TableRow>
      {isExpandable && isOpen && children}
    </>
  );
}

interface PLSubRowProps {
  label: string;
  values: (number | string)[];
  isNegative?: boolean;
  isCurrency?: boolean;
}

export function PLSubRow({ label, values, isNegative, isCurrency = true }: PLSubRowProps) {
  const { formatCurrency } = useCurrency();

  const formatValue = (value: number | string, isCurrencyVal: boolean, showNegative?: boolean): string => {
    if (typeof value === "string") return value;

    if (isCurrencyVal) {
      const absValue = Math.abs(value);
      const formatted = formatCurrency(absValue);
      if (showNegative && value !== 0) {
        return `-${formatted}`;
      }
      return formatted;
    }

    return value.toLocaleString("tr-TR");
  };

  return (
    <TableRow className="bg-muted/30 hover:bg-muted/50 transition-colors">
      <TableCell className="pl-10 text-muted-foreground text-sm">
        <div className="flex items-center gap-2">
          <span className="text-muted-foreground">└</span>
          {label}
        </div>
      </TableCell>
      {values.map((value, index) => (
        <TableCell
          key={index}
          className={cn(
            "text-right text-sm",
            typeof value === "number" && value < 0 && "text-red-600",
            isNegative && typeof value === "number" && value !== 0 && "text-red-600"
          )}
        >
          {formatValue(value, isCurrency, isNegative && typeof value === "number" && value > 0)}
        </TableCell>
      ))}
    </TableRow>
  );
}
