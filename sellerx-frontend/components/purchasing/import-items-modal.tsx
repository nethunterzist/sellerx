"use client";

import { useState, useCallback } from "react";
import { useImportPurchaseOrderItems } from "@/hooks/queries/use-purchasing";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Upload, Loader2, FileSpreadsheet, X } from "lucide-react";

interface ImportItemsModalProps {
  storeId: string;
  poId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const ACCEPT = ".xlsx";

export function ImportItemsModal({
  storeId,
  poId,
  open,
  onOpenChange,
}: ImportItemsModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const importMutation = useImportPurchaseOrderItems();

  const validateFile = (f: File): boolean => {
    if (
      !f.name.endsWith(".xlsx") &&
      f.type !==
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ) {
      setError("Sadece .xlsx (Excel) dosyaları desteklenmektedir.");
      return false;
    }
    if (f.size > 5 * 1024 * 1024) {
      setError("Dosya boyutu 5MB'den büyük olamaz.");
      return false;
    }
    return true;
  };

  const handleFileSelect = useCallback((files: FileList | null) => {
    if (!files || files.length === 0) return;
    setError(null);
    const f = files[0];
    if (validateFile(f)) {
      setFile(f);
    }
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setDragOver(false);
      handleFileSelect(e.dataTransfer.files);
    },
    [handleFileSelect]
  );

  const handleImport = () => {
    if (!file) return;
    importMutation.mutate(
      { storeId, poId, file },
      {
        onSuccess: () => {
          onOpenChange(false);
          setFile(null);
          setError(null);
        },
        onError: (err: Error) => {
          setError(err.message || "İçe aktarma sırasında bir hata oluştu.");
        },
      }
    );
  };

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      setFile(null);
      setError(null);
    }
    onOpenChange(isOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileSpreadsheet className="h-5 w-5" />
            Excel'den İçe Aktar
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Ürünleri Excel dosyasından (.xlsx) içe aktarabilirsiniz. Dosya
            aşağıdaki sütunları içermelidir: Barkod, Adet, Üretim Maliyeti.
          </p>

          {/* File Drop Zone */}
          {!file ? (
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setDragOver(true);
              }}
              onDragLeave={() => setDragOver(false)}
              onDrop={handleDrop}
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                dragOver
                  ? "border-primary bg-primary/5"
                  : "border-border hover:border-muted-foreground/50"
              }`}
            >
              <Upload className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
              <p className="text-sm text-muted-foreground mb-2">
                Excel dosyasını sürükleyip bırakın
              </p>
              <label>
                <input
                  type="file"
                  accept={ACCEPT}
                  className="hidden"
                  onChange={(e) => handleFileSelect(e.target.files)}
                />
                <Button type="button" variant="outline" size="sm" asChild>
                  <span>Dosya Seç</span>
                </Button>
              </label>
            </div>
          ) : (
            <div className="flex items-center gap-3 bg-muted/50 rounded-lg p-3">
              <FileSpreadsheet className="h-8 w-8 text-green-500 flex-shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{file.name}</p>
                <p className="text-xs text-muted-foreground">
                  {(file.size / 1024).toFixed(1)} KB
                </p>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => {
                  setFile(null);
                  setError(null);
                }}
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          )}

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => handleOpenChange(false)}>
            İptal
          </Button>
          <Button
            onClick={handleImport}
            disabled={!file || importMutation.isPending}
            className="gap-2"
          >
            {importMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Upload className="h-4 w-4" />
            )}
            İçe Aktar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
