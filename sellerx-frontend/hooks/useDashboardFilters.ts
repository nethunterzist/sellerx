import { useState, useCallback, useMemo, useEffect } from "react";
import type {
  TabFilterState,
  DashboardViewType,
  TabFiltersState,
  CustomDateRange,
  PeriodType,
  DateRangePreset,
  ViewFilterConfig,
  ComparisonMode,
} from "@/types/dashboard";
import {
  createInitialTabFilters,
  VIEW_FILTER_CONFIG,
} from "@/lib/utils/dashboard-defaults";
import { useCurrency, type SupportedCurrency } from "@/lib/contexts/currency-context";

/**
 * Custom hook for managing dashboard filters per tab
 * Each tab maintains its own independent filter state
 */
export function useDashboardFilters(activeView: DashboardViewType) {
  const { currency: globalCurrency, setCurrency: setGlobalCurrency } = useCurrency();
  const [tabFilters, setTabFilters] = useState<TabFiltersState>(
    createInitialTabFilters
  );

  // Sync dashboard currency filter with global currency preference
  useEffect(() => {
    if (globalCurrency) {
      setTabFilters((prev) => {
        const updated = { ...prev };
        // Update currency for all tabs to match global preference
        (Object.keys(updated) as DashboardViewType[]).forEach((view) => {
          updated[view] = {
            ...updated[view],
            selectedCurrency: globalCurrency,
          };
        });
        return updated;
      });
    }
  }, [globalCurrency]);

  // Get current tab's filter state
  const currentFilters = useMemo(
    () => tabFilters[activeView],
    [tabFilters, activeView]
  );

  // Get filter config for current view
  const filterConfig = useMemo<ViewFilterConfig>(
    () => VIEW_FILTER_CONFIG[activeView],
    [activeView]
  );

  // Update specific filter for current tab
  const updateFilter = useCallback(
    <K extends keyof TabFilterState>(key: K, value: TabFilterState[K]) => {
      setTabFilters((prev) => ({
        ...prev,
        [activeView]: {
          ...prev[activeView],
          [key]: value,
        },
      }));
    },
    [activeView]
  );

  // Convenience setters
  const setSelectedProducts = useCallback(
    (products: string[]) => {
      // For views with singleProductOnly, keep only the last selected product
      const finalProducts =
        filterConfig.singleProductOnly && products.length > 1
          ? [products[products.length - 1]]
          : products;
      updateFilter("selectedProducts", finalProducts);
    },
    [updateFilter, filterConfig.singleProductOnly]
  );

  const setSelectedPeriodGroup = useCallback(
    (group: DateRangePreset) => {
      updateFilter("selectedPeriodGroup", group);
    },
    [updateFilter]
  );

  const setCustomDateRange = useCallback(
    (range: CustomDateRange | null) => {
      updateFilter("customDateRange", range);
    },
    [updateFilter]
  );

  const setSelectedPeriod = useCallback(
    (period: PeriodType) => {
      updateFilter("selectedPeriod", period);
    },
    [updateFilter]
  );

  const setSelectedComparison = useCallback(
    (comparison: ComparisonMode) => {
      updateFilter("selectedComparison", comparison);
    },
    [updateFilter]
  );

  const setSelectedCurrency = useCallback(
    (currency: string) => {
      updateFilter("selectedCurrency", currency);
      // Also update global currency context so all components re-render with new currency
      setGlobalCurrency(currency as SupportedCurrency);
    },
    [updateFilter, setGlobalCurrency]
  );

  // Reset current tab to defaults
  const resetCurrentTab = useCallback(() => {
    setTabFilters((prev) => ({
      ...prev,
      [activeView]: createInitialTabFilters()[activeView],
    }));
  }, [activeView]);

  // Handle date range change (from presets or custom picker)
  const handleDateRangeChange = useCallback(
    (startDate: Date, endDate: Date, label: string) => {
      const format = (date: Date) => date.toISOString().split("T")[0];
      setCustomDateRange({
        startDate: format(startDate),
        endDate: format(endDate),
        label,
      });
    },
    [setCustomDateRange]
  );

  // Handle switching to default 4-card view
  const handleDefaultViewChange = useCallback(() => {
    setCustomDateRange(null);
    setSelectedPeriod("today");
  }, [setCustomDateRange, setSelectedPeriod]);

  return {
    // State
    currentFilters,
    filterConfig,
    tabFilters,

    // Derived values for easier access
    selectedProducts: currentFilters.selectedProducts,
    selectedPeriodGroup: currentFilters.selectedPeriodGroup,
    customDateRange: currentFilters.customDateRange,
    selectedPeriod: currentFilters.selectedPeriod,
    selectedComparison: currentFilters.selectedComparison,
    selectedCurrency: currentFilters.selectedCurrency,

    // Setters
    setSelectedProducts,
    setSelectedPeriodGroup,
    setCustomDateRange,
    setSelectedPeriod,
    setSelectedComparison,
    setSelectedCurrency,
    resetCurrentTab,

    // Handlers
    handleDateRangeChange,
    handleDefaultViewChange,

    // Generic update
    updateFilter,
  };
}
