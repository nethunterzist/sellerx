/**
 * Heatmap coloring utilities for dashboard trends - Sellerboard style
 */

// Sellerboard-style heatmap colors with gradient
export function getHeatmapColor(change: number): string {
  // Strong positive (dark green)
  if (change >= 50) return "bg-emerald-600";
  if (change >= 30) return "bg-emerald-500";
  if (change >= 15) return "bg-emerald-400";
  if (change >= 5) return "bg-emerald-300";
  if (change > 0) return "bg-emerald-200";

  // Neutral
  if (change === 0) return "bg-gray-100";

  // Negative (orange to red gradient)
  if (change >= -5) return "bg-orange-200";
  if (change >= -15) return "bg-orange-300";
  if (change >= -30) return "bg-orange-400";
  if (change >= -50) return "bg-red-400";
  return "bg-red-500";
}

// Get text color based on background intensity
export function getHeatmapTextColor(change: number): string {
  if (change >= 30 || change <= -30) return "text-white";
  if (change >= 15) return "text-emerald-900";
  if (change > 0) return "text-emerald-800";
  if (change === 0) return "text-gray-600";
  if (change >= -15) return "text-orange-800";
  return "text-red-900";
}

// Cell background style for Sellerboard look
export function getHeatmapCellStyle(change: number): string {
  const bg = getHeatmapColor(change);
  const text = getHeatmapTextColor(change);
  return `${bg} ${text}`;
}

export function calculateChange(current: number, previous: number): number {
  if (previous === 0) return current > 0 ? 100 : 0;
  return Math.round(((current - previous) / previous) * 100);
}

export function formatChangeText(change: number): string {
  if (change === 0) return "0%";
  const sign = change > 0 ? "+" : "";
  return `${sign}${change}%`;
}

// Format currency for cells
// Accepts optional formatCurrency function from currency context for dynamic currency display
export function formatCellValue(
  value: number,
  isCurrency: boolean,
  formatCurrency?: (amount: number) => string
): string {
  if (isCurrency) {
    if (formatCurrency) {
      // Use provided formatCurrency for proper currency conversion
      const formatted = formatCurrency(Math.abs(value));
      return value < 0 ? `-${formatted}` : formatted;
    }
    // Fallback: hardcoded TRY format (for backwards compatibility)
    const formatted = new Intl.NumberFormat("tr-TR", {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(Math.abs(value));
    return value < 0 ? `-₺${formatted}` : `₺${formatted}`;
  }
  return value.toLocaleString("tr-TR");
}
