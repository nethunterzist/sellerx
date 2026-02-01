"use client";

import { cn } from "@/lib/utils";
import { LayoutGrid, LineChart, Table, TrendingUp } from "lucide-react";

export type DashboardViewType = "tiles" | "chart" | "pl" | "trends" | "cities";

interface DashboardTabsProps {
  activeView: DashboardViewType;
  onViewChange: (view: DashboardViewType) => void;
}

const tabs: { id: DashboardViewType; label: string; icon: typeof LayoutGrid }[] = [
  { id: "tiles", label: "Kartlar", icon: LayoutGrid },
  { id: "chart", label: "Grafik", icon: LineChart },
  { id: "pl", label: "Kar/Zarar", icon: Table },
  { id: "trends", label: "Trendler", icon: TrendingUp },
];

export function DashboardTabs({ activeView, onViewChange }: DashboardTabsProps) {
  return (
    <nav className="flex items-center gap-1 border-b border-border bg-card rounded-t-lg px-2">
      {tabs.map((tab) => {
        const Icon = tab.icon;
        return (
          <button
            key={tab.id}
            onClick={() => onViewChange(tab.id)}
            className={cn(
              "flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors border-b-2 -mb-[1px]",
              activeView === tab.id
                ? "border-[#1D70F1] text-[#1D70F1]"
                : "border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50"
            )}
          >
            <Icon className="h-4 w-4" />
            {tab.label}
          </button>
        );
      })}
    </nav>
  );
}
