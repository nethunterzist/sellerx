"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Loader2, Video } from "lucide-react";
import { useCreateVideo, useUpdateVideo } from "@/hooks/queries/use-education";
import type { EducationVideo, VideoCategory, VideoType } from "@/types/education";

const videoCategories: { value: VideoCategory; label: string }[] = [
  { value: "GETTING_STARTED", label: "Başlangıç" },
  { value: "PRODUCTS", label: "Ürünler" },
  { value: "ORDERS", label: "Siparişler" },
  { value: "ANALYTICS", label: "Analitik" },
  { value: "SETTINGS", label: "Ayarlar" },
];

const videoTypes: { value: VideoType; label: string }[] = [
  { value: "YOUTUBE", label: "YouTube" },
  { value: "UPLOADED", label: "Yüklenen Video" },
];

const formSchema = z.object({
  title: z.string().min(1, "Başlık gereklidir"),
  description: z.string().optional(),
  category: z.enum(["GETTING_STARTED", "PRODUCTS", "ORDERS", "ANALYTICS", "SETTINGS"]),
  videoUrl: z.string().url("Geçerli bir URL giriniz"),
  thumbnailUrl: z.string().url("Geçerli bir URL giriniz").optional().or(z.literal("")),
  videoType: z.enum(["YOUTUBE", "UPLOADED"]),
  duration: z.string().regex(/^\d{1,2}:\d{2}$/, "Format: 5:30"),
  order: z.coerce.number().min(0, "Sıra 0 veya daha büyük olmalı"),
  isActive: z.boolean(),
});

type FormValues = z.infer<typeof formSchema>;

interface VideoFormModalProps {
  video?: EducationVideo | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function VideoFormModal({
  video,
  open,
  onOpenChange,
}: VideoFormModalProps) {
  const isEditing = !!video;
  const createMutation = useCreateVideo();
  const updateMutation = useUpdateVideo();

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      title: "",
      description: "",
      category: "GETTING_STARTED",
      videoUrl: "",
      thumbnailUrl: "",
      videoType: "YOUTUBE",
      duration: "0:00",
      order: 0,
      isActive: true,
    },
  });

  // Reset form when modal opens or video changes
  useEffect(() => {
    if (open) {
      if (video) {
        form.reset({
          title: video.title || "",
          description: video.description || "",
          category: video.category,
          videoUrl: video.videoUrl || "",
          thumbnailUrl: video.thumbnailUrl || "",
          videoType: video.videoType || "YOUTUBE",
          duration: video.duration || "0:00",
          order: video.order || 0,
          isActive: video.isActive ?? true,
        });
      } else {
        form.reset({
          title: "",
          description: "",
          category: "GETTING_STARTED",
          videoUrl: "",
          thumbnailUrl: "",
          videoType: "YOUTUBE",
          duration: "0:00",
          order: 0,
          isActive: true,
        });
      }
    }
  }, [open, video, form]);

  const onSubmit = (values: FormValues) => {
    const data = {
      title: values.title,
      description: values.description || undefined,
      category: values.category as VideoCategory,
      videoUrl: values.videoUrl,
      thumbnailUrl: values.thumbnailUrl || undefined,
      videoType: values.videoType as VideoType,
      duration: values.duration,
      order: values.order,
      isActive: values.isActive,
    };

    if (isEditing && video) {
      updateMutation.mutate(
        { id: video.id, ...data },
        {
          onSuccess: () => {
            onOpenChange(false);
          },
        }
      );
    } else {
      createMutation.mutate(data, {
        onSuccess: () => {
          onOpenChange(false);
        },
      });
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Video className="h-5 w-5" />
            {isEditing ? "Video Düzenle" : "Yeni Video Ekle"}
          </DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Video bilgilerini güncelleyin"
              : "Eğitim için yeni bir video ekleyin"}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="title"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Başlık</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="Video başlığı"
                      className="border border-gray-300 dark:border-gray-600 rounded-lg"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Açıklama</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Video açıklaması..."
                      className="resize-none border border-gray-300 dark:border-gray-600 rounded-lg"
                      rows={2}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="category"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Kategori</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger className="border border-gray-300 dark:border-gray-600 rounded-lg">
                          <SelectValue placeholder="Kategori seçin" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {videoCategories.map((cat) => (
                          <SelectItem key={cat.value} value={cat.value}>
                            {cat.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="videoType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Video Tipi</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger className="border border-gray-300 dark:border-gray-600 rounded-lg">
                          <SelectValue placeholder="Tip seçin" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {videoTypes.map((type) => (
                          <SelectItem key={type.value} value={type.value}>
                            {type.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="videoUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Video URL</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="https://www.youtube.com/embed/..."
                      className="border border-gray-300 dark:border-gray-600 rounded-lg"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="thumbnailUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Thumbnail URL (Opsiyonel)</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="https://img.youtube.com/vi/.../maxresdefault.jpg"
                      className="border border-gray-300 dark:border-gray-600 rounded-lg"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="duration"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Süre (dakika:saniye)</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="5:30"
                        className="border border-gray-300 dark:border-gray-600 rounded-lg"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="order"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Sıra</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min="0"
                        className="border border-gray-300 dark:border-gray-600 rounded-lg"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="isActive"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border border-gray-300 dark:border-gray-600 p-3">
                  <div className="space-y-0.5">
                    <FormLabel>Aktif</FormLabel>
                    <p className="text-sm text-muted-foreground">
                      Video kullanıcılara gösterilsin mi?
                    </p>
                  </div>
                  <FormControl>
                    <Switch
                      checked={field.value}
                      onCheckedChange={field.onChange}
                    />
                  </FormControl>
                </FormItem>
              )}
            />

            <div className="flex justify-end gap-2 pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isPending}
              >
                İptal
              </Button>
              <Button type="submit" disabled={isPending}>
                {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {isPending ? "Kaydediliyor..." : isEditing ? "Güncelle" : "Ekle"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
