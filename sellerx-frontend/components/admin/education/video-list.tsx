"use client";

import { useState } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { MoreHorizontal, Pencil, Trash2, Play, ExternalLink, Clock } from "lucide-react";
import { useDeleteVideo } from "@/hooks/queries/use-education";
import type { EducationVideo, VideoCategory } from "@/types/education";

const categoryLabels: Record<VideoCategory, string> = {
  GETTING_STARTED: "Başlangıç",
  PRODUCTS: "Ürünler",
  ORDERS: "Siparişler",
  ANALYTICS: "Analitik",
  SETTINGS: "Ayarlar",
};

const categoryColors: Record<VideoCategory, string> = {
  GETTING_STARTED: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  PRODUCTS: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  ORDERS: "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400",
  ANALYTICS: "bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400",
  SETTINGS: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
};

interface VideoListProps {
  videos: EducationVideo[];
  onEdit: (video: EducationVideo) => void;
}

export function VideoList({ videos, onEdit }: VideoListProps) {
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const deleteMutation = useDeleteVideo();

  const handleDelete = () => {
    if (deleteId) {
      deleteMutation.mutate(deleteId, {
        onSuccess: () => {
          setDeleteId(null);
        },
      });
    }
  };

  const sortedVideos = [...videos].sort((a, b) => a.order - b.order);

  return (
    <>
      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[60px]">Sıra</TableHead>
              <TableHead>Başlık</TableHead>
              <TableHead className="w-[120px]">Kategori</TableHead>
              <TableHead className="w-[80px]">Süre</TableHead>
              <TableHead className="w-[100px]">Tip</TableHead>
              <TableHead className="w-[80px]">Durum</TableHead>
              <TableHead className="w-[100px] text-right">İşlemler</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sortedVideos.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                  Henüz video eklenmemiş
                </TableCell>
              </TableRow>
            ) : (
              sortedVideos.map((video) => (
                <TableRow key={video.id}>
                  <TableCell className="font-medium">{video.order}</TableCell>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      {video.thumbnailUrl ? (
                        <img
                          src={video.thumbnailUrl}
                          alt={video.title}
                          className="w-16 h-10 rounded object-cover"
                        />
                      ) : (
                        <div className="w-16 h-10 rounded bg-muted flex items-center justify-center">
                          <Play className="h-4 w-4 text-muted-foreground" />
                        </div>
                      )}
                      <div>
                        <p className="font-medium line-clamp-1">{video.title}</p>
                        {video.description && (
                          <p className="text-sm text-muted-foreground line-clamp-1">
                            {video.description}
                          </p>
                        )}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge className={categoryColors[video.category]} variant="outline">
                      {categoryLabels[video.category]}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1 text-muted-foreground">
                      <Clock className="h-3 w-3" />
                      {video.duration}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="secondary">
                      {video.videoType === "YOUTUBE" ? "YouTube" : "Yüklenen"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {video.isActive ? (
                      <Badge className="bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
                        Aktif
                      </Badge>
                    ) : (
                      <Badge variant="secondary">Pasif</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={() => window.open(video.videoUrl, "_blank")}
                        >
                          <ExternalLink className="mr-2 h-4 w-4" />
                          Videoyu Aç
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onEdit(video)}>
                          <Pencil className="mr-2 h-4 w-4" />
                          Düzenle
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          className="text-destructive focus:text-destructive"
                          onClick={() => setDeleteId(video.id)}
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          Sil
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <AlertDialog open={!!deleteId} onOpenChange={(open) => !open && setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Video Sil</AlertDialogTitle>
            <AlertDialogDescription>
              Bu videoyu silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>İptal</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? "Siliniyor..." : "Sil"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
