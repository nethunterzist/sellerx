import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { userApi } from "@/lib/api/client";
import { authKeys } from "./use-auth";
import type {
  UpdateProfileRequest,
  ChangePasswordRequest,
  UserPreferences,
  UpdatePreferencesRequest,
} from "@/types/user";

// Settings Query Keys
export const settingsKeys = {
  all: ["settings"] as const,
  profile: () => [...settingsKeys.all, "profile"] as const,
  preferences: () => [...settingsKeys.all, "preferences"] as const,
};

// Get user profile
export function useUserProfile() {
  return useQuery({
    queryKey: settingsKeys.profile(),
    queryFn: userApi.getProfile,
    staleTime: 5 * 60 * 1000, // 5 dakika
  });
}

// Update user profile
export function useUpdateProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateProfileRequest) => userApi.updateProfile(data),
    onSuccess: (data) => {
      // Update profile cache
      queryClient.setQueryData(settingsKeys.profile(), data);
      // Also update auth cache if user data changed
      queryClient.invalidateQueries({ queryKey: authKeys.me() });
    },
  });
}

// Change password
export function useChangePassword() {
  return useMutation({
    mutationFn: (data: ChangePasswordRequest) => userApi.changePassword(data),
  });
}

// Get user preferences
export function useUserPreferences() {
  return useQuery<UserPreferences>({
    queryKey: settingsKeys.preferences(),
    queryFn: userApi.getPreferences,
    staleTime: 10 * 60 * 1000, // 10 dakika
  });
}

// Update user preferences
export function useUpdatePreferences() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdatePreferencesRequest) => userApi.updatePreferences(data),
    onSuccess: (data) => {
      // Update preferences cache
      queryClient.setQueryData(settingsKeys.preferences(), data);
    },
  });
}
