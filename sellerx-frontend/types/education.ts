export type VideoCategory =
  | 'GETTING_STARTED'
  | 'PRODUCTS'
  | 'ORDERS'
  | 'ANALYTICS'
  | 'SETTINGS';

export type VideoType = 'YOUTUBE' | 'UPLOADED';

export interface EducationVideo {
  id: string;
  title: string;
  description: string;
  category: VideoCategory;
  duration: string; // Format: "5:30"
  thumbnailUrl?: string;
  videoUrl: string; // YouTube embed URL or file path
  videoType: VideoType;
  order: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  watched?: boolean; // Frontend'de hesaplanacak
}

export interface VideoWatchStatus {
  watchedVideoIds: string[];
}

export interface VideoCategoryInfo {
  id: VideoCategory;
  label: string;
}
