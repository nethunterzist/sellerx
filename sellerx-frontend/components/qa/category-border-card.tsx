"use client";

import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

export type CategoryType = "shipping" | "returns" | "payment" | "product" | "pricing" | "technical";

interface CategoryBorderCardProps {
  category: CategoryType;
  children: ReactNode;
  className?: string;
}

const categoryColors = {
  shipping: "border-l-blue-500 hover:bg-blue-50/50 dark:hover:bg-blue-950/10",
  returns: "border-l-orange-500 hover:bg-orange-50/50 dark:hover:bg-orange-950/10",
  payment: "border-l-green-500 hover:bg-green-50/50 dark:hover:bg-green-950/10",
  product: "border-l-purple-500 hover:bg-purple-50/50 dark:hover:bg-purple-950/10",
  pricing: "border-l-yellow-500 hover:bg-yellow-50/50 dark:hover:bg-yellow-950/10",
  technical: "border-l-cyan-500 hover:bg-cyan-50/50 dark:hover:bg-cyan-950/10",
};

export function CategoryBorderCard({
  category,
  children,
  className,
}: CategoryBorderCardProps) {
  return (
    <Card
      className={cn(
        "border-l-4 transition-all duration-200",
        "hover:shadow-md hover:-translate-y-0.5",
        categoryColors[category],
        className
      )}
    >
      {children}
    </Card>
  );
}
