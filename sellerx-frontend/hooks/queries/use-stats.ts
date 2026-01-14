import { useQuery } from "@tanstack/react-query";
import { statApi } from "@/lib/api/client";

// Stat Query Keys
export const statKeys = {
  all: ["stats"] as const,
  lists: () => [...statKeys.all, "list"] as const,
  list: (filters: Record<string, any>) =>
    [...statKeys.lists(), { filters }] as const,
  details: () => [...statKeys.all, "detail"] as const,
  detail: (id: string) => [...statKeys.details(), id] as const,
};

// Get all stats
export function useStats() {
  return useQuery({
    queryKey: statKeys.lists(),
    queryFn: statApi.getAll,
  });
}
