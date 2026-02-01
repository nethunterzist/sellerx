"use client";

import { useCallback, useState } from "react";
import type { Attachment } from "@/types/purchasing";
import {
  usePOAttachments,
  useUploadAttachment,
  useDeleteAttachment,
} from "@/hooks/queries/use-purchasing";
import { Button } from "@/components/ui/button";
import {
  Upload,
  Loader2,
  Trash2,
  Download,
  FileText,
  FileImage,
  FileSpreadsheet,
  File,
} from "lucide-react";
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

interface POAttachmentsTabProps {
  storeId: string;
  poId: number;
  disabled?: boolean;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_TYPES = [
  "application/pdf",
  "image/jpeg",
  "image/png",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
];
const ALLOWED_EXTENSIONS = ".pdf,.jpg,.jpeg,.png,.xlsx,.docx";

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function getFileIcon(fileType?: string) {
  if (!fileType) return <File className="h-5 w-5 text-muted-foreground" />;
  if (fileType.includes("pdf"))
    return <FileText className="h-5 w-5 text-red-500" />;
  if (fileType.includes("image"))
    return <FileImage className="h-5 w-5 text-blue-500" />;
  if (fileType.includes("spreadsheet") || fileType.includes("excel"))
    return <FileSpreadsheet className="h-5 w-5 text-green-500" />;
  return <File className="h-5 w-5 text-muted-foreground" />;
}

export function POAttachmentsTab({
  storeId,
  poId,
  disabled,
}: POAttachmentsTabProps) {
  const [deleteTarget, setDeleteTarget] = useState<Attachment | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const { data: attachments, isLoading } = usePOAttachments(storeId, poId);
  const uploadMutation = useUploadAttachment();
  const deleteMutation = useDeleteAttachment();

  const handleUpload = useCallback(
    (files: FileList | null) => {
      if (!files || files.length === 0) return;
      setUploadError(null);

      const file = files[0];
      if (file.size > MAX_FILE_SIZE) {
        setUploadError("Dosya boyutu 10MB'den büyük olamaz.");
        return;
      }
      if (!ALLOWED_TYPES.includes(file.type)) {
        setUploadError(
          "Desteklenmeyen dosya formatı. İzin verilen: PDF, JPG, PNG, XLSX, DOCX"
        );
        return;
      }

      uploadMutation.mutate({ storeId, poId, file });
    },
    [storeId, poId, uploadMutation]
  );

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setDragOver(false);
      handleUpload(e.dataTransfer.files);
    },
    [handleUpload]
  );

  const handleDeleteConfirm = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(
      { storeId, poId, attachmentId: deleteTarget.id },
      { onSuccess: () => setDeleteTarget(null) }
    );
  };

  const handleDownload = (attachment: Attachment) => {
    window.open(
      `/api/purchasing/orders/${storeId}/${poId}/attachments/${attachment.id}/download`,
      "_blank"
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Upload Area */}
      {!disabled && (
        <div
          onDragOver={(e) => {
            e.preventDefault();
            setDragOver(true);
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
            dragOver
              ? "border-primary bg-primary/5"
              : "border-border hover:border-muted-foreground/50"
          }`}
        >
          <Upload className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
          <p className="text-sm text-muted-foreground mb-2">
            Dosyayı sürükleyip bırakın veya
          </p>
          <label>
            <input
              type="file"
              accept={ALLOWED_EXTENSIONS}
              className="hidden"
              onChange={(e) => handleUpload(e.target.files)}
              disabled={uploadMutation.isPending}
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={uploadMutation.isPending}
              asChild
            >
              <span>
                {uploadMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : null}
                Dosya Seç
              </span>
            </Button>
          </label>
          <p className="text-xs text-muted-foreground mt-2">
            PDF, JPG, PNG, XLSX, DOCX (maks. 10MB)
          </p>
        </div>
      )}

      {uploadError && (
        <p className="text-sm text-destructive">{uploadError}</p>
      )}

      {/* Attachment List */}
      {(!attachments || attachments.length === 0) ? (
        <div className="text-center py-6 text-muted-foreground text-sm">
          Henüz dosya eklenmemiş.
        </div>
      ) : (
        <div className="divide-y divide-border rounded-lg border border-border">
          {attachments.map((att) => (
            <div
              key={att.id}
              className="flex items-center gap-3 p-3 hover:bg-muted/50 transition-colors"
            >
              {getFileIcon(att.fileType)}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{att.fileName}</p>
                <p className="text-xs text-muted-foreground">
                  {att.fileSize ? formatFileSize(att.fileSize) : "—"} &middot;{" "}
                  {new Date(att.uploadedAt).toLocaleDateString("tr-TR")}
                </p>
              </div>
              <div className="flex items-center gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => handleDownload(att)}
                >
                  <Download className="h-4 w-4" />
                </Button>
                {!disabled && (
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => setDeleteTarget(att)}
                    className="text-destructive hover:text-destructive"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Delete Confirmation */}
      <AlertDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Dosyayı Sil</AlertDialogTitle>
            <AlertDialogDescription>
              &quot;{deleteTarget?.fileName}&quot; dosyasını silmek
              istediğinizden emin misiniz?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>İptal</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                "Sil"
              )}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
