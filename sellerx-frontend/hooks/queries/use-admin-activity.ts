import { useQuery } from "@tanstack/react-query";

export function useAdminActivityLogs(params?: {
  email?: string;
  action?: string;
  page?: number;
}) {
  const searchParams = new URLSearchParams();
  if (params?.email) searchParams.set("email", params.email);
  if (params?.action) searchParams.set("action", params.action);
  if (params?.page !== undefined) searchParams.set("page", String(params.page));
  searchParams.set("size", "20");

  return useQuery({
    queryKey: ["admin", "activity-logs", params],
    queryFn: () =>
      fetch(`/api/admin/activity-logs?${searchParams}`).then((r) => r.json()),
  });
}

export function useAdminSecuritySummary() {
  return useQuery({
    queryKey: ["admin", "security", "summary"],
    queryFn: () =>
      fetch("/api/admin/activity-logs/security").then((r) => r.json()),
  });
}
