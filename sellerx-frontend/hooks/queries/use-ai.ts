import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { aiSettingsApi, knowledgeApi, qaApi } from "@/lib/api/client";
import type { AiSettings, KnowledgeBaseItem, CreateKnowledgeRequest, AiApproveRequest } from "@/types/ai";

// Query Keys
export const aiKeys = {
  all: ["ai"] as const,
  settings: (storeId: string) => [...aiKeys.all, "settings", storeId] as const,
  knowledge: (storeId: string) => [...aiKeys.all, "knowledge", storeId] as const,
};

export const qaKeys = {
  all: ["qa"] as const,
  questions: (storeId: string) => [...qaKeys.all, "questions", storeId] as const,
  questionsPaginated: (storeId: string, page: number, status?: string) =>
    [...qaKeys.questions(storeId), { page, status }] as const,
  question: (id: string) => [...qaKeys.all, "question", id] as const,
  stats: (storeId: string) => [...qaKeys.all, "stats", storeId] as const,
};

// AI Settings Hooks
export function useAiSettings(storeId: string | undefined) {
  return useQuery({
    queryKey: aiKeys.settings(storeId!),
    queryFn: () => aiSettingsApi.get(storeId!),
    enabled: !!storeId,
  });
}

export function useUpdateAiSettings() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, data }: { storeId: string; data: Partial<AiSettings> }) =>
      aiSettingsApi.update(storeId, data),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(aiKeys.settings(variables.storeId), data);
    },
  });
}

// Knowledge Base Hooks
export function useKnowledgeBase(storeId: string | undefined) {
  return useQuery({
    queryKey: aiKeys.knowledge(storeId!),
    queryFn: () => knowledgeApi.getAll(storeId!),
    enabled: !!storeId,
  });
}

export function useCreateKnowledge() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, data }: { storeId: string; data: CreateKnowledgeRequest }) =>
      knowledgeApi.create(storeId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: aiKeys.knowledge(variables.storeId) });
    },
  });
}

export function useUpdateKnowledge() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data, storeId }: { id: string; data: CreateKnowledgeRequest; storeId: string }) =>
      knowledgeApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: aiKeys.knowledge(variables.storeId) });
    },
  });
}

export function useDeleteKnowledge() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, storeId }: { id: string; storeId: string }) =>
      knowledgeApi.delete(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: aiKeys.knowledge(variables.storeId) });
    },
  });
}

export function useToggleKnowledge() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, active, storeId }: { id: string; active: boolean; storeId: string }) =>
      knowledgeApi.toggle(id, active),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: aiKeys.knowledge(variables.storeId) });
    },
  });
}

// Q&A Hooks
export function useQuestions(storeId: string | undefined, page = 0, size = 20, status?: string) {
  return useQuery({
    queryKey: qaKeys.questionsPaginated(storeId!, page, status),
    queryFn: () => qaApi.getQuestions(storeId!, page, size, status),
    enabled: !!storeId,
  });
}

export function useQuestion(questionId: string | undefined) {
  return useQuery({
    queryKey: qaKeys.question(questionId!),
    queryFn: () => qaApi.getQuestion(questionId!),
    enabled: !!questionId,
  });
}

export function useQaStats(storeId: string | undefined) {
  return useQuery({
    queryKey: qaKeys.stats(storeId!),
    queryFn: () => qaApi.getStats(storeId!),
    enabled: !!storeId,
  });
}

export function useSyncQuestions() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (storeId: string) => qaApi.syncQuestions(storeId),
    onSuccess: (_, storeId) => {
      queryClient.invalidateQueries({ queryKey: qaKeys.questions(storeId) });
      queryClient.invalidateQueries({ queryKey: qaKeys.stats(storeId) });
    },
  });
}

// AI Answer Generation Hooks
export function useGenerateAiAnswer() {
  return useMutation({
    mutationFn: (questionId: string) => qaApi.generateAiAnswer(questionId),
  });
}

export function useApproveAiAnswer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ questionId, data }: { questionId: string; data: AiApproveRequest }) =>
      qaApi.approveAiAnswer(questionId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: qaKeys.question(variables.questionId) });
      // Also invalidate questions list as status may have changed
      queryClient.invalidateQueries({ queryKey: qaKeys.all });
    },
  });
}
