// Cross-Sell Types
// Product recommendation system for Q&A answers

// =====================================================
// Cross-Sell Rule Types
// =====================================================

export type TriggerType =
  | "KEYWORD"
  | "CATEGORY"
  | "PRODUCT"
  | "QUESTION_PATTERN";

export type RecommendationType =
  | "COMPLEMENTARY"
  | "UPSELL"
  | "ALTERNATIVE"
  | "BUNDLE";

export type RuleStatus = "ACTIVE" | "INACTIVE" | "DRAFT";

export interface TriggerCondition {
  type: TriggerType;
  value: string;
  /** For KEYWORD triggers, optional match mode */
  matchMode?: "EXACT" | "CONTAINS" | "REGEX";
}

export interface RecommendedProduct {
  barcode: string;
  title: string;
  image: string | null;
  salePrice: number;
  /** Custom message to include in the answer */
  customMessage?: string;
  /** Display order within this rule */
  displayOrder: number;
}

export interface CrossSellRule {
  id: string;
  storeId: string;
  name: string;
  description: string | null;
  triggerConditions: TriggerCondition[];
  recommendationType: RecommendationType;
  recommendedProducts: RecommendedProduct[];
  /** Max products to show per answer */
  maxRecommendations: number;
  /** Template for how recommendations appear in answer */
  messageTemplate: string | null;
  status: RuleStatus;
  /** Priority order - lower = higher priority */
  priority: number;
  /** Analytics */
  impressionCount: number;
  clickCount: number;
  conversionCount: number;
  conversionRate: number;
  createdAt: string;
  updatedAt: string;
}

// =====================================================
// Cross-Sell Settings Types
// =====================================================

export interface CrossSellSettings {
  storeId: string;
  enabled: boolean;
  /** Global max recommendations per answer */
  globalMaxRecommendations: number;
  /** Whether to include product images in answers */
  includeImages: boolean;
  /** Whether to include prices in answers */
  includePrices: boolean;
  /** Default message template */
  defaultMessageTemplate: string;
  updatedAt: string;
}

// =====================================================
// Request Types
// =====================================================

export interface CreateCrossSellRuleRequest {
  name: string;
  description?: string;
  triggerConditions: TriggerCondition[];
  recommendationType: RecommendationType;
  recommendedProducts: Omit<RecommendedProduct, "title" | "image" | "salePrice">[];
  maxRecommendations?: number;
  messageTemplate?: string;
  status?: RuleStatus;
}

export interface UpdateCrossSellRuleRequest extends Partial<CreateCrossSellRuleRequest> {
  id: string;
}

export interface ReorderCrossSellRulesRequest {
  ruleIds: string[];
}

export interface UpdateCrossSellSettingsRequest {
  enabled?: boolean;
  globalMaxRecommendations?: number;
  includeImages?: boolean;
  includePrices?: boolean;
  defaultMessageTemplate?: string;
}

// =====================================================
// Analytics Types
// =====================================================

export interface CrossSellAnalytics {
  totalImpressions: number;
  totalClicks: number;
  totalConversions: number;
  overallConversionRate: number;
  topPerformingRules: {
    ruleId: string;
    ruleName: string;
    impressions: number;
    clicks: number;
    conversions: number;
    conversionRate: number;
  }[];
  dailyStats: {
    date: string;
    impressions: number;
    clicks: number;
    conversions: number;
  }[];
}

// =====================================================
// Product Search Types (for product selector)
// =====================================================

export interface ProductSearchResult {
  barcode: string;
  title: string;
  image: string | null;
  salePrice: number;
  onSale: boolean;
  trendyolQuantity: number;
}
