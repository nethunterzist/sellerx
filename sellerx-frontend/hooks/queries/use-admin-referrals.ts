import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

export function useAdminReferrals(page = 0) {
  return useQuery({
    queryKey: ["admin", "referrals", page],
    queryFn: () =>
      fetch(`/api/admin/referrals?page=${page}&size=20`).then((r) => r.json()),
  });
}

export function useAdminReferralStats() {
  return useQuery({
    queryKey: ["admin", "referrals", "stats"],
    queryFn: () =>
      fetch("/api/admin/referrals/stats").then((r) => r.json()),
  });
}

export function useAdminNotificationStats() {
  return useQuery({
    queryKey: ["admin", "notifications", "stats"],
    queryFn: () =>
      fetch("/api/admin/notifications/stats").then((r) => r.json()),
  });
}

export function useBroadcastNotification() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: {
      type: string;
      title: string;
      message: string;
      link?: string;
    }) =>
      fetch("/api/admin/notifications/broadcast", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      }).then((r) => r.json()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "notifications"] });
    },
  });
}
