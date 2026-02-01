"use client";

import { useState } from "react";
import { Plus, Video, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useEducationVideos } from "@/hooks/queries/use-education";
import { VideoList } from "@/components/admin/education/video-list";
import { VideoFormModal } from "@/components/admin/education/video-form-modal";
import type { EducationVideo } from "@/types/education";

export default function AdminEducationPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVideo, setEditingVideo] = useState<EducationVideo | null>(null);

  const { data: videos = [], isLoading, refetch, isRefetching } = useEducationVideos();

  const handleEdit = (video: EducationVideo) => {
    setEditingVideo(video);
    setIsModalOpen(true);
  };

  const handleCreate = () => {
    setEditingVideo(null);
    setIsModalOpen(true);
  };

  const handleModalClose = (open: boolean) => {
    setIsModalOpen(open);
    if (!open) {
      setEditingVideo(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Eğitim Videoları Yönetimi</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Eğitim videolarını ekleyin, düzenleyin ve yönetin
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isRefetching}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${isRefetching ? "animate-spin" : ""}`} />
            Yenile
          </Button>
          <Button onClick={handleCreate}>
            <Plus className="h-4 w-4 mr-2" />
            Video Ekle
          </Button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-card rounded-lg border border-border p-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-blue-100 dark:bg-blue-900/30">
              <Video className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Toplam Video</p>
              <p className="text-2xl font-semibold">{videos.length}</p>
            </div>
          </div>
        </div>
        <div className="bg-card rounded-lg border border-border p-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-green-100 dark:bg-green-900/30">
              <Video className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Aktif</p>
              <p className="text-2xl font-semibold">
                {videos.filter((v) => v.isActive).length}
              </p>
            </div>
          </div>
        </div>
        <div className="bg-card rounded-lg border border-border p-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-gray-100 dark:bg-gray-900/30">
              <Video className="h-5 w-5 text-gray-600 dark:text-gray-400" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Pasif</p>
              <p className="text-2xl font-semibold">
                {videos.filter((v) => !v.isActive).length}
              </p>
            </div>
          </div>
        </div>
        <div className="bg-card rounded-lg border border-border p-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-purple-100 dark:bg-purple-900/30">
              <Video className="h-5 w-5 text-purple-600 dark:text-purple-400" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">YouTube</p>
              <p className="text-2xl font-semibold">
                {videos.filter((v) => v.videoType === "YOUTUBE").length}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Video List */}
      {isLoading ? (
        <div className="flex items-center justify-center py-16">
          <div className="flex items-center gap-2 text-muted-foreground">
            <RefreshCw className="h-5 w-5 animate-spin" />
            Yükleniyor...
          </div>
        </div>
      ) : (
        <VideoList videos={videos} onEdit={handleEdit} />
      )}

      {/* Video Form Modal */}
      <VideoFormModal
        video={editingVideo}
        open={isModalOpen}
        onOpenChange={handleModalClose}
      />
    </div>
  );
}
