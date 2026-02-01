import type {
  DashboardViewType,
  TabFilterState,
  TabFiltersState,
  ViewFilterConfig,
} from "@/types/dashboard";

/**
 * Filter configuration per dashboard view
 * Defines which filters are applicable for each tab
 */
export const VIEW_FILTER_CONFIG: Record<DashboardViewType, ViewFilterConfig> = {
  tiles: {
    usesProducts: true,
    usesDateRange: true,
    usesCurrency: true,
    usesComparison: true,
  },
  chart: {
    usesProducts: true,
    usesDateRange: false,
    usesCurrency: true,
    usesComparison: false,
  },
  pl: {
    usesProducts: false,
    usesDateRange: false,
    usesCurrency: true,
    usesComparison: true,
  },
  trends: {
    usesProducts: true,
    usesDateRange: false,
    usesCurrency: true,
    usesComparison: false,
  },
  cities: {
    usesProducts: true,
    usesDateRange: true,
    usesCurrency: false,
    usesComparison: false,
    singleProductOnly: true,
  },
};

/**
 * Default filter state for a single tab
 */
export const DEFAULT_TAB_FILTER: TabFilterState = {
  selectedProducts: [],
  selectedPeriodGroup: "default",
  customDateRange: null,
  selectedPeriod: "today",
  selectedComparison: "none",
  selectedCurrency: "TRY",
};

/**
 * Create initial filter states for all tabs
 * Each tab starts with its own default values
 */
export function createInitialTabFilters(): TabFiltersState {
  return {
    tiles: { ...DEFAULT_TAB_FILTER },
    chart: { ...DEFAULT_TAB_FILTER, selectedPeriodGroup: "last30days" },
    pl: { ...DEFAULT_TAB_FILTER },
    trends: { ...DEFAULT_TAB_FILTER },
    cities: { ...DEFAULT_TAB_FILTER, selectedPeriodGroup: "last30days" },
  };
}
