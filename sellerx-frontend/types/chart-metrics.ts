/**
 * Chart Metric Configuration System
 * Defines all available metrics for the dynamic dashboard chart
 */

export type MetricFormatter = 'currency' | 'number' | 'percentage';
export type MetricType = 'bar' | 'line' | 'area';
export type MetricCategory = 'sales' | 'costs' | 'profit' | 'metrics';
export type YAxisId = 'left' | 'right';

export interface ChartMetricConfig {
  id: string;
  label: string;
  shortLabel: string;
  type: MetricType;
  yAxisId: YAxisId;
  color: string;
  formatter: MetricFormatter;
  dataKey: string;
  category: MetricCategory;
  description?: string;
}

/**
 * All available metrics for the dashboard chart
 * Organized by category for UI grouping
 */
export const CHART_METRICS: ChartMetricConfig[] = [
  // ==================== SALES CATEGORY ====================
  {
    id: 'units',
    label: 'Satış Adedi',
    shortLabel: 'Adet',
    type: 'bar',
    yAxisId: 'left',
    color: '#14B8A6', // teal-500
    formatter: 'number',
    dataKey: 'units',
    category: 'sales',
    description: 'Satılan toplam ürün adedi',
  },
  {
    id: 'revenue',
    label: 'Satış (Brüt Ciro)',
    shortLabel: 'Satış',
    type: 'line',
    yAxisId: 'right',
    color: '#3B82F6', // blue-500
    formatter: 'currency',
    dataKey: 'revenue',
    category: 'sales',
    description: 'Toplam satış geliri (brüt)',
  },
  {
    id: 'netRevenue',
    label: 'Net Ciro',
    shortLabel: 'Net Ciro',
    type: 'line',
    yAxisId: 'right',
    color: '#6366F1', // indigo-500
    formatter: 'currency',
    dataKey: 'netRevenue',
    category: 'sales',
    description: 'İndirimler sonrası net ciro',
  },
  {
    id: 'orders',
    label: 'Sipariş Sayısı',
    shortLabel: 'Sipariş',
    type: 'bar',
    yAxisId: 'left',
    color: '#8B5CF6', // violet-500
    formatter: 'number',
    dataKey: 'orders',
    category: 'sales',
    description: 'Toplam sipariş sayısı',
  },

  // ==================== COSTS CATEGORY ====================
  {
    id: 'productCost',
    label: 'Ürün Maliyeti',
    shortLabel: 'Maliyet',
    type: 'line',
    yAxisId: 'right',
    color: '#F97316', // orange-500
    formatter: 'currency',
    dataKey: 'productCost',
    category: 'costs',
    description: 'FIFO bazlı ürün maliyeti',
  },
  {
    id: 'commission',
    label: 'Komisyon',
    shortLabel: 'Komisyon',
    type: 'line',
    yAxisId: 'right',
    color: '#EF4444', // red-500
    formatter: 'currency',
    dataKey: 'commission',
    category: 'costs',
    description: 'Platform komisyonu',
  },
  {
    id: 'shippingCost',
    label: 'Kargo Maliyeti',
    shortLabel: 'Kargo',
    type: 'line',
    yAxisId: 'right',
    color: '#A855F7', // purple-500
    formatter: 'currency',
    dataKey: 'shippingCost',
    category: 'costs',
    description: 'Kargo ve teslimat maliyeti',
  },
  {
    id: 'advertisingCost',
    label: 'Reklam Gideri',
    shortLabel: 'Reklam',
    type: 'line',
    yAxisId: 'right',
    color: '#EC4899', // pink-500
    formatter: 'currency',
    dataKey: 'advertisingCost',
    category: 'costs',
    description: 'Reklam ve pazarlama giderleri',
  },
  {
    id: 'stoppage',
    label: 'Stopaj',
    shortLabel: 'Stopaj',
    type: 'line',
    yAxisId: 'right',
    color: '#F59E0B', // amber-500
    formatter: 'currency',
    dataKey: 'stoppage',
    category: 'costs',
    description: 'Stopaj kesintisi',
  },

  // ==================== PROFIT CATEGORY ====================
  {
    id: 'grossProfit',
    label: 'Brüt Kâr',
    shortLabel: 'Brüt Kâr',
    type: 'line',
    yAxisId: 'right',
    color: '#22C55E', // green-500
    formatter: 'currency',
    dataKey: 'grossProfit',
    category: 'profit',
    description: 'Satış - Ürün Maliyeti',
  },
  {
    id: 'netProfit',
    label: 'Net Kâr',
    shortLabel: 'Net Kâr',
    type: 'line',
    yAxisId: 'right',
    color: '#10B981', // emerald-500
    formatter: 'currency',
    dataKey: 'netProfit',
    category: 'profit',
    description: 'Tüm giderler sonrası net kâr',
  },

  // ==================== METRICS CATEGORY ====================
  {
    id: 'margin',
    label: 'Kâr Marjı',
    shortLabel: 'Marj',
    type: 'line',
    yAxisId: 'left',
    color: '#06B6D4', // cyan-500
    formatter: 'percentage',
    dataKey: 'margin',
    category: 'metrics',
    description: 'Brüt kâr / Satış oranı',
  },
  {
    id: 'roi',
    label: 'ROI',
    shortLabel: 'ROI',
    type: 'line',
    yAxisId: 'left',
    color: '#0EA5E9', // sky-500
    formatter: 'percentage',
    dataKey: 'roi',
    category: 'metrics',
    description: 'Yatırım getirisi',
  },
];

/**
 * Default selected metrics when no selection is saved
 */
export const DEFAULT_SELECTED_METRICS = ['units', 'revenue', 'netProfit'];

/**
 * Maximum number of metrics that can be selected at once
 * Prevents chart from becoming too cluttered
 */
export const MAX_SELECTED_METRICS = 6;

/**
 * Category labels for UI grouping
 */
export const METRIC_CATEGORY_LABELS: Record<MetricCategory, string> = {
  sales: 'Satış',
  costs: 'Maliyetler',
  profit: 'Kâr',
  metrics: 'Oranlar',
};

/**
 * Get metrics grouped by category
 */
export function getMetricsByCategory(): Record<MetricCategory, ChartMetricConfig[]> {
  return CHART_METRICS.reduce((acc, metric) => {
    if (!acc[metric.category]) {
      acc[metric.category] = [];
    }
    acc[metric.category].push(metric);
    return acc;
  }, {} as Record<MetricCategory, ChartMetricConfig[]>);
}

/**
 * Get metric config by ID
 */
export function getMetricById(id: string): ChartMetricConfig | undefined {
  return CHART_METRICS.find(m => m.id === id);
}

/**
 * LocalStorage key for persisting metric selection
 */
export const METRIC_SELECTION_STORAGE_KEY = 'sellerx-chart-metrics';

/**
 * Extended chart data point with all available metrics
 */
export interface ExtendedChartDataPoint {
  date: string;
  displayDate: string;
  // Sales
  units: number;
  revenue: number;
  netRevenue: number;
  orders: number;
  // Costs
  productCost: number;
  commission: number;
  shippingCost: number;
  advertisingCost: number;
  stoppage: number;
  // Profit
  grossProfit: number;
  netProfit: number;
  // Metrics
  margin: number;
  roi: number;
  // Additional
  refunds: number;
  refundCost: number;
}
