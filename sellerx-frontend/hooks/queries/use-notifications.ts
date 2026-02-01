import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/api/client";
import type { Notification } from "@/types/notification";

// Notification Query Keys
export const notificationKeys = {
  all: ["notifications"] as const,
  list: () => [...notificationKeys.all, "list"] as const,
  unreadCount: () => [...notificationKeys.all, "unreadCount"] as const,
};

// Get all notifications
export function useNotifications() {
  return useQuery<Notification[]>({
    queryKey: notificationKeys.list(),
    queryFn: () => apiRequest<Notification[]>("/notifications"),
  });
}

// Get unread count
export function useUnreadCount() {
  return useQuery<number>({
    queryKey: notificationKeys.unreadCount(),
    queryFn: () => apiRequest<number>("/notifications/unread-count"),
  });
}

// Mark notification as read
export function useMarkAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) =>
      apiRequest(`/notifications/${id}/read`, {
        method: "PUT",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
