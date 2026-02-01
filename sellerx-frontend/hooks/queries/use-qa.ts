import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type {
  Question,
  QuestionsPage,
  QaStats,
  QaSyncResponse,
  QuestionStatus,
  KnowledgeSuggestion,
  SuggestionStatus,
  QaPattern,
  SeniorityLevel,
  SeniorityStats,
  ConflictAlert,
  ConflictStatus,
  ConflictStats,
} from "@/types/qa";

// Q&A Query Keys
export const qaKeys = {
  all: ["qa"] as const,
  questions: () => [...qaKeys.all, "questions"] as const,
  questionsByStore: (storeId: string) =>
    [...qaKeys.questions(), "store", storeId] as const,
  questionsByStoreFiltered: (
    storeId: string,
    status: QuestionStatus,
    page: number,
    size: number
  ) => [...qaKeys.questionsByStore(storeId), { status, page, size }] as const,
  question: (id: string) => [...qaKeys.questions(), "detail", id] as const,
  stats: () => [...qaKeys.all, "stats"] as const,
  statsByStore: (storeId: string) => [...qaKeys.stats(), storeId] as const,

  // Knowledge Suggestions
  suggestions: () => [...qaKeys.all, "suggestions"] as const,
  suggestionsByStore: (storeId: string, status?: SuggestionStatus) =>
    [...qaKeys.suggestions(), "store", storeId, { status }] as const,

  // Patterns (Seniority)
  patterns: () => [...qaKeys.all, "patterns"] as const,
  patternsByStore: (storeId: string, seniorityLevel?: SeniorityLevel, autoSubmitOnly?: boolean) =>
    [...qaKeys.patterns(), "store", storeId, { seniorityLevel, autoSubmitOnly }] as const,
  patternsStats: (storeId: string) =>
    [...qaKeys.patterns(), "stats", storeId] as const,

  // Conflicts
  conflicts: () => [...qaKeys.all, "conflicts"] as const,
  conflictsByStore: (storeId: string, status?: ConflictStatus) =>
    [...qaKeys.conflicts(), "store", storeId, { status }] as const,
  conflictsStats: (storeId: string) =>
    [...qaKeys.conflicts(), "stats", storeId] as const,
};

// API Functions
async function fetchQuestions(
  storeId: string,
  status: QuestionStatus,
  page: number,
  size: number
): Promise<QuestionsPage> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });

  if (status !== "ALL") {
    params.set("status", status);
  }

  const response = await fetch(
    `/api/qa/stores/${storeId}/questions?${params.toString()}`
  );

  if (!response.ok) {
    throw new Error("Failed to fetch questions");
  }

  return response.json();
}

async function fetchQuestion(questionId: string): Promise<Question> {
  const response = await fetch(`/api/qa/questions/${questionId}`);

  if (!response.ok) {
    throw new Error("Failed to fetch question");
  }

  return response.json();
}

async function fetchStats(storeId: string): Promise<QaStats> {
  const response = await fetch(`/api/qa/stores/${storeId}/stats`);

  if (!response.ok) {
    throw new Error("Failed to fetch Q&A stats");
  }

  return response.json();
}

async function syncQuestions(storeId: string): Promise<QaSyncResponse> {
  const response = await fetch(`/api/qa/stores/${storeId}/questions/sync`, {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error("Failed to sync questions");
  }

  return response.json();
}

// Hooks

/**
 * Get questions for a store with pagination and filtering
 */
export function useQuestions(
  storeId: string,
  status: QuestionStatus = "ALL",
  page: number = 0,
  size: number = 20
) {
  return useQuery({
    queryKey: qaKeys.questionsByStoreFiltered(storeId, status, page, size),
    queryFn: () => fetchQuestions(storeId, status, page, size),
    enabled: !!storeId,
  });
}

/**
 * Get single question by ID
 */
export function useQuestion(questionId: string) {
  return useQuery({
    queryKey: qaKeys.question(questionId),
    queryFn: () => fetchQuestion(questionId),
    enabled: !!questionId,
  });
}

/**
 * Get Q&A statistics for a store
 */
export function useQaStats(storeId: string) {
  return useQuery({
    queryKey: qaKeys.statsByStore(storeId),
    queryFn: () => fetchStats(storeId),
    enabled: !!storeId,
  });
}

/**
 * Sync questions from Trendyol
 */
export function useSyncQuestions() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: syncQuestions,
    onSuccess: (data, storeId) => {
      // Invalidate and refetch questions and stats
      queryClient.invalidateQueries({ queryKey: qaKeys.questionsByStore(storeId) });
      queryClient.invalidateQueries({ queryKey: qaKeys.statsByStore(storeId) });
    },
  });
}

// API function for manual answer submission
async function submitAnswer(
  storeId: string,
  questionId: string,
  answerText: string
): Promise<void> {
  const response = await fetch(
    `/api/qa/stores/${storeId}/questions/${questionId}/answer`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ answerText }),
    }
  );

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to submit answer");
  }
}

/**
 * Submit manual answer to a question
 */
export function useSubmitAnswer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storeId,
      questionId,
      answerText,
    }: {
      storeId: string;
      questionId: string;
      answerText: string;
    }) => submitAnswer(storeId, questionId, answerText),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: qaKeys.questionsByStore(variables.storeId),
      });
      queryClient.invalidateQueries({
        queryKey: qaKeys.statsByStore(variables.storeId),
      });
    },
  });
}

// =====================================================
// KNOWLEDGE SUGGESTIONS API & HOOKS
// =====================================================

async function fetchSuggestions(
  storeId: string,
  status?: SuggestionStatus
): Promise<KnowledgeSuggestion[]> {
  const params = new URLSearchParams();
  if (status) {
    params.set("status", status);
  }

  const url = `/api/qa/stores/${storeId}/suggestions${params.toString() ? '?' + params.toString() : ''}`;
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error("Failed to fetch suggestions");
  }

  return response.json();
}

async function approveSuggestion(suggestionId: string): Promise<KnowledgeSuggestion> {
  const response = await fetch(`/api/qa/suggestions/${suggestionId}/approve`, {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error("Failed to approve suggestion");
  }

  return response.json();
}

async function rejectSuggestion(
  suggestionId: string,
  reason?: string
): Promise<KnowledgeSuggestion> {
  const response = await fetch(`/api/qa/suggestions/${suggestionId}/reject`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason }),
  });

  if (!response.ok) {
    throw new Error("Failed to reject suggestion");
  }

  return response.json();
}

async function modifySuggestion(
  suggestionId: string,
  data: { title?: string; content?: string }
): Promise<KnowledgeSuggestion> {
  const response = await fetch(`/api/qa/suggestions/${suggestionId}/modify`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error("Failed to modify suggestion");
  }

  return response.json();
}

/**
 * Get knowledge suggestions for a store
 */
export function useSuggestions(storeId: string, status?: SuggestionStatus) {
  return useQuery({
    queryKey: qaKeys.suggestionsByStore(storeId, status),
    queryFn: () => fetchSuggestions(storeId, status),
    enabled: !!storeId,
  });
}

/**
 * Approve a knowledge suggestion
 */
export function useApproveSuggestion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: approveSuggestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qaKeys.suggestions() });
    },
  });
}

/**
 * Reject a knowledge suggestion
 */
export function useRejectSuggestion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ suggestionId, reason }: { suggestionId: string; reason?: string }) =>
      rejectSuggestion(suggestionId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qaKeys.suggestions() });
    },
  });
}

/**
 * Modify and approve a knowledge suggestion
 */
export function useModifySuggestion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      suggestionId,
      title,
      content,
    }: {
      suggestionId: string;
      title?: string;
      content?: string;
    }) => modifySuggestion(suggestionId, { title, content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qaKeys.suggestions() });
    },
  });
}

// =====================================================
// PATTERNS (SENIORITY) API & HOOKS
// =====================================================

async function fetchPatterns(
  storeId: string,
  seniorityLevel?: SeniorityLevel,
  autoSubmitOnly?: boolean
): Promise<QaPattern[]> {
  const params = new URLSearchParams();
  if (seniorityLevel) {
    params.set("seniorityLevel", seniorityLevel);
  }
  if (autoSubmitOnly !== undefined) {
    params.set("autoSubmitOnly", String(autoSubmitOnly));
  }

  const url = `/api/qa/stores/${storeId}/patterns${params.toString() ? '?' + params.toString() : ''}`;
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error("Failed to fetch patterns");
  }

  return response.json();
}

async function fetchSeniorityStats(storeId: string): Promise<SeniorityStats> {
  const response = await fetch(`/api/qa/stores/${storeId}/patterns/stats`);

  if (!response.ok) {
    throw new Error("Failed to fetch seniority stats");
  }

  return response.json();
}

async function promotePattern(patternId: string): Promise<QaPattern> {
  const response = await fetch(`/api/qa/patterns/${patternId}/promote`, {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error("Failed to promote pattern");
  }

  return response.json();
}

async function demotePattern(patternId: string, reason?: string): Promise<QaPattern> {
  const response = await fetch(`/api/qa/patterns/${patternId}/demote`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason }),
  });

  if (!response.ok) {
    throw new Error("Failed to demote pattern");
  }

  return response.json();
}

async function enableAutoSubmit(patternId: string): Promise<QaPattern> {
  const response = await fetch(`/api/qa/patterns/${patternId}/enable-auto-submit`, {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error("Failed to enable auto-submit");
  }

  return response.json();
}

/**
 * Get patterns for a store
 */
export function usePatterns(
  storeId: string,
  seniorityLevel?: SeniorityLevel,
  autoSubmitOnly?: boolean
) {
  return useQuery({
    queryKey: qaKeys.patternsByStore(storeId, seniorityLevel, autoSubmitOnly),
    queryFn: () => fetchPatterns(storeId, seniorityLevel, autoSubmitOnly),
    enabled: !!storeId,
  });
}

/**
 * Get seniority statistics for a store
 */
export function useSeniorityStats(storeId: string) {
  return useQuery({
    queryKey: qaKeys.patternsStats(storeId),
    queryFn: () => fetchSeniorityStats(storeId),
    enabled: !!storeId,
  });
}

/**
 * Promote a pattern to higher seniority
 */
export function usePromotePattern() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: promotePattern,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qaKeys.patterns() });
    },
  });
}

/**
 * Demote a pattern to lower seniority
 */
export function useDemotePattern() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ patternId, reason }: { patternId: string; reason?: string }) =>
      demotePattern(patternId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qaKeys.patterns() });
    },
  });
}

/**
 * Enable auto-submit for a pattern
 */
export function useEnableAutoSubmit() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: enableAutoSubmit,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qaKeys.patterns() });
    },
  });
}

// =====================================================
// CONFLICT ALERTS API & HOOKS
// =====================================================

async function fetchConflicts(
  storeId: string,
  status?: ConflictStatus
): Promise<ConflictAlert[]> {
  const params = new URLSearchParams();
  if (status) {
    params.set("status", status);
  }

  const url = `/api/qa/stores/${storeId}/conflicts${params.toString() ? '?' + params.toString() : ''}`;
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error("Failed to fetch conflicts");
  }

  return response.json();
}

async function fetchConflictStats(storeId: string): Promise<ConflictStats> {
  const response = await fetch(`/api/qa/stores/${storeId}/conflicts/stats`);

  if (!response.ok) {
    throw new Error("Failed to fetch conflict stats");
  }

  return response.json();
}

async function resolveConflict(
  conflictId: string,
  resolutionNotes?: string
): Promise<ConflictAlert> {
  const response = await fetch(`/api/qa/conflicts/${conflictId}/resolve`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ resolutionNotes }),
  });

  if (!response.ok) {
    throw new Error("Failed to resolve conflict");
  }

  return response.json();
}

async function dismissConflict(conflictId: string): Promise<ConflictAlert> {
  const response = await fetch(`/api/qa/conflicts/${conflictId}/dismiss`, {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error("Failed to dismiss conflict");
  }

  return response.json();
}

/**
 * Get conflict alerts for a store
 */
export function useConflicts(storeId: string, status?: ConflictStatus) {
  return useQuery({
    queryKey: qaKeys.conflictsByStore(storeId, status),
    queryFn: () => fetchConflicts(storeId, status),
    enabled: !!storeId,
  });
}

/**
 * Get conflict statistics for a store
 */
export function useConflictStats(storeId: string) {
  return useQuery({
    queryKey: qaKeys.conflictsStats(storeId),
    queryFn: () => fetchConflictStats(storeId),
    enabled: !!storeId,
  });
}

/**
 * Resolve a conflict alert
 */
export function useResolveConflict() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      conflictId,
      resolutionNotes,
    }: {
      conflictId: string;
      resolutionNotes?: string;
    }) => resolveConflict(conflictId, resolutionNotes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qaKeys.conflicts() });
    },
  });
}

/**
 * Dismiss a conflict alert
 */
export function useDismissConflict() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: dismissConflict,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: qaKeys.conflicts() });
    },
  });
}
