"use client";

import { useState, useEffect, useMemo } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  useEducationVideos,
  useVideosByCategory,
  useEducationVideo,
  useMyWatchStatus,
  useMarkVideoAsWatched,
} from "@/hooks/queries/use-education";
import { EducationVideo, VideoCategory } from "@/types/education";
import { VideoPlayerModal } from "@/components/education/video-player-modal";
import { cn } from "@/lib/utils";
import { PlayCircle, Clock, CheckCircle, Filter, GraduationCap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";

const videoCategories: { id: VideoCategory | 'all'; labelKey: string }[] = [
  { id: 'all', labelKey: 'education.categories.all' },
  { id: 'GETTING_STARTED', labelKey: 'education.categories.getting-started' },
  { id: 'PRODUCTS', labelKey: 'education.categories.products' },
  { id: 'ORDERS', labelKey: 'education.categories.orders' },
  { id: 'ANALYTICS', labelKey: 'education.categories.analytics' },
  { id: 'SETTINGS', labelKey: 'education.categories.settings' },
];

function EducationPageSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <div className="flex gap-2 flex-wrap">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-20 rounded-full" />
        ))}
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <Card key={i}>
            <Skeleton className="h-40 w-full rounded-t-lg" />
            <CardContent className="p-4 space-y-2">
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-1/2" />
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

export default function EducationPage() {
  const t = useTranslations("education");
  const searchParams = useSearchParams();
  const videoIdFromUrl = searchParams.get("video");

  const [selectedCategory, setSelectedCategory] = useState<VideoCategory | "all">("all");
  const [selectedVideo, setSelectedVideo] = useState<EducationVideo | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Fetch data from API
  const { data: allVideos = [], isLoading: isLoadingAll } = useEducationVideos();
  const { data: categoryVideos = [], isLoading: isLoadingCategory } = useVideosByCategory(
    selectedCategory as VideoCategory
  );
  const { data: watchStatus, isLoading: isLoadingWatchStatus } = useMyWatchStatus();
  const { data: videoFromUrl } = useEducationVideo(videoIdFromUrl || undefined);
  const markAsWatched = useMarkVideoAsWatched();

  // Combine videos with watch status
  const videos = useMemo(() => {
    const videoList = selectedCategory === "all" ? allVideos : categoryVideos;
    const watchedIds = new Set(watchStatus?.watchedVideoIds || []);

    return videoList.map((video) => ({
      ...video,
      watched: watchedIds.has(video.id),
    }));
  }, [selectedCategory, allVideos, categoryVideos, watchStatus]);

  const isLoading = selectedCategory === "all" ? isLoadingAll : isLoadingCategory || isLoadingWatchStatus;

  // Open video from URL parameter
  useEffect(() => {
    if (videoFromUrl && videoIdFromUrl) {
      const watchedIds = new Set(watchStatus?.watchedVideoIds || []);
      setSelectedVideo({
        ...videoFromUrl,
        watched: watchedIds.has(videoFromUrl.id),
      });
      setIsModalOpen(true);
    }
  }, [videoFromUrl, videoIdFromUrl, watchStatus]);

  const handleVideoClick = (video: EducationVideo) => {
    setSelectedVideo(video);
    setIsModalOpen(true);
    // Mark as watched when opened
    if (!video.watched) {
      markAsWatched.mutate(video.id);
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedVideo(null);
  };

  return (
    <div className="min-h-screen bg-muted p-6">
      {/* Page Header */}
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <div>
            <p className="text-sm text-muted-foreground">{t("subtitle")}</p>
          </div>
        </div>
      </div>

      {/* Category Filters */}
      <div className="mb-6">
        <div className="flex items-center gap-2 flex-wrap">
          <Filter className="h-4 w-4 text-muted-foreground" />
          {videoCategories.map((category) => (
            <Button
              key={category.id}
              variant="ghost"
              size="sm"
              onClick={() => setSelectedCategory(category.id)}
              className={cn(
                "rounded-full px-4 py-1.5 text-sm transition-colors",
                selectedCategory === category.id
                  ? "bg-[#1D70F1] text-white hover:bg-[#1560d9] hover:text-white"
                  : "bg-card text-muted-foreground hover:bg-muted hover:text-foreground border border-border"
              )}
            >
              {t(`categories.${category.id}`)}
            </Button>
          ))}
        </div>
      </div>

      {/* Video Grid */}
      {isLoading ? (
        <EducationPageSkeleton />
      ) : (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {videos.map((video) => (
          <div
            key={video.id}
            onClick={() => handleVideoClick(video)}
            className={cn(
              "group cursor-pointer rounded-xl bg-card border border-border overflow-hidden shadow-sm hover:shadow-md transition-all duration-200",
              video.watched && "opacity-75"
            )}
          >
            {/* Thumbnail */}
            <div className="relative aspect-video bg-muted">
              {video.thumbnailUrl ? (
                <img
                  src={video.thumbnailUrl}
                  alt={video.title}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-muted">
                  <PlayCircle className="h-12 w-12 text-muted-foreground" />
                </div>
              )}
              {/* Play overlay */}
              <div className="absolute inset-0 flex items-center justify-center bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity">
                <div className="flex h-14 w-14 items-center justify-center rounded-full bg-white/90">
                  <PlayCircle className="h-8 w-8 text-[#1D70F1]" />
                </div>
              </div>
              {/* Duration badge */}
              <div className="absolute bottom-2 right-2 flex items-center gap-1 rounded bg-black/70 px-2 py-0.5 text-xs text-white">
                <Clock className="h-3 w-3" />
                {video.duration}
              </div>
              {/* Watched badge */}
              {video.watched && (
                <div className="absolute top-2 right-2 flex items-center gap-1 rounded-full bg-green-500 px-2 py-0.5 text-xs text-white">
                  <CheckCircle className="h-3 w-3" />
                  {t("watched")}
                </div>
              )}
            </div>

            {/* Content */}
            <div className="p-4">
              <h3 className={cn(
                "text-sm mb-1 line-clamp-2",
                !video.watched && "font-medium"
              )}>
                {video.title}
              </h3>
              <p className="text-xs text-muted-foreground line-clamp-2">
                {video.description}
              </p>
            </div>
          </div>
        ))}
      </div>
      )}

      {/* Empty State */}
      {videos.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <GraduationCap className="h-12 w-12 text-muted-foreground mb-4" />
          <p className="text-muted-foreground">{t("noVideos")}</p>
        </div>
      )}

      {/* Video Player Modal */}
      <VideoPlayerModal
        video={selectedVideo}
        isOpen={isModalOpen}
        onClose={handleCloseModal}
      />
    </div>
  );
}
