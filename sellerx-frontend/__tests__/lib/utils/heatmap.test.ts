import { describe, it, expect } from "vitest";
import {
  getHeatmapColor,
  getHeatmapTextColor,
  getHeatmapCellStyle,
  calculateChange,
  formatChangeText,
  formatCellValue,
} from "@/lib/utils/heatmap";

describe("getHeatmapColor", () => {
  it("should return dark green for strong positive changes (>= 50)", () => {
    expect(getHeatmapColor(50)).toBe("bg-emerald-600");
    expect(getHeatmapColor(100)).toBe("bg-emerald-600");
  });

  it("should return progressively lighter green for positive changes", () => {
    expect(getHeatmapColor(30)).toBe("bg-emerald-500");
    expect(getHeatmapColor(15)).toBe("bg-emerald-400");
    expect(getHeatmapColor(5)).toBe("bg-emerald-300");
    expect(getHeatmapColor(1)).toBe("bg-emerald-200");
  });

  it("should return gray for zero change", () => {
    expect(getHeatmapColor(0)).toBe("bg-gray-100");
  });

  it("should return orange/red for negative changes", () => {
    expect(getHeatmapColor(-1)).toBe("bg-orange-200");
    expect(getHeatmapColor(-15)).toBe("bg-orange-300");
    expect(getHeatmapColor(-30)).toBe("bg-orange-400");
    expect(getHeatmapColor(-50)).toBe("bg-red-400");
    expect(getHeatmapColor(-100)).toBe("bg-red-500");
  });
});

describe("getHeatmapTextColor", () => {
  it("should return white text for intense backgrounds", () => {
    expect(getHeatmapTextColor(50)).toBe("text-white");
    expect(getHeatmapTextColor(-50)).toBe("text-white");
  });

  it("should return appropriate text colors for moderate changes", () => {
    expect(getHeatmapTextColor(15)).toBe("text-emerald-900");
    expect(getHeatmapTextColor(5)).toBe("text-emerald-800");
    expect(getHeatmapTextColor(0)).toBe("text-gray-600");
    expect(getHeatmapTextColor(-5)).toBe("text-orange-800");
  });
});

describe("getHeatmapCellStyle", () => {
  it("should combine background and text color classes", () => {
    const style = getHeatmapCellStyle(50);
    expect(style).toBe("bg-emerald-600 text-white");
  });

  it("should handle zero change", () => {
    const style = getHeatmapCellStyle(0);
    expect(style).toBe("bg-gray-100 text-gray-600");
  });
});

describe("calculateChange", () => {
  it("should calculate percentage change correctly", () => {
    expect(calculateChange(150, 100)).toBe(50);
    expect(calculateChange(50, 100)).toBe(-50);
    expect(calculateChange(100, 100)).toBe(0);
  });

  it("should return 100 when previous is 0 and current is positive", () => {
    expect(calculateChange(50, 0)).toBe(100);
  });

  it("should return 0 when both are 0", () => {
    expect(calculateChange(0, 0)).toBe(0);
  });

  it("should round to nearest integer", () => {
    expect(calculateChange(133, 100)).toBe(33);
  });
});

describe("formatChangeText", () => {
  it("should format positive changes with + sign", () => {
    expect(formatChangeText(50)).toBe("+50%");
  });

  it("should format negative changes with - sign", () => {
    expect(formatChangeText(-25)).toBe("-25%");
  });

  it("should format zero change", () => {
    expect(formatChangeText(0)).toBe("0%");
  });
});

describe("formatCellValue", () => {
  it("should format currency values with Turkish Lira symbol (fallback)", () => {
    const result = formatCellValue(1500, true);
    expect(result).toContain("1.500");
    expect(result).toContain("₺");
  });

  it("should format negative currency values", () => {
    const result = formatCellValue(-500, true);
    expect(result).toMatch(/^-/);
    expect(result).toContain("500");
  });

  it("should use custom formatCurrency function when provided", () => {
    const customFormat = (amount: number) => `$${amount.toFixed(2)}`;
    const result = formatCellValue(100, true, customFormat);
    expect(result).toBe("$100.00");
  });

  it("should format non-currency values as locale numbers", () => {
    const result = formatCellValue(1500, false);
    expect(result).toBe("1.500");
  });
});
