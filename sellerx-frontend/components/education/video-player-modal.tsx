"use client";

import { EducationVideo } from "@/types/education";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Clock, Tag } from "lucide-react";
import { useTranslations } from "next-intl";

interface VideoPlayerModalProps {
  video: EducationVideo | null;
  isOpen: boolean;
  onClose: () => void;
}

export function VideoPlayerModal({ video, isOpen, onClose }: VideoPlayerModalProps) {
  const t = useTranslations("education");

  if (!video) return null;

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-3xl p-0 overflow-hidden">
        {/* Video Player */}
        <div className="aspect-video w-full bg-black">
          <iframe
            src={`${video.videoUrl}?autoplay=1`}
            title={video.title}
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
            className="w-full h-full"
          />
        </div>

        {/* Video Info */}
        <div className="p-4">
          <DialogHeader>
            <DialogTitle className="text-lg">{video.title}</DialogTitle>
            <DialogDescription className="text-sm text-gray-600">
              {video.description}
            </DialogDescription>
          </DialogHeader>

          {/* Meta Info */}
          <div className="flex items-center gap-4 mt-4 text-sm text-gray-500">
            <div className="flex items-center gap-1.5">
              <Clock className="h-4 w-4" />
              <span>{video.duration}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Tag className="h-4 w-4" />
              <span>{t(`categories.${video.category}`)}</span>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
