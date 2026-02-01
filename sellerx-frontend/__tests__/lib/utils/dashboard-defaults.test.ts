import { describe, it, expect } from "vitest";
import {
  VIEW_FILTER_CONFIG,
  DEFAULT_TAB_FILTER,
  createInitialTabFilters,
} from "@/lib/utils/dashboard-defaults";

describe("VIEW_FILTER_CONFIG", () => {
  it("should define filter configuration for all dashboard views", () => {
    expect(VIEW_FILTER_CONFIG).toHaveProperty("tiles");
    expect(VIEW_FILTER_CONFIG).toHaveProperty("chart");
    expect(VIEW_FILTER_CONFIG).toHaveProperty("pl");
    expect(VIEW_FILTER_CONFIG).toHaveProperty("trends");
    expect(VIEW_FILTER_CONFIG).toHaveProperty("cities");
  });

  it("should have tiles view supporting all filter types", () => {
    expect(VIEW_FILTER_CONFIG.tiles).toEqual({
      usesProducts: true,
      usesDateRange: true,
      usesCurrency: true,
      usesComparison: true,
    });
  });

  it("should have cities view supporting single product only", () => {
    expect(VIEW_FILTER_CONFIG.cities.singleProductOnly).toBe(true);
  });

  it("should have pl view not using products", () => {
    expect(VIEW_FILTER_CONFIG.pl.usesProducts).toBe(false);
  });
});

describe("DEFAULT_TAB_FILTER", () => {
  it("should have correct default values", () => {
    expect(DEFAULT_TAB_FILTER.selectedProducts).toEqual([]);
    expect(DEFAULT_TAB_FILTER.selectedPeriodGroup).toBe("default");
    expect(DEFAULT_TAB_FILTER.customDateRange).toBeNull();
    expect(DEFAULT_TAB_FILTER.selectedPeriod).toBe("today");
    expect(DEFAULT_TAB_FILTER.selectedComparison).toBe("none");
    expect(DEFAULT_TAB_FILTER.selectedCurrency).toBe("TRY");
  });
});

describe("createInitialTabFilters", () => {
  it("should create filter states for all dashboard views", () => {
    const filters = createInitialTabFilters();

    expect(filters).toHaveProperty("tiles");
    expect(filters).toHaveProperty("chart");
    expect(filters).toHaveProperty("pl");
    expect(filters).toHaveProperty("trends");
    expect(filters).toHaveProperty("cities");
  });

  it("should give tiles view the default period group", () => {
    const filters = createInitialTabFilters();
    expect(filters.tiles.selectedPeriodGroup).toBe("default");
  });

  it("should give chart view the last30days period group", () => {
    const filters = createInitialTabFilters();
    expect(filters.chart.selectedPeriodGroup).toBe("last30days");
  });

  it("should give cities view the last30days period group", () => {
    const filters = createInitialTabFilters();
    expect(filters.cities.selectedPeriodGroup).toBe("last30days");
  });

  it("should return separate objects for different tabs", () => {
    const filters = createInitialTabFilters();
    // The tiles and chart objects themselves should be different references
    expect(filters.tiles).not.toBe(filters.chart);
    // Verify they have their own period group values
    expect(filters.tiles.selectedPeriodGroup).not.toBe(
      filters.chart.selectedPeriodGroup
    );
  });
});
