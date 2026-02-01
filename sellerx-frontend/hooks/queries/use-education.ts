import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/api/client";
import type { EducationVideo, VideoCategory, VideoWatchStatus } from "@/types/education";

// Education Query Keys
export const educationKeys = {
  all: ["education"] as const,
  videos: () => [...educationKeys.all, "videos"] as const,
  video: (id: string) => [...educationKeys.videos(), id] as const,
  category: (category: VideoCategory) => [...educationKeys.videos(), "category", category] as const,
  watchStatus: () => [...educationKeys.all, "watchStatus"] as const,
};

// Get all videos
export function useEducationVideos() {
  return useQuery<EducationVideo[]>({
    queryKey: educationKeys.videos(),
    queryFn: () => apiRequest<EducationVideo[]>("/education/videos"),
  });
}

// Get video by ID
export function useEducationVideo(id: string | undefined) {
  return useQuery<EducationVideo>({
    queryKey: educationKeys.video(id || ""),
    queryFn: () => apiRequest<EducationVideo>(`/education/videos/${id}`),
    enabled: !!id,
  });
}

// Get videos by category
export function useVideosByCategory(category: VideoCategory) {
  return useQuery<EducationVideo[]>({
    queryKey: educationKeys.category(category),
    queryFn: () => apiRequest<EducationVideo[]>(`/education/videos/category/${category}`),
  });
}

// Get user watch status
export function useMyWatchStatus() {
  return useQuery<VideoWatchStatus>({
    queryKey: educationKeys.watchStatus(),
    queryFn: () => apiRequest<VideoWatchStatus>("/education/videos/my-watch-status"),
  });
}

// Mark video as watched
export function useMarkVideoAsWatched() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (videoId: string) =>
      apiRequest(`/education/videos/${videoId}/watch`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: educationKeys.watchStatus() });
    },
  });
}

// Admin: Create video
export function useCreateVideo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: {
      title: string;
      description?: string;
      category: VideoCategory;
      duration: string;
      videoUrl: string;
      thumbnailUrl?: string;
      videoType: "YOUTUBE" | "UPLOADED";
      order: number;
      isActive?: boolean;
    }) =>
      apiRequest<EducationVideo>("/education/videos", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: educationKeys.videos() });
    },
  });
}

// Admin: Update video
export function useUpdateVideo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      ...data
    }: {
      id: string;
      title?: string;
      description?: string;
      category?: VideoCategory;
      duration?: string;
      videoUrl?: string;
      thumbnailUrl?: string;
      videoType?: "YOUTUBE" | "UPLOADED";
      order?: number;
      isActive?: boolean;
    }) =>
      apiRequest<EducationVideo>(`/education/videos/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: educationKeys.videos() });
      queryClient.invalidateQueries({ queryKey: educationKeys.video(variables.id) });
    },
  });
}

// Admin: Delete video
export function useDeleteVideo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) =>
      apiRequest(`/education/videos/${id}`, {
        method: "DELETE",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: educationKeys.videos() });
    },
  });
}
