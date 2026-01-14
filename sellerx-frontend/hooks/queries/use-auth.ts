import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/lib/api/auth";

// Auth Query Keys
export const authKeys = {
  all: ["auth"] as const,
  me: () => [...authKeys.all, "me"] as const,
} as const;

// Auth query hook with aggressive caching and deduplication
export function useMe() {
  return useQuery({
    queryKey: authKeys.me(),
    queryFn: authApi.me,
    staleTime: 10 * 60 * 1000, // accept as fresh for 10 minutes
    gcTime: 15 * 60 * 1000, // keep in cache for 15 minutes
    retry: (failureCount, error: any) => {
      // In case of 401 Unauthorized, do not retry
      if (
        error?.message?.includes("401") ||
        error?.message?.includes("Unauthorized")
      ) {
        return false;
      }
      return failureCount < 3;
    },
    refetchOnWindowFocus: false, // Do not refetch on window focus
    refetchOnMount: false, // Do not refetch on mount (if cache exists)
    refetchOnReconnect: false, // Do not refetch on reconnect
  });
}

// Refresh token mutation
export function useRefreshAuth() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: authApi.refresh,
    onSuccess: (data) => {
      // Update auth cache
      queryClient.setQueryData(authKeys.me(), data);
    },
    onError: () => {
      // If refresh fails, clear auth cache
      queryClient.removeQueries({ queryKey: authKeys.me() });
    },
  });
}

// Login mutation
export function useLogin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      // Update auth cache on successful login
      queryClient.setQueryData(authKeys.me(), data.user || data);
    },
  });
}

// Register mutation
export function useRegister() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: authApi.register,
    onSuccess: (data) => {
      // Update auth cache on successful registration
      queryClient.setQueryData(authKeys.me(), data.user || data);
    },
  });
}

// Logout mutation
export function useLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: authApi.logout,
    onSettled: () => {
      // When logout is complete, clear all cache
      queryClient.clear();
    },
  });
}
