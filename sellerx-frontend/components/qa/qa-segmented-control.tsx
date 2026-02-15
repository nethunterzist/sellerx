"use client";

import { useCallback, useRef } from "react";
import { motion } from "motion/react";
import { type LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export type QaTabValue = "answer-flow" | "ai-brain" | "rules" | "performance" | "cross-sell";

interface QaTab {
  id: QaTabValue;
  label: string;
  icon: LucideIcon;
  badge?: number;
  badgeVariant?: "default" | "destructive" | "secondary";
}

interface QaSegmentedControlProps {
  tabs: QaTab[];
  activeTab: QaTabValue;
  onTabChange: (tab: QaTabValue) => void;
}

export function QaSegmentedControl({
  tabs,
  activeTab,
  onTabChange,
}: QaSegmentedControlProps) {
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
    <nav className="glass-header px-4 py-3" aria-label="QA navigation">
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
              {/* Active indicator background */}
              {isActive && (
                <motion.div
                  layoutId="activeIndicator"
                  className="absolute inset-0 bg-primary/10 dark:bg-primary/20 rounded-lg border border-primary/20"
                  transition={{
                    type: "spring",
                    stiffness: 380,
                    damping: 30,
                  }}
                />
              )}

              {/* Tab content */}
              <div className="relative flex items-center gap-2">
                <Icon className="h-4 w-4" />
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
