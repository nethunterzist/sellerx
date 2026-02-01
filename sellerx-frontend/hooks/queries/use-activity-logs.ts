import { useQuery } from "@tanstack/react-query";
import { userApi, type ActivityLogEntry } from "@/lib/api/client";

// Activity Logs Query Keys
export const activityLogKeys = {
  all: ["activityLogs"] as const,
  list: (limit?: number) => [...activityLogKeys.all, "list", limit] as const,
};

// Get activity logs
export function useActivityLogs(limit = 20) {
  return useQuery<ActivityLogEntry[]>({
    queryKey: activityLogKeys.list(limit),
    queryFn: () => userApi.getActivityLogs(limit),
    staleTime: 30 * 1000, // 30 seconds
  });
}
