// Q&A Module Types
// Trendyol Customer Questions Management

export interface Answer {
  id: string;
  answerText: string;
  isSubmitted: boolean;
  trendyolAnswerId: string | null;
  submittedAt: string | null;
  submittedBy: number | null;
  submittedByEmail: string | null;
  createdAt: string;
}

export interface Question {
  id: string;
  questionId: string; // Trendyol question ID
  productId: string | null;
  barcode: string | null;
  productTitle: string | null;
  customerQuestion: string;
  questionDate: string;
  status: 'PENDING' | 'ANSWERED';
  isPublic: boolean;
  answers: Answer[];
  createdAt: string;
  updatedAt: string;
}

export interface QaStats {
  totalQuestions: number;
  pendingQuestions: number;
  answeredQuestions: number;
}

export interface QaSyncResponse {
  totalFetched: number;
  newQuestions: number;
  updatedQuestions: number;
  message: string;
}

export interface QuestionsPage {
  content: Question[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page (0-indexed)
  first: boolean;
  last: boolean;
  empty: boolean;
}

// Filter types for Q&A list
export type QuestionStatus = 'ALL' | 'PENDING' | 'ANSWERED';

export interface QaFilters {
  status: QuestionStatus;
  page: number;
  size: number;
}

// =====================================================
// KNOWLEDGE SUGGESTIONS TYPES
// =====================================================

export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'MODIFIED';
export type SuggestionPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface KnowledgeSuggestion {
  id: string;
  storeId: string;
  suggestedTitle: string;
  suggestedContent: string;
  sampleQuestions: string[];
  questionCount: number;
  avgSimilarity: number;
  status: SuggestionStatus;
  priority: SuggestionPriority;
  reviewedAt: string | null;
  reviewedBy: number | null;
  reviewedByEmail: string | null;
  reviewNotes: string | null;
  firstSeenAt: string;
  lastSeenAt: string;
  createdAt: string;
}

// =====================================================
// SENIORITY / PATTERNS TYPES
// =====================================================

export type SeniorityLevel = 'JUNIOR' | 'LEARNING' | 'SENIOR' | 'EXPERT';

export interface QaPattern {
  id: string;
  storeId: string;
  patternHash: string;
  canonicalQuestion: string;
  canonicalAnswer: string | null;
  occurrenceCount: number;
  approvalCount: number;
  rejectionCount: number;
  modificationCount: number;
  confidenceScore: number;
  seniorityLevel: SeniorityLevel;
  isAutoSubmitEligible: boolean;
  autoSubmitEnabledAt: string | null;
  autoSubmitDisabledReason: string | null;
  productId: string | null;
  category: string | null;
  lastHumanReview: string | null;
  firstSeenAt: string;
  lastSeenAt: string;
  // Computed fields
  totalReviews: number;
  approvalRate: number;
}

export interface SeniorityStats {
  totalPatterns: number;
  juniorCount: number;
  learningCount: number;
  seniorCount: number;
  expertCount: number;
  autoSubmitEligibleCount: number;
}

// =====================================================
// CONFLICT ALERTS TYPES
// =====================================================

export type ConflictType =
  | 'KNOWLEDGE_CONFLICT'
  | 'BRAND_INCONSISTENCY'
  | 'LEGAL_RISK'
  | 'HEALTH_SAFETY';

export type ConflictSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type ConflictStatus = 'ACTIVE' | 'RESOLVED' | 'DISMISSED';

export interface ConflictAlert {
  id: string;
  storeId: string;
  questionId: string | null;
  customerQuestion: string | null;
  conflictType: ConflictType;
  severity: ConflictSeverity;
  sourceAType: string;
  sourceAContent: string;
  sourceBType: string | null;
  sourceBContent: string | null;
  detectedKeywords: string[];
  status: ConflictStatus;
  resolutionNotes: string | null;
  resolvedAt: string | null;
  resolvedBy: number | null;
  resolvedByEmail: string | null;
  createdAt: string;
}

export interface ConflictStats {
  totalActive: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  legalRiskCount: number;
  healthSafetyCount: number;
  knowledgeConflictCount: number;
  brandInconsistencyCount: number;
}
