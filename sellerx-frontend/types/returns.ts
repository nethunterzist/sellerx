// Return Analytics Types

export interface ReturnCostBreakdown {
  productCost: number;
  shippingCostOut: number;
  shippingCostReturn: number;
  commissionLoss: number;
  packagingCost: number;
  totalLoss: number;
}

export interface TopReturnedProduct {
  barcode: string;
  productName: string;
  imageUrl: string | null;
  returnCount: number;
  soldCount: number;
  returnRate: number;
  totalLoss: number;
  riskLevel: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
  topReasons: string[];
}

export interface DailyReturnStats {
  date: string;
  returnCount: number;
  totalLoss: number;
}

export interface ReturnAnalyticsResponse {
  totalReturns: number;
  totalReturnedItems: number;
  totalReturnLoss: number;
  returnRate: number;
  avgLossPerReturn: number;
  costBreakdown: ReturnCostBreakdown;
  topReturnedProducts: TopReturnedProduct[];
  returnReasonDistribution: Record<string, number>;
  dailyTrend: DailyReturnStats[];
  startDate: string;
  endDate: string;
  calculatedAt: string;
}

// =====================================================
// Returned Order Decision Types
// =====================================================

export interface ReturnedOrderItem {
  barcode: string;
  productName: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
}

export interface ReturnedOrderDecision {
  orderNumber: string;
  customerName: string;
  orderDate: string;
  items: ReturnedOrderItem[];
  shippingCostOut: number;
  shippingCostReturn: number;
  productCost: number;
  totalLoss: number;
  isResalable: boolean | null;
  returnReason?: string;
  claimStatus?: string;
  returnSource?: 'order_status' | 'claim' | 'cargo_invoice';
}

// =====================================================
// Trendyol Claims Types (Returns Management)
// =====================================================

export type ClaimStatus =
  | "Created"
  | "WaitingInAction"
  | "Accepted"
  | "Rejected"
  | "Unresolved"
  | "Cancelled"
  | "InAnalysis";

export interface ClaimItem {
  claimItemId: string;
  barcode: string;
  productName: string;
  productSize: string | null;
  productColor: string | null;
  price: number;
  quantity: number;
  reasonName: string | null;
  reasonCode: string | null;
  status: string | null;
  customerNote: string | null;
  autoAccepted: boolean;
  acceptedBySeller: boolean;
  imageUrl: string | null;
}

export interface TrendyolClaim {
  id: string;
  claimId: string;
  orderNumber: string | null;
  customerFirstName: string | null;
  customerLastName: string | null;
  customerFullName: string;
  claimDate: string;
  cargoTrackingNumber: string | null;
  cargoTrackingLink: string | null;
  cargoProviderName: string | null;
  status: ClaimStatus;
  items: ClaimItem[];
  totalItemCount: number;
  lastModifiedDate: string | null;
  syncedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface ClaimsSyncResponse {
  totalFetched: number;
  newClaims: number;
  updatedClaims: number;
  message: string;
}

export interface ClaimActionResponse {
  success: boolean;
  message: string;
  claimId?: string;
  newStatus?: string;
}

export interface ClaimIssueReason {
  id: number;
  name: string;
  requiresFile: boolean;
}

export interface ClaimsStats {
  totalClaims: number;
  pendingClaims: number;
  acceptedClaims: number;
  rejectedClaims: number;
  unresolvedClaims: number;
}

export interface ClaimsPage {
  content: TrendyolClaim[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface BulkApproveRequest {
  claims: {
    claimId: string;
    claimLineItemIds: string[];
  }[];
}

export interface BulkActionResponse {
  successCount: number;
  failCount: number;
  message: string;
}

export interface ClaimItemAudit {
  claimId: string;
  claimItemId: string;
  previousStatus: string;
  newStatus: string;
  executorId: string | null;
  executorApp: string | null;
  executorUser: string | null;
  date: string;
}
