"use client";

import { useCallback, useRef } from "react";
import { motion } from "motion/react";
import { type LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

interface Tab<T extends string> {
  id: T;
  label: string;
  icon?: LucideIcon;
  badge?: number;
  badgeVariant?: "default" | "destructive" | "secondary";
}

interface SegmentedControlProps<T extends string> {
  tabs: Tab<T>[];
  activeTab: T;
  onTabChange: (tab: T) => void;
  ariaLabel?: string;
  className?: string;
}

export function SegmentedControl<T extends string>({
  tabs,
  activeTab,
  onTabChange,
  ariaLabel = "Navigation",
  className,
}: SegmentedControlProps<T>) {
  const tabListRef = useRef<HTMLDivElement>(null);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      const currentIndex = tabs.findIndex((t) => t.id === activeTab);
      let nextIndex = currentIndex;

      if (e.key === "ArrowRight" || e.key === "ArrowDown") {
        e.preventDefault();
        nextIndex = (currentIndex + 1) % tabs.length;
      } else if (e.key === "ArrowLeft" || e.key === "ArrowUp") {
        e.preventDefault();
        nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
      } else if (e.key === "Home") {
        e.preventDefault();
        nextIndex = 0;
      } else if (e.key === "End") {
        e.preventDefault();
        nextIndex = tabs.length - 1;
      }

      if (nextIndex !== currentIndex) {
        onTabChange(tabs[nextIndex].id);
        // Focus the newly active tab button
        const buttons = tabListRef.current?.querySelectorAll('[role="tab"]');
        (buttons?.[nextIndex] as HTMLElement)?.focus();
      }
    },
    [tabs, activeTab, onTabChange]
  );

  return (
    <nav className={cn("px-4 py-3", className)} aria-label={ariaLabel}>
      <div
        ref={tabListRef}
        role="tablist"
        className="flex items-center gap-2 overflow-x-auto"
        onKeyDown={handleKeyDown}
      >
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;

          return (
            <button
              key={tab.id}
              role="tab"
              aria-selected={isActive}
              tabIndex={isActive ? 0 : -1}
              onClick={() => onTabChange(tab.id)}
              className={cn(
                "relative flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-medium transition-all whitespace-nowrap",
                "hover:bg-muted/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
                isActive
                  ? "text-foreground"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              {/* Active indicator background with spring animation */}
              {isActive && (
                <motion.div
                  layoutId="activeIndicator"
                  className="absolute inset-0 bg-[#E8F1FE] dark:bg-[#1D70F1]/20 rounded-lg shadow-sm"
                  transition={{
                    type: "spring",
                    stiffness: 380,
                    damping: 30,
                  }}
                />
              )}

              {/* Tab content */}
              <div className="relative flex items-center gap-2">
                {Icon && <Icon className="h-4 w-4" />}
                <span>{tab.label}</span>

                {/* Badge */}
                {tab.badge !== undefined && tab.badge > 0 && (
                  <motion.div
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    transition={{ type: "spring", stiffness: 500, damping: 25 }}
                  >
                    <Badge
                      variant={tab.badgeVariant || "secondary"}
                      className={cn(
                        "h-5 min-w-5 px-1.5 text-xs font-semibold",
                        tab.badgeVariant === "destructive" && "animate-pulse"
                      )}
                    >
                      {tab.badge}
                    </Badge>
                  </motion.div>
                )}
              </div>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
