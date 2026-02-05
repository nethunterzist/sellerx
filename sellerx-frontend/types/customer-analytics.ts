export interface CustomerAnalyticsSummary {
  totalCustomers: number;
  repeatCustomers: number;
  repeatRate: number;
  avgOrdersPerCustomer: number;
  avgItemsPerCustomer: number;
  avgRepeatIntervalDays: number;
  repeatCustomerRevenue: number;
  totalRevenue: number;
  repeatRevenueShare: number;
}

export interface SegmentData {
  segment: string;
  customerCount: number;
  totalRevenue: number;
  percentage: number;
}

export interface CityRepeatData {
  city: string;
  totalCustomers: number;
  repeatCustomers: number;
  repeatRate: number;
  totalRevenue: number;
}

export interface MonthlyTrend {
  month: string;
  newCustomers: number;
  repeatCustomers: number;
  newRevenue: number;
  repeatRevenue: number;
}

export interface CustomerAnalyticsResponse {
  summary: CustomerAnalyticsSummary;
  segmentation: SegmentData[];
  cityAnalysis: CityRepeatData[];
  monthlyTrend: MonthlyTrend[];
}

export interface ProductRepeatData {
  barcode: string;
  productName: string;
  totalBuyers: number;
  repeatBuyers: number;
  repeatRate: number;
  avgDaysBetweenRepurchase: number;
  totalQuantitySold: number;
  image?: string;
  productUrl?: string;
}

export interface CustomerListItem {
  customerKey: string;
  displayName: string;
  city: string;
  orderCount: number;
  itemCount: number;
  totalSpend: number;
  firstOrderDate: string;
  lastOrderDate: string;
  avgOrderValue: number;
  rfmSegment: string;
  recencyScore: number;
  frequencyScore: number;
  monetaryScore: number;
}

export interface CustomerListResponse {
  content: CustomerListItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface CrossSellData {
  sourceBarcode: string;
  sourceProductName: string;
  targetBarcode: string;
  targetProductName: string;
  coOccurrenceCount: number;
  confidence: number;
  sourceImage?: string;
  sourceProductUrl?: string;
  targetImage?: string;
  targetProductUrl?: string;
}

export interface BackfillStatus {
  totalOrders: number;
  ordersWithCustomerData: number;
  coveragePercent: number;
  ordersWithoutCustomerData: number;
}

// ============== New Advanced Analytics Types ==============

export interface LifecycleStageData {
  stage: string;
  label: string;
  icon: string;
  color: string;
  customerCount: number;
  totalRevenue: number;
  percentage: number;
}

export interface CohortData {
  cohortMonth: string;
  cohortSize: number;
  retentionRates: Record<string, number>; // month -> retention percentage
}

export interface FrequencyDistributionData {
  bucket: string;        // "1-2", "3-5", "6-10", "11-20", "21+" (total items purchased)
  customerCount: number;
  totalRevenue: number;
  totalOrders: number;   // actually totalItems now
  percentage: number;    // percentage of total customers
  revenueShare: number;  // percentage of total revenue
}

export interface ClvSummaryData {
  avgClv: number;
  medianClv: number;
  top10PercentClv: number;
  top10PercentRevenueShare: number;
}
