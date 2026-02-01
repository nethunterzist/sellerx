import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { webhookApi } from "@/lib/api/client";

// Webhook Query Keys
export const webhookKeys = {
  all: ["webhooks"] as const,
  status: (storeId: string) => [...webhookKeys.all, "status", storeId] as const,
  events: (storeId: string) => [...webhookKeys.all, "events", storeId] as const,
  eventsPaginated: (storeId: string, page: number, size: number, eventType?: string) =>
    [...webhookKeys.events(storeId), { page, size, eventType }] as const,
};

// Get webhook status for a store
export function useWebhookStatus(storeId: string | undefined) {
  return useQuery({
    queryKey: webhookKeys.status(storeId || ""),
    queryFn: () => webhookApi.getStatus(storeId!),
    enabled: !!storeId,
  });
}

// Get webhook events for a store (paginated)
export function useWebhookEvents(
  storeId: string | undefined,
  page = 0,
  size = 20,
  eventType?: string
) {
  return useQuery({
    queryKey: webhookKeys.eventsPaginated(storeId || "", page, size, eventType),
    queryFn: () => webhookApi.getEvents(storeId!, page, size, eventType),
    enabled: !!storeId,
  });
}

// Enable webhooks mutation
export function useEnableWebhooks() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (storeId: string) => webhookApi.enable(storeId),
    onSuccess: (data, storeId) => {
      // Invalidate webhook status
      queryClient.invalidateQueries({ queryKey: webhookKeys.status(storeId) });
    },
  });
}

// Disable webhooks mutation
export function useDisableWebhooks() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (storeId: string) => webhookApi.disable(storeId),
    onSuccess: (data, storeId) => {
      // Invalidate webhook status
      queryClient.invalidateQueries({ queryKey: webhookKeys.status(storeId) });
    },
  });
}

// Test webhook mutation
export function useTestWebhook() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (storeId: string) => webhookApi.test(storeId),
    onSuccess: (data, storeId) => {
      // Invalidate webhook events to show the test event
      queryClient.invalidateQueries({ queryKey: webhookKeys.events(storeId) });
      queryClient.invalidateQueries({ queryKey: webhookKeys.status(storeId) });
    },
  });
}
