"use client";

import { useMemo, useEffect, useCallback } from "react";
import { cn } from "@/lib/utils";
import { RotateCcw, ChevronDown, ChevronUp } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  CHART_METRICS,
  DEFAULT_SELECTED_METRICS,
  MAX_SELECTED_METRICS,
  METRIC_CATEGORY_LABELS,
  METRIC_SELECTION_STORAGE_KEY,
  getMetricsByCategory,
  type ChartMetricConfig,
  type MetricCategory,
} from "@/types/chart-metrics";

interface ChartMetricSelectorProps {
  selectedMetrics: string[];
  onSelectionChange: (metrics: string[]) => void;
  className?: string;
}

export function ChartMetricSelector({
  selectedMetrics,
  onSelectionChange,
  className,
}: ChartMetricSelectorProps) {
  const metricsByCategory = useMemo(() => getMetricsByCategory(), []);
  const categories = Object.keys(metricsByCategory) as MetricCategory[];

  // Load saved selection from localStorage on mount
  useEffect(() => {
    try {
      const saved = localStorage.getItem(METRIC_SELECTION_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          onSelectionChange(parsed);
        }
      }
    } catch {
      // Ignore localStorage errors
    }
  }, []);

  // Save selection to localStorage when it changes
  useEffect(() => {
    try {
      localStorage.setItem(METRIC_SELECTION_STORAGE_KEY, JSON.stringify(selectedMetrics));
    } catch {
      // Ignore localStorage errors
    }
  }, [selectedMetrics]);

  const handleMetricToggle = useCallback((metricId: string) => {
    if (selectedMetrics.includes(metricId)) {
      // Remove metric
      onSelectionChange(selectedMetrics.filter(m => m !== metricId));
    } else {
      // Add metric if under limit
      if (selectedMetrics.length < MAX_SELECTED_METRICS) {
        onSelectionChange([...selectedMetrics, metricId]);
      }
    }
  }, [selectedMetrics, onSelectionChange]);

  const handleReset = useCallback(() => {
    onSelectionChange([...DEFAULT_SELECTED_METRICS]);
  }, [onSelectionChange]);

  const isAtLimit = selectedMetrics.length >= MAX_SELECTED_METRICS;

  return (
    <div className={cn("bg-card rounded-lg border border-border p-4", className)}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-sm font-semibold text-foreground">
            Grafikte Gosterilecek Metrikler
          </h3>
          <p className="text-xs text-muted-foreground mt-0.5">
            En fazla {MAX_SELECTED_METRICS} metrik secilebilir ({selectedMetrics.length}/{MAX_SELECTED_METRICS})
          </p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          onClick={handleReset}
          className="gap-1.5 text-xs h-8"
        >
          <RotateCcw className="h-3.5 w-3.5" />
          Sifirla
        </Button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {categories.map((category) => (
          <MetricCategoryGroup
            key={category}
            category={category}
            label={METRIC_CATEGORY_LABELS[category]}
            metrics={metricsByCategory[category]}
            selectedMetrics={selectedMetrics}
            onToggle={handleMetricToggle}
            isAtLimit={isAtLimit}
          />
        ))}
      </div>
    </div>
  );
}

interface MetricCategoryGroupProps {
  category: MetricCategory;
  label: string;
  metrics: ChartMetricConfig[];
  selectedMetrics: string[];
  onToggle: (metricId: string) => void;
  isAtLimit: boolean;
}

function MetricCategoryGroup({
  label,
  metrics,
  selectedMetrics,
  onToggle,
  isAtLimit,
}: MetricCategoryGroupProps) {
  return (
    <div className="space-y-2">
      <h4 className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
        {label}
      </h4>
      <div className="space-y-1.5">
        {metrics.map((metric) => (
          <MetricCheckbox
            key={metric.id}
            metric={metric}
            isSelected={selectedMetrics.includes(metric.id)}
            onToggle={onToggle}
            isDisabled={isAtLimit && !selectedMetrics.includes(metric.id)}
          />
        ))}
      </div>
    </div>
  );
}

interface MetricCheckboxProps {
  metric: ChartMetricConfig;
  isSelected: boolean;
  onToggle: (metricId: string) => void;
  isDisabled: boolean;
}

function MetricCheckbox({
  metric,
  isSelected,
  onToggle,
  isDisabled,
}: MetricCheckboxProps) {
  return (
    <label
      className={cn(
        "flex items-center gap-2 py-1 px-2 rounded-md cursor-pointer transition-colors",
        isSelected ? "bg-muted/50" : "hover:bg-muted/30",
        isDisabled && !isSelected && "opacity-50 cursor-not-allowed"
      )}
    >
      <Checkbox
        checked={isSelected}
        onCheckedChange={() => onToggle(metric.id)}
        disabled={isDisabled && !isSelected}
        className="h-4 w-4"
      />
      <div
        className="w-3 h-3 rounded-sm flex-shrink-0"
        style={{ backgroundColor: metric.color }}
      />
      <span className="text-sm text-foreground">{metric.shortLabel}</span>
    </label>
  );
}

/**
 * Compact version of the metric selector for smaller screens
 * Uses a collapsible panel
 */
interface CompactMetricSelectorProps extends ChartMetricSelectorProps {
  defaultOpen?: boolean;
}

export function CompactMetricSelector({
  selectedMetrics,
  onSelectionChange,
  className,
  defaultOpen = false,
}: CompactMetricSelectorProps) {
  const metricsByCategory = useMemo(() => getMetricsByCategory(), []);
  const categories = Object.keys(metricsByCategory) as MetricCategory[];

  // Load saved selection from localStorage on mount
  useEffect(() => {
    try {
      const saved = localStorage.getItem(METRIC_SELECTION_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          onSelectionChange(parsed);
        }
      }
    } catch {
      // Ignore localStorage errors
    }
  }, []);

  // Save selection to localStorage when it changes
  useEffect(() => {
    try {
      localStorage.setItem(METRIC_SELECTION_STORAGE_KEY, JSON.stringify(selectedMetrics));
    } catch {
      // Ignore localStorage errors
    }
  }, [selectedMetrics]);

  const handleMetricToggle = useCallback((metricId: string) => {
    if (selectedMetrics.includes(metricId)) {
      onSelectionChange(selectedMetrics.filter(m => m !== metricId));
    } else {
      if (selectedMetrics.length < MAX_SELECTED_METRICS) {
        onSelectionChange([...selectedMetrics, metricId]);
      }
    }
  }, [selectedMetrics, onSelectionChange]);

  const handleReset = useCallback(() => {
    onSelectionChange([...DEFAULT_SELECTED_METRICS]);
  }, [onSelectionChange]);

  const isAtLimit = selectedMetrics.length >= MAX_SELECTED_METRICS;

  // Get selected metric labels for summary
  const selectedLabels = useMemo(() => {
    return selectedMetrics
      .map(id => CHART_METRICS.find(m => m.id === id)?.shortLabel)
      .filter(Boolean)
      .join(", ");
  }, [selectedMetrics]);

  return (
    <Collapsible defaultOpen={defaultOpen} className={className}>
      <div className="bg-card rounded-lg border border-border">
        <CollapsibleTrigger asChild>
          <button className="w-full flex items-center justify-between p-4 hover:bg-muted/30 transition-colors">
            <div className="text-left">
              <h3 className="text-sm font-semibold text-foreground">
                Metrik Secimi
              </h3>
              <p className="text-xs text-muted-foreground mt-0.5 line-clamp-1">
                {selectedLabels || "Metrik seciniz"}
              </p>
            </div>
            <ChevronDown className="h-4 w-4 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-180" />
          </button>
        </CollapsibleTrigger>

        <CollapsibleContent>
          <div className="p-4 pt-0 border-t border-border">
            <div className="flex items-center justify-between mb-4 pt-4">
              <p className="text-xs text-muted-foreground">
                {selectedMetrics.length}/{MAX_SELECTED_METRICS} secili
              </p>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleReset}
                className="gap-1.5 text-xs h-7"
              >
                <RotateCcw className="h-3 w-3" />
                Sifirla
              </Button>
            </div>

            <div className="grid grid-cols-2 gap-4">
              {categories.map((category) => (
                <MetricCategoryGroup
                  key={category}
                  category={category}
                  label={METRIC_CATEGORY_LABELS[category]}
                  metrics={metricsByCategory[category]}
                  selectedMetrics={selectedMetrics}
                  onToggle={handleMetricToggle}
                  isAtLimit={isAtLimit}
                />
              ))}
            </div>
          </div>
        </CollapsibleContent>
      </div>
    </Collapsible>
  );
}
